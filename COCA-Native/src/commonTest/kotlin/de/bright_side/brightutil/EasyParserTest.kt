package de.bright_side.brightutil

import de.bright_side.brightutil.EasyParser
import de.bright_side.brightutil.EasyParser.*
import kotlin.test.Test
import kotlin.test.assertEquals

class EasyParserTest {
    @Test
    fun test_toTextAndPosMap_bulk(){
        assertEquals("hello>1:5", process("hello%1%"))
        assertEquals("hello>1:0", process("%1%hello"))
        assertEquals("hello>1:0,2:5", process("%1%hello%2%"))
        assertEquals("helloTest>1:0,2:5", process("%1%hello%2%Test"))
        assertEquals("helloTest>100:0,102:5", process("%100%hello%102%Test"))
        assertEquals("hello>", process("hello"))
        assertEquals("helloTest>2:5", process("hello%2%Test"))
    }

    private fun process(text: String): String{
        val textAndPosMap = EasyParser.toTextAndPosMap(text)
        var result = textAndPosMap.text + ">"
        println("process: text = >>${textAndPosMap.text}<<")
        var index = 0
        textAndPosMap.positions.forEach { (key, value) ->
            if (index > 0){
                result += ","
            }
            result += "$key:$value"
            index ++
        }
        return result
    }

    private fun testReadUntilOrEnd(
        expectedTag: String?,
        expectedText: String,
        expectedFound: Boolean,
        expectedPos: Int,
        givenText: String,
        startPos: Int,
        tags: List<String>,
        ignoreCase: Boolean,
        includeTag: Boolean,
    ){
        val parser = EasyParser(givenText)
        parser.skipAmount(startPos)
        val result = parser.readUntilOrEnd(tags, ignoreCase, includeTag)

        assertEquals(expectedTag, result.tag)
        assertEquals(expectedText, result.text)
        assertEquals(expectedFound, result.found)
        assertEquals(expectedPos, parser.pos)
    }

    @Test
    fun readUntilOrEnd_bulk(){
        testReadUntilOrEnd("!", "hello", true, 5, "hello!", 0, listOf("!"), false, false)
        testReadUntilOrEnd("!", "hello!", true, 6, "hello!", 0, listOf("!"), false, true)
        testReadUntilOrEnd(null, "hello!", false, 6, "hello!", 0, listOf("?"), false, false)
        testReadUntilOrEnd(null, "hello!", false, 6, "hello!", 0, listOf("?"), false, true)

        testReadUntilOrEnd("!", "hello", true, 5, "hello!!", 0, listOf("!", "!!"), false, false)
        testReadUntilOrEnd("!!", "hello", true, 5, "hello!!", 0, listOf("!!", "!"), false, false)
        testReadUntilOrEnd("!", "hello!", true, 6, "hello!!", 0, listOf("!", "!!"), false, true)
        testReadUntilOrEnd("!!", "hello!!", true, 7, "hello!!", 0, listOf("!!", "!"), false, true)

        testReadUntilOrEnd("!", "ello", true, 5, "hello!", 1, listOf("!"), false, false)
        testReadUntilOrEnd("!", "llo", true, 5, "hello!", 2, listOf("!"), false, false)
        testReadUntilOrEnd("!", "lo", true, 5, "hello!", 3, listOf("!"), false, false)
        testReadUntilOrEnd("!", "o", true, 5, "hello!", 4, listOf("!"), false, false)
        testReadUntilOrEnd("!", "", true, 5, "hello!", 5, listOf("!"), false, false)
        testReadUntilOrEnd(null, "", false, 6, "hello!", 6, listOf("!"), false, false)

        testReadUntilOrEnd("!", "ello!", true, 6, "hello!", 1, listOf("!"), false, true)
        testReadUntilOrEnd("!", "llo!", true, 6, "hello!", 2, listOf("!"), false, true)
        testReadUntilOrEnd("!", "lo!", true, 6, "hello!", 3, listOf("!"), false, true)
        testReadUntilOrEnd("!", "o!", true, 6, "hello!", 4, listOf("!"), false, true)
        testReadUntilOrEnd("!", "!", true, 6, "hello!", 5, listOf("!"), false, true)
        testReadUntilOrEnd(null, "", false, 6, "hello!", 6, listOf("!"), false, true)

        testReadUntilOrEnd(null, "hello!", false, 6, "hello!", 0, listOf(), false, true)

        testReadUntilOrEnd("!", "hello", true, 5, "hello!hi!", 0, listOf("!"), false, false)
        testReadUntilOrEnd("!", "hello!", true, 6, "hello!hi!", 0, listOf("!"), false, true)
        testReadUntilOrEnd(null, "hello!hi!", false, 9, "hello!hi!", 0, listOf("?"), false, false)
        testReadUntilOrEnd(null, "hello!hi!", false, 9, "hello!hi!", 0, listOf("?"), false, true)
        testReadUntilOrEnd("!", "", true, 5, "hello!hi!", 5, listOf("!"), false, false)
        testReadUntilOrEnd("!", "hi", true, 8, "hello!hi!", 6, listOf("!"), false, false)
        testReadUntilOrEnd("!", "hi!", true, 9, "hello!hi!", 6, listOf("!"), false, true)

        testReadUntilOrEnd("AA", "aa", true, 2, "aaAAbbBBcc", 0, listOf("AA"), false, false)
        testReadUntilOrEnd("AA", "aaAA", true, 4, "aaAAbbBBcc", 0, listOf("AA"), false, true)
        testReadUntilOrEnd("AA", "", true, 0, "aaAAbbBBcc", 0, listOf("AA"), true, false)
        testReadUntilOrEnd("AA", "aa", true, 2, "aaAAbbBBcc", 0, listOf("AA"), true, true)
        testReadUntilOrEnd("AA", "", true, 0, "aaAAbbBBcc", 0, listOf("AA", "aa"), true, false)
        testReadUntilOrEnd("aa", "aa", true, 2, "aaAAbbBBcc", 0, listOf("AA", "aa"), false, true)
        testReadUntilOrEnd("AA", "", true, 0, "aaAAbbBBcc", 0, listOf("AA", "aa", "bb", "BB"), true, false)
        testReadUntilOrEnd("bb", "aaAA", true, 4, "aaAAbbBBcc", 0, listOf("bb", "BB"), false, false)
        testReadUntilOrEnd("bb", "aaAAbb", true, 6, "aaAAbbBBcc", 0, listOf("bb", "BB"), false, true)
    }
}