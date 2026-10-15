package hr.podsjetnik.ui.uredi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import hr.podsjetnik.R
import hr.podsjetnik.domain.TaskType
import hr.podsjetnik.ui.components.DayOfMonthGrid
import hr.podsjetnik.ui.components.formatTime
import hr.podsjetnik.ui.components.shortDateText
import hr.podsjetnik.ui.theme.LocalPodsjetnikColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@Composable
fun UrediScreen(
    onClose: () -> Unit,
    viewModel: UrediViewModel = viewModel(factory = UrediViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    UrediContent(
        state = state,
        onClose = onClose,
        onTitle = viewModel::onTitle,
        onType = viewModel::onType,
        onDay = viewModel::onDay,
        onLastDay = viewModel::onLastDay,
        onDate = viewModel::onDate,
        onTime = viewModel::onTime,
        onSave = { viewModel.save(onSaved = onClose) },
    )
}

@Composable
private fun UrediContent(
    state: UrediUiState,
    onClose: () -> Unit,
    onTitle: (String) -> Unit,
    onType: (TaskType) -> Unit,
    onDay: (Int) -> Unit,
    onLastDay: (Boolean) -> Unit,
    onDate: (LocalDate) -> Unit,
    onTime: (LocalTime?) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPodsjetnikColors.current
    val form = state.form
    Column(modifier = modifier.fillMaxSize().background(colors.bg)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.cd_zatvori), tint = colors.text)
            }
            Text(stringResource(R.string.novi_zadatak), style = MaterialTheme.typography.titleLarge, color = colors.text)
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            OutlinedTextField(
                value = form.title,
                onValueChange = onTitle,
                label = { Text(stringResource(R.string.polje_naziv)) },
                isError = state.titleError,
                supportingText = if (state.titleError) {
                    { Text(stringResource(R.string.greska_naziv)) }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SectionLabel(stringResource(R.string.polje_vrsta))
            TypeSelector(form.type, onType)
            Helper(
                stringResource(
                    if (form.type == TaskType.RECURRING) R.string.vrsta_stalni_opis else R.string.vrsta_povremeni_opis,
                ),
            )
            if (form.type == TaskType.RECURRING) {
                RecurrenceFields(form, onDay, onLastDay)
                Text(
                    stringResource(R.string.sljedeci_termin_label, shortDateText(state.nextDue, state.today)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.text,
                )
            } else {
                DateTimeFields(form, state.today, onDate, onTime)
            }
        }
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
        ) {
            Text(stringResource(R.string.spremi_zadatak))
        }
    }
}

@Composable
private fun TypeSelector(selected: TaskType, onType: (TaskType) -> Unit) {
    val options = listOf(TaskType.RECURRING to R.string.tip_stalni, TaskType.ONE_OFF to R.string.tip_povremeni)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (type, label) ->
            SegmentedButton(
                selected = selected == type,
                onClick = { onType(type) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(stringResource(label)) }
        }
    }
}

@Composable
private fun RecurrenceFields(form: UrediForm, onDay: (Int) -> Unit, onLastDay: (Boolean) -> Unit) {
    SectionLabel(stringResource(R.string.ponavljanje))
    RadioRow(stringResource(R.string.odredjeni_dan), selected = !form.lastDay) { onLastDay(false) }
    DayOfMonthGrid(selected = form.day.takeUnless { form.lastDay }, onSelect = onDay)
    Helper(stringResource(R.string.dan_napomena))
    RadioRow(stringResource(R.string.zadnji_dan), selected = form.lastDay) { onLastDay(true) }
    Helper(stringResource(R.string.zadnji_dan_opis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeFields(
    form: UrediForm,
    today: LocalDate,
    onDate: (LocalDate) -> Unit,
    onTime: (LocalTime?) -> Unit,
) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    SectionLabel(stringResource(R.string.datum_i_vrijeme))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { showDate = true }) { Text(shortDateText(form.date, today)) }
        OutlinedButton(onClick = { showTime = true }) {
            Text(
                form.time?.let { stringResource(R.string.rani_u_vrijeme, formatTime(it)) }
                    ?: stringResource(R.string.dodaj_vrijeme),
            )
        }
        if (form.time != null) {
            TextButton(onClick = { onTime(null) }) { Text(stringResource(R.string.bez_vremena)) }
        }
    }
    if (showDate) {
        // DatePicker works in UTC millis at midnight.
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = form.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        onDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDate = false
                }) { Text(stringResource(R.string.u_redu)) }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text(stringResource(R.string.odustani)) } },
        ) { DatePicker(state = pickerState) }
    }
    if (showTime) {
        val initial = form.time ?: LocalTime.of(9, 0)
        val pickerState = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    onTime(LocalTime.of(pickerState.hour, pickerState.minute))
                    showTime = false
                }) { Text(stringResource(R.string.u_redu)) }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text(stringResource(R.string.odustani)) } },
            text = { TimePicker(state = pickerState) },
        )
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalPodsjetnikColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.text)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = LocalPodsjetnikColors.current.text)
}

@Composable
private fun Helper(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = LocalPodsjetnikColors.current.faint)
}
