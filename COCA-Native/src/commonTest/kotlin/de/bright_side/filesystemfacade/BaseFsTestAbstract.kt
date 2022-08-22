package de.bright_side.filesystemfacade

import de.bright_side.filesystemfacade.facade.*
import de.bright_side.velvetkotlin.KVelvetTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class BaseFsTestAbstract {
    abstract val testDirPath: String

    abstract fun createFs(): FsfSystem

    protected fun log(message: String) = println("de.bright_side.filesystemfacade.BaseFsTestAbstract> $message")

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

    @Test
    fun childOperatorTest() {
        val testDir = init("childOperatorTest")

        val operatorFile1 = testDir / "child.txt"
        val classicFile1 = testDir.getChild("child.txt")

        val operatorFile2 = testDir / "dirAbc" / "child.txt"
        val classicFile2 = testDir.getChild("dirAbc").getChild("child.txt")

        val operatorFile3 = (testDir / "dirNew").mkdirs() / "child.txt"
        val classicFile3 = testDir.getChild("dirNew").mkdirs().getChild("child.txt")

        assertEquals(classicFile1.absolutePath, operatorFile1.absolutePath)
        assertEquals(classicFile2.absolutePath, operatorFile2.absolutePath)
        assertEquals(classicFile3.absolutePath, operatorFile3.absolutePath)
    }

    @Test
    fun testMkDirs() {
        val testDir = init("mkDirs")
        val subDir = testDir / "sub1" / "sub2"
        val separator = subDir.fsfSystem.separator

        println("subDir.absolutePath = '${subDir.absolutePath}'")
        assertEquals(testDir.absolutePath.removeSuffix(separator) + separator + "sub1" + testDir.fsfSystem.separator + "sub2", subDir.absolutePath)

        assertFalse(subDir.exists())
        subDir.mkdirs()
        assertTrue(subDir.exists())
    }

    @Test
    fun testNameAndParentFile() {
        val testDir = init("NameAndParentFile")
        val subDir = testDir /"sub1" / "sub2"

        assertEquals("sub2", subDir.name)
        assertEquals("sub1", subDir.parentFile?.name)
    }

    @Test
    fun testLengthAndTime() {
        val testDir = init("LengthAndTime")
        val text = "Hello"
        val file = (testDir / "test.txt").writeString(text)

        assertEquals(text.length.toLong(), file.length)
        val now = KVelvetTime.currentTimeMillis()
        val ageInMillis = now - file.timeLastModified

        println("testLengthAndTime: now:  ${KVelvetTime.toIsoString(now)}")
        println("testLengthAndTime: file: ${KVelvetTime.toIsoString(file.timeLastModified)}")

        println("testLengthAndTime: ageInMillis: $ageInMillis")
        assertTrue(ageInMillis >= 0)
        assertTrue(ageInMillis <= 2000)
    }

    @Test
    fun testWriteFileAndDir(){
        val testDir = init("WriteFileAndDir")
        val filename = "testFile.txt"
        val dirname = "Test-Dir"
        val testText = "Hellööö!\nTest!"
        val testFile = testDir / filename
        val testSubDir = testDir / dirname

        assertFalse(testFile.exists())
        assertFalse(testSubDir.exists())

        testFile.writeString(testText)
        testSubDir.mkdir()

        assertTrue(testFile.exists())
        assertTrue(testSubDir.exists())

        val readString = (testDir / filename).readString()
        assertEquals(testText, readString)

        assertTrue((testDir / filename).isFile)
        assertFalse((testDir / filename).isDirectory)

        assertTrue((testDir / dirname).isDirectory)
        assertFalse((testDir / dirname).isFile)

    }

    @Test
    fun testDeleteFile(){
        val testDir = init("DeleteFile")
        val testFile = testDir / "testFile.txt"

        assertFalse(testFile.exists())

        testFile.writeString("")
        assertTrue(testFile.exists())

        testFile.delete()
        assertFalse(testFile.exists())
    }

    @Test
    fun testDeleteFileAfterAppend(){
        val testDir = init("DeleteFileAfterAppend")
        val testFile = testDir / "testFile.txt"

        assertFalse(testFile.exists())

        testFile.writeString("x", true)
        assertTrue(testFile.exists())

        testFile.delete()
        assertFalse(testFile.exists())
    }

    @Test
    fun testDeleteFileAfterAppendAndRead(){
        val testDir = init("DeleteFileAfterAppendAndRead")
        val testFile = testDir / "testFile.txt"

        assertFalse(testFile.exists())

        testFile.writeString("x", true)
        assertTrue(testFile.exists())
        testFile.readString()

        testFile.delete()
        assertFalse(testFile.exists())
    }

    @Test
    fun testDeleteFileAfterWriteAndRead(){
        val testDir = init("DeleteFileAfterWriteAndRead")
        val testFile = testDir / "testFile.txt"

        assertFalse(testFile.exists())

        testFile.writeString("x")
        assertTrue(testFile.exists())
        testFile.readString()

        testFile.delete()
        assertFalse(testFile.exists())
    }

    @Test
    fun testAppendToFile(){
        val testDir = init("AppendToFile")
        val testFile = testDir / "testFile.txt"

        val text1 = "hello1"
        val text2 = "hello2"

        //: without append the content is replaced
        assertFalse(testFile.exists())
        testFile.writeString(text1)
        assertEquals(text1, testFile.readString())
        testFile.writeString(text2)
        assertEquals(text2, testFile.readString())

        //: with append the content is appended
        testFile.writeString(text1)
        assertEquals(text1, testFile.readString())
        testFile.writeString(text2, true)
        assertEquals(text1 + text2, testFile.readString())
    }

    @Test
    fun testAppendToFileViaOperator(){
        log("testAppendToFileViaOperator: start")
        val testDir = init("AppendToFileViaOperator")
        val testFile = testDir / "testFile.txt"

        val text1 = "hello1"
        val text2 = "hello2"

        //: without append the content is replaced
        assertFalse(testFile.exists())
        testFile += text1
        assertEquals(text1, testFile.readString())
        testFile.writeString(text2)
        assertEquals(text2, testFile.readString())

        //: with append the content is appended
        log("testAppendToFileViaOperator: delete file. '${testFile.absolutePath}'")

        testFile.delete()
        log("testAppendToFileViaOperator: append text1")
        testFile += text1
        log("testAppendToFileViaOperator: check text 1")
        assertEquals(text1, testFile.readString())
        log("testAppendToFileViaOperator: append text2")
        testFile += text2
        log("testAppendToFileViaOperator: check text 1 and 2")
        assertEquals(text1 + text2, testFile.readString())
    }

    @Test
    fun testCopyFileAndDir(){
        val testDir = init("CopyFileAndDir")
        val filename1 = "testFile.txt"
        val filename2 = "testFileCopied.txt"
        val testText1 = "Hellööö!\nTest!"

        println("1")
        val sourceDir = (testDir / "source").mkdirs()
        println("2")
        val destDir = (testDir /"dest").mkdirs()

        println("3")
        (sourceDir / filename1).writeString(testText1)
        val destFile = destDir / filename2

        println("4")
        (sourceDir / filename1).copyTo(destFile)

        println("5")
        assertTrue(destFile.exists())
        assertEquals(testText1, (destDir / filename2).readString())
    }


    @Test
    fun test_ListFiles_order() {
        val testDir = init("test_ListFiles_order")
        val text = "Hello"
        (testDir / "test1.txt").writeString(text)
        (testDir / "test2.txt").writeString(text)
        val dir1 = (testDir /"dir1").mkdirs()
        val dir2 = (testDir /"dir2").mkdirs()
        (dir1 / "dir1-1").mkdirs()
        (dir1 / "zzz").mkdirs()
        (dir1 / "axx").mkdirs()
        (dir1 / "dir1-2").mkdirs()
        (dir1 / "test1-1.txt").writeString(text)
        (dir1 / "abc.txt").writeString(text)
        (dir1 / "xyz.txt").writeString(text)

        (dir2 /"test2-1.txt").writeString(text)


        val expectedListDirAsString =
            """
                <D> dir1
                   <D> axx
                   <D> dir1-1
                   <D> dir1-2
                   <D> zzz
                   <F> abc.txt
                   <F> test1-1.txt
                   <F> xyz.txt
                <D> dir2
                   <F> test2-1.txt
                <F> test1.txt
                <F> test2.txt
            """.trimIndent().trimEnd()
        assertEquals(expectedListDirAsString, testDir.listDirAsString().trimEnd())
    }

    @Test
    fun testListFiles() {
        val testDir = init("ListFiles")
        val text = "Hello"
        (testDir / "test1.txt").writeString(text)
        (testDir / "test2.txt").writeString(text)
        val dir1 = (testDir /"dir1").mkdirs()
        val dir2 = (testDir /"dir2").mkdirs()
        (dir1 / "dir1-1").mkdirs()
        (dir1 / "dir1-2").mkdirs()
        (dir1 / "test1-1.txt").writeString(text)
        (dir2 / "test2-1.txt").writeString(text)

        val dir1Files = dir1.listFiles() ?: throw Exception("dir1 list files did not return any files")
        println("testListFiles: dir1Files names = ${dir1Files.map { it.name }}")
        assertEquals(3, dir1Files.size)




        println("testListFiles: dir:\n${testDir.listDirAsString()}")


        val expectedListDirAsString =
            """
                <D> dir1
                   <D> dir1-1
                   <D> dir1-2
                   <F> test1-1.txt
                <D> dir2
                   <F> test2-1.txt
                <F> test1.txt
                <F> test2.txt
            """.trimIndent().trimEnd()
        assertEquals(expectedListDirAsString, testDir.listDirAsString().trimEnd())

        val listFlat = testDir.listFiles()!!
        assertEquals(4, listFlat.size)
        val namesFlat = listFlat.map{it.name}
        assertTrue(namesFlat.contains("dir1"))
        assertTrue(namesFlat.contains("dir2"))
        assertTrue(namesFlat.contains("test1.txt"))
        assertTrue(namesFlat.contains("test2.txt"))

        val listTree = testDir.listFilesTree()!!
        assertEquals(8, listTree.size)
        val namesTree = listTree.map{it.name}
        assertTrue(namesTree.contains("dir1"))
        assertTrue(namesTree.contains("dir1-1"))
        assertTrue(namesTree.contains("dir1-2"))
        assertTrue(namesTree.contains("dir2"))
        assertTrue(namesTree.contains("test1-1.txt"))
        assertTrue(namesTree.contains("test2-1.txt"))
        assertTrue(namesTree.contains("test1.txt"))
        assertTrue(namesTree.contains("test2.txt"))
    }

    private fun formatItem(baseFile: FsfFile, file: FsfFile): String{
        val typeString = if (file.isFile) "<F>" else "<D>"
        val path = file.absolutePath.substring(baseFile.absolutePath.length).replace("\\", "/").removePrefix("/")
        return "$typeString $path\n"
    }

    private fun formatAbsolutePathList(baseFile: FsfFile, files: List<FsfFile>?): String {
        if (files == null){
            throw Exception("list tree returned null")
        }
        return files.joinToString(separator = "") {formatItem(baseFile, it)}
    }

    @Test
    fun testListFilesTreeFilter() {
        val testDir = init("testListFilesTreeFilter")
        val text = "Hello"
        (testDir / "test1.txt").writeString(text)
        (testDir / "test2.txt").writeString(text)
        val dir1 = (testDir /"dir1").mkdirs()
        val dir2 = (testDir /"dir2e").mkdirs()
        (dir1 / "dir1-1").mkdirs()
        (dir1 / "zzz").mkdirs()
        (dir1 / "axx").mkdirs()
        (dir1 / "dir1-2").mkdirs()
        (dir1 / "test1-1.txt").writeString(text)
        (dir1 / "abc.txt").writeString(text)
        (dir1 / "xyz.txt").writeString(text)

        (dir2 /"test2-1.txt").writeString(text)

        val expectedListDirAsString =
            """
                <D> dir1
                   <D> axx
                   <D> dir1-1
                   <D> dir1-2
                   <D> zzz
                   <F> abc.txt
                   <F> test1-1.txt
                   <F> xyz.txt
                <D> dir2e
                   <F> test2-1.txt
                <F> test1.txt
                <F> test2.txt
            """.trimIndent().trimEnd()
        assertEquals(expectedListDirAsString, testDir.listDirAsString().trimEnd())

        val actualListFullTree = formatAbsolutePathList(testDir, testDir.listFilesTree()).trimEnd()
        val expectedListFullTree =
            """
                <D> dir1
                <F> dir1/abc.txt
                <D> dir1/axx
                <D> dir1/dir1-1
                <D> dir1/dir1-2
                <F> dir1/test1-1.txt
                <F> dir1/xyz.txt
                <D> dir1/zzz
                <D> dir2e
                <F> dir2e/test2-1.txt
                <F> test1.txt
                <F> test2.txt
            """.trimIndent().trimEnd()
        assertEquals(expectedListFullTree, actualListFullTree)

        val filterA = buildList{
            add(PathFilterItem(IncludeExclude.INCLUDE, "e", MatchType.CONTAINS))
        }
        val actualListFullFilterA = formatAbsolutePathList(testDir, testDir.listFilesTree(filterA)).trimEnd()
        val expectedListFullFilterA =
            """
                <F> dir1/test1-1.txt
                <D> dir2e
                <F> dir2e/test2-1.txt
                <F> test1.txt
                <F> test2.txt
            """.trimIndent().trimEnd()
        assertEquals(expectedListFullFilterA, actualListFullFilterA)

        val filterB = buildList{
            add(PathFilterItem(IncludeExclude.EXCLUDE, "e", MatchType.CONTAINS))
        }
        val actualListFullFilterB = formatAbsolutePathList(testDir, testDir.listFilesTree(filterB)).trimEnd()
        val expectedListFullFilterB =
            """
                <D> dir1
                <F> dir1/abc.txt
                <D> dir1/axx
                <D> dir1/dir1-1
                <D> dir1/dir1-2
                <F> dir1/xyz.txt
                <D> dir1/zzz
            """.trimIndent().trimEnd()
        assertEquals(expectedListFullFilterB, actualListFullFilterB)

        val filterC = buildList{
            add(PathFilterItem(IncludeExclude.EXCLUDE, "e", MatchType.CONTAINS))
            add(PathFilterItem(IncludeExclude.EXCLUDE, ".txt", MatchType.ENDS_WITH))
            add(PathFilterItem(IncludeExclude.INCLUDE, "a", MatchType.CONTAINS))
            add(PathFilterItem(IncludeExclude.INCLUDE, "x", MatchType.ENDS_WITH))
        }
        val actualListFullFilterC = formatAbsolutePathList(testDir, testDir.listFilesTree(filterC)).trimEnd()
        val expectedListFullFilterC =
            """
                <D> dir1/axx
            """.trimIndent().trimEnd()
        assertEquals(expectedListFullFilterC, actualListFullFilterC)

    }

}