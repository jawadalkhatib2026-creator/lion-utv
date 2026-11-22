package com.lionutv.app

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Method
import fi.iki.elonen.NanoHTTPD.Response
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Tiny web server that runs only while the admin QR screen is open.
 * Every API call needs a token obtained with the admin code.
 */
class AdminServer(private val ctx: Context, port: Int) : NanoHTTPD(port) {
    private val tokens = HashSet<String>()
    private var fails = 0
    private var lockUntil = 0L

    override fun serve(session: IHTTPSession): Response {
        return try {
            if (session.method == Method.GET) {
                val html = ctx.assets.open("admin.html").bufferedReader().use { it.readText() }
                NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html)
            } else if (session.method == Method.POST && session.uri.startsWith("/api/")) {
                val files = HashMap<String, String>()
                session.parseBody(files)
                val body = JSONObject(files["postData"] ?: "{}")
                handle(session.uri, body)
            } else {
                err("not found")
            }
        } catch (e: Exception) {
            err(e.message ?: "error")
        }
    }

    private fun json(o: JSONObject): Response =
        NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", o.toString())

    private fun ok(o: JSONObject = JSONObject()): Response = json(o.put("ok", true))
    private fun err(msg: String): Response = json(JSONObject().put("ok", false).put("error", msg))

    private fun handle(uri: String, b: JSONObject): Response {
        if (uri == "/api/login") {
            val now = System.currentTimeMillis()
            if (now < lockUntil) return err("Too many attempts. Wait a minute.")
            if (b.optString("code") == Store.adminCode()) {
                fails = 0
                val tk = UUID.randomUUID().toString()
                synchronized(tokens) { tokens.add(tk) }
                return ok(JSONObject().put("token", tk))
            }
            fails++
            if (fails >= 5) {
                lockUntil = now + 60_000
                fails = 0
            }
            return err("Wrong admin code")
        }

        val tk = b.optString("token")
        val valid = synchronized(tokens) { tokens.contains(tk) }
        if (!valid) return err("auth")

        return when (uri) {
            "/api/state" -> state()
            "/api/addChannel" -> {
                val url = b.optString("url").trim()
                if (url.isEmpty()) return err("Stream URL is required")
                val name = b.optString("name").trim().ifEmpty { "Channel" }
                val group = b.optString("group").trim().ifEmpty { "Other" }
                val type = when (b.optString("type")) {
                    Store.MOVIE -> Store.MOVIE
                    Store.SERIES -> Store.SERIES
                    else -> Store.LIVE
                }
                val added = Store.add(Channel(newId(), name, url, b.optString("logo").trim(), group, type))
                ok(JSONObject().put("added", if (added) 1 else 0))
            }
            "/api/importM3u" -> {
                val url = b.optString("url").trim()
                val text = b.optString("text")
                val n = if (url.isNotEmpty()) Importer.fromUrl(url) else Importer.fromText(text)
                ok(JSONObject().put("added", n))
            }
            "/api/removeChannel" -> {
                Store.remove(b.optString("id"))
                ok()
            }
            "/api/clearChannels" -> {
                Store.clearChannels()
                ok()
            }
            "/api/addUser" -> {
                var code = b.optString("code").trim()
                if (code.isEmpty()) code = Store.randomCode()
                val e = Store.addUser(b.optString("name").trim(), code, b.optInt("days", 0))
                if (e != null) err(e) else ok(JSONObject().put("code", code))
            }
            "/api/toggleUser" -> {
                Store.toggleUser(b.optString("code"))
                ok()
            }
            "/api/removeUser" -> {
                Store.removeUser(b.optString("code"))
                ok()
            }
            "/api/renewUser" -> {
                Store.renewUser(b.optString("code"), b.optInt("days", 30))
                ok()
            }
            "/api/setAdminCode" -> {
                val e = Store.setAdminCode(b.optString("code"))
                if (e != null) err(e) else {
                    synchronized(tokens) { tokens.clear() }
                    ok()
                }
            }
            else -> err("unknown")
        }
    }

    private fun state(): Response {
        val chs = JSONArray()
        for (c in Store.all().take(1500)) {
            chs.put(JSONObject().put("id", c.id).put("name", c.name).put("group", c.group).put("type", c.type))
        }
        val us = JSONArray()
        for (u in Store.users()) {
            us.put(JSONObject().put("name", u.name).put("code", u.code).put("enabled", u.enabled).put("expiresAt", u.expiresAt))
        }
        return ok(JSONObject().put("count", Store.count()).put("channels", chs).put("users", us))
    }
}
