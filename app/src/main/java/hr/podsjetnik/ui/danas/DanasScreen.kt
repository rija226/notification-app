package hr.podsjetnik.ui.danas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import hr.podsjetnik.R
import hr.podsjetnik.ui.DoneOutcome
import hr.podsjetnik.ui.components.DoneDialog
import hr.podsjetnik.ui.components.EmptyState
import hr.podsjetnik.ui.components.NewTaskFab
import hr.podsjetnik.ui.components.SectionHeader
import hr.podsjetnik.ui.components.TypeTile
import hr.podsjetnik.ui.components.formatLongDate
import hr.podsjetnik.ui.components.shortDateText
import hr.podsjetnik.ui.components.rememberDeviceStatus
import hr.podsjetnik.ui.components.statusText
import hr.podsjetnik.ui.postavke.BLOCKING_PROBLEMS
import hr.podsjetnik.ui.postavke.problemsOf
import hr.podsjetnik.ui.theme.LocalPodsjetnikColors
import java.time.LocalDate

@Composable
fun DanasScreen(
    onNewTask: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: DanasViewModel = viewModel(factory = DanasViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val (status, _) = rememberDeviceStatus()
    val blocked = problemsOf(status, samsungConfirmed = true, lateAlarms = false).any { it in BLOCKING_PROBLEMS }
    val outcome by viewModel.outcome.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val archivedText = stringResource(R.string.premjesteno_u_arhivu)
    val undoText = stringResource(R.string.ponisti)

    val archived = outcome as? DoneOutcome.Archived
    LaunchedEffect(archived) {
        if (archived == null) return@LaunchedEffect
        val result = snackbar.showSnackbar(archivedText, actionLabel = undoText, duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) viewModel.undo(archived.occurrenceId) else viewModel.outcomeShown()
    }
    (outcome as? DoneOutcome.Recurring)?.let {
        DoneDialog(it.dialog, onUndo = { viewModel.undo(it.occurrenceId) }, onDismiss = viewModel::outcomeShown)
    }

    Box(modifier = Modifier.fillMaxSize().background(LocalPodsjetnikColors.current.bg)) {
        state?.let {
            DanasContent(it.sections, it.now.toLocalDate(), onOpenTask, viewModel::done, if (blocked) onOpenSettings else null)
        }
        NewTaskFab(onNewTask, Modifier.align(Alignment.BottomEnd).padding(16.dp))
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp))
    }
}

@Composable
private fun DanasContent(
    sections: DanasSections,
    today: LocalDate,
    onOpenTask: (Long) -> Unit,
    onDone: (Long) -> Unit,
    onFixProblems: (() -> Unit)?,
) {
    val colors = LocalPodsjetnikColors.current
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (onFixProblems != null) item { ProblemBanner(onFixProblems) }
        item { Header(sections, today) }
        if (sections.isEmpty) {
            item { EmptyDanas(sections.next, today, onOpenTask) }
            return@LazyColumn
        }
        section(
            cards = sections.overdue,
            today = today,
            onOpenTask = onOpenTask,
            onDone = onDone,
        ) {
            SectionHeader(
                stringResource(R.string.sekcija_zakasnjelo),
                dot = colors.red,
                labelColor = colors.red,
                count = sections.overdue.size,
                badge = colors.redC to colors.onRedC,
            )
        }
        section(sections.today, today, onOpenTask, onDone) {
            SectionHeader(
                stringResource(R.string.sekcija_danas),
                dot = colors.ambDot,
                labelColor = colors.amb,
                count = sections.today.size,
                badge = colors.ambC to colors.onAmbC,
            )
        }
        section(sections.upcoming, today, onOpenTask, onDone) {
            SectionHeader(
                stringResource(R.string.sekcija_uskoro),
                dot = colors.faint,
                labelColor = colors.muted,
                hint = stringResource(R.string.sekcija_uskoro_opis),
            )
        }
    }
}

private fun LazyListScope.section(
    cards: List<DanasCard>,
    today: LocalDate,
    onOpenTask: (Long) -> Unit,
    onDone: (Long) -> Unit,
    header: @Composable () -> Unit,
) {
    if (cards.isEmpty()) return
    item { Box(Modifier.padding(top = 12.dp)) { header() } }
    items(cards, key = { it.occurrenceId }) { card ->
        TaskCard(card, today, onOpen = { onOpenTask(card.taskId) }, onDone = { onDone(card.occurrenceId) })
    }
}

/** Slim red banner when something blocks reminders (handoff O5, PRD FR-9). */
@Composable
private fun ProblemBanner(onFix: () -> Unit) {
    val colors = LocalPodsjetnikColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.redCard, RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, colors.redLine), RoundedCornerShape(16.dp))
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Icon(painterResource(R.drawable.ic_alert), null, tint = colors.red)
        Text(
            stringResource(R.string.banner_dozvole),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )
        TextButton(onClick = onFix) { Text(stringResource(R.string.popravi), color = colors.red, style = MaterialTheme.typography.labelLarge) }
    }
}

/** Date line, "Danas", and "2 kasne · 1 za danas · 1 uskoro" with the overdue part in red. */
@Composable
private fun Header(sections: DanasSections, today: LocalDate) {
    val colors = LocalPodsjetnikColors.current
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(
            formatLongDate(today, stringArrayResource(R.array.dani_dugo).toList(), stringArrayResource(R.array.mjeseci_genitiv).toList()),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
        )
        Text(stringResource(R.string.danas_naslov), style = MaterialTheme.typography.headlineLarge, color = colors.text)
        val overdue = sections.overdue.size
        val parts = listOfNotNull(
            stringResource(R.string.danas_sazetak_danas, sections.today.size).takeIf { sections.today.isNotEmpty() },
            stringResource(R.string.danas_sazetak_uskoro, sections.upcoming.size).takeIf { sections.upcoming.isNotEmpty() },
        )
        val separator = stringResource(R.string.danas_sazetak_razmak)
        val overdueText = pluralStringResource(R.plurals.danas_sazetak_kasni, overdue, overdue)
        if (overdue > 0 || parts.isNotEmpty()) {
            Text(
                buildAnnotatedString {
                    if (overdue > 0) {
                        withStyle(SpanStyle(color = colors.red, fontWeight = FontWeight.Bold)) { append(overdueText) }
                        if (parts.isNotEmpty()) append(separator)
                    }
                    append(parts.joinToString(separator))
                },
                style = MaterialTheme.typography.bodyLarge,
                color = colors.muted,
            )
        }
    }
}

/** Empty state (screen 09): "Sve je odrađeno" + the nearest future task. */
@Composable
private fun EmptyDanas(next: DanasCard?, today: LocalDate, onOpenTask: (Long) -> Unit) {
    val colors = LocalPodsjetnikColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 96.dp),
    ) {
        EmptyState(
            icon = R.drawable.ic_check,
            title = stringResource(R.string.prazno_danas_naslov),
            text = stringResource(R.string.prazno_danas_tekst),
            circle = colors.grnC,
            iconTint = colors.grn,
        )
        if (next != null) {
            val shape = RoundedCornerShape(20.dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .clickable { onOpenTask(next.taskId) }
                    .background(colors.card, shape)
                    .border(BorderStroke(1.dp, colors.line), shape)
                    .padding(16.dp),
            ) {
                TypeTile(next.type, colors.c1)
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(stringResource(R.string.prazno_sljedece), style = MaterialTheme.typography.labelMedium, color = colors.faint)
                    Text(next.title, style = MaterialTheme.typography.titleMedium, color = colors.text)
                    Text(
                        stringResource(R.string.meta_pravilo_datum, shortDateText(next.dueDate, today), statusText(next.state).replaceFirstChar { it.lowercase() }),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                }
                Icon(painterResource(R.drawable.ic_chevron_right), null, tint = colors.muted)
            }
        }
    }
}
