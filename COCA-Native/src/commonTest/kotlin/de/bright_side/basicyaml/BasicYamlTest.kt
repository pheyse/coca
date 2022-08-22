package de.bright_side.basicyaml

import de.bright_side.basicyaml.BasicYaml
import de.bright_side.basicyaml.BasicYamlReader
import de.bright_side.velvetkotlin.UniqueException
import de.bright_side.velvetkotlin.assertUniqueExceptionInBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BasicYamlTest {

    private fun createYamlSimple(): String {
        return """
        # my comment
        list a:
         - a 1
         
         - a 2
        list b:
         - b 1
        list c:
        list d:
         - d 1
         # other comment
         - d 2
        """.trimIndent()
    }

    private fun createYamlSingleItemLists(): String {
        return """
        # my comment
        list a:
         - a 1
        list b:
        # other comment
        list c:
         - c 1
        """.trimIndent()
    }

    private fun createYamlSingleItemLines(): String {
        return """
        # my comment
        list a: a 1
        list b:
        # other comment
        list c:c 1
        """.trimIndent()
    }

    private fun createYamlRealisticConfig(): String {
        return """
        # my comment
        archive root path:
         - C:\myArchive
        source root path:
         - C:\myProject
        include paths:
         - \src\main\java\*
         - \src\main\kotlin\*
         - \src\test\java\*
         - \src\test\kotlin\*
        exclude paths:
         - *\gen\*
         - *Test.*
        include file endings:
         - *.kt
         - *.java
        block comments to remove:
         - /*...*/
        block comments to keep:
         - /**...*/
        line comments to remove:
         - //
        line comments to keep:
         - //:
         - //*
        """.trimIndent()
    }

    @Test
    fun test_yamlSimple() {
        val result = BasicYamlReader().fromString(createYamlSimple()).data

        println("test_yamlSimple: keys = ${result.keys}")

        assertEquals(4, result.size)
        assertTrue(result.keys.contains("list a"), "Expected key 'list a'")
        assertTrue(result.keys.contains("list b"), "Expected key 'list b'")
        assertTrue(result.keys.contains("list c"), "Expected key 'list c'")
        assertTrue(result.keys.contains("list d"), "Expected key 'list d'")

        val listA = result["list a"] ?: throw Exception("Missing value")
        assertEquals(2, listA.size)
        assertTrue(listA.contains("a 1"), "Expected item a 1")
        assertTrue(listA.contains("a 2"), "Expected item a 2")

        val listB = result["list b"] ?: throw Exception("Missing value")
        assertEquals(1, listB.size)
        assertTrue(listB.contains("b 1"), "Expected item b 1")

        val listC = result["list c"] ?: throw Exception("Missing value")
        assertEquals(0, listC.size)

        val listD = result["list d"] ?: throw Exception("Missing value")
        assertEquals(2, listD.size)
        assertTrue(listD.contains("d 1"), "Expected item d 1")
        assertTrue(listD.contains("d 2"), "Expected item d 2")
    }

    @Test
    fun test_yamlRealisticConfig() {
        val result = BasicYamlReader().fromString(createYamlRealisticConfig()).data

        println("test_yamlRealisticConfig: keys = ${result.keys}")

        assertEquals(9, result.size)
        assertTrue(result.keys.contains("archive root path"), "Expected key 1")
        assertTrue(result.keys.contains("source root path"), "Expected key 2")
        assertTrue(result.keys.contains("include paths"), "Expected key 3")
        assertTrue(result.keys.contains("exclude paths"), "Expected key 4")
        assertTrue(result.keys.contains("include file endings"), "Expected key 5")
        assertTrue(result.keys.contains("block comments to remove"), "Expected key 6")
        assertTrue(result.keys.contains("block comments to keep"), "Expected key 7")
        assertTrue(result.keys.contains("line comments to remove"), "Expected key 8")
        assertTrue(result.keys.contains("line comments to keep"), "Expected key 9")

        val listA = result["exclude paths"] ?: throw Exception("Missing value")
        assertEquals(2, listA.size)
        assertTrue(listA.contains("*\\gen\\*"), "Expected item *\\gen\\*")
        assertTrue(listA.contains("*Test.*"), "Expected item *Test.*")

        val listB = result["block comments to keep"] ?: throw Exception("Missing value")
        assertEquals(1, listB.size)
        assertTrue(listB.contains("/**...*/"), "Expected item /**...*/")

        val listC = result["line comments to keep"] ?: throw Exception("Missing value")
        assertEquals(2, listC.size)
        assertTrue(listC.contains("//:"), "Expected item //:")
        assertTrue(listC.contains("//*"), "//*")
    }

    @Test
    fun test_singleItemLists() {
        val result1 = BasicYamlReader().fromString(createYamlSingleItemLists()).data
        val result2 = BasicYamlReader().fromString(createYamlSingleItemLines()).data

        assertEquals(3, result1.size)
        assertEquals(1, result1["list a"]?.size)
        assertEquals(0, result1["list b"]?.size)
        assertEquals(1, result1["list c"]?.size)
        assertTrue(result1["list a"]?.contains("a 1") == true)
        assertTrue(result1["list c"]?.contains("c 1") == true)

        assertEquals(3, result2.size)
        assertEquals(1, result2["list a"]?.size)
        assertEquals(0, result2["list b"]?.size)
        assertEquals(1, result2["list c"]?.size)
        assertTrue(result2["list a"]?.contains("a 1") == true)
        assertTrue(result2["list c"]?.contains("c 1") == true)
    }

    @Test
    fun test_noData() {
        val result = BasicYamlReader().fromString("").data
        assertEquals(0, result.size)
    }

    @Test
    fun test_emptyLineKey() {
        assertUniqueExceptionInBlock("1c78640d-bf99-46a5-9530-e62b266cc151"){
            val yaml =
                """
                list a:
                 - a 1
                 - a 2
                :
                 - b 1
                 - b 1
                """.trimIndent()
            BasicYamlReader().fromString(yaml).data
        }
    }

    @Test
    fun test_itemWithoutKey() {
        assertUniqueExceptionInBlock("b18ddc0d-184e-44c6-a960-34652bd12685"){
            val yaml =
                """
                 - a 1
                 - a 2
                """.trimIndent()
            BasicYamlReader().fromString(yaml).data
        }
    }

    @Test
    fun test_missingColon() {
        assertUniqueExceptionInBlock("fe1ed9c1-a7e7-4900-9867-10c776f8e7b3"){
            val yaml =
                """
                 xyz
                 - a 1
                 - a 2
                """.trimIndent()
            BasicYamlReader().fromString(yaml).data
        }
    }

    @Test
    fun test_duplicateKey() {
        assertUniqueExceptionInBlock("9102e8e3-ce70-4198-b866-6fdae4c12b63"){
            val yaml =
                """
                 a:
                 - a 1
                 - a 2
                 b:
                 - b 1
                 - b 2
                 a:
                 - a 3
                 - a 4
                """.trimIndent()
            BasicYamlReader().fromString(yaml).data
        }
    }

    @Test
    fun test_itemFollowingKeyAndValueLine() {
        assertUniqueExceptionInBlock("b18ddc0d-184e-44c6-a960-34652bd12685"){
            val yaml =
                """
                # my comment
                list a: a 1
                list b:
                list c:c 1
                - c2
                """.trimIndent()
            BasicYamlReader().fromString(yaml).data
        }
    }

    @Test
    fun test_keyAndValueLineWithEmptyKey() {
        assertUniqueExceptionInBlock("3b32c294-5913-4d3e-9cfd-b67179b2e12e"){
            val yaml =
                """
                # my comment
                list a: a 1
                list b:
                :c 1
                list d: d 1
                """.trimIndent()
            BasicYamlReader().fromString(yaml).data
        }
    }

}