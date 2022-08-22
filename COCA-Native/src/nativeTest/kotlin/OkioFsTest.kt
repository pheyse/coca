import de.bright_side.filesystemfacade.BaseFsTestAbstract
import de.bright_side.filesystemfacade.okiofs.OkioFile
import de.bright_side.filesystemfacade.okiofs.OkioFs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OkioFsTest: BaseFsTestAbstract() {
    override val testDirPath = "C:\\DA1D\\coca-test\\"
    override fun createFs() = OkioFs()

    fun assertFalse(actual: Boolean){
        assertTrue(!actual)
    }

    @Test
    fun createCleanUpBat() {
        val fs = OkioFs()
        val file = fs.createByPath(testDirPath).parentFile?.getChild("CleanUp_CocaTest.bat") ?:throw Exception("Missing dir")
        file.writeString(
            """
            @echo cleaning dir '$testDirPath'...
            @pause
            @del /S /Q "$testDirPath*" 2>nul
            @rmdir /S /Q "$testDirPath" 2>nul
            @pause
            """.trimIndent()
        )
    }

    @Test
    fun windows_IsAbsolutePath() {
        val testDir = init("windows_IsAbsolutePath")
        val path = "${testDirPath}${testDir.name}\\newFile-12345.txt"
        log("windows_IsAbsolutePath: path = '$path'")
        val newFile = testDir.fsfSystem / path
        log("windows_IsAbsolutePath: new file exists?")
        assertFalse(newFile.exists())
        log("windows_IsAbsolutePath: is path absolute?")
        assertTrue((newFile as OkioFile).isPathAbsolute())
        log("windows_IsAbsolutePath: is path == absolute path")
        assertEquals(path, newFile.absolutePath)
        log("windows_IsAbsolutePath: write text")
        newFile += "hello"
        log("windows_IsAbsolutePath: does file exist after writing")
        assertTrue(newFile.exists())
        log("windows_IsAbsolutePath: is path == absolute path after writing file")
        assertEquals(path, newFile.absolutePath)
    }

    @Test
    fun windows_GetChild() {
        val testDir = init("windows_GetChild")
        val childName = "file.txt"
        val child1 = testDir.getChild(childName)
        log("windows_GetChild: child1 = '$child1'")
        assertEquals(testDir.absolutePath + "\\" + childName, child1.absolutePath)

        val child2 = testDir / childName
        log("windows_GetChild: child2 = '$child2'")
        assertEquals(testDir.absolutePath + "\\" + childName, child2.absolutePath)
    }

    @Test
    fun windows_listFilesTree_deep() {
        val method = "windows_listFilesTree_deep"
        val testDir = init("windows_GetAbsolutePath")
        val path = "C:\\Philip\\Development\\Kotlin\\CommentedOutCodeArchiver\\COCA-Native\\.gradle\\kotlin\\"
        log("$method: path = '$path'")
        val dir = testDir.fsfSystem / path
        val filesTree = dir.listFilesTree() ?: throw Exception("Could not list dir!")
        log("$method: filesTree: ${filesTree.joinToString("") { "\n - $it" }}")
        val absolutePaths = filesTree.map{it.absolutePath}
        log("$method: filesTree absolutePaths: ${absolutePaths.joinToString("") { "\n - $it" }}")
    }

    @Test
    fun windows_GetDeepAbsolutePath() {
        val method = "windows_GetDeepAbsolutePath"
        val testDir = init("windows_GetDeepAbsolutePath")
        val path = "C:\\Philip\\Development\\Kotlin\\CommentedOutCodeArchiver\\COCA-Native\\.gradle\\kotlin\\sourceSetMetadata\\CommentedOutCodeArchiver\\commonMain\\implementation\\org.jetbrains.kotlinx-kotlinx-serialization-core\\org.jetbrains.kotlinx-kotlinx-serialization-core-commonMain.klib"
        log("$method: path = '$path'")
        val newFile = testDir.fsfSystem / path
        log("$method: is path absolute?")
        assertTrue((newFile as OkioFile).isPathAbsolute())
        log("$method: is path == absolute path")
        assertEquals(path, newFile.absolutePath)
    }


}