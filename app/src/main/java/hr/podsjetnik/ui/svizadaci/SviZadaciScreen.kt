package hr.podsjetnik.ui.svizadaci

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import hr.podsjetnik.R
import hr.podsjetnik.ui.components.Chip
import hr.podsjetnik.ui.components.NewTaskFab
import hr.podsjetnik.ui.components.StatusChip
import hr.podsjetnik.ui.components.formatTime
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
    val rows = if (tab == 0) state.recurring else state.oneOff
    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        Column {
            Text(
                stringResource(R.string.svi_naslov),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.text,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
            PrimaryTabRow(selectedTabIndex = tab, containerColor = colors.bg) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = {
                    Text(stringResource(R.string.tab_s_brojem, stringResource(R.string.tab_stalni), state.recurring.size))
                })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = {
                    Text(stringResource(R.string.tab_s_brojem, stringResource(R.string.tab_povremeni), state.oneOff.size))
                })
            }
            if (rows.isEmpty()) {
                Text(
                    stringResource(if (tab == 0) R.string.prazno_stalni_naslov else R.string.prazno_povremeni_naslov),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.muted,
                    modifier = Modifier.padding(20.dp),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(rows, key = { it.taskId }) { row -> TaskListRow(row, state.today) { onOpenTask(row.taskId) } }
                }
            }
        }
        NewTaskFab(onNewTask, Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }
}

@Composable
private fun TaskListRow(row: TaskRow, today: LocalDate, onClick: () -> Unit) {
    val colors = LocalPodsjetnikColors.current
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .background(colors.card, RoundedCornerShape(20.dp))
            .border(BorderStroke(1.dp, colors.line), RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        Text(row.title, style = MaterialTheme.typography.titleMedium, color = colors.text)
        val detail = when {
            row.rule != null -> ruleText(row.rule)
            row.dueDate != null && row.dueTime != null ->
                stringResource(R.string.meta_datum_vrijeme, shortDateText(row.dueDate, today), formatTime(row.dueTime))
            row.dueDate != null -> shortDateText(row.dueDate, today)
            else -> null
        }
        if (detail != null) Text(detail, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            when {
                row.paused -> Chip(stringResource(R.string.chip_pauzirano), colors.neuC, colors.onNeuC)
                row.state != null -> StatusChip(row.state)
            }
            if (row.dueDate != null && !row.paused) {
                Text(
                    stringResource(R.string.rok_datum, shortDateText(row.dueDate, today)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }
        }
    }
}
