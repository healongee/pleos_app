package com.example.myapplication

import com.example.myapplication.data.ClimateZone
import com.example.myapplication.data.SeatPosition
import com.example.myapplication.data.VehicleControlManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PublicApiPolicyTest {
    @Test
    fun localPreviewGatesMockControlsAndResetsOnExit() {
        val manager = VehicleControlManager()
        manager.setTemperature(ClimateZone.DRIVER, 30)
        manager.toggleSeatHeating(SeatPosition.DRIVER)
        assertEquals(22, manager.temperature(ClimateZone.DRIVER))
        assertFalse(manager.settings(SeatPosition.DRIVER).heating)
        manager.startPreview()
        assertTrue(manager.sessionActive.value)
        manager.setTemperature(ClimateZone.DRIVER, 40)
        manager.toggleSeatHeating(SeatPosition.DRIVER)
        assertEquals(30, manager.temperature(ClimateZone.DRIVER))
        assertTrue(manager.settings(SeatPosition.DRIVER).heating)
        manager.endPreview()
        assertFalse(manager.sessionActive.value)
        assertFalse(manager.settings(SeatPosition.DRIVER).heating)
        manager.setTemperature(ClimateZone.DRIVER, 16)
        assertEquals(22, manager.temperature(ClimateZone.DRIVER))
    }
}
