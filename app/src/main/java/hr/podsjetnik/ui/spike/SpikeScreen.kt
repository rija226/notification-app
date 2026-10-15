package hr.podsjetnik.ui.spike

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import hr.podsjetnik.R
import hr.podsjetnik.data.AlarmLogEntity
import hr.podsjetnik.data.AlarmMode
import hr.podsjetnik.reminders.DeviceStatus
import hr.podsjetnik.reminders.PermissionChecker
import hr.podsjetnik.ui.theme.LocalPodsjetnikColors
import java.time.ZoneId

// ponytail: temporary Phase 1 screen, deleted in Phase 5 (PLAN).
@SuppressLint("BatteryLife") // sideloaded app: the direct exemption dialog is allowed (PRD §10.4)
@Composable
fun SpikeScreen(viewModel: SpikeViewModel = viewModel(factory = SpikeViewModel.Factory)) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var status by remember { mutableStateOf(PermissionChecker.status(context)) }
    LifecycleResumeEffect(Unit) {
        status = PermissionChecker.status(context)
        onPauseOrDispose { }
    }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { status = PermissionChecker.status(context) }
    val appUri = "package:${context.packageName}".toUri()

    SpikeContent(
        state = state,
        status = status,
        onFixNotifications = { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) },
        onFixBattery = {
            context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, appUri))
        },
        onOpenAppSettings = {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, appUri))
        },
        onModeChange = viewModel::setMode,
        onAddTest = viewModel::addTest,
        onAddNightSeries = viewModel::addNightSeries,
        onClearPending = viewModel::clearPending,
        onSeed = if (viewModel.seedAvailable) viewModel::loadSeed else null,
    )
}

@Composable
private fun SpikeContent(
    state: SpikeUiState,
    status: DeviceStatus,
    onFixNotifications: () -> Unit,
    onFixBattery: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onModeChange: (AlarmMode) -> Unit,
    onAddTest: (Long) -> Unit,
    onAddNightSeries: () -> Unit,
    onClearPending: () -> Unit,
    onSeed: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPodsjetnikColors.current
    val zone = ZoneId.systemDefault()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .safeDrawingPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                stringResource(R.string.spike_naslov),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.text,
            )
        }
        item { StatusRow(stringResource(R.string.dozvola_obavijesti), status.notifications, onFixNotifications) }
        item { StatusRow(stringResource(R.string.dozvola_alarmi), status.exactAlarms, onFix = null) }
        item { StatusRow(stringResource(R.string.dozvola_baterija), status.batteryExempt, onFixBattery) }
        item {
            OutlinedButton(onClick = onOpenAppSettings, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.spike_postavke_aplikacije))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.mode == AlarmMode.EXACT_WHILE_IDLE,
                    onClick = { onModeChange(AlarmMode.EXACT_WHILE_IDLE) },
                    label = { Text(stringResource(R.string.spike_mod_a)) },
                )
                FilterChip(
                    selected = state.mode == AlarmMode.ALARM_CLOCK,
                    onClick = { onModeChange(AlarmMode.ALARM_CLOCK) },
                    label = { Text(stringResource(R.string.spike_mod_b)) },
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onAddTest(2) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.spike_test_2))
                }
                Button(onClick = { onAddTest(15) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.spike_test_15))
                }
                Button(onClick = onAddNightSeries, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.spike_niz))
                }
                TextButton(onClick = onClearPending, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.spike_obrisi))
                }
                if (onSeed != null) {
                    OutlinedButton(onClick = onSeed, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.spike_seed))
                    }
                }
            }
        }
        item {
            Text(
                stringResource(R.string.spike_dnevnik),
                style = MaterialTheme.typography.titleSmall,
                color = colors.text,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        items(state.log, key = { it.id }) { entry ->
            LogRow(entry, zone)
            HorizontalDivider(color = colors.line)
        }
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean, onFix: (() -> Unit)?) {
    val colors = LocalPodsjetnikColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.text, modifier = Modifier.weight(1f))
        when {
            ok -> Text(stringResource(R.string.status_u_redu), color = colors.grn)
            onFix != null -> Button(onClick = onFix) { Text(stringResource(R.string.popravi)) }
            else -> Text(stringResource(R.string.spike_problem), color = colors.red)
        }
    }
}

@Composable
private fun LogRow(entry: AlarmLogEntity, zone: ZoneId) {
    val colors = LocalPodsjetnikColors.current
    val delay = formatDelay(entry.scheduledFor, entry.firedAt) ?: stringResource(R.string.spike_ceka)
    val mode = if (entry.mode == AlarmMode.EXACT_WHILE_IDLE) "A" else "B"
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            "#${entry.id} · $mode · $delay",
            style = MaterialTheme.typography.titleMedium,
            color = colors.text,
        )
        Text(
            stringResource(
                R.string.spike_log_vremena,
                formatLogTime(entry.scheduledFor, zone),
                entry.firedAt?.let { formatLogTime(it, zone) } ?: "-",
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
        )
    }
}
