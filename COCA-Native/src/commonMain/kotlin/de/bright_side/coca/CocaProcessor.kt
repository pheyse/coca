package de.bright_side.coca

import de.bright_side.coca.CocaAction.*
import de.bright_side.coca.LogLevel.DEBUG
import de.bright_side.coca.PreviewFormat.BEGINNING_OF_TEXT
import de.bright_side.coca.PreviewFormat.HTML
import de.bright_side.filesystemfacade.facade.*
import de.bright_side.filesystemfacade.facade.IncludeExclude.EXCLUDE
import de.bright_side.filesystemfacade.facade.IncludeExclude.INCLUDE
import de.bright_side.filesystemfacade.memoryfs.MemoryFs
import de.bright_side.velvetkotlin.UniqueException

private fun createEmptyOptions(): CocaOptions = CocaOptions(PREVIEW, "", BEGINNING_OF_TEXT, "", DEBUG)
private fun createEmptyConfig() = CocaConfigReader().createEmptyConfig()
private fun createEmptyFileSystem() = MemoryFs()

open class CocaProcessor(
    protected var options: CocaOptions,
    protected var config: CocaConfig,
    protected var fileSystem: FsfSystem,
    protected var writeOutputLineHandler: (String) -> Unit = ::println,
    protected var logDebug: ((String) -> Unit)? = null,
) {
    /** for unit tests only */
    protected constructor() : this(createEmptyOptions(), createEmptyConfig(), createEmptyFileSystem())

    /** if a HTML preview file is written, then the output file is written externally and should not be overwritten
     * with status messages in this class*/
    private val outputFileWrittenExternally = ((options.action == PREVIEW) && (options.previewFormat == HTML))

    private val outputFile: FsfFile? = createOutputFile(fileSystem, options, outputFileWrittenExternally)
    private val bufferedOutput = StringBuilder()

    fun process(){
        bufferedOutput.clear()
        writeInfoLine("outputFileWrittenExternally: $outputFileWrittenExternally")
        if (options.action != WRITE_SAMPLE_CONFIG_FILE){
            val occurrences = findAllOccurrencesInSourceDir()
            performAction(occurrences)
            if (!outputFileWrittenExternally){
                outputFile?.writeString(bufferedOutput.toString())
            }
        } else {
            performAction(listOf())
        }
    }

    private fun performAction(occurrences: List<CommentOccurrence>) {
        when (options.action) {
            PREVIEW -> CocaPreviewPrinter(fileSystem, options, config, ::writeOutputLine).printPreview(occurrences)
            WRITE_SAMPLE_CONFIG_FILE -> CocaHelp().writeSampleConfigFile(fileSystem, options.outputPath)
            ARCHIVE -> CocaArchiver(options).archiveComments(fileSystem, config, occurrences, ::writeInfoLine)
        }
    }

    protected fun findAllOccurrencesInSourceDir(): List<CommentOccurrence> {
        val files = readFilteredSourceFiles()
        writeInfoLine("Source files matching filters: ${files.size}")
        val occurrences = findOccurrencesInFiles(files)
        writeInfoLine("Occurrences found: ${occurrences.size}")
        return occurrences
    }

    private fun findOccurrencesInFiles(files: List<FsfFile>): List<CommentOccurrence> {
        val result = mutableListOf<CommentOccurrence>()
        files.forEach { file ->
            val text = file.readString()
            result.addAll(findOccurrencesInText(text, file.absolutePath))
        }
        return result
    }

    private fun combineLists(items1: List<Pair<String, String>>, items2: List<Pair<String, String>>): List<Pair<String, String>>{
        val result = mutableListOf<Pair<String, String>>()
        result.addAll(items1)
        result.addAll(items2)
        return result
    }

    private fun findOccurrencesInText(text: String, filePath: String): List<CommentOccurrence> {
        val commentsToRemove = combineLists(config.blockCommentsToRemove, toPairListWithLineBreakAsEnding(config.lineCommentsToRemove))
        val commentsToKeep = combineLists(config.blockCommentsToKeep, toPairListWithLineBreakAsEnding(config.lineCommentsToKeep))
        return CocaParser().findOccurrencesToRemoveInText(text, filePath, commentsToRemove, commentsToKeep)
    }


    private fun toPairListWithLineBreakAsEnding(items: List<String>) = items.map { Pair(it, "\n") }

    protected fun readFilteredSourceFiles(): List<FsfFile> {
        val pathFilter = createPathFilter(config.includePaths, config.excludePaths)
        logDebug?.invoke("readFilteredSourceFiles = pathFilter = ${pathFilter.joinToString("") { "\n - $it" }}")
        val files = fileSystem.createByPath(config.sourceRootPath).listFilesTree(pathFilter)
            ?: throw UniqueException("c8a7fee4-8b29-493e-8df1-90e8da6bf1c1", "Source dir '${config.sourceRootPath} is not a valid directory")

        writeInfoLine("Total files in source directory '${config.sourceRootPath}': ${files.size}")
        logDebug?.invoke("config.includePaths = ${config.includePaths}")
        logDebug?.invoke("Included file endings: ${config.includeFileEndings}")

        writeInfoList("Included paths: ", formatPathsFilters(config.includePaths), " - ", "")
        writeInfoList("Excluded paths: ", formatPathsFilters(config.excludePaths), " - ", "")
        writeInfoList("Included file endings: ", config.includeFileEndings.map{it})

        return files.filter {
            val path = it.absolutePath.replace("\\", "/").removePrefix(config.sourceRootPath).removePrefix("/")
            val matchesEnding = matchesEnding(path, config.includeFileEndings)
            logDebug?.invoke("readFilteredSourceFiles. path = '$path', = matchesEnding = $matchesEnding")
            return@filter matchesEnding && it.isFile
        }
    }

    protected fun createPathFilter(
        includePaths: List<StringFilter>,
        excludePaths: List<StringFilter>,
    ): List<PathFilterItem> {
        val result = mutableListOf<PathFilterItem>()
        result.addAll(includePaths.map { PathFilterItem(INCLUDE, it.filterText, mapMatchTypeToPathMatchType(it.matchType)) })
        result.addAll(excludePaths.map { PathFilterItem(EXCLUDE, it.filterText, mapMatchTypeToPathMatchType(it.matchType)) })

        return result
    }

    private fun mapMatchTypeToPathMatchType(matchType: StringMatchType): MatchType = when(matchType){
        StringMatchType.EXACT -> MatchType.EXACT
        StringMatchType.STARTS_WITH -> MatchType.STARTS_WITH
        StringMatchType.ENDS_WITH -> MatchType.ENDS_WITH
        StringMatchType.CONTAINS -> MatchType.CONTAINS
    }

    private fun formatPathsFilters(filters: List<StringFilter>): List<String> {
        return filters.map{ filter ->
            val typeString = when (filter.matchType){
                StringMatchType.EXACT -> "is"
                StringMatchType.STARTS_WITH -> "starts with"
                StringMatchType.ENDS_WITH -> "ends with"
                StringMatchType.CONTAINS -> "contains"
            }
            "$typeString '${filter.filterText}'"
        }
    }

    private fun writeInfoList(title: String, items: List<String>, itemPrefix: String = " - \"", itemSuffix: String = "\"") {
        writeInfoLine("$title")
        items.forEach {
            writeInfoLine("$itemPrefix$it$itemSuffix")
        }
    }

    /**
     * @return true if one of the endings matches
     */
    protected fun matchesEnding(string: String, endings: List<String>): Boolean {
        endings.forEach {
            if (string.endsWith(it)){
                return true
            }
        }
        return false
    }

    protected fun matches(string: String, filters: List<StringFilter>): Boolean {
        filters.forEach {
            val filterText = it.filterText
            when(it.matchType){
                StringMatchType.EXACT -> if (filterText == string) return true
                StringMatchType.STARTS_WITH -> if (string.startsWith(filterText)) return true
                StringMatchType.ENDS_WITH -> if (string.endsWith(filterText)) return true
                StringMatchType.CONTAINS -> if (string.contains(filterText)) return true
            }
        }
        return false
    }

    private fun writeInfoLine(message: String){
        writeOutputLine("$INFO_OUTPUT_PREFIX $message")
    }

    protected fun writeOutputLine(line: String){
        writeOutputLineHandler(line)
        bufferedOutput.append(line + "\n")
    }

    private fun createOutputFile(fileSystem: FsfSystem, options: CocaOptions, outputFileWrittenExternally: Boolean): FsfFile? {
        if (outputFileWrittenExternally){
            return null
        }

        if (options.outputPath.isEmpty()) {
            return null
        }
        val file = fileSystem / options.outputPath
        val parentDir = file.parentFile
        if (parentDir?.exists() != true){
            parentDir?.mkdirs()
        }
        if (file.parentFile?.exists() != true){
            throw UniqueException(
                "86820e78-2292-4098-826f-4c1fb7530cf2",
                "Could not find or create parent directory for output path '${options.outputPath}",
                listOf(options.outputPath)
            )
        }
        return file
    }

    companion object{
        const val INFO_OUTPUT_PREFIX = "INFO:"
    }
}