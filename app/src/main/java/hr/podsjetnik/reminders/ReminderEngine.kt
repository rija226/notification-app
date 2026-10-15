package hr.podsjetnik.reminders

import hr.podsjetnik.data.AlarmLogDao
import hr.podsjetnik.data.AlarmLogEntity
import hr.podsjetnik.data.TaskRepository
import hr.podsjetnik.domain.OccurrenceStatus
import hr.podsjetnik.domain.TaskType
import hr.podsjetnik.domain.planReminders
import hr.podsjetnik.domain.stageAt
import hr.podsjetnik.log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * The single path for reminder timing (PRD §8.1): ensure occurrences → plan → post/cancel →
 * persist → replace THE single exact alarm. Every trigger calls [refresh]; running it twice is harmless.
 */
class ReminderEngine(
    private val repository: TaskRepository,
    private val alarmLog: AlarmLogDao,
    private val scheduler: AlarmScheduler,
    private val notifications: ReminderNotifications,
    private val clock: Clock,
) {
    private val mutex = Mutex()

    suspend fun refresh() = mutex.withLock { refreshLocked() }

    /** Odrađeno from a notification (PRD §7). [showUndo] = false when done in the app (it has its own Poništi). */
    suspend fun onDone(occurrenceId: Long, showUndo: Boolean = true) = mutex.withLock {
        notifications.cancelReminder(occurrenceId)
        val done = repository.markDone(occurrenceId)
        refreshLocked() // creates the next occurrence of a recurring task
        if (done == null || !showUndo) return@withLock
        val (task, _) = repository.find(occurrenceId) ?: return@withLock
        val nextDue = if (task.type == TaskType.RECURRING) {
            repository.openOccurrences(task.id).maxOfOrNull { it.dueDate }
        } else {
            null
        }
        notifications.postDone(occurrenceId, task.id, task.title, nextDue, clock.instant().atZone(clock.zone).toLocalDate())
    }

    /** Poništi: restore the state before Odrađeno, including the visible reminder (PRD AC-3.3). */
    suspend fun onUndo(occurrenceId: Long) = mutex.withLock {
        notifications.cancelDone(occurrenceId)
        repository.undo(occurrenceId)
        refreshLocked()
        repostSilently(occurrenceId)
    }

    /** Odgodi 1 sat from the notification (PRD FR-4). */
    suspend fun onSnooze(occurrenceId: Long) = snoozeUntil(occurrenceId, clock.instant() + SNOOZE_FROM_NOTIFICATION)

    /** Odgodi from the app sheet: 1 sat / Večeras / Sutra ujutro. Never changes due date or status. */
    suspend fun snoozeUntil(occurrenceId: Long, until: Instant) = mutex.withLock {
        notifications.cancelReminder(occurrenceId)
        repository.snooze(occurrenceId, until)
        refreshLocked()
    }

    /** Overdue notification swiped away while unlocked → back immediately and silently (PRD H4). */
    suspend fun onDismissed(occurrenceId: Long) = mutex.withLock { repostSilently(occurrenceId) }

    private suspend fun refreshLocked() {
        val now = clock.instant()
        val zone = clock.zone
        repository.ensureOccurrences()
        val items = repository.currentTasks()
        val settings = repository.currentSettings()
        val plan = planReminders(items, settings, now, zone)

        notifications.cancelInactive(plan.activeOccurrenceIds)
        val byOccurrence = items.flatMap { item -> item.occurrences.map { it.id to (item to it) } }.toMap()
        plan.posts.forEach { post ->
            val (item, occurrence) = byOccurrence[post.occurrenceId] ?: return@forEach
            notifications.postReminder(
                task = item.task,
                occurrence = occurrence.copy(lastNotifiedAt = post.notifiedAt),
                openCount = item.openOccurrences.size,
                stage = post.stage,
                alert = post.alert,
                settings = settings,
                at = post.notifiedAt,
                zone = zone,
            )
        }
        plan.summary?.let(notifications::postSummary)
        repository.markNotified(plan.posts.associate { it.occurrenceId to it.notifiedAt })
        plan.summaryDate?.let { repository.setLastSummaryDate(it) }

        val posted = plan.posts.size + if (plan.summary != null) 1 else 0
        recordFiredAlarm(now, posted)
        schedule(plan.nextWakeAt)
        log("refresh at $now: posted $posted, next ${plan.nextWakeAt}")
    }

    private suspend fun repostSilently(occurrenceId: Long) {
        val (task, occurrence) = repository.find(occurrenceId) ?: return
        // Only a reminder that was actually shown comes back; never for closed or paused tasks.
        if (occurrence.status != OccurrenceStatus.OPEN || task.pausedAt != null || occurrence.lastNotifiedAt == null) return
        val now = clock.instant()
        notifications.postReminder(
            task = task,
            occurrence = occurrence,
            openCount = repository.openOccurrences(task.id).size,
            stage = stageAt(task, occurrence, now, clock.zone),
            alert = false,
            settings = repository.currentSettings(),
            at = now,
            zone = clock.zone,
        )
    }

    /**
     * Alarm log (PRD §8.6): the pending row is the planned wake-up. Once its time has come it is marked
     * fired at the moment a refresh actually ran — from the alarm, or later from app open / safety net,
     * which then shows up as a delay in Dijagnostika.
     */
    private suspend fun recordFiredAlarm(now: Instant, posted: Int) {
        val pending = alarmLog.nextPending() ?: return
        if (pending.scheduledFor <= now + FIRE_TOLERANCE) alarmLog.markFired(pending.id, now, posted)
    }

    private suspend fun schedule(at: Instant?) {
        if (alarmLog.nextPending()?.scheduledFor != at) {
            alarmLog.deletePending()
            if (at != null) alarmLog.insert(listOf(AlarmLogEntity(scheduledFor = at)))
            alarmLog.trim(ALARM_LOG_SIZE)
        }
        if (at == null) scheduler.cancel() else scheduler.schedule(at)
    }

    private companion object {
        val SNOOZE_FROM_NOTIFICATION: Duration = Duration.ofHours(1)
        val FIRE_TOLERANCE: Duration = Duration.ofMinutes(1)
        const val ALARM_LOG_SIZE = 500
    }
}
