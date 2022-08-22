import de.bright_side.coca.*
import de.bright_side.coca.CocaAction.WRITE_SAMPLE_CONFIG_FILE
import de.bright_side.filesystemfacade.facade.FsfSystem
import de.bright_side.filesystemfacade.okiofs.OkioFs
import de.bright_side.velvetkotlin.UniqueException
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    println("COCA - Commented Out Code Archiver.  Version 0.1.0, 2022 by Philip Heyse\n")
    try{
        val fileSystem: FsfSystem = OkioFs()
        if (args.isEmpty()){
            CocaHelp().printHelp()
            return
        }
        val options = CocaOptionsReader().readOptions(args.toList())

        val config = if (options.action == WRITE_SAMPLE_CONFIG_FILE) {
            CocaConfigReader().createEmptyConfig()
        } else {
            CocaConfigReader().readConfig(fileSystem / options.configFilePath)
        }

        CocaProcessor(options, config, fileSystem).process()
        println("\nprocessing complete.")
    } catch (e: UniqueException){
        println("Error: ${e.message}  (error id: ${e.uuid})")
        exitProcess(-1)
    } catch (e: Throwable){
        println("Unexpected error: ${e.message}")
        e.printStackTrace()
        exitProcess(-2)
    }
}

fun printEnv() {
    println("os = '${Platform.osFamily}'")
}