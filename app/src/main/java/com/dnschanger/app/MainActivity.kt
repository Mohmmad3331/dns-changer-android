package com.dnschanger.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class MainActivity : Activity() {

    private lateinit var repo: DnsRepo
    private lateinit var content: FrameLayout
    private lateinit var nav: NavView
    private var tab = 0
    private val h = Handler(Looper.getMainLooper())

    private val speed = HashMap<String, Long>()
    private var speedBusy = false
    private var gauge: GaugeView? = null
    private var speedBox: LinearLayout? = null

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        repo = DnsRepo(this)
        T.accent = Color.parseColor(if (repo.blue) "#4DA3FF" else "#FFC21A")
        window.statusBarColor = T.bg
        window.navigationBarColor = T.nav
        val root = FrameLayout(this)
        root.addView(GlowBg(this), FrameLayout.LayoutParams(-1, -1))
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content = FrameLayout(this)
        col.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        nav = NavView(this) { show(it) }
        col.addView(nav, LinearLayout.LayoutParams(-1, -2))
        root.addView(col, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        show(b?.getInt("tab") ?: 0)
    }

    override fun onSaveInstanceState(o: Bundle) { super.onSaveInstanceState(o); o.putInt("tab", tab) }
    override fun onResume() { super.onResume(); if (tab == 0 && ::content.isInitialized) show(0) }

    private fun current(): Dns = repo.all().firstOrNull { it.name == repo.selected } ?: DnsData.builtin[0]
    private fun toast(m: String) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show() }
    private fun alert(m: String) { if (repo.alerts) toast(m) }

    private fun show(t: Int) {
        tab = t
        nav.sel = t
        nav.invalidate()
        content.removeAllViews()
        content.addView(when (t) { 0 -> homeView(); 1 -> speedView(); 2 -> providersView(); else -> settingsView() })
    }

    private fun page(): Pair<ScrollView, LinearLayout> {
        val sc = ScrollView(this)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(16)) }
        sc.addView(col)
        return Pair(sc, col)
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; background = rect(withAlpha(T.card, 235), T.stroke, 16); setPadding(dp(14), dp(12), dp(14), dp(12))
    }

    private fun header(title: String, leftIcon: String?, onLeft: (() -> Unit)?): View {
        val r = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val left: View = if (leftIcon != null) IconView(this, leftIcon, T.text, 26).apply { setOnClickListener { onLeft?.invoke() } } else View(this)
        r.add(left, 44, 44)
        r.add(tv(title, 20f, T.text, true).apply { gravity = Gravity.CENTER }, 0, -2, weight = 1f)
        r.add(View(this), 44, 44)
        return r
    }

    // ------------------------------------------------------------ Home
    private fun homeView(): View {
        val (sc, col) = page()
        val on = DnsVpnService.active
        val d = current()
        col.add(header("DNS Changer", "menu") { show(3) })
        val power = PowerView(this).apply { this.on = on; setOnClickListener { toggle() } }
        col.add(power, -1, 250, t = 4)
        val st = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        if (on) st.addView(IconView(this, "check", Color.BLACK, 22).apply {
            background = android.graphics.drawable.GradientDrawable().apply { shape = android.graphics.drawable.GradientDrawable.OVAL; setColor(T.ok) }
        })
        st.addView(tv(if (on) "Connected" else "Disconnected", 22f, if (on) T.ok else T.sub, true).apply { setPadding(dp(8), 0, 0, 0) })
        col.add(st)
        col.add(tv(if (on) "Using ${DnsVpnService.currentName.ifBlank { d.name }}" else "برای اتصال دکمه را لمس کن", 13f, T.sub).apply { gravity = Gravity.CENTER })

        val c = card().apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setOnClickListener { show(2) } }
        c.add(LogoView(this, brandLetter(d.name), brandColor(d.name), 44), r = 12)
        val tx = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        tx.addView(tv(d.name, 16f, T.text, true))
        tx.addView(tv(d.ips, 13f, T.sub).apply { textDirection = View.TEXT_DIRECTION_LTR; gravity = Gravity.START })
        c.add(tx, 0, -2, weight = 1f)
        c.add(IconView(this, "chev", T.text, 22), -2, -2)
        col.add(c, t = 12)

        val row = LinearLayout(this)
        val pingTv = tv("…", 22f, T.text, true)
        val t1 = card().apply {
            val hd = LinearLayout(this@MainActivity).apply { gravity = Gravity.CENTER_VERTICAL }
            hd.addView(IconView(this@MainActivity, "bolt", T.accent, 18)); hd.addView(tv("Ping", 13f, T.sub).apply { setPadding(dp(6), 0, 0, 0) })
            addView(hd); addView(pingTv)
        }
        val t2 = card().apply {
            val hd = LinearLayout(this@MainActivity).apply { gravity = Gravity.CENTER_VERTICAL }
            hd.addView(IconView(this@MainActivity, "wifi", T.accent, 18)); hd.addView(tv("Network", 13f, T.sub).apply { setPadding(dp(6), 0, 0, 0) })
            addView(hd); addView(tv(netName(), 22f, T.text, true))
        }
        row.add(t1, 0, -2, 0, 0, 6, 0, 1f)
        row.add(t2, 0, -2, 6, 0, 0, 0, 1f)
        col.add(row, t = 10)

        val hb = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            background = rect(if (on) Color.parseColor("#0F2A1B") else withAlpha(T.card, 235), if (on) T.ok else T.stroke, 16, 2)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        hb.add(IconView(this, "shieldcheck", if (on) T.ok else T.sub, 34), r = 12)
        val ht = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val h1 = tv(if (on) "DNS is working properly" else "DNS is not active", 15f, if (on) T.text else T.sub, true)
        val h2 = tv(if (on) "در حال بررسی..." else "یک DNS انتخاب کن و وصل شو", 12f, T.sub)
        ht.addView(h1); ht.addView(h2)
        hb.add(ht, 0, -2, weight = 1f)
        col.add(hb, t = 10)
        col.add(iconBtn("Game Boost", "bolt", true) { startActivity(Intent(this, BoostActivity::class.java)) }, t = 10)

        Thread {
            val ms = NetTest.tcpPing(d.p, 53)
            runOnUiThread { pingTv.text = if (ms == null) "—" else "$ms ms" }
        }.start()
        if (on) Thread {
            Thread.sleep(1200)
            val r = NetTest.resolveSystem("google.com", false)
            runOnUiThread {
                if (r != null && !NetTest.isBogus(r.first)) h2.text = "پاسخ DNS تأیید شد (${r.second} ms)"
                else { h1.text = "DNS is not responding"; h1.setTextColor(T.bad); h2.text = "DNS جواب درست نمی‌دهد؛ یکی دیگر را امتحان کن" }
            }
        }.start()
        return sc
    }

    @Suppress("DEPRECATION")
    private fun netName(): String {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var r = "Offline"
        for (n in cm.allNetworks) {
            val c = cm.getNetworkCapabilities(n) ?: continue
            if (c.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) continue
            if (c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "Wi-Fi"
            if (c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) r = "Mobile" else if (r == "Offline") r = "Other"
        }
        return r
    }

    private fun toggle() {
        if (DnsVpnService.active) {
            startService(Intent(this, DnsVpnService::class.java).setAction(DnsVpnService.ACTION_STOP))
            h.postDelayed({ alert("DNS قطع شد"); if (tab == 0) show(0) }, 500)
        } else {
            val i = VpnService.prepare(this)
            if (i != null) startActivityForResult(i, 1) else startVpn()
        }
    }

    private fun startVpn() {
        val d = current()
        startService(Intent(this, DnsVpnService::class.java)
            .setAction(DnsVpnService.ACTION_START)
            .putExtra(DnsVpnService.EXTRA_NAME, d.name)
            .putExtra(DnsVpnService.EXTRA_DNS1, d.p)
            .putExtra(DnsVpnService.EXTRA_DNS2, d.s))
        h.postDelayed({ alert("متصل شد: ${d.name}"); if (tab == 0) show(0) }, 800)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        if (req == 1 && res == RESULT_OK) startVpn()
    }

    // ------------------------------------------------------------ Speed
    private fun speedView(): View {
        val (sc, col) = page()
        col.add(header("DNS Speed Test", null, null))
        val g = GaugeView(this)
        gauge = g
        col.add(g, -1, 260, t = 4)
        col.add(iconBtn("Test Again", "play", false) { runSpeed() }, t = 2)
        col.add(iconBtn("Games & IPv4 / IPv6 test", "globe", true) { startActivity(Intent(this, GameTestActivity::class.java)) }, t = 8)
        val box = card()
        speedBox = box
        col.add(box, t = 12)
        col.add(tv("زمان پاسخ واقعی هر DNS برای یک درخواست (google.com)؛ عدد کمتر بهتر است.", 11f, T.sub), t = 8)
        updateGauge()
        renderSpeedList()
        if (speed.isEmpty() && !speedBusy) runSpeed()
        return sc
    }

    private fun renderSpeedList() {
        val box = speedBox ?: return
        box.removeAllViews()
        val sorted = repo.all().sortedWith(compareBy<Dns>({ val m = speed[it.name]; if (m == null) 2 else if (m < 0) 1 else 0 }, { speed[it.name] ?: 0L }))
        sorted.forEach { d ->
            val r = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(8), 0, dp(8)) }
            r.add(LogoView(this, brandLetter(d.name), brandColor(d.name), 34), r = 12)
            r.add(tv(d.name, 15f, T.text, true), 0, -2, weight = 1f)
            val ms = speed[d.name]
            when {
                ms == null -> r.add(tv(if (speedBusy) "…" else "—", 14f, T.sub), -2, -2)
                ms < 0 -> r.add(tv("✗", 15f, T.bad, true), -2, -2)
                else -> {
                    r.add(tv("$ms ms", 15f, T.text, true), -2, -2, r = 8)
                    r.add(BarsView(this, ms), -2, -2)
                }
            }
            box.addView(r)
        }
    }

    private fun updateGauge() {
        val g = gauge ?: return
        val best = speed.values.filter { it >= 0 }.minOrNull()
        g.value = best?.toFloat() ?: 0f
        g.valueText = if (best == null) "— ms" else "$best ms"
        g.label = if (best == null) (if (speedBusy) "Testing..." else "Ready") else verdict(best)
        g.invalidate()
    }

    private fun runSpeed() {
        if (speedBusy) return
        speedBusy = true
        speed.clear()
        val all = repo.all()
        updateGauge(); renderSpeedList()
        val left = AtomicInteger(all.size)
        val ex = Executors.newFixedThreadPool(4)
        all.forEach { d ->
            ex.execute {
                var best = -1L
                repeat(2) {
                    val r = NetTest.resolve(d.p, "google.com", 1, 2000)
                    if (r != null && (best < 0 || r.second < best)) best = r.second
                }
                runOnUiThread {
                    speed[d.name] = best
                    if (left.decrementAndGet() == 0) { speedBusy = false; if (repo.notif) toast("تست سرعت تمام شد") }
                    updateGauge(); renderSpeedList()
                }
            }
        }
        ex.shutdown()
    }

    // ------------------------------------------------------------ Providers
    private fun providersView(): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(8)) }
        root.add(header("DNS Providers", null, null))
        val sr = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL; background = rect(withAlpha(T.card, 235), T.stroke, 14); setPadding(dp(14), dp(2), dp(14), dp(2))
        }
        sr.add(IconView(this, "search", T.sub, 22), r = 10)
        val search = EditText(this).apply {
            hint = "Search providers..."; setHintTextColor(T.sub); setTextColor(T.text); textSize = 15f; background = null; setSingleLine()
        }
        sr.add(search, 0, -2, weight = 1f)
        root.add(sr, t = 8)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.add(ScrollView(this).apply { addView(list) }, -1, 0, t = 10, weight = 1f)

        fun render() {
            val q = search.text.toString().trim()
            list.removeAllViews()
            val favs = repo.favs()
            val items = repo.all().filter { it.name.contains(q, true) || it.p.contains(q) }.sortedByDescending { it.name in favs }
            items.forEach { d ->
                val sel = d.name == repo.selected
                val row = LinearLayout(this).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    background = rect(if (sel) withAlpha(T.accent, 30) else withAlpha(T.card, 235), if (sel) T.accent else T.stroke, 16, 2)
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    setOnClickListener { repo.selected = d.name; if (DnsVpnService.active) startVpn(); render() }
                    if (d.custom) setOnLongClickListener {
                        AlertDialog.Builder(this@MainActivity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                            .setMessage("«${d.name}» حذف شود؟")
                            .setPositiveButton("حذف") { _, _ -> repo.removeCustom(d.name); render() }
                            .setNegativeButton("انصراف", null).show()
                        true
                    }
                }
                row.add(LogoView(this, brandLetter(d.name), brandColor(d.name), 44), r = 12)
                val tx = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                tx.addView(tv(d.name, 16f, T.text, true))
                tx.addView(tv(d.ips, 12f, T.sub).apply { textDirection = View.TEXT_DIRECTION_LTR; gravity = Gravity.START })
                row.add(tx, 0, -2, weight = 1f)
                row.add(IconView(this, if (d.name in favs) "starf" else "star", T.accent, 28).apply {
                    setOnClickListener { repo.toggleFav(d.name); render() }
                }, -2, -2)
                list.add(row, b = 8)
            }
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { render() }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        render()
        root.add(iconBtn("World DNS  (1038)", "globe", false) { startActivity(Intent(this, WorldDnsActivity::class.java)) }, t = 4)
        root.add(iconBtn("Add Custom DNS", "plus", false) { content.removeAllViews(); content.addView(addView()) }, t = 8)
        return root
    }

    // ------------------------------------------------------------ Add custom DNS
    private fun fieldRow(icon: String, hint: String, ltr: Boolean): Pair<LinearLayout, EditText> {
        val r = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL; background = rect(withAlpha(T.card, 235), T.stroke, 14); setPadding(dp(14), dp(4), dp(14), dp(4))
        }
        r.add(IconView(this, icon, T.sub, 24), r = 10)
        val e = EditText(this).apply {
            this.hint = hint; setHintTextColor(T.sub); setTextColor(T.text); textSize = 15f; background = null
            setSingleLine(); inputType = InputType.TYPE_CLASS_TEXT
            if (ltr) textDirection = View.TEXT_DIRECTION_LTR
        }
        r.add(e, 0, -2, weight = 1f)
        return Pair(r, e)
    }

    private fun addView(): View {
        val (sc, col) = page()
        col.add(header("Add Custom DNS", "back") { show(2) })
        val seg = LinearLayout(this).apply { background = rect(withAlpha(T.card, 235), T.stroke, 14); setPadding(dp(4), dp(4), dp(4), dp(4)) }
        listOf("UDP", "DoH", "DoT").forEachIndexed { i, n ->
            val sel = i == 0
            seg.add(tv(n, 15f, if (sel) Color.BLACK else T.sub, true).apply {
                gravity = Gravity.CENTER; setPadding(0, dp(10), 0, dp(10))
                background = if (sel) rect(T.accent, 0, 10) else null
                if (!sel) setOnClickListener { toast("DoH و DoT فعلاً پشتیبانی نمی‌شود؛ فقط UDP") }
            }, 0, -2, weight = 1f)
        }
        col.add(seg, t = 8)
        col.add(tv("Server Address", 15f, T.text, true), t = 18, b = 6)
        val (r1, a1) = fieldRow("server", "e.g. 1.1.1.1  یا  2606:4700:4700::1111", true)
        col.add(r1)
        col.add(tv("Secondary Address (Optional)", 15f, T.text, true), t = 14, b = 6)
        val (r2, a2) = fieldRow("server", "e.g. 1.0.0.1", true)
        col.add(r2)
        col.add(tv("Name (Optional)", 15f, T.text, true), t = 14, b = 6)
        val (r3, nm) = fieldRow("tag", "e.g. My DNS", false)
        col.add(r3)
        col.add(iconBtn("Add DNS", "plus", true) {
            val p = a1.text.toString().trim()
            val s = a2.text.toString().trim()
            if (!NetTest.isIp(p) || (s.isNotEmpty() && !NetTest.isIp(s))) toast("آدرس IP نامعتبر است")
            else {
                val name = nm.text.toString().trim().ifEmpty { p }
                if (repo.all().any { it.name == name }) toast("این نام قبلاً وجود دارد")
                else { repo.addCustom(name, p, s); repo.selected = name; toast("اضافه شد"); show(2) }
            }
        }, t = 22)
        val info = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL; background = rect(withAlpha(T.card, 235), T.stroke, 14); setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        info.add(IconView(this, "info", T.accent, 26), r = 12)
        info.add(tv("آدرس IP (IPv4 یا IPv6) وارد کن. فقط DNS معمولی (UDP پورت ۵۳) پشتیبانی می‌شود.", 12f, T.sub), 0, -2, weight = 1f)
        col.add(info, t = 14)
        return sc
    }

    // ------------------------------------------------------------ Settings
    private fun settingsView(): View {
        val (sc, col) = page()
        col.add(header("Settings", null, null))
        fun section(t: String) = col.add(tv(t, 17f, T.text, true), t = 16, b = 8)
        fun rowBase(icon: String, title: String, sub: String?): LinearLayout {
            val r = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL; background = rect(withAlpha(T.card, 235), T.stroke, 14); setPadding(dp(14), dp(12), dp(14), dp(12))
            }
            r.add(IconView(this, icon, T.text, 26), r = 14)
            val tx = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            tx.addView(tv(title, 15f, T.text, true))
            if (sub != null) tx.addView(tv(sub, 12f, T.sub))
            r.add(tx, 0, -2, weight = 1f)
            col.add(r, b = 8)
            return r
        }
        section("Appearance")
        rowBase("moon", "Theme", "Dark")
        val acc = rowBase("drop", "Accent Color", if (repo.blue) "Blue" else "Gold")
        acc.add(Switch(this).apply { style(); isChecked = !repo.blue; setOnCheckedChangeListener { _, v -> repo.blue = !v; recreate() } }, -2, -2)
        section("Notifications")
        val n1 = rowBase("bell", "Enable Notifications", null)
        n1.add(Switch(this).apply { style(); isChecked = repo.notif; setOnCheckedChangeListener { _, v -> repo.notif = v } }, -2, -2)
        val n2 = rowBase("alert", "Connection Alerts", null)
        n2.add(CheckBox(this).apply { style(); isChecked = repo.alerts; setOnCheckedChangeListener { _, v -> repo.alerts = v } }, -2, -2)
        section("Data")
        rowBase("server", "حذف DNSهای دلخواه", null).apply {
            add(IconView(this@MainActivity, "chev", T.sub, 22), -2, -2)
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setMessage("همهٔ DNSهای افزوده‌شده حذف شوند؟")
                    .setPositiveButton("حذف") { _, _ ->
                        repo.custom().forEach { repo.removeCustom(it.name) }
                        if (repo.all().none { it.name == repo.selected }) repo.selected = DnsData.builtin[0].name
                        toast("حذف شد")
                    }
                    .setNegativeButton("انصراف", null).show()
            }
        }
        rowBase("globe", "حذف دامنه‌های دلخواه تست بازی", null).apply {
            add(IconView(this@MainActivity, "chev", T.sub, 22), -2, -2)
            setOnClickListener { repo.clearHosts(); toast("حذف شد") }
        }
        section("Privacy")
        rowBase("shield", "Privacy Policy", null).apply {
            add(IconView(this@MainActivity, "chev", T.sub, 22), -2, -2)
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle("Privacy Policy")
                    .setMessage("این برنامه هیچ اطلاعاتی جمع‌آوری یا ارسال نمی‌کند. فقط درخواست‌های DNS دستگاه به سرور انتخابی شما فرستاده می‌شود و بقیهٔ ترافیک از VPN عبور نمی‌کند.")
                    .setPositiveButton("باشه", null).show()
            }
        }
        rowBase("info", "About App", null).apply {
            add(IconView(this@MainActivity, "chev", T.sub, 22), -2, -2)
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle("DNS Changer")
                    .setMessage("نسخه 1.6\nتغییر DNS با VPN محلی، تست سرعت DNS و تست پینگ بازی‌ها با IPv4 و IPv6.")
                    .setPositiveButton("باشه", null).show()
            }
        }
        val foot = LinearLayout(this).apply {
            gravity = Gravity.CENTER; background = rect(withAlpha(T.card, 235), T.stroke, 16); setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        foot.add(ImageView(this).apply { setImageResource(R.mipmap.ic_launcher) }, 44, 44, r = 12)
        val ft = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        ft.addView(tv("DNS Changer", 16f, T.text, true)); ft.addView(tv("v1.6", 12f, T.sub))
        foot.add(ft, -2, -2)
        col.add(foot, t = 16)
        return sc
    }
}
