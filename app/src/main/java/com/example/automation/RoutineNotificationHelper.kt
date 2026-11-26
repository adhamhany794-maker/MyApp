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
}
