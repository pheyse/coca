package de.bright_side.filesystemfacade.memoryfs

import de.bright_side.filesystemfacade.facade.FsfEnvironment
import de.bright_side.filesystemfacade.facade.FsfFile
import de.bright_side.filesystemfacade.facade.FsfSystem
import de.bright_side.filesystemfacade.util.FsfFileUtil
import de.bright_side.velvetkotlin.SubstituteSortedMap
import de.bright_side.velvetkotlin.SubstituteSortedSet

/**
 * @author Philip Heyse
 */

class MemoryFs constructor(val environment: FsfEnvironment = FsfFileUtil.createDefaultEnvironment()) : FsfSystem {
    /** if an entry is a dir, it at least contains the key with an empty list. The list items are full paths (and not just names) */
    private val pathToChildrenMap: SubstituteSortedMap<String, SubstituteSortedSet<String>> = SubstituteSortedMap()
    private val pathToItemsMap: SubstituteSortedMap<String, MemoryFsItem> = SubstituteSortedMap()

    constructor(getTimeMillis: () -> Long): this(object: FsfEnvironment{
        override val currentTimeMillis: Long
            get() = getTimeMillis()
    })

    override val separator: String
        get() = SEPARATOR

    init {
        pathToItemsMap[""] = MemoryFsItem(this, true, 0L, 0L)
        pathToChildrenMap[""] = SubstituteSortedSet()
    }

    override fun createByPath(path: String): FsfFile {
        return MemoryFile(this, MemoryFsUtil.normalize(path))
    }

    fun getItem(path: String): MemoryFsItem? = pathToItemsMap[path]

    fun setItem(path: String, item: MemoryFsItem) {
        pathToItemsMap[path] = item
        if (item.isDir) {
            if (!pathToChildrenMap.containsKey(path)) {
                pathToChildrenMap[path] = SubstituteSortedSet()
            }
        } else {
            pathToChildrenMap.remove(path)
        }
    }

    fun setExistenceInParentDir(path: String, exists: Boolean) {
        val parentPath = MemoryFsUtil.getParentPath(path) ?: throw Exception("Parent path of '$path' does not exist")
        if (!exists) {
            if (pathToChildrenMap.containsKey(parentPath)) {
                pathToChildrenMap[parentPath]?.remove(path)
            }
        } else {
            if (!pathToChildrenMap.containsKey(parentPath)) {
                pathToChildrenMap[parentPath] = SubstituteSortedSet()
            }
            pathToChildrenMap[parentPath]!!.add(path)
        }
    }

    fun getChildItemsOrEmpty(path: String?): List<MemoryFsItem> {
        val result: MutableList<MemoryFsItem> = mutableListOf()
        val children: Set<String> = pathToChildrenMap[path] ?: return result
        for (i in children) {
            result.add(pathToItemsMap[i]!!)
        }
        return result
    }

    fun getChildPathsOrEmpty(path: String): List<String> {
        return pathToChildrenMap[path]?.toList() ?: return listOf()
    }

    fun removeItem(path: String) {
        pathToItemsMap.remove(path);
        pathToChildrenMap.remove(path);
    }

    companion object {
        const val SEPARATOR = "/"
    }

}