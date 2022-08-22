package de.bright_side.filesystemfacade.memoryfs

import de.bright_side.filesystemfacade.facade.FsfFile
import de.bright_side.filesystemfacade.facade.FsfSystem
import de.bright_side.filesystemfacade.facade.PathFilterItem
import de.bright_side.filesystemfacade.util.FsfFileUtil
import de.bright_side.filesystemfacade.util.ListDirFormatting
import de.bright_side.velvetkotlin.UniqueException


/**
 * @author Philip Heyse
 */
private const val LOGGING_ENABLED = false
class MemoryFile(private val memoryFS: MemoryFs, private val path: String) : FsfFile {

    private val currentTime: Long
        get() = memoryFS.environment.currentTimeMillis

    override fun mkdirs(): FsfFile {
        log("mkdirs: start")
        val item = memoryFS.getItem(absolutePath)
        log("mkdirs: check if item is null")
        if (item == null) {
            log("mkdirs: set parent item")
            val parentItem = parentFile as MemoryFile?
            log("mkdirs: check parent item null")
            if (parentItem != null) {
                log("mkdirs: check parent item exists")
                if (!parentItem.exists()) {
                    log("mkdirs: mkdirs for parent item")
                    parentItem.mkdirs()
                } else {
                    log("mkdirs: check parent item is file")
                    if (parentItem.isFile) {
                        //: cannot make any more dirs if a file is in the middle of the path
                        return this
                    }
                }
            } else {
                log("mkdirs: return recursive mkdirs")
                MemoryFile(memoryFS, MemoryFsUtil.getParentPath(absolutePath)!!).mkdirs()
            }
            log("mkdirs: call mkdir")
            mkdir()
        }
        log("mkdirs: completed")
        return this
    }

    override fun mkdir(): FsfFile {
        var item = memoryFS.getItem(absolutePath)
        if (item == null) {
            val time = currentTime
            item = MemoryFsItem(memoryFS, true, time, time)
            memoryFS.setItem(absolutePath, item)
            memoryFS.setExistenceInParentDir(absolutePath, true)
        }
        return this
    }

    /**
     * ensures that it is possible to write to the file destFile or
     * throws an Exception otherwise.  Example: if the parent directory does not exist
     * it is not possible to create a file in that directory
     * @throws an Exception if the parent directory does not exist
     */
    private fun assertPathOkToWrite(destFile: FsfFile){
        if (!parentIsRoot(destFile)){
            val parent = destFile.parentFile ?:throw UniqueException(
                "149693eb-52ab-4665-b056-1b32ecef470d",
                "File '${destFile.absolutePath}' has no parent",
                listOf(destFile.absolutePath)
            )
            if (!parent.exists()){
                throw UniqueException(
                    "b286c851-1284-418c-a0b2-66e810cfd11f",
                    "The parent directory of '${destFile.absolutePath}' does not exist",
                    listOf(destFile.absolutePath)
                )
            }
            if (!parent.isDirectory){
                throw UniqueException(
                    "00992bdb-1f25-4fc2-8900-a0d4176a3a85",
                    "The parent of '${destFile.absolutePath}' is not a directory",
                    listOf(destFile.absolutePath)
                )
            }
        }

    }

    override fun copyTo(destFile: FsfFile) {
        log("copyTo: from '" + absolutePath + "' to '" + destFile.absolutePath + "'")
        FsfFileUtil.verifyCopyPossible(this, destFile)
        if (memoryFS === destFile.fsfSystem) {

            assertPathOkToWrite(destFile)
            val item = memoryFS.getItem(absolutePath) ?: throw Exception("The file to be copied does not exist: '$absolutePath'")
            memoryFS.setItem(destFile.absolutePath, MemoryFsUtil.copy(item))
            memoryFS.setExistenceInParentDir(destFile.absolutePath, true)


        } else {
            throw Exception("Not implemented, yet.")
            //: maybe Sequence can be uses as substitution for Inputstream and Outstream.  Then use FSFFileUtil.copyViaStreams(this, destFile)
        }
    }

    /**
     * @return true if the parent is the root ("/") which is the case if the path doesn't contain any "/" after the first "/" as pos 0
     */
    private fun parentIsRoot(destFile: FsfFile) = (destFile.absolutePath.indexOf("/", 1) < 0)

    override fun getChild(name: String): FsfFile {
        if ((name.contains("\\")) || (name.contains("/"))){
            throw UniqueException(
                "5e7552bc-c177-4049-8db9-fb124da33c9d",
                "Illegal child name '$name'. The name may not contain '/' or '\\'.",
                listOf(name)
            )
        }
        return MemoryFile(memoryFS, absolutePath + MemoryFs.SEPARATOR + name)
    }

    override val name: String
        get() {
            val pos = absolutePath.lastIndexOf(MemoryFs.SEPARATOR)
            return if (pos < 0) "" else absolutePath.substring(pos + 1)
        }

    override val absolutePath: String = MemoryFsUtil.normalize(path)

    override val isFile: Boolean
        get() {
            val item = memoryFS.getItem(absolutePath) ?: return false
            return !item.isDir
        }

    override val isDirectory: Boolean
        get() {
            val item = memoryFS.getItem(absolutePath) ?: return false
            return item.isDir
        }

    override fun exists(): Boolean  = memoryFS.getItem(absolutePath) != null

    override val parentFile: FsfFile?
        get() {
            val parentPath = MemoryFsUtil.getParentPath(absolutePath) ?: return null
            return MemoryFile(memoryFS, parentPath)
        }

    override val fsfSystem: FsfSystem
        get() = memoryFS

    override val length: Long
        get() {
            val item = memoryFS.getItem(absolutePath)
            if (item == null || item.isDir) {
                return 0
            }
            return item.dataAsBytes.size.toLong()
        }

    override var timeLastModified: Long
        get() {
            val item = memoryFS.getItem(absolutePath) ?: return 0
            return item.timeLastModified
        }
        set(timeLastModified) {
            val item = memoryFS.getItem(absolutePath) ?: return
            item.timeLastModified = timeLastModified
        }

    override fun readString(): String {
        val item = memoryFS.getItem(absolutePath) ?: throw Exception("There is no file at location '$absolutePath'")
        return item.dataAsBytes.decodeToString()
    }

    private fun createMemoryFSItem() = MemoryFsItem(memoryFS, false, currentTime, currentTime)

    override fun writeString(string: String, append: Boolean): FsfFile {
        assertPathOkToWrite(this)
        val item = createMemoryFSItem()

        var fullString = if (append && exists()) readString() else ""
        fullString += string

        item.dataAsBytes = fullString.encodeToByteArray()

        memoryFS.setItem(absolutePath, item)
        memoryFS.setExistenceInParentDir(absolutePath, true);
        return this
    }

    override fun listDirAsString(formatting: ListDirFormatting): String {
        return FsfFileUtil.listDirAsString(this, formatting, null)
    }

    override fun listFiles(): List<FsfFile>? {
        val result: MutableList<FsfFile> = mutableListOf()
        val item = memoryFS.getItem(path)
        if (item == null || !item.isDir) {
            return null
        }
        for (i in memoryFS.getChildPathsOrEmpty(path)) {
            result.add(MemoryFile(memoryFS, i))
        }
        return result


    }

    override fun listFilesTree(filter: List<PathFilterItem>): List<FsfFile>? {
        return FsfFileUtil.listFilesTree(this, null, filter)
    }

    override fun compareTo(other: FsfFile?): Int {
        log("compareTo: this = $this. Other = $other")
        if (other == null) {
            return 1
        }
        val otherClassName = (other.fsfSystem::class.qualifiedName ?: return 1)
        log("compareTo: otherClassName = '$otherClassName'")
        val result: Int = otherClassName.compareTo(memoryFS::class.qualifiedName!!)
        log("compareTo: compare class name = $result")
        if (result != 0){
            return result
        }
        log("compareTo: this absolute path = '$absolutePath'")
        log("compareTo: other absolute path = '${other.absolutePath}'")
        return absolutePath.compareTo(other.absolutePath)
    }

    private fun log(message: String) {
        if (LOGGING_ENABLED) {
            println("MemoryFile> $message")
        }
    }

    override fun toString(): String {
        return "MemoryFile (path='$absolutePath'). ${super.toString()}"
    }

    override fun delete() {
        if ((exists()) && (isDirectory) && (listFiles()?.isNotEmpty() == true)) {
            throw Exception("Cannot delete '$absolutePath' because it is a non-empty directory")
        }
        if (memoryFS.getItem(path) != null){
            memoryFS.removeItem(path)
            memoryFS.setExistenceInParentDir(path, false)
        }
    }

    override fun hashCode(): Int {
        return absolutePath.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null){
            return false
        }
        if (other is MemoryFile){
            return compareTo(other) == 0
        } else {
            return false
        }
    }
}