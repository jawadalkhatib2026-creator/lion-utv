package com.lionutv.app

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/** Full-screen player. UP/DOWN (or channel +/-) zap, OK shows info, LEFT/RIGHT seek movies. */
class PlayerActivity : Activity() {
    private var player: ExoPlayer? = null
    private lateinit var pv: PlayerView
    private lateinit var overlay: TextView
    private lateinit var status: TextView
    private var index = 0
    private var retries = 0
    private val handler = Handler(Looper.getMainLooper())
    private val hideOverlay = Runnable { overlay.visibility = View.GONE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Store.current() == null || Nav.list.isEmpty()) {
            finish()
            return
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        index = intent.getIntExtra("index", 0).coerceIn(0, Nav.list.size - 1)

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        pv = PlayerView(this).apply {
            useController = false
            setShutterBackgroundColor(Color.BLACK)
            resizeMode = Store.resize()
        }
        root.addView(pv, FrameLayout.LayoutParams(MATCH, MATCH))

        overlay = TextView(this).apply {
            textSize = 24f
            setTextColor(C.CREAM)
            background = scrim(this@PlayerActivity, 14, 0xCC140B06.toInt())
            setPadding(dp(24), dp(14), dp(24), dp(14))
        }
        root.addView(overlay, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM or Gravity.START).apply {
            setMargins(dp(40), 0, 0, dp(40))
        })

        status = TextView(this).apply {
            textSize = 22f
            setTextColor(C.AMBER)
            setBackgroundColor(0x99000000.toInt())
            setPadding(dp(18), dp(10), dp(18), dp(10))
            visibility = View.GONE
        }
        root.addView(status, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
        setContentView(root)
    }

    override fun onStart() {
        super.onStart()
        if (!::pv.isInitialized) return
        val p = buildPlayer(this)
        p.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> showStatus(t("Loading...", "جاري التحميل..."))
                    Player.STATE_READY -> {
                        status.visibility = View.GONE
                        retries = 0
                    }
                    else -> {}
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (retries < 3) {
                    retries++
                    showStatus(t("Reconnecting...", "إعادة الاتصال..."))
                    handler.postDelayed({ playCurrent() }, 2500)
                } else {
                    showStatus(t("Stream unavailable. Press UP/DOWN for another channel.", "البث غير متاح. اضغط للأعلى/الأسفل لقناة أخرى."))
                }
            }
        })
        player = p
        pv.player = p
        playCurrent()
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacksAndMessages(null)
        if (::pv.isInitialized) pv.player = null
        player?.release()
        player = null
    }

    private fun showStatus(s: String) {
        status.text = s
        status.visibility = View.VISIBLE
    }

    private fun current(): Channel = Nav.list[index]

    private fun playCurrent() {
        val ch = current()
        val p = player ?: return
        p.setMediaItem(MediaItem.fromUri(ch.url))
        p.prepare()
        p.playWhenReady = true
        showInfo()
    }

    private fun showInfo() {
        val ch = current()
        overlay.text = "${index + 1}/${Nav.list.size}   ${ch.name}\n${ch.group}"
        overlay.visibility = View.VISIBLE
        handler.removeCallbacks(hideOverlay)
        handler.postDelayed(hideOverlay, 4000)
    }

    private fun zap(delta: Int) {
        val n = Nav.list.size
        index = ((index + delta) % n + n) % n
        retries = 0
        playCurrent()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val isLive = current().type == Store.LIVE
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_PAGE_UP -> {
                zap(-1); return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_PAGE_DOWN -> {
                zap(1); return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                showInfo(); return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (!isLive) player?.let { it.seekTo(maxOf(0L, it.currentPosition - 10_000)) } else showInfo()
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (!isLive) player?.let { it.seekTo(it.currentPosition + 10_000) } else showInfo()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
