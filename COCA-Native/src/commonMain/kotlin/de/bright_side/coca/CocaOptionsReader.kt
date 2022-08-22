package de.bright_side.coca

import de.bright_side.velvetkotlin.KVelvetUtil.endsWithAny
import de.bright_side.velvetkotlin.UniqueException

private const val KEY_PREFIX = "-"
private const val KEY_NAME_ACTION = "a"
private const val KEY_NAME_CONFIG_FILE_PATH = "c"
private const val KEY_NAME_OUTPUT_FILE_PATH = "o"
private const val KEY_PREVIEW_FORMAT = "f"
private val CONFIG_FILE_ENDINGS = listOf(".yaml", ".yml")

class CocaOptionsReader {
    private val actionStringToActionMap: Map<String, CocaAction> = createActionStringToActionMap()

    private fun createActionStringToActionMap(): Map<String, CocaAction> {
        val result = mutableMapOf<String, CocaAction>()
        result["a"] = CocaAction.ARCHIVE
        result["p"] = CocaAction.PREVIEW
        result["c"] = CocaAction.WRITE_SAMPLE_CONFIG_FILE
        return result
    }

    private fun createPreviewFormatParseMap(): Map<String, PreviewFormat> {
        val result = mutableMapOf<String, PreviewFormat>()
        result["b"] = PreviewFormat.BEGINNING_OF_TEXT
        result["m"] = PreviewFormat.MULTILINE
        result["h"] = PreviewFormat.HTML
        return result
    }

    fun readOptions(args: List<String>): CocaOptions{
        val argsMap = toArgsMap(args)

        val actionString = argsMap[KEY_NAME_ACTION] ?: throw UniqueException(
            "cfec0411-c12b-43ca-9626-fb8b48dc9c4d",
            "Missing action parameter '$KEY_PREFIX$KEY_NAME_ACTION'")

        val action = actionStringToActionMap[actionString] ?:throw UniqueException(
            "2c6284a3-d294-46dd-aee6-3448f5980bb4",
            "Unknown value for action. Possible values: ${actionStringToActionMap.keys}")

        val configPath = if (action == CocaAction.WRITE_SAMPLE_CONFIG_FILE) {
            ""
        } else{
            argsMap[KEY_NAME_CONFIG_FILE_PATH] ?: throw UniqueException(
                "76d8fc96-b3c9-4ebf-b4b5-77d1ea7ac524",
                "Missing config file path parameter '$KEY_PREFIX$KEY_NAME_CONFIG_FILE_PATH'")
        }

        if (!endsWithAny(configPath, CONFIG_FILE_ENDINGS)){
            throw UniqueException(
                "80ea782c-439f-4913-94f9-e11cf94667ea",
                "Config filename must have one of these endings: $CONFIG_FILE_ENDINGS", listOf(configPath))
        }

        val previewFormatParam = argsMap[KEY_PREVIEW_FORMAT] ?: "m"
        val previewFormat = createPreviewFormatParseMap()[previewFormatParam] ?: throw UniqueException(
            "d033059e-8934-4668-96db-ae6dff78e156",
            "Unknown preview format '$KEY_PREFIX$KEY_NAME_CONFIG_FILE_PATH': $previewFormatParam")

        val outputPath = argsMap[KEY_NAME_OUTPUT_FILE_PATH] ?: ""
        if (previewFormat == PreviewFormat.HTML){
            if (outputPath.isEmpty()){
                throw UniqueException(
                    "b3fc39f9-0596-43e1-a874-0987dc7c8e8c",
                    "If preview format is HTML, an output file needs to be specified.  Parameter: '$KEY_PREFIX$KEY_NAME_OUTPUT_FILE_PATH'")
            }
            if (!outputPath.endsWith(".html", ignoreCase = true)){
                throw UniqueException(
                    "210f78b8-8d38-4576-b233-b92f89478d87",
                    "If preview format is HTML the file ending of the output file must be '.html'")
            }
        }

        return CocaOptions(action, configPath, previewFormat, outputPath, LogLevel.OFF)
    }

    private fun toArgsMap(args: List<String>): Map<String, String>{
        val result: MutableMap<String, String> = mutableMapOf()
        var readKey = true
        var key = ""
        args.forEach {
            if (readKey){
                if (!it.startsWith(KEY_PREFIX)){
                    throw UniqueException("da8ece32-c8aa-4a42-9002-ff55affd3536", "A parameter name should start with ${KEY_PREFIX}, but parameter name is '$key'")
                }
                key = it.removePrefix((KEY_PREFIX))
                if (result.containsKey(key)){
                    throw UniqueException("cfec0411-c12b-43ca-9626-fb8b48dc9c4d", "The parameter name '$key' occurs multiple times")
                }
                readKey = false
            } else {
                result[key] = it
                readKey = true
            }
        }
        if (!readKey){
            throw UniqueException("3ee4dc56-35c3-4761-a867-68a70bbecf81", "No value given for parameter name '$key'")
        }
        return result
    }
}