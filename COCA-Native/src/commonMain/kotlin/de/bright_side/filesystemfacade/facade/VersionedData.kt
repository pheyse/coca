package de.bright_side.filesystemfacade.facade

/**
 * @author Philip Heyse
 */
class VersionedData<K>(var version: Long, var data: K)