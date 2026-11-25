package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "smart_profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val iconKey: String,
    val accentColorHex: Long,
    val soundMode: String,
    val allowedCategoriesJson: String,
    val priorityContactBehavior: String,
    val allowHighPriorityContacts: Boolean,
    val defaultFocusMinutes: Int,
    val isDefault: Boolean,
    val isProtected: Boolean
)

@Entity(tableName = "smart_routines")
data class RoutineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val isEnabled: Boolean,
    val triggerJson: String,
    val conditionsJson: String,
    val actionsJson: String,
    val exceptionsJson: String,
    val priority: Int,
    val scheduleStartTime: String,
    val scheduleEndTime: String,
    val targetProfileId: String?,
    val lastExecutionResult: String,
    val lastExecutedAtMillis: Long?,
    val executionCount: Int,
    val skippedCount: Int
)

@Entity(tableName = "priority_contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val initials: String,
    val relationship: String,
    val priorityLevel: String,
    val allowedInterruptionsJson: String,
    val isEnabled: Boolean,
    val allowRepeatCallers: Boolean
)

@Entity(tableName = "activity_events")
data class ActivityEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val timestampMillis: Long,
    val category: String,
    val triggerSource: String,
    val result: String,
    val explanation: String,
    val relatedRoutineName: String?,
    val relatedProfileName: String?
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val profileName: String,
    val plannedMinutes: Int,
    val completedMinutes: Int,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val simulatedInterruptionsBlocked: Int,
    val status: String
)

@Entity(tableName = "notification_stats")
data class NotificationStatEntity(
    @PrimaryKey val id: String,
    val appName: String,
    val category: String,
    val totalCount: Int,
    val importantCount: Int,
    val blockedCount: Int,
    val peakHourRange: String,
    val timePeriod: String
)
