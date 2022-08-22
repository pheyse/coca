package de.bright_side.brightutil

import de.bright_side.velvetkotlin.UniqueException

/**
 * @author Philip Heyse
 */
class EasyParser(val string: String) {
    data class TextAndPosMap(val text: String, val positions: Map<Int, Int>){
        /**
         * returns the text at the given startPosIndex until right before the untilPosIndex, so that "my %1%hello%2% text"
         *   will return the range from the position of "h" until including the position of "o" of the word hello
         * */
        fun posRange(startPosIndex: Int, untilPosIndex: Int) = positions[startPosIndex]!! until positions[untilPosIndex]!!
        /**
         * returns the text at the given startPosIndex until right before the untilPosIndex, so that "my %1%hello%2% text"
         *   will return "hello" for textBetween(1, 2)
         * */
        fun textBetween(startPosIndex: Int, untilPosIndex: Int) = text.substring(posRange(startPosIndex, untilPosIndex))
    }

    private var stringInUpperCase: String = string.uppercase()
    var pos = 0
        private set
    private var stringLength = string.length

    /**
     * sets the current position to the index after the given tag
     *
     * @param tag
     * @param ignoreCase
     * @return true if the given tag was found, false otherwise
     */
    fun skipTo(tag: String, ignoreCase: Boolean): Boolean {
        var index = if (ignoreCase) {
            stringInUpperCase.indexOf(tag.uppercase(), pos)
        } else {
            string.indexOf(tag, pos)
        }
        if (index < 0) {
            return false
        }
        pos = index + tag.length
        return true
    }

    fun getNextItem(startTag: String, endTag: String, ignoreCase: Boolean): EasyParserItem? {
        val searchTags: MutableMap<String, List<String>> = HashMap()
        searchTags[startTag] = listOf(endTag)
        return getNextItem(searchTags, ignoreCase)
    }

    fun getNextItem(startTag1: String, endTag1: String, startTag2: String, endTag2: String, ignoreCase: Boolean): EasyParserItem? {
        val searchTags: MutableMap<String, List<String>> = HashMap()
        searchTags[startTag1] = listOf(endTag1)
        searchTags[startTag2] = listOf(endTag2)
        return getNextItem(searchTags, ignoreCase)
    }

    /**
     *
     * @return true if the parser has reached the end of the string
     */
    private fun endReached(): Boolean {
        return pos >= string.length
    }

    /**
     * @param searchTags key: start token to search, value list of strings of possible end-tokens to the given start token
     * @return null if no next item could be found
     */
    fun getNextItem(searchTags: Map<String, List<String>>, ignoreCase: Boolean): EasyParserItem? {
        //: find start tag
        var nextStartTag: String? = null
        var nextStartTagPos = -1
        var useString = string
        if (ignoreCase) {
            useString = stringInUpperCase
        }
        for (startTag in searchTags.keys) {
            var useStartTag = startTag
            if (ignoreCase) {
                useStartTag = startTag.uppercase()
            }
            val index = useString.indexOf(useStartTag, pos)
            if (index >= 0 && (nextStartTag == null || index < nextStartTagPos)) {
                nextStartTag = startTag
                nextStartTagPos = index
            }
        }
        //: no start tag found?
        if (nextStartTagPos < 0) {
            return null
        }
        nextStartTagPos += nextStartTag!!.length
        val endTags = searchTags[nextStartTag]!!

        //: find end tag
        var nextEndTag: String? = null
        var nextEndTagPos = -1
        for (endTag in endTags) {
            var useEndTag = endTag
            if (ignoreCase) {
                useEndTag = endTag.uppercase()
            }
            val index = useString.indexOf(useEndTag, nextStartTagPos)
            if (index >= 0 && (nextEndTag == null || index < nextEndTagPos)) {
                nextEndTag = endTag
                nextEndTagPos = index
            }
        }
        //: no end tag found?
        if (nextEndTagPos < 0) {
            return null
        }
        pos = nextEndTagPos + nextEndTag!!.length
        return EasyParserItem(string, nextStartTag, nextEndTag, nextStartTagPos, nextEndTagPos)
    }

    data class ReadUntilItem(
        val tag: String?,
        /** null if nothing was found  */
        val text: String,
    ){
        val found: Boolean
            get() = (tag != null)

    }

    data class EasyParserItem(
        private val searchString: String,
        val startTag: String,
        val endTag: String,
        val startPos: Int,
        val endPos: Int,
    ) {
        val content: String
            get() = searchString.substring(startPos, endPos)
        override fun toString() = "content = '$content', startTag = '$startTag', endTag = '$endTag', startPos = $startPos, endPos = $endPos"
    }

    /**
     * skips all next spaces
     *
     * @return true if any spaces have been skipped
     */
    fun skipSpaces(): Boolean {
        var skipped = false
        while (pos < stringLength && string[pos] == ' ') {
            pos++
            skipped = true
        }
        return skipped
    }

    /**
     *
     * @param endTag
     * @param ignoreCase
     * @return the string between the current position and before the end tag or null if the endTag does not occur
     */
    fun readUntil(endTag: String, ignoreCase: Boolean): String? {
        val beginIndex = pos
        var index = if (ignoreCase) {
            stringInUpperCase.indexOf(endTag.uppercase(), pos)
        } else {
            string.indexOf(endTag, pos)
        }
        if (index < 0) {
            return null
        }
        pos = index + endTag.length
        return string.substring(beginIndex, index)
    }

    /**
     *
     * @return the string between the current position and before the end tag or null if the endTag does not occur
     */
    fun readAmount(numberOfCharactersToRead: Int): String {
        val result = StringBuilder()
        var readCharacters = 0
        val length = string.length
        while (pos < length && readCharacters < numberOfCharactersToRead) {
            result.append(string[pos])
            pos++
            readCharacters++
        }
        return result.toString()
    }

    fun skipAmount(amount: Int) {
        pos += amount
    }

    /**
     *
     * @param tag
     * @param ignoreCase
     * @return the string between the current position and before the end tag
     */
    fun readUntilOrEnd(tag: String, ignoreCase: Boolean = false): String {
        val beginIndex = pos
        var index = if (ignoreCase) {
            stringInUpperCase.indexOf(tag.uppercase(), pos)
        } else {
            string.indexOf(tag, pos)
        }
        if (index < 0) {
            return string.substring(beginIndex)
        }
        pos = index + tag.length
        return string.substring(beginIndex, index)
    }

    /**
     * (tested)
     * starting from the parsers current position (included) reads until the next matching position or until the end of the parser's text
     * if no match is found.  The next matching position is the tag that occurs next.  If there are multiple tags that occur next which
     * may be the case if they have different length (example '<' and '<!') then the tag which appears first in the list of tags
     * will be the match.
     * @param includeTag false (default) means the text until the tag (exclusive) will be returned and the position of the parser
     *                      is the position of the tag start.  true means that the text until the end of the tag (text before tag
     *                      plus the tag) will be returned and the parser will be set to the position right after the tag.
     */
    fun readUntilOrEnd(tags: List<String>, ignoreCase: Boolean = false, includeTag: Boolean = false): ReadUntilItem {
        var bestIndex = -1
        var bestTag: String? = null
        for (i in tags) {
            val index = if (ignoreCase) {
                stringInUpperCase.indexOf(i.uppercase(), pos)
            } else {
                string.indexOf(i, pos)
            }
            if (bestTag == null || index >= 0 && index < bestIndex) {
                bestIndex = index
                if (index >= 0) {
                    bestTag = i
                }
            }
        }
        var text: String?
        if (bestTag != null) {
            val endIndex = if (includeTag) bestIndex + bestTag.length else bestIndex
            text = string.substring(pos, endIndex)
            pos = bestIndex
            if (includeTag){
                pos += bestTag.length
            }
        } else {
            text = string.substring(pos)
            pos = string.length
        }
        return ReadUntilItem(bestTag, text)
    }

    /**
     * reads until any of the given end tags occurs
     *
     * @param endTags
     * @return
     */
    fun readUntil(endTags: List<String>, ignoreCase: Boolean): String? {
        val beginIndex = pos
        var useString = string
        if (ignoreCase) {
            useString = stringInUpperCase
        }

        //: find end tag
        var nextEndTag: String? = null
        var nextEndTagPos = -1
        for (endTag in endTags) {
            var useEndTag = endTag
            if (ignoreCase) {
                useEndTag = endTag.uppercase()
            }
            val index = useString.indexOf(useEndTag, beginIndex)
            if (index >= 0 && (nextEndTag == null || index < nextEndTagPos)) {
                nextEndTag = endTag
                nextEndTagPos = index
            }
        }
        //: no end tag found?
        if (nextEndTagPos < 0) {
            return null
        }
        pos = nextEndTagPos + nextEndTag!!.length
        return string.substring(beginIndex, nextEndTagPos)
    }

    companion object {
        fun removeTextBetweenTags(string: String, searchTags: Map<String, List<String>>, ignoreCase: Boolean): String {
            val parser = EasyParser(string)
            val returnText = StringBuilder()
            val startSearchTags: List<String> = ArrayList(searchTags.keys)
            var item = parser.readUntilOrEnd(startSearchTags, ignoreCase)
            if (parser.endReached()) {
                returnText.append(item.text)
            }
            while (!parser.endReached()) {
                returnText.append(item.text)
                val startTag = item.tag
                if (startTag != null) {
                    //: read until the end of the tag
                    parser.readUntilOrEnd(searchTags[startTag]!!, ignoreCase)
                }
                //: next item
                item = parser.readUntilOrEnd(startSearchTags, ignoreCase)
                if (parser.endReached()) {
                    returnText.append(item.text)
                }
            }
            return returnText.toString()
        }

        fun replaceTextBetweenTags(
            string: String,
            searchTags: Map<String, List<String>>,
            ignoreCase: Boolean,
            charToReplaceWith: Char
        ): String {
            val parser = EasyParser(string)
            val returnText = StringBuilder()
            val startSearchTags: List<String> = ArrayList(searchTags.keys)
            var item = parser.readUntilOrEnd(startSearchTags, ignoreCase)
            if (parser.endReached()) {
                returnText.append(item.text)
            }
            while (!parser.endReached()) {
                returnText.append(item.text)
                val startTag = item.tag
                if (startTag != null) {
                    //: read until the end of the tag
                    item = parser.readUntilOrEnd(searchTags[startTag]!!, ignoreCase)
                    var length = startTag.length + item.text.length
                    if (item.tag != null) {
                        length += item.tag!!.length
                    }
                    returnText.append(charToReplaceWith.toString().repeat(length))
                }
                //: next item
                item = parser.readUntilOrEnd(startSearchTags, ignoreCase)
                if (parser.endReached()) {
                    returnText.append(item.text)
                    var length = 0
                    if (item.tag != null) {
                        length += item.tag!!.length
                    }
                    returnText.append(charToReplaceWith.toString().repeat(length))
                }
            }
            return returnText.toString()
        }

        /**
         * (tested)
         * read a text such as
         * """
         *   my value %1%" = abc
         *   <xyz>abc%2%<abc>
         * """
         * which contains %1%, %2", etc. as marks.
         *
         * Each mark may occur only once.
         *
         * @return TextAndPosMap object which contains the text without the marks and the positions of each mark
         */
        fun toTextAndPosMap(textWithMarks: String, markIndicator: String = "%"): TextAndPosMap {
            var resultText = ""
            var resultMap = mutableMapOf<Int, Int>()
            val parser = EasyParser(textWithMarks)
            val tags = listOf(markIndicator)
            var match = parser.readUntilOrEnd(tags)
            var offset = 0
            while (match.found){
                val startPos = parser.pos
                parser.skipAmount(match.tag?.length ?: 0)
                resultText += match.text
                val endTag = parser.readUntilOrEnd(tags)
                if (!endTag.found){
                    throw UniqueException("d7f4b655-0e9d-41b1-9452-3fa9dc165c7d", "Found start tag but end tag '$markIndicator' is missing")
                }
                parser.skipAmount(endTag.tag?.length ?: 0)
                val text = endTag.text
                val index: Int = try{
                    text.toInt()
                } catch (e: Exception){
                    throw UniqueException("d7f4b655-0e9d-41b1-9452-3fa9dc165c7d", "Could not read index number from text '$text'")
                }
                if (resultMap.containsKey(index)){
                    throw UniqueException("b600b7cf-03ef-42e0-b0f5-dc93752a9050", "Index $index occurs multiple times")
                }
                resultMap[index] = startPos - offset
                offset += parser.pos - startPos
                match = parser.readUntilOrEnd(tags)
            }
            resultText += match.text
            return TextAndPosMap(resultText, resultMap)
        }
    }
}