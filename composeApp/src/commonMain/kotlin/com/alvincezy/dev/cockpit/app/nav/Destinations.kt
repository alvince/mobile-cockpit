package com.alvincezy.dev.cockpit.app.nav

/**
 * First-level sections shown as Home cards and as the primary navigation rail items.
 */
enum class Section(val label: String) {
    Connection("Connection"),
    DeveloperTools("Developer Tools"),
    Presentation("Presentation");

    /** The sub-section to open when a section is entered without an explicit choice. */
    val defaultSub: SubSection
        get() = SubSection.entries.first { it.section == this }
}

/**
 * Second-level sub-sections, grouped by their owning [Section]. Surfaced via the
 * hover fly-out on the navigation rail (added in a later step).
 */
enum class SubSection(val section: Section, val label: String) {
    // Connection
    AdbDevices(Section.Connection, "设备连接"),
    WirelessPairing(Section.Connection, "无线配对"),

    // Developer Tools
    AdbCommands(Section.DeveloperTools, "ADB 命令"),
    ServerControl(Section.DeveloperTools, "Server 管理"),

    // Presentation
    Scrcpy(Section.Presentation, "投屏 (scrcpy)"),
    ;

    companion object {
        /** All sub-sections belonging to [section], in declaration order. */
        fun forSection(section: Section): List<SubSection> =
            entries.filter { it.section == section }
    }
}
