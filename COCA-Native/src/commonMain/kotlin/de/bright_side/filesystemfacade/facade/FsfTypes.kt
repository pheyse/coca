package de.bright_side.filesystemfacade.facade

enum class MatchType{EXACT, STARTS_WITH, ENDS_WITH, CONTAINS}
enum class IncludeExclude{INCLUDE, EXCLUDE}
data class PathFilterItem(val includeExclude: IncludeExclude, val filterText: String, val matchType: MatchType)