package com.alvincezy.dev.cockpit.core.adb

data class AdbDevice(
    val serial: String,
    val state: String,
    val model: String? = null,
    val product: String? = null,
    val device: String? = null,
    val features: String? = null,
    val transport: String? = null,
    val transportId: String? = null,
)
