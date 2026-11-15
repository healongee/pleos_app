package com.example.myapplication

import com.example.myapplication.data.ClimateZone
import com.example.myapplication.data.SeatPosition
import com.example.myapplication.data.VehicleControlManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleControlManagerTest {
    @Test fun inactiveSessionIgnoresChangesAndValuesClampIndependently() {
        val manager = VehicleControlManager()
        manager.setWindowPercent(SeatPosition.DRIVER, 80)
        manager.setTemperature(ClimateZone.DRIVER, 28)
        manager.setReclineDegrees(SeatPosition.DRIVER, 130)
        manager.toggleAmbient()
        assertEquals(0, manager.settings(SeatPosition.DRIVER).windowPercent)
        assertEquals(22, manager.temperature(ClimateZone.DRIVER))
        assertEquals(100, manager.settings(SeatPosition.DRIVER).reclineDegrees)
        assertFalse(manager.ambientPlaying.value)
        manager.startPreview()
        manager.setWindowPercent(SeatPosition.DRIVER, 150)
        manager.setWindowPercent(SeatPosition.REAR_RIGHT, -10)
        manager.setTemperature(ClimateZone.DRIVER, 7)
        manager.setTemperature(ClimateZone.REAR, 40)
        manager.setReclineDegrees(SeatPosition.DRIVER, 999)
        manager.setForeAftPercent(SeatPosition.PASSENGER, -50)
        assertEquals(100, manager.settings(SeatPosition.DRIVER).windowPercent)
        assertEquals(0, manager.settings(SeatPosition.REAR_RIGHT).windowPercent)
        assertEquals(16, manager.temperature(ClimateZone.DRIVER))
        assertEquals(30, manager.temperature(ClimateZone.REAR))
        assertEquals(22, manager.temperature(ClimateZone.PASSENGER))
        assertEquals(130, manager.settings(SeatPosition.DRIVER).reclineDegrees)
        assertEquals(0, manager.settings(SeatPosition.PASSENGER).foreAftPercent)
    }

    @Test fun seatModesAndWindowsRemainIndependent() {
        val manager = VehicleControlManager()
        manager.startPreview()
        manager.toggleSeatHeating(SeatPosition.DRIVER)
        manager.toggleSeatVentilation(SeatPosition.REAR_LEFT)
        manager.setWindowPercent(SeatPosition.REAR_LEFT, 65)
        manager.toggleSeatVentilation(SeatPosition.DRIVER)
        assertFalse(manager.settings(SeatPosition.DRIVER).heating)
        assertTrue(manager.settings(SeatPosition.DRIVER).ventilation)
        assertTrue(manager.settings(SeatPosition.REAR_LEFT).ventilation)
        assertEquals(65, manager.settings(SeatPosition.REAR_LEFT).windowPercent)
        assertEquals(0, manager.settings(SeatPosition.PASSENGER).windowPercent)
        manager.toggleSeatVentilation(SeatPosition.DRIVER)
        assertFalse(manager.settings(SeatPosition.DRIVER).ventilation)
    }

    @Test fun rearSeatsCannotReclineOrMoveAndResetRestoresAllZones() {
        val manager = VehicleControlManager()
        manager.startPreview()
        manager.selectSeat(SeatPosition.REAR_RIGHT)
        manager.setReclineDegrees(SeatPosition.REAR_RIGHT, 120)
        manager.setForeAftPercent(SeatPosition.REAR_RIGHT, 90)
        assertEquals(100, manager.settings(SeatPosition.REAR_RIGHT).reclineDegrees)
        assertEquals(50, manager.settings(SeatPosition.REAR_RIGHT).foreAftPercent)
        manager.setTemperature(ClimateZone.REAR, 25)
        manager.setWindowPercent(SeatPosition.REAR_RIGHT, 35)
        manager.toggleSeatHeating(SeatPosition.REAR_RIGHT)
        manager.setReclineDegrees(SeatPosition.PASSENGER, 115)
        manager.toggleAmbient()
        manager.pauseAmbient()
        assertFalse(manager.ambientPlaying.value)
        manager.toggleAmbient()
        manager.endPreview()
        assertFalse(manager.sessionActive.value)
        assertEquals(SeatPosition.DRIVER, manager.selectedSeat.value)
        assertTrue(manager.seats.value.values.all { it.windowPercent == 0 && !it.heating && !it.ventilation && it.reclineDegrees == 100 && it.foreAftPercent == 50 })
        assertTrue(manager.temperatures.value.values.all { it == 22 })
        assertFalse(manager.ambientPlaying.value)
    }
}
