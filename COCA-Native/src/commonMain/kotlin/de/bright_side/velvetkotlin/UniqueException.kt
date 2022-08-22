package de.bright_side.velvetkotlin

/**
 * adding UUIDs:
 *      1. install plugin "UUID Generator"
 *      2. type "# gen.uuid #" without spaces
 *      3. replace with uuid with IntelliJ context action
 *
 *
 * @param descriptionItems optional list of items associated with the exception.
 *      Example 1: the exception states that a duplicate key. Then the key may be put into the descriptionItems.
 *      Example 2: the exception states that a value in a file has the wrong format.
 *          The descriptionItems may then contain the value and the line number
 *      By using the descriptionItems it is possible classes that process the error such as a UI class can create a localized message
 *      which also contains the items
 */
class UniqueException(
    val uuid: String,
    message: String? = null,
    cause: Throwable? = null,
    val descriptionItems: List<String> = listOf(),
): Exception(message, cause){
    override fun toString(): String {
        return "UniqueException: uuid = '${uuid}', ${super.toString()}"
    }

    constructor(uuid: String,
                message: String,
                descriptionItems: List<Any>,
                cause: Throwable? = null,
                ) : this(uuid, message, cause, descriptionItems.map { it.toString()})
}

fun throwUniqueExceptionIf(
    uuid: String,
    message: String? = null,
    cause: Throwable? = null,
    descriptionItems: List<String> = listOf(),
    condition: () -> Boolean,
){
    if (condition()){
        throw UniqueException(uuid, message, cause, descriptionItems)
    }
}

fun assertUniqueExceptionInBlock(expectedUuid: String, f: () -> Unit){
    try{
        f()
    } catch (e: UniqueException){
        if (expectedUuid != e.uuid) {
            val messageString = if (e.message?.isNotEmpty() == true) " with message '${e.message}'" else " without message"
            val descriptionItemsString = if (e.descriptionItems.isEmpty()) " without description items" else " with description items = ${e.descriptionItems}"
            throw Exception("Expected uuid '$expectedUuid' but found uuid '${e.uuid}'$messageString$descriptionItemsString")
        }
        return
    }
    throw Exception("Expected exception with UUID '$expectedUuid' but no exception was thrown")
}

