package de.bright_side.filesystemfacade.memoryfs

import de.bright_side.filesystemfacade.memoryfs.MemoryFs
import kotlin.test.Test

class SimpleMemoryFsTest {
    val testDirPath = "/"
    @Test
    fun mainTest(){
        val fs = MemoryFs()
        val testName = "testOne"
        val dir = fs.createByPath(testDirPath + testName)

    }
}