package com.alvincezy.dev.cockpit.app.nav

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations for the main screen. `Home` is the card
 * landing state; `Detail` is the expanded two-pane state for a given [Section]
 * and (optionally) a [SubSection].
 *
 * [Detail] stores the section/sub as their enum *names* (plain [String]s) because
 * Compose Navigation only auto-derives a `NavType` for primitives — enum route
 * arguments would otherwise require a custom `NavType`/`typeMap`. Use [Detail.of]
 * to construct one from enums and [Detail.section]/[Detail.sub] to read them back.
 */
sealed interface Destination {
    @Serializable
    data object Home : Destination

    @Serializable
    data class Detail(
        val sectionName: String,
        val subName: String? = null,
    ) : Destination {
        val section: Section get() = Section.valueOf(sectionName)
        val sub: SubSection? get() = subName?.let { SubSection.valueOf(it) }

        companion object {
            fun of(section: Section, sub: SubSection? = null): Detail =
                Detail(sectionName = section.name, subName = sub?.name)
        }
    }
}

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
