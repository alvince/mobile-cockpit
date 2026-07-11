package com.alvincezy.dev.cockpit.core.adb

object AdbDeviceParser {

    fun parse(output: String): List<AdbDevice> {
        return output.lineSequence()
            .drop(1) // skip "List of devices attached"
            .mapNotNull { line -> parseLine(line.trim()) }
            .toList()
    }

    private fun parseLine(line: String): AdbDevice? {
        if (line.isEmpty()) return null

        val tokens = line.split("\\s+".toRegex())
        if (tokens.size < 2) return null

        val serial = tokens[0]
        val state = tokens[1]

        val pairs = tokens.drop(2)
            .mapNotNull { token -> parseKeyValue(token) }
            .toMap()

        val transport = when {
            serial.contains(":") -> serial
            tokens.any { it.startsWith("usb:") } -> "usb"
            else -> null
        }

        return AdbDevice(
            serial = serial,
            state = state,
            model = pairs["model"],
            product = pairs["product"],
            device = pairs["device"],
            features = pairs["features"],
            transport = transport,
            transportId = pairs["transport_id"],
        )
    }

    private fun parseKeyValue(token: String): Pair<String, String>? {
        val index = token.indexOf(':')
        if (index <= 0) return null
        return token.substring(0, index) to token.substring(index + 1)
    }
}
