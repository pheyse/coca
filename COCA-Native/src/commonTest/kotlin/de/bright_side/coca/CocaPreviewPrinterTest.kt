package de.bright_side.coca

import de.bright_side.filesystemfacade.memoryfs.MemoryFs
import kotlin.test.Test
import kotlin.test.assertEquals

class CocaPreviewPrinterTest: CocaPreviewPrinter(
    MemoryFs(),
    CocaOptions(CocaAction.PREVIEW, "", PreviewFormat.BEGINNING_OF_TEXT, "", LogLevel.DEBUG),
    CocaConfigReader().createEmptyConfig()
) {
    val printedLines = mutableListOf<String>()

    private fun log(message: String) = println("de.bright_side.coca.CocaPreviewPrinterTest> $message")
    private fun createPreviewPrinterInstance(processor: CocaProcessorTest): CocaPreviewPrinter {
        return CocaPreviewPrinter(
            MemoryFs(),
            processor.provideOptions(),
            processor.provideConfig()
        ) { printedLines.add(it) }
    }

    @Test
    fun test_printPreview_multiline_normal(){
        val processor = CocaProcessorTest().crateProcessorInstance(PreviewFormat.MULTILINE)
        val commentText = """
            //    fun commentedOutMethod(param1: String) {
            //        try {
            //            old code 1
            //            old code 2
            //            old code 3
            //            old code 4
            //            old code 5
            //        } catch (e: Exception) {
            //            throw Exception("Problem", e)
            //        }
            //    }
        """.trimIndent()
        val occurrence = CommentOccurrence("/myProject/src/main/java/Main.kt", commentText.indices, 1..11, commentText)
        val previewPrinter = createPreviewPrinterInstance(processor)
        previewPrinter.printPreview(listOf(occurrence))
        val result = printedLines
        log("test_printPreview_multiline_normal. result:>>\n$result\n<<")

        val expectedOutput = """
            src/main/java/Main.kt:1-11>
                //    fun commentedOutMethod(param1: String) {
                //        try {
                [...]
                //        }
                //    }
        """.trimIndent()

        assertEquals(expectedOutput, result.joinToString("\n"))
    }

    @Test
    fun test_printPreview_multiline_singleLineComment(){
        val processor = CocaProcessorTest().crateProcessorInstance(PreviewFormat.MULTILINE)
        val commentText = """
            //    fun commentedOutMethod(param1: String) {
        """.trimIndent()
        val occurrence = CommentOccurrence("/myProject/src/main/java/Main.kt", commentText.indices, 17..17, commentText)
        val previewPrinter = createPreviewPrinterInstance(processor)
        previewPrinter.printPreview(listOf(occurrence))
        val result = printedLines

        val expectedOutput = """
            src/main/java/Main.kt:17-17>
                //    fun commentedOutMethod(param1: String) {
        """.trimIndent()

        assertEquals(expectedOutput, result.joinToString("\n"))
    }

    @Test
    fun test_printPreview_multiline_emptyComment(){
        val processor = CocaProcessorTest().crateProcessorInstance(PreviewFormat.MULTILINE)
        val commentText = ""
        val occurrence = CommentOccurrence("/myProject/src/main/java/Main.kt", commentText.indices, 17..17, commentText)
        val previewPrinter = createPreviewPrinterInstance(processor)
        previewPrinter.printPreview(listOf(occurrence))
        val result = printedLines

        val expectedOutput = """
            src/main/java/Main.kt:17-17>
                
        """.trimIndent()

        assertEquals(expectedOutput, result.joinToString("\n"))
    }

    @Test
    fun test_printPreview_multiline_5lines(){
        val processor = CocaProcessorTest().crateProcessorInstance(PreviewFormat.MULTILINE)
        val commentText = """
            //    fun commentedOutMethod(param1: String) {
            //        old code 1
            //        old code 2
            //        old code 3
            //    }
        """.trimIndent()
        val occurrence = CommentOccurrence("/myProject/src/main/java/Main.kt", commentText.indices, 1..5, commentText)
        val previewPrinter = createPreviewPrinterInstance(processor)
        previewPrinter.printPreview(listOf(occurrence))
        val result = printedLines
        log("test_printPreview_multiline_5lines. result:>>\n$result\n<<")

        val expectedOutput = """
            src/main/java/Main.kt:1-5>
                //    fun commentedOutMethod(param1: String) {
                //        old code 1
                //        old code 2
                //        old code 3
                //    }
        """.trimIndent()

        assertEquals(expectedOutput, result.joinToString("\n"))
    }

    @Test
    fun test_printPreview_multiline_6lines(){
        val processor = CocaProcessorTest().crateProcessorInstance(PreviewFormat.MULTILINE)
        val commentText = """
            //    fun commentedOutMethod(param1: String) {
            //        old code 1
            //        old code 2
            //        old code 3
            //        old code 4
            //    }
        """.trimIndent()
        val occurrence = CommentOccurrence("/myProject/src/main/java/Main.kt", commentText.indices, 1..5, commentText)
        val previewPrinter = createPreviewPrinterInstance(processor)
        previewPrinter.printPreview(listOf(occurrence))
        val result = printedLines
        log("test_printPreview_multiline_6lines. result:>>\n$result\n<<")

        val expectedOutput = """
            src/main/java/Main.kt:1-5>
                //    fun commentedOutMethod(param1: String) {
                //        old code 1
                [...]
                //        old code 4
                //    }
        """.trimIndent()

        assertEquals(expectedOutput, result.joinToString("\n"))
    }

    @Test
    fun test_getHighlightedAndPlainSections_normal(){
        val meth = "test_getHighlightedAndPlainSections_normal"
        val processor = CocaProcessorTest().crateProcessorInstance(PreviewFormat.MULTILINE)
        val fullText = """
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
        val occurrence = CommentOccurrence("/myProject/src/main/java/Main.kt", 122..389, 5..9, commentText)
        val previewPrinter = CocaPreviewPrinter(processor.provideFileSystem(), processor.provideOptions(), processor.provideConfig(), ::println)
        val actual = previewPrinter.getHighlightedAndPlainSections(fullText, listOf(occurrence))

        log("$meth: actual = $actual")

        val expectedPart1 = """
            import de.bright_side.coca.CocaConfigReader
            import de.bright_side.coca.CocaOptionsReader
            
            fun main(args: Array<String>) {
        """.trimIndent() + "\n"

       val expectedPart3 = "}"

        assertEquals(3, actual.size)
        assertEquals(false, actual[0].highlight)
        assertEquals(expectedPart1, actual[0].text)
        assertEquals(true, actual[1].highlight)
        assertEquals(commentText, actual[1].text)
        assertEquals(false, actual[2].highlight)
        assertEquals(expectedPart3, actual[2].text)
    }

}