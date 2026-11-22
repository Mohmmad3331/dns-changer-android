package com.dnschanger.app

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors

/** VPN محلی: فقط ترافیک DNS را می‌گیرد و به DNS انتخابی می‌فرستد. */
class DnsVpnService : VpnService() {

    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val EXTRA_DNS1 = "dns1"
        const val EXTRA_DNS2 = "dns2"
        @Volatile var active = false
        private const val FAKE_DNS = "10.111.222.2"
    }

    private var tun: ParcelFileDescriptor? = null
    private var worker: Thread? = null
    @Volatile private var running = false
    private val pool = Executors.newCachedThreadPool()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn(); stopSelf(); return START_NOT_STICKY
        }
        val d1 = intent?.getStringExtra(EXTRA_DNS1) ?: return START_NOT_STICKY
        val d2 = intent.getStringExtra(EXTRA_DNS2)
        stopVpn()
        startVpn(d1, d2)
        return START_STICKY
    }

    private fun startVpn(d1: String, d2: String?) {
        val b = Builder()
            .setSession("DNS Changer")
            .addAddress("10.111.222.1", 32)
            .addDnsServer(FAKE_DNS)
            .addRoute(FAKE_DNS, 32)
        tun = b.establish() ?: return
        running = true
        active = true
        val servers = listOfNotNull(d1, d2).filter { it != "0.0.0.0" }
        worker = Thread { loop(servers) }.also { it.start() }
    }

    private fun stopVpn() {
        running = false
        active = false
        try { tun?.close() } catch (_: Exception) {}
        tun = null
    }

    private fun loop(servers: List<String>) {
        val fd = tun ?: return
        val input = FileInputStream(fd.fileDescriptor)
        val output = FileOutputStream(fd.fileDescriptor)
        val buf = ByteArray(32767)
        try {
            while (running) {
                val n = input.read(buf)
                if (n <= 0) { Thread.sleep(20); continue }
                if ((buf[0].toInt() shr 4) != 4) continue
                val ihl = (buf[0].toInt() and 0x0F) * 4
                if (n < ihl + 8 || buf[9].toInt() != 17) continue
                val dstPort = ((buf[ihl + 2].toInt() and 0xFF) shl 8) or (buf[ihl + 3].toInt() and 0xFF)
                if (dstPort != 53) continue
                val pkt = buf.copyOf(n)
                pool.execute { handle(pkt, ihl, servers, output) }
            }
        } catch (_: Exception) {
        }
    }

    private fun handle(pkt: ByteArray, ihl: Int, servers: List<String>, out: FileOutputStream) {
        val query = pkt.copyOfRange(ihl + 8, pkt.size)
        var answer: ByteArray? = null
        for (s in servers) {
            try {
                DatagramSocket().use { sock ->
                    protect(sock)
                    sock.soTimeout = 3000
                    sock.send(DatagramPacket(query, query.size, InetAddress.getByName(s), 53))
                    val r = ByteArray(4096)
                    val p = DatagramPacket(r, r.size)
                    sock.receive(p)
                    answer = r.copyOf(p.length)
                }
                break
            } catch (_: Exception) {}
        }
        val a = answer ?: return
        val total = 20 + 8 + a.size
        val o = ByteArray(total)
        o[0] = 0x45; o[2] = (total shr 8).toByte(); o[3] = total.toByte()
        o[8] = 64; o[9] = 17
        // src = ip مقصد اصلی، dst = ip مبدا اصلی
        System.arraycopy(pkt, 16, o, 12, 4)
        System.arraycopy(pkt, 12, o, 16, 4)
        var sum = 0
        for (i in 0 until 20 step 2) sum += ((o[i].toInt() and 0xFF) shl 8) or (o[i + 1].toInt() and 0xFF)
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        val ck = sum.inv() and 0xFFFF
        o[10] = (ck shr 8).toByte(); o[11] = ck.toByte()
        // پورت‌ها جابه‌جا
        o[20] = pkt[ihl + 2]; o[21] = pkt[ihl + 3]
        o[22] = pkt[ihl]; o[23] = pkt[ihl + 1]
        val ul = 8 + a.size
        o[24] = (ul shr 8).toByte(); o[25] = ul.toByte()
        System.arraycopy(a, 0, o, 28, a.size)
        synchronized(out) { try { out.write(o) } catch (_: Exception) {} }
    }

    override fun onRevoke() { stopVpn(); super.onRevoke() }
    override fun onDestroy() { stopVpn(); pool.shutdownNow(); super.onDestroy() }
}
