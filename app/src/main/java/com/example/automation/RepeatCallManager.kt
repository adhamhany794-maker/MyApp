package com.example.automation

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.core.content.ContextCompat
import com.example.models.PriorityContact
import com.example.models.RepeatCallerRule
import kotlin.math.abs

enum class RepeatCallRuleSource {
    EMERGENCY_CONTACT,
    PER_PERSON_RULE,
    GLOBAL_SETTING,
    DISABLED
}

data class EffectiveRepeatPolicy(
    val enabled: Boolean,
    val requiredCalls: Int,
    val windowMinutes: Int,
    val source: RepeatCallRuleSource,
    val matchedRule: RepeatCallerRule? = null,
    val useOsRepeatCallersPolicy: Boolean = false
)

data class RepeatCallEvaluation(
    val phoneNumber: String,
    val normalizedNumber: String,
    val callCountInWindow: Int,
    val requiredCalls: Int,
    val windowMinutes: Int,
    val shouldAllowBypass: Boolean,
    val usedOsRepeatCallersPolicy: Boolean,
    val source: RepeatCallRuleSource,
    val matchedRule: RepeatCallerRule? = null,
    val matchedEmergencyContact: PriorityContact? = null
)

object RepeatCallPolicyResolver {

    const val DEFAULT_REQUIRED_CALLS = 2
    const val DEFAULT_WINDOW_MINUTES = 15

    /**
     * Android's native PRIORITY_CATEGORY_REPEAT_CALLERS policy is hardcoded by the OS
     * to allow a second call from the same number within 15 minutes.
     */
    fun shouldUseOsRepeatCallersPolicy(
        globalEnabled: Boolean,
        globalRequiredCalls: Int,
        globalWindowMinutes: Int
    ): Boolean {
        return globalEnabled &&
            globalRequiredCalls == DEFAULT_REQUIRED_CALLS &&
            globalWindowMinutes == DEFAULT_WINDOW_MINUTES
    }

    fun resolvePolicyForNumber(
        phoneNumber: String,
        perPersonRules: List<RepeatCallerRule>,
        globalEnabled: Boolean,
        globalRequiredCalls: Int,
        globalWindowMinutes: Int
    ): EffectiveRepeatPolicy {
        val perPerson = perPersonRules.firstOrNull { rule ->
            rule.isEnabled &&
                rule.phoneNumber.isNotBlank() &&
                EmergencyPhoneNumberUtils.numbersMatch(rule.phoneNumber, phoneNumber)
        }
        if (perPerson != null) {
            return EffectiveRepeatPolicy(
                enabled = true,
                requiredCalls = perPerson.requiredCalls.coerceIn(1, 50),
                windowMinutes = perPerson.windowMinutes.coerceIn(1, 240),
                source = RepeatCallRuleSource.PER_PERSON_RULE,
                matchedRule = perPerson,
                useOsRepeatCallersPolicy = false
            )
        }

        if (globalEnabled) {
            val req = globalRequiredCalls.coerceIn(1, 50)
            val win = globalWindowMinutes.coerceIn(1, 240)
            return EffectiveRepeatPolicy(
                enabled = true,
                requiredCalls = req,
                windowMinutes = win,
                source = RepeatCallRuleSource.GLOBAL_SETTING,
                matchedRule = null,
                useOsRepeatCallersPolicy = shouldUseOsRepeatCallersPolicy(true, req, win)
            )
        }

        return EffectiveRepeatPolicy(
            enabled = false,
            requiredCalls = globalRequiredCalls.coerceIn(1, 50),
            windowMinutes = globalWindowMinutes.coerceIn(1, 240),
            source = RepeatCallRuleSource.DISABLED,
            matchedRule = null,
            useOsRepeatCallersPolicy = false
        )
    }
}

interface RepeatCallHistoryStore {
    fun recordCallAndPrune(
        normalizedNumber: String,
        timestampMillis: Long,
        windowMillis: Long,
        seedTimestamps: List<Long> = emptyList(),
        dedupeGapMillis: Long = 2_000L
    ): List<Long>

    fun getRecentCalls(
        normalizedNumber: String,
        nowMillis: Long,
        windowMillis: Long
    ): List<Long>

    fun clearAll()
}

class InMemoryRepeatCallHistoryStore : RepeatCallHistoryStore {
    private val buckets = linkedMapOf<String, MutableList<Long>>()

    @Synchronized
    override fun recordCallAndPrune(
        normalizedNumber: String,
        timestampMillis: Long,
        windowMillis: Long,
        seedTimestamps: List<Long>,
        dedupeGapMillis: Long
    ): List<Long> {
        if (normalizedNumber.isBlank()) return emptyList()
        val key = findMatchingBucketKey(normalizedNumber) ?: normalizedNumber
        val cutoff = timestampMillis - windowMillis.coerceAtLeast(1_000L)
        val merged = buckets.getOrPut(key) { mutableListOf() }
            .filter { it >= cutoff && it <= timestampMillis }
            .toMutableList()

        for (seedTs in seedTimestamps) {
            if (seedTs in cutoff..timestampMillis && merged.none { abs(seedTs - it) <= dedupeGapMillis }) {
                merged.add(seedTs)
            }
        }
        if (merged.none { abs(timestampMillis - it) <= dedupeGapMillis }) {
            merged.add(timestampMillis)
        }
        merged.sort()
        buckets[key] = merged
        return merged.toList()
    }

    @Synchronized
    override fun getRecentCalls(
        normalizedNumber: String,
        nowMillis: Long,
        windowMillis: Long
    ): List<Long> {
        if (normalizedNumber.isBlank()) return emptyList()
        val key = findMatchingBucketKey(normalizedNumber) ?: return emptyList()
        val cutoff = nowMillis - windowMillis.coerceAtLeast(1_000L)
        val pruned = buckets[key].orEmpty().filter { it in cutoff..nowMillis }.sorted()
        if (pruned.isEmpty()) {
            buckets.remove(key)
        } else {
            buckets[key] = pruned.toMutableList()
        }
        return pruned
    }

    @Synchronized
    override fun clearAll() {
        buckets.clear()
    }

    private fun findMatchingBucketKey(normalizedNumber: String): String? {
        return buckets.keys.firstOrNull { existingKey ->
            EmergencyPhoneNumberUtils.numbersMatch(existingKey, normalizedNumber)
        }
    }
}

class SharedPreferencesRepeatCallHistoryStore(
    context: Context
) : RepeatCallHistoryStore {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("repeat_call_history_prefs", Context.MODE_PRIVATE)

    @Synchronized
    override fun recordCallAndPrune(
        normalizedNumber: String,
        timestampMillis: Long,
        windowMillis: Long,
        seedTimestamps: List<Long>,
        dedupeGapMillis: Long
    ): List<Long> {
        if (normalizedNumber.isBlank()) return emptyList()
        val all = readAllBuckets().toMutableMap()
        val key = all.keys.firstOrNull { existing ->
            EmergencyPhoneNumberUtils.numbersMatch(existing, normalizedNumber)
        } ?: normalizedNumber

        val cutoff = timestampMillis - windowMillis.coerceAtLeast(1_000L)
        val merged = all[key].orEmpty()
            .filter { it in cutoff..timestampMillis }
            .toMutableList()

        for (seedTs in seedTimestamps) {
            if (seedTs in cutoff..timestampMillis && merged.none { abs(seedTs - it) <= dedupeGapMillis }) {
                merged.add(seedTs)
            }
        }
        if (merged.none { abs(timestampMillis - it) <= dedupeGapMillis }) {
            merged.add(timestampMillis)
        }
        merged.sort()
        all[key] = merged
        writeAllBuckets(all, nowMillis = timestampMillis, maxRetentionMillis = MAX_RETENTION_MILLIS)
        return merged.toList()
    }

    @Synchronized
    override fun getRecentCalls(
        normalizedNumber: String,
        nowMillis: Long,
        windowMillis: Long
    ): List<Long> {
        if (normalizedNumber.isBlank()) return emptyList()
        val all = readAllBuckets()
        val key = all.keys.firstOrNull { existing ->
            EmergencyPhoneNumberUtils.numbersMatch(existing, normalizedNumber)
        } ?: return emptyList()
        val cutoff = nowMillis - windowMillis.coerceAtLeast(1_000L)
        return all[key].orEmpty().filter { it in cutoff..nowMillis }.sorted()
    }

    @Synchronized
    override fun clearAll() {
        prefs.edit().clear().commit()
    }

    private fun readAllBuckets(): Map<String, List<Long>> {
        val result = linkedMapOf<String, List<Long>>()
        for ((key, value) in prefs.all) {
            if (!key.startsWith(KEY_PREFIX)) continue
            val numberKey = key.removePrefix(KEY_PREFIX)
            val raw = value as? String ?: continue
            val timestamps = raw.split(",").mapNotNull { it.trim().toLongOrNull() }
            if (numberKey.isNotBlank() && timestamps.isNotEmpty()) {
                result[numberKey] = timestamps
            }
        }
        return result
    }

    private fun writeAllBuckets(
        buckets: Map<String, List<Long>>,
        nowMillis: Long,
        maxRetentionMillis: Long
    ) {
        val cutoff = nowMillis - maxRetentionMillis
        val editor = prefs.edit().clear()
        for ((numberKey, timestamps) in buckets) {
            val fresh = timestamps.filter { it >= cutoff }.sorted()
            if (fresh.isNotEmpty()) {
                editor.putString(KEY_PREFIX + numberKey, fresh.joinToString(","))
            }
        }
        editor.commit()
    }

    private companion object {
        const val KEY_PREFIX = "bucket_"
        const val MAX_RETENTION_MILLIS = 6L * 60L * 60L * 1000L
    }
}

class RepeatCallTracker(
    private val historyStore: RepeatCallHistoryStore = InMemoryRepeatCallHistoryStore()
) {
    fun recordAndEvaluateIncomingCall(
        phoneNumber: String,
        timestampMillis: Long,
        emergencyContacts: List<PriorityContact>,
        perPersonRules: List<RepeatCallerRule>,
        globalEnabled: Boolean,
        globalRequiredCalls: Int,
        globalWindowMinutes: Int,
        seedCallLogTimestamps: List<Long> = emptyList(),
        dedupeGapMillis: Long = 2_000L
    ): RepeatCallEvaluation {
        val normalized = EmergencyPhoneNumberUtils.normalizePhoneNumber(phoneNumber)

        val matchedEmergency = if (normalized.isNotBlank()) {
            EmergencyPhoneNumberUtils.findMatchingEmergencyContact(
                senderName = null,
                senderPhoneNumber = phoneNumber,
                contacts = emergencyContacts
            )
        } else {
            null
        }
        if (matchedEmergency != null) {
            return RepeatCallEvaluation(
                phoneNumber = phoneNumber,
                normalizedNumber = normalized,
                callCountInWindow = 1,
                requiredCalls = 1,
                windowMinutes = 1,
                shouldAllowBypass = true,
                usedOsRepeatCallersPolicy = false,
                source = RepeatCallRuleSource.EMERGENCY_CONTACT,
                matchedRule = null,
                matchedEmergencyContact = matchedEmergency
            )
        }

        val policy = RepeatCallPolicyResolver.resolvePolicyForNumber(
            phoneNumber = phoneNumber,
            perPersonRules = perPersonRules,
            globalEnabled = globalEnabled,
            globalRequiredCalls = globalRequiredCalls,
            globalWindowMinutes = globalWindowMinutes
        )

        if (!policy.enabled || normalized.isBlank()) {
            return RepeatCallEvaluation(
                phoneNumber = phoneNumber,
                normalizedNumber = normalized,
                callCountInWindow = 0,
                requiredCalls = policy.requiredCalls,
                windowMinutes = policy.windowMinutes,
                shouldAllowBypass = false,
                usedOsRepeatCallersPolicy = false,
                source = policy.source,
                matchedRule = policy.matchedRule,
                matchedEmergencyContact = null
            )
        }

        val windowMillis = policy.windowMinutes * 60_000L
        val timestampsInWindow = historyStore.recordCallAndPrune(
            normalizedNumber = normalized,
            timestampMillis = timestampMillis,
            windowMillis = windowMillis,
            seedTimestamps = seedCallLogTimestamps,
            dedupeGapMillis = dedupeGapMillis
        )
        val count = timestampsInWindow.size
        val thresholdMet = count >= policy.requiredCalls

        return RepeatCallEvaluation(
            phoneNumber = phoneNumber,
            normalizedNumber = normalized,
            callCountInWindow = count,
            requiredCalls = policy.requiredCalls,
            windowMinutes = policy.windowMinutes,
            shouldAllowBypass = thresholdMet,
            usedOsRepeatCallersPolicy = thresholdMet && policy.useOsRepeatCallersPolicy,
            source = policy.source,
            matchedRule = policy.matchedRule,
            matchedEmergencyContact = null
        )
    }

    fun getCurrentCountInWindow(
        phoneNumber: String,
        windowMinutes: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        val normalized = EmergencyPhoneNumberUtils.normalizePhoneNumber(phoneNumber)
        if (normalized.isBlank()) return 0
        return historyStore.getRecentCalls(
            normalizedNumber = normalized,
            nowMillis = nowMillis,
            windowMillis = windowMinutes.coerceAtLeast(1) * 60_000L
        ).size
    }

    fun clearHistory() {
        historyStore.clearAll()
    }
}

object CallLogRepeatReader {

    fun hasReadCallLogPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasReadPhoneStatePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun readRecentCallTimestampsForNumber(
        context: Context,
        phoneNumber: String,
        windowMinutes: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): List<Long> {
        if (!hasReadCallLogPermission(context) || phoneNumber.isBlank()) return emptyList()
        val cutoff = nowMillis - windowMinutes.coerceAtLeast(1) * 60_000L
        val results = mutableListOf<Long>()

        runCatching {
            val projection = arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.DATE,
                CallLog.Calls.TYPE
            )
            val selection = "${CallLog.Calls.DATE} >= ? AND (${CallLog.Calls.TYPE} = ? OR ${CallLog.Calls.TYPE} = ? OR ${CallLog.Calls.TYPE} = ?)"
            val selectionArgs = arrayOf(
                cutoff.toString(),
                CallLog.Calls.INCOMING_TYPE.toString(),
                CallLog.Calls.MISSED_TYPE.toString(),
                CallLog.Calls.REJECTED_TYPE.toString()
            )
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                val numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                while (cursor.moveToNext()) {
                    val number = if (numberIdx >= 0) cursor.getString(numberIdx).orEmpty() else ""
                    val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else 0L
                    if (date >= cutoff && EmergencyPhoneNumberUtils.numbersMatch(number, phoneNumber)) {
                        results.add(date)
                    }
                }
            }
        }
        return results.sorted()
    }
}
