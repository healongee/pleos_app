package com.example.myapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.data.ClimateZone
import com.example.myapplication.data.InteriorCameraConsent
import com.example.myapplication.data.SeatPosition
import com.example.myapplication.data.VehicleControlManager
import com.google.android.material.button.MaterialButton

/** Offline local preview only; no vehicle SDK, camera capture, audio, or network connection. */
class CarRestControlFragment : Fragment() {
    private val manager = VehicleControlManager()
    private val cameraConsent = InteriorCameraConsent()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_car_rest_control, container, false)
        val temperature = view.findViewById<TextView>(R.id.tvCurrentTemp)
        val status = view.findViewById<TextView>(R.id.tvRestTimer)
        val selectedStatus = view.findViewById<TextView>(R.id.tvSelectedSeat)
        val seatPosition = view.findViewById<TextView>(R.id.tvSeatPosition)
        val windowStatus = view.findViewById<TextView>(R.id.tvWindowStatus)
        val window = view.findViewById<SeekBar>(R.id.seekWindow)
        val recline = view.findViewById<SeekBar>(R.id.seekRecline)
        val foreAft = view.findViewById<SeekBar>(R.id.seekForeAft)
        val scene = view.findViewById<VehicleSceneView>(R.id.vehicleScene)
        val seatSelector = view.findViewById<RadioGroup>(R.id.seatSelector)
        val climateSelector = view.findViewById<RadioGroup>(R.id.climateSelector)
        val cameraStatus = view.findViewById<TextView>(R.id.tvCameraConsentStatus)
        val optIn = view.findViewById<MaterialButton>(R.id.btnCameraOptIn)
        val revoke = view.findViewById<MaterialButton>(R.id.btnCameraRevoke)
        val heat = view.findViewById<MaterialButton>(R.id.btnSeatHeating)
        val vent = view.findViewById<MaterialButton>(R.id.btnSeatVentilation)
        val media = view.findViewById<MaterialButton>(R.id.btnMedia)
        val youtube = view.findViewById<MaterialButton>(R.id.btnYouTube)
        val netflix = view.findViewById<MaterialButton>(R.id.btnNetflix)
        // No trusted parked signal or approved in-vehicle video integration exists here.
        youtube.isEnabled = false
        netflix.isEnabled = false
        youtube.contentDescription = "YouTube, 차량에서 실행 불가. 공식 주차 상태 확인 및 승인된 앱 연동 필요"
        netflix.contentDescription = "Netflix, 차량에서 실행 불가. 공식 주차 상태 확인 및 승인된 앱 연동 필요"
        val seatIds = mapOf(R.id.seatDriver to SeatPosition.DRIVER,
            R.id.seatPassenger to SeatPosition.PASSENGER, R.id.seatRearLeft to SeatPosition.REAR_LEFT,
            R.id.seatRearRight to SeatPosition.REAR_RIGHT)
        val zoneIds = mapOf(R.id.zoneDriver to ClimateZone.DRIVER,
            R.id.zonePassenger to ClimateZone.PASSENGER, R.id.zoneRear to ClimateZone.REAR)
        fun zone() = zoneIds[climateSelector.checkedRadioButtonId] ?: ClimateZone.DRIVER
        fun render() {
            val active = manager.sessionActive.value
            val selected = manager.selectedSeat.value
            val settings = manager.settings(selected)
            status.text = if (active) "● 로컬 시연 중 · 주차/예약 상태 미확인" else "○ 시연 대기 · 차량 연결 없음"
            selectedStatus.text = "선택: ${selected.label} · ${if (selected.front) "등받이/앞뒤 위치 지원" else "등받이/앞뒤 위치 미지원"}"
            temperature.text = "${zone().label} ${manager.temperature(zone())} °C · 모의 목표"
            windowStatus.text = "${selected.label} 창문 열림 ${settings.windowPercent}% · 실제 차량 명령 없음"
            seatPosition.text = if (selected.front) "${selected.label} 등받이 ${settings.reclineDegrees}° · 앞뒤 ${settings.foreAftPercent}%" else "뒤 좌석 위치 조절 미지원 (시연)"
            if (window.progress != settings.windowPercent) window.progress = settings.windowPercent
            if (recline.progress != settings.reclineDegrees - 90) recline.progress = settings.reclineDegrees - 90
            if (foreAft.progress != settings.foreAftPercent) foreAft.progress = settings.foreAftPercent
            window.isEnabled = active
            recline.isEnabled = active && selected.front
            foreAft.isEnabled = active && selected.front
            listOf(R.id.btnTempMinus, R.id.btnTempPlus, R.id.btnSeatHeating,
                R.id.btnSeatVentilation, R.id.btnMedia).forEach { view.findViewById<View>(it).isEnabled = active }
            view.findViewById<MaterialButton>(R.id.btnStartPreview).isEnabled = !active
            view.findViewById<MaterialButton>(R.id.btnCompleteRest).isEnabled = active
            heat.text = if (settings.heating) "열선 켜짐" else "열선 꺼짐"
            vent.text = if (settings.ventilation) "통풍 켜짐" else "통풍 꺼짐"
            heat.isSelected = settings.heating
            vent.isSelected = settings.ventilation
            heat.contentDescription = "${selected.label} 모의 ${heat.text}"
            vent.contentDescription = "${selected.label} 모의 ${vent.text}"
            window.contentDescription = "${selected.label} 모의 창문 열림 ${settings.windowPercent} 퍼센트"
            recline.contentDescription = "${selected.label} 모의 등받이 ${settings.reclineDegrees}도${if (selected.front) "" else ", 미지원"}"
            foreAft.contentDescription = "${selected.label} 모의 앞뒤 위치 ${settings.foreAftPercent} 퍼센트${if (selected.front) "" else ", 미지원"}"
            media.text = if (manager.ambientPlaying.value) "앰비언트 모의 일시정지" else "앰비언트 모의 재생"
            scene.showState(manager.seats.value, selected, manager.ambientPlaying.value)
            optIn.isEnabled = active && !cameraConsent.customerOptedIn
            revoke.isEnabled = active && cameraConsent.customerOptedIn
            cameraStatus.text = if (cameraConsent.customerOptedIn) {
                "동의 시연: 켜짐 · 촬영/전송 없음 · 차주 접근 불가"
            } else {
                "동의 시연: 꺼짐 · 촬영/전송 없음 · 차주 접근 불가"
            }
        }
        seatSelector.setOnCheckedChangeListener { _, id ->
            seatIds[id]?.let { manager.selectSeat(it); render() }
        }
        climateSelector.setOnCheckedChangeListener { _, _ -> render() }
        view.findViewById<MaterialButton>(R.id.btnStartPreview).setOnClickListener {
            manager.startPreview(); cameraConsent.startPreview(); render()
        }
        view.findViewById<MaterialButton>(R.id.btnTempMinus).setOnClickListener {
            manager.setTemperature(zone(), manager.temperature(zone()) - 1); render()
        }
        view.findViewById<MaterialButton>(R.id.btnTempPlus).setOnClickListener {
            manager.setTemperature(zone(), manager.temperature(zone()) + 1); render()
        }
        heat.setOnClickListener { manager.toggleSeatHeating(manager.selectedSeat.value); render() }
        vent.setOnClickListener { manager.toggleSeatVentilation(manager.selectedSeat.value); render() }
        media.setOnClickListener { manager.toggleAmbient(); render() }
        fun bindSeekBar(bar: SeekBar, change: (Int) -> Unit) {
            bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) { change(progress); render() }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        bindSeekBar(window) { manager.setWindowPercent(manager.selectedSeat.value, it) }
        bindSeekBar(recline) { manager.setReclineDegrees(manager.selectedSeat.value, it + 90) }
        bindSeekBar(foreAft) { manager.setForeAftPercent(manager.selectedSeat.value, it) }
        optIn.setOnClickListener { cameraConsent.setCustomerOptIn(true); render() }
        revoke.setOnClickListener { cameraConsent.setCustomerOptIn(false); render() }
        view.findViewById<MaterialButton>(R.id.btnCompleteRest).setOnClickListener {
            cameraConsent.endPreview(); manager.endPreview()
            seatSelector.check(R.id.seatDriver)
            climateSelector.check(R.id.zoneDriver)
            render()
        }
        render()
        return view
    }

    override fun onStop() {
        manager.pauseAmbient()
        view?.let { root ->
            root.findViewById<MaterialButton>(R.id.btnMedia)?.text = "앰비언트 모의 재생"
            root.findViewById<VehicleSceneView>(R.id.vehicleScene)?.showState(
                manager.seats.value, manager.selectedSeat.value, false)
        }
        super.onStop()
    }

    override fun onDestroyView() {
        cameraConsent.endPreview()
        manager.endPreview()
        super.onDestroyView()
    }
}
