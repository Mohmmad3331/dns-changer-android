package com.dnschanger.app

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class GameTestActivity : Activity() {

    private lateinit var repo: DnsRepo
    private lateinit var gameSp: Spinner
    private lateinit var dnsSp: Spinner
    private lateinit var startBtn: Button
    private lateinit var box: LinearLayout
    private lateinit var info: TextView
    @Volatile private var busy = false

    private class HostRes(val host: String, val v4: Long?, val v6: Long?, val v4ip: String?, val v6ip: String?, val blocked: Boolean)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        repo = DnsRepo(this)
        T.accent = Color.parseColor(if (repo.blue) "#4DA3FF" else "#FFC21A")
        window.statusBarColor = T.bg
        window.navigationBarColor = T.bg
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(18), dp(14), dp(10))
        }
        root.add(tv("Games Test  (IPv4 / IPv6)", 20f, T.text, true).apply { gravity = Gravity.CENTER })
        root.add(tv("بازی و DNS را انتخاب کن. برنامه برای هر دامنه، هم IPv4 و هم IPv6 را می‌سنجد و می‌گوید کدام بهتر است.", 12f, T.sub), t = 6, b = 6)

        gameSp = Spinner(this)
        dnsSp = Spinner(this)
        refreshSpinners()
        root.add(gameSp)
        root.add(dnsSp)
        startBtn = goldBtn("شروع تست", true) { run() }
        root.add(startBtn, t = 6)
        root.add(goldBtn("＋  افزودن دامنه / سرور دلخواه", false) { addHostDialog() }, t = 6)
        info = tv("", 13f, T.ok, true).apply { setPadding(0, dp(8), 0, dp(6)) }
        root.add(info)
        box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.add(ScrollView(this).apply { addView(box) }, -1, 0, weight = 1f)
        root.add(tv("توجه: آدرس سرور مچ هر بازی هنگام بازی تعیین می‌شود و ثابت نیست. این تست دامنه‌های ورود و سرویس بازی را می‌سنجد (TCP پورت ۴۴۳). اگر آدرس سرور مچ را می‌دانی، با «افزودن دامنه / سرور دلخواه» (به شکل host یا ip:port) تست کن. ✗ در IPv6 یعنی شبکهٔ تو IPv6 ندارد.", 11f, T.sub), t = 6)
        val fl = FrameLayout(this)
        fl.addView(GlowBg(this), FrameLayout.LayoutParams(-1, -1))
        fl.addView(root, FrameLayout.LayoutParams(-1, -1))
        setContentView(fl)
    }

    private fun refreshSpinners() {
        val names = mutableListOf("همه بازی‌ها (یک دامنه از هر بازی)")
        names.addAll(DnsData.games.map { it.name })
        names.add("دامنه‌های دلخواه من (${repo.hosts().size})")
        gameSp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        val d = mutableListOf("همهٔ DNSها", "DNS فعلی سیستم")
        d.addAll(repo.all().map { it.name })
        dnsSp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, d)
    }

    private fun addHostDialog() {
        val e = EditText(this).apply {
            hint = "مثلاً game.example.com یا 1.2.3.4:5000"; setSingleLine()
            inputType = InputType.TYPE_CLASS_TEXT; textDirection = View.TEXT_DIRECTION_LTR
        }
        val c = LinearLayout(this).apply { setPadding(dp(20), dp(8), dp(20), 0); addView(e, LinearLayout.LayoutParams(-1, -2)) }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("افزودن دامنه / سرور")
            .setView(c)
            .setPositiveButton("افزودن") { _, _ ->
                val t = e.text.toString().trim()
                if (t.isNotEmpty()) { repo.addHost(t); refreshSpinners(); gameSp.setSelection(gameSp.adapter.count - 1) }
            }
            .setNegativeButton("انصراف", null).show()
    }

    private fun hostsFor(pos: Int): List<String> = when {
        pos == 0 -> DnsData.games.map { it.hosts[0] }.distinct()
        pos == gameSp.adapter.count - 1 -> repo.hosts()
        else -> DnsData.games[pos - 1].hosts
    }

    private fun testHost(d: Dns?, spec: String): HostRes {
        var host = spec
        var port = 443
        if (spec.count { it == ':' } == 1) {
            host = spec.substringBefore(':'); port = spec.substringAfter(':').toIntOrNull() ?: 443
        }
        if (NetTest.isIpv4(host)) return HostRes(spec, NetTest.tcpPing(host, port), null, host, null, false)
        if (NetTest.isIpv6(host)) return HostRes(spec, null, NetTest.tcpPing(host, port), null, host, false)
        val a = if (d == null) NetTest.resolveSystem(host, false) else NetTest.resolve(d.p, host, 1)
        val b = if (d == null) NetTest.resolveSystem(host, true) else NetTest.resolve(d.p, host, 28)
        val ab = (a != null && NetTest.isBogus(a.first)) || (b != null && NetTest.isBogus(b.first))
        val v4 = if (a != null && !NetTest.isBogus(a.first)) NetTest.tcpPing(a.first, port) else null
        val v6 = if (b != null && !NetTest.isBogus(b.first)) NetTest.tcpPing(b.first, port) else null
        return HostRes(spec, v4, v6, a?.first, b?.first, ab)
    }

    private fun ms(v: Long?) = if (v == null) "✗" else "${v}ms"

    private class Summary(val text: String, val rank: Long, val best: Long, val verdict: String)

    private fun summarize(name: String, res: List<HostRes>): Summary {
        val a4 = res.mapNotNull { it.v4 }
        val a6 = res.mapNotNull { it.v6 }
        val avg4 = if (a4.isEmpty()) null else a4.sum() / a4.size
        val avg6 = if (a6.isEmpty()) null else a6.sum() / a6.size
        val ok = res.count { it.v4 != null || it.v6 != null }
        val v = when {
            avg4 == null && avg6 == null -> "✗ بدون اتصال"
            avg6 == null -> "فقط IPv4 کار می‌کند"
            avg4 == null -> "فقط IPv6 کار می‌کند"
            avg4 <= avg6 -> "IPv4 بهتر است"
            else -> "IPv6 بهتر است"
        }
        val best = listOfNotNull(avg4, avg6).minOrNull() ?: Long.MAX_VALUE
        val rank = if (ok == 0) Long.MAX_VALUE else (res.size - ok) * 100000L + best
        val sb = StringBuilder("$name\n$v  (${ok}/${res.size} دامنه)")
        res.forEach {
            sb.append("\n• ${it.host}\n   IPv4: ${ms(it.v4)}   |   IPv6: ${ms(it.v6)}")
            if (it.blocked) sb.append("   ⚠ مسدود/نامعتبر")
        }
        return Summary(sb.toString(), rank, best, v)
    }

    private fun row(): TextView {
        val t = tv("", 12f, T.text).apply { background = rect(T.card, T.stroke); setPadding(dp(12), dp(10), dp(12), dp(10)); textDirection = View.TEXT_DIRECTION_LTR }
        box.add(t, b = 6)
        return t
    }

    private fun run() {
        if (busy) return
        val hosts = hostsFor(gameSp.selectedItemPosition)
        if (hosts.isEmpty()) { Toast.makeText(this, "اول یک دامنه اضافه کن", Toast.LENGTH_SHORT).show(); return }
        busy = true
        startBtn.isEnabled = false
        box.removeAllViews()
        val all = repo.all()
        val dp = dnsSp.selectedItemPosition
        val targets: List<Pair<String, Dns?>> = when (dp) {
            0 -> listOf(Pair("DNS فعلی سیستم", null as Dns?)) + all.map { Pair(it.name, it as Dns?) }
            1 -> listOf(Pair("DNS فعلی سیستم", null as Dns?))
            else -> listOf(Pair(all[dp - 2].name, all[dp - 2] as Dns?))
        }
        info.text = "در حال تست ${hosts.size} دامنه روی ${targets.size} DNS ..."
        val rows = targets.map { row().apply { text = "${it.first}\nمنتظر..." } }
        Thread {
            val ex = Executors.newFixedThreadPool(4)
            val tasks = targets.mapIndexed { i, t ->
                Callable {
                    runOnUiThread { rows[i].text = "${t.first}\nدر حال تست..." }
                    val s = summarize(t.first, hosts.map { testHost(t.second, it) })
                    runOnUiThread { rows[i].text = s.text }
                    s
                }
            }
            val sums = ex.invokeAll(tasks).map { it.get() }
            ex.shutdown()
            var bi = -1
            sums.forEachIndexed { i, s -> if (s.rank != Long.MAX_VALUE && (bi < 0 || s.rank < sums[bi].rank)) bi = i }
            runOnUiThread {
                if (bi >= 0) {
                    rows[bi].background = rect(Color.parseColor("#14301F"), T.ok, 14, 2)
                    rows[bi].text = "⭐ " + rows[bi].text
                    info.text = "بهترین: ${targets[bi].first} — ${sums[bi].verdict} (میانگین ${sums[bi].best}ms)"
                } else info.text = "هیچ اتصالی برقرار نشد؛ اینترنت را بررسی کن."
                startBtn.isEnabled = true
                busy = false
            }
        }.start()
    }
}
