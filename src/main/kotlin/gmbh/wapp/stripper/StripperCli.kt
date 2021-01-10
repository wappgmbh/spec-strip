package gmbh.wapp.stripper

import io.airlift.airline.Arguments
import io.airlift.airline.Command
import io.airlift.airline.Option
import java.io.File

@Command(name = "strip")
class StripperCli : Runnable {

    @Option(
        name = ["-i", "--input"],
        title = "input file",
        required = true
    )
    private val input = ""

    @Option(
        name = ["-o", "--output"],
        title = "output file",
        required = true
    )
    private val output = ""

    @Option(
        name = ["-f", "--force", "--overwrite"],
        title = "overwrite existing output file",
        required = false
    )
    private val force = false

    @Option(
        name = ["-t", "--type"],
        title = "output type (JSON or YAML)",
        required = false
    )
    private val outputType = OutputType.JSON

    @Arguments(
        title = "included tags",
        required = true
    )
    private val tags = mutableSetOf<String>()

    override fun run() {
        val inputFile = File(input)
        val outputFile = File(output)

        if (!inputFile.exists()) {
            throw IllegalStateException("Input file not found")
        }
        if (outputFile.exists() && !force) {
            throw  IllegalStateException("Output file already exists")
        }
        val options = StripperOptions(
            input = inputFile,
            output = outputFile,
            force = force,
            type = outputType,
            tags = tags
        )
        Stripper(options).run()
    }

}
