package de.bright_side.filesystemfacade.memoryfs

/**
 * @author Philip Heyse
 */
object MemoryFsUtil {

    fun getParentPath(path: String): String? {
        val pos: Int = path.lastIndexOf(MemoryFs.SEPARATOR)
        return if (pos < 0) null else path.substring(0, pos)
    }

    fun normalize(path: String): String {
        var result = path
        if (result.endsWith(MemoryFs.SEPARATOR)) {
            result = result.substring(0, result.length - 1)
        }
        result = result.replace("\\", MemoryFs.SEPARATOR)
        return result
    }

    fun copy(item: MemoryFsItem): MemoryFsItem {
        val result = MemoryFsItem(item.getMemoryFS(), item.isDir, item.timeLastModified, item.timeCreated)
        result.dataAsBytes = item.dataAsBytes.copyOf()
        return result
    }

}