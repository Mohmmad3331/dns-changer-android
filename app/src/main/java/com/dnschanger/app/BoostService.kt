package com.dnschanger.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Game Boost: هر ۲ ثانیه یک اتصال کوچک می‌زند تا رادیوی موبایل/وای‌فای در حالت خواب نرود،
 * قفل کم‌تأخیر وای‌فای می‌گیرد و کیفیت اتصال را اندازه می‌گیرد.
 */
class BoostService : Service() {

    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val EXTRA_HOST = "host"
        const val EXTRA_PORT = "port"
        @Volatile var running = false
        @Volatile var target = ""
        @Volatile var startedAt = 0L
        val samples: MutableList<Long> = java.util.Collections.synchronizedList(ArrayList<Long>())
    }

    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null
    @Volatile private var alive = false
    private var worker: Thread? = null

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopWork(); stopForeground(true); stopSelf(); return START_NOT_STICKY
        }
        val host = intent?.getStringExtra(EXTRA_HOST) ?: "1.1.1.1"
        val port = intent?.getIntExtra(EXTRA_PORT, 443) ?: 443
        startFg()
        stopWork()
        startWork(host, port)
        return START_STICKY
    }

    private fun startFg() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val pi = PendingIntent.getActivity(this, 0, Intent(this, BoostActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val b = if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel("boost", "Game Boost", NotificationManager.IMPORTANCE_LOW))
            Notification.Builder(this, "boost")
        } else {
            @Suppress("DEPRECATION") Notification.Builder(this)
        }
        val n = b.setContentTitle("Game Boost فعال است")
            .setContentText("پایش و پایدارسازی اتصال")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pi).setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(77, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(77, n)
    }

    private fun add(v: Long) {
        synchronized(samples) { samples.add(v); if (samples.size > 90) samples.removeAt(0) }
    }

    private fun startWork(host: String, port: Int) {
        alive = true; running = true
        samples.clear(); target = "$host:$port"; startedAt = System.currentTimeMillis()
        try {
            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val mode = if (Build.VERSION.SDK_INT >= 29) WifiManager.WIFI_MODE_FULL_LOW_LATENCY else WifiManager.WIFI_MODE_FULL_HIGH_PERF
            wifiLock = wm.createWifiLock(mode, "dnschanger:boost").also { it.setReferenceCounted(false); it.acquire() }
        } catch (_: Exception) {}
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "dnschanger:boost").also {
                it.setReferenceCounted(false); it.acquire(4 * 60 * 60 * 1000L)
            }
        } catch (_: Exception) {}
        worker = Thread {
            var addr: InetAddress? = null
            var resolvedAt = 0L
            while (alive) {
                try {
                    if (addr == null || System.currentTimeMillis() - resolvedAt > 60000) {
                        addr = InetAddress.getByName(host); resolvedAt = System.currentTimeMillis()
                    }
                    val t = System.nanoTime()
                    Socket().use { it.connect(InetSocketAddress(addr, port), 2000) }
                    add((System.nanoTime() - t) / 1_000_000)
                } catch (_: Exception) { add(-1) }
                try { Thread.sleep(2000) } catch (_: InterruptedException) {}
            }
        }.also { it.start() }
    }

    private fun stopWork() {
        alive = false; running = false
        worker?.interrupt(); worker = null
        try { wifiLock?.let { if (it.isHeld) it.release() } } catch (_: Exception) {}
        try { wakeLock?.let { if (it.isHeld) it.release() } } catch (_: Exception) {}
        wifiLock = null; wakeLock = null
    }

    override fun onDestroy() { stopWork(); super.onDestroy() }
}
