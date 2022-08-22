package de.bright_side.filesystemfacade.util

/**
 * @author Philip Heyse
 */
class ListDirFormatting() {
    enum class Style {
        TREE, FULL_PATH
    }

    var isIncludeTime = false
    var isIncludeSize = false
    var isAllSubItems = true
    var style: Style = Style.TREE

    fun setIncludeTime(includeTime: Boolean): ListDirFormatting {
        isIncludeTime = includeTime
        return this
    }

    fun setIncludeSize(includeSize: Boolean): ListDirFormatting {
        isIncludeSize = includeSize
        return this
    }

    fun setAllSubItems(allSubItems: Boolean): ListDirFormatting {
        isAllSubItems = allSubItems
        return this
    }

    fun setStyle(style: Style): ListDirFormatting {
        this.style = style
        return this
    }
}