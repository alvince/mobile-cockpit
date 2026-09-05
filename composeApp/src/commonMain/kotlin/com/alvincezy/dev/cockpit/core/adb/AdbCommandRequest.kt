package com.alvincezy.dev.cockpit.core.adb

import okio.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class AdbCommandRequest(
    val args: List<String>,
    val env: Map<String, String> = emptyMap(),
    val workingDir: Path? = null,
    val timeout: Duration = 30.seconds,
)
