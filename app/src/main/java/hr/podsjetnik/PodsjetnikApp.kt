package hr.podsjetnik

import android.app.Application
import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import hr.podsjetnik.reminders.NotificationFactory
import hr.podsjetnik.reminders.SafetyNetWorker
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

class PodsjetnikApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        useCroatian()
        container = AppContainer(this)
        NotificationFactory.createChannels(this)
        SafetyNetWorker.schedule(this)
        refreshOnEveryChange()
    }

    /**
     * The UI is Croatian only, but plural forms ("1 dan / 2 dana / 21 dan", "3 kasne") and pickers follow the
     * app locale. Pin it to hr so a phone set to another language still gets Croatian grammar.
     */
    private fun useCroatian() {
        val locales = getSystemService(LocaleManager::class.java)
        if (locales.applicationLocales.isEmpty) locales.applicationLocales = LocaleList.forLanguageTags("hr")
    }

    /**
     * Any task, occurrence or settings change → refresh (CLAUDE.md: every data change calls refresh).
     * Writes made by refresh itself trigger one more run, which changes nothing (refresh is idempotent).
     */
    private fun refreshOnEveryChange() {
        val repository = container.taskRepository
        container.appScope.launch {
            combine(repository.tasks, repository.settings) { _, _ -> }
                .conflate()
                .collect { container.reminderEngine.refresh() }
        }
    }
}

val Context.container: AppContainer
    get() = (applicationContext as PodsjetnikApp).container
