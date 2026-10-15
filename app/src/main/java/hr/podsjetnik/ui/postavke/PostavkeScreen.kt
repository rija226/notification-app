package hr.podsjetnik.ui.postavke

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import hr.podsjetnik.R
import hr.podsjetnik.domain.Settings
import hr.podsjetnik.ui.components.AddTimeChip
import hr.podsjetnik.ui.components.IconTile
import hr.podsjetnik.ui.components.IntervalSelector
import hr.podsjetnik.ui.components.SamsungSteps
import hr.podsjetnik.ui.components.Stepper
import hr.podsjetnik.ui.components.TimeChip
import hr.podsjetnik.ui.components.TimePickerDialog
import hr.podsjetnik.ui.components.canAddDayTime
import hr.podsjetnik.ui.components.formatTime
import hr.podsjetnik.ui.components.rememberDeviceFixes
import hr.podsjetnik.ui.components.rememberDeviceStatus
import hr.podsjetnik.ui.components.stepEarlyDays
import hr.podsjetnik.ui.theme.LocalPodsjetnikColors
import java.time.LocalTime

private enum class Picker { QUIET_FROM, QUIET_TO, DAY_TIME }

/** Postavke (HANDOFF §6.7, screen 07 + PRD FR-7/FR-9). */
@Composable
fun PostavkeScreen(onOpenDiagnostics: () -> Unit, viewModel: PostavkeViewModel = viewModel(factory = PostavkeViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val (status, recheck) = rememberDeviceStatus()
    val fixes = rememberDeviceFixes(onNotificationResult = recheck)
    val colors = LocalPodsjetnikColors.current
    val current = state ?: return Box(Modifier.fillMaxSize().background(colors.bg))
    val settings = current.settings
    val problems = problemsOf(status, settings.samsungSetupConfirmed, current.lateAlarms)
    var picker by remember { mutableStateOf<Picker?>(null) }
    var showSamsung by remember { mutableStateOf(false) }
    var showEarly by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize().background(colors.bg).verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Text(
            stringResource(R.string.postavke_naslov),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.text,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            SectionTitle(stringResource(R.string.dozvole), Modifier.weight(1f))
            if (problems.isNotEmpty()) {
                Text(
                    pluralStringResource(R.plurals.dozvole_paznja, problems.size, problems.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.red,
                )
            }
        }
        Card {
            Column {
                PermissionRow(
                    icon = R.drawable.ic_bell,
                    title = stringResource(R.string.dozvola_obavijesti),
                    okText = stringResource(R.string.dozvola_obavijesti_ok),
                    problemText = stringResource(
                        if (status.notificationPermission) R.string.dozvola_kanali_ne else R.string.dozvola_obavijesti_ne,
                    ),
                    ok = Problem.NOTIFICATIONS !in problems,
                    onFix = { fixes.notifications(status.notificationPermission) },
                )
                Divider()
                PermissionRow(
                    icon = R.drawable.ic_clock,
                    title = stringResource(R.string.dozvola_alarmi),
                    okText = stringResource(R.string.dozvola_alarmi_ok),
                    problemText = stringResource(R.string.dozvola_alarmi_ne),
                    ok = Problem.EXACT_ALARMS !in problems,
                    onFix = fixes::appSettings,
                )
                Divider()
                PermissionRow(
                    icon = R.drawable.ic_battery,
                    title = stringResource(R.string.dozvola_baterija),
                    okText = stringResource(R.string.dozvola_baterija_ok),
                    problemText = stringResource(R.string.dozvola_baterija_ne),
                    ok = Problem.BATTERY !in problems,
                    onFix = fixes::battery,
                )
                Divider()
                PermissionRow(
                    icon = R.drawable.ic_moon,
                    title = stringResource(R.string.dozvola_samsung),
                    okText = stringResource(R.string.dozvola_samsung_ok),
                    problemText = stringResource(R.string.dozvola_samsung_ne),
                    ok = Problem.SAMSUNG !in problems,
                    onFix = { showSamsung = true },
                    warning = true,
                )
                Divider()
                PermissionRow(
                    icon = R.drawable.ic_alert,
                    title = stringResource(R.string.dozvola_tocnost),
                    okText = stringResource(R.string.dozvola_tocnost_ok),
                    problemText = stringResource(R.string.dozvola_tocnost_ne),
                    ok = Problem.LATE_ALARMS !in problems,
                    onFix = onOpenDiagnostics,
                    warning = true,
                )
            }
        }

        SectionTitle(stringResource(R.string.tihi_sati_naslov), Modifier.padding(top = 12.dp))
        Card {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                    IconTile(R.drawable.ic_moon, colors.c1, colors.muted)
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                        Text(stringResource(R.string.tihi_prekidac), style = MaterialTheme.typography.titleMedium, color = colors.text)
                        Text(
                            stringResource(if (settings.quietEnabled) R.string.tihi_ukljuceno else R.string.tihi_iskljuceno),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted,
                        )
                    }
                    Switch(
                        checked = settings.quietEnabled,
                        onCheckedChange = viewModel::setQuietEnabled,
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.pri, uncheckedTrackColor = colors.c2),
                    )
                }
                if (settings.quietEnabled) {
                    Divider()
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
                        TimeBox(stringResource(R.string.tihi_od), settings.quietFrom, Modifier.weight(1f)) { picker = Picker.QUIET_FROM }
                        TimeBox(stringResource(R.string.tihi_do), settings.quietTo, Modifier.weight(1f)) { picker = Picker.QUIET_TO }
                    }
                }
            }
        }
        Helper(stringResource(R.string.tihi_napomena, formatTime(settings.quietTo)))

        SectionTitle(stringResource(R.string.zadano_naslov), Modifier.padding(top = 12.dp))
        DefaultsCard(settings, viewModel, onAddTime = { picker = Picker.DAY_TIME }, onEarly = { showEarly = true })

        SectionTitle(stringResource(R.string.dijagnostika_naslov), Modifier.padding(top = 12.dp))
        Card {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenDiagnostics).padding(16.dp),
            ) {
                IconTile(R.drawable.ic_list, colors.c1, colors.muted)
                Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    Text(stringResource(R.string.dijagnostika_naslov), style = MaterialTheme.typography.titleMedium, color = colors.text)
                    Text(stringResource(R.string.dijagnostika_opis), style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                }
                Icon(painterResource(R.drawable.ic_chevron_right), null, tint = colors.muted)
            }
        }
        Spacer(Modifier.size(16.dp))
    }

    when (picker) {
        Picker.QUIET_FROM -> TimePickerDialog(settings.quietFrom, onConfirm = {
            viewModel.setQuietFrom(it)
            picker = null
        }, onDismiss = { picker = null })
        Picker.QUIET_TO -> TimePickerDialog(settings.quietTo, onConfirm = {
            viewModel.setQuietTo(it)
            picker = null
        }, onDismiss = { picker = null })
        Picker.DAY_TIME -> TimePickerDialog(LocalTime.of(12, 0), onConfirm = {
            viewModel.addDefaultDayTime(it)
            picker = null
        }, onDismiss = { picker = null })
        null -> Unit
    }
    if (showSamsung) {
        AlertDialog(
            onDismissRequest = { showSamsung = false },
            title = { Text(stringResource(R.string.samsung_naslov)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.samsung_tekst), style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                    SamsungSteps()
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setSamsungConfirmed(true)
                    showSamsung = false
                }) { Text(stringResource(R.string.samsung_potvrda)) }
            },
            dismissButton = { TextButton(onClick = fixes::appSettings) { Text(stringResource(R.string.samsung_gumb)) } },
        )
    }
    if (showEarly) EarlyDefaultDialog(settings, onSave = viewModel::setDefaultEarly, onDismiss = { showEarly = false })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DefaultsCard(settings: Settings, viewModel: PostavkeViewModel, onAddTime: () -> Unit, onEarly: () -> Unit) {
    val colors = LocalPodsjetnikColors.current
    Card {
        Column {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.podsjetnici_na_dan), style = MaterialTheme.typography.titleMedium, color = colors.text)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    settings.defaultDayTimes.forEach { time ->
                        TimeChip(time, onRemove = if (settings.defaultDayTimes.size > 1) ({ viewModel.removeDefaultDayTime(time) }) else null)
                    }
                    AddTimeChip(onClick = onAddTime, enabled = canAddDayTime(settings.defaultDayTimes))
                }
            }
            Divider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onEarly).padding(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.rani_podsjetnik), style = MaterialTheme.typography.titleMedium, color = colors.text)
                    Text(
                        if (settings.defaultEarlyDays == 0) {
                            stringResource(R.string.rani_bez)
                        } else {
                            pluralStringResource(
                                R.plurals.rani_opis,
                                settings.defaultEarlyDays,
                                settings.defaultEarlyDays,
                                formatTime(settings.defaultEarlyTime),
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                }
                Icon(painterResource(R.drawable.ic_chevron_right), null, tint = colors.muted)
            }
            Divider()
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.ako_kasni_svakih), style = MaterialTheme.typography.titleMedium, color = colors.text)
                IntervalSelector(settings.defaultOverdueIntervalHours, viewModel::setDefaultInterval)
            }
        }
    }
}

@Composable
private fun EarlyDefaultDialog(settings: Settings, onSave: (Int, LocalTime) -> Unit, onDismiss: () -> Unit) {
    var days by remember { mutableIntStateOf(settings.defaultEarlyDays) }
    var time by remember { mutableStateOf(settings.defaultEarlyTime) }
    var pickTime by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rani_podsjetnik)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Stepper(
                    label = if (days == 0) stringResource(R.string.rani_bez) else pluralStringResource(R.plurals.rani_dana_prije, days, days),
                    onMinus = { days = stepEarlyDays(days, -1) },
                    onPlus = { days = stepEarlyDays(days, 1) },
                    canMinus = days > 0,
                    canPlus = days < 7,
                )
                OutlinedButton(onClick = { pickTime = true }, enabled = days > 0) {
                    Text(stringResource(R.string.rani_u_vrijeme, formatTime(time)))
                }
                Text(stringResource(R.string.rani_napomena), style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(days, time)
                onDismiss()
            }) { Text(stringResource(R.string.u_redu)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.odustani)) } },
    )
    if (pickTime) {
        TimePickerDialog(time, onConfirm = {
            time = it
            pickTime = false
        }, onDismiss = { pickTime = false })
    }
}

/** OK row (muted, "✓ U redu") or problem row (tinted + Popravi / Provjeri) — screen 07. */
@Composable
private fun PermissionRow(
    @DrawableRes icon: Int,
    title: String,
    okText: String,
    problemText: String,
    ok: Boolean,
    onFix: () -> Unit,
    warning: Boolean = false,
) {
    val colors = LocalPodsjetnikColors.current
    val (surface, tileBg, tileTint) = when {
        ok -> Triple(Color.Transparent, colors.c1, colors.muted)
        warning -> Triple(Color.Transparent, colors.ambC, colors.onAmbC)
        else -> Triple(colors.redCard, colors.redC, colors.red)
    }
    Row(modifier = Modifier.fillMaxWidth().background(surface).padding(16.dp)) {
        IconTile(icon, tileBg, tileTint)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.text, modifier = Modifier.weight(1f))
                if (ok) {
                    Icon(painterResource(R.drawable.ic_check), null, tint = colors.grn, modifier = Modifier.size(18.dp))
                    Text(
                        stringResource(R.string.status_u_redu),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.grn,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
            Text(if (ok) okText else problemText, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
            if (!ok) {
                Button(
                    onClick = onFix,
                    colors = if (warning) {
                        ButtonDefaults.buttonColors(containerColor = colors.ambC, contentColor = colors.onAmbC)
                    } else {
                        ButtonDefaults.buttonColors(containerColor = colors.redS, contentColor = colors.onRedS)
                    },
                    modifier = Modifier.padding(top = 4.dp).heightIn(min = 48.dp),
                ) {
                    Text(stringResource(if (warning) R.string.provjeri else R.string.popravi), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun TimeBox(label: String, time: LocalTime, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalPodsjetnikColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, colors.outline), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
        Text(formatTime(time), style = MaterialTheme.typography.headlineMedium, color = colors.text)
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = LocalPodsjetnikColors.current.muted,
        modifier = modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun Helper(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = LocalPodsjetnikColors.current.faint,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun Divider() = HorizontalDivider(color = LocalPodsjetnikColors.current.line)

@Composable
private fun Card(content: @Composable () -> Unit) {
    val colors = LocalPodsjetnikColors.current
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card, shape)
            .border(BorderStroke(1.dp, colors.line), shape),
    ) { content() }
}
