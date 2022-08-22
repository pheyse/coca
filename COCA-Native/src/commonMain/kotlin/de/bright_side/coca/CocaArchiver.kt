package de.bright_side.coca

import de.bright_side.coca.CocaUtil.createFilesToCommentsMap
import de.bright_side.coca.LogLevel.OFF
import de.bright_side.filesystemfacade.facade.FsfFile
import de.bright_side.filesystemfacade.facade.FsfSystem
import de.bright_side.velvetkotlin.KVelvetTime
import de.bright_side.velvetkotlin.KVelvetTime.currentTimeMillis
import de.bright_side.velvetkotlin.KVelvetUtil.formatEscapeChars
import de.bright_side.velvetkotlin.KVelvetUtil.highlightPosInString
import de.bright_side.velvetkotlin.UniqueException

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

const val CODE_ARCHIVE_DIR_NAME = "code-archive"
const val OPERATION_INDEX_DIR_NAME = "operation-index"
const val CHANGES_HTML_DIR_NAME = "changes-html"
const val SUMMARY_DIR_NAME = "summary"
const val OPERATION_INDEX_FILENAME_EXTENSION = ".dat"
const val SUMMARY_FILE_EXTENSION = ".json"
const val CHANGES_HTML_FILE_EXTENSION = ".html"

open class CocaArchiver(protected var options: CocaOptions) {
    private fun log(enabled: Boolean, message: String) = if ((options.logLevel != OFF) && (enabled)) log(message) else Unit
    private fun log(message: String) = if (options.logLevel != OFF) println("CocaArchiver> $message") else Unit
    /** allows overwriting the run id for testing purposes*/
    protected var overwrittenRunId:String? = null

    fun archiveComments(fileSystem: FsfSystem, config: CocaConfig, comments: List<CommentOccurrence>, writeOutputLine: ((String) -> Unit)? = null){
        val runId = createRunId()
        val filesToCommentsMap:Map<FsfFile, List<CommentOccurrence>> = createFilesToCommentsMap(fileSystem, comments)

        val output = mutableListOf<String>()
        output += ""
        output += (if (filesToCommentsMap.isEmpty()) "No occurrences found and no archiving needed." else "Archiving result:")
        output += ""

        writeHtmlFile(fileSystem, config, runId, comments)

        filesToCommentsMap.forEach { (file, comments) ->
            output += processFile(config, runId, file, comments)
        }
        output.forEach{ writeOutputLine?.invoke(it) }
    }

    private fun writeHtmlFile(fileSystem: FsfSystem, config: CocaConfig, runId: String, comments: List<CommentOccurrence>) {
        val htmlFile = (getArchiveRootDir(fileSystem, config) / CHANGES_HTML_DIR_NAME).mkdirs() / (runId + CHANGES_HTML_FILE_EXTENSION)
        CocaPreviewPrinter(fileSystem, createDummyOptions(), config).printHtmlPreview(htmlFile, comments)
    }

    private fun createDummyOptions() = CocaOptions(CocaAction.PREVIEW, "", PreviewFormat.BEGINNING_OF_TEXT, "", LogLevel.OFF)

    private fun createRunId(): String{
        overwrittenRunId?.let { return it }
        return KVelvetTime.toIsoString(currentTimeMillis(), true)
            .replace("'", "")
            .replace(":", "-")
            .replace(".", "_")
    }

    /**
     * @return list of strings containing the relative path and the amount of removed comments or file removed information
     */
    private fun processFile(config: CocaConfig, runId: String, file: FsfFile, comments: List<CommentOccurrence>): List<String> {
        val result = mutableListOf<String>()
        try{
            val cleanedText = getCleanedText(file, comments)
            copyToArchive(config, runId, file)
            result += addFileToIndex(config, runId, file, comments, cleanedText)
            addSummaryToArchive(config, runId, file, comments)
            updateFile(file, cleanedText)
        } catch (e: Throwable) {
            throw UniqueException("6d657fcb-0311-4afc-a749-8357afd09602",
                "Unexpected error while processing file '${file.absolutePath}':\n${e.stackTraceToString()}",
                listOf(file.absolutePath, e.stackTraceToString(), e)
            )
        }
        return result
    }

    protected fun addSummaryToArchive(config: CocaConfig, runId: String, file: FsfFile, comments: List<CommentOccurrence>) {
        val dir = (getArchiveRootDir(file.fsfSystem, config) / SUMMARY_DIR_NAME / runId).mkdirs()
        val relativePath = CocaUtil.getRelativePath(file.absolutePath, config.sourceRootPath).replace("\\", "/")
        val json = Json.encodeToString(toFileCommentArchivingSummary(relativePath, comments))
        log("addSummaryToArchive: file = >>${file.absolutePath}<<")
        log("addSummaryToArchive: json = >>$json<<")
        log("addSummaryToArchive: dir = $dir")
        val destFile = getSummaryDestFile(dir, file)
        log("addSummaryToArchive: destFile = >>${destFile.absolutePath}<<")
        destFile += json
    }

    private fun getSummaryDestFile(dir: FsfFile, file: FsfFile): FsfFile {
        var index = 0
        var candidate: FsfFile
        val (filename, _) = CocaUtil.getFilenameAndExtension(file.name)
        do{
            var indexString = if (index == 0) "" else "-$index"
            candidate = dir / "${filename}$indexString$SUMMARY_FILE_EXTENSION"
            index ++
        } while (candidate.exists())
        return candidate
    }

    private fun toFileCommentArchivingSummary(relativePath: String, comments: List<CommentOccurrence>): FileCommentArchivingSummary {
        val serializableComments = comments.map {
            SerializableCommentOccurrence(
                it.filePath,
                SerializableIntRange(it.pos.first, it.pos.last),
                SerializableIntRange(it.lines.first, it.lines.last),
                it.commentText )
        }
        return FileCommentArchivingSummary(relativePath, serializableComments)
    }

    protected fun addFileToIndex(config: CocaConfig, runId: String, file: FsfFile, comments: List<CommentOccurrence>, cleanedText: String): String {
        val destFile = (getArchiveRootDir(file.fsfSystem, config) / OPERATION_INDEX_DIR_NAME).mkdirs() / (runId + OPERATION_INDEX_FILENAME_EXTENSION)
        val relativePath = CocaUtil.getRelativePath(file.absolutePath, config.sourceRootPath).replace("\\", "/")
        val removedInfo = when {
            cleanedText.isEmpty() -> "file removed"
            comments.size == 1 -> "1 comment removed"
            else -> "${comments.size} comments removed"
        }
        val info = "$relativePath:$removedInfo"
        destFile += "$info\n"
        return info
    }

    private fun getArchiveRootDir(fileSystem: FsfSystem, config: CocaConfig): FsfFile {
        val archiveRootPath = config.archiveRootPath.replace("/", fileSystem.separator)
        return (fileSystem / archiveRootPath).mkdirs()
    }

    protected fun copyToArchive(config: CocaConfig, runId: String, file: FsfFile) {
        if (!file.exists()){
            throw UniqueException("d0b3c1ff-446e-43b8-8f9c-32738e620449", "File '${file.absolutePath}' does not exist", listOf(file.absolutePath))
        }
        val archiveFile = getFileLocationInArchive(config, runId, file)
        log("copyToArchive: file = $file")
        log("copyToArchive: dest = $archiveFile")
        archiveFile.parentFile?.mkdirs()
        file.copyTo(archiveFile)
    }

    private fun getFileLocationInArchive(config: CocaConfig, runId: String, file: FsfFile): FsfFile {
        val parentFile = file.parentFile ?: throw UniqueException("8710c540-9fd1-4082-b039-099fe3cc0181", "File '$file' has no parent dir", listOf(file))
        val baseDir = getArchiveRootDir(file.fsfSystem, config)
        val relativePath = CocaUtil.getRelativePath(parentFile.absolutePath, config.sourceRootPath).replace("/", file.fsfSystem.separator)

        log("getFileLocationInArchive. relativePath = '$relativePath'")

        val result = baseDir / CODE_ARCHIVE_DIR_NAME / relativePath / runId / file.name
        log("getFileLocationInArchive. result = $result")
        return result
    }

    private fun updateFile(file: FsfFile, cleanedText: String) {
        if (cleanedText.isEmpty()) {
            file.delete()
        } else {
            file.writeString(cleanedText)
        }
    }

    private fun getCleanedText(file: FsfFile, comments: List<CommentOccurrence>): String {
        val text = file.readString()
        return removeCommentsFromText(text, comments)
    }

    protected fun removeCommentsFromText(text: String, comments: List<CommentOccurrence>): String{
        var result = text
        val log = false
        log(log, "removeCommentsFromText: start")
        comments.sortedByDescending{it.pos.first}.forEach {comment ->
            log(log, "removeCommentsFromText: processing $comment")
            result = removeCommentFromText(result, comment)
        }
        log(log, "removeCommentsFromText: end")
        return result
    }

    /**
     * Starting BEFORE pos read backwards until the next line break and returns the text.
     * If there is no such text all the text from the beginning to pos is returned
     */
    protected fun readLineBackwardsUntil(text: String, pos: Int): String{
        val foundPos = text.lastIndexOf("\n", pos - 1)
        if (foundPos < 0){
            return text.substring(0 until pos)
        }
        return text.substring(foundPos + 1 until pos)
    }

    /**
     * Starting AFTER pos read forward until the next line break and returns the text.
     * If there is no such text an empty string is returned
     */
    protected fun readLineForwardUntil(text: String, pos: Int): String{
        if ((pos < text.length) && (text[pos] == '\n')){
            return ""
        }
        val foundPos = text.indexOf("\n", pos + 1)
        if (foundPos < 0){
            if (pos + 1 >= text.length){
                return ""
            }
            return text.substring(pos + 1)
        }
        return text.substring(pos + 1 until foundPos)
    }

    private fun getCharOrFallback(text: String, pos: Int, fallbackChar: Char = '_'): Char{
        if ((pos < 0) || (pos >= text.length)){
            return fallbackChar
        }
        return text[pos]
    }

    protected fun removeCommentFromText(text: String, comment: CommentOccurrence): String{
        val log = true
        val lineComment = comment.commentText.endsWith("\n")
        val lineBeforeComment = readLineBackwardsUntil(text, comment.pos.first)
        val lineAfterComment = if (lineComment) "" else readLineForwardUntil(text, comment.pos.last)

        log(log, "removeCommentFromText. lineComment = $lineComment")
        log(log, "removeCommentFromText. lineBeforeComment = >>${formatEscapeChars(lineBeforeComment)}<<")
        log(log, "removeCommentFromText. lineAfterComment = >>${formatEscapeChars(lineAfterComment)}<<")

        val lineBeforeOnlyWhitespace = lineBeforeComment.isBlank()
        val lineAfterOnlyWhitespace = lineAfterComment.isBlank()
        val commentHasMultipleLines = comment.lines.first < comment.lines.last

        log(log, "removeCommentFromText. lineBeforeOnlyWhitespace = $lineBeforeOnlyWhitespace, lineAfterOnlyWhitespace = $lineAfterOnlyWhitespace")

        var firstRemovePos = comment.pos.first
        var lastRemovePos = comment.pos.last

        log(log, "removeCommentFromText. text = >>${formatEscapeChars(text)}<<")
        log(log, "removeCommentFromText. initial firstRemovePos = ${highlightPosInString(text, firstRemovePos)}")
        log(log, "removeCommentFromText. initial lastRemovePos = ${highlightPosInString(text, lastRemovePos)}")
        log(log, "removeCommentFromText. commentHasMultipleLines = $commentHasMultipleLines")
        if (commentHasMultipleLines){
            if (lineBeforeOnlyWhitespace){
                firstRemovePos -= lineBeforeComment.length
            }
            if (lineAfterOnlyWhitespace){
                lastRemovePos += lineAfterComment.length
                if (lineBeforeOnlyWhitespace){
                    if (getCharOrFallback(text, lastRemovePos + 1) == '\r'){
                        log(log, "removeCommentFromText. multiline, remove \\r at the end")
                        lastRemovePos ++
                    }
                    if (getCharOrFallback(text, lastRemovePos + 1) == '\n'){
                        log(log, "removeCommentFromText. multiline, remove \\n at the end")
                        lastRemovePos ++
                    }
                }
            }
        } else {
            if (lineBeforeOnlyWhitespace && lineAfterOnlyWhitespace){
                firstRemovePos -= lineBeforeComment.length
                lastRemovePos += lineAfterComment.length
            }
        }

        log(log, "removeCommentFromText. final firstRemovePos = ${highlightPosInString(text, firstRemovePos)}")
        log(log, "removeCommentFromText. final lastRemovePos = ${highlightPosInString(text, lastRemovePos)}")

        val start = if (firstRemovePos > 0) text.substring(0 until firstRemovePos).trimEnd(' ', '\t') else ""
        val end = if (lastRemovePos + 1 < text.length) text.substring(lastRemovePos + 1) else ""

        log(log, "removeCommentFromText. start = >>${formatEscapeChars(start)}<<")
        log(log, "removeCommentFromText. end = >>${formatEscapeChars(end)}<<")

        var spaceToSeparate = ""
        var lineBreakToKeep = ""
        if ((!lineBeforeOnlyWhitespace) && (lineComment)){
            log(log, "removeCommentFromText. text before and line comment -> keep line break")
            lineBreakToKeep = "\n"
        }
        if ((!lineBeforeOnlyWhitespace) && (!lineAfterOnlyWhitespace)){
            if (commentHasMultipleLines){
                log(log, "removeCommentFromText. text before and after and multiline -> keep line break")
                lineBreakToKeep = "\n"
            } else {
                log(log, "removeCommentFromText. text before and after and only 1 line -> space to separate")
                if ((!start.endsWith(" ")) && (!end.startsWith(" "))){
                    spaceToSeparate = " "
                }
            }
        }

        log(log, "removeCommentFromText. lineBreakToKeep = '$lineBreakToKeep'")
        log(log, "removeCommentFromText. spaceToSeparate = '${formatEscapeChars(spaceToSeparate)}'")

        return start + lineBreakToKeep + spaceToSeparate + end
    }

}