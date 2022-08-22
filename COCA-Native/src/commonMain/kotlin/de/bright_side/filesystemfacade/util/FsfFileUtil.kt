package de.bright_side.filesystemfacade.util

import de.bright_side.filesystemfacade.facade.*
import de.bright_side.filesystemfacade.util.ListDirFormatting.Style.TREE
import de.bright_side.filesystemfacade.util.ListDirFormatting.Style.FULL_PATH
import de.bright_side.velvetkotlin.SubstituteSortedMap
import de.bright_side.velvetkotlin.KVelvetTime

/**
 * @author Philip Heyse
 */
object FsfFileUtil {
    private const val STRING_ENCODING = "UTF-8"
    private const val LOGGING_ENABLED = false

    /**
     *
     * @param fs file system of which the files should be listed
     * @return the file listing as a string
     * @throws Exception on general error
     */
    private fun listDirFormattingSimple(fs: FsfSystem): String {
        val formatting = ListDirFormatting()
        formatting.setStyle(TREE)
        formatting.setAllSubItems(true)
        return fs.createByPath("").listDirAsString(formatting)
    }

    private fun createDefaultListDirFormatting(): ListDirFormatting {
        val result = ListDirFormatting()
        result.setAllSubItems(false)
        result.setIncludeSize(false)
        result.setIncludeTime(false)
        result.setStyle(TREE)
        return result
    }

    fun listDirAsString(file: FsfFile, formatting: ListDirFormatting?): String {
        return listDirAsString(file, formatting, null)
    }

    fun listDirAsString(file: FsfFile, formatting: ListDirFormatting?, filenamesToSkip: Set<String>?): String {
        return listDirAsString(file, formatting, filenamesToSkip, "").toString()
    }

    private fun listDirAsString(
        file: FsfFile,
        formatting: ListDirFormatting?,
        filenamesToSkip: Set<String>?,
        indent: String
    ): StringBuilder {
        var useFormatting: ListDirFormatting? = formatting
        if (useFormatting == null) {
            useFormatting = createDefaultListDirFormatting()
        }
        val result = StringBuilder()
        val files: List<FsfFile> = file.listFiles() ?: return StringBuilder("(path does not exist: '" + file.absolutePath + "')")

        val filesOfTypeFileList = mutableListOf<FsfFile>()
        val filesOfTypeDirList = mutableListOf<FsfFile>()

        for (i in files) {
            if (filenamesToSkip == null || !filenamesToSkip.contains(i.name)) {
                if (i.isDirectory) {
                    filesOfTypeDirList.add(i)
                } else {
                    filesOfTypeFileList.add(i)
                }
            }
        }

        filesOfTypeDirList.sortBy { it.name }
        filesOfTypeFileList.sortBy { it.name }

        val allFiles: LinkedHashMap<String, FsfFile> = LinkedHashMap()
        filesOfTypeDirList.forEach {
            allFiles.put(it.name, it)
        }
        filesOfTypeFileList.forEach {
            allFiles.put(it.name, it)
        }

        for (i in allFiles) {
            val fileItem: FsfFile = i.value
            val filename: String = i.key
            val type = if (fileItem.isDirectory) "<D>" else "<F>"
            when (useFormatting.style) {
                FULL_PATH -> result.append(fileItem.absolutePath + " " + type)
                TREE -> result.append("$indent$type $filename")
            }
            if (useFormatting.isIncludeSize) {
                result.append(" | " + fileItem.length)
            }
            if (useFormatting.isIncludeTime) {
                result.append(" | " + getTimeLastModifiedString(fileItem))
            }
            result.append("\n")
            if (useFormatting.isAllSubItems && fileItem.isDirectory) {
                result.append(listDirAsString(fileItem, useFormatting, filenamesToSkip, "$indent   "))
            }
        }
        return result
    }

    private fun getTimeLastModifiedString(file: FsfFile): String? {
        try {
            KVelvetTime.toIsoString(file.timeLastModified)
        } catch (e: Exception) {
            return "?"
        }
        return null
    }

    fun verifyCopyPossible(source: FsfFile?, dest: FsfFile?) {
        if (source == null) {
            throw Exception("Source file is null")
        }
        if (dest == null) {
            throw Exception("Dest file is null")
        }
        if (!source.exists()) {
            throw Exception("Source file does not exist: '" + source.absolutePath + "'")
        }
        if (source.isDirectory && !(source.listFiles()!!.isEmpty())) {
            throw Exception("Cannot from '${source.absolutePath}' copy to '" + dest.absolutePath + "' because source is non-empty directory. Do you want to use the copyTree method?")
        }
        if (dest.exists() && dest.isDirectory && !dest.listFiles()!!.isEmpty()) {
            throw Exception("Cannot copy to '" + dest.absolutePath + "' because destination is non-empty directory.")
        }
    }

    /**
     * @param file file object that holds the directory of which the file tree should be listed
     * @param filenamesToSkip list of filenames to be skipped (e.g. history directories)
     * @param filter: if there are filters with INCLUDE: one of them must be matched.  None of the filters marked as EXCLUDE may be matched
     * @return all sub-items recursively and as a list ordered by path
     * @throws Exception on general error
     */
    fun listFilesTree(
        file: FsfFile,
        filenamesToSkip: Set<String>? = null,
        filter: List<PathFilterItem> = listOf(),
    ): List<FsfFile> {
        var unsortedItems: List<FsfFile> = ArrayList()
        if (file.isDirectory) {
            unsortedItems = listFilesTreeUnsorted(file.listFiles(), filenamesToSkip)
        }

        //: don't use collections sort, because in the special case of encrypted files with encrypted file names
        //: there may be multiple file names that actually mean the same plain file name
        //: hence getAbsolutePath() must be used which created an uncrypted version of the path
        val pathToItemMap: SubstituteSortedMap<String, FsfFile> = SubstituteSortedMap()
        val basePath = file.absolutePath
        for (i in unsortedItems) {
            if (matchesPathFilter(basePath, i.absolutePath, filter)) {
                pathToItemMap[i.absolutePath] = i
            }
        }
        return pathToItemMap.values.toList()
    }

    private fun listFilesTreeUnsorted(file: List<FsfFile>?, filenamesToSkip: Set<String>?): List<FsfFile> {
        val result: MutableList<FsfFile> = mutableListOf()
        if (file == null) {
            return result
        }
        for (i in file) {
            if (filenamesToSkip == null || !filenamesToSkip.contains(i.name)) {
                result.add(i)
                if (i.isDirectory) {
                    result.addAll(listFilesTreeUnsorted(i.listFiles(), filenamesToSkip))
                }
            }
        }
        return result
    }

    private fun log(message: String) {
        if (LOGGING_ENABLED) {
            println("FSFFileUtil> $message")
        }
    }

    fun createDefaultEnvironment(): FsfEnvironment {
        return object : FsfEnvironment {
            override val currentTimeMillis: Long
                get() = KVelvetTime.currentTimeMillis()
        }
    }

    fun compareString(string1: String?, string2: String?): Int {
        if (string1 == null) {
            return if (string2 == null) 0 else -1
        }
        return if (string2 == null) 1 else string1.compareTo(string2)
    }

    /**
     * @param basePath the full base path. Example: "/users/abc"
     * @param path the full path of the file to match. Example: "/users/abc/my file"
     * @param filter: if there are filters with INCLUDE: one of them must be matched.  None of the filters marked as EXCLUDE may be matched
     */
    fun matchesPathFilter(basePath: String, path: String, filter: List<PathFilterItem>): Boolean {
        val subPath = path.substring(basePath.length)
        if (filter.isEmpty()) {
            return true
        }
        if (!matchesIncludes(subPath, filter)) {
            return false
        }
        if (!matchesExcludes(subPath, filter)) {
            return false
        }
        return true
    }

    private fun matchesIncludes(path: String, filter: List<PathFilterItem>): Boolean {
        val includes = filter.filter { it.includeExclude == IncludeExclude.INCLUDE }
        if (includes.isEmpty()) {
            return true
        }
        includes.forEach {
            if (matchesFilterItem(path, it.matchType, it.filterText)) {
                return true
            }
        }
        return false
    }

    private fun matchesFilterItem(givenText: String, matchType: MatchType, filterText: String): Boolean {
        when (matchType) {
            MatchType.EXACT -> if (filterText == givenText) return true
            MatchType.STARTS_WITH -> if (givenText.startsWith(filterText)) return true
            MatchType.ENDS_WITH -> if (givenText.endsWith(filterText)) return true
            MatchType.CONTAINS -> if (givenText.contains(filterText)) return true
        }
        return false
    }

    private fun matchesExcludes(path: String, filter: List<PathFilterItem>): Boolean {
        val excludes = filter.filter { it.includeExclude == IncludeExclude.EXCLUDE }
        if (excludes.isEmpty()) {
            return true
        }
        excludes.forEach {
            if (matchesFilterItem(path, it.matchType, it.filterText)) {
                return false
            }
        }
        return true
    }
}

