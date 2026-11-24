package com.tippool

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit

// ─── Public scheduling API ────────────────────────────────────────────────────

fun scheduleSmartReminder(context: Context, settings: AppSettings) {
    val now = Calendar.getInstance()
    val hour = now.get(Calendar.HOUR_OF_DAY)
    val isSaturday = now.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY

    if (hour < 17) {
        if (!settings.morningReminderEnabled) return
        val items = buildList {
            addAll(settings.morningReminderItems)
            if (isSaturday) addAll(settings.saturdayExtraItems)
        }
        if (items.isEmpty()) return
        scheduleReminder(
            context      = context,
            title        = if (isSaturday) "Saturday Catering Check" else "Morning Checklist",
            delayMinutes = settings.morningReminderDelayMinutes,
            items        = items,
            notifId      = NOTIF_ID_MORNING,
        )
    } else {
        if (!settings.closingReminderEnabled) return
        if (settings.closingReminderItems.isEmpty()) return
        scheduleReminder(
            context      = context,
            title        = "Closing Checklist",
            delayMinutes = settings.closingReminderDelayMinutes,
            items        = settings.closingReminderItems,
            notifId      = NOTIF_ID_CLOSING,
        )
    }
}

// ─── Internal helpers ─────────────────────────────────────────────────────────

private const val NOTIF_ID_MORNING = 41
private const val NOTIF_ID_CLOSING = 42

private const val KEY_TITLE    = "title"
private const val KEY_ITEMS    = "items"
private const val KEY_NOTIF_ID = "notif_id"

private fun scheduleReminder(
    context: Context,
    title: String,
    delayMinutes: Int,
    items: List<String>,
    notifId: Int,
) {
    val inputData = Data.Builder()
        .putString(KEY_TITLE, title)
        .putString(KEY_ITEMS, items.joinToString("\n"))
        .putInt(KEY_NOTIF_ID, notifId)
        .build()

    val request = OneTimeWorkRequestBuilder<ReminderWorker>()
        .setInputData(inputData)
        .apply {
            if (delayMinutes > 0) setInitialDelay(delayMinutes.toLong(), TimeUnit.MINUTES)
        }
        .build()

    WorkManager.getInstance(context).enqueue(request)
}

// ─── Worker ───────────────────────────────────────────────────────────────────

class ReminderWorker(
    private val context: Context,
    params: WorkerParameters,
) : Worker(context, params) {

    override fun doWork(): Result {
        val title     = inputData.getString(KEY_TITLE)  ?: "Reminder"
        val rawItems  = inputData.getString(KEY_ITEMS)  ?: ""
        val notifId   = inputData.getInt(KEY_NOTIF_ID, NOTIF_ID_CLOSING)

        val items      = rawItems.split("\n").filter { it.isNotBlank() }
        val bulletText = items.joinToString("\n") { "• $it" }
        val shortText  = items.firstOrNull() ?: ""

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notifId, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(shortText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bulletText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notifId, notification)

        return Result.success()
    }
}