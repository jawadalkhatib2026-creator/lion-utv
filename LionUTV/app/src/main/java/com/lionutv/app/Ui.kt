package com.lionutv.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

/** Lion palette */
object C {
    val BG = Color.parseColor("#140B06")
    val PANEL = Color.parseColor("#CC24160C")
    val BROWN = Color.parseColor("#6B3A18")
    val GOLD = Color.parseColor("#E8A317")
    val AMBER = Color.parseColor("#F5B83D")
    val ORANGE = Color.parseColor("#C8651B")
    val CREAM = Color.parseColor("#FFF1D6")
    val MUTED = Color.parseColor("#C9A978")
    val RED = Color.parseColor("#E5484D")
    val GREEN = Color.parseColor("#7BD88F")
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

fun lp(w: Int, h: Int, weight: Float = 0f): LinearLayout.LayoutParams = LinearLayout.LayoutParams(w, h, weight)

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

fun scrim(ctx: Context, radiusDp: Int = 16, color: Int = C.PANEL): GradientDrawable =
    GradientDrawable().apply {
        cornerRadius = ctx.dp(radiusDp).toFloat()
        setColor(color)
    }

fun focusTextColors(): ColorStateList = ColorStateList(
    arrayOf(
        intArrayOf(android.R.attr.state_focused),
        intArrayOf(android.R.attr.state_selected),
        intArrayOf()
    ),
    intArrayOf(C.BG, C.BG, C.CREAM)
)

/** Gold gradient when focused/selected, panel colour otherwise. */
fun focusBg(ctx: Context, radiusDp: Int = 10, active: Boolean = false): StateListDrawable {
    val r = ctx.dp(radiusDp).toFloat()
    val on = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(C.AMBER, C.ORANGE)).apply {
        cornerRadius = r
        setStroke(ctx.dp(2), C.CREAM)
    }
    val off = GradientDrawable().apply {
        cornerRadius = r
        setColor(if (active) C.BROWN else C.PANEL)
        if (active) setStroke(ctx.dp(1), C.GOLD)
    }
    return StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_focused), on)
        addState(intArrayOf(android.R.attr.state_selected), on)
        addState(intArrayOf(android.R.attr.state_pressed), on)
        addState(intArrayOf(), off)
    }
}

fun label(ctx: Context, text: String, sp: Float = 16f, color: Int = C.CREAM, bold: Boolean = false): TextView =
    TextView(ctx).apply {
        this.text = text
        textSize = sp
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

fun button(ctx: Context, text: String, sp: Float = 18f, onClick: () -> Unit): TextView =
    TextView(ctx).apply {
        this.text = text
        textSize = sp
        setTextColor(focusTextColors())
        gravity = Gravity.CENTER_VERTICAL
        setPadding(ctx.dp(18), ctx.dp(12), ctx.dp(18), ctx.dp(12))
        background = focusBg(ctx)
        isFocusable = true
        isClickable = true
        setOnClickListener { onClick() }
    }

fun View.focusScale(f: Float = 1.08f) {
    onFocusChangeListener = View.OnFocusChangeListener { v, has ->
        v.animate().scaleX(if (has) f else 1f).scaleY(if (has) f else 1f).setDuration(120).start()
    }
}

fun toast(ctx: Context, s: String) {
    Toast.makeText(ctx, s, Toast.LENGTH_LONG).show()
}

fun form(
    act: Activity, title: String, labels: List<String>, defaults: List<String>,
    numeric: List<Boolean>, ok: (List<String>) -> Unit
) {
    val box = LinearLayout(act).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(act.dp(24), act.dp(8), act.dp(24), 0)
    }
    val edits = ArrayList<EditText>()
    for (i in labels.indices) {
        val e = EditText(act)
        e.hint = labels[i]
        e.setSingleLine(true)
        e.setText(defaults.getOrElse(i) { "" })
        if (numeric.getOrElse(i) { false }) e.inputType = InputType.TYPE_CLASS_NUMBER
        box.addView(e)
        edits.add(e)
    }
    AlertDialog.Builder(act, android.R.style.Theme_Material_Dialog_Alert)
        .setTitle(title)
        .setView(box)
        .setPositiveButton(t("OK", "موافق")) { _, _ -> ok(edits.map { it.text.toString().trim() }) }
        .setNegativeButton(t("Cancel", "إلغاء"), null)
        .show()
}

fun confirm(act: Activity, msg: String, yes: () -> Unit) {
    AlertDialog.Builder(act, android.R.style.Theme_Material_Dialog_Alert)
        .setMessage(msg)
        .setPositiveButton(t("Yes", "نعم")) { _, _ -> yes() }
        .setNegativeButton(t("No", "لا"), null)
        .show()
}

/** Closes the screen unless the signed-in user is the admin. */
fun Activity.requireAdmin(): AppUser? {
    val u = Store.current()
    if (!Store.isAdmin(u)) {
        finish()
        return null
    }
    return u
}

/** Plain text rows with the lion focus style (used by several screens). */
class TextAdapter(private val ctx: Context, var items: List<String>, var active: Int = -1) : BaseAdapter() {
    override fun getCount(): Int = items.size
    override fun getItem(position: Int): Any = items[position]
    override fun getItemId(position: Int): Long = position.toLong()
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val tv = (convertView as? TextView) ?: TextView(ctx).apply {
            textSize = 18f
            setTextColor(focusTextColors())
            gravity = Gravity.CENTER_VERTICAL
            minHeight = ctx.dp(48)
            setPadding(ctx.dp(16), ctx.dp(6), ctx.dp(16), ctx.dp(6))
            setSingleLine(true)
            ellipsize = TextUtils.TruncateAt.END
        }
        tv.text = items[position]
        tv.background = focusBg(ctx, 8, position == active)
        return tv
    }
}

fun buildPlayer(ctx: Context): ExoPlayer {
    val http = DefaultHttpDataSource.Factory()
        .setUserAgent("VLC/3.0.18 LibVLC/3.0.18")
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15000)
        .setReadTimeoutMs(20000)
    val factory = DefaultMediaSourceFactory(DefaultDataSource.Factory(ctx, http))
    val p = ExoPlayer.Builder(ctx).setMediaSourceFactory(factory).build()
    p.playWhenReady = true
    return p
}
