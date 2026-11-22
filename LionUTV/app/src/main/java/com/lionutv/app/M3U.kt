package com.lionutv.app

import java.io.InputStreamReader
import java.io.Reader
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL

object M3U {
    private val attr = Regex("([\\w-]+)=\"([^\"]*)\"")

    private fun classify(url: String, group: String): String {
        val u = url.lowercase()
        val g = group.lowercase()
        return when {
            u.contains("/movie/") || g.contains("movie") || g.contains("film") || g.contains("vod") || g.contains("أفلام") || g.contains("افلام") -> Store.MOVIE
            u.contains("/series/") || g.contains("series") || g.contains("مسلسل") -> Store.SERIES
            else -> Store.LIVE
        }
    }

    fun parse(reader: Reader): List<Channel> {
        val out = ArrayList<Channel>()
        var name = ""
        var logo = ""
        var group = ""
        var pending = false
        reader.buffered().forEachLine { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEachLine
            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                pending = true
                val attrs = HashMap<String, String>()
                for (m in attr.findAll(line)) attrs[m.groupValues[1].lowercase()] = m.groupValues[2]
                logo = attrs["tvg-logo"] ?: ""
                group = attrs["group-title"] ?: ""
                name = line.substringAfterLast(',', "").trim()
                if (name.isEmpty()) name = attrs["tvg-name"] ?: ""
            } else if (line.startsWith("#EXTGRP:", ignoreCase = true)) {
                if (group.isEmpty()) group = line.substringAfter(':').trim()
            } else if (!line.startsWith("#")) {
                val isStream = line.startsWith("http", true) || line.startsWith("rtmp", true) ||
                        line.startsWith("rtsp", true) || line.startsWith("udp", true)
                if (pending && isStream) {
                    val g = if (group.isEmpty()) "Other" else group
                    val n = if (name.isEmpty()) "Channel ${out.size + 1}" else name
                    out.add(Channel(newId(), n, line, logo, g, classify(line, g)))
                }
                pending = false
                name = ""
                logo = ""
                group = ""
            }
        }
        return out
    }
}

object Importer {
    /** Downloads an M3U playlist and adds its channels. Must run off the main thread. */
    fun fromUrl(url: String): Int {
        val conn = URL(url.trim()).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 60000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "VLC/3.0.18 LibVLC/3.0.18")
        try {
            val list = InputStreamReader(conn.inputStream, Charsets.UTF_8).use { M3U.parse(it) }
            return Store.addAll(list)
        } finally {
            conn.disconnect()
        }
    }

    fun fromText(text: String): Int = Store.addAll(M3U.parse(StringReader(text)))
}
