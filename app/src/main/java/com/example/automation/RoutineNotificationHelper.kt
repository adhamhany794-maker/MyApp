package com.example.automation

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R

object RoutineNotificationHelper {
    private const val CHANNEL_ID = "routine_phone_control_alerts"
    private const val DND_ACCESS_NOTIFICATION_ID = 9001

    fun showDndPermissionRequiredNotification(
        context: Context,
        routineName: String,
        isArabic: Boolean
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = if (isArabic) "تنبيهات الروتين والتحكم بالهاتف" else "Routine & Phone Control Alerts"
            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = if (isArabic) {
                    "تنبيهات عند الحاجة إلى إذن عدم الإزعاج لتشغيل الروتينات"
                } else {
                    "Alerts when Do Not Disturb access is needed to run routines"
                }
            }
            manager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val settingsIntent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            DND_ACCESS_NOTIFICATION_ID,
            settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isArabic) {
            "مطلوب إذن عدم الإزعاج للروتين"
        } else {
            "Do Not Disturb access needed"
        }
        val body = if (isArabic) {
            "تعذر تطبيق \"$routineName\" لأن إذن الوصول إلى عدم الإزعاج غير ممنوح. اضغط لمنح الإذن."
        } else {
            "Could not apply \"$routineName\" because Do Not Disturb access is not granted. Tap to grant access."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(DND_ACCESS_NOTIFICATION_ID, notification)
        }
    }

    private const val EMERGENCY_BYPASS_CHANNEL_ID = "emergency_contacts_bypass_alerts"
    private const val EMERGENCY_BYPASS_NOTIFICATION_ID = 9002

    fun showEmergencyBypassAlert(
        context: Context,
        contactName: String,
        phoneNumber: String,
        isCall: Boolean,
        isArabic: Boolean
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = if (isArabic) "تنبيهات جهات الطوارئ" else "Emergency Contact Alerts"
            val channel = NotificationChannel(
                EMERGENCY_BYPASS_CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = if (isArabic) {
                    "تنبيهات عاجلة لجهات الطوارئ تتجاوز الصامت وعدم الإزعاج"
                } else {
                    "Urgent alerts from emergency contacts that bypass Silent and Do Not Disturb"
                }
                setBypassDnd(true)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val title = if (isArabic) {
            if (isCall) "مكالمة طوارئ: $contactName" else "رسالة طوارئ: $contactName"
        } else {
            if (isCall) "Emergency call: $contactName" else "Emergency message: $contactName"
        }
        val body = if (isArabic) {
            val numPart = if (phoneNumber.isNotBlank()) " ($phoneNumber)" else ""
            "جهة طوارئ$numPart — هيوصلك دايما رغم أي روتين أو وضع صامت."
        } else {
            val numPart = if (phoneNumber.isNotBlank()) " ($phoneNumber)" else ""
            "Emergency contact$numPart bypassed active routine/Silent mode."
        }

        val category = if (isCall) NotificationCompat.CATEGORY_CALL else NotificationCompat.CATEGORY_MESSAGE

        val notification = NotificationCompat.Builder(context, EMERGENCY_BYPASS_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(category)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(EMERGENCY_BYPASS_NOTIFICATION_ID, notification)
        }
    }
}
