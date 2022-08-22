package de.bright_side.basicyaml

import de.bright_side.velvetkotlin.UniqueException

private const val COMMENT_INDICATOR = "#"
private const val ITEM_INDICATOR = "- "
private const val KEY_INDICATOR = ":"

class BasicYamlReader {
    private data class ReadingState(var resultMap: MutableMap<String, List<String>>, var key: String, var values: MutableList<String>)

    /**
     * reads a simple YAML file where comments are ignored, only top level lists with "-" or in format "a: b" are
     * allowed
     */
    fun fromString(string: String): BasicYaml{
        val state = ReadingState(mutableMapOf(), "", mutableListOf())
        string.replace("\r", "").split("\n").withIndex().forEach { (row, fullLine) ->
            val line = fullLine.trim()
            try{
                if ((line.isNotEmpty()) && (!line.startsWith(COMMENT_INDICATOR))){
                    if (line.startsWith(ITEM_INDICATOR)){
                        readItemLine(line, state)
                    } else if (line.endsWith(KEY_INDICATOR)) {
                        readKeyLine(line, state)
                    } else if (isLineWithKeyAndValue(line)){
                        readKeyValueLine(line, state)
                    } else {
                        throw UniqueException("fe1ed9c1-a7e7-4900-9867-10c776f8e7b3", "Unexpected line that is not a comment, key or item")
                    }
                }
            } catch (e: UniqueException){
                val message = if (e.message != null) ": ${e.message}" else ""
                throw UniqueException(e.uuid, "Error in row ${row + 1}$message. Data: >>$line<<", e, e.descriptionItems)
            }
        }
        moveOpenItemIntoMap(state)
        return BasicYaml(state.resultMap)
    }

    private fun readKeyValueLine(line: String, state: BasicYamlReader.ReadingState) {
        val pos = line.indexOf(KEY_INDICATOR)
        val key = line.substring(0 until pos).trim()
        if (key.isEmpty()){
            throw UniqueException("3b32c294-5913-4d3e-9cfd-b67179b2e12e", "Could not read key in key-value line from line >>$line<<")
        }
        val value = line.substring(pos + 1).trim()
        moveOpenItemIntoMap(state)
        state.key = key
        state.values.add(value)
        moveOpenItemIntoMap(state)
    }

    private fun isLineWithKeyAndValue(line: String): Boolean {
        val pos = line.indexOf(KEY_INDICATOR)
        if (pos < 0) {
            return false
        }
        //: ensure no quotes in key
        val quotePos = line.indexOfAny(listOf("\"", "'"))
        return ((quotePos < 0) || (quotePos > pos))
    }

    private fun moveOpenItemIntoMap(state: ReadingState) {
        if (state.key.isEmpty()){
            return
        }
        if (state.resultMap.containsKey(state.key)){
            throw UniqueException("9102e8e3-ce70-4198-b866-6fdae4c12b63", "Key '${state.key} already exists")
        }
        state.resultMap[state.key] = state.values
        state.values = mutableListOf()
        state.key = ""
    }

    private fun readItemLine(line: String, state: ReadingState) {
        if (state.key.isEmpty()){
            throw UniqueException("b18ddc0d-184e-44c6-a960-34652bd12685", "Found item without key", listOf(line))
        }
        val value = line.removePrefix(ITEM_INDICATOR).trim()
        state.values.add(value)
    }

    private fun readKeyLine(line: String, state: ReadingState) {
        val key = line.removeSuffix(KEY_INDICATOR).trim()
        if (key.isEmpty()){
            throw UniqueException("1c78640d-bf99-46a5-9530-e62b266cc151", "Could not read key from line >>$line<<")
        }
        moveOpenItemIntoMap(state)
        state.key = key
    }
}