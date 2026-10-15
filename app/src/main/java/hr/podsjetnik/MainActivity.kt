package hr.podsjetnik

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import hr.podsjetnik.ui.AppNavHost
import hr.podsjetnik.ui.theme.PodsjetnikTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    /** Task to open from a notification tap (PRD AC-5.4); consumed by the nav host. */
    private val openTaskId = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) readTaskId(intent)
        setContent {
            PodsjetnikTheme {
                AppNavHost(openTaskId = openTaskId.value, onOpenTaskHandled = { openTaskId.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readTaskId(intent)
    }

    override fun onResume() {
        super.onResume()
        // App opened = refresh trigger (PRD §8.1). Also re-registers the alarm after a force stop.
        container.appScope.launch { container.reminderEngine.refresh() }
    }

    private fun readTaskId(intent: Intent) {
        intent.getLongExtra(EXTRA_TASK_ID, -1).takeIf { it > 0 }?.let { openTaskId.value = it }
    }

    companion object {
        const val EXTRA_TASK_ID = "taskId"
    }
}
