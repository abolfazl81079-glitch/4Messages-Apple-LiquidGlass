package com.goodwy.messages.feature.compose

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.goodwy.messages.common.util.SmartNumberDetector

object SmartActionsDialog {

    fun show(activity: Activity, rawText: String) {
        val entities = SmartNumberDetector.detect(rawText)
        if (entities.isEmpty()) return

        val items = entities.map { entity ->
            val icon = when (entity.type) {
                SmartNumberDetector.NumberType.PHONE -> "📞"
                SmartNumberDetector.NumberType.BANK_CARD -> "💳"
                SmartNumberDetector.NumberType.IBAN -> "🏦"
                SmartNumberDetector.NumberType.OTP -> "🔑"
                SmartNumberDetector.NumberType.TRACKING -> "📦"
                else -> "🔢"
            }
            "$icon ${entity.title}: ${entity.description}"
        }.toTypedArray()

        AlertDialog.Builder(activity)
            .setTitle("شناسایی هوشمند ارقام و اطلاعات")
            .setItems(items) { _, which ->
                val selected = entities[which]
                showEntityActions(activity, selected)
            }
            .setNegativeButton("بستن", null)
            .show()
    }

    private fun showEntityActions(activity: Activity, entity: SmartNumberDetector.DetectedEntity) {
        val actions = mutableListOf<String>()
        actions.add("📋 کپی نسخه نرمال‌شده (اعداد انگلیسی): ${entity.normalizedValue}")
        actions.add("📋 کپی با قالب اصلی: ${entity.originalText}")

        if (entity.type == SmartNumberDetector.NumberType.PHONE) {
            actions.add("📞 تماس مستقیم با ${entity.normalizedValue}")
        }
        if (entity.type == SmartNumberDetector.NumberType.BANK_CARD) {
            val statusText = if (entity.isValid) "معتبر (الگوریتم Luhn)" else "نامعتبر"
            actions.add("💳 بررسی وضعیت کارت: $statusText")
        }

        AlertDialog.Builder(activity)
            .setTitle(entity.title)
            .setItems(actions.toTypedArray()) { _, which ->
                when (which) {
                    0 -> SmartNumberDetector.copyToClipboard(activity, entity, useNormalized = true)
                    1 -> SmartNumberDetector.copyToClipboard(activity, entity, useNormalized = false)
                    2 -> {
                        if (entity.type == SmartNumberDetector.NumberType.PHONE) {
                            SmartNumberDetector.callNumber(activity, entity.normalizedValue)
                        } else if (entity.type == SmartNumberDetector.NumberType.BANK_CARD) {
                            SmartNumberDetector.copyToClipboard(activity, entity, useNormalized = true)
                        }
                    }
                }
            }
            .setNegativeButton("انصراف", null)
            .show()
    }
}
