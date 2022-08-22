package de.bright_side.filesystemfacade.memoryfs

import de.bright_side.filesystemfacade.facade.FsfFile
import de.bright_side.filesystemfacade.facade.FsfSystem
import de.bright_side.filesystemfacade.memoryfs.MemoryFs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * copy & paste from de.bright_side.filesystemfacade.BaseFsTestAbstract because there were unexpected execution errors
 */
class MemoryFsTestClone {
    val testDirPath = "/"

    fun createFs(): FsfSystem = MemoryFs()

    fun init(testName: String): FsfFile{
        println("start init - $testName")
        try{
            println("init: create FS")
            val fs = createFs()
            println("init: create dir by path")
            val dir = fs.createByPath(testDirPath + testName)
            println("init: check if dir exists")
            if (dir.exists()){
                throw Exception("Dir '" + dir.absolutePath + "' already exists. Did you forget the clean?")
            }
            println("init: dir.mkdirs. Dir = $dir")
            dir.mkdirs()
            println("completed init - $testName")
            return dir
        } catch (e: Throwable){
            e.printStackTrace()
            throw e
        }
    }


    private fun assertFalse(actual: Boolean){
        assertTrue(!actual)
    }

    @Test
    fun testMkDirsBase() {
        try{
            val testDir = init("mkDirsBase")
            assertTrue(testDir.exists())
        } catch (e: Throwable){
            e.printStackTrace()
            assertTrue(false, "Test failed")
        }
    }

}