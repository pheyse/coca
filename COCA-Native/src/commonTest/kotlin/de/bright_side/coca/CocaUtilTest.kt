package de.bright_side.coca

import de.bright_side.coca.CocaUtil.getFilenameAndExtension
import de.bright_side.coca.CocaUtil.getRelativePath
import kotlin.test.Test
import kotlin.test.assertEquals

class CocaUtilTest {
    @Test
    fun test_getRelativePath_bulk(){
        val slashPath = "/users/user-abc/src/my-file.txt"
        assertEquals("src/my-file.txt", getRelativePath(slashPath, "/users/user-abc/"))
        assertEquals("src/my-file.txt", getRelativePath(slashPath, "/users/user-abc"))
        assertEquals("user-abc/src/my-file.txt", getRelativePath(slashPath, "/users/"))
        assertEquals("users/user-abc/src/my-file.txt", getRelativePath(slashPath, "/"))
        assertEquals("my-file.txt", getRelativePath(slashPath, "/users/user-abc/src"))
        assertEquals("my-file.txt", getRelativePath(slashPath, "/users/user-abc/src/"))

        val windowsPath = "C:\\users\\user-abc\\src\\my-file.txt"
        assertEquals("src\\my-file.txt", getRelativePath(windowsPath, "C:\\users\\user-abc\\"))
        assertEquals("src\\my-file.txt", getRelativePath(windowsPath, "C:\\users\\user-abc"))
        assertEquals("user-abc\\src\\my-file.txt", getRelativePath(windowsPath, "C:\\users\\"))
        assertEquals("users\\user-abc\\src\\my-file.txt", getRelativePath(windowsPath, "C:\\"))
        assertEquals("my-file.txt", getRelativePath(windowsPath, "C:\\users\\user-abc\\src"))
        assertEquals("my-file.txt", getRelativePath(windowsPath, "C:\\users\\user-abc\\src\\"))

    }

    @Test
    fun test_getFilenameAndExtension_bulk(){
        assertEquals("test" to "txt", getFilenameAndExtension("test.txt"))
        assertEquals("hello.test" to "txt", getFilenameAndExtension("hello.test.txt"))
        assertEquals("test" to "", getFilenameAndExtension("test."))
        assertEquals("test" to "", getFilenameAndExtension("test"))
        assertEquals("" to "test", getFilenameAndExtension(".test"))
        assertEquals("test" to "", getFilenameAndExtension("test"))
        assertEquals("" to "", getFilenameAndExtension(""))
    }
}