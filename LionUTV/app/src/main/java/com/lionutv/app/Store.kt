package com.lionutv.app

import android.app.Application
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.UUID

class LionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
    }
}

data class Channel(
    val id: String,
    val name: String,
    val url: String,
    val logo: String,
    val group: String,
    val type: String
)

data class AppUser(
    val name: String,
    val code: String,
    val enabled: Boolean,
    val expiresAt: Long // 0 = never expires
)

/** Simple bilingual helper (English / Arabic). */
fun t(en: String, ar: String): String = if (Store.lang() == "ar") ar else en

fun newId(): String = UUID.randomUUID().toString()

object Store {
    const val LIVE = "LIVE"
    const val MOVIE = "MOVIE"
    const val SERIES = "SERIES"
    const val DEFAULT_ADMIN_CODE = "112233"

    private lateinit var app: Context
    private var inited = false
    private val channels = ArrayList<Channel>()

    private fun prefs() = app.getSharedPreferences("lion_store", Context.MODE_PRIVATE)
    private fun channelFile() = File(app.filesDir, "channels.json")

    @Synchronized
    fun init(ctx: Context) {
        if (inited) return
        app = ctx.applicationContext
        loadChannels()
        inited = true
    }

    // ---------------------------------------------------------------- channels

    private fun loadChannels() {
        channels.clear()
        val f = channelFile()
        if (!f.exists()) {
            // One public test stream so the player can be tried right away.
            channels.add(
                Channel(
                    newId(), "Lion UTV Demo (test stream)",
                    "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
                    "", "Demo", LIVE
                )
            )
            saveChannels()
            return
        }
        try {
            val arr = JSONArray(f.readText())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                channels.add(
                    Channel(
                        o.optString("id", newId()), o.optString("name"), o.optString("url"),
                        o.optString("logo"), o.optString("group", "Other"), o.optString("type", LIVE)
                    )
                )
            }
        } catch (e: Exception) {
            // corrupted file: start empty
        }
    }

    private fun saveChannels() {
        val arr = JSONArray()
        for (c in channels) {
            arr.put(
                JSONObject().put("id", c.id).put("name", c.name).put("url", c.url)
                    .put("logo", c.logo).put("group", c.group).put("type", c.type)
            )
        }
        channelFile().writeText(arr.toString())
    }

    @Synchronized
    fun all(): List<Channel> = ArrayList(channels)

    @Synchronized
    fun byType(type: String): List<Channel> = channels.filter { it.type == type }

    @Synchronized
    fun count(): Int = channels.size

    @Synchronized
    fun addAll(list: List<Channel>): Int {
        val seen = HashSet<String>()
        for (c in channels) seen.add(c.url)
        var n = 0
        for (c in list) {
            if (seen.add(c.url)) {
                channels.add(c)
                n++
            }
        }
        if (n > 0) saveChannels()
        return n
    }

    @Synchronized
    fun add(c: Channel): Boolean = addAll(listOf(c)) > 0

    @Synchronized
    fun remove(id: String) {
        if (channels.removeAll { it.id == id }) saveChannels()
    }

    @Synchronized
    fun clearChannels() {
        channels.clear()
        saveChannels()
    }

    // ------------------------------------------------------------------- users

    @Synchronized
    fun users(): List<AppUser> {
        val out = ArrayList<AppUser>()
        try {
            val arr = JSONArray(prefs().getString("users", "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(AppUser(o.optString("name"), o.optString("code"), o.optBoolean("enabled", true), o.optLong("expiresAt", 0L)))
            }
        } catch (e: Exception) {
        }
        return out
    }

    private fun saveUsers(list: List<AppUser>) {
        val arr = JSONArray()
        for (u in list) {
            arr.put(JSONObject().put("name", u.name).put("code", u.code).put("enabled", u.enabled).put("expiresAt", u.expiresAt))
        }
        prefs().edit().putString("users", arr.toString()).apply()
    }

    fun adminCode(): String = prefs().getString("admin_code", DEFAULT_ADMIN_CODE) ?: DEFAULT_ADMIN_CODE

    /** Returns an error message, or null on success. */
    @Synchronized
    fun setAdminCode(code: String): String? {
        val c = code.trim()
        if (c.length < 4) return t("Code must be at least 4 digits", "الكود يجب أن يكون 4 أرقام على الأقل")
        if (users().any { it.code == c }) return t("Code already used by a user", "الكود مستخدم من قبل مستخدم")
        prefs().edit().putString("admin_code", c).apply()
        return null
    }

    @Synchronized
    fun randomCode(): String {
        val used = users().map { it.code }.toHashSet()
        used.add(adminCode())
        var c: String
        do {
            c = (100000 + java.util.Random().nextInt(900000)).toString()
        } while (used.contains(c))
        return c
    }

    /** Returns an error message, or null on success. */
    @Synchronized
    fun addUser(name: String, code: String, days: Int): String? {
        val c = code.trim()
        if (c.length < 4) return t("Code must be at least 4 digits", "الكود يجب أن يكون 4 أرقام على الأقل")
        if (c == adminCode() || users().any { it.code == c }) return t("Code already used", "الكود مستخدم من قبل")
        val exp = if (days > 0) System.currentTimeMillis() + days * 86_400_000L else 0L
        saveUsers(users() + AppUser(name.ifEmpty { "User" }, c, true, exp))
        return null
    }

    @Synchronized
    fun removeUser(code: String) {
        saveUsers(users().filter { it.code != code })
        if (prefs().getString("session", null) == code) signOut()
    }

    @Synchronized
    fun toggleUser(code: String) {
        saveUsers(users().map { if (it.code == code) it.copy(enabled = !it.enabled) else it })
    }

    @Synchronized
    fun renewUser(code: String, days: Int) {
        val now = System.currentTimeMillis()
        saveUsers(users().map {
            if (it.code == code) {
                val base = if (it.expiresAt > now) it.expiresAt else now
                it.copy(expiresAt = if (days > 0) base + days * 86_400_000L else 0L)
            } else it
        })
    }

    // ---------------------------------------------------------------- sessions

    fun authenticate(code: String): AppUser? {
        val c = code.trim()
        if (c.isEmpty()) return null
        if (c == adminCode()) return AppUser("Admin", c, true, 0L)
        val now = System.currentTimeMillis()
        return users().firstOrNull { it.code == c && it.enabled && (it.expiresAt == 0L || it.expiresAt > now) }
    }

    fun signIn(code: String): AppUser? {
        val u = authenticate(code)
        if (u != null) prefs().edit().putString("session", u.code).apply()
        return u
    }

    fun current(): AppUser? {
        val c = prefs().getString("session", null) ?: return null
        return authenticate(c)
    }

    fun isAdmin(u: AppUser?): Boolean = u != null && u.code == adminCode()

    fun signOut() {
        prefs().edit().remove("session").apply()
    }

    // -------------------------------------------------------------- favorites

    fun favs(code: String): Set<String> = prefs().getStringSet("fav_$code", emptySet()) ?: emptySet()

    fun toggleFav(code: String, id: String): Boolean {
        val s = HashSet(favs(code))
        val on: Boolean
        if (s.contains(id)) {
            s.remove(id)
            on = false
        } else {
            s.add(id)
            on = true
        }
        prefs().edit().putStringSet("fav_$code", s).apply()
        return on
    }

    // ---------------------------------------------------------------- settings

    fun lang(): String =
        prefs().getString("lang", null) ?: if (Locale.getDefault().language == "ar") "ar" else "en"

    fun setLang(l: String) {
        prefs().edit().putString("lang", l).apply()
    }

    /** 0 = fit, 3 = fill, 4 = zoom (AspectRatioFrameLayout resize modes). */
    fun resize(): Int = prefs().getInt("resize", 0)

    fun setResize(m: Int) {
        prefs().edit().putInt("resize", m).apply()
    }
}

/** Channel list handed from a list screen to the full-screen player. */
object Nav {
    var list: List<Channel> = emptyList()
}
