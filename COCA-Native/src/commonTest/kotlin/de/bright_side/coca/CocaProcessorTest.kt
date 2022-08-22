package de.bright_side.coca

import de.bright_side.coca.StringMatchType.*
import de.bright_side.filesystemfacade.facade.FsfFile
import de.bright_side.filesystemfacade.facade.IncludeExclude
import de.bright_side.filesystemfacade.facade.MatchType
import de.bright_side.filesystemfacade.facade.PathFilterItem
import de.bright_side.filesystemfacade.memoryfs.MemoryFs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CocaProcessorTest : CocaProcessor() {
    private val printedLines = mutableListOf<String>()

    fun provideOptions() = options
    fun provideConfig() = config
    fun provideFileSystem() = fileSystem
    fun provideWriteOutputLine(line: String) = writeOutputLine(line)

    private fun log(message: String) = println("de.bright_side.coca.CocaProcessorTest> $message")

    fun crateProcessorInstance(previewFormat: PreviewFormat = PreviewFormat.BEGINNING_OF_TEXT): CocaProcessorTest {
        val configFilePath = "/myConfigFile.yaml"
        val fileSystem = MemoryFs()
        val options = CocaOptions(CocaAction.PREVIEW, configFilePath, previewFormat, "", LogLevel.DEBUG)
        val configFileText = """
        coca config version: 1
        archive root path: /myArchive
        source root path: /myProject
        include paths:
         - /src/main/java/*
         - /src/main/kotlin/*
         - /src/test/java/*
         - /src/test/kotlin/*
        exclude paths:
         - */gen/*
         - *Test.*
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
        val configFile = fileSystem.createByPath(configFilePath).writeString(configFileText)
        val config = CocaConfigReader().readConfig(configFile)
        val result = CocaProcessorTest()
        result.options = options
        result.config = config
        result.fileSystem = fileSystem
        result.writeOutputLineHandler = { printedLines.add(it) }
        result.logDebug = { println("CocaProcessor>$it") }
        return result
    }

    @Test
    fun test_matchesEnding_bulk() {
        val processor = crateProcessorInstance()
        assertEquals(true, processor.matchesEnding("abc\n", listOf("\n")))
        assertEquals(false, processor.matchesEnding("abc\n", listOf("x")))
        assertEquals(true, processor.matchesEnding("abc\n", listOf("\r", "\n")))
        assertEquals(true, processor.matchesEnding("\n", listOf("\r", "\n")))
        assertEquals(false, processor.matchesEnding("\n", listOf("a", "b")))
        assertEquals(false, processor.matchesEnding("", listOf("\r", "\n")))
    }

    @Test
    fun test_matches_bulk() {
        val processor = crateProcessorInstance()
        assertEquals(true, processor.matches("hello", listOf(StringFilter("hello", EXACT))))
        assertEquals(true, processor.matches("hello", listOf(StringFilter("hello", STARTS_WITH))))
        assertEquals(true, processor.matches("hello", listOf(StringFilter("hello", ENDS_WITH))))
        assertEquals(true, processor.matches("hello", listOf(StringFilter("hello", CONTAINS))))

        assertEquals(false, processor.matches("XhelloX", listOf(StringFilter("hello", EXACT))))
        assertEquals(false, processor.matches("XhelloX", listOf(StringFilter("hello", STARTS_WITH))))
        assertEquals(false, processor.matches("XhelloX", listOf(StringFilter("hello", ENDS_WITH))))
        assertEquals(true, processor.matches("XhelloX", listOf(StringFilter("hello", CONTAINS))))

        assertEquals(false, processor.matches("Xhello", listOf(StringFilter("hello", EXACT))))
        assertEquals(false, processor.matches("Xhello", listOf(StringFilter("hello", STARTS_WITH))))
        assertEquals(true, processor.matches("Xhello", listOf(StringFilter("hello", ENDS_WITH))))
        assertEquals(true, processor.matches("Xhello", listOf(StringFilter("hello", CONTAINS))))

        assertEquals(false, processor.matches("helloX", listOf(StringFilter("hello", EXACT))))
        assertEquals(true, processor.matches("helloX", listOf(StringFilter("hello", STARTS_WITH))))
        assertEquals(false, processor.matches("helloX", listOf(StringFilter("hello", ENDS_WITH))))
        assertEquals(true, processor.matches("helloX", listOf(StringFilter("hello", CONTAINS))))

        assertEquals(false, processor.matches("there", listOf(StringFilter("hello", EXACT))))
        assertEquals(false, processor.matches("there", listOf(StringFilter("hello", STARTS_WITH))))
        assertEquals(false, processor.matches("there", listOf(StringFilter("hello", ENDS_WITH))))
        assertEquals(false, processor.matches("there", listOf(StringFilter("hello", CONTAINS))))
    }

    private fun writeFiles(dirs: List<FsfFile>, filenames: List<String>, fileContent: String) {
        dirs.forEach { dir ->
            filenames.forEach { filename ->
                (dir / filename).writeString(fileContent)
            }
        }
    }

    private fun prepareTestFilteredSourceFiles(processor: CocaProcessorTest) {
        val sourceDir = processor.fileSystem / processor.config.sourceRootPath
        val externalDirToIgnoreA = (sourceDir / "otherDir").mkdir()
        val externalDirToIgnoreB = (sourceDir / "otherDirB" / "xyz").mkdirs()
        val projectDirToIgnore = (sourceDir / "src" / "other" / "kotlin").mkdirs()
        val kotlinMainDir = (sourceDir / "src" / "main" / "kotlin").mkdirs()
        val kotlinGenDir = (kotlinMainDir / "gen" / "main-classes").mkdirs()
        val kotlinMainOtherSubDir = (kotlinMainDir / "generic" / "main-classes").mkdirs()
        val kotlinTestDir = (sourceDir / "src" / "test" / "kotlin").mkdirs()
        val kotlinMainSubDir = (kotlinMainDir / "de" / "bright_side" / "abc").mkdirs()

        writeFiles(listOf(externalDirToIgnoreA, externalDirToIgnoreB, projectDirToIgnore, kotlinGenDir),
            listOf("fileA.kt", "fileB.kt"),
            "test /*comment*/ end")
        writeFiles(listOf(kotlinMainDir, kotlinTestDir, kotlinMainSubDir), listOf("ignoreA.txt", "ignoreB.txt"), "test /*comment*/ end")
        writeFiles(listOf(kotlinMainDir), listOf("Main.kt"), "/*comment*/fun main(){println(\"hello\")}")
        writeFiles(listOf(kotlinTestDir), listOf("MyClassTest.kt"), "/*comment*/class MyClassTest(){}")
        writeFiles(listOf(kotlinTestDir), listOf("MyClassIT.kt"), "/*comment*/class MyClassIT(){}")
        writeFiles(listOf(kotlinMainSubDir), listOf("MyClass.kt"), "/*comment*/class MyClass(): XYZ(){}")
        writeFiles(listOf(kotlinMainSubDir), listOf("MyJavaClass.java"), "/*comment*/class MyOtherClass(): ABC(){}")
        writeFiles(listOf(kotlinMainOtherSubDir), listOf("MyGenericClass.kt"), "/*comment*/class MyGenericClass(): XYZ(){}")
    }

    @Test
    fun test_readFilteredSourceFiles_normal() {
        val processor = crateProcessorInstance()
        prepareTestFilteredSourceFiles(processor)

        val result = processor.readFilteredSourceFiles()
        val resultPaths = result.map { it.absolutePath }
        val basePath = processor.config.sourceRootPath
        assertEquals(5, resultPaths.size)
        assertTrue(resultPaths.contains("$basePath/src/main/kotlin/Main.kt"))
        assertTrue(resultPaths.contains("$basePath/src/test/kotlin/MyClassIT.kt"))
        assertTrue(resultPaths.contains("$basePath/src/main/kotlin/de/bright_side/abc/MyClass.kt"))
        assertTrue(resultPaths.contains("$basePath/src/main/kotlin/de/bright_side/abc/MyJavaClass.java"))
        assertTrue(resultPaths.contains("$basePath/src/main/kotlin/generic/main-classes/MyGenericClass.kt"))
    }

    @Test
    fun test_readFilteredSourceFiles_noFiles() {
        val processor = crateProcessorInstance()
        val sourceDir = processor.fileSystem / processor.config.sourceRootPath
        val externalDirToIgnoreA = (sourceDir / "otherDir").mkdir()
        val externalDirToIgnoreB = (sourceDir / "otherDirB" / "xyz").mkdirs()
        val projectDirToIgnore = (sourceDir / "src" / "other" / "kotlin").mkdirs()
        val kotlinMainDir = (sourceDir / "src" / "main" / "kotlin").mkdirs()
        val kotlinGenDir = (kotlinMainDir / "gen" / "main-classes").mkdirs()
        val kotlinMainOtherSubDir = (kotlinMainDir / "generic" / "main-classes").mkdirs()
        val kotlinTestDir = (sourceDir / "src" / "test" / "kotlin").mkdirs()
        val kotlinMainSubDir = (kotlinMainDir / "de" / "bright_side" / "abc").mkdirs()

        val result = processor.readFilteredSourceFiles()
        assertTrue(result.isEmpty())
    }

    @Test
    fun test_readFilteredSourceFiles_allExcluded() {
        val processor = crateProcessorInstance()
        prepareTestFilteredSourceFiles(processor)

        //: exclude all
        val newExcludedPath: MutableList<StringFilter> = processor.config.excludePaths.toMutableList()
        newExcludedPath += StringFilter("Class", CONTAINS)
        newExcludedPath += StringFilter("/Main.kt", ENDS_WITH)
        processor.config = processor.config.copy(excludePaths = newExcludedPath)

        val result = processor.readFilteredSourceFiles()

        log("test_readFilteredSourceFiles_allExcluded: result:${result.joinToString("") { "\n - $it" }}")

        assertTrue(result.isEmpty())
    }

    @Test
    fun test_readFilteredSourceFiles_noneIncluded() {
        val processor = crateProcessorInstance()
        prepareTestFilteredSourceFiles(processor)
        log("test_readFilteredSourceFiles_noneIncluded: sourceRootPath: ${processor.config.sourceRootPath}")
        log("test_readFilteredSourceFiles_noneIncluded: includePaths before update: ${processor.config.includePaths}")
        log("test_readFilteredSourceFiles_noneIncluded: excludePaths before update: ${processor.config.excludePaths}")

        log("test_readFilteredSourceFiles_noneIncluded: processor.config before update = ${processor.config.includeFileEndings}")

        //: include none
        val newIncludeEndings: MutableList<String> = mutableListOf()
        newIncludeEndings += ".yaml"
        newIncludeEndings += ".bat"
        processor.config = processor.config.copy(includeFileEndings = newIncludeEndings)
        log("test_readFilteredSourceFiles_noneIncluded: sourceRootPath after update: ${processor.config.sourceRootPath}")
        log("test_readFilteredSourceFiles_noneIncluded: includePaths: ${processor.config.includePaths}")
        log("test_readFilteredSourceFiles_noneIncluded: excludePaths: ${processor.config.excludePaths}")
        log("test_readFilteredSourceFiles_noneIncluded: processor.includeFileEndings after update = ${processor.config.includeFileEndings}")

        val result = processor.readFilteredSourceFiles()
        log("test_readFilteredSourceFiles_noneIncluded: result:${result.joinToString("") { "\n - $it" }}")
        assertTrue(result.isEmpty())
    }

    private fun prepareOccurrencesInFile(processor: CocaProcessorTest, filename: String, fileContent: String): FsfFile {
        val mainDir = (processor.fileSystem / processor.config.sourceRootPath / "src" / "main" / "kotlin").mkdirs()
        return (mainDir / filename).writeString(fileContent)
    }


    @Test
    fun test_findAllOccurrencesInSourceDir_normal() {
        val processor = crateProcessorInstance()
        val codeA = "/*commented\nout*/\nclass MyClassA{\n    //old text\n    val number: Int\n}"
        val codeB = "class MyClassB{\n    val number: Int //:keep\n}\n//old text1\n//old text2\nend"
        val codeC = "/*commented\r\nout*/\r\nclass MyClassA{\r\n    //old text\r\n    val number: Int\r\n}"
                 //: 00000000001 1 111111 1 1222222222233333 3 333344444444445 5 55555555666666666677 7 7
                 //: 01234567890 1 234567 8 9012345678901234 5 678901234567890 1 23456789012345678901 2 3
        val codeD = "abc/**keep info\n//nested comment\nabc*/"
                 //: 000000000011111 11111222222222233 333333
                 //: 012345678901234 56789012345678901 234567
        val codeE = "abc/*remove info\n//:nested comment\nabd*/end"
                 //: 0000000000111111 111122222222223333 333333444
                 //: 0123456789012345 678901234567890123 456789012
        val fileA = prepareOccurrencesInFile(processor, "ClassA.kt", codeA)
        val fileB = prepareOccurrencesInFile(processor, "ClassB.kt", codeB)
        val fileC = prepareOccurrencesInFile(processor, "ClassC.kt", codeC)
        val fileD = prepareOccurrencesInFile(processor, "ClassD.kt", codeD)
        val fileE = prepareOccurrencesInFile(processor, "ClassE.kt", codeE)

        val result = processor.findAllOccurrencesInSourceDir()
        log("test_findAllOccurrencesInSourceDir_normal: result = \n$result")
        assertEquals(6, result.size)

        assertEquals(fileA.absolutePath, result[0].filePath)
        assertEquals(0, result[0].pos.first)
        assertEquals(16, result[0].pos.last)
        assertEquals(1, result[0].lines.first)
        assertEquals(2, result[0].lines.last)
        assertEquals("/*commented\nout*/", result[0].commentText)

        assertEquals(fileA.absolutePath, result[1].filePath)
        assertEquals(38, result[1].pos.first)
        assertEquals(48, result[1].pos.last)
        assertEquals(4, result[1].lines.first)
        assertEquals(4, result[1].lines.last)
        assertEquals("//old text\n", result[1].commentText)

        assertEquals(fileB.absolutePath, result[2].filePath)
        assertEquals(46, result[2].pos.first)
        assertEquals(69, result[2].pos.last)
        assertEquals(4, result[2].lines.first)
        assertEquals(5, result[2].lines.last)
        assertEquals("//old text1\n//old text2\n", result[2].commentText)

        assertEquals(fileC.absolutePath, result[3].filePath)
        assertEquals(0, result[3].pos.first)
        assertEquals(17, result[3].pos.last)
        assertEquals(1, result[3].lines.first)
        assertEquals(2, result[3].lines.last)
        assertEquals("/*commented\r\nout*/", result[3].commentText)

        assertEquals(fileC.absolutePath, result[4].filePath)
        assertEquals(41, result[4].pos.first)
        assertEquals(52, result[4].pos.last)
        assertEquals(4, result[4].lines.first)
        assertEquals(4, result[4].lines.last)
        assertEquals("//old text\r\n", result[4].commentText)

        assertEquals(fileE.absolutePath, result[5].filePath)
        assertEquals(3, result[5].pos.first)
        assertEquals(39, result[5].pos.last)
        assertEquals(1, result[5].lines.first)
        assertEquals(3, result[5].lines.last)
        assertEquals("/*remove info\n//:nested comment\nabd*/", result[5].commentText)
    }

    @Test
    fun test_process_normal() {
        val processor = crateProcessorInstance()
        val codeA = "/*commented\nout*/\nclass MyClassA{\n    //old text\n    val number: Int\n}"
        val codeB = "class MyClassB{\n    val number: Int //:keep\n}\n//old text1\n//old text2\nend"
        val codeC = "/*commented\r\nout*/\r\nclass MyClassA{\r\n    //old text\r\n    val number: Int\r\n}"
        val codeD = "abc/**keep info\n//nested comment\nabc*/"
        val codeE = "abc/*remove info\n//:nested comment\nabd*/end"
        val codeF = "//1\nval x = \"some text with comment //old text\""
        val fileA = prepareOccurrencesInFile(processor, "ClassA.kt", codeA)
        val fileB = prepareOccurrencesInFile(processor, "ClassB.kt", codeB)
        val fileC = prepareOccurrencesInFile(processor, "ClassC.kt", codeC)
        val fileD = prepareOccurrencesInFile(processor, "ClassD.kt", codeD)
        val fileE = prepareOccurrencesInFile(processor, "ClassE.kt", codeE)
        val fileF = prepareOccurrencesInFile(processor, "ClassF.kt", codeF)

        processor.process()
        val result = printedLines.filter{!it.startsWith(INFO_OUTPUT_PREFIX)}
        log("test_process_normal: printedLines:\n${result.joinToString("\n")}")
        val sourcePathPrefixToRemoveLength = (processor.config.sourceRootPath + "/").length
        assertEquals(7, result.size)
        assertTrue(result[0].startsWith("${fileA.absolutePath.substring(sourcePathPrefixToRemoveLength)}:1>"))
        assertTrue(result[1].startsWith("${fileA.absolutePath.substring(sourcePathPrefixToRemoveLength)}:4>"))
        assertTrue(result[2].startsWith("${fileB.absolutePath.substring(sourcePathPrefixToRemoveLength)}:4>"))
        assertTrue(result[3].startsWith("${fileC.absolutePath.substring(sourcePathPrefixToRemoveLength)}:1>"))
        assertTrue(result[4].startsWith("${fileC.absolutePath.substring(sourcePathPrefixToRemoveLength)}:4>"))
        assertTrue(result[5].startsWith("${fileE.absolutePath.substring(sourcePathPrefixToRemoveLength)}:1>"))
        assertTrue(result[6].startsWith("${fileF.absolutePath.substring(sourcePathPrefixToRemoveLength)}:1>"))
    }

    @Test
    fun createPathFilter_bulk(){
        val filterA = StringFilter("a", CONTAINS)
        val filterB = StringFilter("hello", EXACT)
        val filterC = StringFilter(".txt", ENDS_WITH)
        val filterD = StringFilter("x", STARTS_WITH)

        assertEquals(0, createPathFilter(listOf(), listOf()).size)

        var result = createPathFilter(listOf(filterA), listOf())
        assertEquals(1, result.size)
        assertTrue(result.contains(PathFilterItem(IncludeExclude.INCLUDE, "a", MatchType.CONTAINS)))

        result = createPathFilter(listOf(filterA), listOf(filterB))
        assertEquals(2, result.size)
        assertTrue(result.contains(PathFilterItem(IncludeExclude.INCLUDE, "a", MatchType.CONTAINS)))
        assertTrue(result.contains(PathFilterItem(IncludeExclude.EXCLUDE, "hello", MatchType.EXACT)))

        result = createPathFilter(listOf(), listOf(filterB))
        assertEquals(1, result.size)
        assertTrue(result.contains(PathFilterItem(IncludeExclude.EXCLUDE, "hello", MatchType.EXACT)))

        result = createPathFilter(listOf(filterA, filterC), listOf(filterB, filterD))
        assertEquals(4, result.size)
        assertTrue(result.contains(PathFilterItem(IncludeExclude.INCLUDE, "a", MatchType.CONTAINS)))
        assertTrue(result.contains(PathFilterItem(IncludeExclude.EXCLUDE, "hello", MatchType.EXACT)))
        assertTrue(result.contains(PathFilterItem(IncludeExclude.INCLUDE, ".txt", MatchType.ENDS_WITH)))
        assertTrue(result.contains(PathFilterItem(IncludeExclude.EXCLUDE, "x", MatchType.STARTS_WITH)))
    }
}


