package com.goodwy.messages.common.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.util.regex.Pattern

object SmartNumberDetector {

    enum class NumberType {
        PHONE,
        BANK_CARD,
        IBAN,
        OTP,
        TRACKING,
        NUMERIC
    }

    data class DetectedEntity(
        val type: NumberType,
        val originalText: String,
        val normalizedValue: String,
        val title: String,
        val description: String,
        val isValid: Boolean = true
    )

    // Normalize Persian and Arabic numerals to ASCII 0-9
    fun normalizeDigits(input: String): String {
        val builder = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                '۰', '٠' -> builder.append('0')
                '۱', '١' -> builder.append('1')
                '۲', '٢' -> builder.append('2')
                '۳', '٣' -> builder.append('3')
                '۴', '٤' -> builder.append('4')
                '۵', '٥' -> builder.append('5')
                '۶', '٦' -> builder.append('6')
                '۷', '٧' -> builder.append('7')
                '۸', '٨' -> builder.append('8')
                '۹', '٩' -> builder.append('9')
                else -> builder.append(ch)
            }
        }
        return builder.toString()
    }

    // Luhn Algorithm for validating 16-digit bank cards
    fun validateLuhn(cardNumber: String): Boolean {
        val digits = cardNumber.filter { it.isDigit() }
        if (digits.length != 16) return false
        var sum = 0
        var alternate = false
        for (i in digits.length - 1 downTo 0) {
            var n = digits[i] - '0'
            if (alternate) {
                n *= 2
                if (n > 9) n = (n % 10) + 1
            }
            sum += n
            alternate = !alternate
        }
        return (sum % 10 == 0)
    }

    // Bank name resolver for Iranian bank cards
    fun getIranianBankName(cardPrefix: String): String {
        return when (cardPrefix.take(6)) {
            "603799" -> "بانک ملی"
            "589210" -> "بانک سپه"
            "627648" -> "بانک توسعه صادرات"
            "627961" -> "بانک صنعت و معدن"
            "603770" -> "بانک کشاورزی"
            "628023" -> "بانک مسکن"
            "627760" -> "پست بانک"
            "502908" -> "بانک توسعه تعاون"
            "627412" -> "بانک اقتصاد نوین"
            "622106", "639194" -> "بانک پارسیان"
            "502229", "639599" -> "بانک پاسارگاد"
            "627488" -> "بانک کارآفرین"
            "621986" -> "بانک سامان"
            "639346" -> "بانک سینا"
            "639607" -> "بانک سرمایه"
            "636214" -> "بانک آینده"
            "502806" -> "بانک شهر"
            "502938" -> "بانک دی"
            "603769" -> "بانک صادرات"
            "610433", "991975" -> "بانک ملت"
            "585983" -> "بانک تجارت"
            "627353" -> "بانک تجارت"
            "589463" -> "بانک رفاه"
            "627381" -> "بانک انصار"
            "505416" -> "بانک گردشگری"
            "606373" -> "بانک مهر ایران"
            "505785" -> "بانک ایران زمین"
            "505801" -> "موسسه کوثر"
            "606256" -> "موسسه ملل"
            "639370" -> "بانک مهر اقتصاد"
            "581874" -> "بانک ایران ونزوئلا"
            "504706" -> "بانک رسالت"
            else -> "کارت بانکی"
        }
    }

    fun detect(text: String): List<DetectedEntity> {
        val results = mutableListOf<DetectedEntity>()
        if (text.isBlank()) return results

        val normalized = normalizeDigits(text)

        // 1. Bank Cards (16 digits with optional spaces or dashes)
        val cardRegex = Pattern.compile("(?<!\\d)(\\d{4}[ -]?\\d{4}[ -]?\\d{4}[ -]?\\d{4})(?!\\d)")
        val cardMatcher = cardRegex.matcher(normalized)
        val foundCards = mutableSetOf<String>()
        while (cardMatcher.find()) {
            val raw = cardMatcher.group(1) ?: continue
            val digits = raw.filter { it.isDigit() }
            if (digits.length == 16 && foundCards.add(digits)) {
                val isLuhnValid = validateLuhn(digits)
                val bank = getIranianBankName(digits)
                results.add(
                    DetectedEntity(
                        type = NumberType.BANK_CARD,
                        originalText = raw,
                        normalizedValue = digits,
                        title = bank,
                        description = "${digits.chunked(4).joinToString("-")}${if (isLuhnValid) " (معتبر)" else ""}",
                        isValid = isLuhnValid
                    )
                )
            }
        }

        // 2. IBAN / Sheba (IR followed by 24 digits, or 24 digits after 'شبا')
        val ibanRegex = Pattern.compile("(?i)(?:IR|ایران)?\\s*(\\d{24})(?!\\d)")
        val ibanMatcher = ibanRegex.matcher(normalized)
        val foundIbans = mutableSetOf<String>()
        while (ibanMatcher.find()) {
            val digits = ibanMatcher.group(1) ?: continue
            if (foundIbans.add(digits)) {
                results.add(
                    DetectedEntity(
                        type = NumberType.IBAN,
                        originalText = ibanMatcher.group(0) ?: digits,
                        normalizedValue = "IR$digits",
                        title = "شماره شبا (IBAN)",
                        description = "IR${digits.chunked(4).joinToString(" ")}"
                    )
                )
            }
        }

        // 3. Mobile & Phone numbers (09..., +98..., +...)
        val phoneRegex = Pattern.compile("(?<!\\d)(\\+?\\d{10,14})(?!\\d)")
        val phoneMatcher = phoneRegex.matcher(normalized.replace("[\\s-]".toRegex(), ""))
        val foundPhones = mutableSetOf<String>()
        while (phoneMatcher.find()) {
            val phone = phoneMatcher.group(1) ?: continue
            if (phone.length in 10..14 && !foundCards.any { it.contains(phone) } && foundPhones.add(phone)) {
                val isIranianMobile = phone.startsWith("09") || phone.startsWith("989") || phone.startsWith("+989")
                results.add(
                    DetectedEntity(
                        type = NumberType.PHONE,
                        originalText = phone,
                        normalizedValue = if (phone.startsWith("00")) "+" + phone.substring(2) else phone,
                        title = if (isIranianMobile) "شماره موبایل" else "شماره تماس",
                        description = phone
                    )
                )
            }
        }

        // 4. Verification / OTP Codes (4 to 8 digits with context keywords)
        val isOtpContext = text.contains("کد") || text.contains("رمز") || 
                           text.contains("code", ignoreCase = true) || 
                           text.contains("otp", ignoreCase = true) ||
                           text.contains("تایید") || text.contains("تأیید")
        if (isOtpContext) {
            val otpRegex = Pattern.compile("(?<!\\d)(\\d{4,8})(?!\\d)")
            val otpMatcher = otpRegex.matcher(normalized)
            while (otpMatcher.find()) {
                val otp = otpMatcher.group(1) ?: continue
                if (!foundCards.any { it.contains(otp) } && !foundPhones.any { it.contains(otp) }) {
                    results.add(
                        DetectedEntity(
                            type = NumberType.OTP,
                            originalText = otp,
                            normalizedValue = otp,
                            title = "کد تأیید یکبارمصرف",
                            description = otp
                        )
                    )
                    break
                }
            }
        }

        // 5. Postal / Tracking Codes
        if (text.contains("رهگیری") || text.contains("پست") || text.contains("مرسوله")) {
            val trackRegex = Pattern.compile("(?<!\\d)(\\d{10,24})(?!\\d)")
            val trackMatcher = trackRegex.matcher(normalized)
            while (trackMatcher.find()) {
                val code = trackMatcher.group(1) ?: continue
                if (!foundCards.contains(code) && !foundIbans.contains(code) && !foundPhones.contains(code)) {
                    results.add(
                        DetectedEntity(
                            type = NumberType.TRACKING,
                            originalText = code,
                            normalizedValue = code,
                            title = "کد رهگیری پستی",
                            description = code
                        )
                    )
                    break
                }
            }
        }

        return results
    }

    fun copyToClipboard(context: Context, entity: DetectedEntity, useNormalized: Boolean = true) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val valueToCopy = if (useNormalized) entity.normalizedValue else entity.originalText
        val clip = ClipData.newPlainText(entity.title, valueToCopy)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "${entity.title} کپی شد: $valueToCopy", Toast.LENGTH_SHORT).show()
    }

    fun callNumber(context: Context, phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phoneNumber)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "خطا در تماس: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
