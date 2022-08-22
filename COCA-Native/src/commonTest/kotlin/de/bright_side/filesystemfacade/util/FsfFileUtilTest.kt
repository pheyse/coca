package de.bright_side.filesystemfacade.util

import de.bright_side.filesystemfacade.facade.IncludeExclude
import de.bright_side.filesystemfacade.facade.IncludeExclude.EXCLUDE
import de.bright_side.filesystemfacade.facade.IncludeExclude.INCLUDE
import de.bright_side.filesystemfacade.facade.MatchType
import de.bright_side.filesystemfacade.facade.MatchType.*
import de.bright_side.filesystemfacade.facade.PathFilterItem
import de.bright_side.filesystemfacade.util.FsfFileUtil.matchesPathFilter
import kotlin.test.Test
import kotlin.test.assertEquals

class FsfFileUtilTest {

    private fun Pfi(includeExclude: IncludeExclude, filterText: String, matchType: MatchType)
        = PathFilterItem(includeExclude, filterText, matchType)
    private fun mpf(basePath: String, path: String, filter: List<PathFilterItem>) = matchesPathFilter(basePath, path, filter)

    @Test
    fun test_matches_include_bulk() {
        listOf("", "/", "abc", "/users", "/users/").forEach { basePath ->
            val msg = "error for base path '$basePath'"
            assertEquals(true, mpf(basePath, "${basePath}hello", listOf(Pfi(INCLUDE, "hello", EXACT))), msg)
            assertEquals(true, mpf(basePath, "${basePath}hello", listOf(Pfi(INCLUDE, "hello", STARTS_WITH))), msg)
            assertEquals(true, mpf(basePath, "${basePath}hello", listOf(Pfi(INCLUDE, "hello", ENDS_WITH))), msg)
            assertEquals(true, mpf(basePath, "${basePath}hello", listOf(Pfi(INCLUDE, "hello", CONTAINS))), msg)

            assertEquals(false, mpf(basePath, "${basePath}XhelloX", listOf(Pfi(INCLUDE, "hello", EXACT))), msg)
            assertEquals(false, mpf(basePath, "${basePath}XhelloX", listOf(Pfi(INCLUDE, "hello", STARTS_WITH))), msg)
            assertEquals(false, mpf(basePath, "${basePath}XhelloX", listOf(Pfi(INCLUDE, "hello", ENDS_WITH))), msg)
            assertEquals(true, mpf(basePath, "${basePath}XhelloX", listOf(Pfi(INCLUDE, "hello", CONTAINS))), msg)

            assertEquals(false, mpf(basePath, "${basePath}Xhello", listOf(Pfi(INCLUDE, "hello", EXACT))), msg)
            assertEquals(false, mpf(basePath, "${basePath}Xhello", listOf(Pfi(INCLUDE, "hello", STARTS_WITH))), msg)
            assertEquals(true, mpf(basePath, "${basePath}Xhello", listOf(Pfi(INCLUDE, "hello", ENDS_WITH))), msg)
            assertEquals(true, mpf(basePath, "${basePath}Xhello", listOf(Pfi(INCLUDE, "hello", CONTAINS))), msg)

            assertEquals(false, mpf(basePath, "${basePath}helloX", listOf(Pfi(INCLUDE, "hello", EXACT))), msg)
            assertEquals(true, mpf(basePath, "${basePath}helloX", listOf(Pfi(INCLUDE, "hello", STARTS_WITH))), msg)
            assertEquals(false, mpf(basePath, "${basePath}helloX", listOf(Pfi(INCLUDE, "hello", ENDS_WITH))), msg)
            assertEquals(true, mpf("", "${basePath}helloX", listOf(Pfi(INCLUDE, "hello", CONTAINS))), msg)

            assertEquals(false, mpf(basePath, "${basePath}there", listOf(Pfi(INCLUDE, "hello", EXACT))), msg)
            assertEquals(false, mpf(basePath, "${basePath}there", listOf(Pfi(INCLUDE, "hello", STARTS_WITH))), msg)
            assertEquals(false, mpf(basePath, "${basePath}there", listOf(Pfi(INCLUDE, "hello", ENDS_WITH))), msg)
            assertEquals(false, mpf(basePath, "${basePath}there", listOf(Pfi(INCLUDE, "hello", CONTAINS))), msg)
        }
    }

    @Test
    fun test_matches_exclude_bulk() {
        assertEquals(false, mpf("", "hello", listOf(Pfi(EXCLUDE, "hello", EXACT))))
        assertEquals(false, mpf("", "hello", listOf(Pfi(EXCLUDE, "hello", STARTS_WITH))))
        assertEquals(false, mpf("", "hello", listOf(Pfi(EXCLUDE, "hello", ENDS_WITH))))
        assertEquals(false, mpf("", "hello", listOf(Pfi(EXCLUDE, "hello", CONTAINS))))

        assertEquals(true, mpf("", "XhelloX", listOf(Pfi(EXCLUDE, "hello", EXACT))))
        assertEquals(true, mpf("", "XhelloX", listOf(Pfi(EXCLUDE, "hello", STARTS_WITH))))
        assertEquals(true, mpf("", "XhelloX", listOf(Pfi(EXCLUDE, "hello", ENDS_WITH))))
        assertEquals(false, mpf("", "XhelloX", listOf(Pfi(EXCLUDE, "hello", CONTAINS))))

        assertEquals(true, mpf("", "Xhello", listOf(Pfi(EXCLUDE, "hello", EXACT))))
        assertEquals(true, mpf("", "Xhello", listOf(Pfi(EXCLUDE, "hello", STARTS_WITH))))
        assertEquals(false, mpf("", "Xhello", listOf(Pfi(EXCLUDE, "hello", ENDS_WITH))))
        assertEquals(false, mpf("", "Xhello", listOf(Pfi(EXCLUDE, "hello", CONTAINS))))

        assertEquals(true, mpf("", "helloX", listOf(Pfi(EXCLUDE, "hello", EXACT))))
        assertEquals(false, mpf("", "helloX", listOf(Pfi(EXCLUDE, "hello", STARTS_WITH))))
        assertEquals(true, mpf("", "helloX", listOf(Pfi(EXCLUDE, "hello", ENDS_WITH))))
        assertEquals(false, mpf("", "helloX", listOf(Pfi(EXCLUDE, "hello", CONTAINS))))

        assertEquals(true, mpf("", "there", listOf(Pfi(EXCLUDE, "hello", EXACT))))
        assertEquals(true, mpf("", "there", listOf(Pfi(EXCLUDE, "hello", STARTS_WITH))))
        assertEquals(true, mpf("", "there", listOf(Pfi(EXCLUDE, "hello", ENDS_WITH))))
        assertEquals(true, mpf("", "there", listOf(Pfi(EXCLUDE, "hello", CONTAINS))))
    }

    @Test
    fun test_matchCombinationsBulk() {
        assertEquals(true, matchesPathFilter("", "hello", listOf()))
        //: I = include, X = exclude, M = match, N = no match
        val filterIM1 = PathFilterItem(INCLUDE, "hello", STARTS_WITH)
        val filterIN1 = PathFilterItem(INCLUDE, "Xhello", STARTS_WITH)
        val filterIM2 = PathFilterItem(INCLUDE, "hel", STARTS_WITH)
        val filterIN2 = PathFilterItem(INCLUDE, "yhel", STARTS_WITH)
        assertEquals(true, matchesPathFilter("", "hello", listOf(filterIM1)))
        assertEquals(false, matchesPathFilter("", "hello", listOf(filterIN1)))
        assertEquals(true, matchesPathFilter("", "hello", listOf(filterIM2)))
        assertEquals(false, matchesPathFilter("", "hello", listOf(filterIN2)))
        assertEquals(true, matchesPathFilter("", "hello", listOf(filterIM1, filterIN1)))
        assertEquals(true, matchesPathFilter("", "hello", listOf(filterIM1, filterIN1, filterIM2)))
        assertEquals(true, matchesPathFilter("", "hello", listOf(filterIM1, filterIN1, filterIM2, filterIN2)))
        assertEquals(true, matchesPathFilter("", "hello", listOf(filterIM1, filterIM2)))
        assertEquals(true, matchesPathFilter("", "hello", listOf(filterIM1, filterIN1, filterIM2)))
        assertEquals(true, matchesPathFilter("", "hello", listOf(filterIM1, filterIM2, filterIN2)))

        val filterXM1 = PathFilterItem(EXCLUDE, "hello", STARTS_WITH)
        val filterXN1 = PathFilterItem(EXCLUDE, "Xhello", STARTS_WITH)
        val filterXM2 = PathFilterItem(EXCLUDE, "hel", STARTS_WITH)
        val filterXN2 = PathFilterItem(EXCLUDE, "yhel", STARTS_WITH)
        assertEquals(false, matchesPathFilter("", "hello", listOf(filterXM1)))
        assertEquals(true, matchesPathFilter("", "hello", listOf(filterXN1)))
        assertEquals(false, matchesPathFilter("", "hello", listOf(filterXM1, filterXN1)))
        assertEquals(true, matchesPathFilter("", "hello", listOf(filterXN2)))
        assertEquals(false, matchesPathFilter("", "hello", listOf(filterXM1, filterXN1)))
        assertEquals(false, matchesPathFilter("", "hello", listOf(filterXM1, filterXN1, filterXM2)))
        assertEquals(false, matchesPathFilter("", "hello", listOf(filterXM1, filterXN1, filterXM2, filterXN2)))
        assertEquals(true, matchesPathFilter("", "hello", listOf(filterXN1, filterXN2)))
        assertEquals(false, matchesPathFilter("", "hello", listOf(filterXN1, filterXN2, filterXM1)))
        assertEquals(false, matchesPathFilter("", "hello", listOf(filterXN1, filterXN2, filterXM2)))
    }

    @Test
    fun test_matches_basePath_bulk() {
        val filterTest = PathFilterItem(INCLUDE, "/test", CONTAINS)
        val filterEnding = PathFilterItem(INCLUDE, ".txt", ENDS_WITH)
        val filterOne = PathFilterItem(INCLUDE, "one", CONTAINS)
        val filterOneX = PathFilterItem(EXCLUDE, "one", CONTAINS)

        assertEquals(true, matchesPathFilter("/users/one", "/users/one/test.txt", listOf(filterTest)))
        assertEquals(true, matchesPathFilter("/users/one", "/users/one/test.txt", listOf(filterEnding)))
        assertEquals(false, matchesPathFilter("/users/one", "/users/one/test.txt", listOf(filterOne)))
        assertEquals(false, matchesPathFilter("/users/one/", "/users/one/test.txt", listOf(filterOne)))
        assertEquals(true, matchesPathFilter("/users/", "/users/one/test.txt", listOf(filterOne)))
        assertEquals(true, matchesPathFilter("/users", "/users/one/test.txt", listOf(filterOne)))
        assertEquals(true, matchesPathFilter("/users/one", "/users/one/test-one.txt", listOf(filterOne)))
        assertEquals(true, matchesPathFilter("/users/one", "/users/one/test.txt", listOf(filterTest, filterEnding)))
        assertEquals(true, matchesPathFilter("/users/one", "/users/one/test.txt", listOf(filterTest, filterEnding, filterOneX)))
        assertEquals(false, matchesPathFilter("/users/", "/users/one/test.txt", listOf(filterTest, filterEnding, filterOneX)))
    }


}