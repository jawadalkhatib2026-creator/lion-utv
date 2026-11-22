package com.lionutv.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout

class SplashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.lion_logo)
            adjustViewBounds = true
        }
        root.addView(logo, lp(dp(260), dp(260)))
        root.addView(label(this, "LION UTV", 54f, C.AMBER, true).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.12f
        }, lp(WRAP, WRAP))
        root.addView(label(this, "IPTV", 26f, C.CREAM).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.6f
        }, lp(WRAP, WRAP))
        setContentView(root)

        root.alpha = 0f
        root.animate().alpha(1f).setDuration(900).start()

        Handler(Looper.getMainLooper()).postDelayed({
            val next = if (Store.current() != null) HomeActivity::class.java else LoginActivity::class.java
            startActivity(Intent(this, next))
            finish()
        }, 2000)
    }
}
