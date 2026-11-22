package com.dnschanger.app

import android.content.Context
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

data class Dns(
    val name: String, val p: String, val s: String,
    val type: String, val desc: String, val custom: Boolean = false
) {
    val ips: String get() = if (s.isBlank() || s == "0.0.0.0") p else "$p  /  $s"
}

data class Game(val name: String, val hosts: List<String>)

object DnsData {
    val builtin = listOf(

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
        Dns("کواد۹ (Quad9)", "9.9.9.9", "149.112.112.112", "بین‌المللی", "DNS بین‌المللی با تمرکز بر امنیت"),
        Dns("اد‌گارد (AdGuard)", "94.140.14.14", "94.140.15.15", "بین‌المللی", "DNS با مسدودسازی تبلیغات"),
        Dns("اوپن‌دی‌ان‌اس (OpenDNS)", "208.67.222.222", "208.67.220.220", "بین‌المللی", "DNS سیسکو، پایدار و شناخته‌شده"),
        Dns("کلین‌براوزینگ (CleanBrowsing)", "185.228.168.168", "185.228.169.168", "بین‌المللی", "DNS با فیلتر محتوا")
    )

    val games = listOf(
        Game("Mobile Legends", listOf("mobilelegends.com", "m.mobilelegends.com", "moonton.com")),
        Game("PUBG Mobile", listOf("pubgmobile.com", "igamecj.com", "proximabeta.com")),
        Game("Call of Duty Mobile", listOf("callofduty.com", "activision.com")),
        Game("Free Fire", listOf("ff.garena.com", "garena.com", "freefiremobile.com")),
        Game("Clash of Clans / Royale", listOf("supercell.com", "clashofclans.com", "clashroyale.com")),
        Game("Brawl Stars", listOf("brawlstars.com", "supercell.com")),
        Game("Genshin Impact", listOf("hoyoverse.com", "genshin.hoyoverse.com", "mihoyo.com")),
        Game("Fortnite", listOf("epicgames.com", "fortnite.com")),
        Game("Valorant / League", listOf("playvalorant.com", "leagueoflegends.com", "riotgames.com")),
        Game("Roblox / Minecraft", listOf("roblox.com", "minecraft.net")),
        Game("Steam / Dota 2", listOf("store.steampowered.com", "steamcommunity.com", "dota2.com")),
        Game("PlayStation Network", listOf("playstation.com", "playstation.net")),
        Game("اینترنت عمومی", listOf("google.com", "cloudflare.com", "youtube.com"))
    )
}

class DnsRepo(ctx: Context) {
    private val sp = ctx.getSharedPreferences("dnsprefs", Context.MODE_PRIVATE)
    private fun clean(s: String) = s.replace("|", " ").replace("\n", " ").trim()

    fun custom(): List<Dns> = (sp.getString("custom", "") ?: "").lines().mapNotNull {
        val p = it.split("|")
        if (p.size >= 3) Dns(p[0], p[1], p[2], "سفارشی", "DNS افزوده‌شده توسط شما", true) else null
    }

    fun addCustom(name: String, a: String, b: String) {
        val cur = sp.getString("custom", "") ?: ""
        sp.edit().putString("custom", (cur + "\n" + clean(name) + "|" + clean(a) + "|" + clean(b)).trim()).apply()
    }

    fun removeCustom(name: String) {
        val kept = (sp.getString("custom", "") ?: "").lines().filter { it.isNotBlank() && !it.startsWith("$name|") }
        sp.edit().putString("custom", kept.joinToString("\n")).apply()
    }

    fun all(): List<Dns> = DnsData.builtin + custom()

    var selected: String
        get() = sp.getString("sel", DnsData.builtin[0].name) ?: DnsData.builtin[0].name
        set(v) { sp.edit().putString("sel", v).apply() }

    fun favs(): Set<String> = (sp.getStringSet("favs", emptySet()) ?: emptySet()).toSet()
    fun toggleFav(n: String) {
        val s = favs().toMutableSet()
        if (!s.add(n)) s.remove(n)
        sp.edit().putStringSet("favs", s).apply()
    }

    var blue: Boolean
        get() = sp.getBoolean("blue", false)
        set(v) { sp.edit().putBoolean("blue", v).apply() }
    var alerts: Boolean
        get() = sp.getBoolean("alerts", true)
        set(v) { sp.edit().putBoolean("alerts", v).apply() }

    var notif: Boolean
        get() = sp.getBoolean("notif", true)
        set(v) { sp.edit().putBoolean("notif", v).apply() }

    fun hosts(): List<String> = (sp.getString("hosts", "") ?: "").lines().filter { it.isNotBlank() }
    fun addHost(h: String) {
        sp.edit().putString("hosts", (hosts() + clean(h)).joinToString("\n")).apply()
    }
    fun clearHosts() { sp.edit().putString("hosts", "").apply() }
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

    fun isIpv4(s: String) = Regex("^(\\d{1,3}\\.){3}\\d{1,3}$").matches(s) && s.split(".").all { it.toInt() in 0..255 }
    fun isIpv6(s: String) = s.contains(":") && Regex("^[0-9a-fA-F:.]+$").matches(s)
    fun isIp(s: String) = isIpv4(s) || isIpv6(s)

    /** حل نام با یک DNS مشخص. qtype: 1 = IPv4 (A)، 28 = IPv6 (AAAA). خروجی: (IP، زمان حل نام) */
    fun resolve(dns: String, host: String, qtype: Int = 1, timeout: Int = 2000): Pair<String, Long>? {
        return try {
            val q = ByteArrayOutputStream()
            q.write(byteArrayOf(0x12, 0x34, 0x01, 0, 0, 1, 0, 0, 0, 0, 0, 0))
            host.split(".").forEach { q.write(it.length); q.write(it.toByteArray()) }
            q.write(0); q.write(byteArrayOf(0, qtype.toByte(), 0, 1))
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
                if (type == qtype && ((qtype == 1 && rdlen == 4) || (qtype == 28 && rdlen == 16))) {
                    val bytes = buf.copyOfRange(pos + 10, pos + 10 + rdlen)
                    result = Pair(InetAddress.getByAddress(bytes).hostAddress ?: "", ms)
                    break
                }
                pos += 10 + rdlen
            }
            result
        } catch (_: Exception) { null }
    }

    /** حل نام با DNS پیش‌فرض سیستم */
    fun resolveSystem(host: String, v6: Boolean): Pair<String, Long>? = try {
        val t = System.nanoTime()
        val all = InetAddress.getAllByName(host)
        val a = if (v6) all.firstOrNull { it is Inet6Address } else all.firstOrNull { it is Inet4Address }
        if (a == null) null else Pair(a.hostAddress ?: "", (System.nanoTime() - t) / 1_000_000)
    } catch (_: Exception) { null }

    /** میانگین زمان اتصال TCP. null = ناموفق */
    fun tcpPing(ip: String, port: Int = 443, tries: Int = 2, timeout: Int = 1500): Long? {
        val r = mutableListOf<Long>()
        repeat(tries) {
            try {
                val t = System.nanoTime()
                Socket().use { it.connect(InetSocketAddress(ip, port), timeout) }
                r.add((System.nanoTime() - t) / 1_000_000)
            } catch (_: Exception) {}
        }
        return if (r.isEmpty()) null else r.sum() / r.size
    }

    fun isBogus(ip: String) = ip.startsWith("10.") || ip.startsWith("127.") || ip.startsWith("192.168.") ||
        ip == "0.0.0.0" || ip == "::" || ip == "::1" || ip.startsWith("fc") || ip.startsWith("fd") || ip.startsWith("fe80")
}
