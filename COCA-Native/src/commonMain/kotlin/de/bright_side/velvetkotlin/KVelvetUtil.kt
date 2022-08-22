package de.bright_side.velvetkotlin

import kotlin.math.max
import kotlin.math.min

object KVelvetUtil {

    /**
     * @return escapes the \-chars such as \n, \r, \t and \\ to \\n, \\r, \\t and \\\\
     */
    fun formatEscapeChars(string: String?): String {
        if (string == null){
            return "null"
        }
        return string
            .replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * given a string and a pos the position is highlighted.
     * Example: "one two th-->r<--ee four"
     *
     * @param escapeChars if true the chars such as line break tab, etc. will be written as "\n" etc.
     */
    fun highlightPosInString(
        string: String,
        pos: Int,
        charsBefore: Int = 10,
        charsAfter: Int = 10,
        startIndicator: String = "-->",
        endIndicator: String = "<--",
        escapeChars: Boolean = true,
    ): String {
        var result = ""
        if (pos >= string.length){
            return "(pos $pos >) string length ${string.length})"
        }
        if (pos < 0){
            return "(pos $pos < 0)"
        }

        val firstPos = max(0, pos - charsBefore)
        val lastPos = min(string.length - 1, pos + charsAfter)

        result += string.substring(firstPos until pos)
        result += startIndicator + string[pos] + endIndicator
        result += string.substring(pos + 1..lastPos)

        if (escapeChars){
            result = formatEscapeChars(result)
        }
        return result
    }

    fun endsWithAny(string: String, endings: List<String>, ignoreCase: Boolean = false): Boolean{
        endings.forEach {
            if (string.endsWith(it, ignoreCase)){
                return true
            }
        }
        return false
    }
}