package de.bright_side.filesystemfacade.facade

/**
 * @author Philip Heyse
 */
interface FsfSystem {
    fun createByPath(path: String): FsfFile
    val separator: String

    /** call >>myFileSystem / "/path/to/file.txt"<< for myFileSystem.createByPath("/path/to/file.txt") */
    operator fun div(path: String) = createByPath(path)

}