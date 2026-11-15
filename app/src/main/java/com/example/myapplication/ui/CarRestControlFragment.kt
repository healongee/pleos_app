package com.example.myapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.data.InteriorCameraConsent
import com.example.myapplication.data.VehicleControlManager
import com.google.android.material.button.MaterialButton

/** Offline local preview only; no vehicle SDK, camera capture or network connection. */
class CarRestControlFragment : Fragment() {
    private val manager = VehicleControlManager()
    private val cameraConsent = InteriorCameraConsent()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_car_rest_control, container, false)
        val temperature = view.findViewById<TextView>(R.id.tvCurrentTemp)
        val status = view.findViewById<TextView>(R.id.tvRestTimer)
        val cameraStatus = view.findViewById<TextView>(R.id.tvCameraConsentStatus)
        val optIn = view.findViewById<MaterialButton>(R.id.btnCameraOptIn)
        val revoke = view.findViewById<MaterialButton>(R.id.btnCameraRevoke)
        val controls = listOf(R.id.btnTempMinus, R.id.btnTempPlus, R.id.btnSeatHeating, R.id.btnSeatVentilation, R.id.btnRelaxSeat, R.id.btnWindow, R.id.btnMedia)
        fun render() {
            val active = manager.sessionActive.value
            status.text = if (active) "로컬 시연 중 · 예약 및 주차 상태 미확인" else "시연 종료 · 차량 연결 없음"
            temperature.text = "${manager.targetTemperature.value} °C (모의 값)"
            controls.forEach { view.findViewById<View>(it).isEnabled = active }
            view.findViewById<MaterialButton>(R.id.btnStartPreview).isEnabled = !active
            view.findViewById<MaterialButton>(R.id.btnCompleteRest).isEnabled = active
            optIn.isEnabled = active && !cameraConsent.customerOptedIn
            revoke.isEnabled = active && cameraConsent.customerOptedIn
            cameraStatus.text = if (cameraConsent.customerOptedIn) {
                "고객 동의 시연: 켜짐 · 촬영 및 전송 없음 · 차주 접근 불가"
            } else {
                "고객 동의 시연: 꺼짐 · 촬영 및 전송 없음 · 차주 접근 불가"
            }
        }
        view.findViewById<MaterialButton>(R.id.btnStartPreview).setOnClickListener { manager.startPreview(); cameraConsent.startPreview(); render() }
        view.findViewById<MaterialButton>(R.id.btnTempMinus).setOnClickListener { manager.setTemperature(manager.targetTemperature.value - 1); render() }
        view.findViewById<MaterialButton>(R.id.btnTempPlus).setOnClickListener { manager.setTemperature(manager.targetTemperature.value + 1); render() }
        view.findViewById<MaterialButton>(R.id.btnSeatHeating).setOnClickListener { manager.toggleSeatHeating(); notice("시트 열선 모의 상태 변경 · 차량 명령 없음") }
        view.findViewById<MaterialButton>(R.id.btnSeatVentilation).setOnClickListener { manager.toggleSeatVentilation(); notice("시트 통풍 모의 상태 변경 · 차량 명령 없음") }
        view.findViewById<MaterialButton>(R.id.btnRelaxSeat).setOnClickListener { manager.toggleRelaxSeatMode(); notice("시트 자세 모의 상태 변경 · 차량 명령 없음") }
        view.findViewById<MaterialButton>(R.id.btnWindow).setOnClickListener { notice("창문 제어 불가 · 공식 차량 SDK 필요") }
        view.findViewById<MaterialButton>(R.id.btnMedia).setOnClickListener { notice("미디어 이용 불가 · 승인된 앱과 주차 상태 확인 필요") }
        optIn.setOnClickListener { cameraConsent.setCustomerOptIn(true); render() }
        revoke.setOnClickListener { cameraConsent.setCustomerOptIn(false); render() }
        view.findViewById<MaterialButton>(R.id.btnCompleteRest).setOnClickListener {
            cameraConsent.endPreview(); manager.endPreview(); render()
            notice("로컬 시연 종료 · 동의 철회됨 · 웹 요청에는 영향 없음")
        }
        render()
        return view
    }

    override fun onDestroyView() {
        cameraConsent.endPreview()
        manager.endPreview()
        super.onDestroyView()
    }

    private fun notice(message: String) = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
}
