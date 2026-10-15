package hr.podsjetnik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import hr.podsjetnik.ui.AppNavHost
import hr.podsjetnik.ui.theme.PodsjetnikTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PodsjetnikTheme {
                AppNavHost()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // App opened = refresh trigger (PRD §8.1). Also re-registers alarms after a force stop.
        container.appScope.launch {
            container.taskRepository.ensureOccurrences()
            container.spikeReminders.refresh()
        }
    }
}
