package com.example.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeUtils {
    fun formatTime12h(hour: Int, minute: Int, isArabic: Boolean = false): String {
        val h = ((hour % 24) + 24) % 24
        val m = minute.coerceIn(0, 59)
        val amPm = if (isArabic) {
            if (h < 12) "ص" else "م"
        } else {
            if (h < 12) "AM" else "PM"
        }
        val displayH = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return String.format(Locale.US, "%d:%02d %s", displayH, m, amPm)
    }

    fun formatTimer(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val mins = safe / 60
        val secs = safe % 60
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    fun formatDurationMinutes(totalMinutes: Int, isArabic: Boolean = false): String {
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return if (isArabic) {
            when {
                hours > 0 && mins > 0 -> "${hours} س ${mins} د"
                hours > 0 -> "${hours} س"
                else -> "${mins} د"
            }
        } else {
            when {
                hours > 0 && mins > 0 -> "${hours}h ${mins}m"
                hours > 0 -> "${hours}h"
                else -> "${mins}m"
            }
        }
    }

    fun formatTimestamp(millis: Long, isArabic: Boolean = false): String {
        val locale = if (isArabic) Locale("ar") else Locale.ENGLISH
        val sdf = SimpleDateFormat("MMM d, h:mm a", locale)
        return sdf.format(Date(millis))
    }

    fun formatDateGroup(millis: Long, isArabic: Boolean = false): String {
        val cal = Calendar.getInstance()
        val todayYear = cal.get(Calendar.YEAR)
        val todayDay = cal.get(Calendar.DAY_OF_YEAR)

        val target = Calendar.getInstance().apply { timeInMillis = millis }
        val targetYear = target.get(Calendar.YEAR)
        val targetDay = target.get(Calendar.DAY_OF_YEAR)

        return when {
            todayYear == targetYear && todayDay == targetDay -> if (isArabic) "اليوم" else "Today"
            todayYear == targetYear && todayDay - 1 == targetDay -> if (isArabic) "أمس" else "Yesterday"
            else -> {
                val locale = if (isArabic) Locale("ar") else Locale.ENGLISH
                SimpleDateFormat("EEEE, MMM d", locale).format(Date(millis))
            }
        }
    }

    fun currentGreeting(isArabic: Boolean): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (isArabic) {
            when (hour) {
                in 5..11 -> "صباح الخير"
                in 12..16 -> "مساء الخير"
                in 17..20 -> "مساء النور"
                else -> "ليلة هادئة"
            }
        } else {
            when (hour) {
                in 5..11 -> "Good morning"
                in 12..16 -> "Good afternoon"
                in 17..20 -> "Good evening"
                else -> "Quiet night"
            }
        }
    }

    fun formattedCurrentDate(isArabic: Boolean): String {
        val locale = if (isArabic) Locale("ar") else Locale.ENGLISH
        return SimpleDateFormat("EEEE, MMMM d", locale).format(Date())
    }

    fun timeContextLabel(hour: Int, isArabic: Boolean): String {
        return if (isArabic) {
            when (hour) {
                in 5..11 -> "الصباح"
                in 12..16 -> "بعد الظهر"
                in 17..20 -> "المساء"
                else -> "الليل"
            }
        } else {
            when (hour) {
                in 5..11 -> "Morning"
                in 12..16 -> "Afternoon"
                in 17..20 -> "Evening"
                else -> "Night"
            }
        }
    }
}
