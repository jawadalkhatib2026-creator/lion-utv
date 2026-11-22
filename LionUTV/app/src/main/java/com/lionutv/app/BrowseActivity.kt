package com.lionutv.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.load

/** Live TV / Movies / Series / Favorites: categories | channel list | preview. */
class BrowseActivity : Activity() {
    private lateinit var mode: String
    private lateinit var user: AppUser
    private var base: List<Channel> = emptyList()
    private var groups: List<String> = emptyList()
    private var activeGroup = 0
    private var query = ""
    private var shown: List<Channel> = emptyList()
    private var playingId: String? = null
    private var player: ExoPlayer? = null

    private lateinit var catList: ListView
    private lateinit var chList: ListView
    private lateinit var catAdapter: TextAdapter
    private lateinit var chAdapter: ChannelAdapter
    private lateinit var pv: PlayerView
    private lateinit var nameTv: TextView
    private lateinit var infoTv: TextView
    private lateinit var emptyTv: TextView
    private lateinit var countTv: TextView
    private lateinit var favBtn: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val u = Store.current()
        if (u == null) {
            finish()
            return
        }
        user = u
        mode = intent.getStringExtra("mode") ?: Store.LIVE
        base = if (mode == "FAV") {
            val f = Store.favs(user.code)
            Store.all().filter { f.contains(it.id) }
        } else Store.byType(mode)
        groups = listOf(t("All", "الكل")) + base.map { it.group }.distinct().sorted()

        val title = when (mode) {
            Store.LIVE -> t("Live TV", "البث المباشر")
            Store.MOVIE -> t("Movies", "أفلام")
            Store.SERIES -> t("Series", "مسلسلات")
            else -> t("Favorites", "المفضلة")
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(28), dp(20), dp(28), dp(20))
        }

        // ---- categories
        val left = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = scrim(this@BrowseActivity, 16)
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        left.addView(label(this, title, 24f, C.AMBER, true).apply { setPadding(dp(8), dp(4), 0, dp(8)) })
        catAdapter = TextAdapter(this, groups, 0)
        catList = ListView(this).apply {
            adapter = catAdapter
            divider = null
            selector = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    if (pos != activeGroup) {
                        activeGroup = pos
                        catAdapter.active = pos
                        applyFilter()
                    }
                }

                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
            setOnItemClickListener { _, _, pos, _ ->
                activeGroup = pos
                catAdapter.active = pos
                applyFilter()
                chList.requestFocus()
            }
        }
        left.addView(catList, lp(MATCH, 0, 1f))
        root.addView(left, lp(dp(240), MATCH))

        // ---- channel list
        val mid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(4), dp(16), 0)
        }
        countTv = label(this, "", 16f, C.MUTED).apply { setPadding(dp(8), 0, 0, dp(6)) }
        mid.addView(countTv)
        chAdapter = ChannelAdapter(this)
        chList = ListView(this).apply {
            adapter = chAdapter
            divider = null
            selector = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            setOnItemClickListener { _, _, pos, _ ->
                val ch = shown[pos]
                if (playingId == ch.id) openFullscreen() else play(ch)
            }
        }
        emptyTv = label(
            this,
            t("No channels here yet.\nThe admin can add them from Settings > Add Channels (QR).",
                "لا توجد قنوات بعد.\nيمكن للمدير إضافتها من الإعدادات > إضافة قنوات (QR)."),
            18f, C.MUTED
        ).apply { gravity = Gravity.CENTER; setPadding(dp(20), dp(40), dp(20), 0) }
        val frame = FrameLayout(this)
        frame.addView(chList, FrameLayout.LayoutParams(MATCH, MATCH))
        frame.addView(emptyTv, FrameLayout.LayoutParams(MATCH, WRAP))
        mid.addView(frame, lp(MATCH, 0, 1f))
        root.addView(mid, lp(0, MATCH, 1f))

        // ---- preview
        val right = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = scrim(this@BrowseActivity, 16)
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        pv = PlayerView(this).apply {
            useController = false
            setShutterBackgroundColor(Color.BLACK)
            resizeMode = Store.resize()
        }
        right.addView(pv, lp(dp(420), dp(236)))
        nameTv = label(this, t("Select a channel", "اختر قناة"), 22f, C.CREAM, true).apply {
            setPadding(0, dp(12), 0, dp(2))
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }
        infoTv = label(this, t("Press OK to preview, OK again for full screen", "اضغط OK للمعاينة وOK مرة أخرى لملء الشاشة"), 15f, C.MUTED)
        right.addView(nameTv)
        right.addView(infoTv)

        val btns = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val fsBtn = button(this, t("Full", "ملء"), 16f) { openFullscreen() }.apply { gravity = Gravity.CENTER }
        favBtn = button(this, "★", 18f) { toggleFav() }.apply { gravity = Gravity.CENTER }
        val searchBtn = button(this, t("Search", "بحث"), 16f) { search() }.apply { gravity = Gravity.CENTER }
        val reloadBtn = button(this, t("Reload", "تحديث"), 16f) { reload() }.apply { gravity = Gravity.CENTER }
        for (b in listOf(fsBtn, favBtn, searchBtn, reloadBtn)) {
            btns.addView(b, lp(0, WRAP, 1f).apply { rightMargin = dp(6) })
        }
        right.addView(btns, lp(MATCH, WRAP).apply { topMargin = dp(14) })
        root.addView(right, lp(dp(450), MATCH))

        setContentView(root)
        applyFilter()
        if (shown.isNotEmpty()) chList.requestFocus() else catList.requestFocus()
    }

    override fun onStart() {
        super.onStart()
        if (!::pv.isInitialized) return
        val p = buildPlayer(this)
        p.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                infoTv.text = t("Stream unavailable", "البث غير متاح")
            }
        })
        player = p
        pv.player = p
        pv.resizeMode = Store.resize()
        val id = playingId
        if (id != null) base.firstOrNull { it.id == id }?.let { play(it) }
    }

    override fun onStop() {
        super.onStop()
        if (::pv.isInitialized) pv.player = null
        player?.release()
        player = null
    }

    private fun applyFilter() {
        val g = if (activeGroup == 0) null else groups[activeGroup]
        val q = query.lowercase()
        shown = base.filter { (g == null || it.group == g) && (q.isEmpty() || it.name.lowercase().contains(q)) }
        chAdapter.items = shown
        chAdapter.favs = Store.favs(user.code)
        chAdapter.notifyDataSetChanged()
        catAdapter.notifyDataSetChanged()
        emptyTv.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
        countTv.text = shown.size.toString() + (if (query.isNotEmpty()) "  ·  \"$query\"" else "")
    }

    private fun play(ch: Channel) {
        playingId = ch.id
        nameTv.text = ch.name
        infoTv.text = ch.group
        updateFavBtn()
        val p = player ?: return
        p.setMediaItem(MediaItem.fromUri(ch.url))
        p.prepare()
        p.playWhenReady = true
    }

    private fun openFullscreen() {
        val id = playingId ?: return
        val idx = shown.indexOfFirst { it.id == id }
        if (idx < 0) return
        Nav.list = shown
        startActivity(Intent(this, PlayerActivity::class.java).putExtra("index", idx))
    }

    private fun toggleFav() {
        val id = playingId ?: return
        Store.toggleFav(user.code, id)
        chAdapter.favs = Store.favs(user.code)
        chAdapter.notifyDataSetChanged()
        updateFavBtn()
    }

    private fun updateFavBtn() {
        val id = playingId
        val on = id != null && Store.favs(user.code).contains(id)
        favBtn.text = if (on) "★ ✓" else "★"
    }

    private fun search() {
        form(this, t("Search", "بحث"), listOf(t("Channel name", "اسم القناة")), listOf(query), listOf(false)) { v ->
            query = v[0]
            applyFilter()
        }
    }

    private fun reload() {
        val id = playingId ?: return
        base.firstOrNull { it.id == id }?.let { play(it) }
    }
}

class ChannelAdapter(private val ctx: Context) : BaseAdapter() {
    var items: List<Channel> = emptyList()
    var favs: Set<String> = emptySet()

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): Any = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val row = (convertView as? LinearLayout) ?: LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ctx.dp(12), ctx.dp(6), ctx.dp(12), ctx.dp(6))
            val img = ImageView(ctx).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
            addView(img, lp(ctx.dp(44), ctx.dp(44)))
            val tv = TextView(ctx).apply {
                textSize = 18f
                setTextColor(focusTextColors())
                setSingleLine(true)
                ellipsize = TextUtils.TruncateAt.END
                setPadding(ctx.dp(14), 0, 0, 0)
                isDuplicateParentStateEnabled = true
            }
            addView(tv, lp(0, WRAP, 1f))
        }
        val ch = items[position]
        val img = row.getChildAt(0) as ImageView
        val tv = row.getChildAt(1) as TextView
        if (ch.logo.isNotEmpty()) img.load(ch.logo) { error(R.drawable.lion_logo) } else img.setImageResource(R.drawable.lion_logo)
        tv.text = (if (favs.contains(ch.id)) "★ " else "") + ch.name
        row.background = focusBg(ctx, 8)
        return row
    }
}
