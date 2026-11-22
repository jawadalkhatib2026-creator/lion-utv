package com.lionutv.app

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ListView

/** Admin only: add, import and delete channels directly on the TV. */
class ChannelsAdminActivity : Activity() {
    private lateinit var adapter: TextAdapter
    private lateinit var titleTv: android.widget.TextView
    private var channels: List<Channel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (requireAdmin() == null) return

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(48), dp(28), dp(48), dp(28))
        }
        titleTv = label(this, "", 30f, C.AMBER, true)
        root.addView(titleTv)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, dp(12))
        }
        val add = button(this, "+ " + t("Add channel", "إضافة قناة"), 18f) { addChannel() }
        val imp = button(this, t("Import M3U link", "استيراد رابط M3U"), 18f) { importM3u() }
        val free = button(this, t("Free public channels", "قنوات عامة مجانية"), 18f) { importFree() }
        val clear = button(this, t("Delete all", "حذف الكل"), 18f) {
            confirm(this, t("Delete ALL channels?", "حذف كل القنوات؟")) {
                Store.clearChannels()
                refresh()
            }
        }
        for (b in listOf(add, imp, free, clear)) row.addView(b, lp(WRAP, WRAP).apply { rightMargin = dp(10) })
        root.addView(row)

        adapter = TextAdapter(this, emptyList())
        val list = ListView(this).apply {
            adapter = this@ChannelsAdminActivity.adapter
            divider = null
            selector = ColorDrawable(Color.TRANSPARENT)
            setOnItemClickListener { _, _, pos, _ ->
                val c = channels[pos]
                confirm(this@ChannelsAdminActivity, t("Delete \"", "حذف \"") + c.name + "\"?") {
                    Store.remove(c.id)
                    refresh()
                }
            }
        }
        root.addView(list, lp(MATCH, 0, 1f))
        setContentView(root)
        refresh()
    }

    private fun refresh() {
        channels = Store.all()
        adapter.items = channels.map { "${it.name}   ·   ${it.group}   ·   ${it.type}" }
        adapter.notifyDataSetChanged()
        titleTv.text = t("Manage Channels", "إدارة القنوات") + "  (" + channels.size + ")"
    }

    private fun addChannel() {
        form(
            this, t("Add channel", "إضافة قناة"),
            listOf(t("Name", "الاسم"), t("Stream URL", "رابط البث"), t("Category", "التصنيف"), t("Logo URL (optional)", "رابط الشعار (اختياري)"), "Type: LIVE / MOVIE / SERIES"),
            listOf("", "", "Other", "", "LIVE"),
            listOf(false, false, false, false, false)
        ) { v ->
            if (v[1].isEmpty()) {
                toast(this, t("Stream URL is required", "رابط البث مطلوب"))
                return@form
            }
            val type = when (v[4].uppercase()) {
                "MOVIE" -> Store.MOVIE
                "SERIES" -> Store.SERIES
                else -> Store.LIVE
            }
            Store.add(Channel(newId(), v[0].ifEmpty { "Channel" }, v[1], v[3], v[2].ifEmpty { "Other" }, type))
            refresh()
        }
    }

    private fun importFree() {
        val base = "https://iptv-org.github.io/iptv/"
        val names = arrayOf(
            t("Everything (large)", "كل القنوات (كبيرة)"),
            t("Arabic channels", "قنوات عربية"),
            t("Jordan", "الأردن"),
            t("Movies", "أفلام"),
            t("Series", "مسلسلات"),
            t("Sports", "رياضة"),
            t("News", "أخبار"),
            t("Kids", "أطفال")
        )
        val urls = arrayOf(
            base + "index.m3u", base + "languages/ara.m3u", base + "countries/jo.m3u",
            base + "categories/movies.m3u", base + "categories/series.m3u",
            base + "categories/sports.m3u", base + "categories/news.m3u", base + "categories/kids.m3u"
        )
        android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(t("Free public channels", "قنوات عامة مجانية"))
            .setItems(names) { _, which -> runImport(urls[which]) }
            .show()
    }

    private fun runImport(url: String) {
        toast(this, t("Importing...", "جاري الاستيراد..."))
        Thread {
            val msg = try {
                val n = Importer.fromUrl(url)
                t("Added $n channels", "تمت إضافة $n قناة")
            } catch (e: Exception) {
                t("Import failed: ", "فشل الاستيراد: ") + (e.message ?: "")
            }
            runOnUiThread {
                toast(this, msg)
                refresh()
            }
        }.start()
    }

    private fun importM3u() {
        form(this, t("Import M3U link", "استيراد رابط M3U"), listOf("http://.../playlist.m3u"), listOf(""), listOf(false)) { v ->
            if (v[0].isEmpty()) return@form
            toast(this, t("Importing...", "جاري الاستيراد..."))
            Thread {
                val msg = try {
                    val n = Importer.fromUrl(v[0])
                    t("Added $n channels", "تمت إضافة $n قناة")
                } catch (e: Exception) {
                    t("Import failed: ", "فشل الاستيراد: ") + (e.message ?: "")
                }
                runOnUiThread {
                    toast(this, msg)
                    refresh()
                }
            }.start()
        }
    }
}
