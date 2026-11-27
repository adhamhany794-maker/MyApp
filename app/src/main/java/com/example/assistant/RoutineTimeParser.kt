package com.example.assistant

import com.example.models.SoundMode

data class ParsedTimePoint(
    val hour24: Int,
    val minute: Int,
    val startIndex: Int = 0
)

data class ParsedDetailedRange(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
)

object RoutineTimeParser {

    private val arabicWordToNumber = mapOf(
        "واحدة" to 1, "واحد" to 1, "وحده" to 1,
        "اثنين" to 2, "إثنين" to 2, "تنين" to 2,
        "ثلاثة" to 3, "تلاتة" to 3, "ثلاث" to 3, "تلات" to 3,
        "أربعة" to 4, "اربعة" to 4, "أربع" to 4, "اربع" to 4,
        "خمسة" to 5, "خمس" to 5,
        "ستة" to 6, "ست" to 6,
        "سبعة" to 7, "سبع" to 7,
        "ثمانية" to 8, "تمانية" to 8, "ثماني" to 8, "تمان" to 8,
        "تسعة" to 9, "تسع" to 9,
        "عشرة" to 10, "عشر" to 10,
        "إحدى عشر" to 11, "احد عشر" to 11, "احداعش" to 11,
        "اثنا عشر" to 12, "اثني عشر" to 12, "اتناعش" to 12
    )

    private val englishWordToNumber = mapOf(
        "one" to 1, "two" to 2, "three" to 3, "four" to 4,
        "five" to 5, "six" to 6, "seven" to 7, "eight" to 8,
        "nine" to 9, "ten" to 10, "eleven" to 11, "twelve" to 12
    )

    private data class RawCandidate(
        val startIndex: Int,
        val endIndex: Int,
        val rawHour: Int,
        val minute: Int,
        val periodStr: String
    )

    fun extractTimePoints(text: String): List<ParsedTimePoint> {
        val lower = text.lowercase()
        val candidates = mutableListOf<RawCandidate>()

        // 1. English fractions: "quarter past 8", "half past 8", "quarter to 9"
        val enFractionRegex = Regex(
            "(quarter|half)\\s+(past|after|to|till)\\s+(\\d{1,2}|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve)\\s*(am|pm)?",
            RegexOption.IGNORE_CASE
        )
        for (m in enFractionRegex.findAll(lower)) {
            val type = m.groupValues[1]
            val direction = m.groupValues[2]
            val hourStr = m.groupValues[3]
            val rawHour = hourStr.toIntOrNull() ?: englishWordToNumber[hourStr] ?: continue
            val period = m.groupValues[4]
            val (effectiveHour, min) = when {
                direction == "to" || direction == "till" -> {
                    val h = if (rawHour == 1) 12 else (rawHour - 1).coerceAtLeast(0)
                    h to 45
                }
                type == "half" -> rawHour to 30
                else -> rawHour to 15
            }
            candidates.add(RawCandidate(m.range.first, m.range.last + 1, effectiveHour, min, period))
        }

        // 2. Arabic "الا ربع" / "إلا ربع": e.g. "9 الا ربع", "9 إلا ربع" -> hour = 8, minute = 45
        val arMinusQuarterRegex = Regex(
            "(\\d{1,2}|واحدة|واحد|اثنين|إثنين|تنين|ثلاثة|تلاتة|أربعة|اربعة|خمسة|ستة|سبعة|ثمانية|تمانية|تسعة|عشرة|إحدى عشر|احداعش|اثنا عشر|اتناعش)\\s*(?:الا|إلا|إلّا)\\s*ربع\\s*(صباحاً|صباحا|صبح|الصبح|صباح|ص|مساءً|مساء|م|بليل|بالليل|ليل|الليل|ضهر|ظهر|عصر|am|pm)?",
            RegexOption.IGNORE_CASE
        )
        for (m in arMinusQuarterRegex.findAll(lower)) {
            val hourStr = m.groupValues[1]
            val rawHour = hourStr.toIntOrNull() ?: arabicWordToNumber[hourStr] ?: continue
            val period = m.groupValues[2]
            val effectiveHour = if (rawHour == 1) 12 else (rawHour - 1).coerceAtLeast(0)
            candidates.add(RawCandidate(m.range.first, m.range.last + 1, effectiveHour, 45, period))
        }

        // 3. Arabic "ونص" / "ونصف": e.g. "8 ونص", "8 ونصف" -> minute = 30
        val arHalfRegex = Regex(
            "(\\d{1,2}|واحدة|واحد|اثنين|إثنين|تنين|ثلاثة|تلاتة|أربعة|اربعة|خمسة|ستة|سبعة|ثمانية|تمانية|تسعة|عشرة|إحدى عشر|احداعش|اثنا عشر|اتناعش)\\s*(?:و|)(?:نص|نصف)\\s*(صباحاً|صباحا|صبح|الصبح|صباح|ص|مساءً|مساء|م|بليل|بالليل|ليل|الليل|ضهر|ظهر|عصر|am|pm)?",
            RegexOption.IGNORE_CASE
        )
        for (m in arHalfRegex.findAll(lower)) {
            val hourStr = m.groupValues[1]
            val rawHour = hourStr.toIntOrNull() ?: arabicWordToNumber[hourStr] ?: continue
            val period = m.groupValues[2]
            candidates.add(RawCandidate(m.range.first, m.range.last + 1, rawHour, 30, period))
        }

        // 4. Arabic "وربع": e.g. "8 وربع" -> minute = 15
        val arQuarterRegex = Regex(
            "(\\d{1,2}|واحدة|واحد|اثنين|إثنين|تنين|ثلاثة|تلاتة|أربعة|اربعة|خمسة|ستة|سبعة|ثمانية|تمانية|تسعة|عشرة|إحدى عشر|احداعش|اثنا عشر|اتناعش)\\s*(?:و|)ربع\\s*(صباحاً|صباحا|صبح|الصبح|صباح|ص|مساءً|مساء|م|بليل|بالليل|ليل|الليل|ضهر|ظهر|عصر|am|pm)?",
            RegexOption.IGNORE_CASE
        )
        for (m in arQuarterRegex.findAll(lower)) {
            val hourStr = m.groupValues[1]
            val rawHour = hourStr.toIntOrNull() ?: arabicWordToNumber[hourStr] ?: continue
            val period = m.groupValues[2]
            candidates.add(RawCandidate(m.range.first, m.range.last + 1, rawHour, 15, period))
        }

        // 5. Exact clock time HH:mm: e.g. "8:30", "10:45"
        val exactClockRegex = Regex(
            "(\\d{1,2}):(\\d{2})\\s*(صباحاً|صباحا|صبح|الصبح|صباح|ص|مساءً|مساء|م|بليل|بالليل|ليل|الليل|ضهر|ظهر|الضهر|عصر|العصر|am|pm)?",
            RegexOption.IGNORE_CASE
        )
        for (m in exactClockRegex.findAll(lower)) {
            val rawHour = m.groupValues[1].toIntOrNull() ?: continue
            val rawMinute = m.groupValues[2].toIntOrNull() ?: continue
            if (rawMinute !in 0..59) continue
            val period = m.groupValues[3]
            candidates.add(RawCandidate(m.range.first, m.range.last + 1, rawHour, rawMinute, period))
        }

        // 6. Plain hour with period or explicit keyword: e.g. "8 مساء", "من 8 إلى 10"
        val plainHourRegex = Regex(
            "(\\d{1,2})\\s*(صباحاً|صباحا|صبح|الصبح|صباح|ص|مساءً|مساء|م|بليل|بالليل|ليل|الليل|ضهر|ظهر|الضهر|عصر|العصر|am|pm)",
            RegexOption.IGNORE_CASE
        )
        for (m in plainHourRegex.findAll(lower)) {
            val rawHour = m.groupValues[1].toIntOrNull() ?: continue
            val period = m.groupValues[2]
            candidates.add(RawCandidate(m.range.first, m.range.last + 1, rawHour, 0, period))
        }

        // 7. Plain digits inside range context: e.g. "من 8 ل10", "8 to 11", "من 8 الى 10"
        val preSeparatorDigitsRegex = Regex(
            "(?<![:\\d])(\\d{1,2})(?!:)\\s*(?:لـ|ل|to|الى|إلى|-|–)",
            RegexOption.IGNORE_CASE
        )
        for (m in preSeparatorDigitsRegex.findAll(lower)) {
            val digitGroup = m.groups[1] ?: continue
            val rawHour = digitGroup.value.toIntOrNull() ?: continue
            candidates.add(RawCandidate(digitGroup.range.first, digitGroup.range.last + 1, rawHour, 0, ""))
        }

        val rangeDigitsRegex = Regex(
            "(?:من|from|لـ|ل|to|الى|إلى|-|–)\\s*(\\d{1,2})(?!:)",
            RegexOption.IGNORE_CASE
        )
        for (m in rangeDigitsRegex.findAll(lower)) {
            val digitGroup = m.groups[1] ?: continue
            val rawHour = digitGroup.value.toIntOrNull() ?: continue
            candidates.add(RawCandidate(digitGroup.range.first, digitGroup.range.last + 1, rawHour, 0, ""))
        }

        // Filter out overlapping candidates, prioritizing more specific matches (fractions, HH:mm)
        val nonOverlapping = mutableListOf<RawCandidate>()
        // Sort by length descending then start index to keep longest / most specific match
        val sortedCandidates = candidates.sortedWith(
            compareByDescending<RawCandidate> { it.endIndex - it.startIndex }
                .thenBy { it.startIndex }
        )
        for (c in sortedCandidates) {
            val overlaps = nonOverlapping.any { existing ->
                maxOf(c.startIndex, existing.startIndex) < minOf(c.endIndex, existing.endIndex)
            }
            if (!overlaps && c.rawHour in 0..23) {
                nonOverlapping.add(c)
            }
        }

        if (nonOverlapping.isEmpty()) return emptyList()

        val ordered = nonOverlapping.sortedBy { it.startIndex }

        val standalonePmToken = Regex("(?:^|\\s|\\d)(م|pm)(?=\\s|$|[.,!])")
        val standaloneAmToken = Regex("(?:^|\\s|\\d)(ص|am)(?=\\s|$|[.,!])")
        val globalEvening = lower.contains("مساء") || lower.contains("بليل") || lower.contains("بالليل") ||
            lower.contains("ليل") || lower.contains("عصر") || standalonePmToken.containsMatchIn(lower)
        val globalMorning = lower.contains("صباح") || lower.contains("صبح") || lower.contains("الصبح") ||
            lower.contains("فجر") || standaloneAmToken.containsMatchIn(lower)

        // Period propagation: if any candidate has explicit evening or morning, propagate to the other candidate in range
        val anyExplicitPm = ordered.any { isPmPeriod(it.periodStr) }
        val anyExplicitAm = ordered.any { isAmPeriod(it.periodStr) }

        return ordered.mapIndexed { index, item ->
            val directPm = isPmPeriod(item.periodStr)
            val directAm = isAmPeriod(item.periodStr)

            val isPm = when {
                directPm -> true
                directAm -> false
                anyExplicitPm && !anyExplicitAm -> true
                anyExplicitAm && !anyExplicitPm -> false
                globalEvening && !globalMorning -> true
                globalMorning && !globalEvening -> false
                else -> false
            }

            val isAm = when {
                directAm -> true
                directPm -> false
                anyExplicitAm && !anyExplicitPm -> true
                anyExplicitPm && !anyExplicitAm -> false
                globalMorning && !globalEvening -> true
                globalEvening && !globalMorning -> false
                else -> false
            }

            val hour = when {
                isPm && item.rawHour < 12 -> item.rawHour + 12
                isAm && item.rawHour == 12 -> 0
                else -> item.rawHour
            }.coerceIn(0, 23)

            ParsedTimePoint(
                hour24 = hour,
                minute = item.minute.coerceIn(0, 59),
                startIndex = item.startIndex
            )
        }
    }

    private fun isPmPeriod(period: String): Boolean {
        val p = period.lowercase().trim()
        if (p.isEmpty()) return false
        return p.contains("مساء") || p.contains("بليل") || p.contains("ليل") || p == "م" ||
            p == "pm" || p.contains("عصر") || p.contains("ضهر") || p.contains("ظهر")
    }

    private fun isAmPeriod(period: String): Boolean {
        val p = period.lowercase().trim()
        if (p.isEmpty()) return false
        return p.contains("صباح") || p.contains("صبح") || p == "ص" || p == "am" || p.contains("فجر")
    }

    fun extractTimeHours(text: String): List<Int> {
        return extractTimePoints(text).map { it.hour24 }
    }

    fun parseTimeRange(text: String): Pair<Int, Int>? {
        val points = extractTimePoints(text)
        if (points.isEmpty()) return null
        val start = points.first()
        val end = if (points.size >= 2) points[1] else ParsedTimePoint((start.hour24 + 3) % 24, start.minute)
        return start.hour24 to end.hour24
    }

    fun parseDetailedTimeRange(text: String): ParsedDetailedRange? {
        val points = extractTimePoints(text)
        if (points.isEmpty()) return null
        val start = points.first()
        val end = if (points.size >= 2) {
            points[1]
        } else {
            ParsedTimePoint((start.hour24 + 2) % 24, start.minute)
        }
        return ParsedDetailedRange(
            startHour = start.hour24,
            startMinute = start.minute,
            endHour = end.hour24,
            endMinute = end.minute
        )
    }

    fun hasExplicitPeriod(text: String): Boolean {
        val lower = text.lowercase()
        val keywords = listOf(
            "صباح", "صبح", "الصبح", "مساء", "بليل", "بالليل", "ليل", "الليل",
            "ضهر", "ظهر", "الضهر", "عصر", "العصر"
        )
        if (keywords.any { lower.contains(it) }) return true
        return Regex("(?:^|\\s|\\d)(ص|م|am|pm)(?=\\s|$|[.,!])").containsMatchIn(lower)
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

