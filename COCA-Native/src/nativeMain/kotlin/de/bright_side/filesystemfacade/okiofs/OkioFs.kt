package de.bright_side.filesystemfacade.okiofs

import de.bright_side.filesystemfacade.facade.FsfFile
import de.bright_side.filesystemfacade.facade.FsfSystem
import okio.Path.Companion.toPath

/**
 * @author Philip Heyse
 */
class OkioFs: FsfSystem {
    private val separatorInternal = if (Platform.osFamily == OsFamily.WINDOWS) "\\" else "/"

    override fun createByPath(path: String): FsfFile {
        return OkioFile(this, path.toPath())
    }

    override val separator = separatorInternal

}