package gmbh.wapp.stripper

import java.io.File

data class StripperOptions(
    val input: File,
    val output: File,
    val force: Boolean,
    val type: OutputType,
    val tags: Set<String>
)

enum class OutputType {
    JSON,
    YAML
}