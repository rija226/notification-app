package hr.podsjetnik.ui.detalji

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import hr.podsjetnik.domain.StatusKind
import hr.podsjetnik.domain.TaskType
import hr.podsjetnik.ui.DoneOutcome
import hr.podsjetnik.ui.components.Chip
import hr.podsjetnik.ui.components.DoneButton
import hr.podsjetnik.ui.components.DoneDialog
import hr.podsjetnik.ui.components.IconTile
import hr.podsjetnik.ui.components.SnoozeSheet
import hr.podsjetnik.ui.components.StatusChip
import hr.podsjetnik.ui.components.dateTimeText
import hr.podsjetnik.ui.components.formatDate
import hr.podsjetnik.ui.components.formatTime
import hr.podsjetnik.ui.components.intervalText
import hr.podsjetnik.ui.components.ruleText
import hr.podsjetnik.ui.components.shortDateText
import hr.podsjetnik.ui.components.statusText
import hr.podsjetnik.ui.theme.LocalPodsjetnikColors

@Composable
fun DetaljiScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: DetaljiViewModel = viewModel(factory = DetaljiViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val outcome by viewModel.outcome.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val archivedText = stringResource(R.string.premjesteno_u_arhivu)
    val undoText = stringResource(R.string.ponisti)
    var showSnooze by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val archived = outcome as? DoneOutcome.Archived
    LaunchedEffect(archived) {
        if (archived == null) return@LaunchedEffect
        val result = snackbar.showSnackbar(archivedText, actionLabel = undoText)
        if (result == SnackbarResult.ActionPerformed) viewModel.undo(archived.occurrenceId) else viewModel.outcomeShown()
    }
    (outcome as? DoneOutcome.Recurring)?.let {
        DoneDialog(it.dialog, onUndo = { viewModel.undo(it.occurrenceId) }, onDismiss = viewModel::outcomeShown)
    }

    val current = state ?: run {
        Box(Modifier.fillMaxSize().background(LocalPodsjetnikColors.current.bg)) { BackButton(onBack) }
        return
    }
    Box(Modifier.fillMaxSize()) {
        DetaljiContent(
            state = current,
            onBack = onBack,
            onEdit = { onEdit(current.taskId) },
            onSkip = { current.current?.let { viewModel.skip(it.id) } },
            onPause = viewModel::setPaused,
            onDelete = { confirmDelete = true },
            onSnooze = { showSnooze = true },
            onDone = { current.current?.let { viewModel.done(it.id) } },
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
    }

    val occurrence = current.current
    if (showSnooze && occurrence != null) {
        SnoozeSheet(
            title = current.title,
            statusLabel = statusText(occurrence.state),
            overdue = occurrence.state.kind == StatusKind.OVERDUE,
            now = current.now,
            onSnooze = {
                viewModel.snooze(occurrence.id, it)
                showSnooze = false
            },
            onDismiss = { showSnooze = false },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.obrisi_potvrda_naslov)) },
            text = { Text(stringResource(R.string.obrisi_potvrda_tekst, current.title)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onDeleted = onBack)
                }) { Text(stringResource(R.string.obrisi), color = LocalPodsjetnikColors.current.red) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.odustani)) } },
        )
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack, modifier = Modifier.padding(4.dp)) {
        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.cd_natrag), tint = LocalPodsjetnikColors.current.text)
    }
}

@Composable
private fun DetaljiContent(
    state: DetaljiUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSkip: () -> Unit,
    onPause: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onSnooze: () -> Unit,
    onDone: () -> Unit,
) {
    val colors = LocalPodsjetnikColors.current
    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            BackButton(onBack)
            Spacer(Modifier.weight(1f))
            if (!state.archived) {
                if (state.type == TaskType.RECURRING) OverflowMenu(state, onSkip, onPause)
                IconButton(onClick = onEdit) {
                    Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.cd_uredi_zadatak), tint = colors.text)
                }
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            TypeLine(state)
            Text(state.title, style = MaterialTheme.typography.headlineMedium, color = colors.text)
            StatusLine(state)
            state.current?.let { DueCard(state, it) }
            if (!state.archived) Schedule(state)
            state.note?.takeIf { it.isNotBlank() }?.let { note ->
                SectionTitle(stringResource(R.string.napomena_naslov))
                Card { Text(note, style = MaterialTheme.typography.bodyLarge, color = colors.text, modifier = Modifier.padding(16.dp)) }
            }
            state.lastDone?.let { LastDoneCard(state, it) }
            TextButton(onClick = onDelete, modifier = Modifier.padding(bottom = 16.dp)) {
                Icon(painterResource(R.drawable.ic_delete), null, tint = colors.red)
                Text(
                    stringResource(R.string.obrisi_zadatak),
                    color = colors.red,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        if (state.current != null && !state.paused && !state.archived) {
            HorizontalDivider(color = colors.line)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                OutlinedButton(onClick = onSnooze, modifier = Modifier.weight(1f).height(56.dp)) {
                    Icon(painterResource(R.drawable.ic_clock), null, tint = colors.text)
                    Text(
                        stringResource(R.string.odgodi),
                        color = colors.text,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                DoneButton(onClick = onDone, filled = true, modifier = Modifier.weight(1.4f).height(56.dp))
            }
        }
    }
}

@Composable
private fun OverflowMenu(state: DetaljiUiState, onSkip: () -> Unit, onPause: (Boolean) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(painterResource(R.drawable.ic_more_vert), stringResource(R.string.cd_vise_opcija), tint = LocalPodsjetnikColors.current.text)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (state.current != null && !state.paused) {
                DropdownMenuItem(text = { Text(stringResource(R.string.meni_preskoci)) }, onClick = {
                    open = false
                    onSkip()
                })
            }
            DropdownMenuItem(
                text = { Text(stringResource(if (state.paused) R.string.meni_nastavi else R.string.meni_pauziraj)) },
                onClick = {
                    open = false
                    onPause(!state.paused)
                },
            )
        }
    }
}

/** "Stalni · zadnji dan u mjesecu" / "Povremeni · uto, 3. 11. u 10:00" with the type icon. */
@Composable
private fun TypeLine(state: DetaljiUiState) {
    val colors = LocalPodsjetnikColors.current
    val text = if (state.type == TaskType.RECURRING && state.rule != null) {
        stringResource(R.string.detalji_tip_pravilo, ruleText(state.rule).replaceFirstChar { it.lowercase() })
    } else {
        state.current?.let { stringResource(R.string.detalji_tip_povremeni, dateTimeText(it.dueDate, state.dueTime, state.today)) }
            ?: stringResource(R.string.tip_povremeni)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        val icon = if (state.type == TaskType.RECURRING) R.drawable.ic_repeat else R.drawable.ic_today
        Icon(painterResource(icon), null, tint = colors.muted, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.muted, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun StatusLine(state: DetaljiUiState) {
    val colors = LocalPodsjetnikColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        when {
            state.archived -> Chip(stringResource(R.string.chip_odradjeno), colors.grnC, colors.onGrnC, icon = R.drawable.ic_check)
            state.paused -> Chip(stringResource(R.string.chip_pauzirano), colors.neuC, colors.onNeuC)
            state.current != null -> StatusChip(state.current.state)
        }
        if (!state.paused && !state.archived) {
            state.nextReminder?.let { next ->
                val time = formatTime(next.toLocalTime())
                Text(
                    if (next.toLocalDate() == state.today) {
                        stringResource(R.string.detalji_sljedeci_podsjetnik, time)
                    } else {
                        stringResource(R.string.detalji_sljedeci_podsjetnik_datum, formatDate(next.toLocalDate(), state.today), time)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }
        }
    }
}

/** Trenutni rok | Sljedeći termin (Stalni only). */
@Composable
private fun DueCard(state: DetaljiUiState, current: OccurrenceRow) {
    val colors = LocalPodsjetnikColors.current
    Card {
        Row(modifier = Modifier.height(IntrinsicSize.Min).padding(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(if (state.type == TaskType.RECURRING) R.string.detalji_trenutni_rok else R.string.detalji_rok),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
                Text(
                    shortDateText(current.dueDate, state.today),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (current.state.kind == StatusKind.OVERDUE) colors.red else colors.text,
                )
            }
            state.nextTermin?.let { next ->
                Box(Modifier.width(1.dp).fillMaxHeight().background(colors.line))
                Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(stringResource(R.string.detalji_sljedeci_termin), style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                    Text(shortDateText(next, state.today), style = MaterialTheme.typography.titleLarge, color = colors.text)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Schedule(state: DetaljiUiState) {
    val colors = LocalPodsjetnikColors.current
    SectionTitle(stringResource(R.string.detalji_raspored))
    Card {
        Column {
            // Rani podsjetnik
            ScheduleRow(
                icon = R.drawable.ic_bell,
                title = stringResource(R.string.rani_podsjetnik),
                subtitle = state.early?.let { pluralStringResource(R.plurals.rani_opis, it.days, it.days, formatTime(it.time)) }
                    ?: stringResource(R.string.rani_iskljucen),
                trailing = {
                    state.early?.let { early ->
                        if (early.sent) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(painterResource(R.drawable.ic_check), null, tint = colors.grn, modifier = Modifier.size(16.dp))
                                Text(
                                    stringResource(R.string.rani_poslan, formatDate(early.at.toLocalDate(), state.today)),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.grn,
                                )
                            }
                        } else {
                            Text(
                                stringResource(R.string.rani_planiran, formatDate(early.at.toLocalDate(), state.today), formatTime(early.at.toLocalTime())),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.faint,
                            )
                        }
                    }
                },
            )
            HorizontalDivider(color = colors.line, modifier = Modifier.padding(start = 72.dp))
            // Na dan roka
            ScheduleRow(
                icon = R.drawable.ic_clock,
                title = stringResource(R.string.na_dan_roka),
                subtitle = null,
                trailing = {
                    val due = state.current?.dueDate
                    if (state.allDayTimesSent && due != null) {
                        Text(
                            stringResource(R.string.na_dan_sve_poslano, formatDate(due, state.today)),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.faint,
                        )
                    }
                },
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.dayTimes.forEach { chip ->
                        val (bg, fg) = when (chip.state) {
                            ChipState.PAST -> colors.c1 to colors.muted
                            ChipState.NEXT -> colors.priC to colors.onPriC
                            ChipState.FUTURE -> colors.c1 to colors.text
                        }
                        Chip(formatTime(chip.time), bg, fg, icon = if (chip.state == ChipState.PAST) R.drawable.ic_check else null)
                    }
                }
            }
            HorizontalDivider(color = colors.line)
            // Ako kasni
            val overdue = state.currentIntervalHours != null
            Box(Modifier.background(if (overdue) colors.redCard else Color.Transparent)) {
                ScheduleRow(
                    icon = R.drawable.ic_alert,
                    title = stringResource(R.string.ako_kasni),
                    subtitle = stringResource(R.string.ako_kasni_opis, intervalText(state.baseIntervalHours).replaceFirstChar { it.uppercase() }),
                    tile = if (overdue) colors.redC to colors.red else colors.c1 to colors.muted,
                ) {
                    val nextTime = state.nextReminder
                    if (state.currentIntervalHours != null && nextTime != null) {
                        Text(
                            stringResource(R.string.ako_kasni_sada, intervalText(state.currentIntervalHours), formatTime(nextTime.toLocalTime())),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.red,
                        )
                    }
                }
            }
            HorizontalDivider(color = colors.line)
            // Tihi sati
            ScheduleRow(
                icon = R.drawable.ic_moon,
                title = stringResource(R.string.tihi_sati),
                subtitle = if (state.quietEnabled) {
                    stringResource(R.string.tihi_opis, formatTime(state.quietFrom), formatTime(state.quietTo))
                } else {
                    stringResource(R.string.tihi_iskljuceno)
                },
            )
        }
    }
}

@Composable
private fun ScheduleRow(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String?,
    tile: Pair<Color, Color>? = null,
    trailing: @Composable () -> Unit = {},
    extra: @Composable () -> Unit = {},
) {
    val colors = LocalPodsjetnikColors.current
    val (tileBg, tileTint) = tile ?: (colors.c1 to colors.muted)
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        IconTile(icon, tileBg, tileTint)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.text, modifier = Modifier.weight(1f))
                trailing()
            }
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
            extra()
        }
    }
}

@Composable
private fun LastDoneCard(state: DetaljiUiState, done: LastDone) {
    val colors = LocalPodsjetnikColors.current
    SectionTitle(stringResource(R.string.zadnje_odradjeno))
    Card {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp).background(colors.grnC, CircleShape)) {
                Icon(painterResource(R.drawable.ic_check), null, tint = colors.grn)
            }
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    stringResource(
                        R.string.zadnje_datum_vrijeme,
                        shortDateText(done.completedAt.toLocalDate(), state.today),
                        formatTime(done.completedAt.toLocalTime()),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.text,
                )
                val dueText = formatDate(done.dueDate, state.today)
                Text(
                    if (done.daysLate > 0) {
                        pluralStringResource(R.plurals.zadnje_kasnije, done.daysLate, done.daysLate, dueText)
                    } else {
                        stringResource(R.string.zadnje_na_vrijeme, dueText)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = LocalPodsjetnikColors.current.muted,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    val colors = LocalPodsjetnikColors.current
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.card, shape)
            .border(BorderStroke(1.dp, colors.line), shape)
            .clip(shape),
    ) { content() }
}
