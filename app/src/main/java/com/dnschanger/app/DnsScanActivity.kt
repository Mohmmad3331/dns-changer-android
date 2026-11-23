package com.dnschanger.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class DnsScanActivity : Activity() {

    private class Cand(val name: String, val ip: String, val tag: String)
    private class Res(val c: Cand, val dnsMs: Long, val ping: Long, val ok: Int, val total: Int)

    private lateinit var repo: DnsRepo
    @Volatile private var hosts: List<String> = listOf("google.com")
    private var hostsEt: EditText? = null
    private lateinit var gameName: String
    private lateinit var status: TextView
    private lateinit var bar: ProgressBar
    private lateinit var list: LinearLayout
    private lateinit var btn: LinearLayout
    private val results = java.util.Collections.synchronizedList(ArrayList<Res>())
    @Volatile private var scanning = false
    @Volatile private var cancel = false

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        repo = DnsRepo(this)
        T.accent = Color.parseColor(if (repo.blue) "#4DA3FF" else "#FFC21A")
        window.statusBarColor = T.bg
        window.navigationBarColor = T.bg
        gameName = intent.getStringExtra("name") ?: "اینترنت عمومی"
        hosts = (intent.getStringArrayExtra("hosts") ?: arrayOf("google.com")).toList()

        val sc = ScrollView(this)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(20)) }
        sc.addView(col)
        val hd = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        hd.add(IconView(this, "back", T.text, 26).apply { setOnClickListener { finish() } }, 44, 44)
        hd.add(tv("Find Best DNS", 20f, T.text, true).apply { gravity = Gravity.CENTER }, 0, -2, weight = 1f)
        hd.add(View(this), 44, 44)
        col.add(hd)
        col.add(tv(gameName, 18f, T.accent, true).apply { gravity = Gravity.CENTER }, t = 8)
        if (intent.getBooleanExtra("editable", false)) {
            col.add(tv("دامنه‌های سرور برنامه (با کاما جدا کن؛ اگر اشتباه بود اصلاحش کن)", 12f, T.sub), t = 10, b = 4)
            hostsEt = EditText(this).apply {
                setText(hosts.joinToString(", "))
                setTextColor(T.text); textSize = 13f
                textDirection = View.TEXT_DIRECTION_LTR
                background = rect(withAlpha(T.card, 235), T.stroke, 14)
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }
            col.add(hostsEt!!)
        } else col.add(tv("دامنه‌ها: ${hosts.joinToString("، ")}", 11f, T.sub).apply { gravity = Gravity.CENTER })

        bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            progressTintList = android.content.res.ColorStateList.valueOf(T.accent)
        }
        col.add(bar, -1, 8, t = 14)
        status = tv("آماده؛ شروع را بزن.", 13f, T.sub, true)
        col.add(status, t = 6)
        btn = iconBtn("Start Scan", "play", true) { if (scanning) cancel = true else start() }
        col.add(btn, t = 10)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.add(list, t = 12)
        col.add(tv("هر DNS دامنه‌های بازی را حل می‌کند و برنامه به آدرس برگشتی وصل می‌شود؛ هرچه پینگ کمتر، آن DNS تو را به سرور نزدیک‌تر می‌فرستد. DNSهایی که جواب نمی‌دهند یا آدرس مسدود/نامعتبر می‌دهند کنار گذاشته می‌شوند. بسیاری از DNSهای کشورها فقط برای کاربران همان کشور جواب می‌دهند.", 11f, T.sub), t = 12)

        val fl = FrameLayout(this)
        fl.addView(GlowBg(this), FrameLayout.LayoutParams(-1, -1))
        fl.addView(sc, FrameLayout.LayoutParams(-1, -1))
        setContentView(fl)
    }

    private fun candidates(): List<Cand> {
        val out = ArrayList<Cand>()
        val seen = HashSet<String>()
        repo.all().forEach { d -> if (seen.add(d.p)) out.add(Cand(d.title, d.p, "لیست من")) }
        try {
            assets.open("world_dns.csv").bufferedReader().useLines { ls ->
                ls.forEach { l ->
                    val p = l.split("|")
                    if (p.size >= 4 && seen.add(p[3])) out.add(Cand(p[2].substringBefore(" - ").trim() + " (" + p[0] + ")", p[3], p[1]))
                }
            }
        } catch (_: Exception) {}
        return out
    }

    private fun testOne(c: Cand): Res? {
        val first = NetTest.resolve(c.ip, hosts[0], 1, 1500) ?: return null
        var sum = 0L
        var ok = 0
        for ((i, hst) in hosts.withIndex()) {
            if (cancel) break
            val r = if (i == 0) first else NetTest.resolve(c.ip, hst, 1, 1500)
            if (r != null && !NetTest.isBogus(r.first)) {
                val p = NetTest.tcpPing(r.first, 443, 2, 1500)
                if (p != null) { sum += p; ok++ }
            }
        }
        return if (ok == 0) null else Res(c, first.second, sum / ok, ok, hosts.size)
    }

    private fun setBtn(text: String, icon: String) {
        btn.removeAllViews()
        btn.addView(IconView(this, icon, Color.BLACK, 22))
        btn.addView(tv(text, 16f, Color.BLACK, true).apply { setPadding(dp(8), 0, 0, 0) })
    }

    private fun start() {
        scanning = true; cancel = false
        results.clear(); list.removeAllViews()
        setBtn("Stop", "alert")
        hostsEt?.let { et ->
            val h = et.text.toString().split(",", " ", "\n", "،").map { it.trim().lowercase().removePrefix("https://").removePrefix("http://").substringBefore('/') }.filter { it.isNotEmpty() }
            if (h.isNotEmpty()) hosts = h
        }
        val cands = candidates()
        val total = cands.size
        bar.max = total; bar.progress = 0
        status.text = "بررسی دامنه‌ها..."
        status.setTextColor(T.sub)
        val done = AtomicInteger(0)
        Thread {
            val probe = listOf("8.8.8.8", "1.1.1.1", "78.157.42.100", "178.22.122.100")
            val valid = hosts.filter { h ->
                probe.any { d -> val r = NetTest.resolve(d, h, 1, 2000); r != null && !NetTest.isBogus(r.first) }
            }
            if (valid.isEmpty()) {
                runOnUiThread {
                    scanning = false
                    status.text = "هیچ‌کدام از دامنه‌ها پیدا نشد؛ دامنهٔ سرور برنامه را در کادر بالا درست کن."
                    status.setTextColor(T.bad)
                    setBtn("Scan Again", "play")
                }
                return@Thread
            }
            hosts = valid
            runOnUiThread { status.text = "در حال تست 0 / $total ..." }
            val ex = Executors.newFixedThreadPool(32)
            cands.forEach { c ->
                ex.execute {
                    try { if (!cancel) testOne(c)?.let { results.add(it) } } catch (_: Exception) {}
                    val n = done.incrementAndGet()
                    if (n % 8 == 0 || n == total) runOnUiThread { progress(n, total) }
                }
            }
            ex.shutdown()
            ex.awaitTermination(20, TimeUnit.MINUTES)
            runOnUiThread {
                scanning = false
                progress(total, total)
                status.text = (if (cancel) "متوقف شد" else "تمام شد") + " — ${results.size} DNS از $total جواب مفید داد"
                status.setTextColor(T.ok)
                setBtn("Scan Again", "play")
            }
        }.start()
    }

    private fun progress(n: Int, total: Int) {
        bar.progress = n
        status.text = "در حال تست $n / $total — ${results.size} جواب مفید"
        status.setTextColor(T.sub)
        render()
    }

    private fun render() {
        val top = synchronized(results) { ArrayList(results) }
            .sortedWith(compareBy<Res>({ -it.ok }, { it.ping })).take(15)
        list.removeAllViews()
        top.forEachIndexed { i, r ->
            val row = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                background = rect(withAlpha(T.card, 235), if (i == 0) T.accent else T.stroke, 16, if (i == 0) 2 else 1)
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }
            row.add(tv("${i + 1}", 18f, T.accent, true).apply { gravity = Gravity.CENTER }, 30)
            val tx = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            tx.addView(tv(r.c.name, 14f, T.text, true))
            tx.addView(tv("${r.c.ip}  •  ${r.c.tag}", 11f, T.sub))
            tx.addView(tv("پینگ بازی: ${r.ping}ms  •  پاسخ DNS: ${r.dnsMs}ms  •  ${r.ok}/${r.total} دامنه", 11f, T.ok))
            row.add(tx, 0, -2, weight = 1f)
            row.add(tv("Use", 14f, Color.BLACK, true).apply {
                gravity = Gravity.CENTER; background = rect(T.accent, 0, 10); setPadding(dp(14), dp(8), dp(14), dp(8))
                setOnClickListener { use(r) }
            }, -2, -2)
            list.add(row, b = 8)
        }
    }

    private fun use(r: Res) {
        val existing = repo.all().firstOrNull { it.p == r.c.ip }
        val name = existing?.name ?: (r.c.name + " · " + r.c.ip).also { repo.addCustom(it, r.c.ip, "") }
        repo.selected = name
        if (DnsVpnService.active) {
            startService(Intent(this, DnsVpnService::class.java)
                .setAction(DnsVpnService.ACTION_START)
                .putExtra(DnsVpnService.EXTRA_NAME, name)
                .putExtra(DnsVpnService.EXTRA_DNS1, r.c.ip)
                .putExtra(DnsVpnService.EXTRA_DNS2, ""))
            Toast.makeText(this, "DNS عوض شد: $name", Toast.LENGTH_SHORT).show()
        } else Toast.makeText(this, "انتخاب شد؛ در صفحهٔ Home وصل شو", Toast.LENGTH_LONG).show()
    }
}
