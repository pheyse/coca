package de.bright_side.filesystemfacade.memoryfs

/**
 * contains the actual data and properties of the file while MemoryFile will be reading from this.
 * It is required because it is possible to create a MemoryFile instance which does not exist in the FileSystem.
 * Also there may be multiple MemoryFile instances which point to the same MemoryFSItem and the MemoryFSItem may change.
 *
 * @author Philip Heyse
 */
class MemoryFsItem(memoryFS: MemoryFs, isDir: Boolean, timeLastModified: Long, timeCreated: Long) {
    private var memoryFS: MemoryFs
    var isDir: Boolean
    var timeLastModified: Long
    var timeCreated: Long
    var dataAsBytes: ByteArray = ByteArray(0)

    init {
        this.memoryFS = memoryFS
        this.isDir = isDir
        this.timeLastModified = timeLastModified
        this.timeCreated = timeCreated
    }

    fun getMemoryFS(): MemoryFs {
        return memoryFS
    }

    fun setMemoryFS(memoryFS: MemoryFs) {
        this.memoryFS = memoryFS
    }
}