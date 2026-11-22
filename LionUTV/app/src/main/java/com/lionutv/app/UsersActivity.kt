package com.lionutv.app

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ListView
import java.text.DateFormat
import java.util.Date

/** Admin only: create / disable / renew / delete user access codes. */
class UsersActivity : Activity() {
    private lateinit var listView: ListView
    private lateinit var adapter: TextAdapter
    private var users: List<AppUser> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (requireAdmin() == null) return

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(48), dp(28), dp(48), dp(28))
        }
        root.addView(label(this, t("Manage Users", "إدارة المستخدمين"), 30f, C.AMBER, true))
        root.addView(label(this, t("Each user signs in with their own code.", "كل مستخدم يدخل بكوده الخاص."), 16f, C.MUTED).apply {
            setPadding(0, dp(2), 0, dp(12))
        })
        root.addView(button(this, "+  " + t("Add user", "إضافة مستخدم"), 20f) { addUser() }, lp(dp(300), WRAP).apply { bottomMargin = dp(12) })

        adapter = TextAdapter(this, emptyList())
        listView = ListView(this).apply {
            adapter = this@UsersActivity.adapter
            divider = null
            selector = ColorDrawable(Color.TRANSPARENT)
            setOnItemClickListener { _, _, pos, _ -> manage(users[pos]) }
        }
        root.addView(listView, lp(MATCH, 0, 1f))
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) refresh()
    }

    private fun status(u: AppUser): String {
        val now = System.currentTimeMillis()
        return when {
            !u.enabled -> t("DISABLED", "معطل")
            u.expiresAt != 0L && u.expiresAt <= now -> t("EXPIRED", "منتهي")
            u.expiresAt == 0L -> t("active · no expiry", "فعال · بدون انتهاء")
            else -> t("active · until ", "فعال · حتى ") + DateFormat.getDateInstance().format(Date(u.expiresAt))
        }
    }

    private fun refresh() {
        users = Store.users()
        adapter.items = users.map { "${it.name}   ·   ${it.code}   ·   ${status(it)}" }
        adapter.notifyDataSetChanged()
    }

    private fun addUser() {
        form(
            this, t("Add user", "إضافة مستخدم"),
            listOf(t("Name", "الاسم"), t("Code (4+ digits)", "الكود (4 أرقام+)"), t("Days valid (0 = unlimited)", "عدد الأيام (0 = غير محدود)")),
            listOf("", Store.randomCode(), "30"),
            listOf(false, true, true)
        ) { v ->
            val err = Store.addUser(v[0], v[1], v[2].toIntOrNull() ?: 0)
            if (err != null) toast(this, err) else toast(this, t("User added. Code: ", "تمت الإضافة. الكود: ") + v[1])
            refresh()
        }
    }

    private fun manage(u: AppUser) {
        val options = arrayOf(
            if (u.enabled) t("Disable", "تعطيل") else t("Enable", "تفعيل"),
            t("Renew +30 days", "تجديد +30 يوم"),
            t("Make unlimited", "جعله غير محدود"),
            t("Delete", "حذف")
        )
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("${u.name} · ${u.code}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> Store.toggleUser(u.code)
                    1 -> Store.renewUser(u.code, 30)
                    2 -> Store.renewUser(u.code, 0)
                    3 -> confirm(this, t("Delete this user?", "حذف هذا المستخدم؟")) {
                        Store.removeUser(u.code)
                        refresh()
                    }
                }
                refresh()
            }
            .show()
    }
}
