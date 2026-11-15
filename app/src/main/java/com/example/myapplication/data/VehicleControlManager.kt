package com.example.myapplication.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class SeatPosition(val label: String, val front: Boolean) {
    DRIVER("앞 운전석", true), PASSENGER("앞 동승석", true),
    REAR_LEFT("뒤 왼쪽", false), REAR_RIGHT("뒤 오른쪽", false)
}

data class SeatSettings(
    val heating: Boolean = false, val ventilation: Boolean = false,
    val windowPercent: Int = 0, val reclineDegrees: Int = 100,
    val foreAftPercent: Int = 50
)

enum class ClimateZone(val label: String) {
    DRIVER("운전석"), PASSENGER("동승석"), REAR("뒷좌석")
}

/** Offline simulated controls only. No vehicle access or verified parked-state signal. */
class VehicleControlManager {
    private val _sessionActive = MutableStateFlow(false)
    val sessionActive: StateFlow<Boolean> = _sessionActive
    private val _selectedSeat = MutableStateFlow(SeatPosition.DRIVER)
    val selectedSeat: StateFlow<SeatPosition> = _selectedSeat
    private val _seats = MutableStateFlow(defaultSeats())
    val seats: StateFlow<Map<SeatPosition, SeatSettings>> = _seats
    private val _temperatures = MutableStateFlow(defaultTemperatures())
    val temperatures: StateFlow<Map<ClimateZone, Int>> = _temperatures
    private val _ambientPlaying = MutableStateFlow(false)
    val ambientPlaying: StateFlow<Boolean> = _ambientPlaying

    private fun defaultSeats() = SeatPosition.entries.associateWith { SeatSettings() }
    private fun defaultTemperatures() = ClimateZone.entries.associateWith { 22 }

    fun startPreview() { _sessionActive.value = true }
    fun endPreview() {
        _sessionActive.value = false
        _selectedSeat.value = SeatPosition.DRIVER
        _seats.value = defaultSeats()
        _temperatures.value = defaultTemperatures()
        _ambientPlaying.value = false
    }
    fun selectSeat(seat: SeatPosition) { _selectedSeat.value = seat }
    fun settings(seat: SeatPosition) = _seats.value.getValue(seat)
    fun temperature(zone: ClimateZone) = _temperatures.value.getValue(zone)
    fun setTemperature(zone: ClimateZone, temp: Int) {
        if (_sessionActive.value) _temperatures.value = _temperatures.value + (zone to temp.coerceIn(16, 30))
    }
    private fun updateSeat(seat: SeatPosition, transform: (SeatSettings) -> SeatSettings) {
        if (_sessionActive.value) _seats.value = _seats.value + (seat to transform(settings(seat)))
    }
    fun toggleSeatHeating(seat: SeatPosition) = updateSeat(seat) {
        it.copy(heating = !it.heating, ventilation = if (!it.heating) false else it.ventilation)
    }
    fun toggleSeatVentilation(seat: SeatPosition) = updateSeat(seat) {
        it.copy(ventilation = !it.ventilation, heating = if (!it.ventilation) false else it.heating)
    }
    fun setWindowPercent(seat: SeatPosition, percent: Int) = updateSeat(seat) {
        it.copy(windowPercent = percent.coerceIn(0, 100))
    }
    fun setReclineDegrees(seat: SeatPosition, degrees: Int) {
        if (seat.front) updateSeat(seat) { it.copy(reclineDegrees = degrees.coerceIn(90, 130)) }
    }
    fun setForeAftPercent(seat: SeatPosition, percent: Int) {
        if (seat.front) updateSeat(seat) { it.copy(foreAftPercent = percent.coerceIn(0, 100)) }
    }
    fun toggleAmbient() { if (_sessionActive.value) _ambientPlaying.value = !_ambientPlaying.value }
    fun pauseAmbient() { _ambientPlaying.value = false }
}
