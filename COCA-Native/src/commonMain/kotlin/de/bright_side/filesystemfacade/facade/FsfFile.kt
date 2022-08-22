package de.bright_side.filesystemfacade.facade

import de.bright_side.filesystemfacade.util.ListDirFormatting

/**
 * @author Philip Heyse
 *
 * equals, compareTo and HashCode must be provided
 */
interface FsfFile : Comparable<FsfFile?> {
    fun mkdirs(): FsfFile
    fun mkdir(): FsfFile
    fun copyTo(destFile: FsfFile)
    fun getChild(name: String): FsfFile
    val name: String
    val absolutePath: String
    val isFile: Boolean
    val isDirectory: Boolean
    fun exists(): Boolean
    val parentFile: FsfFile?
    val fsfSystem: FsfSystem
    val length: Long
    fun delete()
    val timeLastModified: Long
    fun readString(): String
    fun writeString(string: String, append: Boolean = false): FsfFile
    fun listDirAsString(formatting: ListDirFormatting = ListDirFormatting()): String
    fun listFiles(): List<FsfFile>?

    /**
     * @param filter: if there are filters with INCLUDE: at least one of them must be matched.  None of the filters marked as EXCLUDE may be matched
     * @return list of files ordered by path
     */
    fun listFilesTree(filter: List<PathFilterItem> = listOf()): List<FsfFile>?

    fun appendString(string: String): FsfFile = writeString(string, true)

    /**
     * call >>myFile / "child.txt"<< for myFile.getChild("child.txt")
     * the call can also handle paths. Example >>myFile / "dir1/dir2/fileX.txt"<< is possible.
     * Wrong slashes (backslash instead of slash and vice versa) are leading unneeded slashes also accepted.
     * Example >>myFile / "/dir1\\dir2/fileX.txt"<< is possible.
     * */
    operator fun div(path: String): FsfFile{
        var result = this
        path.removePrefix("/").removePrefix("\\").split('\\', '/').forEach {
            result = result.getChild(it)
        }
        return result
    }

    /**
     * call >>myFile += "hello"<< to append the text "hello" to the file "myFile"
     * (or if the file is empty, just write the text "hello")
     */
    operator fun plusAssign(string: String){
        appendString(string)
    }
}

