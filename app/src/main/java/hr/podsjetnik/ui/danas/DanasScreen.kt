package hr.podsjetnik.ui.danas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hr.podsjetnik.R
import hr.podsjetnik.ui.components.NewTaskFab
import hr.podsjetnik.ui.theme.LocalPodsjetnikColors

// ponytail: placeholder until Phase 5 builds the real Danas screen.
@Composable
fun DanasScreen(onNewTask: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalPodsjetnikColors.current
    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        Text(
            stringResource(R.string.danas_naslov),
            style = MaterialTheme.typography.headlineLarge,
            color = colors.text,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
        NewTaskFab(onNewTask, Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }
}
