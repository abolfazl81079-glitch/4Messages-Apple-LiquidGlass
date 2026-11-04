package com.goodwy.messages.common.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import com.goodwy.messages.R
import com.goodwy.messages.common.util.Colors
import com.goodwy.messages.common.util.extensions.resolveThemeBoolean
import com.goodwy.messages.common.util.extensions.resolveThemeColor
import com.goodwy.messages.injection.appComponent
import com.goodwy.messages.util.Preferences
import javax.inject.Inject

class QkSwitch @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : SwitchCompat(context, attrs) {

    @Inject lateinit var colors: Colors
    @Inject lateinit var prefs: Preferences

    init {
        if (!isInEditMode) {
            appComponent.inject(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (!isInEditMode) {
            applySwitchColors()
        }
    }

    fun applySwitchColors() {
        val states = arrayOf(
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf(android.R.attr.state_checked),
            intArrayOf() // Unchecked
        )

        val isLight = context.resolveThemeBoolean(R.attr.isLightTheme)
        val activeAccent = colors.theme().theme

        // Thumb: Always clean pure white when enabled (authentic Apple iOS switch behavior)
        thumbTintList = ColorStateList(states, intArrayOf(
            if (isLight) 0xFFD1D1D6.toInt() else 0xFF636366.toInt(), // Disabled
            0xFFFFFFFF.toInt(),                                      // Checked (Pure White)
            0xFFFFFFFF.toInt()                                       // Unchecked (Pure White)
        ))

        // Track: Apple Blue / Accent when checked; Slate Gray when unchecked
        val uncheckedTrack = if (isLight) 0xFFE5E5EA.toInt() else 0xFF393946.toInt()
        val disabledTrack = if (isLight) 0x33000000.toInt() else 0x33FFFFFF.toInt()

        trackTintList = ColorStateList(states, intArrayOf(
            disabledTrack,
            activeAccent,
            uncheckedTrack
        ))
    }
}
