package de.bright_side.velvetkotlin

import kotlinx.datetime.*

/**
 * @author Philip Heyse
 */
object KVelvetTime {
    fun toIsoString(epochTimeInMillis: Long, includeMillis: Boolean = true): String{
        val timeInstant: Instant = Instant.fromEpochMilliseconds(epochTimeInMillis)
        val time: LocalDateTime = timeInstant.toLocalDateTime(TimeZone.UTC)
        var result = ""
        result += format4Digits(time.year)
        result += "-"
        result += format2Digits(time.monthNumber)
        result += "-"
        result += format2Digits(time.dayOfMonth)
        result += "'T'"
        result += format2Digits(time.hour)
        result += ":"
        result += format2Digits(time.minute)
        result += ":"
        result += format2Digits(time.second)
        if (includeMillis){
            result += "."
            result += format4Digits(time.nanosecond / 1_000_000)
        }
        return result
    }

    private fun format2Digits(number: Int) = if (number < 10) "0$number" else number.toString()
    private fun format4Digits(number: Int): String {
        var padding = ""
        if (number < 10){
            padding = "000"
        } else if (number < 100){
            padding = "00"
        } else if (number < 1000){
            padding = "0"
        }

        return "$padding$number"
    }

    fun currentTimeMillisUtc() = Clock.System.now().toEpochMilliseconds()
    fun currentTimeMillis(): Long {
        return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toInstant(UtcOffset(0)).toEpochMilliseconds()
    }

    /**
     * @return the offset from UTC in millis.  Example: if current time is 19:00:00, and UTC time is 17:00:00 then the function
     * will return 2 hours as milliseconds
     */
    fun systemTimeZoneOffsetFromUtcMillis(): Long {
        val now = Clock.System.now()
        val localTime = now.toLocalDateTime(TimeZone.currentSystemDefault()).toInstant(UtcOffset(0)).toEpochMilliseconds()
        val utcTime = now.toEpochMilliseconds()
        return localTime - utcTime
    }

}