package hr.podsjetnik.ui.dijagnostika

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import hr.podsjetnik.R
import hr.podsjetnik.data.AlarmLogEntity
import hr.podsjetnik.ui.theme.LocalPodsjetnikColors
import java.time.ZoneId

/** Dijagnostika (PRD FR-7, H12): alarm log with delays, warning when late, test tools. */
@Composable
fun DijagnostikaScreen(onBack: () -> Unit, viewModel: DijagnostikaViewModel = viewModel(factory = DijagnostikaViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalPodsjetnikColors.current
    val zone = ZoneId.systemDefault()
    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.cd_natrag), tint = colors.text)
            }
            Text(stringResource(R.string.dijagnostika_naslov), style = MaterialTheme.typography.titleLarge, color = colors.text)
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.lateAlarms) {
                item {
                    Text(
                        stringResource(R.string.dijagnostika_upozorenje),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.red,
                        modifier = Modifier.fillMaxWidth().background(colors.redCard, RoundedCornerShape(12.dp)).padding(12.dp),
                    )
                }
            }
            item {
                Text(
                    stringResource(R.string.dijagnostika_alati),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.muted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.addTest(2) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.dijagnostika_test_2))
                    }
                    OutlinedButton(onClick = { viewModel.addTest(15) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.dijagnostika_test_15))
                    }
                    if (viewModel.seedAvailable) {
                        OutlinedButton(onClick = viewModel::loadSeed, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.dijagnostika_seed))
                        }
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.dijagnostika_opis),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.muted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (state.log.isEmpty()) {
                item { Text(stringResource(R.string.dijagnostika_prazno), style = MaterialTheme.typography.bodyMedium, color = colors.faint) }
            }
            items(state.log, key = { it.id }) { entry ->
                LogRow(entry, zone)
                HorizontalDivider(color = colors.line)
            }
        }
    }
}

@Composable
private fun LogRow(entry: AlarmLogEntity, zone: ZoneId) {
    val colors = LocalPodsjetnikColors.current
    val delay = formatDelay(entry.scheduledFor, entry.firedAt) ?: stringResource(R.string.dijagnostika_ceka)
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            stringResource(R.string.dijagnostika_log_naslov, delay, entry.postedCount),
            style = MaterialTheme.typography.titleMedium,
            color = colors.text,
        )
        Text(
            stringResource(
                R.string.dijagnostika_log_vremena,
                formatLogTime(entry.scheduledFor, zone),
                entry.firedAt?.let { formatLogTime(it, zone) } ?: "-",
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
        )
    }
}
