package hr.podsjetnik.data.backup

import android.content.Context
import android.net.Uri
import hr.podsjetnik.data.TaskRepository
import hr.podsjetnik.reminders.ReminderEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Clock
import java.time.LocalDate

/**
 * Export / import through the Storage Access Framework (PRD FR-10): the user picks the file in the
 * system picker, the app only reads/writes that one Uri — no storage permission.
 */
class BackupRepository(
    private val context: Context,
    private val repository: TaskRepository,
    private val engine: ReminderEngine,
    private val clock: Clock,
) {
    /** "podsjetnik-backup-2026-10-15.json" */
    fun suggestedFileName(): String = "podsjetnik-backup-${LocalDate.now(clock)}.json"

    suspend fun export(uri: Uri) {
        val items = repository.currentTasks()
        val content = BackupContent(
            tasks = items.map { it.task },
            occurrences = items.flatMap { it.occurrences },
            settings = repository.currentSettings(),
        )
        val text = encodeBackup(content, clock.instant())
        withContext(Dispatchers.IO) {
            val stream = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("cannot open $uri")
            stream.bufferedWriter().use { it.write(text) }
        }
    }

    /** Reads and validates a file without changing anything; throws [InvalidBackupException]. */
    suspend fun read(uri: Uri): BackupContent = withContext(Dispatchers.IO) {
        val text = try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } catch (e: IOException) {
            throw InvalidBackupException("cannot read file", e)
        } ?: throw InvalidBackupException("cannot read file")
        decodeBackup(text)
    }

    /** Replaces all data in one transaction (a failure leaves the old data), then re-plans reminders. */
    suspend fun apply(content: BackupContent) {
        repository.replaceAll(content.tasks, content.occurrences, content.settings)
        engine.onDataReplaced()
    }
}
