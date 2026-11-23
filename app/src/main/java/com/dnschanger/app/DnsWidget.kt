package com.dnschanger.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.widget.RemoteViews

/** ویجت صفحهٔ اصلی: وضعیت DNS و دکمهٔ اتصال/قطع */
class DnsWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, m: AppWidgetManager, ids: IntArray) {
        val v = build(ctx)
        for (id in ids) m.updateAppWidget(id, v)
    }

    companion object {
        fun refresh(ctx: Context) {
            try {
                val m = AppWidgetManager.getInstance(ctx)
                val ids = m.getAppWidgetIds(ComponentName(ctx, DnsWidget::class.java))
                if (ids.isEmpty()) return
                val v = build(ctx)
                for (id in ids) m.updateAppWidget(id, v)
            } catch (_: Exception) {}
        }

        private fun build(ctx: Context): RemoteViews {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_dns)
            val repo = DnsRepo(ctx)
            val on = DnsVpnService.active
            val d = repo.all().firstOrNull { it.name == repo.selected } ?: DnsData.builtin[0]
            rv.setTextViewText(R.id.w_status, if (on) "Connected" else "Disconnected")
            rv.setTextColor(R.id.w_status, if (on) 0xFF3DDC84.toInt() else 0xFF9A9AAE.toInt())
            rv.setTextViewText(R.id.w_name, if (on) DnsVpnService.currentName.ifBlank { d.title } else d.title)
            rv.setTextViewText(R.id.w_btn, if (on) "ON" else "OFF")
            rv.setTextColor(R.id.w_btn, if (on) 0xFF000000.toInt() else 0xFFFFC21A.toInt())
            rv.setInt(R.id.w_btn, "setBackgroundResource", if (on) R.drawable.w_btn_on else R.drawable.w_btn_off)
            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            val pi = if (on) {
                PendingIntent.getService(ctx, 1, Intent(ctx, DnsVpnService::class.java).setAction(DnsVpnService.ACTION_STOP), flags)
            } else if (VpnService.prepare(ctx) != null) {
                PendingIntent.getActivity(ctx, 2, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags)
            } else {
                PendingIntent.getService(ctx, 3, Intent(ctx, DnsVpnService::class.java)
                    .setAction(DnsVpnService.ACTION_START)
                    .putExtra(DnsVpnService.EXTRA_NAME, d.name)
                    .putExtra(DnsVpnService.EXTRA_DNS1, d.p)
                    .putExtra(DnsVpnService.EXTRA_DNS2, d.s), flags)
            }
            rv.setOnClickPendingIntent(R.id.w_root, pi)
            return rv
        }
    }
}
