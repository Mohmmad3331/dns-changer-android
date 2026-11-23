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
import java.net.URI
import java.io.DataInputStream
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

data class Dns(
    val name: String, val p: String, val s: String,
    val type: String, val desc: String, val custom: Boolean = false
) {
    val ips: String get() = if (s.isBlank() || s == "0.0.0.0") p else "$p  /  $s"

    /** نام کوتاه انگلیسی برای نمایش (مثل Cloudflare) */
    val title: String get() {
        if (name.any { it.code in 0x0600..0x06FF }) {
            val m = Regex("\\(([^)]+)\\)").find(name)
            if (m != null && m.groupValues[1].any { it in 'A'..'Z' || it in 'a'..'z' }) return m.groupValues[1]
        }
        return name
    }

    /** آدرس و پورت برای سنجش پینگ */
    fun pingTarget(): Pair<String, Int> {
        val boot = p.substringAfter('#', "")
        val main = p.substringBefore('#')
        return when {
            main.startsWith("https://") ->
                Pair(if (boot.isNotBlank()) boot else (try { URI(main).host } catch (_: Exception) { null } ?: main), 443)
            main.startsWith("tls://") ->
                Pair(if (boot.isNotBlank()) boot else main.removePrefix("tls://").substringBefore(':').substringBefore('/'), 853)
            else -> Pair(p, 53)
        }
    }
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
        Dns("کلین‌براوزینگ (CleanBrowsing)", "185.228.168.168", "185.228.169.168", "بین‌المللی", "DNS با فیلتر محتوا"),
        Dns("403 آنلاین (403.online)", "10.202.10.202", "10.202.10.102", "داخلی", "دور زدن خطای ۴۰۳ و تحریم برخی سایت‌ها"),
        Dns("کامودو (Comodo)", "8.26.56.26", "8.20.247.20", "بین‌المللی", "DNS با محافظت در برابر سایت‌های مخرب"),
        Dns("کنترل‌دی (Control D)", "76.76.2.0", "76.76.10.0", "بین‌المللی", "سریع و قابل‌تنظیم"),
        Dns("کلودفلر ضدبدافزار (Cloudflare Malware)", "1.1.1.2", "1.0.0.2", "بین‌المللی", "کلودفلر با مسدودسازی سایت‌های مخرب"),
        Dns("کلودفلر خانواده (Cloudflare Family)", "1.1.1.3", "1.0.0.3", "بین‌المللی", "کلودفلر با فیلتر مخرب و بزرگسال"),
        Dns("کواد۹ بدون فیلتر (Quad9 Unfiltered)", "9.9.9.10", "149.112.112.10", "بین‌المللی", "کواد۹ بدون مسدودسازی"),
        Dns("اد‌گارد بدون فیلتر (AdGuard Non-filtering)", "94.140.14.140", "94.140.14.141", "بین‌المللی", "اد‌گارد بدون بلاک تبلیغات"),
        Dns("کلین‌براوزینگ امنیتی (CleanBrowsing Security)", "185.228.168.9", "185.228.169.9", "بین‌المللی", "فیلتر سایت‌های مخرب"),
        Dns("اوپن‌دی‌ان‌اس خانواده (OpenDNS FamilyShield)", "208.67.222.123", "208.67.220.123", "بین‌المللی", "فیلتر محتوای بزرگسال"),
        Dns("لول۳ (Level3)", "4.2.2.1", "4.2.2.2", "بین‌المللی", "قدیمی و پایدار"),
        Dns("یاندکس (Yandex)", "77.88.8.8", "77.88.8.1", "بین‌المللی", "DNS روسی؛ برای برخی بازی‌ها امتحان‌کردنی"),
        Dns("ورایزاین (Verisign)", "64.6.64.6", "64.6.65.6", "بین‌المللی", "پایدار؛ مناسب کنسول"),
        Dns("آلترنیت (Alternate DNS)", "76.76.19.19", "76.223.122.150", "بین‌المللی", "با بلاک تبلیغات"),
        Dns("دی‌ان‌اس‌واچ (DNS.WATCH)", "84.200.69.80", "84.200.70.40", "بین‌المللی", "بدون لاگ"),
        Dns("نکست‌دی‌ان‌اس (NextDNS)", "45.90.28.0", "45.90.30.0", "بین‌المللی", "قابل‌تنظیم؛ تنظیم پیش‌فرض"),
        Dns("مولواد (Mullvad)", "194.242.2.2", "", "بین‌المللی", "حریم خصوصی، بدون لاگ"),
        Dns("کلودفلر IPv6 (Cloudflare IPv6)", "2606:4700:4700::1111", "2606:4700:4700::1001", "IPv6", "فقط اگر شبکه‌ات IPv6 دارد"),
        Dns("گوگل IPv6 (Google IPv6)", "2001:4860:4860::8888", "2001:4860:4860::8844", "IPv6", "فقط اگر شبکه‌ات IPv6 دارد"),
        Dns("کواد۹ IPv6 (Quad9 IPv6)", "2620:fe::fe", "2620:fe::9", "IPv6", "فقط اگر شبکه‌ات IPv6 دارد"),
        Dns("کلودفلر DoH (Cloudflare DoH)", "https://cloudflare-dns.com/dns-query", "", "DoH", "DNS رمزنگاری‌شده روی HTTPS"),
        Dns("گوگل DoH (Google DoH)", "https://dns.google/dns-query", "", "DoH", "DNS رمزنگاری‌شده روی HTTPS"),
        Dns("کواد۹ DoH (Quad9 DoH)", "https://dns.quad9.net/dns-query", "", "DoH", "DNS رمزنگاری‌شده روی HTTPS"),
        Dns("اد‌گارد DoH (AdGuard DoH)", "https://dns.adguard-dns.com/dns-query", "", "DoH", "DNS رمزنگاری‌شده روی HTTPS"),
        Dns("کلودفلر DoT (Cloudflare DoT)", "tls://one.one.one.one", "", "DoT", "DNS رمزنگاری‌شده روی TLS"),
        Dns("گوگل DoT (Google DoT)", "tls://dns.google", "", "DoT", "DNS رمزنگاری‌شده روی TLS"),
        Dns("کواد۹ DoT (Quad9 DoT)", "tls://dns.quad9.net", "", "DoT", "DNS رمزنگاری‌شده روی TLS")
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
        if (p.size >= 3) Dns(p[0], p[1], p[2], if (p[1].startsWith("https://")) "DoH" else if (p[1].startsWith("tls://")) "DoT" else "سفارشی", "DNS افزوده‌شده توسط شما", true) else null
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

    /** حل نام با یک DNS مشخص (IP برای UDP، https://... برای DoH، tls://... برای DoT). qtype: 1=A، 28=AAAA */
    fun resolve(dns: String, host: String, qtype: Int = 1, timeout: Int = 2000): Pair<String, Long>? {
        return try {
            val q = ByteArrayOutputStream()
            q.write(byteArrayOf(0x12, 0x34, 0x01, 0, 0, 1, 0, 0, 0, 0, 0, 0))
            host.split(".").forEach { q.write(it.length); q.write(it.toByteArray()) }
            q.write(0); q.write(byteArrayOf(0, qtype.toByte(), 0, 1))
            val data = q.toByteArray()
            val t = System.nanoTime()
            val buf: ByteArray
            if (Upstream.isEncrypted(dns)) {
                buf = Upstream.exchange(dns, data, timeout + 2000, null) ?: return null
            } else {
                val b = ByteArray(1500)
                DatagramSocket().use { s ->
                    s.soTimeout = timeout
                    s.send(DatagramPacket(data, data.size, InetAddress.getByName(dns), 53))
                    s.receive(DatagramPacket(b, b.size))
                }
                buf = b
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


/** ارتباط با DNS رمزنگاری‌شده: DoH (HTTP/1.1 روی TLS) و DoT (TLS پورت ۸۵۳) */
object Upstream {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Long>>()
    private val bootstrap = listOf("8.8.8.8", "1.1.1.1", "9.9.9.9", "78.157.42.100", "178.22.122.100")

    fun isEncrypted(spec: String) = spec.startsWith("https://") || spec.startsWith("tls://")

    /** آدرس IP سرور DoH/DoT را بدون عبور از DNS خود برنامه پیدا می‌کند */
    private fun hostIp(host: String, hint: String): String? {
        if (hint.isNotBlank()) return hint
        if (NetTest.isIp(host)) return host
        val c = cache[host]
        if (c != null && System.currentTimeMillis() - c.second < 300_000) return c.first
        for (b in bootstrap) {
            val r = NetTest.resolve(b, host, 1, 1500)
            if (r != null && !NetTest.isBogus(r.first)) {
                cache[host] = Pair(r.first, System.currentTimeMillis())
                return r.first
            }
        }
        return try { InetAddress.getByName(host).hostAddress } catch (_: Exception) { null }
    }

    private fun openTls(host: String, ip: String, port: Int, timeout: Int, protect: ((Socket) -> Unit)?): SSLSocket {
        val raw = Socket()
        protect?.invoke(raw)
        raw.connect(InetSocketAddress(ip, port), timeout)
        raw.soTimeout = timeout
        val ss = SSLSocketFactory.getDefault().createSocket(raw, host, port, true) as SSLSocket
        ss.startHandshake()
        if (!HttpsURLConnection.getDefaultHostnameVerifier().verify(host, ss.session)) {
            ss.close(); throw SSLPeerUnverifiedException("hostname mismatch")
        }
        return ss
    }

    /** spec: https://host/path[#bootstrapIP]  یا  tls://host[:port][#bootstrapIP] */
    fun exchange(spec: String, q: ByteArray, timeout: Int = 3000, protect: ((Socket) -> Unit)? = null): ByteArray? {
        return try {
            val hint = spec.substringAfter('#', "")
            val main = spec.substringBefore('#')
            if (main.startsWith("tls://")) {
                val hp = main.removePrefix("tls://")
                val host = hp.substringBefore(':').substringBefore('/')
                val port = if (hp.contains(':')) (hp.substringAfter(':').substringBefore('/').toIntOrNull() ?: 853) else 853
                val ip = hostIp(host, hint) ?: return null
                openTls(host, ip, port, timeout, protect).use { s ->
                    val o = s.outputStream
                    o.write(byteArrayOf((q.size shr 8).toByte(), q.size.toByte()))
                    o.write(q); o.flush()
                    val inp = DataInputStream(s.inputStream)
                    val len = inp.readUnsignedShort()
                    val r = ByteArray(len)
                    inp.readFully(r)
                    r
                }
            } else {
                val u = URI(main)
                val host = u.host ?: return null
                val port = if (u.port > 0) u.port else 443
                var path = u.rawPath ?: ""
                if (path.isEmpty()) path = "/dns-query"
                if (u.rawQuery != null) path += "?" + u.rawQuery
                val ip = hostIp(host, hint) ?: return null
                openTls(host, ip, port, timeout, protect).use { s ->
                    val req = "POST $path HTTP/1.1\r\nHost: $host\r\nUser-Agent: DNSChanger\r\n" +
                        "Accept: application/dns-message\r\nContent-Type: application/dns-message\r\n" +
                        "Content-Length: ${q.size}\r\nConnection: close\r\n\r\n"
                    val o = s.outputStream
                    o.write(req.toByteArray(Charsets.ISO_8859_1))
                    o.write(q); o.flush()
                    parseHttp(s.inputStream.readBytes())
                }
            }
        } catch (_: Exception) { null }
    }

    private fun parseHttp(b: ByteArray): ByteArray? {
        var idx = -1
        for (i in 0 until b.size - 3) {
            if (b[i] == 13.toByte() && b[i + 1] == 10.toByte() && b[i + 2] == 13.toByte() && b[i + 3] == 10.toByte()) { idx = i; break }
        }
        if (idx < 0) return null
        val head = String(b, 0, idx, Charsets.ISO_8859_1)
        val status = head.lineSequence().first().split(" ").getOrNull(1)
        if (status != "200") return null
        var body = b.copyOfRange(idx + 4, b.size)
        if (head.contains("transfer-encoding: chunked", true)) body = dechunk(body)
        return body
    }

    private fun dechunk(b: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        var i = 0
        while (i < b.size) {
            var j = i
            while (j + 1 < b.size && !(b[j] == 13.toByte() && b[j + 1] == 10.toByte())) j++
            if (j + 1 >= b.size) break
            val size = String(b, i, j - i, Charsets.ISO_8859_1).trim().substringBefore(';').toIntOrNull(16) ?: break
            if (size == 0) break
            i = j + 2
            if (i + size > b.size) { out.write(b, i, b.size - i); break }
            out.write(b, i, size)
            i += size + 2
        }
        return out.toByteArray()
    }
}
