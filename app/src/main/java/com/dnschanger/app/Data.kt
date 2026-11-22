package com.dnschanger.app

import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

data class Dns(val name: String, val p: String, val s: String, val type: String, val desc: String)
data class Game(val name: String, val host: String)

object DnsData {
    val list = listOf(

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

    val games = listOf(
        Game("Mobile Legends", "mobilelegends.com"),
        Game("PUBG Mobile", "pubgmobile.com"),
        Game("Call of Duty Mobile", "callofduty.com"),
        Game("Free Fire", "ff.garena.com"),
        Game("Clash of Clans / Royale", "supercell.com"),
        Game("Brawl Stars", "brawlstars.com"),
        Game("Genshin Impact", "hoyoverse.com"),
        Game("Fortnite", "epicgames.com"),
        Game("Steam", "store.steampowered.com"),
        Game("PlayStation Network", "playstation.com"),
        Game("اینترنت عمومی (Google)", "google.com"),
        Game("اینترنت عمومی (Cloudflare)", "cloudflare.com")
    )
}

object NetTest {
    private fun u16(b: ByteArray, i: Int) = ((b[i].toInt() and 0xFF) shl 8) or (b[i + 1].toInt() and 0xFF)

    private fun skipName(b: ByteArray, p: Int): Int {
        var i = p
        while (true) {
            val l = b[i].toInt() and 0xFF
            if (l == 0) return i + 1
            if (l and 0xC0 == 0xC0) return i + 2
            i += l + 1
        }
    }

    /** حل نام با یک DNS مشخص. خروجی: (IP، زمان حل نام به میلی‌ثانیه) */
    fun resolve(dns: String, host: String, timeout: Int = 2500): Pair<String, Long>? {
        return try {
            val q = ByteArrayOutputStream()
            q.write(byteArrayOf(0x12, 0x34, 0x01, 0, 0, 1, 0, 0, 0, 0, 0, 0))
            host.split(".").forEach { q.write(it.length); q.write(it.toByteArray()) }
            q.write(0); q.write(byteArrayOf(0, 1, 0, 1))
            val data = q.toByteArray()
            val t = System.nanoTime()
            val buf = ByteArray(1500)
            DatagramSocket().use { s ->
                s.soTimeout = timeout
                s.send(DatagramPacket(data, data.size, InetAddress.getByName(dns), 53))
                s.receive(DatagramPacket(buf, buf.size))
            }
            val ms = (System.nanoTime() - t) / 1_000_000
            var pos = 12
            val qd = u16(buf, 4)
            val an = u16(buf, 6)
            repeat(qd) { pos = skipName(buf, pos) + 4 }
            var result: Pair<String, Long>? = null
            for (k in 0 until an) {
                pos = skipName(buf, pos)
                val type = u16(buf, pos)
                val rdlen = u16(buf, pos + 8)
                if (type == 1 && rdlen == 4) {
                    val ip = (0 until 4).joinToString(".") { (buf[pos + 10 + it].toInt() and 0xFF).toString() }
                    result = Pair(ip, ms); break
                }
                pos += 10 + rdlen
            }
            result
        } catch (_: Exception) { null }
    }

    /** حل نام با DNS پیش‌فرض سیستم */
    fun resolveSystem(host: String): Pair<String, Long>? = try {
        val t = System.nanoTime()
        val ip = InetAddress.getAllByName(host).first { it.address.size == 4 }.hostAddress!!
        Pair(ip, (System.nanoTime() - t) / 1_000_000)
    } catch (_: Exception) { null }

    /** میانگین زمان اتصال TCP (۳ بار). null = ناموفق */
    fun tcpPing(ip: String, port: Int = 443): Long? {
        val r = mutableListOf<Long>()
        repeat(3) {
            try {
                val t = System.nanoTime()
                Socket().use { it.connect(InetSocketAddress(ip, port), 2000) }
                r.add((System.nanoTime() - t) / 1_000_000)
            } catch (_: Exception) {}
        }
        return if (r.isEmpty()) null else r.sum() / r.size
    }

    fun isBogus(ip: String) = ip.startsWith("10.") || ip.startsWith("127.") ||
        ip.startsWith("192.168.") || ip == "0.0.0.0"
}
