package com.alvincezy.dev.cockpit.core.adb

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdbDeviceParserTest {

    @Test
    fun parsesEmptyList() {
        val output = "List of devices attached\n"
        assertTrue(AdbDeviceParser.parse(output).isEmpty())
    }

    @Test
    fun parsesUsbDevice() {
        val output = """
            List of devices attached
            emulator-5554          device product:sdk_gphone64 model:Pixel_6 device:emulator64 transport_id:1
        """.trimIndent()

        val devices = AdbDeviceParser.parse(output)
        assertEquals(1, devices.size)
        val d = devices.first()
        assertEquals("emulator-5554", d.serial)
        assertEquals("device", d.state)
        assertEquals("Pixel_6", d.model)
        assertEquals("sdk_gphone64", d.product)
        assertEquals("emulator64", d.device)
        assertEquals("1", d.transportId)
    }

    @Test
    fun parsesIpWirelessDevice() {
        val output = """
            List of devices attached
            192.168.1.10:5555      device product:redfin model:Pixel_5 device:redfin transport_id:3
        """.trimIndent()

        val devices = AdbDeviceParser.parse(output)
        assertEquals(1, devices.size)
        val d = devices.first()
        assertEquals("192.168.1.10:5555", d.serial)
        assertEquals("device", d.state)
        assertEquals("192.168.1.10:5555", d.transport)
    }

    @Test
    fun parsesOfflineAndUnauthorized() {
        val output = """
            List of devices attached
            ABCD1234               offline
            EFGH5678               unauthorized
        """.trimIndent()

        val devices = AdbDeviceParser.parse(output)
        assertEquals(2, devices.size)
        assertEquals("offline", devices[0].state)
        assertEquals("unauthorized", devices[1].state)
        assertNull(devices[0].model)
    }

    @Test
    fun parsesRecoveryAndBootloader() {
        val output = """
            List of devices attached
            SER1                   recovery
            SER2                   bootloader
        """.trimIndent()

        val devices = AdbDeviceParser.parse(output)
        assertEquals("recovery", devices[0].state)
        assertEquals("bootloader", devices[1].state)
    }

    @Test
    fun parsesMultipleMixedDevices() {
        val output = """
            List of devices attached
            emulator-5554          device product:sdk model:Emu device:emu transport_id:1
            192.168.1.10:5555      device product:redfin model:Pixel_5 device:redfin transport_id:2
            BADDEVICE              unauthorized
        """.trimIndent()

        val devices = AdbDeviceParser.parse(output)
        assertEquals(3, devices.size)
    }
}
