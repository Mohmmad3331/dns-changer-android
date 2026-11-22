package com.dnschanger.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

object T {
    var accent = Color.parseColor("#FFC21A")
    val bg = Color.parseColor("#0B0B0F")
    val card = Color.parseColor("#17171F")
    val stroke = Color.parseColor("#2C2C38")
    val text = Color.WHITE
    val sub = Color.parseColor("#9A9AAE")
    val ok = Color.parseColor("#3DDC84")
    val bad = Color.parseColor("#FF5C5C")
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun Context.rect(fill: Int, stroke: Int = 0, r: Int = 14, sw: Int = 1): GradientDrawable = GradientDrawable().apply {
    cornerRadius = dp(r).toFloat()
    setColor(fill)
    if (stroke != 0) setStroke(dp(sw), stroke)
}

fun Context.tv(t: String, size: Float = 14f, color: Int = T.text, bold: Boolean = false): TextView = TextView(this).apply {
    text = t; textSize = size; setTextColor(color)
    if (bold) typeface = Typeface.DEFAULT_BOLD
}

fun Context.goldBtn(t: String, filled: Boolean = true, onClick: () -> Unit): Button = Button(this).apply {
    text = t; isAllCaps = false; textSize = 15f; typeface = Typeface.DEFAULT_BOLD
    setTextColor(if (filled) Color.BLACK else T.accent)
    background = if (filled) rect(T.accent, 0, 14) else rect(Color.TRANSPARENT, T.accent, 14, 2)
    stateListAnimator = null
    setOnClickListener { onClick() }
}

fun LinearLayout.add(v: View, w: Int = -1, h: Int = -2, l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0, weight: Float = 0f): View {
    val c = context
    val p = LinearLayout.LayoutParams(if (w > 0) c.dp(w) else w, if (h > 0) c.dp(h) else h, weight)
    p.setMargins(c.dp(l), c.dp(t), c.dp(r), c.dp(b))
    addView(v, p)
    return v
}

fun bars(ms: Long): String = when {
    ms < 50 -> "▂▄▆█"
    ms < 100 -> "▂▄▆"
    ms < 200 -> "▂▄"
    else -> "▂"
}

fun verdict(ms: Long): String = when {
    ms < 50 -> "عالی"
    ms < 120 -> "خوب"
    else -> "کند"
}

class GaugeView(c: Context) : View(c) {
    var value = 0f
    var label = "—"
    var valueText = "— ms"
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2
        val cy = h * 0.52f
        val r = minOf(w, h) * 0.42f
        val rc = RectF(cx - r, cy - r, cx + r, cy + r)
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = r * 0.14f
        p.color = T.stroke
        cv.drawArc(rc, 150f, 240f, false, p)
        val frac = (value / 200f).coerceIn(0f, 1f)
        p.color = T.accent
        if (frac > 0f) cv.drawArc(rc, 150f, 240f * frac, false, p)
        val ang = Math.toRadians((150 + 240 * frac).toDouble())
        p.strokeWidth = r * 0.04f
        p.color = Color.WHITE
        cv.drawLine(cx, cy, (cx + Math.cos(ang) * r * 0.75).toFloat(), (cy + Math.sin(ang) * r * 0.75).toFloat(), p)
        p.style = Paint.Style.FILL
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.DEFAULT_BOLD
        p.color = Color.WHITE
        p.textSize = r * 0.34f
        cv.drawText(valueText, cx, cy + r * 0.62f, p)
        p.textSize = r * 0.2f
        p.color = T.ok
        cv.drawText(label, cx, cy + r * 0.9f, p)
    }
}
