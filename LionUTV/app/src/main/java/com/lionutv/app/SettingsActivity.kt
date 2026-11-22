package com.lionutv.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.DateFormat
import java.util.Date

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Store.current() == null) {
            finish()
            return
        }
    }

    override fun onResume() {
        super.onResume()
        val user = Store.current()
        if (user == null) {
            finish()
            return
        }
        build(user)
    }

    private fun build(user: AppUser) {
        val admin = Store.isAdmin(user)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(40), dp(28), dp(40), dp(28))
        }
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        head.addView(label(this, t("Settings", "الإعدادات"), 32f, C.AMBER, true))
        if (admin) {
            head.addView(label(this, "   " + t("Admin mode", "وضع المدير"), 22f, C.GREEN, true).apply { gravity = Gravity.BOTTOM })
        }
        root.addView(head, lp(MATCH, WRAP).apply { bottomMargin = dp(16) })

        val grid = GridLayout(this).apply { columnCount = 4 }
        val tiles = ArrayList<Pair<String, () -> Unit>>()

        tiles.add(t("Account", "الحساب") to { showAccount(user) })
        tiles.add(t("Player: ", "المشغل: ") + resizeName() to {
            val next = when (Store.resize()) {
                0 -> 3
                3 -> 4
                else -> 0
            }
            Store.setResize(next)
            build(user)
        })
        tiles.add(t("Language: English", "اللغة: العربية") to {
            Store.setLang(if (Store.lang() == "ar") "en" else "ar")
            build(user)
        })
        tiles.add(t("About", "حول") to {
            AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Lion UTV")
                .setMessage("Lion UTV 1.0\n" + t("Channels: ", "القنوات: ") + Store.count())
                .setPositiveButton("OK", null).show()
        })

        if (admin) {
            tiles.add(t("Add Channels (QR)", "إضافة قنوات (QR)") to {
                startActivity(Intent(this, AdminQrActivity::class.java))
            })
            tiles.add(t("Manage Users", "إدارة المستخدمين") to {
                startActivity(Intent(this, UsersActivity::class.java))
            })
            tiles.add(t("Manage Channels", "إدارة القنوات") to {
                startActivity(Intent(this, ChannelsAdminActivity::class.java))
            })
            tiles.add(t("Change Admin Code", "تغيير كود المدير") to { changeAdminCode() })
        }

        var first: View? = null
        for ((text, action) in tiles) {
            val tile = TextView(this).apply {
                this.text = text
                textSize = 20f
                gravity = Gravity.CENTER
                setTextColor(focusTextColors())
                background = focusBg(this@SettingsActivity, 14)
                isFocusable = true
                isClickable = true
                setPadding(dp(10), dp(10), dp(10), dp(10))
                focusScale(1.06f)
                setOnClickListener { action() }
            }
            if (first == null) first = tile
            val params = GridLayout.LayoutParams().apply {
                width = dp(220)
                height = dp(120)
                setMargins(dp(8), dp(8), dp(8), dp(8))
            }
            grid.addView(tile, params)
        }
        root.addView(ScrollView(this).apply { addView(grid) }, lp(MATCH, 0, 1f))
        setContentView(root)
        first?.requestFocus()
    }

    private fun resizeName(): String = when (Store.resize()) {
        3 -> t("Fill", "ملء")
        4 -> t("Zoom", "تكبير")
        else -> t("Fit", "ملائم")
    }

    private fun showAccount(user: AppUser) {
        val exp = if (user.expiresAt == 0L) t("Never", "غير محدود")
        else DateFormat.getDateInstance().format(Date(user.expiresAt))
        val role = if (Store.isAdmin(user)) t("Admin", "مدير") else t("User", "مستخدم")
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(t("Account", "الحساب"))
            .setMessage("${user.name}\n$role\n" + t("Expires: ", "ينتهي: ") + exp)
            .setPositiveButton(t("Sign out", "تسجيل خروج")) { _, _ ->
                Store.signOut()
                startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            }
            .setNegativeButton(t("Close", "إغلاق"), null)
            .show()
    }

    private fun changeAdminCode() {
        form(this, t("Change Admin Code", "تغيير كود المدير"), listOf(t("New admin code (4+ digits)", "كود المدير الجديد (4 أرقام+)")), listOf(""), listOf(true)) { v ->
            val err = Store.setAdminCode(v[0])
            if (err != null) {
                toast(this, err)
            } else {
                Store.signIn(v[0])
                toast(this, t("Admin code changed", "تم تغيير كود المدير"))
            }
        }
    }
}
