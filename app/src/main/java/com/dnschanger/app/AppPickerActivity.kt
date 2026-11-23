package com.dnschanger.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

/** فهرست همهٔ برنامه‌های گوشی؛ با انتخاب هر کدام، بهترین DNS برایش جستجو می‌شود */
class AppPickerActivity : Activity() {

    private class AppItem(val label: String, val pkg: String, val icon: Drawable)
    private class Holder(val icon: ImageView, val name: TextView, val sub: TextView)

    private var all: List<AppItem> = emptyList()
    private var shown: List<AppItem> = emptyList()
    private lateinit var info: TextView
    private lateinit var adapter: BaseAdapter

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val repo = DnsRepo(this)
        T.accent = Color.parseColor(if (repo.blue) "#4DA3FF" else "#FFC21A")
        window.statusBarColor = T.bg
        window.navigationBarColor = T.bg

        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), 0) }
        val hd = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        hd.add(IconView(this, "back", T.text, 26).apply { setOnClickListener { finish() } }, 44, 44)
        hd.add(tv("Choose App", 20f, T.text, true).apply { gravity = Gravity.CENTER; gold() }, 0, -2, weight = 1f)
        hd.add(View(this), 44, 44)
        col.add(hd)

        val search = EditText(this).apply {
            hint = "جستجوی برنامه..."
            setTextColor(T.text); setHintTextColor(T.sub); textSize = 15f
            isSingleLine = true
            background = rect(withAlpha(T.card, 235), T.stroke, 14)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        col.add(search, t = 10)
        info = tv("در حال خواندن برنامه‌ها...", 12f, T.sub)
        col.add(info, t = 8, b = 6)

        val lv = ListView(this).apply {
            divider = null; dividerHeight = 0
            selector = ColorDrawable(Color.TRANSPARENT)
            cacheColorHint = Color.TRANSPARENT
            isVerticalScrollBarEnabled = false
        }
        adapter = object : BaseAdapter() {
            override fun getCount() = shown.size
            override fun getItem(i: Int): Any = shown[i]
            override fun getItemId(i: Int) = i.toLong()
            override fun getView(i: Int, cv: View?, parent: ViewGroup?): View {
                val h: Holder
                val v: View
                if (cv == null) {
                    val outer = LinearLayout(this@AppPickerActivity).apply { setPadding(0, 0, 0, dp(8)) }
                    val card = LinearLayout(this@AppPickerActivity).apply {
                        gravity = Gravity.CENTER_VERTICAL
                        background = rect(withAlpha(T.card, 235), T.stroke, 14)
                        setPadding(dp(12), dp(10), dp(12), dp(10))
                    }
                    val ic = ImageView(this@AppPickerActivity)
                    card.add(ic, 44, 44, r = 12)
                    val tx = LinearLayout(this@AppPickerActivity).apply { orientation = LinearLayout.VERTICAL }
                    val n = tv("", 15f, T.text, true).apply { isSingleLine = true }
                    val s2 = tv("", 11f, T.sub).apply { isSingleLine = true; textDirection = View.TEXT_DIRECTION_LTR }
                    tx.addView(n); tx.addView(s2)
                    card.add(tx, 0, -2, weight = 1f)
                    card.add(IconView(this@AppPickerActivity, "search", T.accent, 22), -2, -2)
                    outer.addView(card, LinearLayout.LayoutParams(-1, -2))
                    h = Holder(ic, n, s2)
                    outer.tag = h
                    v = outer
                } else { v = cv; h = cv.tag as Holder }
                val a = shown[i]
                h.icon.setImageDrawable(a.icon)
                h.name.text = a.label
                h.sub.text = a.pkg
                return v
            }
        }
        lv.adapter = adapter
        lv.setOnItemClickListener { _, _, pos, _ ->
            val a = shown[pos]
            startActivity(Intent(this, DnsScanActivity::class.java)
                .putExtra("name", a.label)
                .putExtra("hosts", AppHosts.forPackage(a.pkg, a.label).toTypedArray())
                .putExtra("editable", true))
        }
        col.add(lv, -1, 0, weight = 1f)

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s.toString().trim().lowercase()
                shown = if (q.isEmpty()) all else all.filter { it.label.lowercase().contains(q) || it.pkg.lowercase().contains(q) }
                adapter.notifyDataSetChanged()
                info.text = "${shown.size} برنامه"
            }
        })

        val fl = FrameLayout(this)
        fl.addView(GlowBg(this), FrameLayout.LayoutParams(-1, -1))
        fl.addView(col, FrameLayout.LayoutParams(-1, -1))
        setContentView(fl)

        Thread {
            val pm = packageManager
            val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val list = try {
                pm.queryIntentActivities(q, 0)
                    .filter { it.activityInfo.packageName != packageName }
                    .map { AppItem(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.loadIcon(pm)) }
                    .distinctBy { it.pkg }
                    .sortedBy { it.label.lowercase() }
            } catch (_: Exception) { emptyList<AppItem>() }
            runOnUiThread {
                all = list; shown = list
                adapter.notifyDataSetChanged()
                info.text = if (list.isEmpty()) "برنامه‌ای پیدا نشد" else "${list.size} برنامه روی گوشی — یکی را انتخاب کن"
            }
        }.start()
    }
}
