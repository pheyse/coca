package de.bright_side.coca

import kotlinx.serialization.Serializable

/**
 * @param archiveRootPath never ends with "\\" or "/"
 * @param sourceRootPath never ends with "\\" or "/"
 */
data class CocaConfig(
    val archiveRootPath: String,
    val sourceRootPath: String,
    val includePaths: List<StringFilter>,
    val excludePaths: List<StringFilter>,
    val includeFileEndings: List<String>,
    val blockCommentsToRemove: List<Pair<String, String>>,
    val blockCommentsToKeep: List<Pair<String, String>>,
    val lineCommentsToRemove: List<String>,
    val lineCommentsToKeep: List<String>,
)

/**
      *x    -> endsWith
      x*    -> startsWith
      *x*   -> contains
      x     -> is
 */
data class StringFilter(val filterText: String, val matchType: StringMatchType)

data class CommentOccurrence(
    val filePath: String,
    val pos: IntRange,
    val lines: IntRange,
    val commentText: String,
)

@Serializable
data class SerializableIntRange(val first: Int, val last: Int)

@Serializable
data class SerializableCommentOccurrence(
    val filePath: String,
    val pos: SerializableIntRange,
    val lines: SerializableIntRange,
    val commentText: String,
)

@Serializable
data class FileCommentArchivingSummary(val relativePath: String, val items: List<SerializableCommentOccurrence>)



enum class StringMatchType{EXACT, STARTS_WITH, ENDS_WITH, CONTAINS}
enum class CocaAction{PREVIEW, ARCHIVE, WRITE_SAMPLE_CONFIG_FILE}
enum class PreviewFormat{BEGINNING_OF_TEXT, MULTILINE, HTML}
enum class LogLevel{DEBUG, OFF}

data class CocaOptions(
    val action: CocaAction,
    val configFilePath: String,
    val previewFormat: PreviewFormat,
    val outputPath: String,
    val logLevel: LogLevel,
)