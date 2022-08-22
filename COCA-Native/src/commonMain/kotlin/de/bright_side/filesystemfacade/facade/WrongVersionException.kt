package de.bright_side.filesystemfacade.facade

/**
 * @author Philip Heyse
 */
class WrongVersionException(message: String?) : Exception(message) {
    companion object {
        private const val serialVersionUID = -7290564309049346359L
    }
}