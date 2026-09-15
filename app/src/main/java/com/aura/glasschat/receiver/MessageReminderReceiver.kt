package com.aura.glasschat.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aura.glasschat.MainActivity
import com.aura.glasschat.R

class MessageReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val chatId = intent.getStringExtra(EXTRA_CHAT_ID) ?: return
        val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID) ?: ""
        val senderName = intent.getStringExtra(EXTRA_SENDER_NAME) ?: "Buddies"
        val messageText = intent.getStringExtra(EXTRA_MESSAGE_TEXT) ?: "You have a message reminder"
        val privacySetting = intent.getStringExtra(EXTRA_PRIVACY_SETTING) ?: "SHOW_ALL"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "buddys_reminders_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Message Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for starred or saved messages"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to_chat_id", chatId)
            putExtra("highlight_message_id", messageId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            messageId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (privacySetting) {
            "HIDE_PREVIEW" -> "Buddies"
            else -> "Reminder: $senderName"
        }

        val content = when (privacySetting) {
            "HIDE_PREVIEW" -> "You have a new message reminder"
            "SENDER_ONLY" -> "Message reminder from $senderName"
            else -> messageText
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(messageId.hashCode(), notification)
    }

    companion object {
        const val EXTRA_CHAT_ID = "extra_chat_id"
        const val EXTRA_MESSAGE_ID = "extra_message_id"
        const val EXTRA_SENDER_NAME = "extra_sender_name"
        const val EXTRA_MESSAGE_TEXT = "extra_message_text"
        const val EXTRA_PRIVACY_SETTING = "extra_privacy_setting"
    }
}
