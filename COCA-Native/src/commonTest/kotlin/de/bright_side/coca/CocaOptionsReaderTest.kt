package de.bright_side.coca

import de.bright_side.coca.CocaAction
import de.bright_side.coca.CocaOptionsReader
import de.bright_side.velvetkotlin.assertUniqueExceptionInBlock
import kotlin.test.Test
import kotlin.test.assertEquals

class CocaOptionsReaderTest {

    @Test
    fun test_normalCase(){
        val configPath = "C:\\myconfig.yml"
        val args = listOf("-a", "p", "-c", "$configPath")
        val result = CocaOptionsReader().readOptions(args)

        assertEquals(CocaAction.PREVIEW, result.action)
        assertEquals(configPath, result.configFilePath)
    }

    @Test
    fun test_missingValue(){
        val args = listOf("-a", "p", "-c")
        assertUniqueExceptionInBlock("3ee4dc56-35c3-4761-a867-68a70bbecf81"){
            CocaOptionsReader().readOptions(args)
        }
    }

    @Test
    fun test_missingKeyPrefix(){
        assertUniqueExceptionInBlock("da8ece32-c8aa-4a42-9002-ff55affd3536"){
            val configPath = "C:\\myconfig.yml"
            val args = listOf("-a", "p", "c", "$configPath")
            CocaOptionsReader().readOptions(args)
        }
    }

    @Test
    fun test_duplicateParameter(){
        assertUniqueExceptionInBlock("cfec0411-c12b-43ca-9626-fb8b48dc9c4d"){
            val configPath = "C:\\myconfig.yml"
            val args = listOf("-a", "p", "-c", "$configPath", "-a", "p", )
            CocaOptionsReader().readOptions(args)
        }
    }

    @Test
    fun test_wrongConfigFileEnding(){
        assertUniqueExceptionInBlock("80ea782c-439f-4913-94f9-e11cf94667ea"){
            val configPath = "C:\\myconfig.dat"
            val args = listOf("-a", "p", "-c", "$configPath")
            CocaOptionsReader().readOptions(args)
        }
    }

    @Test
    fun test_missingAction(){
        val configPath = "C:\\myconfig.yml"
        val args = listOf("-c", "$configPath")
        assertUniqueExceptionInBlock("cfec0411-c12b-43ca-9626-fb8b48dc9c4d"){
            CocaOptionsReader().readOptions(args)
        }
    }

    @Test
    fun test_unknownAction(){
        val configPath = "C:\\myconfig.yml"
        val args = listOf("-a", "x", "-c", "$configPath")
        assertUniqueExceptionInBlock("2c6284a3-d294-46dd-aee6-3448f5980bb4"){
            CocaOptionsReader().readOptions(args)
        }
    }

    @Test
    fun test_missingConfigFile(){
        val args = listOf("-a", "p")
        assertUniqueExceptionInBlock("76d8fc96-b3c9-4ebf-b4b5-77d1ea7ac524"){
            CocaOptionsReader().readOptions(args)
        }
    }

    @Test
    fun test_previewFormatMultiline(){
        val configPath = "C:\\myconfig.yml"
        val args = listOf("-a", "p", "-f", "m", "-c", "$configPath")
        val result = CocaOptionsReader().readOptions(args)

        assertEquals(CocaAction.PREVIEW, result.action)
        assertEquals(PreviewFormat.MULTILINE, result.previewFormat)
        assertEquals(configPath, result.configFilePath)
    }

    @Test
    fun test_previewFormat_Beginning(){
        val configPath = "C:\\myconfig.yml"
        val args = listOf("-a", "p", "-f", "b", "-c", "$configPath")
        val result = CocaOptionsReader().readOptions(args)

        assertEquals(CocaAction.PREVIEW, result.action)
        assertEquals(PreviewFormat.BEGINNING_OF_TEXT, result.previewFormat)
        assertEquals(configPath, result.configFilePath)
    }

    @Test
    fun test_previewFormat_Html(){
        val configPath = "C:\\myconfig.yml"
        val args = listOf("-a", "p", "-f", "h", "-o", "/my-output-file.html", "-c", "$configPath")
        val result = CocaOptionsReader().readOptions(args)

        assertEquals(CocaAction.PREVIEW, result.action)
        assertEquals(PreviewFormat.HTML, result.previewFormat)
        assertEquals(configPath, result.configFilePath)
    }

    @Test
    fun test_previewFormat_unknown(){
        val configPath = "C:\\myconfig.yml"
        val args = listOf("-a", "p", "-f", "x", "-c", "$configPath")
        assertUniqueExceptionInBlock("d033059e-8934-4668-96db-ae6dff78e156"){
            CocaOptionsReader().readOptions(args)
        }
    }

    @Test
    fun test_previewFormat_html_missingOutputPath(){
        val configPath = "C:\\myconfig.yml"
        val args = listOf("-a", "p", "-f", "h", "-c", "$configPath")
        assertUniqueExceptionInBlock("b3fc39f9-0596-43e1-a874-0987dc7c8e8c"){
            CocaOptionsReader().readOptions(args)
        }
    }

    @Test
    fun test_previewFormat_html_outputPathNotHtml(){
        val configPath = "C:\\myconfig.yml"
        val args = listOf("-a", "p", "-f", "h", "-o", "/my-output-file.txt", "-c", "$configPath")
        assertUniqueExceptionInBlock("210f78b8-8d38-4576-b233-b92f89478d87"){
            CocaOptionsReader().readOptions(args)
        }
    }

}