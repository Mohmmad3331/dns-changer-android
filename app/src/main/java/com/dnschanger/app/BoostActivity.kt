package com.dnschanger.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*

class BoostActivity : Activity() {

    private val h = Handler(Looper.getMainLooper())
    private val targets = listOf("Cloudflare (1.1.1.1)" to "1.1.1.1", "Google (8.8.8.8)" to "8.8.8.8") +
        DnsData.games.map { it.name to it.hosts[0] }
    private lateinit var power: PowerView
    private lateinit var status: TextView
    private lateinit var spinner: Spinner
    private lateinit var tAvg: TextView
    private lateinit var tJit: TextView
    private lateinit var tLoss: TextView
    private lateinit var graph: GraphView
    private lateinit var verdictTv: TextView
    private val tick = object : Runnable {
        override fun run() { refresh(); h.postDelayed(this, 1000) }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val repo = DnsRepo(this)
        T.accent = Color.parseColor(if (repo.blue) "#4DA3FF" else "#FFC21A")
        window.statusBarColor = T.bg
        window.navigationBarColor = T.bg
        val sc = ScrollView(this)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(20)) }
        sc.addView(col)

        val hd = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        hd.add(IconView(this, "back", T.text, 26).apply { setOnClickListener { finish() } }, 44, 44)
        hd.add(tv("Game Boost", 20f, T.text, true).apply { gravity = Gravity.CENTER }, 0, -2, weight = 1f)
        hd.add(View(this), 44, 44)
        col.add(hd)

        power = PowerView(this).apply { setOnClickListener { toggle() } }
        col.add(power, -1, 230)
        status = tv("", 20f, T.text, true).apply { gravity = Gravity.CENTER }
        col.add(status)

        col.add(tv("سرور هدف برای پایش", 13f, T.sub), t = 14, b = 4)
        spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@BoostActivity, android.R.layout.simple_spinner_dropdown_item, targets.map { it.first })
        }
        col.add(spinner)

        val row = LinearLayout(this)
        fun tile(label: String, icon: String): TextView {
            val v = tv("—", 20f, T.text, true)
            val c = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; background = rect(withAlpha(T.card, 235), T.stroke, 16); setPadding(dp(12), dp(10), dp(12), dp(10))
            }
            val hh = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            hh.addView(IconView(this@BoostActivity, icon, T.accent, 16))
            hh.addView(tv(label, 12f, T.sub).apply { setPadding(dp(4), 0, 0, 0) })
            c.addView(hh); c.addView(v)
            row.add(c, 0, -2, 3, 0, 3, 0, 1f)
            return v
        }
        tAvg = tile("Ping", "bolt")
        tJit = tile("Jitter", "speed")
        tLoss = tile("Loss", "alert")
        col.add(row, t = 12)

        val gc = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; background = rect(withAlpha(T.card, 235), T.stroke, 16); setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        gc.addView(tv("پایداری اتصال (۳ دقیقهٔ اخیر)", 12f, T.sub))
        graph = GraphView(this)
        gc.add(graph, -1, 120, t = 6)
        verdictTv = tv("", 15f, T.text, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(8), 0, 0) }
        gc.addView(verdictTv)
        col.add(gc, t = 10)

        val info = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL; background = rect(withAlpha(T.card, 235), T.stroke, 14); setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        info.add(IconView(this, "info", T.accent, 26), r = 12)
        info.add(tv("Boost چه می‌کند: ۱) هر ۲ ثانیه یک اتصال کوچک می‌زند تا مودم گوشی خواب نرود و پینگ کمتر بپرد. ۲) روی Wi-Fi حالت کم‌تأخیر را روشن می‌کند (اندروید ۱۰ به بالا). ۳) پایداری را زنده نشان می‌دهد.\n\nنمی‌تواند سرعت اینترنت را بیشتر کند یا فاصلهٔ تو تا سرور بازی را کم کند. مصرف باتری کمی بیشتر می‌شود؛ بعد از بازی خاموشش کن.", 12f, T.sub), 0, -2, weight = 1f)
        col.add(info, t = 10)

        val fl = FrameLayout(this)
        fl.addView(GlowBg(this), FrameLayout.LayoutParams(-1, -1))
        fl.addView(sc, FrameLayout.LayoutParams(-1, -1))
        setContentView(fl)
        refresh()
    }

    override fun onResume() { super.onResume(); h.post(tick) }
    override fun onPause() { super.onPause(); h.removeCallbacks(tick) }

    private fun toggle() {
        if (BoostService.running) {
            startService(Intent(this, BoostService::class.java).setAction(BoostService.ACTION_STOP))
            BoostService.running = false
        } else {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED)
                requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 5)
            val t = targets[spinner.selectedItemPosition]
            val i = Intent(this, BoostService::class.java).setAction(BoostService.ACTION_START)
                .putExtra(BoostService.EXTRA_HOST, t.second).putExtra(BoostService.EXTRA_PORT, 443)
            startForegroundService(i)
            BoostService.running = true
        }
        refresh()
    }

    private fun refresh() {
        val on = BoostService.running
        power.on = on; power.invalidate()
        status.text = if (on) "Boost ON" else "Boost OFF"
        status.setTextColor(if (on) T.accent else T.sub)
        val s: List<Long> = synchronized(BoostService.samples) { ArrayList(BoostService.samples) }
        graph.setData(s)
        val ok = s.filter { it >= 0 }
        if (s.isEmpty() || ok.isEmpty()) {
            tAvg.text = "—"; tJit.text = "—"; tLoss.text = if (s.isEmpty()) "—" else "100%"
            verdictTv.text = if (on) "در حال جمع‌آوری نمونه..." else "برای شروع پایش، دکمه را لمس کن"
            verdictTv.setTextColor(T.sub)
            return
        }
        val avg = ok.sum() / ok.size
        var jit = 0L
        if (ok.size > 1) jit = (1 until ok.size).sumOf { Math.abs(ok[it] - ok[it - 1]) } / (ok.size - 1)
        val loss = (s.size - ok.size) * 100 / s.size
        tAvg.text = "$avg ms"; tJit.text = "$jit ms"; tLoss.text = "$loss%"
        when {
            s.size < 5 -> { verdictTv.text = "در حال جمع‌آوری نمونه..."; verdictTv.setTextColor(T.sub) }
            loss > 5 || jit > 40 -> { verdictTv.text = "اتصال ناپایدار ✗"; verdictTv.setTextColor(T.bad) }
            loss > 0 || jit > 15 -> { verdictTv.text = "اتصال متوسط"; verdictTv.setTextColor(T.accent) }
            else -> { verdictTv.text = "اتصال پایدار ✓"; verdictTv.setTextColor(T.ok) }
        }
    }
}
