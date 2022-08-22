package de.bright_side.coca

import de.bright_side.filesystemfacade.facade.FsfSystem
import de.bright_side.velvetkotlin.UniqueException

class CocaHelp {
    fun printHelp(){
        val helpText = """
             USAGE:
            ========
            
             Syntax:
            ---------
            coca -a <ACTION> -c <PATH-TO-CONFIG-FILE> -o <PATH-TO-OUTPUT-FILE> -f <PREVIEW-FORMAT>
            
             Parameters:
            -------------
            <ACTION>: 
               - "p" to show a preview of all occurrences to be removed
               - "a" archive commented out code in the following steps: 
                        1. find comment occurrences to remove 
                        2. write old files index and summary to archive
                        3. update source files and remove comments
                        4. remove all files that only consist of comments
               - "c" to write a sample config file to <PATH-TO-OUTPUT-FILE>
            
            <PATH-TO-CONFIG-FILE>: 
             - Either the filename or a path and filename
            
            <PATH-TO-OUTPUT-FILE>:
               - If the action is "c" then a sample config file is written to this location.
            
            <PREVIEW-FORMAT>:
               - "b": beginning of text: single line
               - "m": multi-line
               - "h": html: in this case an output path needs to be provided via "-o". The file extension must be '.html'.
            
             Examples:
            -----------
             coca -a p -c C:\MyCocaConfig.yaml
             coca -a p -c /home/myuser/coca-config.yaml
             coca -a p -f h -o /home/myuser/html_preview.html -c /home/myuser/coca-config.yaml
             coca -a p -c coca.yaml -o "C:\CommentsToRemovePreview.txt"
             coca -a c -o /home/myuser/my-generated-coca-config.yaml    
        """.trimIndent()
        println(helpText)
    }

    private fun getSampleConfigFileText(): String {
        return """
        coca config version: 1
        archive root path: /user/me/my-coca-archive-for-project-abc
        source root path: /user/me/my-project-abc
        include paths:
         - /src/main/java/*
         - /src/main/kotlin/*
         - /src/test/java/
         - /src/test/kotlin/*
        exclude paths:
         - */gen/*
         - *IntegrationTest.*
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


    fun writeSampleConfigFile(fileSystem: FsfSystem, outputPath: String) {
        if (outputPath.isEmpty()) {
            throw UniqueException("f8b58986-65e9-4f3a-aa22-f17c245fb54b", "Output path is empty")
        }
        val file = fileSystem / outputPath
        val parentDir = file.parentFile
        if (parentDir?.exists() != true){
            parentDir?.mkdirs()
        }
        if (file.parentFile?.exists() != true){
            throw UniqueException(
                "0c84397b-4e20-4baf-ad67-1adbb358861c",
                "Could not find or create parent directory of path '$outputPath",
                listOf(outputPath)
            )
        }
        file.writeString(getSampleConfigFileText())
        println("Written sample config file to '$outputPath'.")
    }

}