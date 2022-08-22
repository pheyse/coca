package de.bright_side.coca

import de.bright_side.brightutil.EasyParser
import de.bright_side.brightutil.EasyParser.TextAndPosMap
import de.bright_side.velvetkotlin.KVelvetUtil.formatEscapeChars
import kotlin.test.Test
import kotlin.test.assertEquals

class CocaParserTest: CocaParser() {
    private fun crateInstance() = CocaParserTest()

    private fun log(message: String) = println("de.bright_side.coca.CocaParserTest> $message")

    @Test
    fun test_findAllPositions_bulk() {
        val parser = crateInstance()
        assertEquals(listOf(), parser.findAllPositions("", "X"))
        assertEquals(listOf(0), parser.findAllPositions("X", "X"))
        assertEquals(listOf(3, 7, 11), parser.findAllPositions("abcXdefXghiX", "X"))
        assertEquals(listOf(3, 7, 11), parser.findAllPositions("abc\ndef\nghi\n", "\n"))
        assertEquals(listOf(0, 4, 8, 12), parser.findAllPositions("XabcXdefXghiX", "X"))
        assertEquals(listOf(), parser.findAllPositions("XabcXdefXghiX", "Y"))
    }


    @Test
    fun test_findLineNumber_bulk() {
        val processor: CocaParserTest = crateInstance()

        val singleLine = processor.findAllPositions("line 1", "\n")
        assertEquals(1, processor.findLineNumber(0, singleLine))
        assertEquals(1, processor.findLineNumber(1, singleLine))

        val singleLineWithLineBreakEnd = processor.findAllPositions("line 1\n", "\n")
        assertEquals(1, processor.findLineNumber(0, singleLineWithLineBreakEnd))
        assertEquals(1, processor.findLineNumber(1, singleLineWithLineBreakEnd))
        assertEquals(1, processor.findLineNumber(6, singleLineWithLineBreakEnd))  //: special case: exact pos of \n

        val fiveSimpleLines = processor.findAllPositions("line 1\nline 2\nline 3\nline 4\nline 5", "\n")
        assertEquals(1, processor.findLineNumber(0, fiveSimpleLines))
        assertEquals(1, processor.findLineNumber(1, fiveSimpleLines))
        assertEquals(1, processor.findLineNumber(5, fiveSimpleLines))
        assertEquals(1, processor.findLineNumber(6, fiveSimpleLines)) //: special case: exact pos of \n
        assertEquals(2, processor.findLineNumber(7, fiveSimpleLines))
        assertEquals(2, processor.findLineNumber(8, fiveSimpleLines))
        assertEquals(2, processor.findLineNumber(13, fiveSimpleLines))
        assertEquals(3, processor.findLineNumber(14, fiveSimpleLines))
    }

    @Test
    fun test_nextToEachOther_bulk() {
        val processor = crateInstance()
        val textA = "hello/*comment1*//*comment2*/there"
        val textB = "hello/*comment1*/\r\n\t /*comment2*/there"
        val textC = "hello/*comment1*/____/*comment2*/there"
        val textD = "hello/*comment1*/\r\n\t /*comment2*/"
        val occurrence1 = CommentOccurrence("", 5..16, 1..1, "")
        val occurrence2 = CommentOccurrence("", 17..28, 1..1, "")
        val occurrence3 = CommentOccurrence("", 21..32, 1..1, "")
        assertEquals(true, processor.nextToEachOther(occurrence1, occurrence2, textA))
        assertEquals(true, processor.nextToEachOther(occurrence1, occurrence3, textB))
        assertEquals(false, processor.nextToEachOther(occurrence1, occurrence3, textC))
        assertEquals(true, processor.nextToEachOther(occurrence1, occurrence3, textD))
        assertEquals(false, processor.nextToEachOther(null, occurrence1, textA))
    }

    private fun readUntilEndOfStringOrEndOfText(text: String, startPos: Int) : Pair<Int, Boolean> {
        val processor = crateInstance()
        val parser = EasyParser(text)
        parser.skipAmount(startPos)
        val found = processor.readUntilEndOfStringOrEndOfText(parser)
        return Pair(parser.pos, found)
    }

    @Test
    fun test_readUntilEndOfStringOrEndOfText_bulk(){
        assertEquals(Pair(10, true), readUntilEndOfStringOrEndOfText("x = \"test\"", 5))
        assertEquals(Pair(5, true), readUntilEndOfStringOrEndOfText("x = \"test\"", 0))
        assertEquals(Pair(1, true), readUntilEndOfStringOrEndOfText("\"", 0))
        assertEquals(Pair(1, false), readUntilEndOfStringOrEndOfText("x", 0))
        assertEquals(Pair(2, true), readUntilEndOfStringOrEndOfText("x\"", 0))
        assertEquals(Pair(5, false), readUntilEndOfStringOrEndOfText("x\"abc", 2))
    }

    private fun createTestTextOne(): TextAndPosMap {
        val textWithMarksOrig = """
            %1%// comment to remove 1
            %2%package abc
            //: comment to keep 1, quote: "
            /** test class*/
            %3%/* comment to 
             * remove 2
             */%4%
            class MyClass{
                val myString = "hello \"world\"!"
                
                val myRawString = §§§
                    /*ignored comment to remove*/
                    "ignored string"
                    //other ignored comment
                §§§.trimIndent()
                
                val myStringWithComment = "hello \" quote. and comment is /*comment */"
                
                %5%/*
                 * commented out code
                 * bla
                 */%6%
            }
            /** comment to keep 2
             * //comment to ignore
             */
        """.trimIndent()
        val textWithMarks = textWithMarksOrig.replace("§§§", "\"\"\"").replace("\r", "")
        log("createTestTextOne. textWithMarks = >>${formatEscapeChars(textWithMarks)}")

        return EasyParser.toTextAndPosMap(textWithMarks)
    }

    @Test
    fun test_findOccurrencesToRemoveInText_normal(){
        val testText = createTestTextOne()
        log("test_findOccurrencesToRemoveInText_normal. testText.text = >>\n${testText.text}\n<<")
        log("test_findOccurrencesToRemoveInText_normal. testText.positions = ${testText.positions}")
        val processor = crateInstance()

        val commentsToRemove = listOf(Pair("/*", "*/"), Pair("//", "\n"))
        val commentsToKeep = listOf(Pair("/**", "*/"), Pair("//:", "\n"))

        val result = processor.findOccurrencesToRemoveInText(testText.text, "/", commentsToRemove, commentsToKeep)
        log("test_findOccurrencesToRemoveInText_normal. Result = ${result.joinToString { "\n - ${it.toString().replace("\n", "\\n")}" }}")

        assertEquals(3, result.size)

        assertEquals(testText.positions[1], result[0].pos.first)
        assertEquals(testText.positions[2]!! - 1, result[0].pos.last) //: -1 because the pos of the mark is right AFTER the tag
        assertEquals(1, result[0].lines.first)
        assertEquals(1, result[0].lines.last)
        assertEquals("// comment to remove 1\n", result[0].commentText)

        assertEquals(testText.positions[3], result[1].pos.first)
        assertEquals(testText.positions[4]!! -1, result[1].pos.last) //: -1 because the pos of the mark is right AFTER the tag
        assertEquals(5, result[1].lines.first)
        assertEquals(7, result[1].lines.last)
        assertEquals("/* comment to \n * remove 2\n */", result[1].commentText)

        assertEquals(testText.positions[5], result[2].pos.first)
        assertEquals(testText.positions[6]!! -1, result[2].pos.last) //: -1 because the pos of the mark is right AFTER the tag
        assertEquals(19, result[2].lines.first)
        assertEquals(22, result[2].lines.last)
        assertEquals("/*\n     * commented out code\n     * bla\n     */", result[2].commentText)
    }

    @Test
    fun test_findOccurrencesToRemoveInText_singleLineToRemove(){
        val testText = """
            //line 1
        """.trimIndent().replace("\r", "")
        val processor = crateInstance()
        val commentsToRemove = listOf(Pair("/*", "*/"), Pair("//", "\n"))
        val commentsToKeep = listOf(Pair("/**", "*/"), Pair("//:", "\n"))
        val result = processor.findOccurrencesToRemoveInText(testText, "/", commentsToRemove, commentsToKeep)
        log("test_findOccurrencesToRemoveInText_normal. Result = ${result.joinToString { "\n - ${it.toString().replace("\n", "\\n")}" }}")

        assertEquals(1, result.size)

        assertEquals(0, result[0].pos.first)
        assertEquals(7, result[0].pos.last)
        assertEquals(1, result[0].lines.first)
        assertEquals(1, result[0].lines.last)
        assertEquals("//line 1", result[0].commentText)
    }

    @Test
    fun test_findOccurrencesToRemoveInText_allCommentToRemove(){
        val testText = """
            //line 1
            //line 2
            //line 3
        """.trimIndent().replace("\r", "")
        val processor = crateInstance()
        val commentsToRemove = listOf(Pair("/*", "*/"), Pair("//", "\n"))
        val commentsToKeep = listOf(Pair("/**", "*/"), Pair("//:", "\n"))
        val result = processor.findOccurrencesToRemoveInText(testText, "/", commentsToRemove, commentsToKeep)
        log("test_findOccurrencesToRemoveInText_allCommentToRemove. Result = ${result.joinToString { "\n - ${it.toString().replace("\n", "\\n")}" }}")

        assertEquals(1, result.size)

        assertEquals(0, result[0].pos.first)
        assertEquals(25, result[0].pos.last)
        assertEquals(1, result[0].lines.first)
        assertEquals(3, result[0].lines.last)
        assertEquals("//line 1\n//line 2\n//line 3", result[0].commentText)
    }

    @Test
    fun test_findOccurrencesToRemoveInText_mergeTwoDifferentCommentTypes(){
        val testText = """
            /*line 1
            line 2*/
            //line 3
        """.trimIndent().replace("\r", "")
        val processor = crateInstance()
        val commentsToRemove = listOf(Pair("/*", "*/"), Pair("//", "\n"))
        val commentsToKeep = listOf(Pair("/**", "*/"), Pair("//:", "\n"))
        val result = processor.findOccurrencesToRemoveInText(testText, "/", commentsToRemove, commentsToKeep)
        log("test_findOccurrencesToRemoveInText_mergeTwoDifferentCommentTypes. Result = ${result.joinToString { "\n - ${it.toString().replace("\n", "\\n")}" }}")

        assertEquals(1, result.size)

        assertEquals(0, result[0].pos.first)
        assertEquals(25, result[0].pos.last)
        assertEquals(1, result[0].lines.first)
        assertEquals(3, result[0].lines.last)
        assertEquals("/*line 1\nline 2*/\n//line 3", result[0].commentText)
    }

    @Test
    fun test_findOccurrencesToRemoveInText_mergeTwoDifferentCommentTypesWithWhiteSpace(){
        val testText = """
            /*line 1
            line 2*/
            
            
            //line 3
        """.trimIndent().replace("\r", "")
        val processor = crateInstance()
        val commentsToRemove = listOf(Pair("/*", "*/"), Pair("//", "\n"))
        val commentsToKeep = listOf(Pair("/**", "*/"), Pair("//:", "\n"))
        val result = processor.findOccurrencesToRemoveInText(testText, "/", commentsToRemove, commentsToKeep)
        log("test_findOccurrencesToRemoveInText_mergeTwoDifferentCommentTypes. Result = ${result.joinToString { "\n - ${it.toString().replace("\n", "\\n")}" }}")

        assertEquals(1, result.size)

        assertEquals(0, result[0].pos.first)
        assertEquals(27, result[0].pos.last)
        assertEquals(1, result[0].lines.first)
        assertEquals(5, result[0].lines.last)
        assertEquals("/*line 1\nline 2*/\n\n\n//line 3", result[0].commentText)
    }

    @Test
    fun test_findOccurrencesToRemoveInText_noComments(){
        val testText = """
            class xyz(){
             val x = 5
            }
        """.trimIndent().replace("\r", "")
        val processor = crateInstance()
        val commentsToRemove = listOf(Pair("/*", "*/"), Pair("//", "\n"))
        val commentsToKeep = listOf(Pair("/**", "*/"), Pair("//:", "\n"))
        val result = processor.findOccurrencesToRemoveInText(testText, "/", commentsToRemove, commentsToKeep)
        assertEquals(0, result.size)
    }

    @Test
    fun test_normalCaseWithFewCharsAfterComment(){
        val meth = "test_normalCaseWithFewCharsAfterComment"
        val testText = """
            import de.bright_side.coca.CocaConfigReader
            import de.bright_side.coca.CocaOptionsReader
            
            fun main(args: Array<String>) {
            //    val fileSystem: FSFFileSystem = OkFileSystem()
            //
            //    val options = CocaOptionsReader().readOptions(args.toList())
            //    val config = CocaConfigReader().readConfig(fileSystem / options.configFilePath)
            //    CocoProcessor().process(options, config, fileSystem)
            }""".trimIndent()
        val processor = crateInstance()
        val commentsToRemove = listOf(Pair("/*", "*/"), Pair("//", "\n"))
        val commentsToKeep = listOf(Pair("/**", "*/"), Pair("//:", "\n"))
        val result = processor.findOccurrencesToRemoveInText(testText, "/", commentsToRemove, commentsToKeep)

        log("$meth: result = $result")

        assertEquals(1, result.size)
    }


}