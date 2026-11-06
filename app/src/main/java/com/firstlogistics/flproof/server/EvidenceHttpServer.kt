package com.firstlogistics.flproof.server

import android.content.Context
import android.net.wifi.WifiManager
import com.firstlogistics.flproof.data.entity.EvidenceEntity
import com.firstlogistics.flproof.data.repository.EvidenceRepository
import com.firstlogistics.flproof.utils.StorageManager
import kotlinx.coroutines.*
import java.io.*
import java.net.*
import java.util.Collections

class EvidenceHttpServer(
    private val context: Context,
    private val evidenceRepository: EvidenceRepository,
    private val storageManager: StorageManager,
    var preferredPort: Int = 8080
) {
    var activePort: Int = preferredPort
        private set

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var isRunning: Boolean = false
        private set

    fun start() {
        if (isRunning || serverJob?.isActive == true) return
        serverJob = coroutineScope.launch {
            val candidatePorts = listOf(preferredPort, 8081, 8082, 8888)
            var socketCreated = false

            for (p in candidatePorts) {
                try {
                    val s = ServerSocket().apply {
                        reuseAddress = true
                        bind(InetSocketAddress("0.0.0.0", p))
                    }
                    serverSocket = s
                    activePort = p
                    socketCreated = true
                    break
                } catch (e: Exception) {
                    // Try next port
                }
            }

            if (!socketCreated) {
                try {
                    val s = ServerSocket(0).apply { reuseAddress = true }
                    serverSocket = s
                    activePort = s.localPort
                    socketCreated = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (!socketCreated) {
                isRunning = false
                return@launch
            }

            isRunning = true

            while (isActive) {
                try {
                    val clientSocket = serverSocket?.accept() ?: break
                    launch {
                        handleClient(clientSocket)
                    }
                } catch (e: Exception) {
                    if (!isActive) break
                }
            }
            isRunning = false
        }
    }

    fun stop() {
        try {
            isRunning = false
            serverSocket?.close()
            serverJob?.cancel()
            serverJob = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Mendapatkan alamat IP lokal terbaik untuk diakses oleh HP lain.
     * Prioritas:
     * 1. Interface Wi-Fi / WifiManager aktif (wlan0, wlan1)
     * 2. Interface Hotspot / SoftAP aktif (ap0, softap0, swlan0, 192.168.43.x)
     * 3. IP non-loopback site-local lainnya (mengecualikan interface seluler seperti rmnet/ccmni)
     * 4. Jika offline/tidak ada koneksi, kembalikan default Hotspot Android "192.168.43.1"
     */
    fun getLocalIpAddress(): String {
        try {
            // Cek WifiManager terlebih dahulu
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiIpInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (wifiIpInt != 0) {
                val wifiIp = android.text.format.Formatter.formatIpAddress(wifiIpInt)
                if (!wifiIp.isNullOrBlank() && wifiIp != "0.0.0.0" && wifiIp != "127.0.0.1") {
                    return wifiIp
                }
            }

            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())

            // 1. Cek Wi-Fi client interface (wlan0, eth0)
            for (intf in interfaces) {
                val name = intf.name.lowercase()
                if (name.startsWith("wlan") || name.startsWith("eth") || name.contains("wifi")) {
                    for (addr in Collections.list(intf.inetAddresses)) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val host = addr.hostAddress ?: ""
                            if (host.isNotBlank() && host != "127.0.0.1" && !host.startsWith("169.254.")) {
                                return host
                            }
                        }
                    }
                }
            }

            // 2. Cek AP / Hotspot interfaces
            for (intf in interfaces) {
                val name = intf.name.lowercase()
                if (name.startsWith("ap") || name.startsWith("softap") || name.startsWith("swlan")) {
                    for (addr in Collections.list(intf.inetAddresses)) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val host = addr.hostAddress ?: ""
                            if (host.isNotBlank() && host != "127.0.0.1") {
                                return host
                            }
                        }
                    }
                }
            }

            // 3. Cek subnet privat lainnya yang BUKAN antarmuka seluler
            for (intf in interfaces) {
                val name = intf.name.lowercase()
                // Abaikan seluler carrier CGNAT (rmnet, pdp, ccmni, dummy, tun, ppp)
                if (name.startsWith("rmnet") || name.startsWith("pdp") || name.startsWith("ccmni") ||
                    name.startsWith("dummy") || name.startsWith("tun") || name.startsWith("ppp")) {
                    continue
                }
                for (addr in Collections.list(intf.inetAddresses)) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: ""
                        if ((host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) && !host.startsWith("169.254.")) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Default ke IP gateway standar Hotspot Android jika tidak ada jaringan aktif.
        // Pengguna cukup menyalakan Portable Hotspot dan HP lain menyambung ke Hotspot ini.
        return "192.168.43.1"
    }

    fun getNetworkStatusDescription(): String {
        val ip = getLocalIpAddress()
        return when {
            ip.startsWith("192.168.43.") -> "Mode Hotspot Mandiri ($ip)"
            ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.") -> "Wi-Fi Terhubung ($ip)"
            else -> "Lokal ($ip)"
        }
    }

    fun getSessionUrl(sessionIdOrEvidenceId: String): String {
        val ip = getLocalIpAddress()
        return "http://$ip:$activePort/session?id=$sessionIdOrEvidenceId"
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            socket.soTimeout = 12000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val out = BufferedOutputStream(socket.getOutputStream())

            val requestLine = reader.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0]
            val uri = parts[1]

            if (method != "GET") {
                sendResponse(out, 405, "Method Not Allowed", "text/plain", "Hanya metode GET yang didukung.".toByteArray())
                return@withContext
            }

            val path = if (uri.contains("?")) uri.substringBefore("?") else uri
            val query = if (uri.contains("?")) uri.substringAfter("?") else ""
            val queryParams = parseQuery(query)

            when {
                path == "/session" || path == "/evidence" || path.startsWith("/session") -> {
                    val id = queryParams["id"] ?: ""
                    handleSessionRequest(out, id)
                }
                path == "/image" || path.startsWith("/photo") -> {
                    val id = queryParams["id"] ?: ""
                    handleImageRequest(out, id)
                }
                else -> {
                    handleIndexRequest(out)
                }
            }
        } catch (e: Exception) {
            // Socket timeout or closed
        } finally {
            try { socket.close() } catch (ignored: Exception) {}
        }
    }

    private suspend fun handleSessionRequest(out: OutputStream, id: String) {
        if (id.isBlank()) {
            sendResponse(out, 400, "Bad Request", "text/plain", "Parameter ID diperlukan.".toByteArray())
            return
        }

        // Search for matching session photos first
        var sessionPhotos = evidenceRepository.getSessionPhotosList(id)
        var evidence: EvidenceEntity? = sessionPhotos.firstOrNull()

        if (evidence == null) {
            evidence = evidenceRepository.getByEvidenceId(id) ?: evidenceRepository.findEvidenceByCode(id)
        }

        if (evidence != null && sessionPhotos.isEmpty()) {
            val sessionKey = evidence.sessionId ?: evidence.evidenceId
            sessionPhotos = evidenceRepository.getSessionPhotosList(sessionKey)
            if (sessionPhotos.isEmpty()) {
                sessionPhotos = listOf(evidence)
            }
        }

        if (evidence == null) {
            val notFoundHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>FL PROOF - Tidak Ditemukan</title>
                    <style>
                        body { background:#070D1B; color:#FFF; font-family:-apple-system, sans-serif; padding:30px; text-align:center; }
                        .card { background:#0F1A30; border:1px solid #1E293B; border-radius:12px; padding:24px; max-width:480px; margin:40px auto; }
                        h2 { color:#FF6600; }
                        code { background:rgba(255,255,255,0.1); padding:2px 8px; border-radius:4px; color:#00E676; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h2>Dokumentasi Tidak Ditemukan</h2>
                        <p style="margin:16px 0; color:#94A3B8;">Bukti dokumentasi / ID Sesi <code>$id</code> tidak ditemukan pada sistem lokal FL PROOF.</p>
                        <p style="font-size:12px; color:#64748B;">Pastikan nomor barcode atau ID bukti telah sesuai.</p>
                    </div>
                </body>
                </html>
            """.trimIndent()
            sendResponse(out, 404, "Not Found", "text/html; charset=utf-8", notFoundHtml.toByteArray(Charsets.UTF_8))
            return
        }

        val htmlFile = storageManager.generateHtmlWebViewer(evidence, sessionPhotos)
        val htmlBytes = htmlFile.readBytes()
        sendResponse(out, 200, "OK", "text/html; charset=utf-8", htmlBytes)
    }

    private suspend fun handleImageRequest(out: OutputStream, id: String) {
        val evidence = evidenceRepository.getByEvidenceId(id) ?: evidenceRepository.findEvidenceByCode(id)
        if (evidence == null) {
            sendResponse(out, 404, "Not Found", "text/plain", "Foto tidak ditemukan.".toByteArray())
            return
        }
        val file = File(evidence.watermarkedPhotoPath).let { if (it.exists()) it else File(evidence.originalPhotoPath) }
        if (file.exists()) {
            sendResponse(out, 200, "OK", "image/jpeg", file.readBytes())
        } else {
            sendResponse(out, 404, "Not Found", "text/plain", "File foto fisik tidak ditemukan.".toByteArray())
        }
    }

    private suspend fun handleIndexRequest(out: OutputStream) {
        val totalCount = evidenceRepository.getAllList().size
        val ip = getLocalIpAddress()
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>FL PROOF Evidence Server</title>
                <style>
                    body { background: #070D1B; color: #FFF; font-family: -apple-system, sans-serif; padding: 24px; text-align: center; }
                    .box { background: #0F1A30; border: 1px solid #1E293B; border-radius: 12px; padding: 24px; max-width: 480px; margin: 40px auto; }
                    h1 { color: #00E676; font-size: 20px; }
                    p { color: #94A3B8; font-size: 14px; }
                    .badge { background: rgba(0,230,118,0.15); color: #00E676; border: 1px solid #00E676; padding: 6px 14px; border-radius: 99px; display: inline-block; font-size: 13px; font-weight: bold; margin: 12px 0; }
                    .guide { background: rgba(255,102,0,0.1); border: 1px solid #FF6600; border-radius: 8px; padding: 12px; text-align: left; font-size: 12px; color: #CBD5E1; margin-top: 16px; line-height: 1.6; }
                </style>
            </head>
            <body>
                <div class="box">
                    <h1>FIRST LOGISTICS EVIDENCE SERVER</h1>
                    <p>FL PROOF Enterprise Documentation Hub</p>
                    <div class="badge">● Server Online (http://$ip:$activePort)</div>
                    <p>Total Arsip Bukti Tersimpan: <strong>$totalCount Dokumen</strong></p>
                    <div class="guide">
                        <strong>💡 Panduan Akses HP Lain:</strong><br>
                        1. Pastikan kedua HP terhubung ke Wi-Fi yang sama, ATAU<br>
                        2. Aktifkan Hotspot di HP ini, lalu sambungkan HP lain ke Hotspot ini.<br>
                        3. Scan QR Code pada foto dokumentasi atau buka link bukti secara langsung.
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
        sendResponse(out, 200, "OK", "text/html; charset=utf-8", html.toByteArray(Charsets.UTF_8))
    }

    private fun parseQuery(query: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        if (query.isBlank()) return result
        val pairs = query.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                result[key] = value
            }
        }
        return result
    }

    private fun sendResponse(out: OutputStream, code: Int, reason: String, contentType: String, body: ByteArray) {
        val writer = PrintWriter(OutputStreamWriter(out, Charsets.UTF_8))
        writer.print("HTTP/1.1 $code $reason\r\n")
        writer.print("Content-Type: $contentType\r\n")
        writer.print("Content-Length: ${body.size}\r\n")
        writer.print("Access-Control-Allow-Origin: *\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.flush()
        out.write(body)
        out.flush()
    }
}
