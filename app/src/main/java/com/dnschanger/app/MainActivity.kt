package com.dnschanger.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
    private lateinit var navRow: LinearLayout
    private var tab = 0
    private val h = Handler(Looper.getMainLooper())
    private val navItems = listOf("خانه" to "⌂", "سرعت" to "◔", "سرورها" to "◎", "تنظیمات" to "⚙")

    // نتایج تست سرعت
    private val speed = HashMap<String, Long>()
    private var speedBusy = false
    private var gauge: GaugeView? = null
    private val speedRows = HashMap<String, TextView>()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        repo = DnsRepo(this)
        T.accent = Color.parseColor(if (repo.blue) "#4DA3FF" else "#FFC21A")
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        window.statusBarColor = T.bg
        window.navigationBarColor = T.card
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(T.bg) }
        content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        navRow = LinearLayout(this).apply { setBackgroundColor(T.card); setPadding(0, dp(8), 0, dp(8)) }
        root.addView(navRow, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
        show(b?.getInt("tab") ?: 0)
    }

    override fun onSaveInstanceState(o: Bundle) { super.onSaveInstanceState(o); o.putInt("tab", tab) }
    override fun onResume() { super.onResume(); if (tab == 0 && ::content.isInitialized) show(0) }

    private fun current(): Dns = repo.all().firstOrNull { it.name == repo.selected } ?: DnsData.builtin[0]
    private fun alert(m: String) { if (repo.alerts) Toast.makeText(this, m, Toast.LENGTH_SHORT).show() }

    private fun show(t: Int) {
        tab = t
        content.removeAllViews()
        content.addView(when (t) { 0 -> homeView(); 1 -> speedView(); 2 -> providersView(); else -> settingsView() })
        navRow.removeAllViews()
        navItems.forEachIndexed { i, (name, icon) ->
            val c = if (i == t) T.accent else T.sub
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setOnClickListener { show(i) }
            }
            item.addView(tv(icon, 22f, c, true))
            item.addView(tv(name, 11f, c, i == t))
            navRow.addView(item, LinearLayout.LayoutParams(0, -2, 1f))
        }
    }

    private fun page(): Pair<ScrollView, LinearLayout> {
        val sc = ScrollView(this)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)) }
        sc.addView(col)
        return Pair(sc, col)
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; background = rect(T.card, T.stroke); setPadding(dp(14), dp(12), dp(14), dp(12))
    }

    private fun title(t: String) = tv(t, 20f, T.text, true).apply { gravity = Gravity.CENTER }

    // ---------------------------------------------------------------- خانه
    private fun homeView(): View {
        val (sc, col) = page()
        val on = DnsVpnService.active
        val d = current()
        col.add(title("DNS Changer"))
        val power = TextView(this).apply {
            text = "⏻"; textSize = 72f; gravity = Gravity.CENTER
            setTextColor(if (on) T.accent else T.sub)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(T.card); setStroke(dp(6), if (on) T.accent else T.stroke) }
            setOnClickListener { toggle() }
        }
        val pl = LinearLayout.LayoutParams(dp(190), dp(190)).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(24) }
        col.addView(power, pl)
        col.add(tv(if (on) "● متصل" else "○ قطع", 22f, if (on) T.ok else T.sub, true).apply { gravity = Gravity.CENTER }, t = 14)
        col.add(tv(if (on) "با ${DnsVpnService.currentName.ifBlank { d.name }}" else "انتخاب‌شده: ${d.name}", 13f, T.sub).apply { gravity = Gravity.CENTER })

        val c = card().apply { setOnClickListener { show(2) } }
        c.addView(tv(d.name, 16f, T.text, true))
        c.addView(tv(d.ips, 13f, T.accent).apply { textDirection = View.TEXT_DIRECTION_LTR })
        c.addView(tv(d.desc, 12f, T.sub))
        col.add(c, t = 20)

        val row = LinearLayout(this)
        val pingTv = tv("…", 20f, T.text, true)
        val t1 = card().apply { addView(tv("⚡ پینگ", 12f, T.sub)); addView(pingTv) }
        val t2 = card().apply { addView(tv("📶 شبکه", 12f, T.sub)); addView(tv(netName(), 20f, T.text, true)) }
        row.add(t1, 0, -2, 0, 0, 6, 0, 1f)
        row.add(t2, 0, -2, 6, 0, 0, 0, 1f)
        col.add(row, t = 12)

        val health = tv(if (on) "در حال بررسی DNS ..." else "DNS متصل نیست", 14f, if (on) T.ok else T.sub, true).apply {
            background = rect(Color.parseColor("#122A1C"), if (on) T.ok else T.stroke, 14, 1)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        col.add(health, t = 12)

        Thread {
            val ms = NetTest.tcpPing(d.p, 53)
            runOnUiThread { pingTv.text = if (ms == null) "✗" else "$ms ms" }
        }.start()
        if (on) Thread {
            Thread.sleep(1200)
            val r = NetTest.resolveSystem("google.com", false)
            runOnUiThread {
                if (r != null && !NetTest.isBogus(r.first)) health.text = "✓ DNS درست کار می‌کند (${r.second}ms)"
                else { health.text = "✗ DNS جواب درست نمی‌دهد"; health.setTextColor(T.bad) }
            }
        }.start()
        return sc
    }

    @Suppress("DEPRECATION")
    private fun netName(): String {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var r = "قطع"
        for (n in cm.allNetworks) {
            val c = cm.getNetworkCapabilities(n) ?: continue
            if (c.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) continue
            if (c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "Wi-Fi"
            if (c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) r = "موبایل"
            else if (r == "قطع") r = "سایر"
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

    // ---------------------------------------------------------------- سرعت
    private fun speedView(): View {
        val (sc, col) = page()
        col.add(title("تست سرعت DNS"))
        val g = GaugeView(this)
        gauge = g
        col.add(g, -1, 230, t = 8)
        col.add(goldBtn("▶  تست دوباره", false) { runSpeed() }, t = 4)
        col.add(goldBtn("🎮  تست بازی‌ها و IPv4 / IPv6", true) {
            startActivity(Intent(this, GameTestActivity::class.java))
        }, t = 8)
        val c = card()
        speedRows.clear()
        repo.all().forEach { d ->
            val r = tv("", 14f).apply { setPadding(0, dp(9), 0, dp(9)) }
            speedRows[d.name] = r
            c.addView(r)
            setRow(d)
        }
        col.add(c, t = 12)
        col.add(tv("زمان پاسخ واقعی هر DNS برای یک درخواست (google.com). عدد کمتر بهتر است.", 11f, T.sub), t = 8)
        updateGauge()
        return sc
    }

    private fun setRow(d: Dns) {
        val ms = speed[d.name]
        speedRows[d.name]?.text = when {
            ms == null -> "${d.name}   —"
            ms < 0 -> "${d.name}   ✗ بدون پاسخ"
            else -> "${d.name}   $ms ms   ${bars(ms)}"
        }
    }

    private fun updateGauge() {
        val g = gauge ?: return
        val best = speed.values.filter { it >= 0 }.minOrNull()
        g.value = best?.toFloat() ?: 0f
        g.valueText = if (best == null) "— ms" else "$best ms"
        g.label = if (best == null) (if (speedBusy) "در حال تست..." else "آماده") else verdict(best)
        g.invalidate()
    }

    private fun runSpeed() {
        if (speedBusy) return
        speedBusy = true
        speed.clear()
        val all = repo.all()
        all.forEach { setRow(it) }
        updateGauge()
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
                    setRow(d)
                    if (left.decrementAndGet() == 0) speedBusy = false
                    updateGauge()
                }
            }
        }
        ex.shutdown()
    }

    // ---------------------------------------------------------------- سرورها
    private val avatarColors = listOf("#F6821F", "#4285F4", "#E5397A", "#2FBF71", "#FF6A00", "#7B61FF", "#00B8D9")

    private fun providersView(): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(8)) }
        root.add(title("سرورهای DNS"))
        val search = EditText(this).apply {
            hint = "جستجو..."; setHintTextColor(T.sub); setTextColor(T.text); textSize = 14f
            background = rect(T.card, T.stroke); setPadding(dp(14), dp(10), dp(14), dp(10)); setSingleLine()
        }
        root.add(search, t = 12)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val sc = ScrollView(this).apply { addView(list) }
        root.add(sc, -1, 0, t = 10, weight = 1f)

        fun render() {
            val q = search.text.toString().trim()
            list.removeAllViews()
            val favs = repo.favs()
            val items = repo.all().filter { it.name.contains(q, true) || it.p.contains(q) }.sortedByDescending { it.name in favs }
            items.forEach { d ->
                val sel = d.name == repo.selected
                val row = LinearLayout(this).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    background = rect(if (sel) Color.parseColor("#26210F") else T.card, if (sel) T.accent else T.stroke, 14, 2)
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    setOnClickListener {
                        repo.selected = d.name
                        if (DnsVpnService.active) startVpn()
                        render()
                    }
                    if (d.custom) setOnLongClickListener {
                        AlertDialog.Builder(this@MainActivity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                            .setMessage("«${d.name}» حذف شود؟")
                            .setPositiveButton("حذف") { _, _ -> repo.removeCustom(d.name); render() }
                            .setNegativeButton("انصراف", null).show()
                        true
                    }
                }
                val av = tv(d.name.take(1).uppercase(), 18f, Color.WHITE, true).apply {
                    gravity = Gravity.CENTER
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor(avatarColors[Math.abs(d.name.hashCode()) % avatarColors.size]))
                    }
                }
                row.add(av, 42, 42, r = 12)
                val tx = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                tx.addView(tv(d.name, 15f, T.text, true))
                tx.addView(tv(d.ips, 12f, T.sub).apply { textDirection = View.TEXT_DIRECTION_LTR })
                row.add(tx, 0, -2, weight = 1f)
                row.add(tv(if (d.name in favs) "★" else "☆", 24f, T.accent).apply {
                    setOnClickListener { repo.toggleFav(d.name); render() }
                    setPadding(dp(8), 0, dp(4), 0)
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
        root.add(goldBtn("＋  افزودن DNS دلخواه", false) { addDialog { render() } }, t = 4)
        return root
    }

    private fun addDialog(after: () -> Unit) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0) }
        fun field(hint: String, ltr: Boolean): EditText = EditText(this).apply {
            this.hint = hint; setSingleLine(); inputType = InputType.TYPE_CLASS_TEXT
            if (ltr) textDirection = View.TEXT_DIRECTION_LTR
        }
        val a1 = field("آدرس اصلی؛ مثلاً 1.1.1.1 یا 2606:4700:4700::1111", true)
        val a2 = field("آدرس ثانویه (اختیاری)", true)
        val nm = field("نام (اختیاری)", false)
        box.addView(a1); box.addView(a2); box.addView(nm)
        box.addView(tv("فقط DNS معمولی (UDP پورت ۵۳) با آدرس IP پشتیبانی می‌شود.", 11f, T.sub))
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("افزودن DNS")
            .setView(box)
            .setPositiveButton("افزودن") { _, _ ->
                val p = a1.text.toString().trim()
                val s = a2.text.toString().trim()
                if (!NetTest.isIp(p) || (s.isNotEmpty() && !NetTest.isIp(s))) {
                    Toast.makeText(this, "آدرس IP نامعتبر است", Toast.LENGTH_LONG).show()
                } else {
                    val name = nm.text.toString().trim().ifEmpty { p }
                    if (repo.all().any { it.name == name }) Toast.makeText(this, "این نام قبلاً وجود دارد", Toast.LENGTH_LONG).show()
                    else { repo.addCustom(name, p, s); after() }
                }
            }
            .setNegativeButton("انصراف", null).show()
    }

    // ---------------------------------------------------------------- تنظیمات
    private fun settingsView(): View {
        val (sc, col) = page()
        col.add(title("تنظیمات"))
        fun section(t: String) = col.add(tv(t, 15f, T.sub, true), t = 18, b = 6)
        fun switchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit): View {
            val r = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                background = rect(T.card, T.stroke); setPadding(dp(14), dp(12), dp(14), dp(12))
            }
            r.add(tv(label, 15f), 0, -2, weight = 1f)
            r.add(Switch(this).apply { isChecked = checked; setOnCheckedChangeListener { _, v -> onChange(v) } }, -2, -2)
            return col.add(r, b = 8)
        }
        fun linkRow(label: String, onClick: () -> Unit) {
            val r = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                background = rect(T.card, T.stroke); setPadding(dp(14), dp(14), dp(14), dp(14)); setOnClickListener { onClick() }
            }
            r.add(tv(label, 15f), 0, -2, weight = 1f)
            r.add(tv("›", 20f, T.sub), -2, -2)
            col.add(r, b = 8)
        }
        section("ظاهر")
        switchRow("رنگ اصلی: آبی (خاموش = طلایی)", repo.blue) { repo.blue = it; recreate() }
        section("اعلان‌ها")
        switchRow("هشدار اتصال و قطع", repo.alerts) { repo.alerts = it }
        section("داده‌ها")
        linkRow("حذف DNSهای دلخواه") {
            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setMessage("همهٔ DNSهای افزوده‌شده حذف شوند؟")
                .setPositiveButton("حذف") { _, _ ->
                    repo.custom().forEach { repo.removeCustom(it.name) }
                    if (repo.all().none { it.name == repo.selected }) repo.selected = DnsData.builtin[0].name
                    Toast.makeText(this, "حذف شد", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("انصراف", null).show()
        }
        linkRow("حذف دامنه‌های دلخواه تست بازی") { repo.clearHosts(); Toast.makeText(this, "حذف شد", Toast.LENGTH_SHORT).show() }
        section("حریم خصوصی")
        linkRow("سیاست حریم خصوصی") {
            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("حریم خصوصی")
                .setMessage("این برنامه هیچ اطلاعاتی جمع‌آوری یا ارسال نمی‌کند. فقط درخواست‌های DNS دستگاه به سرور انتخابی شما فرستاده می‌شود و بقیهٔ ترافیک از VPN عبور نمی‌کند.")
                .setPositiveButton("باشه", null).show()
        }
        linkRow("دربارهٔ برنامه") {
            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("DNS Changer")
                .setMessage("نسخه 1.1\nتغییر DNS با VPN محلی، تست سرعت DNS و تست پینگ بازی‌ها با IPv4 و IPv6.")
                .setPositiveButton("باشه", null).show()
        }
        col.add(tv("DNS Changer  v1.1", 12f, T.sub).apply { gravity = Gravity.CENTER }, t = 16)
        return sc
    }
}
