package de.bright_side.coca

import de.bright_side.brightutil.EasyParser.Companion.toTextAndPosMap
import de.bright_side.coca.CocaAction.ARCHIVE
import de.bright_side.coca.CocaUtil.createFilesToCommentsMap
import de.bright_side.coca.LogLevel.DEBUG
import de.bright_side.coca.PreviewFormat.BEGINNING_OF_TEXT
import de.bright_side.filesystemfacade.memoryfs.MemoryFs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


const val DEFAULT_RUN_ID = "2022-07-31T21-09-48_543"
const val SECOND_RUN_ID = "2022-08-02T20-09-30_000"

private fun createOptions() = CocaOptions(ARCHIVE, "", BEGINNING_OF_TEXT, "", DEBUG)

class CocaArchiverTest: CocaArchiver(createOptions()) {
    private fun CO(filePath: String, pos: IntRange, lines: IntRange, commentText: String) = CommentOccurrence(filePath, pos, lines, commentText)
    private fun log(message: String) = println("CocaArchiverTest> $message")
    private val printedLines = mutableListOf<String>()

    init {
        overwrittenRunId = DEFAULT_RUN_ID
    }

    private class BasicTestBed{
        val fileSystem = MemoryFs { time }

        val projectDir = (fileSystem / "/myProjects/projectA").mkdirs()
        val sourceDir = (projectDir / "src").mkdirs()
        val archiveDir = (fileSystem / "/myArchives/arch_prj_A").mkdirs()

        val sourceBasePackageDir = (sourceDir / "base").mkdirs()
        val sourceOtherPackageDir = (sourceDir / "other").mkdirs()

        val sourceFileA1 = (sourceBasePackageDir / "MyClassA.kt").writeString(getSourceFileTextSingleComment("base", "MyClassA"))
        val sourceFileA2 = (sourceOtherPackageDir / "MyClassA.java").writeString(getSourceFileTextSingleComment("other", "MyClassA"))
        val sourceFileB = (sourceBasePackageDir / "MyClassB.kt").writeString(getSourceFileTextDoubleComments("base", "MyClassB"))
        val sourceFileCommentsOnly = (sourceBasePackageDir / "OldClass.kt").writeString(getCommentedOutSourceFileText())

        val config = CocaConfigReader().readConfig((sourceDir / "config.yaml").writeString(createConfigText()))

        var time = 0L

        val comments = buildList {
            add(CommentOccurrence(sourceFileA1.absolutePath, 0..19, 1..1, "//comment\"to\"remove\n"))
            add(CommentOccurrence(sourceFileA2.absolutePath, 0..19, 1..1, "//comment\"to\"remove\n"))
            add(CommentOccurrence(sourceFileB.absolutePath, 0..19, 1..1, "//comment to remove\n"))
            add(CommentOccurrence(sourceFileB.absolutePath, 39..65, 3..3, "//second comment to remove"))
            add(CommentOccurrence(sourceFileCommentsOnly.absolutePath, 0..19, 1..1, "//comment to remove\n"))
        }

        private fun getSourceFileTextSingleComment(packageName: String, className: String): String {
            return """
            //comment"to"remove
            package §1§
            data class A(val x: Int)
            class §2§{
                val x = 5
                val y = "6"
            }
            """.trimIndent().replace("§1§", packageName).replace("§2§", className)
        }

        private fun getSourceFileTextDoubleComments(packageName: String, className: String): String {
            return """
            //comment to remove
            //:comment to keep
            //second comment to remove
            package §1§
            data class A(val x: Int)
            class §2§{
                val x = 5
                val y = 6
            }
            """.trimIndent().replace("§1§", packageName).replace("§2§", className)
        }

        private fun getCommentedOutSourceFileText() = "//comment to remove"

        private fun createConfigText(): String {
            return """
        coca config version: 1
        archive root path: /myArchives/arch_prj_A
        source root path: /myProjects/projectA
        include paths: /src/*
        exclude paths:
        include file endings:
         - *.kt
         - *.java
        block comments to remove: /*...*/
        block comments to keep: /**...*/
        line comments to remove: //
        line comments to keep:
         - //:
         - //*
        """.trimIndent()
        }
    }

    @Test
    fun test_readLineBackwards_bulk(){
        assertEquals("abc", readLineBackwardsUntil("abc", 3))
        assertEquals("ab", readLineBackwardsUntil("abc", 2))
        assertEquals("a", readLineBackwardsUntil("abc", 1))
        assertEquals("", readLineBackwardsUntil("abc", 0))
        assertEquals("abc", readLineBackwardsUntil("abc\nxyz", 3))
        assertEquals("", readLineBackwardsUntil("abc\nxyz", 4))
        assertEquals("x", readLineBackwardsUntil("abc\nxyz", 5))
        assertEquals("xy", readLineBackwardsUntil("abc\nxyz", 6))
        assertEquals("xyz", readLineBackwardsUntil("abc\nxyz", 7))
        assertEquals("", readLineBackwardsUntil("", 0))
    }

    @Test
    fun test_readLineForward_bulk(){
        assertEquals("bc", readLineForwardUntil("abc", 0))
        assertEquals("c", readLineForwardUntil("abc", 1))
        assertEquals("", readLineForwardUntil("abc", 2))
        assertEquals("", readLineForwardUntil("abc", 3))
        assertEquals("bc", readLineForwardUntil("abc\nxyz", 0))
        assertEquals("c", readLineForwardUntil("abc\nxyz", 1))
        assertEquals("", readLineForwardUntil("abc\nxyz", 2))
        assertEquals("", readLineForwardUntil("abc\nxyz", 3))
        assertEquals("yz", readLineForwardUntil("abc\nxyz", 4))
        assertEquals("z", readLineForwardUntil("abc\nxyz", 5))
        assertEquals("", readLineForwardUntil("abc\nxyz", 6))

        assertEquals("", readLineForwardUntil("abc\nxyz\n123", 3))
        assertEquals("yz", readLineForwardUntil("abc\nxyz\n123", 4))
        assertEquals("z", readLineForwardUntil("abc\nxyz\n123", 5))
        assertEquals("", readLineForwardUntil("abc\nxyz\n123", 6))

        assertEquals("", readLineBackwardsUntil("", 0))
    }

    @Test
    fun test_removeCommentFromText_bulk() {
        //: comment is entire file
        assertEquals("", removeCommentFromText("//hi", CO("", 0..3, 1..1, "//hi")))
        assertEquals("", removeCommentFromText("//hi\n", CO("", 0..4, 1..1, "//hi\n")))
        assertEquals("", removeCommentFromText("/*hi\nabc*/", CO("", 0..10, 1..2, "/*hi\nabc*/")))
        assertEquals("", removeCommentFromText("/*hi\nabc*/\n", CO("", 0..11, 1..2, "/*hi\nabc*/\n")))

        //: single line comments
        assertEquals("abc\nbla", removeCommentFromText("abc\n  //xy\nbla", CO("", 6..10, 1..1, "//xy\n")))
        assertEquals("abc\nbla", removeCommentFromText("abc\n//xy\nbla", CO("", 4..8, 1..1, "//xy\n")))
        assertEquals("abc\n", removeCommentFromText("abc\n  //xy\n", CO("", 6..11, 1..1, "//xy\n")))
        assertEquals("abc\n", removeCommentFromText("abc\n//xy\n", CO("", 4..9, 1..1, "//xy\n")))
        assertEquals("abc\n", removeCommentFromText("//xy\nabc\n", CO("", 0..4, 1..1, "//xy\n")))

        //: multi line comments
        assertEquals("abc\nxyz", removeCommentFromText("abc/*c1\n c2*/xyz", CO("", 3..12, 1..2, "/*c1\n c2*/")))
        assertEquals("abc\nxyz", removeCommentFromText("abc\n/*c1\n c2*/\nxyz", CO("", 4..14, 1..2, "/*c1\n c2*/")))
        assertEquals("abc\n xyz", removeCommentFromText("abc /*c1\n c2*/ xyz", CO("", 4..13, 1..2, "/*c1\n c2*/")))
        assertEquals("abc\nxyz", removeCommentFromText("abc\n /*c1\n c2*/ \nxyz", CO("", 5..16, 1..2, "/*c1\n c2*/")))
        assertEquals("abc\nxyz", removeCommentFromText("abc\n /*c1\n c2*/xyz", CO("", 5..14, 1..2, "/*c1\n c2*/")))
        assertEquals("abc\nxyz", removeCommentFromText("abc\n/*c1\n c2*/ \nxyz", CO("", 4..15, 1..2, "/*c1\n c2*/")))
        assertEquals("xyz", removeCommentFromText("/*c1\n c2*/\nxyz", CO("", 0..10, 1..2, "/*c1\n c2*/")))
        assertEquals("xyz", removeCommentFromText("/*c1\n c2*/xyz", CO("", 0..9, 1..2, "/*c1\n c2*/")))
        assertEquals("abc", removeCommentFromText("abc/*c1\n c2*/", CO("", 3..14, 1..2, "/*c1\n c2*/")))
        assertEquals("abc\n", removeCommentFromText("abc\n/*c1\n c2*/", CO("", 4..15, 1..2, "/*c1\n c2*/")))
    }

    @Test
    fun test_removeCommentFromText_realisticCaseMultiline_bulk(){
        val input = toTextAndPosMap("""
            package abc
            %1%/* comment to 
             * remove 2
             */%2%
            class MyClass{
            """.trimIndent())
        val expectedResult = """
            package abc
            class MyClass{
            """.trimIndent()
        val comment = CO("", input.posRange(1, 2), 2..3, input.textBetween(1, 2))
        assertEquals(expectedResult, removeCommentFromText(input.text, comment))
    }

    @Test
    fun test_removeCommentFromText_realisticCaseMultilineTextBefore(){
        val input = toTextAndPosMap("""
            package abc
            data class X(val a: Int) %1%/* comment to 
             * remove 2
             */%2%
            class MyClass{
            """.trimIndent())
        val expectedResult = """
            package abc
            data class X(val a: Int)
            class MyClass{
            """.trimIndent()
        val comment = CO("", input.posRange(1, 2), 2..3, input.textBetween(1, 2))
        assertEquals(expectedResult, removeCommentFromText(input.text, comment))
    }

    @Test
    fun test_removeCommentFromText_realisticCaseMultilineTextBeforeAndAfter(){
        val input = toTextAndPosMap("""
            package abc
            data class X(val a: Int) %1%/* comment to 
             * remove 2
             */%2%data class Y(val b: Int)
            class MyClass{
            """.trimIndent())
        val expectedResult = """
            package abc
            data class X(val a: Int)
            data class Y(val b: Int)
            class MyClass{
            """.trimIndent()
        val comment = CO("", input.posRange(1, 2), 2..3, input.textBetween(1, 2))
        assertEquals(expectedResult, removeCommentFromText(input.text, comment))
    }

    @Test
    fun test_removeCommentFromText_realisticCaseMultilineComment1LineTextBeforeAndAfter(){
        val input = toTextAndPosMap("""
            package abc
            data class X(val a: Int) %1%/* comment to remove 2 */%2% data class Y(val b: Int)
            class MyClass{
            """.trimIndent())
        val expectedResult = """
            package abc
            data class X(val a: Int) data class Y(val b: Int)
            class MyClass{
            """.trimIndent()
        val comment = CO("", input.posRange(1, 2), 1..1, input.textBetween(1, 2))
        assertEquals(expectedResult, removeCommentFromText(input.text, comment))
    }

    @Test
    fun test_removeCommentFromText_realisticCaseMultilineTextAfter(){
        val input = toTextAndPosMap("""
            package abc
            %1%/* comment to 
             * remove 2
             */%2%data class X(val a: Int) 
            class MyClass{
            """.trimIndent())
        val expectedResult = """
            package abc
            data class X(val a: Int) 
            class MyClass{
            """.trimIndent()
        val comment = CO("", input.posRange(1, 2), 2..3, input.textBetween(1, 2))
        assertEquals(expectedResult, removeCommentFromText(input.text, comment))
    }

    @Test
    fun test_removeCommentFromText_realisticCase_KeepIndentInNextLine(){
        val input = toTextAndPosMap("""
            class MyClass{
                val x = 5
                %1%/*
                 * commented out code
                 */%2%
                val y = 6
            }
            """.trimIndent())
        val expectedResult = """
            class MyClass{
                val x = 5
                val y = 6
            }
            """.trimIndent()
        val comment = CO("", input.posRange(1, 2), 3..5, input.textBetween(1, 2))
        assertEquals(expectedResult, removeCommentFromText(input.text, comment))
    }

    @Test
    fun test_removeCommentFromText_realisticCaseSingleLine(){
        val input = toTextAndPosMap("""
            package abc
            %1%// comment to remove
            %2%class MyClass{
            """.trimIndent())
        val expectedResult = """
            package abc
            class MyClass{
            """.trimIndent()
        val comment = CO("", input.posRange(1, 2), 2..2, input.textBetween(1, 2))
        assertEquals(expectedResult, removeCommentFromText(input.text, comment))
    }

    @Test
    fun test_removeCommentFromText_realisticCaseSingleLineTextBefore(){
        val input = toTextAndPosMap("""
            package abc
            data class X(val a: Int) %1%// comment to remove
            %2%class MyClass{
            """.trimIndent())
        val expectedResult = """
            package abc
            data class X(val a: Int)
            class MyClass{
            """.trimIndent()
        val comment = CO("", input.posRange(1, 2), 2..2, input.textBetween(1, 2))
        assertEquals(expectedResult, removeCommentFromText(input.text, comment))
    }

    @Test
    fun test_removeCommentsFromText(){
        val input = toTextAndPosMap("""
            %1%// comment to remove 1
            %2%package abc
            %3%/* comment to
             * remove 2
             */%4%data class A(val x: Int)
            class MyClass{
                val x = 5
                %5%/*
                 * commented out code
                 */%6%
                val y = 6
            }
        """.trimIndent())
        val expectedResult = """
            package abc
            data class A(val x: Int)
            class MyClass{
                val x = 5
                val y = 6
            }
        """.trimIndent()

        val comments = mutableListOf<CommentOccurrence>()
        comments += CO("", input.posRange(1, 2), 1..1, input.textBetween(1, 2))
        comments += CO("", input.posRange(3, 4), 3..5, input.textBetween(3, 4))
        comments += CO("", input.posRange(5, 6), 8..10, input.textBetween(5, 6))

        val actualResult = removeCommentsFromText(input.text, comments)
        assertEquals(expectedResult, actualResult)
    }

    @Test
    fun test_removeCommentsFromText_singleCharAfterComment(){
        val input = """
            import de.bright_side.coca.CocaConfigReader
            import de.bright_side.coca.CocaOptionsReader
            
            fun main(args: Array<String>) {
            //    val fileSystem: FSFFileSystem = OkFileSystem()
            //
            //    val options = CocaOptionsReader().readOptions(args.toList())
            //    val config = CocaConfigReader().readConfig(fileSystem / options.configFilePath)
            //    CocoProcessor().process(options, config, fileSystem)
            }""".trimIndent()

        val commentText = """
            //    val fileSystem: FSFFileSystem = OkFileSystem()
            //
            //    val options = CocaOptionsReader().readOptions(args.toList())
            //    val config = CocaConfigReader().readConfig(fileSystem / options.configFilePath)
            //    CocoProcessor().process(options, config, fileSystem)
        """.trimIndent() + "\n"

        val expectedResult = """
            import de.bright_side.coca.CocaConfigReader
            import de.bright_side.coca.CocaOptionsReader
            
            fun main(args: Array<String>) {
            }""".trimIndent()

        val comments = mutableListOf<CommentOccurrence>()
        comments += CO("/myProject/src/main/java/Main.kt", 122..389, 5..9, commentText)

        val actualResult = removeCommentsFromText(input, comments)
        assertEquals(expectedResult, actualResult)
    }

    @Test
    fun test_copyToArchive_singleFile() {
        val testBed = BasicTestBed()
        val rootDir = (testBed.fileSystem / "/")
        val fsBefore = rootDir.listDirAsString().trimIndent()
        val expectedBefore = """
            <D> myArchives
               <D> arch_prj_A
            <D> myProjects
               <D> projectA
                  <D> src
                     <D> base
                        <F> MyClassA.kt
                        <F> MyClassB.kt
                        <F> OldClass.kt
                     <D> other
                        <F> MyClassA.java
                     <F> config.yaml
        """.trimIndent()
        assertEquals(expectedBefore, fsBefore)

        copyToArchive(testBed.config, DEFAULT_RUN_ID, testBed.sourceFileA1)
        val fsAfter = rootDir.listDirAsString().trimIndent()
        val expectedAfter = """
            <D> myArchives
               <D> arch_prj_A
                  <D> code-archive
                     <D> src
                        <D> base
                           <D> %DEFAULT_RUN_ID%
                              <F> MyClassA.kt
            <D> myProjects
               <D> projectA
                  <D> src
                     <D> base
                        <F> MyClassA.kt
                        <F> MyClassB.kt
                        <F> OldClass.kt
                     <D> other
                        <F> MyClassA.java
                     <F> config.yaml
        """.trimIndent().replace("%DEFAULT_RUN_ID%", DEFAULT_RUN_ID)
        assertEquals(expectedAfter, fsAfter)
    }

    @Test
    fun test_createFilesToCommentsMap_normal(){
        val testBed = BasicTestBed()
        val actual = createFilesToCommentsMap(testBed.fileSystem, testBed.comments)

        assertEquals(4, actual.keys.size)
        val allPaths = actual.keys.map { it.absolutePath }
        assertTrue(allPaths.contains(testBed.sourceFileA1.absolutePath))
        assertTrue(allPaths.contains(testBed.sourceFileA2.absolutePath))
        assertTrue(allPaths.contains(testBed.sourceFileB.absolutePath))
        assertTrue(allPaths.contains(testBed.sourceFileCommentsOnly.absolutePath))

        val allPathsList = actual.keys.map { it.absolutePath }.toList()
        assertEquals(4, allPathsList.size)
        assertEquals(testBed.sourceFileA1.absolutePath, allPathsList[0])
        assertEquals(testBed.sourceFileA2.absolutePath, allPathsList[1])
        assertEquals(testBed.sourceFileB.absolutePath, allPathsList[2])
        assertEquals(testBed.sourceFileCommentsOnly.absolutePath, allPathsList[3])
    }

    @Test
    fun test_copyToArchive_multipleFiles() {
        val testBed = BasicTestBed()
        val rootDir = (testBed.fileSystem / "/")
        val fsBefore = rootDir.listDirAsString().trimIndent()
        val expectedBefore = """
            <D> myArchives
               <D> arch_prj_A
            <D> myProjects
               <D> projectA
                  <D> src
                     <D> base
                        <F> MyClassA.kt
                        <F> MyClassB.kt
                        <F> OldClass.kt
                     <D> other
                        <F> MyClassA.java
                     <F> config.yaml
        """.trimIndent()
        assertEquals(expectedBefore, fsBefore)

        copyToArchive(testBed.config, DEFAULT_RUN_ID, testBed.sourceFileA1)
        copyToArchive(testBed.config, DEFAULT_RUN_ID, testBed.sourceFileA2)
        copyToArchive(testBed.config, DEFAULT_RUN_ID, testBed.sourceFileB)
        copyToArchive(testBed.config, DEFAULT_RUN_ID, testBed.sourceFileCommentsOnly)
        val fsAfter = rootDir.listDirAsString().trimIndent()
        val expectedAfter = """
            <D> myArchives
               <D> arch_prj_A
                  <D> code-archive
                     <D> src
                        <D> base
                           <D> %DEFAULT_RUN_ID%
                              <F> MyClassA.kt
                              <F> MyClassB.kt
                              <F> OldClass.kt
                        <D> other
                           <D> %DEFAULT_RUN_ID%
                              <F> MyClassA.java
            <D> myProjects
               <D> projectA
                  <D> src
                     <D> base
                        <F> MyClassA.kt
                        <F> MyClassB.kt
                        <F> OldClass.kt
                     <D> other
                        <F> MyClassA.java
                     <F> config.yaml
        """.trimIndent().replace("%DEFAULT_RUN_ID%", DEFAULT_RUN_ID)
        assertEquals(expectedAfter, fsAfter)

        copyToArchive(testBed.config, SECOND_RUN_ID, testBed.sourceFileA1)
        copyToArchive(testBed.config, SECOND_RUN_ID, testBed.sourceFileA2)
        copyToArchive(testBed.config, SECOND_RUN_ID, testBed.sourceFileCommentsOnly)
        val fsAfter2 = rootDir.listDirAsString().trimIndent()
        val expectedAfter2 = """
            <D> myArchives
               <D> arch_prj_A
                  <D> code-archive
                     <D> src
                        <D> base
                           <D> %DEFAULT_RUN_ID%
                              <F> MyClassA.kt
                              <F> MyClassB.kt
                              <F> OldClass.kt
                           <D> %SECOND_RUN_ID%
                              <F> MyClassA.kt
                              <F> OldClass.kt
                        <D> other
                           <D> %DEFAULT_RUN_ID%
                              <F> MyClassA.java
                           <D> %SECOND_RUN_ID%
                              <F> MyClassA.java
            <D> myProjects
               <D> projectA
                  <D> src
                     <D> base
                        <F> MyClassA.kt
                        <F> MyClassB.kt
                        <F> OldClass.kt
                     <D> other
                        <F> MyClassA.java
                     <F> config.yaml
        """.trimIndent().replace("%DEFAULT_RUN_ID%", DEFAULT_RUN_ID).replace("%SECOND_RUN_ID%", SECOND_RUN_ID)
        assertEquals(expectedAfter2, fsAfter2)
    }

    @Test
    fun test_addFileToIndex_singleFile() {
        val testBed = BasicTestBed()
        val rootDir = (testBed.fileSystem / "/")
        val fsBefore = rootDir.listDirAsString().trimIndent()
        val expectedBefore = """
            <D> myArchives
               <D> arch_prj_A
            <D> myProjects
               <D> projectA
                  <D> src
                     <D> base
                        <F> MyClassA.kt
                        <F> MyClassB.kt
                        <F> OldClass.kt
                     <D> other
                        <F> MyClassA.java
                     <F> config.yaml
        """.trimIndent()
        assertEquals(expectedBefore, fsBefore)

        addFileToIndex(testBed.config, DEFAULT_RUN_ID, testBed.sourceFileA1, testBed.comments.filter { it.filePath == testBed.sourceFileA1.absolutePath }, "x")
        val fsAfter = rootDir.listDirAsString().trimIndent()
        val expectedAfter = """
            <D> myArchives
               <D> arch_prj_A
                  <D> operation-index
                     <F> %DEFAULT_RUN_ID%.dat
            <D> myProjects
               <D> projectA
                  <D> src
                     <D> base
                        <F> MyClassA.kt
                        <F> MyClassB.kt
                        <F> OldClass.kt
                     <D> other
                        <F> MyClassA.java
                     <F> config.yaml
        """.trimIndent().replace("%DEFAULT_RUN_ID%", DEFAULT_RUN_ID)
        assertEquals(expectedAfter, fsAfter)

        assertEquals("src/base/MyClassA.kt:1 comment removed\n", (testBed.fileSystem / "/myArchives/arch_prj_A/operation-index/$DEFAULT_RUN_ID.dat").readString())
    }

    @Test
    fun test_addFileToIndex_multipleFiles() {
        val testBed = BasicTestBed()
        val rootDir = (testBed.fileSystem / "/")
        val fsBefore = rootDir.listDirAsString().trimIndent()
        val expectedBefore = """
            <D> myArchives
               <D> arch_prj_A
            <D> myProjects
               <D> projectA
                  <D> src
                     <D> base
                        <F> MyClassA.kt
                        <F> MyClassB.kt
                        <F> OldClass.kt
                     <D> other
                        <F> MyClassA.java
                     <F> config.yaml
        """.trimIndent()
        assertEquals(expectedBefore, fsBefore)

        addFileToIndex(testBed.config, DEFAULT_RUN_ID, testBed.sourceFileA1, testBed.comments.filter { it.filePath == testBed.sourceFileA1.absolutePath }, "x")
        addFileToIndex(testBed.config, DEFAULT_RUN_ID, testBed.sourceFileB, testBed.comments.filter { it.filePath == testBed.sourceFileB.absolutePath }, "x")
        addFileToIndex(testBed.config, DEFAULT_RUN_ID, testBed.sourceFileA2, testBed.comments.filter { it.filePath == testBed.sourceFileA2.absolutePath }, "x")
        val fsAfter = rootDir.listDirAsString().trimIndent()
        val expectedAfter = """
            <D> myArchives
               <D> arch_prj_A
                  <D> operation-index
                     <F> %DEFAULT_RUN_ID%.dat
            <D> myProjects
               <D> projectA
                  <D> src
                     <D> base
                        <F> MyClassA.kt
                        <F> MyClassB.kt
                        <F> OldClass.kt
                     <D> other
                        <F> MyClassA.java
                     <F> config.yaml
        """.trimIndent().replace("%DEFAULT_RUN_ID%", DEFAULT_RUN_ID)
        assertEquals(expectedAfter, fsAfter)

        val expected = """
            src/base/MyClassA.kt:1 comment removed
            src/base/MyClassB.kt:2 comments removed
            src/other/MyClassA.java:1 comment removed
        """.trimIndent()
        assertEquals(expected, (testBed.fileSystem / "/myArchives/arch_prj_A/operation-index/$DEFAULT_RUN_ID.dat").readString().trimIndent())
    }

    @Test
    fun test_addFileToIndex_multipleRuns() {
        val testBed = BasicTestBed()
        val rootDir = (testBed.fileSystem / "/")
        val fsBefore = rootDir.listDirAsString().trimIndent()
        val expectedBefore = """
            <D> myArchives
               <D> arch_prj_A
            <D> myProjects
               <D> projectA
                  <D> src
                     <D> base
                        <F> MyClassA.kt
                        <F> MyClassB.kt
                        <F> OldClass.kt
                     <D> other
                        <F> MyClassA.java
                     <F> config.yaml
        """.trimIndent()
        assertEquals(expectedBefore, fsBefore)

        addFileToIndex(testBed.config, DEFAULT_RUN_ID, testBed.sourceFileA1, testBed.comments.filter { it.filePath == testBed.sourceFileA1.absolutePath }, "x")
        addFileToIndex(testBed.config, DEFAULT_RUN_ID, testBed.sourceFileB, testBed.comments.filter { it.filePath == testBed.sourceFileB.absolutePath }, "x")
        addFileToIndex(testBed.config, DEFAULT_RUN_ID, testBed.sourceFileA2, testBed.comments.filter { it.filePath == testBed.sourceFileA2.absolutePath }, "x")

        addFileToIndex(testBed.config, SECOND_RUN_ID, testBed.sourceFileA2, testBed.comments.filter { it.filePath == testBed.sourceFileA2.absolutePath }, "x")
        addFileToIndex(testBed.config, SECOND_RUN_ID, testBed.sourceFileCommentsOnly, testBed.comments.filter { it.filePath == testBed.sourceFileCommentsOnly.absolutePath }, "")
        val fsAfter = rootDir.listDirAsString().trimIndent()
        val expectedAfter = """
            <D> myArchives
               <D> arch_prj_A
                  <D> operation-index
                     <F> %DEFAULT_RUN_ID%.dat
                     <F> %SECOND_RUN_ID%.dat
            <D> myProjects
               <D> projectA
                  <D> src
                     <D> base
                        <F> MyClassA.kt
                        <F> MyClassB.kt
                        <F> OldClass.kt
                     <D> other
                        <F> MyClassA.java
                     <F> config.yaml
        """.trimIndent().replace("%DEFAULT_RUN_ID%", DEFAULT_RUN_ID).replace("%SECOND_RUN_ID%", SECOND_RUN_ID)
        assertEquals(expectedAfter, fsAfter)

        val expectedRun1 = """
            src/base/MyClassA.kt:1 comment removed
            src/base/MyClassB.kt:2 comments removed
            src/other/MyClassA.java:1 comment removed
        """.trimIndent()
        assertEquals(expectedRun1, (testBed.fileSystem / "/myArchives/arch_prj_A/operation-index/$DEFAULT_RUN_ID.dat").readString().trimIndent())
        val expectedRun2 = """
            src/other/MyClassA.java:1 comment removed
            src/base/OldClass.kt:file removed
        """.trimIndent()
        assertEquals(expectedRun2, (testBed.fileSystem / "/myArchives/arch_prj_A/operation-index/$SECOND_RUN_ID.dat").readString().trimIndent())
    }

    @Test
    fun test_addSummaryToArchive_singleFile() {
        val testBed = BasicTestBed()
        val rootDir = (testBed.fileSystem / "/")
        val fsBefore = rootDir.listDirAsString().trimIndent()
        val expectedBefore = """
            <D> myArchives
               <D> arch_prj_A
            <D> myProjects
               <D> projectA
                  <D> src
                     <D> base
                        <F> MyClassA.kt
                        <F> MyClassB.kt
                        <F> OldClass.kt
                     <D> other
                        <F> MyClassA.java
                     <F> config.yaml
        """.trimIndent()
        assertEquals(expectedBefore, fsBefore)

        val sourceFileAComments = testBed.comments.filter { it.filePath == testBed.sourceFileA1.absolutePath }
        log("test_addSummaryToArchive_singleFile: testBed.comments = ${testBed.comments}")
        log("test_addSummaryToArchive_singleFile: sourceFileAComments = $sourceFileAComments")
        val sourceFileARemainingText = removeCommentsFromText(testBed.sourceFileA1.readString(), sourceFileAComments)

        val expectedSourceFileARemainingText = """
            package base
            data class A(val x: Int)
            class MyClassA{
                val x = 5
                val y = "6"
            }
        """.trimIndent()
        assertEquals(expectedSourceFileARemainingText, sourceFileARemainingText)

        addSummaryToArchive(testBed.config, DEFAULT_RUN_ID, testBed.sourceFileA1, sourceFileAComments)
        val fsAfter = rootDir.listDirAsString().trimIndent()
        val expectedAfter = """
            <D> myArchives
               <D> arch_prj_A
                  <D> summary
                     <D> %DEFAULT_RUN_ID%
                        <F> MyClassA.json
            <D> myProjects
               <D> projectA
                  <D> src
                     <D> base
                        <F> MyClassA.kt
                        <F> MyClassB.kt
                        <F> OldClass.kt
                     <D> other
                        <F> MyClassA.java
                     <F> config.yaml
        """.trimIndent().replace("%DEFAULT_RUN_ID%", DEFAULT_RUN_ID)
        assertEquals(expectedAfter, fsAfter)

        val expectedJson = """
            {"relativePath":"src/base/MyClassA.kt",
            "items":[
            {"filePath":"/myProjects/projectA/src/base/MyClassA.kt"
            ,"pos":{"first":0,"last":19}
            ,"lines":{"first":1,"last":1}
            ,"commentText":"//comment\"to\"remove\n"}
            ]
            }
        """.trimIndent().replace("\n", "")
        val actualJson = (testBed.fileSystem / "/myArchives/arch_prj_A/summary/$DEFAULT_RUN_ID/MyClassA.json").readString()
        log("test_addSummaryToArchive_singleFile: >>$actualJson<<")
        assertEquals(expectedJson, actualJson)
    }

    @Test
    fun test_addSummaryToArchive_multipleFiles() {
        val testBed = BasicTestBed()
        val rootDir = (testBed.fileSystem / "/")
        val fsBefore = rootDir.listDirAsString().trimIndent()
        val expectedBefore = """
            <D> myArchives
               <D> arch_prj_A
            <D> myProjects
               <D> projectA
                  <D> src
                     <D> base
                        <F> MyClassA.kt
                        <F> MyClassB.kt
                        <F> OldClass.kt
                     <D> other
                        <F> MyClassA.java
                     <F> config.yaml
        """.trimIndent()
        assertEquals(expectedBefore, fsBefore)

        val sourceFileA1Comments = testBed.comments.filter { it.filePath == testBed.sourceFileA1.absolutePath }
        val sourceFileA1RemainingText = removeCommentsFromText(testBed.sourceFileA1.readString(), sourceFileA1Comments)
        addSummaryToArchive(testBed.config, DEFAULT_RUN_ID, testBed.sourceFileA1, sourceFileA1Comments)

        val sourceFileA2Comments = testBed.comments.filter { it.filePath == testBed.sourceFileA2.absolutePath }
        val sourceFileA2RemainingText = removeCommentsFromText(testBed.sourceFileA2.readString(), sourceFileA2Comments)
        addSummaryToArchive(testBed.config, DEFAULT_RUN_ID, testBed.sourceFileA2, sourceFileA2Comments)

        val fsAfter = rootDir.listDirAsString().trimIndent()
        val expectedAfter = """
            <D> myArchives
               <D> arch_prj_A
                  <D> summary
                     <D> %DEFAULT_RUN_ID%
                        <F> MyClassA-1.json
                        <F> MyClassA.json
            <D> myProjects
               <D> projectA
                  <D> src
                     <D> base
                        <F> MyClassA.kt
                        <F> MyClassB.kt
                        <F> OldClass.kt
                     <D> other
                        <F> MyClassA.java
                     <F> config.yaml
        """.trimIndent().replace("%DEFAULT_RUN_ID%", DEFAULT_RUN_ID)
        assertEquals(expectedAfter, fsAfter)

        val expectedJsonA1 = """
            {"relativePath":"src/base/MyClassA.kt",
            "items":[
            {"filePath":"/myProjects/projectA/src/base/MyClassA.kt"
            ,"pos":{"first":0,"last":19}
            ,"lines":{"first":1,"last":1}
            ,"commentText":"//comment\"to\"remove\n"}
            ]
            }
        """.trimIndent().replace("\n", "")
        val actualJsonA1 = (testBed.fileSystem / "/myArchives/arch_prj_A/summary/$DEFAULT_RUN_ID/MyClassA.json").readString()
        log("test_addSummaryToArchive_multipleFiles A1: >>$actualJsonA1<<")
        assertEquals(expectedJsonA1, actualJsonA1)

        val expectedJsonA2 = """
            {"relativePath":"src/other/MyClassA.java",
            "items":[
            {"filePath":"/myProjects/projectA/src/other/MyClassA.java"
            ,"pos":{"first":0,"last":19}
            ,"lines":{"first":1,"last":1}
            ,"commentText":"//comment\"to\"remove\n"}
            ]
            }
        """.trimIndent().replace("\n", "")
        val actualJsonA2 = (testBed.fileSystem / "/myArchives/arch_prj_A/summary/$DEFAULT_RUN_ID/MyClassA-1.json").readString()
        log("test_addSummaryToArchive_multipleFiles A2: >>$actualJsonA2<<")
        assertEquals(expectedJsonA2, actualJsonA2)
    }

    @Test
    fun test_addSummaryToArchive_multipleRuns() {
        val testBed = BasicTestBed()
        val rootDir = (testBed.fileSystem / "/")
        val fsBefore = rootDir.listDirAsString().trimIndent()
        val expectedBefore = """
            <D> myArchives
               <D> arch_prj_A
            <D> myProjects
               <D> projectA
                  <D> src
                     <D> base
                        <F> MyClassA.kt
                        <F> MyClassB.kt
                        <F> OldClass.kt
                     <D> other
                        <F> MyClassA.java
                     <F> config.yaml
        """.trimIndent()
        assertEquals(expectedBefore, fsBefore)

        val sourceFileA1Comments = testBed.comments.filter { it.filePath == testBed.sourceFileA1.absolutePath }
        val sourceFileA1RemainingText = removeCommentsFromText(testBed.sourceFileA1.readString(), sourceFileA1Comments)
        addSummaryToArchive(testBed.config, DEFAULT_RUN_ID, testBed.sourceFileA1, sourceFileA1Comments)

        val sourceFileA2Comments = testBed.comments.filter { it.filePath == testBed.sourceFileA2.absolutePath }
        val sourceFileA2RemainingText = removeCommentsFromText(testBed.sourceFileA2.readString(), sourceFileA2Comments)
        addSummaryToArchive(testBed.config, DEFAULT_RUN_ID, testBed.sourceFileA2, sourceFileA2Comments)

        val sourceFileBComments = testBed.comments.filter { it.filePath == testBed.sourceFileB.absolutePath }
        val sourceFileBRemainingText = removeCommentsFromText(testBed.sourceFileB.readString(), sourceFileBComments)
        addSummaryToArchive(testBed.config, SECOND_RUN_ID, testBed.sourceFileB, sourceFileBComments)

        val fsAfter = rootDir.listDirAsString().trimIndent()
        val expectedAfter = """
            <D> myArchives
               <D> arch_prj_A
                  <D> summary
                     <D> %DEFAULT_RUN_ID%
                        <F> MyClassA-1.json
                        <F> MyClassA.json
                     <D> %SECOND_RUN_ID%
                        <F> MyClassB.json
            <D> myProjects
               <D> projectA
                  <D> src
                     <D> base
                        <F> MyClassA.kt
                        <F> MyClassB.kt
                        <F> OldClass.kt
                     <D> other
                        <F> MyClassA.java
                     <F> config.yaml
        """.trimIndent().replace("%DEFAULT_RUN_ID%", DEFAULT_RUN_ID).replace("%SECOND_RUN_ID%", SECOND_RUN_ID)
        assertEquals(expectedAfter, fsAfter)

        val expectedJsonA1 = """
            {"relativePath":"src/base/MyClassA.kt",
            "items":[
            {"filePath":"/myProjects/projectA/src/base/MyClassA.kt"
            ,"pos":{"first":0,"last":19}
            ,"lines":{"first":1,"last":1}
            ,"commentText":"//comment\"to\"remove\n"}
            ]
            }
        """.trimIndent().replace("\n", "")
        val actualJsonA1 = (testBed.fileSystem / "/myArchives/arch_prj_A/summary/$DEFAULT_RUN_ID/MyClassA.json").readString()
        log("test_addSummaryToArchive_multipleRuns A1: >>$actualJsonA1<<")
        assertEquals(expectedJsonA1, actualJsonA1)

        val expectedJsonA2 = """
            {"relativePath":"src/other/MyClassA.java",
            "items":[
            {"filePath":"/myProjects/projectA/src/other/MyClassA.java"
            ,"pos":{"first":0,"last":19}
            ,"lines":{"first":1,"last":1}
            ,"commentText":"//comment\"to\"remove\n"}
            ]
            }
        """.trimIndent().replace("\n", "")
        val actualJsonA2 = (testBed.fileSystem / "/myArchives/arch_prj_A/summary/$DEFAULT_RUN_ID/MyClassA-1.json").readString()
        log("test_addSummaryToArchive_multipleRuns A2: >>$actualJsonA2<<")
        assertEquals(expectedJsonA2, actualJsonA2)

        val expectedJsonB = """
            {"relativePath":"src/base/MyClassB.kt",
            "items":[
            {"filePath":"/myProjects/projectA/src/base/MyClassB.kt"
            ,"pos":{"first":0,"last":19}
            ,"lines":{"first":1,"last":1}
            ,"commentText":"//comment to remove\n"}
            ,{"filePath":"/myProjects/projectA/src/base/MyClassB.kt"
            ,"pos":{"first":39,"last":65}
            ,"lines":{"first":3,"last":3}
            ,"commentText":"//second comment to remove"}
            ]
            }
        """.trimIndent().replace("\n", "")
        val actualJsonB = (testBed.fileSystem / "/myArchives/arch_prj_A/summary/$SECOND_RUN_ID/MyClassB.json").readString()
        log("test_addSummaryToArchive_multipleRuns B: >>$actualJsonB<<")
        assertEquals(expectedJsonB, actualJsonB)
    }

    @Test
    fun test_archiveComments_normalCase() {
        val testBed = BasicTestBed()
        val rootDir = (testBed.fileSystem / "/")
        val fsBefore = rootDir.listDirAsString().trimIndent()
        val expectedBefore = """
            <D> myArchives
               <D> arch_prj_A
            <D> myProjects
               <D> projectA
                  <D> src
                     <D> base
                        <F> MyClassA.kt
                        <F> MyClassB.kt
                        <F> OldClass.kt
                     <D> other
                        <F> MyClassA.java
                     <F> config.yaml
        """.trimIndent()
        assertEquals(expectedBefore, fsBefore)
        val expectedSourceFileAArchivedText = testBed.sourceFileA1.readString()

        archiveComments(testBed.fileSystem, testBed.config, testBed.comments) { printedLines.add(it) }

        val fsAfter = rootDir.listDirAsString().trimIndent()
        val expectedAfter = """
            <D> myArchives
               <D> arch_prj_A
                  <D> changes-html
                     <F> %DEFAULT_RUN_ID%.html
                  <D> code-archive
                     <D> src
                        <D> base
                           <D> %DEFAULT_RUN_ID%
                              <F> MyClassA.kt
                              <F> MyClassB.kt
                              <F> OldClass.kt
                        <D> other
                           <D> %DEFAULT_RUN_ID%
                              <F> MyClassA.java
                  <D> operation-index
                     <F> %DEFAULT_RUN_ID%.dat
                  <D> summary
                     <D> %DEFAULT_RUN_ID%
                        <F> MyClassA-1.json
                        <F> MyClassA.json
                        <F> MyClassB.json
                        <F> OldClass.json
            <D> myProjects
               <D> projectA
                  <D> src
                     <D> base
                        <F> MyClassA.kt
                        <F> MyClassB.kt
                     <D> other
                        <F> MyClassA.java
                     <F> config.yaml
        """.trimIndent().replace("%DEFAULT_RUN_ID%", DEFAULT_RUN_ID)
        assertEquals(expectedAfter, fsAfter)

        //: test of on summary item
        val expectedJsonA1 = """
            {"relativePath":"src/base/MyClassA.kt",
            "items":[
            {"filePath":"/myProjects/projectA/src/base/MyClassA.kt"
            ,"pos":{"first":0,"last":19}
            ,"lines":{"first":1,"last":1}
            ,"commentText":"//comment\"to\"remove\n"}
            ]
            }
        """.trimIndent().replace("\n", "")
        val actualJsonA1 = (testBed.fileSystem / "/myArchives/arch_prj_A/summary/$DEFAULT_RUN_ID/MyClassA.json").readString()
        log("test_addSummaryToArchive_multipleRuns A1: >>$actualJsonA1<<")
        assertEquals(expectedJsonA1, actualJsonA1)

        val expectedIndexText = """
            src/base/MyClassA.kt:1 comment removed
            src/base/MyClassB.kt:2 comments removed
            src/base/OldClass.kt:file removed
            src/other/MyClassA.java:1 comment removed
        """.trimIndent()
        assertEquals(expectedIndexText, (testBed.fileSystem / "/myArchives/arch_prj_A/operation-index/$DEFAULT_RUN_ID.dat").readString().trimIndent())

        //: test file with removed comments
        val expectedSourceFileARemainingText = """
            package base
            data class A(val x: Int)
            class MyClassA{
                val x = 5
                val y = "6"
            }
        """.trimIndent()
        assertEquals(expectedSourceFileAArchivedText, (testBed.fileSystem / "/myArchives/arch_prj_A/code-archive/src/base/$DEFAULT_RUN_ID/MyClassA.kt").readString().trimIndent())
        assertEquals(expectedSourceFileARemainingText, (testBed.fileSystem / "/myProjects/projectA/src/base/MyClassA.kt").readString().trimIndent())
    }

    @Test
    fun test_archiveComments_multipleRuns() {
        val testBed = BasicTestBed()
        val rootDir = (testBed.fileSystem / "/")
        val fsBefore = rootDir.listDirAsString().trimIndent()
        val expectedBefore = """
            <D> myArchives
               <D> arch_prj_A
            <D> myProjects
               <D> projectA
                  <D> src
                     <D> base
                        <F> MyClassA.kt
                        <F> MyClassB.kt
                        <F> OldClass.kt
                     <D> other
                        <F> MyClassA.java
                     <F> config.yaml
        """.trimIndent()
        assertEquals(expectedBefore, fsBefore)
        val expectedSourceFileA1ArchivedText = testBed.sourceFileA1.readString()
        val expectedSourceFileA2ArchivedText = testBed.sourceFileA2.readString()

        val commentsFirstRun = testBed.comments.filter { (it.filePath.contains("MyClassA.java") || (it.filePath.contains("MyClassB.kt"))) }
        val commentsSecondRun = testBed.comments.filter { !commentsFirstRun.map { it.filePath }.contains(it.filePath) }

        archiveComments(testBed.fileSystem, testBed.config, commentsFirstRun) { printedLines.add(it) }

        val fsAfter = rootDir.listDirAsString().trimIndent()
        val expectedAfter = """
            <D> myArchives
               <D> arch_prj_A
                  <D> changes-html
                     <F> %DEFAULT_RUN_ID%.html
                  <D> code-archive
                     <D> src
                        <D> base
                           <D> %DEFAULT_RUN_ID%
                              <F> MyClassB.kt
                        <D> other
                           <D> %DEFAULT_RUN_ID%
                              <F> MyClassA.java
                  <D> operation-index
                     <F> %DEFAULT_RUN_ID%.dat
                  <D> summary
                     <D> %DEFAULT_RUN_ID%
                        <F> MyClassA.json
                        <F> MyClassB.json
            <D> myProjects
               <D> projectA
                  <D> src
                     <D> base
                        <F> MyClassA.kt
                        <F> MyClassB.kt
                        <F> OldClass.kt
                     <D> other
                        <F> MyClassA.java
                     <F> config.yaml
        """.trimIndent().replace("%DEFAULT_RUN_ID%", DEFAULT_RUN_ID)
        assertEquals(expectedAfter, fsAfter)

        //: test of on summary item
        val expectedJsonA1 = """
            {"relativePath":"src/other/MyClassA.java",
            "items":[
            {"filePath":"/myProjects/projectA/src/other/MyClassA.java"
            ,"pos":{"first":0,"last":19}
            ,"lines":{"first":1,"last":1}
            ,"commentText":"//comment\"to\"remove\n"}
            ]
            }
        """.trimIndent().replace("\n", "")
        val actualJsonA1 = (testBed.fileSystem / "/myArchives/arch_prj_A/summary/$DEFAULT_RUN_ID/MyClassA.json").readString()
        log("test_archiveComments_multipleRuns A1: >>$actualJsonA1<<")
        assertEquals(expectedJsonA1, actualJsonA1)

        val expectedIndexText = """
            src/base/MyClassB.kt:2 comments removed
            src/other/MyClassA.java:1 comment removed
        """.trimIndent()
        assertEquals(expectedIndexText, (testBed.fileSystem / "/myArchives/arch_prj_A/operation-index/$DEFAULT_RUN_ID.dat").readString().trimIndent())

        //: test file with removed comments
        val expectedSourceFileA2RemainingText = """
            package other
            data class A(val x: Int)
            class MyClassA{
                val x = 5
                val y = "6"
            }
        """.trimIndent()
        assertEquals(expectedSourceFileA2ArchivedText, (testBed.fileSystem / "/myArchives/arch_prj_A/code-archive/src/other/$DEFAULT_RUN_ID/MyClassA.java").readString().trimIndent())
        assertEquals(expectedSourceFileA2RemainingText, (testBed.fileSystem / "/myProjects/projectA/src/other/MyClassA.java").readString().trimIndent())

        //: ---------- second run -------------------------

        overwrittenRunId = SECOND_RUN_ID
        archiveComments(testBed.fileSystem, testBed.config, commentsSecondRun) { printedLines.add(it) }

        val fsAfter2 = rootDir.listDirAsString().trimIndent()
        val expectedAfter2 = """
            <D> myArchives
               <D> arch_prj_A
                  <D> changes-html
                     <F> %DEFAULT_RUN_ID%.html
                     <F> %SECOND_RUN_ID%.html
                  <D> code-archive
                     <D> src
                        <D> base
                           <D> %DEFAULT_RUN_ID%
                              <F> MyClassB.kt
                           <D> %SECOND_RUN_ID%
                              <F> MyClassA.kt
                              <F> OldClass.kt
                        <D> other
                           <D> %DEFAULT_RUN_ID%
                              <F> MyClassA.java
                  <D> operation-index
                     <F> %DEFAULT_RUN_ID%.dat
                     <F> %SECOND_RUN_ID%.dat
                  <D> summary
                     <D> %DEFAULT_RUN_ID%
                        <F> MyClassA.json
                        <F> MyClassB.json
                     <D> %SECOND_RUN_ID%
                        <F> MyClassA.json
                        <F> OldClass.json
            <D> myProjects
               <D> projectA
                  <D> src
                     <D> base
                        <F> MyClassA.kt
                        <F> MyClassB.kt
                     <D> other
                        <F> MyClassA.java
                     <F> config.yaml
        """.trimIndent().replace("%DEFAULT_RUN_ID%", DEFAULT_RUN_ID).replace("%SECOND_RUN_ID%", SECOND_RUN_ID)
        assertEquals(expectedAfter2, fsAfter2)

        //: test of on summary item
        val expectedJsonA1SecondRun = """
            {"relativePath":"src/base/MyClassA.kt",
            "items":[
            {"filePath":"/myProjects/projectA/src/base/MyClassA.kt"
            ,"pos":{"first":0,"last":19}
            ,"lines":{"first":1,"last":1}
            ,"commentText":"//comment\"to\"remove\n"}
            ]
            }
        """.trimIndent().replace("\n", "")
        val actualJsonA1SecondRun = (testBed.fileSystem / "/myArchives/arch_prj_A/summary/$SECOND_RUN_ID/MyClassA.json").readString()
        log("test_archiveComments_multipleRuns A1: >>$actualJsonA1<<")
        assertEquals(expectedJsonA1SecondRun, actualJsonA1SecondRun)

        val expectedIndexTextSecondRun = """
            src/base/MyClassA.kt:1 comment removed
            src/base/OldClass.kt:file removed
        """.trimIndent()
        assertEquals(expectedIndexTextSecondRun, (testBed.fileSystem / "/myArchives/arch_prj_A/operation-index/$SECOND_RUN_ID.dat").readString().trimIndent())

        //: test file with removed comments
        val expectedSourceFileA1RemainingText = """
            package base
            data class A(val x: Int)
            class MyClassA{
                val x = 5
                val y = "6"
            }
        """.trimIndent()

        assertEquals(expectedSourceFileA1ArchivedText, (testBed.fileSystem / "/myArchives/arch_prj_A/code-archive/src/base/$SECOND_RUN_ID/MyClassA.kt").readString().trimIndent())
        assertEquals(expectedSourceFileA1RemainingText, (testBed.fileSystem / "/myProjects/projectA/src/base/MyClassA.kt").readString().trimIndent())
    }

}