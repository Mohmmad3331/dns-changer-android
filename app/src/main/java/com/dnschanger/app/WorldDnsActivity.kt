package com.dnschanger.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*

class WorldDnsActivity : Activity() {

    private class W(val cc: String, val country: String, val provider: String, val ip: String) {
        val short: String get() = provider.substringBefore(" - ").trim()
    }

    private lateinit var repo: DnsRepo
    private val all = ArrayList<W>()
    private lateinit var list: LinearLayout
    private lateinit var count: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        repo = DnsRepo(this)
        T.accent = Color.parseColor(if (repo.blue) "#4DA3FF" else "#FFC21A")
        window.statusBarColor = T.bg
        window.navigationBarColor = T.bg
        try {
            assets.open("world_dns.csv").bufferedReader().useLines { ls ->
                ls.forEach { l ->
                    val p = l.split("|")
                    if (p.size >= 4) all.add(W(p[0], p[1], p[2], p[3]))
                }
            }
        } catch (_: Exception) {}

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(8)) }
        val hd = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        hd.add(IconView(this, "back", T.text, 26).apply { setOnClickListener { finish() } }, 44, 44)
        hd.add(tv("World DNS", 20f, T.text, true).apply { gravity = Gravity.CENTER }, 0, -2, weight = 1f)
        hd.add(View(this), 44, 44)
        root.add(hd)

        val sr = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL; background = rect(withAlpha(T.card, 235), T.stroke, 14); setPadding(dp(14), dp(2), dp(14), dp(2))
        }
        sr.add(IconView(this, "search", T.sub, 22), r = 10)
        val search = EditText(this).apply {
            hint = "کشور، کد کشور (مثلاً DE)، ارائه‌دهنده یا IP"; setHintTextColor(T.sub); setTextColor(T.text)
            textSize = 14f; background = null; setSingleLine(); inputType = InputType.TYPE_CLASS_TEXT
        }
        sr.add(search, 0, -2, weight = 1f)
        root.add(sr, t = 8)
        count = tv("", 12f, T.sub)
        root.add(count, t = 8, b = 4)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.add(ScrollView(this).apply { addView(list) }, -1, 0, weight = 1f)
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { render(s.toString().trim()) }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        val fl = FrameLayout(this)
        fl.addView(GlowBg(this), FrameLayout.LayoutParams(-1, -1))
        fl.addView(root, FrameLayout.LayoutParams(-1, -1))
        setContentView(fl)
        render("")
    }

    private fun render(q: String) {
        list.removeAllViews()
        val m = all.filter {
            q.isEmpty() || it.country.contains(q, true) || it.cc.equals(q, true) || it.provider.contains(q, true) || it.ip.contains(q)
        }
        count.text = "${m.size} DNS" + if (m.size > 60) " — ۶۰ تای اول نمایش داده شد؛ جستجو را دقیق‌تر کن" else ""
        m.take(60).forEach { w ->
            val row = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                background = rect(withAlpha(T.card, 235), T.stroke, 16)
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }
            row.add(LogoView(this, w.cc.take(2), brandColor(w.cc), 40), r = 12)
            val tx = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            tx.addView(tv(w.short, 14f, T.text, true).apply { textDirection = View.TEXT_DIRECTION_LTR })
            tx.addView(tv(w.country, 11f, T.sub))
            tx.addView(tv(w.ip, 12f, T.accent).apply { textDirection = View.TEXT_DIRECTION_LTR })
            row.add(tx, 0, -2, weight = 1f)
            row.add(tv("Use", 14f, Color.BLACK, true).apply {
                gravity = Gravity.CENTER; background = rect(T.accent, 0, 10); setPadding(dp(14), dp(8), dp(14), dp(8))
                setOnClickListener { use(w) }
            }, -2, -2)
            list.add(row, b = 8)
        }
    }

    private fun use(w: W) {
        val existing = repo.all().firstOrNull { it.p == w.ip }
        val name = existing?.name ?: (w.short + " (" + w.cc + ") · " + w.ip).also { repo.addCustom(it, w.ip, "") }
        repo.selected = name
        if (DnsVpnService.active) {
            startService(Intent(this, DnsVpnService::class.java)
                .setAction(DnsVpnService.ACTION_START)
                .putExtra(DnsVpnService.EXTRA_NAME, name)
                .putExtra(DnsVpnService.EXTRA_DNS1, w.ip)
                .putExtra(DnsVpnService.EXTRA_DNS2, ""))
            Toast.makeText(this, "DNS عوض شد", Toast.LENGTH_SHORT).show()
        } else Toast.makeText(this, "انتخاب شد؛ در صفحهٔ Home وصل شو", Toast.LENGTH_LONG).show()
    }
}
