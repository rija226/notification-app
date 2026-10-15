package hr.podsjetnik.ui.svizadaci

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import hr.podsjetnik.R
import hr.podsjetnik.domain.StatusKind
import hr.podsjetnik.domain.TaskType
import hr.podsjetnik.ui.components.Chip
import hr.podsjetnik.ui.components.EmptyState
import hr.podsjetnik.ui.components.NewTaskFab
import hr.podsjetnik.ui.components.StatusChip
import hr.podsjetnik.ui.components.TypeTile
import hr.podsjetnik.ui.components.dateTimeText
import hr.podsjetnik.ui.components.formatDate
import hr.podsjetnik.ui.components.ruleText
import hr.podsjetnik.ui.components.shortDateText
import hr.podsjetnik.ui.theme.LocalPodsjetnikColors
import java.time.LocalDate

@Composable
fun SviZadaciScreen(
    onNewTask: () -> Unit,
    onOpenTask: (Long) -> Unit,
    viewModel: SviZadaciViewModel = viewModel(factory = SviZadaciViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SviZadaciContent(state, onNewTask, onOpenTask)
}

@Composable
private fun SviZadaciContent(
    state: SviZadaciUiState,
    onNewTask: () -> Unit,
    onOpenTask: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPodsjetnikColors.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        Column {
            Text(
                stringResource(R.string.svi_naslov),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.text,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
            PrimaryTabRow(selectedTabIndex = tab, containerColor = colors.bg, contentColor = colors.pri) {
                TabLabel(stringResource(R.string.tab_stalni), state.recurring.size, tab == 0) { tab = 0 }
                TabLabel(stringResource(R.string.tab_povremeni), state.oneOff.size, tab == 1) { tab = 1 }
                TabLabel(stringResource(R.string.tab_arhiva), null, tab == 2) { tab = 2 }
            }
            when (tab) {
                0 -> TaskList(state.recurring, state.today, R.string.prazno_stalni_naslov, onOpenTask)
                1 -> TaskList(state.oneOff, state.today, R.string.prazno_povremeni_naslov, onOpenTask)
                else -> ArchiveList(state.archive, state.today, onOpenTask)
            }
        }
        if (tab != 2) NewTaskFab(onNewTask, Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }
}

@Composable
private fun TabLabel(label: String, count: Int?, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalPodsjetnikColors.current
    Tab(selected = selected, onClick = onClick, selectedContentColor = colors.pri, unselectedContentColor = colors.muted, text = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            if (count != null && count > 0) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .heightIn(min = 22.dp)
                        .background(if (selected) colors.priC else colors.c1, CircleShape)
                        .padding(horizontal = 8.dp),
                ) {
                    Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = if (selected) colors.onPriC else colors.muted)
                }
            }
        }
    })
}

@Composable
private fun TaskList(rows: List<TaskRow>, today: LocalDate, emptyText: Int, onOpenTask: (Long) -> Unit) {
    val colors = LocalPodsjetnikColors.current
    if (rows.isEmpty()) {
        Text(stringResource(emptyText), style = MaterialTheme.typography.bodyLarge, color = colors.muted, modifier = Modifier.padding(20.dp))
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(rows, key = { it.taskId }) { row -> TaskListRow(row, today) { onOpenTask(row.taskId) } }
    }
}

@Composable
private fun TaskListRow(row: TaskRow, today: LocalDate, onClick: () -> Unit) {
    val colors = LocalPodsjetnikColors.current
    RowCard(onClick) {
        TypeTile(row.type, colors.c1)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(row.title, style = MaterialTheme.typography.titleMedium, color = colors.text)
            val detail = when {
                row.rule != null -> ruleText(row.rule)
                row.dueDate != null -> dateTimeText(row.dueDate, row.dueTime, today)
                else -> null
            }
            if (detail != null) Text(detail, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                when {
                    row.paused -> Chip(stringResource(R.string.chip_pauzirano), colors.neuC, colors.onNeuC)
                    row.state != null -> StatusChip(row.state)
                }
                if (row.dueDate != null && !row.paused && row.type == TaskType.RECURRING) {
                    val date = shortDateText(row.dueDate, today)
                    Text(
                        if (row.state?.kind == StatusKind.OVERDUE) stringResource(R.string.rok_datum, date) else date,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                }
            }
        }
        Icon(painterResource(R.drawable.ic_chevron_right), null, tint = colors.muted)
    }
}

@Composable
private fun ArchiveList(rows: List<ArchiveRow>, today: LocalDate, onOpenTask: (Long) -> Unit) {
    val colors = LocalPodsjetnikColors.current
    if (rows.isEmpty()) {
        EmptyState(
            icon = R.drawable.ic_archive,
            title = stringResource(R.string.prazno_arhiva_naslov),
            text = stringResource(R.string.prazno_arhiva_tekst),
            circle = colors.c1,
            iconTint = colors.muted,
            modifier = Modifier.padding(top = 120.dp),
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(rows, key = { it.taskId }) { row ->
            RowCard({ onOpenTask(row.taskId) }) {
                TypeTile(TaskType.ONE_OFF, colors.c1)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(row.title, style = MaterialTheme.typography.titleMedium, color = colors.text)
                    Chip(
                        stringResource(R.string.arhiva_odradjeno_datum, formatDate(row.archivedOn, today)),
                        colors.grnC,
                        colors.onGrnC,
                        icon = R.drawable.ic_check,
                    )
                }
                Icon(painterResource(R.drawable.ic_chevron_right), null, tint = colors.muted)
            }
        }
    }
}

@Composable
private fun RowCard(onClick: () -> Unit, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    val colors = LocalPodsjetnikColors.current
    val shape = RoundedCornerShape(20.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .background(colors.card, shape)
            .border(BorderStroke(1.dp, colors.line), shape)
            .padding(14.dp),
        content = content,
    )
}
