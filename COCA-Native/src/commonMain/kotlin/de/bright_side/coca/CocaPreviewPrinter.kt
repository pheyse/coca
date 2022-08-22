package de.bright_side.coca

import de.bright_side.filesystemfacade.facade.FsfFile
import de.bright_side.filesystemfacade.facade.FsfSystem

open class CocaPreviewPrinter(
    protected var fileSystem: FsfSystem,
    protected var options: CocaOptions,
    protected var config: CocaConfig,
    protected var writeOutputLine: ((String) -> Unit)? = null
) {
    data class TextSection(val text: String, val highlight: Boolean)

    fun printPreview(occurrences: List<CommentOccurrence>) {
        if (options.previewFormat == PreviewFormat.HTML){
            printHtmlPreview(fileSystem / options.outputPath, occurrences)
        }

        occurrences.forEach {
            when (options.previewFormat) {
                PreviewFormat.BEGINNING_OF_TEXT -> printPreviewBeginningOfLine(it)
                PreviewFormat.MULTILINE -> printPreviewMultiline(it)
                PreviewFormat.HTML -> {} //:case covered before
            }
        }
    }

    fun printHtmlPreview(outputFile: FsfFile, occurrences: List<CommentOccurrence>) {
        val filesToCommentsMap = CocaUtil.createFilesToCommentsMap(fileSystem, occurrences)
        outputFile.writeString(getHtmlFilePrefix())

        filesToCommentsMap.forEach {
            outputFile += getFileTitleHtml(it.key)
            printFileHtmlPreview(outputFile, it.key, it.value)
        }
        if (filesToCommentsMap.isEmpty()){
            outputFile += "<span class=\"plain\">(No comments to be removed)</span>"
        }

        outputFile += getHtmlFileSuffix()
    }

    fun getHighlightedAndPlainSections(fullFileText: String, comments: List<CommentOccurrence>): List<TextSection> {
        val result = mutableListOf<TextSection>()
        var pos = 0
        comments.forEach { comment ->
            result += TextSection(fullFileText.substring(pos until comment.pos.first), false)
            result += TextSection(comment.commentText, true)
            pos = comment.pos.last + 1
        }
        if (pos < fullFileText.length){
            result += TextSection(fullFileText.substring(pos), false)
        }
        return result
    }

    private fun printFileHtmlPreview(outputFile: FsfFile, file:FsfFile, comments: List<CommentOccurrence>) {
        val sections = getHighlightedAndPlainSections(file.readString(), comments)
        sections.forEach { section ->
            val cssClass = if (section.highlight) "occurrence" else "plain"
            val htmlPreparedText = section.text.replace("\r", "")
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace(" ", "&nbsp;")
                .replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;")
                .replace("\n", "<br/>")
            outputFile += "<span class=\"$cssClass\">$htmlPreparedText</span>\n"
        }
    }

    private fun printPreviewBeginningOfLine(occurrence: CommentOccurrence) {
        val maxPreviewLength = 40
        var preview = occurrence.commentText.replace("\r", "").replace("\n", "\\n")
        if (preview.length > maxPreviewLength) {
            preview = preview.take(maxPreviewLength - 3) + "..."
        }
        val path = CocaUtil.getRelativePath(occurrence.filePath, config.sourceRootPath)
        writeOutputLine?.invoke("$path:${occurrence.lines.first}> $preview")
    }


    private fun printPreviewMultiline(occurrence: CommentOccurrence) {
        val maxLinesStart = 2
        val maxLinesEnd = 2
        val commentTextIndent = "    "

        var preview = ""
        val maxLinesTotal = maxLinesStart + 1 + maxLinesEnd
        var commentCleaned = occurrence.commentText.replace("\r", "").replace("\t", "    ").removeSuffix("\n")
        val commentLines = commentCleaned.split("\n")

        if (commentLines.size <= maxLinesTotal){
            preview += commentTextIndent + commentLines.joinToString("\n$commentTextIndent")
        } else {
            preview += commentTextIndent + commentLines.slice(0 until maxLinesStart).joinToString("\n$commentTextIndent")
            preview += "\n$commentTextIndent[...]\n"
            val endFirstIndex = commentLines.size - maxLinesEnd
            preview += commentTextIndent + commentLines.slice(endFirstIndex until commentLines.size).joinToString("\n$commentTextIndent")
        }
        val path = CocaUtil.getRelativePath(occurrence.filePath, config.sourceRootPath)
        writeOutputLine?.invoke("$path:${occurrence.lines.first}-${occurrence.lines.last}>\n$preview")
    }

    private fun getHtmlFilePrefix(): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
            <style>
            body {color:white;background-color: black;}
            p, div, span    {font-family:monospace;}
            .file {
              background-color: white;
              color:black;
            }
            .plain {
              color: white;
            }
            .occurrence {
              background-color: #aa8800;
              color: white;
            }
            </style>
            </head>
            <body>
            <h1>COCA - Comment Occurrences</h1>
        """.trimIndent() + "\n"
    }

    private fun getHtmlFileSuffix(): String {
        return "\n" + """
            </body>
            </html> 
        """.trimIndent()
    }

    private fun getFileTitleHtml(file: FsfFile) = "\n<h2 class=\"file\">${file.absolutePath}</h2>\n"

}