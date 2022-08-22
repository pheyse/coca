[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
# COCA - Commented Out Code Remover
 
## Description
A little command line tool written in Kotlin/Native (but also working on JVM) to remove code that has been commented out.
Which comments should be kept or removed is configured via a config file, so that e.g. all "/**"..."*/" and "//:" comments are kept while the "/*..*/" and "//" comments are removed.

## No Warranties
No warranties. All use at your own risk.
 
## Features
 - command line tool running native (e.g. EXE on windows)
 - flexible configuration of source folders, path patterns as well as comment styles to keep and remove in (basic) YAML format
 - preview/show which comments would be removed as text or an HTML file with highlighting
 - during archiving process, 
      - Commented out code is removed from source files
      - old file versions are stored in an archive
      - information what was changed are stored in archive: HTML view, JSON per file, list of affected files

## Usage
```text
coca -a <ACTION> -c <PATH-TO-CONFIG-FILE> -o <PATH-TO-OUTPUT-FILE> -f <PREVIEW-FORMAT>
```            
### Parameters
#### ACTION: 
  - "p" to show a preview of all occurrences to be removed
  - "a" archive commented out code in the following steps:
     1. find comment occurrences to remove 
     2. write old files index and summary to archive
     3. update source files and remove comments
     4. remove all files that only consist of comments
  - "c" to write a sample config file to <PATH-TO-OUTPUT-FILE>
            
#### PATH-TO-CONFIG-FILE: 
Either the filename or a path and filename
            
#### PATH-TO-OUTPUT-FILE
If the action is "c" then a sample config file is written to this location.
            
#### PREVIEW-FORMAT
  - "b": beginning of text: single line
  - "m": multi-line
  - "h": html: in this case an output path needs to be provided via "-o". The file extension must be '.html'.

### Command Line Usage Examples
````text
   coca -a p -c C:\MyCocaConfig.yaml
   coca -a p -c /home/myuser/coca-config.yaml
   coca -a p -f h -o /home/myuser/html_preview.html -c /home/myuser/coca-config.yaml
   coca -a p -c coca.yaml -o "C:\CommentsToRemovePreview.txt"
   coca -a c -o /home/myuser/my-generated-coca-config.yaml    
````

### build.gradle.kts Usage Examples
```kotlin
[...]

repositories {
 [...]
}

[...]

tasks.register("coca_help") {
    doLast {
        println("running COCA help:")
        exec {
            commandLine = listOf("coca")
        }
    }
}


tasks.register("coca_preview_html") {
    doLast {
        println("running COCA HTML preview:")
        exec {
            commandLine = listOf("coca"
                , "-a", "p"
                , "-f", "h"
                , "-o", "<your-preview-html-location>"
                , "-c", "<your-coca-config-location>")
        }
    }
}

tasks.register("coca_preview_multiline") {
    doLast {
        println("creating COCA multiline preview:")
        exec {
            commandLine = listOf("coca"
                , "-a", "p"
                , "-f", "m"
                , "-c", "<your-coca-config-location>")
        }
    }
}

tasks.register("coca_archive_comments") {
    doLast {
        println("archiving comments:")
        exec {
            commandLine = listOf("coca"
                , "-a", "a"
                , "-c", "<your-coca-config-location>")
        }
    }
}
```

## Change History
 - Version 0.1.0 (2022-08-22)
    - initial version
