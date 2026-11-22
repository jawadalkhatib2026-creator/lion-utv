package com.lionutv.app

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView
import coil.load

class HomeActivity : Activity() {

    override fun onResume() {
        super.onResume()
        val user = Store.current()
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        build(user)
    }

    private fun open(mode: String) {
        startActivity(Intent(this, BrowseActivity::class.java).putExtra("mode", mode))
    }

    private fun build(user: AppUser) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(32), dp(24), dp(32), dp(24))
        }

        // ---- left menu
        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = scrim(this@HomeActivity, 18)
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        val items = listOf(
            t("Live TV", "البث المباشر") to { open(Store.LIVE) },
            t("Movies", "أفلام") to { open(Store.MOVIE) },
            t("Series", "مسلسلات") to { open(Store.SERIES) },
            t("Favorites", "المفضلة") to { open("FAV") },
            t("Settings", "الإعدادات") to { startActivity(Intent(this, SettingsActivity::class.java)) },
            t("Exit", "خروج") to { finishAffinity() }
        )
        var first: View? = null
        for ((text, action) in items) {
            val b = button(this, text, 22f) { action() }
            if (first == null) first = b
            menu.addView(b, lp(dp(230), WRAP).apply { bottomMargin = dp(10) })
        }
        root.addView(menu, lp(WRAP, MATCH))

        // ---- right content
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), 0, 0, 0)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(ImageView(this).apply { setImageResource(R.drawable.lion_logo) }, lp(dp(64), dp(64)))
        header.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
            addView(label(this@HomeActivity, "LION UTV", 30f, C.AMBER, true))
            val role = if (Store.isAdmin(user)) t("Admin", "مدير") else user.name
            addView(label(this@HomeActivity, t("Welcome, ", "أهلاً، ") + role, 16f, C.MUTED))
        }, lp(0, WRAP, 1f))
        header.addView(TextClock(this).apply {
            format12Hour = "EEE, dd MMM | hh:mm a"
            format24Hour = "EEE, dd MMM | HH:mm"
            textSize = 20f
            setTextColor(C.AMBER)
        }, lp(WRAP, WRAP))
        content.addView(header, lp(MATCH, WRAP).apply { bottomMargin = dp(14) })

        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val movies = Store.byType(Store.MOVIE).takeLast(20).reversed()
        val series = Store.byType(Store.SERIES).takeLast(20).reversed()
        if (movies.isNotEmpty()) posterRow(t("New Movies", "أحدث الأفلام"), movies, col)
        if (series.isNotEmpty()) posterRow(t("New Series", "أحدث المسلسلات"), series, col)
        if (movies.isEmpty() && series.isEmpty()) {
            val msg = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = scrim(this@HomeActivity, 18)
                setPadding(dp(30), dp(30), dp(30), dp(30))
            }
            msg.addView(ImageView(this).apply { setImageResource(R.drawable.lion_logo) }, lp(dp(180), dp(180)))
            val hint = if (Store.isAdmin(user))
                t("Add channels: Settings > Add Channels (QR)", "لإضافة قنوات: الإعدادات > إضافة قنوات (QR)")
            else
                t("Open Live TV to start watching", "افتح البث المباشر للمشاهدة")
            msg.addView(label(this, hint, 20f, C.CREAM).apply { gravity = Gravity.CENTER })
            col.addView(msg, lp(MATCH, dp(340)))
        }
        content.addView(ScrollView(this).apply { addView(col) }, lp(MATCH, 0, 1f))

        root.addView(content, lp(0, MATCH, 1f))
        setContentView(root)
        first?.requestFocus()
    }

    private fun posterRow(title: String, items: List<Channel>, parent: LinearLayout) {
        parent.addView(label(this, title, 22f, C.AMBER, true).apply { setPadding(dp(4), dp(6), 0, dp(6)) })
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        items.forEachIndexed { index, ch ->
            val card = FrameLayout(this).apply {
                background = focusBg(this@HomeActivity, 10)
                setPadding(dp(3), dp(3), dp(3), dp(3))
                isFocusable = true
                isClickable = true
                focusScale()
                setOnClickListener {
                    Nav.list = items
                    startActivity(Intent(this@HomeActivity, PlayerActivity::class.java).putExtra("index", index))
                }
            }
            val img = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(0xFF2A1A0F.toInt())
                if (ch.logo.isNotEmpty()) {
                    load(ch.logo) { error(R.drawable.lion_logo) }
                } else {
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    setImageResource(R.drawable.lion_logo)
                }
            }
            card.addView(img, FrameLayout.LayoutParams(dp(150), dp(210)))
            val cap = TextView(this).apply {
                text = ch.name
                textSize = 14f
                setTextColor(C.CREAM)
                setSingleLine(true)
                setPadding(dp(8), dp(6), dp(8), dp(6))
                setBackgroundColor(0xAA140B06.toInt())
            }
            card.addView(cap, FrameLayout.LayoutParams(dp(150), WRAP, Gravity.BOTTOM))
            row.addView(card, lp(WRAP, WRAP).apply { rightMargin = dp(12) })
        }
        parent.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        })
    }
}
