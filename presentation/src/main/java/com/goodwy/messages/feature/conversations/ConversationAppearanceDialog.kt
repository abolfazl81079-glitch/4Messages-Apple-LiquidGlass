package com.goodwy.messages.feature.conversations

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.goodwy.messages.feature.main.MainActivity
import com.goodwy.messages.model.Conversation
import com.goodwy.messages.util.Preferences
import java.io.File

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
        val avatarKey = "custom_avatar_${conversation.id}"
        val hasCustomAvatar = prefs.sharedPrefs.getString(avatarKey, null) != null

        val optionsList = mutableListOf(
            "🎨 تغییر رنگ شیشه‌ای این مخاطب",
            "📏 تغییر ابعاد و ارتفاع سلول",
            "📸 انتخاب عکس پروفایل مخاطب (از گالری)"
        )
        if (hasCustomAvatar) {
            optionsList.add("🗑️ حذف عکس پروفایل سفارشی")
        }
        optionsList.add("🔄 بازنشانی تمام تنظیمات به پیش‌فرض")
        optionsList.add("✅ انتخاب این مکالمه (Select)")

        val options = optionsList.toTypedArray()

        AlertDialog.Builder(activity)
            .setTitle(conversation.getTitle())
            .setItems(options) { _, which ->
                val selectedText = options[which]
                when {
                    selectedText.contains("رنگ") -> showColorPicker(activity, conversation, prefs, onAppearanceChanged)
                    selectedText.contains("ارتفاع") -> showHeightPicker(activity, conversation, prefs, onAppearanceChanged)
                    selectedText.contains("انتخاب عکس") -> {
                        prefs.sharedPrefs.edit().putLong("pending_avatar_conv_id", conversation.id).apply()
                        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "image/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                        }
                        try {
                            activity.startActivityForResult(
                                Intent.createChooser(intent, "انتخاب تصویر پروفایل مخاطب"),
                                MainActivity.REQUEST_CODE_PICK_AVATAR
                            )
                        } catch (e: Exception) {
                            Toast.makeText(activity, "برنامه گالری در دستگاه یافت نشد: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    selectedText.contains("حذف عکس") -> {
                        val path = prefs.sharedPrefs.getString(avatarKey, null)
                        if (path != null) {
                            try { File(path).delete() } catch (e: Exception) {}
                        }
                        prefs.sharedPrefs.edit().remove(avatarKey).apply()
                        onAppearanceChanged()
                        Toast.makeText(activity, "عکس پروفایل سفارشی مخاطب حذف شد", Toast.LENGTH_SHORT).show()
                    }
                    selectedText.contains("بازنشانی") -> {
                        val path = prefs.sharedPrefs.getString(avatarKey, null)
                        if (path != null) {
                            try { File(path).delete() } catch (e: Exception) {}
                        }
                        prefs.sharedPrefs.edit().remove(colorKey).remove(heightKey).remove(avatarKey).apply()
                        onAppearanceChanged()
                        Toast.makeText(activity, "تمام تنظیمات اختصاصی به پیش‌فرض بازگشت", Toast.LENGTH_SHORT).show()
                    }
                    selectedText.contains("انتخاب") -> onSelectClicked()
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
