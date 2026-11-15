package com.example.myapplication.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Offline console only. No Pleos SDK, vehicle access, booking credentials or park-state signal. */
class VehicleControlManager {
    private val _sessionActive = MutableStateFlow(false)
    val sessionActive: StateFlow<Boolean> = _sessionActive
    private val _targetTemperature = MutableStateFlow(22)
    val targetTemperature: StateFlow<Int> = _targetTemperature
    private val _isHeatingOn = MutableStateFlow(false)
    val isHeatingOn: StateFlow<Boolean> = _isHeatingOn
    private val _isVentilationOn = MutableStateFlow(false)
    val isVentilationOn: StateFlow<Boolean> = _isVentilationOn
    private val _isRelaxModeOn = MutableStateFlow(false)
    val isRelaxModeOn: StateFlow<Boolean> = _isRelaxModeOn

    /** Explicit local preview gate; NOT a verified booking or proof the vehicle is parked. */
    fun startPreview() { _sessionActive.value = true }
    fun endPreview() {
        _sessionActive.value = false
        _isHeatingOn.value = false
        _isVentilationOn.value = false
        _isRelaxModeOn.value = false
    }
    fun setTemperature(temp: Int) { if (_sessionActive.value) _targetTemperature.value = temp.coerceIn(16, 30) }
    fun toggleSeatHeating() { if (_sessionActive.value) _isHeatingOn.value = !_isHeatingOn.value }
    fun toggleSeatVentilation() { if (_sessionActive.value) _isVentilationOn.value = !_isVentilationOn.value }
    fun toggleRelaxSeatMode() { if (_sessionActive.value) _isRelaxModeOn.value = !_isRelaxModeOn.value }
}
