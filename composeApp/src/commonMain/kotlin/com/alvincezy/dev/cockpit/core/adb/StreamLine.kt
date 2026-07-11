package com.alvincezy.dev.cockpit.core.adb

sealed class StreamLine {
    data class Output(val line: String) : StreamLine()
    data class Error(val line: String) : StreamLine()
    data class Completed(val exitCode: Int) : StreamLine()
}
