package de.bright_side.coca

import de.bright_side.basicyaml.BasicYaml
import de.bright_side.basicyaml.BasicYamlReader
import de.bright_side.filesystemfacade.facade.FsfFile
import de.bright_side.velvetkotlin.UniqueException
import de.bright_side.velvetkotlin.throwUniqueExceptionIf

private const val KEY_CONFIG_VERSION = "coca config version"
private const val KEY_ARCHIVE_ROOT_PATH = "archive root path"
private const val KEY_SOURCE_ROOT_PATH = "source root path"
private const val KEY_INCLUDE_PATHS = "include paths"
private const val KEY_EXCLUDE_PATHS = "exclude paths"
private const val KEY_INCLUDE_FILE_ENDINGS = "include file endings"
private const val KEY_BLOCK_COMMENTS_TO_REMOVE = "block comments to remove"
private const val KEY_BLOCK_COMMENTS_TO_KEEP = "block comments to keep"
private const val KEY_LINE_COMMENTS_TO_REMOVE = "line comments to remove"
private const val KEY_LINE_COMMENTS_TO_KEEP = "line comments to keep"

private const val BLOCK_COMMENT_START_AND_END_SEPARATOR = "..."


class CocaConfigReader {

    fun createEmptyConfig() = CocaConfig("", "", listOf(), listOf(), listOf(), listOf(), listOf(), listOf(), listOf())

    fun readConfig(file: FsfFile): CocaConfig{
        val yaml = readYaml(file).data

        val version = yaml[KEY_CONFIG_VERSION] ?: throw UniqueException("e6ef1ca8-6562-41d0-b53f-dab372c2c110", "Missing definition of '$KEY_CONFIG_VERSION'")
        val archiveRootPath = yaml[KEY_ARCHIVE_ROOT_PATH] ?: throw UniqueException("b2b59c5c-0c30-4367-94ec-8b76e2e4dfab", "Missing definition of '$KEY_ARCHIVE_ROOT_PATH'")
        val sourceRootPath = yaml[KEY_SOURCE_ROOT_PATH] ?: throw UniqueException("a5950dd4-eb32-4230-9c11-7233fccb72c1", "Missing definition of '$KEY_SOURCE_ROOT_PATH'")
        val includePaths = yaml[KEY_INCLUDE_PATHS] ?: throw UniqueException("a5950dd4-eb32-4230-9c11-7233fccb72c1", "Missing definition of '$KEY_INCLUDE_PATHS'")
        val excludePaths = yaml[KEY_EXCLUDE_PATHS] ?: throw UniqueException("c02fa15d-f80d-4fe7-b596-80789ae41230", "Missing definition of '$KEY_EXCLUDE_PATHS'")
        val includeFileEndings = yaml[KEY_INCLUDE_FILE_ENDINGS] ?: throw UniqueException("c02fa15d-f80d-4fe7-b596-80789ae41230", "Missing definition of '$KEY_INCLUDE_FILE_ENDINGS'")
        val blockCommentsToRemove = yaml[KEY_BLOCK_COMMENTS_TO_REMOVE] ?: throw UniqueException("825281e1-d261-4855-bf8d-b10acff1080a", "Missing definition of '$KEY_BLOCK_COMMENTS_TO_REMOVE'")
        val blockCommentsToKeep = yaml[KEY_BLOCK_COMMENTS_TO_KEEP] ?: throw UniqueException("b6a4f758-587e-4cb4-aa85-5e66aed4a7d8", "Missing definition of '$KEY_BLOCK_COMMENTS_TO_KEEP'")
        val lineCommentsToRemove = yaml[KEY_LINE_COMMENTS_TO_REMOVE] ?: throw UniqueException("47837935-74fe-46fd-b1b9-8281b4e75297", "Missing definition of '$KEY_LINE_COMMENTS_TO_REMOVE'")
        val lineCommentsToKeep = yaml[KEY_LINE_COMMENTS_TO_KEEP] ?: throw UniqueException("54c2452a-617e-4c73-9700-ddece942bb99", "Missing definition of '$KEY_LINE_COMMENTS_TO_KEEP'")

        throwUniqueExceptionIf("7227b151-e2e3-4f60-a844-8c4cf44605bc", "Value of '$KEY_CONFIG_VERSION' must be 1"){
            (version.size != 1) || ((version[0].toIntOrNull() ?:0) != 1)
        }
        throwUniqueExceptionIf("cd402137-9a01-4cd6-8e5f-d737455bbb2e", "Value of '$KEY_ARCHIVE_ROOT_PATH' may not be empty"){archiveRootPath.isEmpty()}
        throwUniqueExceptionIf("6e462760-8727-45aa-975e-bc0550a7408c", "'$KEY_ARCHIVE_ROOT_PATH' may only contain one item"){archiveRootPath.size != 1}
        throwUniqueExceptionIf("47db1ae9-d5c2-4a48-b41e-9ae77d66adf6", "Value of '$KEY_SOURCE_ROOT_PATH' may not be empty"){sourceRootPath.isEmpty()}
        throwUniqueExceptionIf("080eb868-bbc6-478f-9d9b-2e968c0ea71d", "'$KEY_SOURCE_ROOT_PATH' may only contain one item"){sourceRootPath.size > 1}
        throwUniqueExceptionIf("e8937f17-013d-44fe-af0b-137d3d9f81d1", "Value of '$KEY_INCLUDE_PATHS' may not be empty"){includePaths.isEmpty()}
        throwUniqueExceptionIf("09aa8d64-ff84-4659-8aed-fc0171a8726b", "Value of '$KEY_INCLUDE_FILE_ENDINGS' may not be empty"){includeFileEndings.isEmpty()}

        val blockCommentsToRemovePairs = blockCommentsToRemove.map{ commentStartAndEndToPair(it, "dbe09487-03e9-4351-a077-91baee4e4fc3")}
        val blockCommentsToKeepPairs = blockCommentsToKeep.map{ commentStartAndEndToPair(it, "f6da8fb1-0624-4a13-9b65-f6577bc1ba00")}

        val includePathFilters = parseFilters(replaceBackslash(includePaths), "75e07b35-3133-4552-871d-c761c23f3316", "Could not read filter in include paths")
        includePathFilters.forEach {
            if (it.matchType == StringMatchType.EXACT){
                throw UniqueException(
                    "068f785a-9a7e-4627-b24e-d2e22dfb8b65",
                    "Wrong include path '${it.filterText}': All include paths must contain an asterisk ('*'). Example: 'src/main/java/*'"
                )
            }
        }

        assertAllCommentStartsAreUnique(blockCommentsToRemovePairs, blockCommentsToKeepPairs, lineCommentsToRemove, lineCommentsToKeep)

        return CocaConfig(
            replaceBackslash(archiveRootPath[0].removeSuffix("/")),
            replaceBackslash(sourceRootPath[0].removeSuffix("/")),
            includePathFilters,
            parseFilters(replaceBackslash(excludePaths), "6ac45bbe-1b64-4c1f-9309-c50cf7e5654d", "Could not read filter in exclude paths"),
            includeFileEndings.map { parseFileFilterEnding(it) },
            blockCommentsToRemovePairs,
            blockCommentsToKeepPairs,
            lineCommentsToRemove,
            lineCommentsToKeep,
        )
    }

    private fun assertAllCommentStartsAreUnique(
        list1: List<Pair<String, String>>,
        list2: List<Pair<String, String>>,
        list3: List<String>,
        list4: List<String>,
    ) {
        val allStartTagsList = mutableListOf<String>()
        allStartTagsList.addAll(list1.map { it.first })
        allStartTagsList.addAll(list2.map { it.first })
        allStartTagsList.addAll(list3)
        allStartTagsList.addAll(list4)

        val processedStartTags = mutableSetOf<String>()
        allStartTagsList.forEach{
            if (processedStartTags.contains(it)){
                throw UniqueException(
                    "33c9ebff-272d-4027-bdcb-a3e7bcb28bd8",
                    "The start tags of all comments need to be unique, but '$it' occurs multiple times",
                    listOf(it)
                )
            }
            processedStartTags.add(it)
        }
    }

    fun parseFileFilterEnding(rawItem: String): String {
        val errorPrefix = "Wrong file ending '$rawItem'.  "
        val errorUuid = "99fdc7f9-4051-479f-8cbf-5298f05126b5"
        if (!rawItem.startsWith("*")){
            throw UniqueException(errorUuid, "${errorPrefix}File ending must start with '*.'")
        }
        if (countChars(rawItem, '*') > 1){
            throw UniqueException(errorUuid, "${errorPrefix}File ending may only contain 1 '*'.")
        }
        return rawItem.replace("*", "")
    }

    fun parseFilter(item: String, errorUuid: String, errorMessage: String): StringFilter {
        if (item == "*"){
            return StringFilter("", StringMatchType.CONTAINS)
        }
        if (item.isEmpty()){
            throw UniqueException(errorUuid, "$errorMessage: the filter may not be empty")
        }

        val amountAsterisk = countChars(item, '*')
        var plainText = item.replace("*", "")

        var matchType: StringMatchType
        if (item.startsWith("*") && item.endsWith("*") && (amountAsterisk == 2)){
            return StringFilter(plainText, StringMatchType.CONTAINS)
        } else if (amountAsterisk > 1){
            throw UniqueException(errorUuid, "$errorMessage: there may only be 1 '*' per filter")
        }

        if (amountAsterisk == 0){
            matchType = StringMatchType.EXACT
        } else if (item.startsWith("*")){
            matchType = StringMatchType.ENDS_WITH
        } else if (item.endsWith("*")){
            matchType = StringMatchType.STARTS_WITH
        } else {
            throw UniqueException(errorUuid, "$errorMessage: unexpected format: the '*' may only occur at the beginning and/or at the end.")
        }

        return StringFilter(plainText, matchType)
    }

    private fun parseFilters(items: List<String>, errorUuid: String, errorMessage: String) = items.map { parseFilter(it, errorUuid, errorMessage) }

    private fun countChars(text: String, charToCount: Char) = text.toList().filter { it == charToCount }.size

    private fun replaceBackslash(string: String) = string.replace("\\", "/")
    private fun replaceBackslash(strings: List<String>) = strings.map{it.replace("\\", "/")}

    private fun commentStartAndEndToPair(item: String, exceptionUuid: String): Pair<String, String> {
        var startAndEnd = item.split(BLOCK_COMMENT_START_AND_END_SEPARATOR)
        if (startAndEnd.size != 2){
            throw UniqueException(exceptionUuid, "Could not read comment start and end from '$item'.  Expected format 'start...end' as e.g. in '/*...*/'")
        }
        return Pair(startAndEnd[0], startAndEnd[1])
    }

    private fun readYaml(file: FsfFile): BasicYaml {
        throwUniqueExceptionIf("5b107cfb-31be-45d2-a0a0-50c2d4ed82c9", "There is no file at location '${file.absolutePath}'."){!file.exists()}
        throwUniqueExceptionIf("2991ed3b-75e3-4421-b638-6e56c43c03cf", "Location '${file.absolutePath}' does not point to a file."){!file.isFile}
        val yaml = file.readString()
        try {
            return BasicYamlReader().fromString(yaml)
        } catch (e: Exception) {
            throw UniqueException("1ca27e4f-87c9-47ab-809a-225a241b2381", "Could not parse basic YAML data from '${file.absolutePath}'.", e)
        }
    }
}