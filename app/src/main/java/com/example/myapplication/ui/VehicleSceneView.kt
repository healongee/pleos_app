package com.example.myapplication.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.myapplication.data.SeatPosition
import com.example.myapplication.data.SeatSettings

/** Decorative four-seat plan, with independent local simulated seat and window indicators. */
class VehicleSceneView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var seats: Map<SeatPosition, SeatSettings> = SeatPosition.entries.associateWith { SeatSettings() }
    private var selected = SeatPosition.DRIVER
    private var ambient = false
    private val cyan = Color.rgb(82, 220, 226)
    private val orange = Color.rgb(241, 153, 115)

    fun showState(settings: Map<SeatPosition, SeatSettings>, selectedSeat: SeatPosition, playing: Boolean) {
        seats = settings.toMap()
        selected = selectedSeat
        ambient = playing
        contentDescription = "모의 차량 좌석 배치. 선택 ${selected.label}. " + SeatPosition.entries.joinToString("; ") {
            val state = seats.getValue(it)
            "${it.label} 열선 ${if (state.heating) "켜짐" else "꺼짐"}, 통풍 ${if (state.ventilation) "켜짐" else "꺼짐"}, 창문 ${state.windowPercent}%"
        }
        invalidate()
    }

    private fun box(canvas: Canvas, l: Float, t: Float, r: Float, b: Float, radius: Float, color: Int) {
        paint.color = color
        paint.style = Paint.Style.FILL
        rect.set(l, t, r, b)
        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    private fun label(canvas: Canvas, text: String, x: Float, y: Float, size: Float, color: Int) {
        paint.color = color
        paint.textSize = size
        paint.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        canvas.drawText(text, x, y, paint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        val save = canvas.save()
        val scale = minOf(width / 720f, height / 360f)
        canvas.translate((width - 720f * scale) / 2f, (height - 360f * scale) / 2f)
        canvas.scale(scale, scale)
        box(canvas, 0f, 0f, 720f, 360f, 24f, Color.rgb(16, 27, 38))
        box(canvas, 24f, 18f, 696f, 342f, 95f, Color.rgb(48, 74, 86))
        box(canvas, 40f, 32f, 680f, 328f, 76f, Color.rgb(12, 26, 38))
        box(canvas, 52f, 50f, 668f, 310f, 62f, Color.rgb(24, 44, 56))
        label(canvas, "FRONT  ↑", 305f, 77f, 19f, Color.rgb(170, 196, 203))
        if (ambient) box(canvas, 59f, 300f, 661f, 305f, 2f, cyan)
        val positions = listOf(
            Triple(SeatPosition.DRIVER, 125f, 102f), Triple(SeatPosition.PASSENGER, 395f, 102f),
            Triple(SeatPosition.REAR_LEFT, 125f, 211f), Triple(SeatPosition.REAR_RIGHT, 395f, 211f)
        )
        for ((seat, x, y) in positions) {
            val state = seats.getValue(seat)
            val highlight = when {
                state.heating -> orange
                state.ventilation -> cyan
                else -> Color.rgb(106, 141, 153)
            }
            if (seat == selected) box(canvas, x - 13f, y - 12f, x + 213f, y + 104f, 24f, cyan)
            box(canvas, x - 9f, y - 8f, x + 209f, y + 100f, 21f, Color.rgb(22, 40, 52))
            // Individual glass segments visually follow each window's mock opening percentage.
            box(canvas, x, y - 3f, x + 200f, y + 2f, 2f, Color.rgb(66, 97, 107))
            box(canvas, x, y - 3f, x + 200f * (1f - state.windowPercent / 100f), y + 2f, 2f, cyan)
            val offset = if (seat.front) (state.foreAftPercent - 50) * 0.18f else 0f
            val back = if (seat.front) (state.reclineDegrees - 100) * 0.38f else 0f
            box(canvas, x + 25f + offset - back, y + 16f, x + 67f + offset - back, y + 69f, 13f, highlight)
            box(canvas, x + 39f + offset, y + 56f, x + 115f + offset, y + 78f, 11f, highlight)
            label(canvas, when (seat) {
                SeatPosition.DRIVER -> "DRIVER"
                SeatPosition.PASSENGER -> "PASSENGER"
                SeatPosition.REAR_LEFT -> "REAR L"
                SeatPosition.REAR_RIGHT -> "REAR R"
            }, x + 126f, y + 55f, 16f, Color.WHITE)
            if (state.heating || state.ventilation) {
                label(canvas, if (state.heating) "HEAT" else "VENT", x + 126f, y + 77f, 13f, highlight)
            }
        }
        canvas.restoreToCount(save)
    }
}
