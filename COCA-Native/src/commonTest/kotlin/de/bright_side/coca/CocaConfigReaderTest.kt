package de.bright_side.coca

import de.bright_side.coca.CocaConfigReader
import de.bright_side.coca.StringFilter
import de.bright_side.coca.StringMatchType
import de.bright_side.filesystemfacade.memoryfs.MemoryFs
import de.bright_side.velvetkotlin.assertUniqueExceptionInBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CocaConfigReaderTest {

    private fun createYamlRealisticConfigSingleItemList(): String {
        return """
        coca config version: 1
        archive root path:
         - /myArchive
        source root path:
         - /myProject
        include paths:
         - /src/main/java/*
         - /src/main/kotlin/*
         - /src/test/java/*
         - /src/test/kotlin/*
        exclude paths:
         - */gen/*
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

    private fun createYamlRealisticConfigCondensed(): String {
        return """
        coca config version: 1
        archive root path: /myArchive
        source root path: /myProject
        include paths:
         - /src/main/java/*
         - /src/main/kotlin/*
         - /src/test/java/*
         - /src/test/kotlin/*
        exclude paths:
         - */gen/*
         - *Test.*
        include file endings:
         - *.kt
         - *.java
        block comments to remove: /*...*/
        block comments to keep: /**...*/
        line comments to remove: //
        line comments to keep:
         - //:
         - //*
        """.trimIndent()
    }


    @Test
    fun test_normalCase() {
        runNormalCaseTest(createYamlRealisticConfigSingleItemList())
        runNormalCaseTest(createYamlRealisticConfigCondensed())
    }

    private fun runNormalCaseTest(configYaml: String) {
        val fs = MemoryFs()
        val configFile = fs.createByPath("/config.yaml")
        configFile.writeString(configYaml)
        val config = CocaConfigReader().readConfig(configFile)
        assertEquals("/myArchive", config.archiveRootPath)
        assertEquals("/myProject", config.sourceRootPath)
        assertEquals(4, config.includePaths.size)
        assertTrue(config.includePaths.contains(StringFilter("/src/main/java/", StringMatchType.STARTS_WITH)))
        assertTrue(config.includePaths.contains(StringFilter("/src/main/kotlin/", StringMatchType.STARTS_WITH)))
        assertTrue(config.includePaths.contains(StringFilter("/src/test/java/", StringMatchType.STARTS_WITH)))
        assertTrue(config.includePaths.contains(StringFilter("/src/test/kotlin/", StringMatchType.STARTS_WITH)))
        assertEquals(2, config.excludePaths.size)
        assertTrue(config.excludePaths.contains(StringFilter("/gen/", StringMatchType.CONTAINS)))
        assertTrue(config.excludePaths.contains(StringFilter("Test.", StringMatchType.CONTAINS)))
        assertEquals(2, config.includeFileEndings.size)
        assertTrue(config.includeFileEndings.contains(".kt"))
        assertTrue(config.includeFileEndings.contains(".java"))
        assertEquals(1, config.blockCommentsToRemove.size)
        assertTrue(config.blockCommentsToRemove.contains(Pair("/*", "*/")))
        assertEquals(1, config.blockCommentsToKeep.size)
        assertTrue(config.blockCommentsToKeep.contains(Pair("/**", "*/")))
        assertEquals(1, config.lineCommentsToRemove.size)
        assertTrue(config.lineCommentsToRemove.contains("//"))
        assertEquals(2, config.lineCommentsToKeep.size)
        assertTrue(config.lineCommentsToKeep.contains("//:"))
        assertTrue(config.lineCommentsToKeep.contains("//*"))
    }


    @Test
    fun test_yamlWrongFormat() {
        assertUniqueExceptionInBlock("1ca27e4f-87c9-47ab-809a-225a241b2381") {
            val fs = MemoryFs()
            val configFile = fs.createByPath("/config.yaml")
            configFile.writeString("Not yaml")
            CocaConfigReader().readConfig(configFile)
        }
    }

    @Test
    fun test_archiveRootMissing() {
        val configText = """
        coca config version: 1
        source root path: /myProject
        include paths:
         - /src/main/java/*
         - /src/main/kotlin/*
         - /src/test/java/*
         - /src/test/kotlin/*
        exclude paths:
         - */gen/*
         - *Test.*
        include file endings:
         - *.kt
         - *.java
        block comments to remove: /*...*/
        block comments to keep: /**...*/
        line comments to remove: //
        line comments to keep:
         - //:
         - //*
        """.trimIndent()

        assertUniqueExceptionInBlock("b2b59c5c-0c30-4367-94ec-8b76e2e4dfab") {
            val fs = MemoryFs()
            val configFile = fs.createByPath("/config.yaml")
            configFile.writeString(configText)
            CocaConfigReader().readConfig(configFile)
        }
    }

    @Test
    fun test_versionMissing() {
        val configText = """
        archive root path: /myArchive1
        source root path: /myProject
        include paths:
         - /src/main/java/*
         - /src/main/kotlin/*
         - /src/test/java/*
         - /src/test/kotlin/*
        exclude paths:
         - */gen/*
         - *Test.*
        include file endings:
         - *.kt
         - *.java
        block comments to remove: /*...*/
        block comments to keep: /**...*/
        line comments to remove: //
        line comments to keep:
         - //:
         - //*
        """.trimIndent()

        assertUniqueExceptionInBlock("e6ef1ca8-6562-41d0-b53f-dab372c2c110") {
            CocaConfigReader().readConfig(MemoryFs().createByPath("/config.yaml").writeString(configText))
        }
    }

    @Test
    fun test_versionWrong() {
        val configText = """
        coca config version: 2
        archive root path: /myArchive1
        source root path: /myProject
        include paths:
         - /src/main/java/*
         - /src/main/kotlin/*
         - /src/test/java/*
         - /src/test/kotlin/*
        exclude paths:
         - */gen/*
         - *Test.*
        include file endings:
         - *.kt
         - *.java
        block comments to remove: /*...*/
        block comments to keep: /**...*/
        line comments to remove: //
        line comments to keep:
         - //:
         - //*
        """.trimIndent()

        assertUniqueExceptionInBlock("7227b151-e2e3-4f60-a844-8c4cf44605bc") {
            CocaConfigReader().readConfig(MemoryFs().createByPath("/config.yaml").writeString(configText))
        }
    }

    @Test
    fun test_versionNotANumber() {
        val configText = """
        coca config version: abc
        archive root path: /myArchive1
        source root path: /myProject
        include paths:
         - /src/main/java/*
         - /src/main/kotlin/*
         - /src/test/java/*
         - /src/test/kotlin/*
        exclude paths:
         - */gen/*
         - *Test.*
        include file endings:
         - *.kt
         - *.java
        block comments to remove: /*...*/
        block comments to keep: /**...*/
        line comments to remove: //
        line comments to keep:
         - //:
         - //*
        """.trimIndent()

        assertUniqueExceptionInBlock("7227b151-e2e3-4f60-a844-8c4cf44605bc") {
            CocaConfigReader().readConfig(MemoryFs().createByPath("/config.yaml").writeString(configText))
        }
    }

    @Test
    fun test_archiveRootMultiple() {
        val configText = """
        coca config version: 1
        archive root path:
         - /myArchive1
         - /myArchive2
        source root path: /myProject
        include paths:
         - /src/main/java/*
         - /src/main/kotlin/*
         - /src/test/java/*
         - /src/test/kotlin/*
        exclude paths:
         - */gen/*
         - *Test.*
        include file endings:
         - *.kt
         - *.java
        block comments to remove: /*...*/
        block comments to keep: /**...*/
        line comments to remove: //
        line comments to keep:
         - //:
         - //*
        """.trimIndent()

        assertUniqueExceptionInBlock("6e462760-8727-45aa-975e-bc0550a7408c") {
            val fs = MemoryFs()
            val configFile = fs.createByPath("/config.yaml")
            configFile.writeString(configText)
            CocaConfigReader().readConfig(configFile)
        }
    }

    @Test
    fun test_sourceRootMissing() {
        val configText = """
        coca config version: 1
        archive root path: /myArchive
        include paths:
         - src/main/java/*
         - src/main/kotlin/*
         - src/test/java/*
         - src/test/kotlin/*
        exclude paths:
         - */gen/*
         - *Test.*
        include file endings:
         - *.kt
         - *.java
        block comments to remove: /*...*/
        block comments to keep: /**...*/
        line comments to remove: //
        line comments to keep:
         - //:
         - //*
        """.trimIndent()

        assertUniqueExceptionInBlock("a5950dd4-eb32-4230-9c11-7233fccb72c1") {
            val fs = MemoryFs()
            val configFile = fs.createByPath("/config.yaml")
            configFile.writeString(configText)
            CocaConfigReader().readConfig(configFile)
        }
    }

    @Test
    fun test_sourceRootMultiple() {
        val configText = """
        coca config version: 1
        archive root path: /myArchive
        source root path:
         - /myProject1
         - /myProject2
        include paths:
         - /src/main/java/*
         - /src/main/kotlin/*
         - /src/test/java/*
         - /src/test/kotlin/*
        exclude paths:
         - */gen/*
         - *Test.*
        include file endings:
         - *.kt
         - *.java
        block comments to remove: /*...*/
        block comments to keep: /**...*/
        line comments to remove: //
        line comments to keep:
         - //:
         - //*
        """.trimIndent()

        assertUniqueExceptionInBlock("080eb868-bbc6-478f-9d9b-2e968c0ea71d") {
            val fs = MemoryFs()
            val configFile = fs.createByPath("/config.yaml")
            configFile.writeString(configText)
            CocaConfigReader().readConfig(configFile)
        }
    }

    @Test
    fun test_blockCommentMissingEllipse() {
        val configText = """
        coca config version: 1
        archive root path: /myArchive
        source root path: /myProject
        include paths:
         - /src/main/java/*
         - /src/main/kotlin/*
         - /src/test/java/*
         - /src/test/kotlin/*
        exclude paths:
         - */gen/*
         - *Test.*
        include file endings:
         - *.kt
         - *.java
        block comments to remove: /*-*/
        block comments to keep: /**...*/
        line comments to remove: //
        line comments to keep:
         - //:
         - //*
        """.trimIndent()

        assertUniqueExceptionInBlock("dbe09487-03e9-4351-a077-91baee4e4fc3") {
            val fs = MemoryFs()
            val configFile = fs.createByPath("/config.yaml")
            configFile.writeString(configText)
            CocaConfigReader().readConfig(configFile)
        }
    }

    @Test
    fun test_blockCommentMultipleEllipse() {
        val configText = """
        coca config version: 1
        archive root path: /myArchive
        source root path: /myProject
        include paths:
         - /src/main/java/*
         - /src/main/kotlin/*
         - /src/test/java/*
         - /src/test/kotlin/*
        exclude paths:
         - */gen/*
         - *Test.*
        include file endings:
         - *.kt
         - *.java
        block comments to remove: /*...x...*/
        block comments to keep: /**...*/
        line comments to remove: //
        line comments to keep:
         - //:
         - //*
        """.trimIndent()

        assertUniqueExceptionInBlock("dbe09487-03e9-4351-a077-91baee4e4fc3") {
            val fs = MemoryFs()
            val configFile = fs.createByPath("/config.yaml")
            configFile.writeString(configText)
            CocaConfigReader().readConfig(configFile)
        }
    }


    @Test
    fun test_includeFileEndingsEmpty() {
        val configText = """
        coca config version: 1
        archive root path: /myArchive
        source root path: /myProject
        include paths:
         - /src/main/java/*
         - /src/main/kotlin/*
         - /src/test/java/*
         - /src/test/kotlin/*
        exclude paths:
         - */gen/*
         - *Test.*
        include file endings:
        block comments to remove: /*...*/
        block comments to keep: /**...*/
        line comments to remove: //
        line comments to keep:
         - //:
         - //*
        """.trimIndent()

        assertUniqueExceptionInBlock("09aa8d64-ff84-4659-8aed-fc0171a8726b") {
            val fs = MemoryFs()
            val configFile = fs.createByPath("/config.yaml")
            configFile.writeString(configText)
            CocaConfigReader().readConfig(configFile)
        }
    }

    @Test
    fun test_replaceBackslashInPaths() {
        val configString = """
        coca config version: 1
        archive root path: \myArchive
        source root path: \myProject
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
        block comments to remove: /*...*/
        block comments to keep: /**...*/
        line comments to remove: //
        line comments to keep:
         - //:
         - //*
        """.trimIndent()

        val fs = MemoryFs()
        val configFile = fs.createByPath("/config.yaml")
        configFile.writeString(configString)
        val config = CocaConfigReader().readConfig(configFile)
        assertEquals("/myArchive", config.archiveRootPath)
        assertEquals("/myProject", config.sourceRootPath)
        assertEquals(4, config.includePaths.size)
        assertTrue(config.includePaths.contains(StringFilter("/src/main/java/", StringMatchType.STARTS_WITH)))
        assertTrue(config.includePaths.contains(StringFilter("/src/main/kotlin/", StringMatchType.STARTS_WITH)))
        assertTrue(config.includePaths.contains(StringFilter("/src/test/java/", StringMatchType.STARTS_WITH)))
        assertTrue(config.includePaths.contains(StringFilter("/src/test/kotlin/", StringMatchType.STARTS_WITH)))
        assertEquals(2, config.excludePaths.size)
        assertTrue(config.excludePaths.contains(StringFilter("/gen/", StringMatchType.CONTAINS)))
        assertTrue(config.excludePaths.contains(StringFilter("Test.", StringMatchType.CONTAINS)))
        assertEquals(2, config.includeFileEndings.size)
        assertTrue(config.includeFileEndings.contains(".kt"))
        assertTrue(config.includeFileEndings.contains(".java"))
        assertEquals(1, config.blockCommentsToRemove.size)
        assertTrue(config.blockCommentsToRemove.contains(Pair("/*", "*/")))
        assertEquals(1, config.blockCommentsToKeep.size)
        assertTrue(config.blockCommentsToKeep.contains(Pair("/**", "*/")))
        assertEquals(1, config.lineCommentsToRemove.size)
        assertTrue(config.lineCommentsToRemove.contains("//"))
        assertEquals(2, config.lineCommentsToKeep.size)
        assertTrue(config.lineCommentsToKeep.contains("//:"))
        assertTrue(config.lineCommentsToKeep.contains("//*"))
    }

    @Test
    fun test_includePathMissingAsterisk() {
        val configText = """
        coca config version: 1
        archive root path: /myArchive
        source root path: /myProject1
        include paths:
         - /src/main/java/*
         - /src/main/kotlin/*
         - /src/test/java/
         - /src/test/kotlin/*
        exclude paths:
         - */gen/*
         - *Test.*
        include file endings:
         - *.kt
         - *.java
        block comments to remove: /*...*/
        block comments to keep: /**...*/
        line comments to remove: //
        line comments to keep:
         - //:
         - //*
        """.trimIndent()

        assertUniqueExceptionInBlock("068f785a-9a7e-4627-b24e-d2e22dfb8b65") {
            val fs = MemoryFs()
            val configFile = fs.createByPath("/config.yaml")
            configFile.writeString(configText)
            CocaConfigReader().readConfig(configFile)
        }
    }


    @Test
    fun test_parseFilter_bulk() {
        val reader = CocaConfigReader()
        val uuid = "0dfacb63-2545-4545-a80f-3435810040d7"
        val message = "error"
        assertEquals(StringFilter("hello", StringMatchType.EXACT), reader.parseFilter("hello", uuid, message))
        assertEquals(StringFilter("hello", StringMatchType.STARTS_WITH), reader.parseFilter("hello*", uuid, message))
        assertEquals(StringFilter("hello", StringMatchType.ENDS_WITH), reader.parseFilter("*hello", uuid, message))
        assertEquals(StringFilter("hello", StringMatchType.CONTAINS), reader.parseFilter("*hello*", uuid, message))
        assertEquals(StringFilter("", StringMatchType.CONTAINS), reader.parseFilter("*", uuid, message))
        assertUniqueExceptionInBlock(uuid) { reader.parseFilter("", uuid, message) }
        assertUniqueExceptionInBlock(uuid) { reader.parseFilter("hel*lo", uuid, message) }
        assertUniqueExceptionInBlock(uuid) { reader.parseFilter("*h*ello", uuid, message) }
    }

    @Test
    fun test_parseFileFilterEnding_bulk() {
        val reader = CocaConfigReader()
        assertEquals(".kt", reader.parseFileFilterEnding("*.kt"))
        assertEquals(".meta", reader.parseFileFilterEnding("*.meta"))
        assertUniqueExceptionInBlock("99fdc7f9-4051-479f-8cbf-5298f05126b5") { reader.parseFileFilterEnding(".meta") }
        assertUniqueExceptionInBlock("99fdc7f9-4051-479f-8cbf-5298f05126b5") { reader.parseFileFilterEnding(".meta*") }
        assertUniqueExceptionInBlock("99fdc7f9-4051-479f-8cbf-5298f05126b5") { reader.parseFileFilterEnding("*.m*") }
        assertUniqueExceptionInBlock("99fdc7f9-4051-479f-8cbf-5298f05126b5") { reader.parseFileFilterEnding("*.m*e") }
    }
}