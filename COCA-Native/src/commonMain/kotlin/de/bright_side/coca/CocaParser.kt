package de.bright_side.coca

import de.bright_side.brightutil.EasyParser
import de.bright_side.brightutil.EasyParser.ReadUntilItem
import de.bright_side.velvetkotlin.KVelvetUtil.formatEscapeChars
import de.bright_side.velvetkotlin.KVelvetUtil.highlightPosInString

private const val ORDINAL_STRING_START = "\""
private const val ORDINAL_STRING_END = "\""
private const val ORDINAL_ESCAPED_QUOTE = "\\\""
private const val RAW_STRING_START = "\"\"\""
private const val RAW_STRING_END = "\"\"\""

open class CocaParser {
    private fun log(message: String) = println("CocaParser> $message")

    fun findOccurrencesToRemoveInText(
        text: String,
        filePath: String,
        commentsToRemove: List<Pair<String, String>>,
        commentsToKeep: List<Pair<String, String>>,
    ): List<CommentOccurrence> {
        val result = mutableListOf<CommentOccurrence>()
        val parser = EasyParser(text)
        val lineBreakPositions: List<Int> = findAllPositions(text, "\n")
        val commentToRemoveMap = commentsToRemove.toMap()
        val blocksToSkipMap = commentsToKeep.toMap().toMutableMap()
        //: handle raw strings """...""" just like simple comments because there are no escape chars
        blocksToSkipMap[RAW_STRING_START] = RAW_STRING_END
        val ordinalStringMap = listOf(Pair(ORDINAL_STRING_START, ORDINAL_STRING_END)).toMap()
        val searchTags: List<String> = createSearchTags(blocksToSkipMap.keys, commentToRemoveMap.keys, ordinalStringMap.keys)
        var nextMatch: ReadUntilItem = parser.readUntilOrEnd(searchTags)
        var previousOccurrence: CommentOccurrence? = null

        while (nextMatch.tag != null){
            val tag = nextMatch.tag
            val tagLength = tag?.length ?:0
            val commentToRemoveEnd: String? = commentToRemoveMap[tag]
            val blockToSkipEnd: String? = blocksToSkipMap[tag]
            val ordinalStringEnd: String? = ordinalStringMap[tag]
            if (commentToRemoveEnd != null){
                val startPos = parser.pos
                parser.skipAmount(tagLength)
                var commentItem = parser.readUntilOrEnd(listOf(commentToRemoveEnd), includeTag = true)
                if (!commentItem.found){
                    //: special case: no end found, but it is a comment that ends when the line ends (//...\n)
                    //: in this case it also counts as a complete comment and the function should continue
                    //: In any other case, the comment is not complete but the end of the file was reached so no other comments
                    //: may occur and the method should return
                    if (commentToRemoveEnd != "\n"){
                        return result
                    }
                }
                val endPos = parser.pos - 1
                var thisOccurrence = createOccurrence(filePath, lineBreakPositions, parser.string, startPos, endPos)
                if (nextToEachOther(previousOccurrence, thisOccurrence, text)){
                    result.removeLast()
                    thisOccurrence = mergeOccurrences(previousOccurrence, thisOccurrence, text)
                    result.add(thisOccurrence)
                } else {
                    result += thisOccurrence
                }
                previousOccurrence = thisOccurrence
            } else if (blockToSkipEnd != null){
                //: skip all chars until either end of text of end tag
                parser.skipAmount(tagLength)
                if (!parser.readUntilOrEnd(listOf(blockToSkipEnd), includeTag = true).found){
                    return result //: if nothing was found = end of text has been reached, return the result
                }
            } else if (ordinalStringEnd != null){
                parser.skipAmount(tagLength)
                val endOfText = !readUntilEndOfStringOrEndOfText(parser)
                if (endOfText){
                    return result
                }
            }
            nextMatch = parser.readUntilOrEnd(searchTags)
        }
        return result
    }

    private fun createOccurrence(filePath: String, lineBreakPositions: List<Int>, text: String, startPos: Int, endPos: Int): CommentOccurrence {
        val startLineNumber: Int = findLineNumber(startPos, lineBreakPositions)
        val endLineNumber: Int = findLineNumber(endPos, lineBreakPositions)
        val commentText = text.substring(startPos..endPos)
        return CommentOccurrence(filePath, startPos..endPos, startLineNumber..endLineNumber, commentText)
    }

    protected fun findLineNumber(pos: Int, lineBreakPositions: List<Int>) = lineBreakPositions.filter { it < pos}.size + 1

    private fun mergeOccurrences(first: CommentOccurrence?, second: CommentOccurrence, text: String): CommentOccurrence {
        if (first == null) {
            return second
        }
        val mergedPosRange = first.pos.first..second.pos.last
        val commentText = text.substring(mergedPosRange)
        return CommentOccurrence(first.filePath, mergedPosRange, first.lines.first..second.lines.last, commentText)
    }

    fun nextToEachOther(first: CommentOccurrence?, second: CommentOccurrence, text: String): Boolean {
        if (first == null){
            return false
        }
        val between = text.subSequence(first.pos.last + 1, second.pos.first).toString()
        return removeWhiteSpace(between).isEmpty()
    }

    private fun removeWhiteSpace(subSequence: String): String {
        return subSequence.replace("\t", "").replace("\r", "").replace("\n", "").replace(" ", "")
    }

    /**
     * @return true if the end of the string was reached and false if the end of the text was reached
     */
    protected fun readUntilEndOfStringOrEndOfText(parser: EasyParser): Boolean {
        var escaped = false
        var amount = 0
        parser.string.subSequence(parser.pos, parser.string.length).forEach { c ->
            if (escaped){
                escaped = false
            } else if (c == '\\'){
                escaped = true
            } else if (c == '"'){
                parser.skipAmount(amount + 1)
                return true
            }
            amount ++
        }
        parser.skipAmount(amount)
        return false
    }

    private fun createSearchTags(set1: Set<String>, set2: Set<String>, set3: Set<String>): List<String> {
        val result = mutableListOf<String>()
        result += set1
        result += set2
        result += set3
        //: sort by length descending because a tag such as "/**" should be found before "/*"
        return result.sortedByDescending { it.length }
    }

    fun findAllPositions(text: String, textToFind: String): List<Int> {
        val result = mutableListOf<Int>()
        var pos = text.indexOf(textToFind, 0)
        while (pos >= 0){
            result += pos
            pos = text.indexOf(textToFind, pos + 1)
        }
        return result
    }



//    private fun createSearchTagsOld(commentsToRemove: List<Pair<String, String>>, commentsToKeep: List<Pair<String, String>>): List<String> {
//        val result = mutableListOf<String>()
//        result.addAll(commentsToRemove.map { it.first })
//        result.addAll(commentsToKeep.map { it.first })
//        result.add(ORDINAL_STRING_START)
//        result.add(RAW_STRING_START)
//        return result
//    }
//    private fun createSearchTags(commentsToRemove: List<Pair<String, String>>, commentsToKeep: List<Pair<String, String>>): Map<String, List<String>> {
//        val result = mutableMapOf<String, List<String>>()
//        val allComments = mutableListOf<Pair<String, String>>()
//        allComments += commentsToRemove
//        allComments += commentsToKeep
//        allComments += listOf(Pair(ORDINAL_STRING_START, ORDINAL_STRING_END), Pair(RAW_STRING_START, RAW_STRING_END))
//
//        allComments.forEach {
//            result[it.first] = listOf(it.second)
//        }
//        return result
//    }





}