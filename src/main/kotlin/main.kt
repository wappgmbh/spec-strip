import gmbh.wapp.stripper.StripperCli
import io.airlift.airline.Cli
import io.airlift.airline.Help

fun main(args: Array<String>) {
    System.setProperty("org.slf4j.simpleLogger.logFile", "System.out")
    Cli.CliBuilder<Runnable>("spec-strip")
        .withDefaultCommand(Help::class.java)
        .withCommands(
            StripperCli::class.java,
            Help::class.java
        )
        .build()
        .parse(*args)
        .run()
}