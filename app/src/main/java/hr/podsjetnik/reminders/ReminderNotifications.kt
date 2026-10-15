package hr.podsjetnik.reminders

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import hr.podsjetnik.MainActivity
import hr.podsjetnik.R
import hr.podsjetnik.domain.Occurrence
import hr.podsjetnik.domain.RecurrenceRule
import hr.podsjetnik.domain.Settings
import hr.podsjetnik.domain.Stage
import hr.podsjetnik.domain.SummaryPost
import hr.podsjetnik.domain.Task
import hr.podsjetnik.ui.components.formatDate
import hr.podsjetnik.ui.components.formatTime
import hr.podsjetnik.ui.theme.LightPodsjetnikColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Builds, posts and cancels notifications (PRD §9). One reminder notification per open occurrence
 * (tag "reminder", id = occurrence id), updated in place as it escalates.
 */
class ReminderNotifications(private val context: Context) {
    private val compat = NotificationManagerCompat.from(context)
    private val system = context.getSystemService(NotificationManager::class.java)
    private val res = context.resources

    fun postReminder(
        task: Task,
        occurrence: Occurrence,
        openCount: Int,
        stage: Stage,
        alert: Boolean,
        settings: Settings,
        at: Instant,
        zone: ZoneId,
    ) {
        val today = at.atZone(zone).toLocalDate()
        val name = if (openCount > 1) {
            context.getString(R.string.notif_naslov_mjesec, task.title, monthName(occurrence.dueDate))
        } else {
            task.title
        }
        val (channel, title, text, color) = when (stage) {
            Stage.EARLY -> {
                val days = ChronoUnit.DAYS.between(today, occurrence.dueDate).toInt()
                val date = longDate(occurrence.dueDate, today)
                Content(
                    NotificationFactory.CHANNEL_RANI,
                    res.getQuantityString(R.plurals.notif_rano_naslov, days, days, name),
                    task.recurrence?.let { context.getString(R.string.notif_rano_tekst_pravilo, date, lowerFirst(ruleText(it))) }
                        ?: context.getString(R.string.notif_rano_tekst, date),
                    LightPodsjetnikColors.pri,
                )
            }
            Stage.DUE -> {
                val next = nextReminderToday(task, occurrence, settings, at, zone)
                Content(
                    NotificationFactory.CHANNEL_NA_DAN,
                    context.getString(R.string.notif_danas_naslov, name),
                    next?.let { context.getString(R.string.notif_danas_tekst, formatTime(it)) }
                        ?: context.getString(R.string.notif_danas_tekst_zadnji),
                    LightPodsjetnikColors.ambDot,
                )
            }
            Stage.OVERDUE -> {
                val daysLate = ChronoUnit.DAYS.between(occurrence.dueDate, today).toInt()
                Content(
                    NotificationFactory.CHANNEL_KASNI,
                    if (daysLate == 0) {
                        context.getString(R.string.notif_rok_istekao_naslov, name)
                    } else {
                        res.getQuantityString(R.plurals.notif_kasni_naslov, daysLate, daysLate, name)
                    },
                    context.getString(
                        R.string.notif_kasni_tekst,
                        formatDate(occurrence.dueDate, today),
                        intervalText(currentOverdueInterval(task, occurrence, today)),
                    ),
                    LightPodsjetnikColors.red,
                )
            }
        }
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(color.toArgb())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(false)
            .setSilent(!alert)
            .setContentIntent(openTask(task.id))
            .addAction(0, context.getString(R.string.notif_akcija_odradjeno), action(NotificationActionReceiver.ACTION_DONE, occurrence.id))
            .addAction(0, context.getString(R.string.notif_akcija_odgodi), action(NotificationActionReceiver.ACTION_SNOOZE, occurrence.id))
        if (stage == Stage.OVERDUE) {
            // Ongoing + delete intent: swiped away while unlocked → re-posted silently (PRD H4).
            builder.setOngoing(true).setDeleteIntent(action(NotificationActionReceiver.ACTION_DISMISSED, occurrence.id))
        }
        notify(TAG_REMINDER, occurrence.id.toInt(), builder.build())
    }

    /** "Odrađeno: ‹naslov›" + Poništi, silent, gone after 10 s (PRD H5). */
    fun postDone(occurrenceId: Long, taskId: Long, title: String, nextDue: LocalDate?, today: LocalDate) {
        val builder = NotificationCompat.Builder(context, NotificationFactory.CHANNEL_NA_DAN)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(LightPodsjetnikColors.grn.toArgb())
            .setContentTitle(context.getString(R.string.notif_odradjeno_naslov, title))
            .setSilent(true)
            .setTimeoutAfter(DONE_TIMEOUT_MS)
            .setContentIntent(openTask(taskId))
            .addAction(0, context.getString(R.string.ponisti), action(NotificationActionReceiver.ACTION_UNDO, occurrenceId))
        nextDue?.let { builder.setContentText(context.getString(R.string.sljedeci_termin_label, formatDate(it, today))) }
        notify(TAG_DONE, occurrenceId.toInt(), builder.build())
    }

    /** Morning summary (PRD FR-6). */
    fun postSummary(summary: SummaryPost) {
        val todayPart = res.getQuantityString(R.plurals.sazetak_danas_imas, summary.dueToday, summary.dueToday)
        val title = when (summaryVariant(summary.dueToday, summary.overdue)) {
            SummaryVariant.TODAY_AND_OVERDUE -> context.getString(
                R.string.sazetak_s_kasnjenjem,
                todayPart,
                res.getQuantityString(R.plurals.danas_sazetak_kasni, summary.overdue, summary.overdue),
            )
            SummaryVariant.TODAY_ONLY -> todayPart
            SummaryVariant.OVERDUE_ONLY -> res.getQuantityString(R.plurals.sazetak_samo_kasni, summary.overdue, summary.overdue)
        }
        val style = NotificationCompat.InboxStyle()
        summary.titles.forEach(style::addLine)
        val notification = NotificationCompat.Builder(context, NotificationFactory.CHANNEL_SAZETAK)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(LightPodsjetnikColors.pri.toArgb())
            .setContentTitle(title)
            .setContentText(summary.titles.joinToString(", "))
            .setStyle(style)
            .setSilent(!summary.alert)
            .setAutoCancel(true)
            .setContentIntent(openApp())
            .build()
        notify(TAG_SUMMARY, 0, notification)
    }

    fun cancelReminder(occurrenceId: Long) = compat.cancel(TAG_REMINDER, occurrenceId.toInt())

    fun cancelDone(occurrenceId: Long) = compat.cancel(TAG_DONE, occurrenceId.toInt())

    /** Removes reminder notifications of occurrences that are closed, paused or deleted. */
    fun cancelInactive(activeOccurrenceIds: Set<Long>) {
        system.activeNotifications
            .filter { it.tag == TAG_REMINDER && it.id.toLong() !in activeOccurrenceIds }
            .forEach { compat.cancel(TAG_REMINDER, it.id) }
    }

    @SuppressLint("MissingPermission") // checked by areNotificationsEnabled()
    private fun notify(tag: String, id: Int, notification: Notification) {
        if (compat.areNotificationsEnabled()) compat.notify(tag, id, notification)
    }

    private fun longDate(date: LocalDate, today: LocalDate): String =
        "${res.getStringArray(R.array.dani_dugo)[date.dayOfWeek.value - 1]}, ${formatDate(date, today)}"

    private fun monthName(date: LocalDate): String = res.getStringArray(R.array.mjeseci)[date.monthValue - 1]

    private fun ruleText(rule: RecurrenceRule): String = when (rule) {
        is RecurrenceRule.DayOfMonth -> context.getString(R.string.pravilo_svakog_dana, rule.day)
        RecurrenceRule.LastDayOfMonth -> context.getString(R.string.pravilo_zadnji_dan)
    }

    private fun intervalText(hours: Int): String = context.getString(
        when (hours) {
            1 -> R.string.interval_1h
            2 -> R.string.interval_2h
            else -> R.string.interval_4h
        },
    )

    private fun openTask(taskId: Long): PendingIntent = PendingIntent.getActivity(
        context,
        taskId.toInt(),
        Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_TASK_ID, taskId)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        context,
        -1,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private fun action(action: String, occurrenceId: Long): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java)
            .setAction(action)
            .putExtra(NotificationActionReceiver.EXTRA_ID, occurrenceId)
        // Distinct request code per (occurrence, action) so PendingIntents don't overwrite each other.
        val requestCode = (occurrenceId * 8 + NotificationActionReceiver.ACTIONS.indexOf(action)).toInt()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private data class Content(
        val channel: String,
        val title: String,
        val text: String,
        val color: androidx.compose.ui.graphics.Color,
    )

    private companion object {
        const val TAG_REMINDER = "reminder"
        const val TAG_DONE = "done"
        const val TAG_SUMMARY = "summary"
        const val DONE_TIMEOUT_MS = 10_000L
    }
}
