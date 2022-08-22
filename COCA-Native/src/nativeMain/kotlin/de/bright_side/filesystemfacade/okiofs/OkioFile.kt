package de.bright_side.filesystemfacade.okiofs

import de.bright_side.filesystemfacade.facade.FsfFile
import de.bright_side.filesystemfacade.facade.FsfSystem
import de.bright_side.filesystemfacade.facade.PathFilterItem
import de.bright_side.filesystemfacade.util.FsfFileUtil
import de.bright_side.filesystemfacade.util.ListDirFormatting
import de.bright_side.velvetkotlin.KVelvetTime
import okio.FileSystem
import okio.Path
import okio.buffer

private const val LOGGING_ENABLED = false


/**
 * @author Philip Heyse
 */
class OkioFile(private val fs: OkioFs, private val path: Path): FsfFile {
    private val cachedAbsolutePath: String? = null
    private val pathAsString = path.toString()

    private fun log(message: String) = if (LOGGING_ENABLED) println("OkioFile> $message") else Unit

    override fun mkdir(): FsfFile {
        FileSystem.SYSTEM.createDirectory(path, false)
        return this
    }

    override fun mkdirs(): FsfFile {
        FileSystem.SYSTEM.createDirectories(path, false)
        return this
    }

    override fun copyTo(destFile: FsfFile) {
        FsfFileUtil.verifyCopyPossible(this, destFile)
        if (destFile is OkioFile){
            FileSystem.SYSTEM.copy(path, destFile.path)
        } else {
            throw Exception("Copying to file other than OkioFile not implemented")
        }
    }


    override val parentFile: FsfFile?
        get() {
            return OkioFile(fs, path.parent ?: return null)
        }
    override val fsfSystem: FsfSystem
        get() = fs
    override val length: Long
        get() = FileSystem.SYSTEM.metadata(path).size ?: 0

    override fun delete() {
        log("delete: meta data = ${FileSystem.SYSTEM.metadata(path)}")
        log("delete: exists() = ${exists()}")

        if ((exists()) && (isDirectory) && (listFiles()?.isNotEmpty() == true)) {
            throw Exception("Cannot delete '$absolutePath' because it is a non-empty directory")
        }
        FileSystem.SYSTEM.delete(path, false)
    }

    override val timeLastModified: Long
        get(){
            val time = FileSystem.SYSTEM.metadata(path).lastModifiedAtMillis ?: return 0
            return time + KVelvetTime.systemTimeZoneOffsetFromUtcMillis()
        }

    override fun readString(): String {
        //: option 1
        //: WARNING: DOES NOT PROPERLY CLOSE THE FILE SO THAT DELETE CAUSES PERMISSION DENIED  FileSystem.SYSTEM.read(path) {
        //: WARNING: DOES NOT PROPERLY CLOSE THE FILE SO THAT DELETE CAUSES PERMISSION DENIED      return readUtf8()
        //: WARNING: DOES NOT PROPERLY CLOSE THE FILE SO THAT DELETE CAUSES PERMISSION DENIED  }

        //: alternative
        //: WARNING: DOES NOT PROPERLY CLOSE THE FILE SO THAT DELETE CAUSES PERMISSION DENIED  FileSystem.SYSTEM.source(path).buffer().use {
        //: WARNING: DOES NOT PROPERLY CLOSE THE FILE SO THAT DELETE CAUSES PERMISSION DENIED      return it.readUtf8()
        //: WARNING: DOES NOT PROPERLY CLOSE THE FILE SO THAT DELETE CAUSES PERMISSION DENIED  }

        //: working
        var buffer = FileSystem.SYSTEM.source(path).buffer()
        try{
            return buffer.readUtf8()
        } finally{
            buffer.close()
        }
    }

    override fun writeString(string: String, append: Boolean): FsfFile {
        if (append){
            FileSystem.SYSTEM.appendingSink(path).buffer().writeUtf8(string).close()
        } else {
            FileSystem.SYSTEM.write(path) {
                writeUtf8(string).close()
            }
        }
        return this
    }

    override fun compareTo(other: FsfFile?): Int {
        if (other == null){
            return 1
        }
        if (other is OkioFile){
            return absolutePath.compareTo(other.absolutePath)
        } else {
            return 2
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null){
            return false
        }
        if (other is OkioFile){
            return compareTo(other) == 0
        } else {
            return false
        }
    }


    override fun exists(): Boolean {
        return FileSystem.SYSTEM.exists(path)
    }

    override val isFile: Boolean
        get() = FileSystem.SYSTEM.metadata(path).isRegularFile

    override val isDirectory: Boolean
        get() = FileSystem.SYSTEM.metadata(path).isDirectory

    override fun getChild(name: String): FsfFile {
        if ((name.contains("\\")) || (name.contains("/"))){
            throw Exception("Illegal child name '$name'. The name may not contain '/' or '\\'.")
        }
        return OkioFile(fs, path.resolve(name))
    }

    override val name: String
        get() = path.name

    fun isPathAbsolute(): Boolean{
        val pathString = pathAsString
        //: on UNIX, Linux, etc.: the path needs to start with /
        if (pathString.startsWith("/")){
            return true
        }
        //: special case for windows:
        var part = pathString.substring(1)
        if (part.isEmpty()){
            return false
        }
        if (!pathString[0].isLetter()){
            return false
        }
        if (part.startsWith(":\\")){
            return true
        }
        return false
    }

    override val absolutePath: String
        get() {
            if (cachedAbsolutePath != null){
                return cachedAbsolutePath
            }

            try{
                if (isPathAbsolute()){
                    return pathAsString
                }
                return FileSystem.SYSTEM.canonicalize(path).toString()
            } catch (e: Throwable){
                throw Exception("Could not get absolute path from file with path '$path'", e)
            }
        }

    override fun listDirAsString(formatting: ListDirFormatting): String {
        return FsfFileUtil.listDirAsString(this, formatting, null)
    }

    override fun listFiles(): List<FsfFile>? {
        if (!isDirectory){
            return null
        }
        return FileSystem.SYSTEM.list(path).map { childPath ->
            OkioFile(fs, childPath)
        }
    }

    override fun listFilesTree(filter: List<PathFilterItem>): List<FsfFile>? {
        if (!isDirectory){
            return null
        }
        val basePath = absolutePath
        return FileSystem.SYSTEM.listRecursively(path).mapNotNull {
            if (FsfFileUtil.matchesPathFilter(basePath, it.toString(), filter)) OkioFile(fs, it) else null
        }.toMutableList().sortedBy { it.absolutePath }
    }

    override fun hashCode(): Int {
        return absolutePath.hashCode()
    }

    override fun toString() = "OkioFile(path='$pathAsString')"
}