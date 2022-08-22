package de.bright_side.coca

import de.bright_side.filesystemfacade.facade.FsfFile
import de.bright_side.filesystemfacade.facade.FsfSystem
import de.bright_side.velvetkotlin.SubstituteSortedMap

object CocaUtil {
    fun getRelativePath(filePath:String, filePathBase: String): String{
        return filePath.substring(filePathBase.length).removePrefix("\\").removePrefix("/")
    }

    fun getFilenameAndExtension(fullFilename: String): Pair<String, String>{
        val dotPos = fullFilename.lastIndexOf(".")
        if (dotPos < 0){
            return fullFilename to ""
        }
        if (fullFilename.endsWith(".")){
            return fullFilename.dropLast(1) to ""
        }
        return fullFilename.substring(0 until dotPos) to fullFilename.substring(dotPos + 1)
    }

    fun createFilesToCommentsMap(fileSystem: FsfSystem, comments: List<CommentOccurrence>): Map<FsfFile, List<CommentOccurrence>> {
        val result = SubstituteSortedMap<FsfFile, MutableList<CommentOccurrence>>()

        comments.forEach { comment ->
            val file = fileSystem / comment.filePath
            if (!result.containsKey(file)){
                result[file] = mutableListOf()
            }
            result[file]?.add(comment)
        }

        return result
    }

}