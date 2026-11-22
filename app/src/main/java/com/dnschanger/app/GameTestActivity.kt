package com.dnschanger.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*

class GameTestActivity : Activity() {

    private lateinit var spinner: Spinner
    private lateinit var startBtn: Button
    private lateinit var box: LinearLayout
    private lateinit var info: TextView
    @Volatile private var busy = false

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        window.statusBarColor = Color.parseColor("#0F111A")
        window.navigationBarColor = Color.parseColor("#0F111A")
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F111A"))
            setPadding(dp(12), dp(20), dp(12), dp(12))
        }
        root.addView(TextView(this).apply {
            text = "تست پینگ بازی و اینترنت"; textSize = 20f
            setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "اول بازی را انتخاب کن؛ برنامه با هر DNS آدرس سرور بازی را پیدا می‌کند و زمان اتصال را می‌سنجد. عدد کمتر بهتر است."
            textSize = 12f; setTextColor(Color.parseColor("#9AA0B5")); setPadding(0, dp(6), 0, dp(6))
        })
        spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@GameTestActivity, android.R.layout.simple_spinner_dropdown_item,
                DnsData.games.map { it.name })
        }
        root.addView(spinner)
        startBtn = Button(this).apply { text = "شروع تست"; setOnClickListener { run() } }
        root.addView(startBtn)
        info = TextView(this).apply { textSize = 13f; setTextColor(Color.parseColor("#7ED9A0")); setPadding(0, dp(6), 0, dp(6)) }
        root.addView(info)
        box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(box) }, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(TextView(this).apply {
            text = "توجه: پینگ تا سرور ورود/سایت رسمی بازی سنجیده می‌شود (نه سرور مچ). برای مقایسه DNSها مفید است ولی عدد دقیق پینگ داخل بازی نیست."
            textSize = 11f; setTextColor(Color.parseColor("#9AA0B5")); setPadding(0, dp(6), 0, 0)
        })
        setContentView(root)
    }

    private fun row(): TextView {
        val tv = TextView(this).apply {
            textSize = 13f; setTextColor(Color.WHITE)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.parseColor("#1A1D2B"))
        }
        val lp = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) }
        box.addView(tv, lp)
        return tv
    }

    private fun run() {
        if (busy) return
        busy = true
        startBtn.isEnabled = false
        box.removeAllViews()
        val game = DnsData.games[spinner.selectedItemPosition]
        info.text = "در حال تست ${game.name} ..."
        val targets = listOf<Dns?>(null) + DnsData.list
        val rows = targets.map { row().apply { text = "${it?.name ?: "DNS فعلی سیستم"}\nمنتظر..." } }
        Thread {
            val scores = HashMap<Int, Long>()
            targets.forEachIndexed { i, d ->
                val name = d?.name ?: "DNS فعلی سیستم"
                runOnUiThread { rows[i].text = "$name\nدر حال تست..." }
                val r = if (d == null) NetTest.resolveSystem(game.host) else NetTest.resolve(d.p, game.host)
                val txt: String
                if (r == null) {
                    txt = "$name\n✗ حل نام ناموفق"
                } else if (NetTest.isBogus(r.first)) {
                    txt = "$name\n⚠ IP نامعتبر/مسدود (${r.first})"
                } else {
                    val ping = NetTest.tcpPing(r.first)
                    if (ping == null) {
                        txt = "$name\nحل نام: ${r.second}ms  |  اتصال ناموفق (${r.first})"
                    } else {
                        scores[i] = ping
                        txt = "$name\nحل نام: ${r.second}ms  |  پینگ: ${ping}ms  |  ${r.first}"
                    }
                }
                runOnUiThread { rows[i].text = txt }
            }
            val best = scores.minByOrNull { it.value }
            runOnUiThread {
                if (best != null) {
                    val bn = targets[best.key]?.name ?: "DNS فعلی سیستم"
                    rows[best.key].setBackgroundColor(Color.parseColor("#1F4D36"))
                    rows[best.key].text = "⭐ " + rows[best.key].text
                    info.text = "بهترین برای ${game.name}: $bn (${best.value}ms)"
                } else info.text = "هیچ DNS ای جواب نداد؛ اینترنت را چک کن."
                startBtn.isEnabled = true
                busy = false
            }
        }.start()
    }
}
