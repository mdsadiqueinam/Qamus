package io.github.mdsadiqueinam.qamus.receiver

import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import io.github.mdsadiqueinam.qamus.R
import io.github.mdsadiqueinam.qamus.ui.activity.ReminderActivity
import androidx.core.net.toUri

/**
 * BroadcastReceiver to handle screen state changes and show Kalima reminders accordingly.
 * If the device is locked or the screen is off, it starts the ReminderActivity directly.
 * Otherwise, it shows a notification that the user can tap to open the ReminderActivity.
 */
class ScreenStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ScreenStateReceiver"
        private const val NOTIFICATION_CHANNEL_ID = "KalimaReminderChannel"
        private const val NOTIFICATION_ID = 1
        // Add to companion object in ScreenStateReceiver
        const val ACTION_SHOW_REMINDER = "io.github.mdsadiqueinam.qamus.action.SHOW_REMINDER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")
        val useNotificationForNow = true
        // Check if the device is locked or the screen is off
        val keyguardManager = context.getSystemService<KeyguardManager>()
        val powerManager = context.getSystemService<PowerManager>()

        val isDeviceLocked = keyguardManager?.isKeyguardLocked == true
        val isScreenOn = powerManager?.isInteractive != false

        if ((isDeviceLocked || !isScreenOn) && !useNotificationForNow) {
            // If device is locked or screen is off, start the ReminderActivity directly
            Log.d(TAG, "Device is locked or screen is off, starting ReminderActivity")
            val activityIntent = ReminderActivity.createIntent(context, showWhenLocked = true)
            context.startActivity(activityIntent, null)
        } else {
            // If device is in use, show a notification
            Log.d(TAG, "Device is in use, showing notification")
            showNotification(context)
        }
    }

    /**
     * Show a notification that the user can tap to open the ReminderActivity.
     */
    private fun showNotification(context: Context) {
        val notificationManager = context.getSystemService<NotificationManager>()

        if (notificationManager == null) {
            Log.w(TAG, "NotificationManager not available, cannot show notification")
            return
        }

        // Create notification channel (required for Android 8.0+)
        createNotificationChannel(context)

        // Create intent for notification tap action
        val intent = ReminderActivity.createIntent(context)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.kalima_reminder_notification))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setAutoCancel(true)
            .build()

        // Show the notification
        notificationManager.notify(NOTIFICATION_ID, notification)

        Log.d(TAG, "Notified user with reminder notification")
    }

    /**
     * Create the notification channel for the reminder notifications.
     */
    private fun createNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService<NotificationManager>() ?: return
        val soundUri = ("android.resource://" + context.packageName + "/" + R.raw.notification_sound).toUri()

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.kalima_reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.kalima_reminder_channel_description)
            setSound(soundUri, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
            enableVibration(true)
            enableLights(true)
        }

        notificationManager.createNotificationChannel(channel)
    }
}