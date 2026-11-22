package com.lionutv.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import fi.iki.elonen.NanoHTTPD
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Admin only. Shows a QR code. Scan it with a phone (same Wi-Fi), enter the admin code
 * and add channels / M3U playlists / users from the phone.
 */
class AdminQrActivity : Activity() {
    private var server: AdminServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (requireAdmin() == null) return
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        var port = 0
        for (p in 8080..8090) {
            try {
                val s = AdminServer(this, p)
                s.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
                server = s
                port = p
                break
            } catch (e: Exception) {
                // try next port
            }
        }
        val ip = localIp()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(40), dp(24), dp(40), dp(24))
        }

        val left = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = scrim(this@AdminQrActivity, 20, 0xFFFFF1D6.toInt())
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        val right = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(40), 0, 0, 0)
        }

        if (server != null && ip != null) {
            val url = "http://$ip:$port/"
            left.addView(ImageView(this).apply { setImageBitmap(qr(url, 560)) }, lp(dp(300), dp(300)))
            right.addView(label(this, t("Add channels from your phone", "أضف القنوات من هاتفك"), 30f, C.AMBER, true))
            right.addView(label(this, "1. " + t("Connect the phone to the same Wi-Fi as this TV", "صل الهاتف بنفس شبكة الواي فاي"), 20f).apply { setPadding(0, dp(18), 0, 0) })
            right.addView(label(this, "2. " + t("Scan the QR code with the phone camera", "امسح رمز QR بكاميرا الهاتف"), 20f).apply { setPadding(0, dp(8), 0, 0) })
            right.addView(label(this, "3. " + t("Enter the admin code", "أدخل كود المدير"), 20f).apply { setPadding(0, dp(8), 0, 0) })
            right.addView(label(this, "4. " + t("Add channels, paste an M3U link, manage users", "أضف قنوات أو رابط M3U أو أدر المستخدمين"), 20f).apply { setPadding(0, dp(8), 0, 0) })
            right.addView(label(this, url, 24f, C.GREEN, true).apply { setPadding(0, dp(22), 0, 0) })
            right.addView(label(this, t("Press BACK to close this page", "اضغط رجوع لإغلاق الصفحة"), 16f, C.MUTED).apply { setPadding(0, dp(18), 0, 0) })
        } else {
            left.addView(label(this, "?", 120f, C.BG, true))
            right.addView(label(this, t("Could not start the admin page", "تعذر تشغيل صفحة المدير"), 28f, C.RED, true))
            right.addView(label(this, t("Make sure the TV is connected to Wi-Fi / Ethernet, then try again.", "تأكد أن التلفاز متصل بالشبكة ثم حاول مرة أخرى."), 20f).apply { setPadding(0, dp(14), 0, 0) })
        }
        root.addView(left, lp(WRAP, WRAP))
        root.addView(right, lp(0, WRAP, 1f))
        setContentView(root)
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
        server = null
    }

    private fun qr(text: String, size: Int): Bitmap {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) for (y in 0 until size) {
            bmp.setPixel(x, y, if (m.get(x, y)) 0xFF2A1608.toInt() else Color.WHITE)
        }
        return bmp
    }

    private fun localIp(): String? {
        try {
            val ifs = NetworkInterface.getNetworkInterfaces()
            while (ifs.hasMoreElements()) {
                val ni = ifs.nextElement()
                if (!ni.isUp || ni.isLoopback) continue
                val addrs = ni.inetAddresses
                while (addrs.hasMoreElements()) {
                    val a = addrs.nextElement()
                    if (a is Inet4Address && a.isSiteLocalAddress) return a.hostAddress
                }
            }
        } catch (e: Exception) {
        }
        return null
    }
}
