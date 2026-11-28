package com.example.models

enum class SoundMode(val displayName: String, val arabicName: String) {
    SOUND("Sound", "صوت"),
    VIBRATE("Vibrate", "اهتزاز"),
    SILENT("Silent", "صامت")
}

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}

enum class AppLanguage(val label: String) {
    ENGLISH("English"),
    ARABIC("العربية")
}

enum class ProfileIconKey {
    STUDY,
    WORK,
    SLEEP,
    PERSONAL,
    CUSTOM
}

enum class NotificationCategory(val displayName: String) {
    COMMUNICATION("Communication"),
    SOCIAL_MEDIA("Social Media"),
    WORK("Work"),
    EDUCATION("Education"),
    ENTERTAINMENT("Entertainment"),
    SYSTEM("System"),
    OTHER("Other")
}

enum class ContactPriority(val displayName: String, val rank: Int) {
    EMERGENCY("Emergency", 3)
}

enum class InterruptionType(val displayName: String) {
    CALL("Calls"),
    MESSAGE("Messages")
}

enum class TriggerType(val displayName: String) {
    SPECIFIC_TIME("Specific Time"),
    SPECIFIC_DAYS("Specific Days"),
    DATE_AND_TIME("Date & Time"),
    MANUAL("Manual Activation"),
    FOCUS_STARTED("Focus Session Started"),
    FOCUS_ENDED("Focus Session Ended"),
    LOCATION_ENTERED("Simulated Location Entered"),
    LOCATION_EXITED("Simulated Location Exited"),
    ACTIVITY_STARTED("Simulated Activity Started"),
    ACTIVITY_ENDED("Simulated Activity Ended")
}

enum class ConditionType(val displayName: String) {
    DAY_OF_WEEK("Day of Week"),
    TIME_RANGE("Time Range"),
    ACTIVE_PROFILE("Active Profile"),
    AUTOMATION_ENABLED("Automation Status"),
    SIMULATED_LOCATION("Simulated Location"),
    SIMULATED_ACTIVITY("Simulated Activity"),
    FOCUS_SESSION_ACTIVE("Focus Session Status")
}

enum class ActionType(val displayName: String) {
    ACTIVATE_PROFILE("Activate Profile"),
    SET_SOUND_MODE("Set Sound Mode"),
    START_FOCUS_SESSION("Start Focus Session"),
    END_FOCUS_SESSION("End Focus Session"),
    PAUSE_AUTOMATION("Pause Automation"),
    RESUME_AUTOMATION("Resume Automation"),
    SHOW_NOTIFICATION("Show In-App Notification"),
    RECORD_ACTIVITY("Record Activity Event")
}

enum class ExceptionType(val displayName: String) {
    PRIORITY_CONTACTS("Priority Contacts"),
    EMERGENCY_CONTACTS("Emergency Contacts"),
    SPECIFIC_DAYS("Specific Days"),
    TIME_RANGE("Time Range"),
    MANUALLY_DISABLED("Manually Disabled Rule")
}

enum class SimulatedLocation(val displayName: String) {
    HOME("Home"),
    UNIVERSITY("University"),
    WORKPLACE("Workplace"),
    LIBRARY("Library"),
    CUSTOM("Custom Location")
}

enum class SimulatedActivity(val displayName: String) {
    STUDYING("Studying"),
    WORKING("Working"),
    SLEEPING("Sleeping"),
    DRIVING("Driving"),
    PERSONAL_TIME("Personal Time"),
    CUSTOM("Custom Activity")
}

enum class ConflictResolutionPolicy(val displayName: String) {
    ASK_USER("Ask Me to Confirm"),
    HIGHEST_PRIORITY("Auto-Select Highest Priority"),
    KEEP_CURRENT("Keep Current State")
}

enum class EventCategory(val displayName: String) {
    PROFILE("Profile"),
    ROUTINE("Routine"),
    AUTOMATION("Automation"),
    SOUND_MODE("Sound Mode"),
    FOCUS("Focus"),
    CONTACT_EXCEPTION("Contact Exception"),
    CONTEXT("Context"),
    CONFLICT("Conflict"),
    ASSISTANT("Assistant"),
    SYSTEM("System")
}

enum class DayFilter(val label: String, val shortLabel: String) {
    EVERY_DAY("Every Day", "Daily"),
    WEEKDAYS("Weekdays", "Mon–Fri"),
    WEEKENDS("Weekends", "Sat–Sun"),
    MONDAY("Monday", "Mon"),
    TUESDAY("Tuesday", "Tue"),
    WEDNESDAY("Wednesday", "Wed"),
    THURSDAY("Thursday", "Thu"),
    FRIDAY("Friday", "Fri"),
    SATURDAY("Saturday", "Sat"),
    SUNDAY("Sunday", "Sun")
}

enum class TimePeriodFilter(val label: String) {
    TODAY("Today"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days")
}

enum class RoutineLogEventType {
    STARTED,
    ENDED,
    PERMISSION_DENIED,
    NO_ACTION
}

