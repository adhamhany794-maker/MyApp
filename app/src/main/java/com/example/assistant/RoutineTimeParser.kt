package com.example.assistant

import com.example.models.SoundMode

object RoutineTimeParser {

    fun extractTimeHours(text: String): List<Int> {
        val regex = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(صباحاً|صباحا|صبح|الصبح|صباح|ص|مساءً|مساء|م|بليل|بالليل|ليل|الليل|ضهر|ظهر|الضهر|عصر|العصر|am|pm)?")
        val matches = regex.findAll(text).toList()
        if (matches.isEmpty()) return emptyList()

        val globalEvening = text.contains("مساء") || text.contains("بليل") || text.contains("بالليل") || text.contains("ليل") || text.contains("pm") || text.contains("م")
        val globalMorning = text.contains("صباح") || text.contains("صبح") || text.contains("الصبح") || text.contains("am") || text.contains("ص")

        val result = mutableListOf<Int>()
        for (m in matches) {
            val rawHour = m.groupValues[1].toIntOrNull() ?: continue
            if (rawHour !in 0..23) continue
            val period = m.groupValues[3].lowercase()
            val isPm = period.contains("مساء") || period.contains("بليل") || period.contains("ليل") || period == "م" || period == "pm" || period.contains("عصر") || period.contains("ضهر") || period.contains("ظهر")
            val isAm = period.contains("صباح") || period.contains("صبح") || period == "ص" || period == "am"

            val hour = when {
                isPm && rawHour < 12 -> rawHour + 12
                isAm && rawHour == 12 -> 0
                !isPm && !isAm -> {
                    if (globalEvening && !globalMorning && rawHour < 12 && rawHour < 18) rawHour + 12
                    else if (globalMorning && rawHour == 12) 0
                    else rawHour
                }
                else -> rawHour
            }
            result.add(hour.coerceIn(0, 23))
        }
        return result
    }

    fun parseTimeRange(text: String): Pair<Int, Int>? {
        val hours = extractTimeHours(text)
        if (hours.isEmpty()) return null
        val start = hours.first()
        val end = if (hours.size >= 2) hours[1] else (start + 3) % 24
        return start to end
    }

    fun parseExplicitSoundMode(text: String): SoundMode? {
        val lower = text.lowercase()
        return when {
            lower.contains("vibrate") || lower.contains("اهتزاز") -> SoundMode.VIBRATE
            lower.contains("sound") || lower.contains("عادي") || (lower.contains("صوت") && !lower.contains("صامت") && !lower.contains("بدون")) -> SoundMode.SOUND
            lower.contains("silent") || lower.contains("صامت") || lower.contains("كتم") || lower.contains("اكتم") || lower.contains("هدوء") -> SoundMode.SILENT
            else -> null
        }
    }
}
