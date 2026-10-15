package hr.podsjetnik.ui.detalji

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import hr.podsjetnik.domain.TaskType
import hr.podsjetnik.ui.components.Chip
import hr.podsjetnik.ui.components.StatusChip
import hr.podsjetnik.ui.components.formatDate
import hr.podsjetnik.ui.components.formatTime
import hr.podsjetnik.ui.components.ruleText
import hr.podsjetnik.ui.components.shortDateText
import hr.podsjetnik.ui.theme.LocalPodsjetnikColors

@Composable
fun DetaljiScreen(
    onBack: () -> Unit,
    viewModel: DetaljiViewModel = viewModel(factory = DetaljiViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DetaljiContent(state, onBack)
}

@Composable
private fun DetaljiContent(state: DetaljiUiState?, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalPodsjetnikColors.current
    Column(modifier = modifier.fillMaxSize().background(colors.bg)) {
        IconButton(onClick = onBack, modifier = Modifier.padding(4.dp)) {
            Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.cd_natrag), tint = colors.text)
        }
        if (state == null) return@Column
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            Text(typeLine(state), style = MaterialTheme.typography.bodyMedium, color = colors.muted)
            Text(state.title, style = MaterialTheme.typography.headlineMedium, color = colors.text)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                val current = state.openOccurrences.firstOrNull()
                when {
                    state.paused -> Chip(stringResource(R.string.chip_pauzirano), colors.neuC, colors.onNeuC)
                    current != null -> StatusChip(current.state)
                }
                nextReminderText(state)?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                }
            }
            if (state.openOccurrences.isNotEmpty()) {
                Text(
                    stringResource(R.string.detalji_trenutni_rok),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.text,
                    modifier = Modifier.padding(top = 8.dp),
                )
                state.openOccurrences.forEach { occurrence ->
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.card, RoundedCornerShape(20.dp))
                            .border(BorderStroke(1.dp, colors.line), RoundedCornerShape(20.dp))
                            .padding(14.dp),
                    ) {
                        Text(
                            shortDateText(occurrence.dueDate, state.today),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.text,
                        )
                        StatusChip(occurrence.state)
                    }
                }
            }
        }
    }
}

/** "Stalni · Zadnji dan u mjesecu" / "Povremeni · uto, 3. 11. u 10:00". */
@Composable
private fun typeLine(state: DetaljiUiState): String {
    if (state.type == TaskType.RECURRING && state.rule != null) {
        return stringResource(R.string.detalji_tip_pravilo, ruleText(state.rule))
    }
    val date = state.openOccurrences.firstOrNull()?.dueDate ?: return stringResource(R.string.tip_povremeni)
    val dateText = shortDateText(date, state.today)
    val withTime = state.dueTime?.let { stringResource(R.string.meta_datum_vrijeme, dateText, formatTime(it)) } ?: dateText
    return stringResource(R.string.detalji_tip_povremeni, withTime)
}

/** "sljedeći podsjetnik u 11:00" today, "sljedeći podsjetnik 8. 11. u 09:00" otherwise. */
@Composable
private fun nextReminderText(state: DetaljiUiState): String? {
    val next = state.nextReminder ?: return null
    val time = formatTime(next.toLocalTime())
    return if (next.toLocalDate() == state.today) {
        stringResource(R.string.detalji_sljedeci_podsjetnik, time)
    } else {
        stringResource(R.string.detalji_sljedeci_podsjetnik_datum, formatDate(next.toLocalDate(), state.today), time)
    }
}
