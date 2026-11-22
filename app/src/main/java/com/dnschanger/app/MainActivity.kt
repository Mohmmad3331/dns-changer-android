package com.dnschanger.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.VpnService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import java.net.InetSocketAddress
import java.net.Socket

data class Dns(val name: String, val p: String, val s: String, val type: String, val desc: String)

class MainActivity : Activity() {

    private val list = listOf(
        Dns("رادار بازی (Radar Game)", "10.202.10.10", "10.202.10.11", "داخلی", "بهبود اتصال، کاهش برخی مشکلات دسترسی و تست پینگ"),
        Dns("الکترو (Electro)", "78.157.42.100", "78.157.42.101", "داخلی", "همه‌کاره داخلی؛ مناسب تست فعال‌سازی گیفت کارت پلی‌استیشن"),
        Dns("شکن (Shecan)", "178.22.122.100", "185.51.200.2", "داخلی", "دارای راهنما برای دستگاه‌ها و سیستم‌عامل‌های مختلف"),
        Dns("همراه اول", "208.67.220.200", "208.67.222.220", "داخلی", "مخصوص اپراتور همراه اول"),
        Dns("ایرانسل", "74.82.42.42", "0.0.0.0", "داخلی", "مخصوص اپراتور ایرانسل"),
        Dns("زئوس (Zeus)", "37.32.5.60", "37.32.5.61", "داخلی", "قابل‌تست برای بازی‌های آنلاین، به‌ویژه روی موبایل"),
        Dns("وانیلا (Vanilla)", "194.146.68.68", "194.146.68.66", "داخلی", "تست روی برخی اپراتورها و کنسول PS4/PS5"),
        Dns("بگذر (Begzar)", "185.55.226.26", "185.55.225.25", "داخلی", "تست رفع برخی محدودیت‌های دسترسی"),
        Dns("کلودفلر (Cloudflare)", "1.1.1.1", "1.0.0.1", "بین‌المللی", "تست لاگین و دانلود بازی‌ها، تمرکز بر حریم خصوصی"),
        Dns("گوگل (Google Public DNS)", "8.8.8.8", "8.8.4.4", "بین‌المللی", "تست اتصال با اپراتورهای مختلف در شرایط عادی"),
        Dns("کواد۹ (Quad9)", "9.9.9.9", "149.112.112.112", "بین‌المللی", "DNS بین‌المللی با تمرکز بر امنیت")
    )

    private var selected = -1
    private val cards = mutableListOf<LinearLayout>()
    private val pings = mutableListOf<TextView>()
    private lateinit var btn: Button
    private lateinit var status: TextView
    private val handler = Handler(Looper.getMainLooper())

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun bg(sel: Boolean) = GradientDrawable().apply {
        cornerRadius = dp(14).toFloat()
        setColor(if (sel) Color.parseColor("#2A2F45") else Color.parseColor("#1A1D2B"))
        setStroke(dp(2), if (sel) Color.parseColor("#7C9CFF") else Color.parseColor("#2A2F45"))
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        window.statusBarColor = Color.parseColor("#0F111A")
        window.navigationBarColor = Color.parseColor("#0F111A")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F111A"))
        }
        status = TextView(this).apply {
            textSize = 20f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; setPadding(dp(16), dp(24), dp(16), dp(8))
        }
        root.addView(status)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(8), dp(12), dp(12))
        }
        list.forEachIndexed { i, d ->
            val c = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = bg(false)
                setOnClickListener { select(i) }
            }
            c.addView(TextView(this).apply {
                text = "${d.name}  •  ${d.type}"; textSize = 16f
                setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD
            })
            val ips = if (d.s == "0.0.0.0") d.p else "${d.p}  |  ${d.s}"
            c.addView(TextView(this).apply {
                text = ips; textSize = 13f; setTextColor(Color.parseColor("#9FB4FF"))
                layoutDirection = View.LAYOUT_DIRECTION_LTR; gravity = Gravity.START
            })
            c.addView(TextView(this).apply {
                text = d.desc; textSize = 12f; setTextColor(Color.parseColor("#9AA0B5"))
            })
            val pv = TextView(this).apply {
                text = "پینگ: ؟ (برای تست، لمس طولانی)"; textSize = 12f
                setTextColor(Color.parseColor("#7ED9A0"))
            }
            c.addView(pv)
            c.setOnLongClickListener { ping(i); true }
            val lp = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }
            col.addView(c, lp)
            cards.add(c); pings.add(pv)
        }
        root.addView(ScrollView(this).apply { addView(col) }, LinearLayout.LayoutParams(-1, 0, 1f))

        val pingAll = Button(this).apply {
            text = "تست پینگ همه"; setOnClickListener { list.indices.forEach { ping(it) } }
        }
        btn = Button(this).apply {
            textSize = 18f; setOnClickListener { toggle() }
        }
        root.addView(pingAll, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(12), 0, dp(12), 0) })
        root.addView(btn, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(12), 0, dp(12), dp(12)) })
        setContentView(root)
        refresh()
    }

    private fun select(i: Int) {
        selected = i
        cards.forEachIndexed { k, c -> c.background = bg(k == i) }
        refresh()
    }

    private fun ping(i: Int) {
        pings[i].text = "پینگ: ..."
        Thread {
            val t = System.nanoTime()
            val r = try {
                Socket().use { it.connect(InetSocketAddress(list[i].p, 53), 2000) }
                "${(System.nanoTime() - t) / 1_000_000} ms"
            } catch (_: Exception) { "ناموفق" }
            runOnUiThread { pings[i].text = "پینگ: $r" }
        }.start()
    }

    private fun toggle() {
        if (DnsVpnService.active) {
            startService(Intent(this, DnsVpnService::class.java).setAction(DnsVpnService.ACTION_STOP))
            handler.postDelayed({ refresh() }, 500)
        } else {
            if (selected < 0) { Toast.makeText(this, "اول یک DNS انتخاب کن", Toast.LENGTH_SHORT).show(); return }
            val i = VpnService.prepare(this)
            if (i != null) startActivityForResult(i, 1) else startVpn()
        }
    }

    private fun startVpn() {
        val d = list[selected]
        startService(Intent(this, DnsVpnService::class.java)
            .setAction(DnsVpnService.ACTION_START)
            .putExtra(DnsVpnService.EXTRA_DNS1, d.p)
            .putExtra(DnsVpnService.EXTRA_DNS2, d.s))
        handler.postDelayed({ refresh() }, 700)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        if (req == 1 && res == RESULT_OK) startVpn()
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        val on = DnsVpnService.active
        status.text = if (on) "● متصل" else "○ قطع"
        status.setTextColor(Color.parseColor(if (on) "#7ED9A0" else "#9AA0B5"))
        btn.text = if (on) "قطع DNS" else "اتصال"
    }
}
