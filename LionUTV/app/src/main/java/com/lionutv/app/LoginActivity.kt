package com.lionutv.app

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class LoginActivity : Activity() {
    private var fails = 0
    private var lockUntil = 0L
    private lateinit var codeField: EditText
    private lateinit var errorTv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Store.current() != null) {
            goHome()
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = scrim(this@LoginActivity, 22)
            setPadding(dp(56), dp(28), dp(56), dp(32))
        }
        card.addView(ImageView(this).apply { setImageResource(R.drawable.lion_logo) }, lp(dp(120), dp(120)))
        card.addView(label(this, "LION UTV", 34f, C.AMBER, true).apply { gravity = Gravity.CENTER }, lp(WRAP, WRAP))
        card.addView(
            label(this, t("Enter your access code", "أدخل كود الدخول"), 18f, C.MUTED).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(6), 0, dp(14))
            }, lp(WRAP, WRAP)
        )

        val fieldBg = GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(0x33FFFFFF)
            setStroke(dp(2), C.GOLD)
        }
        codeField = EditText(this).apply {
            hint = t("Access code", "كود الدخول")
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(C.CREAM)
            setHintTextColor(C.MUTED)
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = arrayOf(InputFilter.LengthFilter(12))
            background = fieldBg
            imeOptions = EditorInfo.IME_ACTION_DONE
            setOnEditorActionListener { _, _, _ ->
                submit()
                true
            }
        }
        card.addView(codeField, lp(dp(360), dp(64)))

        errorTv = label(this, "", 16f, C.RED).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(8))
        }
        card.addView(errorTv, lp(WRAP, WRAP))

        val go = button(this, t("Sign in", "دخول"), 22f) { submit() }.apply { gravity = Gravity.CENTER }
        card.addView(go, lp(dp(360), WRAP))

        root.addView(card, lp(WRAP, WRAP))
        setContentView(root)
        codeField.requestFocus()
    }

    private fun submit() {
        val now = System.currentTimeMillis()
        if (now < lockUntil) {
            errorTv.text = t("Too many attempts. Wait a moment.", "محاولات كثيرة. انتظر قليلاً.")
            return
        }
        val u = Store.signIn(codeField.text.toString())
        if (u != null) {
            goHome()
        } else {
            fails++
            if (fails >= 5) {
                lockUntil = now + 30_000
                fails = 0
            }
            errorTv.text = t("Invalid, disabled or expired code", "الكود غير صحيح أو معطل أو منتهي")
            codeField.setText("")
        }
    }

    private fun goHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
