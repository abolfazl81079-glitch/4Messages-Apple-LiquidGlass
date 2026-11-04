package com.goodwy.messages.common.util

import android.content.Context
import android.text.format.DateFormat
import com.goodwy.messages.common.util.extensions.isSameDay
import com.goodwy.messages.common.util.extensions.isSameWeek
import com.goodwy.messages.common.util.extensions.isSameYear
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DateFormatter @Inject constructor(val context: Context) {

    object JalaliCalendar {
        fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
            val g_d_m = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
            val jy = if (gy > 1600) 979 else 0
            val gy2 = if (gy > 1600) gy - 1600 else gy - 621
            val gy2_p = if (gm > 2 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) 1 else 0
            val days = (365 * gy2) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) - 80 + gd + g_d_m[gm - 1] + gy2_p
            var jy2 = jy + 33 * (days / 12053)
            var rem_days = days % 12053
            jy2 += 4 * (rem_days / 1461)
            rem_days %= 1461
            if (rem_days > 365) {
                jy2 += (rem_days - 1) / 365
                rem_days = (rem_days - 1) % 365
            }
            val jm = if (rem_days < 186) 1 + (rem_days / 31) else 7 + ((rem_days - 186) / 30)
            val jd = 1 + if (rem_days < 186) (rem_days % 31) else ((rem_days - 186) % 30)
            return Triple(jy2, jm, jd)
        }

        val JALALI_MONTHS = arrayOf(
            "فروردین", "اردیبهشت", "خرداد",
            "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر",
            "دی", "بهمن", "اسفند"
        )

        fun toPersianDigits(text: String): String {
            val farsiDigits = arrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
            val sb = StringBuilder()
            for (c in text) {
                if (c in '0'..'9') {
                    sb.append(farsiDigits[c - '0'])
                } else {
                    sb.append(c)
                }
            }
            return sb.toString()
        }
    }

    private val isPersian: Boolean
        get() = Locale.getDefault().language == "fa"

    /**
     * Formats the [pattern] correctly for the current locale, and replaces 12 hour format with
     * 24 hour format if necessary
     */
    private fun getFormatter(pattern: String): SimpleDateFormat {
        var formattedPattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), pattern)

        if (DateFormat.is24HourFormat(context)) {
            formattedPattern = formattedPattern
                    .replace("h", "HH")
                    .replace("K", "HH")
                    .replace(" a".toRegex(), "")
        }

        return SimpleDateFormat(formattedPattern, Locale.getDefault())
    }

    fun getDetailedTimestamp(date: Long): String {
        if (isPersian) {
            val cal = Calendar.getInstance().apply { timeInMillis = date }
            val (jy, jm, jd) = JalaliCalendar.gregorianToJalali(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
            val time = getFormatter("HH:mm:ss").format(date)
            return JalaliCalendar.toPersianDigits("$jd ${JalaliCalendar.JALALI_MONTHS[jm - 1]} $jy، $time")
        }
        return getFormatter("M/d/y, h:mm:ss a").format(date)
    }

    fun getTimestamp(date: Long): String {
        val formatted = getFormatter("h:mm a").format(date)
        return if (isPersian) JalaliCalendar.toPersianDigits(formatted) else formatted
    }

    fun getMessageTimestamp(date: Long): String {
        val now = Calendar.getInstance()
        val then = Calendar.getInstance()
        then.timeInMillis = date

        if (isPersian) {
            val (jy, jm, jd) = JalaliCalendar.gregorianToJalali(
                then.get(Calendar.YEAR),
                then.get(Calendar.MONTH) + 1,
                then.get(Calendar.DAY_OF_MONTH)
            )
            val time = getFormatter("h:mm a").format(date)
            val result = when {
                now.isSameDay(then) -> time
                now.isSameWeek(then) -> "${getFormatter("E").format(date)} $time"
                now.isSameYear(then) -> "$jd ${JalaliCalendar.JALALI_MONTHS[jm - 1]}، $time"
                else -> "$jd ${JalaliCalendar.JALALI_MONTHS[jm - 1]} $jy، $time"
            }
            return JalaliCalendar.toPersianDigits(result)
        }

        return when {
            now.isSameDay(then) -> getFormatter("h:mm a")
            now.isSameWeek(then) -> getFormatter("E h:mm a")
            now.isSameYear(then) -> getFormatter("MMM d, h:mm a")
            else -> getFormatter("MMM d yyyy, h:mm a")
        }.format(date)
    }

    fun getConversationTimestamp(date: Long): String {
        val now = Calendar.getInstance()
        val then = Calendar.getInstance()
        then.timeInMillis = date

        if (isPersian) {
            val (jy, jm, jd) = JalaliCalendar.gregorianToJalali(
                then.get(Calendar.YEAR),
                then.get(Calendar.MONTH) + 1,
                then.get(Calendar.DAY_OF_MONTH)
            )
            val result = when {
                now.isSameDay(then) -> getFormatter("h:mm a").format(date)
                now.isSameWeek(then) -> getFormatter("E").format(date)
                now.isSameYear(then) -> "$jd ${JalaliCalendar.JALALI_MONTHS[jm - 1]}"
                else -> "$jy/$jm/$jd"
            }
            return JalaliCalendar.toPersianDigits(result)
        }

        return when {
            now.isSameDay(then) -> getFormatter("h:mm a")
            now.isSameWeek(then) -> getFormatter("E")
            now.isSameYear(then) -> getFormatter("MMM d")
            else -> getFormatter("MM/d/yy")
        }.format(date)
    }

    fun getScheduledTimestamp(date: Long): String {
        val now = Calendar.getInstance()
        val then = Calendar.getInstance()
        then.timeInMillis = date

        if (isPersian) {
            val (jy, jm, jd) = JalaliCalendar.gregorianToJalali(
                then.get(Calendar.YEAR),
                then.get(Calendar.MONTH) + 1,
                then.get(Calendar.DAY_OF_MONTH)
            )
            val time = getFormatter("h:mm a").format(date)
            val result = when {
                now.isSameDay(then) -> time
                now.isSameYear(then) -> "$jd ${JalaliCalendar.JALALI_MONTHS[jm - 1]} $time"
                else -> "$jd ${JalaliCalendar.JALALI_MONTHS[jm - 1]} $jy $time"
            }
            return JalaliCalendar.toPersianDigits(result)
        }

        return when {
            now.isSameDay(then) -> getFormatter("h:mm a")
            now.isSameYear(then) -> getFormatter("MMM d h:mm a")
            else -> getFormatter("MMM d yyyy h:mm a")
        }.format(date)
    }

}
