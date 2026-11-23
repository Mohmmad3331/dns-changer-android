package com.dnschanger.app

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
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

fun lighten(c: Int, f: Float): Int {
    fun m(v: Int) = (v + (255 - v) * f).toInt().coerceIn(0, 255)
    return Color.argb(Color.alpha(c), m(Color.red(c)), m(Color.green(c)), m(Color.blue(c)))
}

fun darken(c: Int, f: Float): Int {
    fun m(v: Int) = (v * (1f - f)).toInt().coerceIn(0, 255)
    return Color.argb(Color.alpha(c), m(Color.red(c)), m(Color.green(c)), m(Color.blue(c)))
}

/** متن با گرادیانت طلایی فلزی */
fun TextView.gold(): TextView {
    paint.shader = LinearGradient(0f, 0f, 0f, textSize * 1.3f,
        intArrayOf(lighten(T.accent, 0.65f), T.accent, darken(T.accent, 0.3f)), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
    return this
}

/** پس‌زمینهٔ برجسته: دکمهٔ طلایی فلزی یا کارت بافت‌دار با حاشیهٔ طلایی */
class FancyDrawable(private val gold: Boolean, private val fill: Int, private val stroke: Int, private val rad: Float, private val d: Float) : Drawable() {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clip = Path()

    override fun draw(cv: Canvas) {
        val b = bounds
        val w = b.width().toFloat(); val h = b.height().toFloat()
        if (w <= 0f || h <= 0f) return
        cv.save(); cv.translate(b.left.toFloat(), b.top.toFloat())
        if (gold) {
            val a = T.accent
            val ed = d * 2f
            p.style = Paint.Style.FILL; p.shader = null; p.color = darken(a, 0.62f)
            cv.drawRoundRect(RectF(0f, ed, w, h), rad, rad, p)
            val face = RectF(0f, 0f, w, h - ed)
            p.shader = LinearGradient(0f, 0f, 0f, face.height(), intArrayOf(lighten(a, 0.6f), lighten(a, 0.15f), a, darken(a, 0.3f)), floatArrayOf(0f, 0.35f, 0.6f, 1f), Shader.TileMode.CLAMP)
            cv.drawRoundRect(face, rad, rad, p)
            clip.reset(); clip.addRoundRect(face, rad, rad, Path.Direction.CW)
            cv.save(); cv.clipPath(clip)
            p.shader = LinearGradient(0f, 0f, 0f, face.height() * 0.5f, Color.argb(110, 255, 255, 255), Color.argb(0, 255, 255, 255), Shader.TileMode.CLAMP)
            cv.drawRect(0f, 0f, w, face.height() * 0.5f, p)
            p.shader = null; p.style = Paint.Style.STROKE; p.strokeWidth = d * 0.7f; p.color = Color.argb(22, 0, 0, 0)
            var x = -face.height()
            while (x < w) { cv.drawLine(x, face.height(), x + face.height(), 0f, p); x += d * 5f }
            cv.restore()
            p.style = Paint.Style.STROKE; p.strokeWidth = d * 1.2f; p.color = withAlpha(lighten(a, 0.8f), 210)
            cv.drawRoundRect(RectF(d * 0.6f, d * 0.6f, w - d * 0.6f, face.height() - d * 0.6f), rad, rad, p)
        } else {
            val al = Color.alpha(fill)
            p.style = Paint.Style.FILL
            p.shader = LinearGradient(0f, 0f, 0f, h, Color.argb(al, 36, 36, 46), Color.argb(al, 15, 15, 20), Shader.TileMode.CLAMP)
            val full = RectF(0f, 0f, w, h)
            cv.drawRoundRect(full, rad, rad, p)
            clip.reset(); clip.addRoundRect(full, rad, rad, Path.Direction.CW)
            cv.save(); cv.clipPath(clip)
            p.shader = null; p.style = Paint.Style.STROKE; p.strokeWidth = d * 0.6f; p.color = Color.argb(11, 255, 255, 255)
            var x = -h
            while (x < w) { cv.drawLine(x, h, x + h, 0f, p); x += d * 6f }
            p.style = Paint.Style.FILL
            p.shader = RadialGradient(0f, 0f, maxOf(w * 0.8f, 1f), withAlpha(T.accent, 34), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            cv.drawRect(0f, 0f, w, h, p)
            cv.restore()
            val sc = if (stroke != 0) stroke else T.stroke
            p.style = Paint.Style.STROKE; p.strokeWidth = d * 1.2f
            p.shader = LinearGradient(0f, 0f, w, h, intArrayOf(withAlpha(lighten(T.accent, 0.2f), 170), sc, withAlpha(T.accent, 80)), null, Shader.TileMode.CLAMP)
            cv.drawRoundRect(RectF(d * 0.6f, d * 0.6f, w - d * 0.6f, h - d * 0.6f), rad, rad, p)
            p.shader = null; p.strokeWidth = d * 0.8f; p.color = Color.argb(34, 255, 255, 255)
            cv.drawLine(rad, d * 2f, w - rad, d * 2f, p)
        }
        p.shader = null
        cv.restore()
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(cf: ColorFilter?) {}
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

fun Context.rect(fill: Int, stroke: Int = 0, r: Int = 14, sw: Int = 1): Drawable {
    val rgb = fill and 0xFFFFFF
    val alpha = (fill ushr 24) and 0xFF
    if (stroke == 0 && alpha == 255 && rgb == (T.accent and 0xFFFFFF)) return FancyDrawable(true, fill, stroke, dpf(r.toFloat()), dpf(1f))
    if (rgb == (T.card and 0xFFFFFF) && alpha > 100) return FancyDrawable(false, fill, stroke, dpf(r.toFloat()), dpf(1f))
    return GradientDrawable().apply {
        cornerRadius = dp(r).toFloat()
        setColor(fill)
        if (stroke != 0) setStroke(dp(sw), stroke)
    }
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

class LogoView(c: Context, private val letter: String, private val color: Int, private val sizeDp: Int = 42, private val key: String = "") : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onMeasure(w: Int, h: Int) { val s = context.dp(sizeDp); setMeasuredDimension(s, s) }
    override fun onDraw(cv: Canvas) {
        val s = width.toFloat()
        val r = s / 2
        val k = key.lowercase()
        p.reset(); p.isAntiAlias = true; p.style = Paint.Style.FILL
        when {
            "cloudflare" in k -> {
                p.color = Color.parseColor("#F6821F")
                cv.drawCircle(s * 0.30f, s * 0.62f, s * 0.17f, p)
                cv.drawCircle(s * 0.50f, s * 0.44f, s * 0.24f, p)
                cv.drawCircle(s * 0.72f, s * 0.60f, s * 0.19f, p)
                cv.drawRoundRect(RectF(s * 0.13f, s * 0.58f, s * 0.91f, s * 0.80f), s * 0.1f, s * 0.1f, p)
            }
            "google" in k -> {
                p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.16f
                val rc = RectF(s * 0.2f, s * 0.2f, s * 0.8f, s * 0.8f)
                p.color = Color.parseColor("#EA4335"); cv.drawArc(rc, 210f, 100f, false, p)
                p.color = Color.parseColor("#FBBC05"); cv.drawArc(rc, 130f, 80f, false, p)
                p.color = Color.parseColor("#34A853"); cv.drawArc(rc, 50f, 80f, false, p)
                p.color = Color.parseColor("#4285F4"); cv.drawArc(rc, 340f, 70f, false, p)
                cv.drawLine(s * 0.5f, s * 0.5f, s * 0.8f, s * 0.5f, p)
            }
            "quad9" in k -> {
                p.color = Color.parseColor("#E5397A")
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    cv.drawCircle((r + Math.cos(a) * s * 0.27).toFloat(), (r + Math.sin(a) * s * 0.27).toFloat(), s * 0.12f, p)
                }
                p.color = Color.parseColor("#F7A1C0"); cv.drawCircle(r, r, s * 0.12f, p)
            }
            "adguard" in k -> {
                val path = Path()
                path.moveTo(r, s * 0.08f); path.lineTo(s * 0.88f, s * 0.24f); path.lineTo(s * 0.88f, s * 0.55f)
                path.quadTo(s * 0.88f, s * 0.82f, r, s * 0.94f); path.quadTo(s * 0.12f, s * 0.82f, s * 0.12f, s * 0.55f)
                path.lineTo(s * 0.12f, s * 0.24f); path.close()
                p.color = Color.parseColor("#2FBF71"); cv.drawPath(path, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.08f; p.strokeCap = Paint.Cap.ROUND; p.strokeJoin = Paint.Join.ROUND
                p.color = Color.WHITE
                val ck = Path(); ck.moveTo(s * 0.32f, s * 0.5f); ck.lineTo(s * 0.46f, s * 0.64f); ck.lineTo(s * 0.70f, s * 0.36f)
                cv.drawPath(ck, p)
            }
            "opendns" in k -> {
                p.color = Color.parseColor("#FF6A00")
                cv.drawRoundRect(RectF(0f, 0f, s, s), s * 0.22f, s * 0.22f, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.11f; p.color = Color.WHITE
                cv.drawOval(RectF(s * 0.32f, s * 0.2f, s * 0.68f, s * 0.8f), p)
            }
            "cleanbrowsing" in k -> {
                p.color = Color.parseColor("#2D9CDB"); cv.drawCircle(r, r, r, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.09f; p.strokeCap = Paint.Cap.ROUND; p.color = Color.WHITE
                cv.drawArc(RectF(s * 0.2f, s * 0.2f, s * 0.8f, s * 0.8f), 30f, 250f, false, p)
                cv.drawArc(RectF(s * 0.36f, s * 0.36f, s * 0.64f, s * 0.64f), 200f, 250f, false, p)
            }
            else -> {
                p.color = color
                cv.drawCircle(r, r, r, p)
                p.color = Color.WHITE; p.typeface = Typeface.DEFAULT_BOLD; p.textAlign = Paint.Align.CENTER; p.textSize = r * 1.05f
                cv.drawText(letter, r, r + p.textSize * 0.35f, p)
            }
        }
    }
}

fun Context.logo(name: String, size: Int): View = LogoView(this, brandLetter(name), brandColor(name), size, name)

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
    private var grain = FloatArray(0)
    private var dots = FloatArray(0)

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val rnd = java.util.Random(11)
        val g = FloatArray(1800)
        for (i in 0 until 900) { g[i * 2] = rnd.nextFloat() * w; g[i * 2 + 1] = rnd.nextFloat() * h }
        grain = g
        val sp = context.dpf(8f)
        val list = ArrayList<Float>()
        var y = h * 0.05f
        while (y < h * 0.64f) {
            var x = 0f
            while (x < w) {
                val fx = x / w
                val fy = y / h
                val v = Math.sin(fx * 9.0) + Math.cos(fy * 11.0 + fx * 3.0) + Math.sin((fx + fy) * 14.0) * 0.7
                val dx = fx - 0.5f
                val dy = (fy - 0.3f) * 1.3f
                if (v > 0.55 && dx * dx + dy * dy < 0.22f) { list.add(x); list.add(y) }
                x += sp
            }
            y += sp
        }
        dots = list.toFloatArray()
    }

    private fun waveY(x: Float, base: Float, amp: Float, len: Float, ph: Float): Float {
        val a = Math.sin(2.0 * Math.PI * x / len + ph)
        val b = Math.sin(2.0 * Math.PI * x / (len * 0.53) + ph * 1.7) * 0.4
        return base + (amp * (a + b)).toFloat()
    }

    private fun wave(cv: Canvas, w: Float, h: Float, base: Float, amp: Float, len: Float, ph: Float, a: Int, fillIt: Boolean) {
        val d = context.dpf(1f)
        val path = Path()
        var x = -10f
        path.moveTo(x, waveY(x, base, amp, len, ph))
        while (x < w + 10f) { x += 10f; path.lineTo(x, waveY(x, base, amp, len, ph)) }
        if (fillIt) {
            val f = Path(path)
            f.lineTo(w + 10f, h); f.lineTo(-10f, h); f.close()
            p.style = Paint.Style.FILL
            p.shader = LinearGradient(0f, base - amp, 0f, h, withAlpha(T.accent, a / 7), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            cv.drawPath(f, p)
        }
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.shader = null
        p.strokeWidth = d * 8f; p.color = withAlpha(T.accent, a / 8)
        cv.drawPath(path, p)
        p.strokeWidth = d * 3.5f; p.color = withAlpha(T.accent, a / 3)
        cv.drawPath(path, p)
        p.strokeWidth = d * 1.3f
        p.shader = LinearGradient(0f, 0f, w, 0f,
            intArrayOf(Color.TRANSPARENT, withAlpha(darken(T.accent, 0.35f), a), withAlpha(lighten(T.accent, 0.55f), a), withAlpha(T.accent, a), Color.TRANSPARENT),
            floatArrayOf(0f, 0.2f, 0.5f, 0.8f, 1f), Shader.TileMode.CLAMP)
        cv.drawPath(path, p)
        p.shader = null
    }

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); val d = context.dpf(1f)
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, 0f, 0f, h, Color.parseColor("#0E0E14"), Color.parseColor("#050507"), Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, w, h, p)
        p.shader = RadialGradient(w * 0.5f, h, h * 0.65f, withAlpha(T.accent, 95), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, w, h, p)
        p.shader = RadialGradient(w * 0.15f, 0f, h * 0.4f, withAlpha(T.accent, 55), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, w, h, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = d * 2.2f
        p.color = withAlpha(T.accent, 50)
        cv.drawPoints(dots, p)
        p.strokeWidth = d * 1f
        p.color = Color.argb(28, 255, 255, 255)
        cv.drawPoints(grain, p)
        for (k in 0..6) {
            wave(cv, w, h, h * (0.72f + 0.045f * k), h * (0.045f - 0.003f * k), w * (1.3f - 0.07f * k), k * 0.7f, 235 - k * 22, true)
        }
        for (k in 0..2) {
            wave(cv, w, h, h * (0.05f + 0.03f * k), h * 0.022f, w * 1.1f, 2f + k, 120 - k * 20, false)
        }
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(w / 2, h / 2, maxOf(w, h) * 0.8f, intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.argb(170, 0, 0, 0)), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, w, h, p)
        p.shader = null
    }
}

class PowerView(c: Context) : View(c) {
    var on = false
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        val cx = width / 2f; val cy = height / 2f
        val r = minOf(width, height) * 0.34f
        val d = context.dpf(1f)
        val gl = if (on) 140 else 40
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(cx, cy, r * 1.9f, intArrayOf(withAlpha(T.accent, gl), withAlpha(T.accent, gl / 4), Color.TRANSPARENT), floatArrayOf(0.45f, 0.75f, 1f), Shader.TileMode.CLAMP)
        cv.drawCircle(cx, cy, r * 1.9f, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = d * 2f
        p.color = withAlpha(T.accent, if (on) 170 else 55)
        for (i in 0 until 60) {
            val a = Math.toRadians(i * 6.0)
            val r2 = if (i % 5 == 0) r * 1.38 else r * 1.31
            cv.drawLine((cx + Math.cos(a) * r * 1.22).toFloat(), (cy + Math.sin(a) * r * 1.22).toFloat(),
                (cx + Math.cos(a) * r2).toFloat(), (cy + Math.sin(a) * r2).toFloat(), p)
        }
        p.style = Paint.Style.FILL
        p.color = Color.parseColor("#050507")
        cv.drawCircle(cx, cy, r * 1.12f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = d * 10f
        p.shader = if (on) SweepGradient(cx, cy, intArrayOf(darken(T.accent, 0.5f), T.accent, lighten(T.accent, 0.75f), T.accent, darken(T.accent, 0.5f), T.accent, lighten(T.accent, 0.55f), T.accent, darken(T.accent, 0.5f)), null)
        else SweepGradient(cx, cy, intArrayOf(Color.parseColor("#2C2C34"), Color.parseColor("#6A6A78"), Color.parseColor("#34343C"), Color.parseColor("#5A5A66"), Color.parseColor("#2C2C34")), null)
        cv.drawCircle(cx, cy, r, p)
        p.shader = null
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(cx, cy - r * 0.3f, r, Color.parseColor("#2A2A34"), Color.parseColor("#09090C"), Shader.TileMode.CLAMP)
        cv.drawCircle(cx, cy, r - d * 6f, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = d * 1.2f
        p.color = withAlpha(lighten(T.accent, 0.5f), if (on) 170 else 40)
        cv.drawCircle(cx, cy, r - d * 6f, p)
        if (on) Icons.draw(cv, "power", cx, cy + r * 0.05f, r * 0.42f, withAlpha(T.accent, 80), r * 0.24f)
        Icons.draw(cv, "power", cx, cy + r * 0.05f, r * 0.42f, if (on) lighten(T.accent, 0.3f) else T.sub, r * 0.1f)
    }
}

class NavView(c: Context, private val onSel: (Int) -> Unit) : View(c) {
    var sel = 0
    private val items = listOf("Home" to "home", "Speed" to "speed", "Providers" to "globe", "Settings" to "gear")
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onMeasure(w: Int, h: Int) { setMeasuredDimension(MeasureSpec.getSize(w), context.dp(66)) }
    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); val d = context.dpf(1f)
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, 0f, 0f, h, Color.parseColor("#17171F"), Color.parseColor("#09090D"), Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, w, h, p)
        p.shader = LinearGradient(0f, 0f, w, 0f, intArrayOf(Color.TRANSPARENT, withAlpha(T.accent, 220), Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
        p.style = Paint.Style.STROKE; p.strokeWidth = d * 2f
        cv.drawLine(0f, d, w, d, p)
        p.shader = null
        items.forEachIndexed { i, (name, icon) ->
            val cx = w * (i + 0.5f) / items.size
            val col = if (i == sel) lighten(T.accent, 0.2f) else T.sub
            if (i == sel) {
                p.style = Paint.Style.FILL
                p.shader = RadialGradient(cx, h * 0.38f, d * 34f, withAlpha(T.accent, 90), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                cv.drawCircle(cx, h * 0.38f, d * 34f, p)
                p.shader = LinearGradient(cx - d * 16f, 0f, cx + d * 16f, 0f, intArrayOf(Color.TRANSPARENT, T.accent, Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
                cv.drawRoundRect(RectF(cx - d * 16f, d * 1f, cx + d * 16f, d * 4f), d * 2f, d * 2f, p)
                p.shader = null
            }
            Icons.draw(cv, icon, cx, h * 0.38f, d * 10f, col)
            p.style = Paint.Style.FILL; p.color = col; p.textAlign = Paint.Align.CENTER
            p.typeface = if (i == sel) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            p.textSize = d * 11f
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
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(cx, cy, r * 1.3f, withAlpha(T.accent, 38), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        cv.drawCircle(cx, cy, r * 1.3f, p)
        p.shader = null
        p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = r * 0.14f
        p.color = Color.parseColor("#1E1E26")
        cv.drawArc(rc, 150f, 240f, false, p)
        p.strokeWidth = r * 0.02f
        p.color = Color.argb(30, 255, 255, 255)
        cv.drawArc(RectF(rc.left - r * 0.08f, rc.top - r * 0.08f, rc.right + r * 0.08f, rc.bottom + r * 0.08f), 150f, 240f, false, p)
        val frac = (value / 200f).coerceIn(0f, 1f)
        if (frac > 0f) {
            val sg = SweepGradient(cx, cy, intArrayOf(darken(T.accent, 0.45f), T.accent, lighten(T.accent, 0.6f), lighten(T.accent, 0.6f), darken(T.accent, 0.45f)), floatArrayOf(0f, 0.3f, 0.62f, 0.66f, 1f))
            val m = Matrix(); m.postRotate(150f, cx, cy); sg.setLocalMatrix(m)
            p.color = withAlpha(T.accent, 55); p.strokeWidth = r * 0.26f
            cv.drawArc(rc, 150f, 240f * frac, false, p)
            p.shader = sg; p.strokeWidth = r * 0.14f
            cv.drawArc(rc, 150f, 240f * frac, false, p)
            p.shader = null
        }
        p.style = Paint.Style.STROKE; p.strokeWidth = r * 0.012f
        for (i in 0..40) {
            val a = Math.toRadians(150.0 + 240.0 * i / 40.0)
            val r1 = if (i % 10 == 0) r * 0.82 else r * 0.86
            p.color = if (i % 10 == 0) withAlpha(T.accent, 200) else Color.argb(70, 255, 255, 255)
            cv.drawLine((cx + Math.cos(a) * r1).toFloat(), (cy + Math.sin(a) * r1).toFloat(), (cx + Math.cos(a) * r * 0.92).toFloat(), (cy + Math.sin(a) * r * 0.92).toFloat(), p)
        }
        p.style = Paint.Style.FILL; p.textAlign = Paint.Align.CENTER; p.textSize = r * 0.14f; p.typeface = Typeface.DEFAULT
        p.color = T.sub
        for (v in listOf(0, 50, 100, 150, 200)) {
            val a = Math.toRadians(150.0 + 240.0 * v / 200.0)
            val x = (cx + Math.cos(a) * r * 0.7).toFloat(); val y = (cy + Math.sin(a) * r * 0.7).toFloat() + p.textSize * 0.35f
            cv.drawText(v.toString(), x, y, p)
        }
        val ang = Math.toRadians(150.0 + 240.0 * frac)
        p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND; p.strokeWidth = r * 0.05f; p.color = lighten(T.accent, 0.3f)
        cv.drawLine((cx + Math.cos(ang) * r * 0.2).toFloat(), (cy + Math.sin(ang) * r * 0.2).toFloat(),
            (cx + Math.cos(ang) * r * 0.62).toFloat(), (cy + Math.sin(ang) * r * 0.62).toFloat(), p)
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(cx, cy, r * 0.1f, lighten(T.accent, 0.6f), darken(T.accent, 0.4f), Shader.TileMode.CLAMP)
        cv.drawCircle(cx, cy, r * 0.09f, p)
        p.shader = null
        p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
        p.color = Color.WHITE; p.textSize = r * 0.4f
        cv.drawText(valueText, cx, cy + r * 0.72f, p)
        p.textSize = r * 0.2f; p.color = T.ok
        cv.drawText(label, cx, cy + r * 1.0f, p)
    }
}

class GraphView(c: Context) : View(c) {
    private var data: List<Long> = emptyList()
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    fun setData(d: List<Long>) { data = d; invalidate() }

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        p.style = Paint.Style.STROKE; p.strokeWidth = 1f; p.color = Color.argb(40, 255, 255, 255)
        for (i in 1..3) cv.drawLine(0f, h * i / 4, w, h * i / 4, p)
        if (data.size < 2) return
        val maxV = maxOf(100L, data.maxOrNull() ?: 100L).toFloat()
        val step = w / 89f
        val x0 = w - (data.size - 1) * step
        val path = Path()
        var pen = false
        var lastX = x0
        data.forEachIndexed { i, v ->
            val x = x0 + i * step
            if (v < 0) {
                pen = false
                p.color = T.bad; p.strokeWidth = context.dpf(2f)
                cv.drawLine(x, h * 0.75f, x, h, p)
            } else {
                val y = h - (v / maxV).coerceIn(0f, 1f) * (h - context.dpf(6f)) - context.dpf(3f)
                if (!pen) { path.moveTo(x, y); pen = true } else path.lineTo(x, y)
                lastX = x
            }
        }
        val fillP = Path(path)
        fillP.lineTo(lastX, h); fillP.lineTo(x0, h); fillP.close()
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, 0f, 0f, h, withAlpha(T.accent, 90), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        cv.drawPath(fillP, p)
        p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND; p.strokeJoin = Paint.Join.ROUND
        p.shader = null
        p.color = withAlpha(T.accent, 60); p.strokeWidth = context.dpf(6f)
        cv.drawPath(path, p)
        p.shader = LinearGradient(0f, 0f, w, 0f, intArrayOf(darken(T.accent, 0.3f), lighten(T.accent, 0.5f), T.accent), null, Shader.TileMode.CLAMP)
        p.strokeWidth = context.dpf(2.4f)
        cv.drawPath(path, p)
        p.shader = null
    }
}
