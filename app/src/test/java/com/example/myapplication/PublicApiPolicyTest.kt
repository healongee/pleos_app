package com.example.myapplication

import com.example.myapplication.data.VehicleControlManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PublicApiPolicyTest {
    @Test
    fun localPreviewGatesMockControlsAndResetsOnExit() {
        val manager = VehicleControlManager()
        manager.setTemperature(30)
        manager.toggleSeatHeating()
        assertEquals(22, manager.targetTemperature.value)
        assertFalse(manager.isHeatingOn.value)
        manager.startPreview()
        assertTrue(manager.sessionActive.value)
        manager.setTemperature(40)
        manager.toggleSeatHeating()
        assertEquals(30, manager.targetTemperature.value)
        assertTrue(manager.isHeatingOn.value)
        manager.endPreview()
        assertFalse(manager.sessionActive.value)
        assertFalse(manager.isHeatingOn.value)
        manager.setTemperature(16)
        assertEquals(30, manager.targetTemperature.value)
    }
}
