package de.bright_side.filesystemfacade.memoryfs

import de.bright_side.filesystemfacade.BaseFsTestAbstract

class MemoryFsTest: BaseFsTestAbstract() {
    override val testDirPath = "/"
    override fun createFs() = MemoryFs()


}