package com.goodwy.messages.feature.conversations

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.goodwy.messages.model.Conversation
import com.goodwy.messages.util.Preferences

object ConversationAppearanceDialog {

    data class GlassTint(val name: String, val color: Int)
    data class CellHeightOption(val name: String, val heightIndex: Int)

    val GLASS_TINTS = listOf(
        GlassTint("شیشه شفاف نقره‌ای (پیش‌فرض)", 0),
        GlassTint("شیشه آبی اقیانوسی (iOS Blue)", 0x3D007AFF),
        GlassTint("شیشه زمردی کریستالی (Emerald)", 0x3D34C759),
        GlassTint("شیشه بنفش کیهانی (Purple)", 0x3DAF52DE),
        GlassTint("شیشه یاقوتی (Ruby Red)", 0x3DFF2D55),
        GlassTint("شیشه کهربایی طلایی (Amber)", 0x3DFF9500),
        GlassTint("شیشه دودی مات (Frosted Dark)", 0x59000000.toInt())
    )

    val CELL_HEIGHTS = listOf(
        CellHeightOption("فشرده (Compact - 56dp)", 1),
        CellHeightOption("استاندارد (Standard - 72dp)", 0),
        CellHeightOption("متوسط و باز (Comfortable - 88dp)", 2),
        CellHeightOption("بزرگ (Spacious - 104dp)", 3)
    )

    fun show(
        activity: Activity,
        conversation: Conversation,
        prefs: Preferences,
        onAppearanceChanged: () -> Unit,
        onSelectClicked: () -> Unit
    ) {
        val colorKey = "custom_cell_color_${conversation.id}"
        val heightKey = "custom_cell_height_${conversation.id}"

        val options = arrayOf(
            "🎨 تغییر رنگ شیشه‌ای این مخاطب",
            "📏 تغییر ابعاد و ارتفاع سلول",
            "🔄 بازنشانی ظاهر به پیش‌فرض",
            "✅ انتخاب این مکالمه (Select)"
        )

        AlertDialog.Builder(activity)
            .setTitle(conversation.getTitle())
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showColorPicker(activity, conversation, prefs, onAppearanceChanged)
                    1 -> showHeightPicker(activity, conversation, prefs, onAppearanceChanged)
                    2 -> {
                        prefs.sharedPrefs.edit().remove(colorKey).remove(heightKey).apply()
                        onAppearanceChanged()
                    }
                    3 -> onSelectClicked()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showColorPicker(
        activity: Activity,
        conversation: Conversation,
        prefs: Preferences,
        onAppearanceChanged: () -> Unit
    ) {
        val colorKey = "custom_cell_color_${conversation.id}"
        val currentColor = prefs.sharedPrefs.getInt(colorKey, 0)

        val names = GLASS_TINTS.map { it.name }.toTypedArray()
        val currentIndex = GLASS_TINTS.indexOfFirst { it.color == currentColor }.let { if (it >= 0) it else 0 }

        AlertDialog.Builder(activity)
            .setTitle(conversation.getTitle())
            .setSingleChoiceItems(names, currentIndex) { dialog, which ->
                val selected = GLASS_TINTS[which].color
                if (selected == 0) {
                    prefs.sharedPrefs.edit().remove(colorKey).apply()
                } else {
                    prefs.sharedPrefs.edit().putInt(colorKey, selected).apply()
                }
                onAppearanceChanged()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showHeightPicker(
        activity: Activity,
        conversation: Conversation,
        prefs: Preferences,
        onAppearanceChanged: () -> Unit
    ) {
        val heightKey = "custom_cell_height_${conversation.id}"
        val currentHeight = prefs.sharedPrefs.getInt(heightKey, 0)

        val names = CELL_HEIGHTS.map { it.name }.toTypedArray()
        val currentIndex = CELL_HEIGHTS.indexOfFirst { it.heightIndex == currentHeight }.let { if (it >= 0) it else 1 }

        AlertDialog.Builder(activity)
            .setTitle(conversation.getTitle())
            .setSingleChoiceItems(names, currentIndex) { dialog, which ->
                val selected = CELL_HEIGHTS[which].heightIndex
                prefs.sharedPrefs.edit().putInt(heightKey, selected).apply()
                onAppearanceChanged()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
