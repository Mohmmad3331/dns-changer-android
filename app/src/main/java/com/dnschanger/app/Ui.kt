package com.dnschanger.app

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

object T {
    var accent = Color.parseColor("#FFC21A")
    val bg = Color.parseColor("#08080B")
    val card = Color.parseColor("#15151C")
    val nav = Color.parseColor("#0E0E13")
    val stroke = Color.parseColor("#2B2B35")
    val text = Color.WHITE
    val sub = Color.parseColor("#9A9AAE")
    val ok = Color.parseColor("#3DDC84")
    val bad = Color.parseColor("#FF5C5C")
}

fun withAlpha(c: Int, a: Int): Int = (c and 0x00FFFFFF) or (a shl 24)
fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
fun Context.dpf(v: Float): Float = v * resources.displayMetrics.density

fun Context.rect(fill: Int, stroke: Int = 0, r: Int = 14, sw: Int = 1): GradientDrawable = GradientDrawable().apply {
    cornerRadius = dp(r).toFloat()
    setColor(fill)
    if (stroke != 0) setStroke(dp(sw), stroke)
}

fun Context.tv(t: String, size: Float = 14f, color: Int = T.text, bold: Boolean = false): TextView = TextView(this).apply {
    text = t; textSize = size; setTextColor(color)
    if (bold) typeface = Typeface.DEFAULT_BOLD
}

fun Context.goldBtn(t: String, filled: Boolean = true, onClick: () -> Unit): android.widget.Button = android.widget.Button(this).apply {
    text = t; isAllCaps = false; textSize = 15f; typeface = Typeface.DEFAULT_BOLD
    setTextColor(if (filled) Color.BLACK else T.accent)
    background = if (filled) rect(T.accent, 0, 14) else rect(Color.TRANSPARENT, T.accent, 14, 2)
    stateListAnimator = null
    setOnClickListener { onClick() }
}

fun Context.iconBtn(t: String, kind: String, filled: Boolean, onClick: () -> Unit): LinearLayout {
    val col = if (filled) Color.BLACK else T.accent
    val b = LinearLayout(this)
    b.gravity = Gravity.CENTER
    b.setPadding(dp(14), dp(14), dp(14), dp(14))
    b.background = if (filled) rect(T.accent, 0, 14) else rect(withAlpha(T.accent, 22), T.accent, 14, 2)
    b.isClickable = true
    b.addView(IconView(this, kind, col, 22))
    b.addView(tv(t, 16f, col, true).apply { setPadding(dp(8), 0, 0, 0) })
    b.setOnClickListener { onClick() }
    return b
}

fun LinearLayout.add(v: View, w: Int = -1, h: Int = -2, l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0, weight: Float = 0f): View {
    val c = context
    val p = LinearLayout.LayoutParams(if (w > 0) c.dp(w) else w, if (h > 0) c.dp(h) else h, weight)
    p.setMargins(c.dp(l), c.dp(t), c.dp(r), c.dp(b))
    addView(v, p)
    return v
}

fun Switch.style() {
    val st = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
    thumbTintList = ColorStateList(st, intArrayOf(Color.WHITE, Color.parseColor("#B0B0B8")))
    trackTintList = ColorStateList(st, intArrayOf(T.accent, T.stroke))
}

fun CheckBox.style() {
    buttonTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(T.accent, T.sub))
}

fun verdict(ms: Long): String = when {
    ms < 50 -> "Excellent"
    ms < 120 -> "Good"
    else -> "Slow"
}

fun brandLetter(n: String): String {
    val m = Regex("\\(([A-Za-z0-9]+)").find(n)
    return (m?.groupValues?.get(1) ?: n).take(1).uppercase()
}

fun brandColor(n: String): Int {
    val l = n.lowercase()
    return when {
        "cloudflare" in l -> Color.parseColor("#F6821F")
        "google" in l -> Color.parseColor("#4285F4")
        "quad9" in l -> Color.parseColor("#E5397A")
        "adguard" in l -> Color.parseColor("#2FBF71")
        "opendns" in l -> Color.parseColor("#FF6A00")
        "cleanbrowsing" in l -> Color.parseColor("#2D9CDB")
        else -> intArrayOf(0xFF7B61FF.toInt(), 0xFF00B8D9.toInt(), 0xFFE5397A.toInt(), 0xFF2FBF71.toInt(), 0xFFF6821F.toInt())[Math.abs(n.hashCode()) % 5]
    }
}

object Icons {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    fun draw(c: Canvas, kind: String, cx: Float, cy: Float, r: Float, color: Int, sw: Float = r * 0.17f) {
        p.reset(); p.isAntiAlias = true
        p.color = color; p.style = Paint.Style.STROKE; p.strokeWidth = sw
        p.strokeCap = Paint.Cap.ROUND; p.strokeJoin = Paint.Join.ROUND
        val path = Path()
        when (kind) {
            "home" -> {
                path.moveTo(cx - r, cy - r * 0.05f); path.lineTo(cx, cy - r); path.lineTo(cx + r, cy - r * 0.05f)
                path.moveTo(cx - r * 0.72f, cy - r * 0.25f); path.lineTo(cx - r * 0.72f, cy + r * 0.9f)
                path.lineTo(cx + r * 0.72f, cy + r * 0.9f); path.lineTo(cx + r * 0.72f, cy - r * 0.25f)
                c.drawPath(path, p)
            }
            "speed" -> {
                c.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), 140f, 260f, false, p)
                c.drawLine(cx, cy, cx + r * 0.5f, cy - r * 0.45f, p)
            }
            "globe" -> {
                c.drawCircle(cx, cy, r, p)
                c.drawOval(RectF(cx - r * 0.45f, cy - r, cx + r * 0.45f, cy + r), p)
                c.drawLine(cx - r, cy, cx + r, cy, p)
            }
            "gear" -> {
                c.drawCircle(cx, cy, r * 0.4f, p)
                c.drawCircle(cx, cy, r * 0.72f, p)
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    c.drawLine((cx + Math.cos(a) * r * 0.72).toFloat(), (cy + Math.sin(a) * r * 0.72).toFloat(),
                        (cx + Math.cos(a) * r).toFloat(), (cy + Math.sin(a) * r).toFloat(), p)
                }
            }
            "bolt" -> {
                p.style = Paint.Style.FILL
                path.moveTo(cx + r * 0.25f, cy - r); path.lineTo(cx - r * 0.55f, cy + r * 0.1f); path.lineTo(cx - r * 0.05f, cy + r * 0.1f)
                path.lineTo(cx - r * 0.25f, cy + r); path.lineTo(cx + r * 0.55f, cy - r * 0.15f); path.lineTo(cx + r * 0.05f, cy - r * 0.15f)
                path.close(); c.drawPath(path, p)
            }
            "wifi" -> {
                for (k in 1..3) {
                    val rr = r * k * 0.4f
                    c.drawArc(RectF(cx - rr, cy + r * 0.55f - rr, cx + rr, cy + r * 0.55f + rr), 225f, 90f, false, p)
                }
                p.style = Paint.Style.FILL; c.drawCircle(cx, cy + r * 0.55f, sw * 0.7f, p)
            }
            "search" -> {
                c.drawCircle(cx - r * 0.15f, cy - r * 0.15f, r * 0.62f, p)
                c.drawLine(cx + r * 0.3f, cy + r * 0.3f, cx + r * 0.95f, cy + r * 0.95f, p)
            }
            "back" -> {
                path.moveTo(cx + r * 0.8f, cy); path.lineTo(cx - r * 0.8f, cy)
                path.moveTo(cx - r * 0.1f, cy - r * 0.7f); path.lineTo(cx - r * 0.8f, cy); path.lineTo(cx - r * 0.1f, cy + r * 0.7f)
                c.drawPath(path, p)
            }
            "star", "starf" -> {
                if (kind == "starf") p.style = Paint.Style.FILL_AND_STROKE
                for (i in 0 until 10) {
                    val a = Math.toRadians(-90.0 + i * 36.0)
                    val rr = if (i % 2 == 0) r else r * 0.45f
                    val x = (cx + Math.cos(a) * rr).toFloat(); val y = (cy + Math.sin(a) * rr).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close(); c.drawPath(path, p)
            }
            "check" -> {
                path.moveTo(cx - r * 0.7f, cy); path.lineTo(cx - r * 0.2f, cy + r * 0.55f); path.lineTo(cx + r * 0.75f, cy - r * 0.5f)
                c.drawPath(path, p)
            }
            "shield" -> {
                path.moveTo(cx, cy - r); path.lineTo(cx + r * 0.8f, cy - r * 0.6f); path.lineTo(cx + r * 0.8f, cy + r * 0.1f)
                path.quadTo(cx + r * 0.8f, cy + r * 0.7f, cx, cy + r)
                path.quadTo(cx - r * 0.8f, cy + r * 0.7f, cx - r * 0.8f, cy + r * 0.1f)
                path.lineTo(cx - r * 0.8f, cy - r * 0.6f); path.close(); c.drawPath(path, p)
            }
            "shieldcheck" -> {
                draw(c, "shield", cx, cy, r, color, sw)
                draw(c, "check", cx, cy + r * 0.05f, r * 0.45f, color, sw)
            }
            "power" -> {
                c.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), 305f, 290f, false, p)
                c.drawLine(cx, cy - r * 1.15f, cx, cy - r * 0.1f, p)
            }
            "menu" -> {
                c.drawLine(cx - r, cy - r * 0.6f, cx + r, cy - r * 0.6f, p)
                c.drawLine(cx - r, cy, cx + r, cy, p)
                c.drawLine(cx - r, cy + r * 0.6f, cx + r, cy + r * 0.6f, p)
            }
            "chev" -> {
                path.moveTo(cx - r * 0.3f, cy - r * 0.7f); path.lineTo(cx + r * 0.4f, cy); path.lineTo(cx - r * 0.3f, cy + r * 0.7f)
                c.drawPath(path, p)
            }
            "plus" -> {
                c.drawLine(cx - r, cy, cx + r, cy, p); c.drawLine(cx, cy - r, cx, cy + r, p)
            }
            "play" -> {
                p.style = Paint.Style.FILL
                path.moveTo(cx - r * 0.6f, cy - r * 0.8f); path.lineTo(cx + r * 0.8f, cy); path.lineTo(cx - r * 0.6f, cy + r * 0.8f)
                path.close(); c.drawPath(path, p)
            }
            "info" -> {
                c.drawCircle(cx, cy, r, p)
                c.drawLine(cx, cy - r * 0.05f, cx, cy + r * 0.5f, p)
                p.style = Paint.Style.FILL; c.drawCircle(cx, cy - r * 0.45f, sw * 0.65f, p)
            }
            "server" -> {
                c.drawRoundRect(RectF(cx - r, cy - r * 0.85f, cx + r, cy - r * 0.05f), r * 0.2f, r * 0.2f, p)
                c.drawRoundRect(RectF(cx - r, cy + r * 0.05f, cx + r, cy + r * 0.85f), r * 0.2f, r * 0.2f, p)
                p.style = Paint.Style.FILL
                c.drawCircle(cx - r * 0.55f, cy - r * 0.45f, sw * 0.5f, p); c.drawCircle(cx - r * 0.55f, cy + r * 0.45f, sw * 0.5f, p)
            }
            "tag" -> {
                path.moveTo(cx - r, cy - r * 0.6f); path.lineTo(cx + r * 0.2f, cy - r * 0.6f); path.lineTo(cx + r, cy)
                path.lineTo(cx + r * 0.2f, cy + r * 0.6f); path.lineTo(cx - r, cy + r * 0.6f); path.close(); c.drawPath(path, p)
                p.style = Paint.Style.FILL; c.drawCircle(cx - r * 0.45f, cy, sw * 0.6f, p)
            }
            "bell" -> {
                c.drawArc(RectF(cx - r * 0.6f, cy - r * 0.95f, cx + r * 0.6f, cy + r * 0.25f), 180f, 180f, false, p)
                path.moveTo(cx - r * 0.6f, cy - r * 0.35f); path.lineTo(cx - r * 0.8f, cy + r * 0.5f); path.lineTo(cx + r * 0.8f, cy + r * 0.5f)
                path.lineTo(cx + r * 0.6f, cy - r * 0.35f); c.drawPath(path, p)
                p.style = Paint.Style.FILL; c.drawCircle(cx, cy + r * 0.8f, sw * 0.7f, p)
            }
            "alert" -> {
                path.moveTo(cx, cy - r); path.lineTo(cx + r, cy + r * 0.8f); path.lineTo(cx - r, cy + r * 0.8f); path.close(); c.drawPath(path, p)
                c.drawLine(cx, cy - r * 0.25f, cx, cy + r * 0.25f, p)
                p.style = Paint.Style.FILL; c.drawCircle(cx, cy + r * 0.55f, sw * 0.55f, p)
            }
            "moon" -> {
                p.style = Paint.Style.FILL
                val a = Path(); a.addCircle(cx, cy, r, Path.Direction.CW)
                val b = Path(); b.addCircle(cx + r * 0.55f, cy - r * 0.35f, r * 0.85f, Path.Direction.CW)
                a.op(b, Path.Op.DIFFERENCE); c.drawPath(a, p)
            }
            "drop" -> {
                path.moveTo(cx, cy - r); path.cubicTo(cx + r * 0.95f, cy - r * 0.1f, cx + r * 0.8f, cy + r, cx, cy + r)
                path.cubicTo(cx - r * 0.8f, cy + r, cx - r * 0.95f, cy - r * 0.1f, cx, cy - r); c.drawPath(path, p)
            }
        }
    }
}

class IconView(c: Context, var kind: String, var color: Int, private val sizeDp: Int = 24) : View(c) {
    override fun onMeasure(w: Int, h: Int) { val s = context.dp(sizeDp); setMeasuredDimension(s, s) }
    override fun onDraw(cv: Canvas) { Icons.draw(cv, kind, width / 2f, height / 2f, width * 0.38f, color) }
}

class LogoView(c: Context, private val letter: String, private val color: Int, private val sizeDp: Int = 42) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onMeasure(w: Int, h: Int) { val s = context.dp(sizeDp); setMeasuredDimension(s, s) }
    override fun onDraw(cv: Canvas) {
        val r = width / 2f
        p.color = color; p.style = Paint.Style.FILL
        cv.drawCircle(r, r, r, p)
        p.color = Color.WHITE; p.typeface = Typeface.DEFAULT_BOLD; p.textAlign = Paint.Align.CENTER; p.textSize = r * 1.05f
        cv.drawText(letter, r, r + p.textSize * 0.35f, p)
    }
}

class BarsView(c: Context, private val ms: Long) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onMeasure(w: Int, h: Int) { setMeasuredDimension(context.dp(24), context.dp(18)) }
    override fun onDraw(cv: Canvas) {
        val n = when { ms < 50 -> 4; ms < 100 -> 3; ms < 200 -> 2; else -> 1 }
        val bw = width / 7f
        for (i in 0 until 4) {
            p.color = if (i < n) T.ok else T.stroke
            val top = height * (1f - (i + 1) / 4f)
            cv.drawRoundRect(RectF(i * bw * 1.7f, top, i * bw * 1.7f + bw, height.toFloat()), 2f, 2f, p)
        }
    }
}

class GlowBg(c: Context) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        cv.drawColor(T.bg)
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(w * 0.5f, h, h * 0.55f, withAlpha(T.accent, 60), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, w, h, p)
        p.shader = RadialGradient(w * 0.2f, 0f, h * 0.35f, withAlpha(T.accent, 40), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, w, h, p)
        p.style = Paint.Style.STROKE
        for (k in 0..3) {
            val y = h * (0.80f + 0.04f * k)
            val path = Path()
            path.moveTo(-w * 0.1f, y)
            path.cubicTo(w * 0.3f, y - h * (0.12f - 0.02f * k), w * 0.6f, y + h * (0.1f - 0.015f * k), w * 1.1f, y - h * 0.06f)
            p.shader = LinearGradient(0f, 0f, w, 0f, intArrayOf(Color.TRANSPARENT, withAlpha(T.accent, 150 - k * 25), Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
            p.strokeWidth = context.dpf(1.4f)
            cv.drawPath(path, p)
        }
        for (k in 0..1) {
            val y = h * (0.08f + 0.03f * k)
            val path = Path()
            path.moveTo(-w * 0.1f, y + h * 0.05f)
            path.cubicTo(w * 0.3f, y - h * 0.05f, w * 0.6f, y + h * 0.08f, w * 1.1f, y - h * 0.03f)
            p.shader = LinearGradient(0f, 0f, w, 0f, intArrayOf(Color.TRANSPARENT, withAlpha(T.accent, 90), Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
            cv.drawPath(path, p)
        }
        p.shader = null
    }
}

class PowerView(c: Context) : View(c) {
    var on = false
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        val cx = width / 2f; val cy = height / 2f
        val r = minOf(width, height) * 0.36f
        if (on) {
            p.style = Paint.Style.FILL
            p.shader = RadialGradient(cx, cy, r * 1.7f, intArrayOf(withAlpha(T.accent, 110), withAlpha(T.accent, 30), Color.TRANSPARENT), floatArrayOf(0.5f, 0.75f, 1f), Shader.TileMode.CLAMP)
            cv.drawCircle(cx, cy, r * 1.7f, p)
            p.shader = null
        }
        p.style = Paint.Style.FILL
        p.color = Color.parseColor("#0C0C10")
        cv.drawCircle(cx, cy, r, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = context.dpf(7f)
        if (on) {
            p.shader = SweepGradient(cx, cy, intArrayOf(T.accent, Color.parseColor("#FFE9A8"), T.accent, Color.parseColor("#B07A00"), T.accent), null)
        } else p.color = T.stroke
        cv.drawCircle(cx, cy, r, p)
        p.shader = null
        if (on) {
            p.strokeWidth = context.dpf(1.5f)
            p.color = withAlpha(T.accent, 120)
            cv.drawCircle(cx, cy, r * 1.14f, p)
        }
        Icons.draw(cv, "power", cx, cy + r * 0.05f, r * 0.42f, if (on) T.accent else T.sub, r * 0.1f)
    }
}

class NavView(c: Context, private val onSel: (Int) -> Unit) : View(c) {
    var sel = 0
    private val items = listOf("Home" to "home", "Speed" to "speed", "Providers" to "globe", "Settings" to "gear")
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onMeasure(w: Int, h: Int) { setMeasuredDimension(MeasureSpec.getSize(w), context.dp(66)) }
    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        p.style = Paint.Style.FILL; p.color = T.nav; cv.drawRect(0f, 0f, w, h, p)
        p.style = Paint.Style.STROKE; p.color = T.stroke; p.strokeWidth = 2f; cv.drawLine(0f, 1f, w, 1f, p)
        items.forEachIndexed { i, (name, icon) ->
            val cx = w * (i + 0.5f) / items.size
            val col = if (i == sel) T.accent else T.sub
            if (i == sel) {
                p.style = Paint.Style.FILL
                p.shader = RadialGradient(cx, h * 0.38f, context.dpf(30f), withAlpha(T.accent, 70), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                cv.drawCircle(cx, h * 0.38f, context.dpf(30f), p)
                p.shader = null
            }
            Icons.draw(cv, icon, cx, h * 0.38f, context.dpf(10f), col)
            p.style = Paint.Style.FILL; p.color = col; p.textAlign = Paint.Align.CENTER
            p.typeface = if (i == sel) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            p.textSize = context.dpf(11f)
            cv.drawText(name, cx, h * 0.82f, p)
        }
    }
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_UP) {
            val i = (e.x / (width / items.size.toFloat())).toInt().coerceIn(0, items.size - 1)
            performClick(); onSel(i)
        }
        return true
    }
    override fun performClick(): Boolean = super.performClick()
}

class GaugeView(c: Context) : View(c) {
    var value = 0f
    var label = "—"
    var valueText = "— ms"
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2; val cy = h * 0.52f
        val r = minOf(w, h) * 0.40f
        val rc = RectF(cx - r, cy - r, cx + r, cy + r)
        p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = r * 0.12f
        p.color = T.stroke
        cv.drawArc(rc, 150f, 240f, false, p)
        val frac = (value / 200f).coerceIn(0f, 1f)
        if (frac > 0f) {
            p.color = withAlpha(T.accent, 60); p.strokeWidth = r * 0.2f
            cv.drawArc(rc, 150f, 240f * frac, false, p)
            p.color = T.accent; p.strokeWidth = r * 0.12f
            cv.drawArc(rc, 150f, 240f * frac, false, p)
        }
        p.strokeWidth = 2f; p.color = T.sub
        p.style = Paint.Style.FILL; p.textAlign = Paint.Align.CENTER; p.textSize = r * 0.14f; p.typeface = Typeface.DEFAULT
        for (v in listOf(0, 50, 100, 150, 200)) {
            val a = Math.toRadians(150.0 + 240.0 * v / 200.0)
            val x = (cx + Math.cos(a) * r * 0.74).toFloat(); val y = (cy + Math.sin(a) * r * 0.74).toFloat() + p.textSize * 0.35f
            cv.drawText(v.toString(), x, y, p)
        }
        val ang = Math.toRadians(150.0 + 240.0 * frac)
        p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND; p.strokeWidth = r * 0.045f; p.color = T.accent
        cv.drawLine((cx + Math.cos(ang) * r * 0.25).toFloat(), (cy + Math.sin(ang) * r * 0.25).toFloat(),
            (cx + Math.cos(ang) * r * 0.62).toFloat(), (cy + Math.sin(ang) * r * 0.62).toFloat(), p)
        p.style = Paint.Style.FILL; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
        p.color = Color.WHITE; p.textSize = r * 0.4f
        cv.drawText(valueText, cx, cy + r * 0.72f, p)
        p.textSize = r * 0.2f; p.color = T.ok
        cv.drawText(label, cx, cy + r * 1.0f, p)
    }
}
