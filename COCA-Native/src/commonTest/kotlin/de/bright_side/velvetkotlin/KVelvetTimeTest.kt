package de.bright_side.velvetkotlin

import de.bright_side.velvetkotlin.KVelvetTime
import de.bright_side.velvetkotlin.KVelvetTime.currentTimeMillis
import de.bright_side.velvetkotlin.KVelvetTime.currentTimeMillisUtc
import de.bright_side.velvetkotlin.KVelvetTime.systemTimeZoneOffsetFromUtcMillis
import kotlin.test.Test

class KVelvetTimeTest {

    @Test
    fun test_currentTime(){
        println("time: " + KVelvetTime.toIsoString(currentTimeMillis(), true))
    }

    @Test
    fun test_defaultTimeZoneOffsetFromUTC(){
        println("current time: " + KVelvetTime.toIsoString(currentTimeMillis(), true))
        println("utc time: " + KVelvetTime.toIsoString(currentTimeMillisUtc(), true))
        val defaultTimeZoneOffsetFromUTC = systemTimeZoneOffsetFromUtcMillis()
        println("offset: $defaultTimeZoneOffsetFromUTC")
        println("offset as ISO string: " + KVelvetTime.toIsoString(defaultTimeZoneOffsetFromUTC, true))
    }
}