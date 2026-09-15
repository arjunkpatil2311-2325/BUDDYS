package com.aura.glasschat.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.aura.glasschat.data.model.Message
import com.aura.glasschat.receiver.MessageReminderReceiver

object MessageReminderManager {

    fun scheduleReminder(
        context: Context,
        chatId: String,
        message: Message,
        senderName: String,
        triggerAtMillis: Long,
        privacySetting: String = "SHOW_ALL"
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MessageReminderReceiver::class.java).apply {
            putExtra(MessageReminderReceiver.EXTRA_CHAT_ID, chatId)
            putExtra(MessageReminderReceiver.EXTRA_MESSAGE_ID, message.id)
            putExtra(MessageReminderReceiver.EXTRA_SENDER_NAME, senderName)
            putExtra(MessageReminderReceiver.EXTRA_MESSAGE_TEXT, message.text)
            putExtra(MessageReminderReceiver.EXTRA_PRIVACY_SETTING, privacySetting)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            message.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(context: Context, messageId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MessageReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            messageId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
