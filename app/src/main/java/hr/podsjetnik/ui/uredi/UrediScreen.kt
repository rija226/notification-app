package hr.podsjetnik.ui.uredi

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import hr.podsjetnik.R
import hr.podsjetnik.domain.TaskType
import hr.podsjetnik.ui.components.AddTimeChip
import hr.podsjetnik.ui.components.DayOfMonthGrid
import hr.podsjetnik.ui.components.InlineMonthCalendar
import hr.podsjetnik.ui.components.Stepper
import hr.podsjetnik.ui.components.TimeChip
import hr.podsjetnik.ui.components.TimePickerDialog
import hr.podsjetnik.ui.components.formatShortDateWithYear
import hr.podsjetnik.ui.components.formatTime
import hr.podsjetnik.ui.components.shortDateText
import hr.podsjetnik.ui.theme.LocalPodsjetnikColors
import java.time.LocalTime

@Composable
fun UrediScreen(onClose: () -> Unit, viewModel: UrediViewModel = viewModel(factory = UrediViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalPodsjetnikColors.current
    val current = state ?: return Box(Modifier.fillMaxSize().background(colors.bg))
    UrediContent(current, viewModel, onClose)
}

/** Which time picker is open. */
private enum class TimeTarget { DUE, EARLY, DAY }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UrediContent(state: UrediUiState, viewModel: UrediViewModel, onClose: () -> Unit) {
    val colors = LocalPodsjetnikColors.current
    val form = state.form
    var picker by remember { mutableStateOf<TimeTarget?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg).imePadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.cd_zatvori), tint = colors.text)
            }
            Text(
                stringResource(if (state.isEdit) R.string.uredi_zadatak else R.string.novi_zadatak),
                style = MaterialTheme.typography.titleLarge,
                color = colors.text,
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = form.title,
                onValueChange = viewModel::onTitle,
                label = { Text(stringResource(R.string.polje_naziv)) },
                isError = state.titleError,
                supportingText = if (state.titleError) {
                    { Text(stringResource(R.string.greska_naziv)) }
                } else {
                    null
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Label(stringResource(R.string.polje_vrsta))
            TypeSelector(form.type, enabled = !state.isEdit, onType = viewModel::onType)
            Helper(stringResource(if (form.type == TaskType.RECURRING) R.string.vrsta_stalni_opis else R.string.vrsta_povremeni_opis))

            if (form.type == TaskType.RECURRING) {
                Label(stringResource(R.string.ponavljanje))
                RuleCard(state, viewModel)
            } else {
                Label(stringResource(R.string.datum_i_vrijeme))
                DateTimeRow(form, onTime = { picker = TimeTarget.DUE })
                Card {
                    InlineMonthCalendar(form.date, state.today, viewModel::onDate, Modifier.padding(16.dp))
                }
            }

            Label(stringResource(R.string.rani_podsjetnik))
            Card {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                    Stepper(
                        label = if (form.earlyDays == 0) {
                            stringResource(R.string.rani_bez)
                        } else {
                            pluralStringResource(R.plurals.rani_dana_prije, form.earlyDays, form.earlyDays)
                        },
                        onMinus = { viewModel.onEarlyStep(-1) },
                        onPlus = { viewModel.onEarlyStep(1) },
                        canMinus = form.earlyDays > 0,
                        canPlus = form.earlyDays < 7,
                    )
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick = { picker = TimeTarget.EARLY }, enabled = form.earlyDays > 0) {
                        Text(stringResource(R.string.rani_u_vrijeme, formatTime(form.earlyTime)), color = colors.text)
                    }
                }
            }
            Helper(stringResource(R.string.rani_napomena))

            Label(stringResource(R.string.podsjetnici_na_dan))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                form.dayTimes.forEach { time ->
                    TimeChip(time, onRemove = if (form.dayTimes.size > 1) ({ viewModel.onRemoveDayTime(time) }) else null)
                }
                AddTimeChip(onClick = { picker = TimeTarget.DAY }, enabled = canAddDayTime(form.dayTimes))
            }
            Helper(stringResource(R.string.na_dan_napomena))

            Label(stringResource(R.string.ako_kasni_svakih))
            IntervalSelector(form.overdueIntervalHours, viewModel::onInterval)
            Helper(stringResource(R.string.ako_kasni_napomena))

            val optional = stringResource(R.string.neobavezno)
            Label(
                buildAnnotatedString {
                    append(stringResource(R.string.napomena))
                    withStyle(SpanStyle(color = colors.faint, fontWeight = FontWeight.Normal)) { append(" $optional") }
                },
            )
            OutlinedTextField(
                value = form.note,
                onValueChange = viewModel::onNote,
                placeholder = { Text(stringResource(R.string.napomena_placeholder)) },
                minLines = 3,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            )
        }
        Button(
            onClick = { viewModel.save(onSaved = onClose) },
            colors = ButtonDefaults.buttonColors(containerColor = colors.pri, contentColor = colors.onPri),
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
        ) {
            Text(
                stringResource(if (state.isEdit) R.string.spremi_promjene else R.string.spremi_zadatak),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }

    when (picker) {
        TimeTarget.DUE -> TimePickerDialog(
            initial = form.time ?: LocalTime.of(9, 0),
            onConfirm = {
                viewModel.onTime(it)
                picker = null
            },
            onDismiss = { picker = null },
            onClear = {
                viewModel.onTime(null)
                picker = null
            },
        )
        TimeTarget.EARLY -> TimePickerDialog(
            initial = form.earlyTime,
            onConfirm = {
                viewModel.onEarlyTime(it)
                picker = null
            },
            onDismiss = { picker = null },
        )
        TimeTarget.DAY -> TimePickerDialog(
            initial = LocalTime.of(12, 0),
            onConfirm = {
                viewModel.onAddDayTime(it)
                picker = null
            },
            onDismiss = { picker = null },
        )
        null -> Unit
    }
}

@Composable
private fun TypeSelector(selected: TaskType, enabled: Boolean, onType: (TaskType) -> Unit) {
    val options = listOf(
        Triple(TaskType.RECURRING, R.string.tip_stalni, R.drawable.ic_repeat),
        Triple(TaskType.ONE_OFF, R.string.tip_povremeni, R.drawable.ic_today),
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (type, label, icon) ->
            SegmentedButton(
                selected = selected == type,
                onClick = { onType(type) },
                enabled = enabled || selected == type,
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                icon = { Icon(painterResource(icon), null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(label)) }
        }
    }
}

@Composable
private fun IntervalSelector(selected: Int, onSelect: (Int) -> Unit) {
    val options = listOf(1 to R.string.seg_1h, 2 to R.string.seg_2h, 4 to R.string.seg_4h)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (hours, label) ->
            SegmentedButton(
                selected = selected == hours,
                onClick = { onSelect(hours) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                icon = {},
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(label)) }
        }
    }
}

@Composable
private fun RuleCard(state: UrediUiState, viewModel: UrediViewModel) {
    val colors = LocalPodsjetnikColors.current
    val form = state.form
    Card {
        Column {
            RadioRow(stringResource(R.string.odredjeni_dan), selected = !form.lastDay, onClick = { viewModel.onLastDay(false) }) {
                if (!form.lastDay) {
                    Text(stringResource(R.string.svakog_dan_kratko, form.day), style = MaterialTheme.typography.labelLarge, color = colors.pri)
                }
            }
            DayOfMonthGrid(
                selected = form.day.takeUnless { form.lastDay },
                onSelect = viewModel::onDay,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Helper(stringResource(R.string.dan_napomena), Modifier.padding(16.dp))
            HorizontalDivider(color = colors.line)
            RadioRow(
                stringResource(R.string.zadnji_dan),
                selected = form.lastDay,
                subtitle = stringResource(R.string.zadnji_dan_opis),
                onClick = { viewModel.onLastDay(true) },
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().background(colors.c1).padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Icon(painterResource(R.drawable.ic_today), null, tint = colors.muted, modifier = Modifier.size(18.dp))
                Text(
                    stringResource(R.string.sljedeci_termin_label, shortDateText(state.nextDue, state.today)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.text,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun DateTimeRow(form: UrediForm, onTime: () -> Unit) {
    val colors = LocalPodsjetnikColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldBox(R.drawable.ic_today, formatShortDateWithYear(form.date, stringArrayResource(R.array.dani_kratko).toList()), Modifier.weight(1.6f))
        FieldBox(
            R.drawable.ic_clock,
            form.time?.let(::formatTime) ?: stringResource(R.string.bez_vremena),
            Modifier.weight(1f).clickable(onClick = onTime),
            textColor = if (form.time == null) colors.faint else colors.text,
        )
    }
}

@Composable
private fun FieldBox(icon: Int, text: String, modifier: Modifier = Modifier, textColor: androidx.compose.ui.graphics.Color? = null) {
    val colors = LocalPodsjetnikColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = 56.dp)
            .border(BorderStroke(1.dp, colors.outline), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp),
    ) {
        Icon(painterResource(icon), null, tint = colors.muted, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = textColor ?: colors.text, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null,
    trailing: @Composable () -> Unit = {},
) {
    val colors = LocalPodsjetnikColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(end = 16.dp),
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.text)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
        }
        trailing()
    }
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

@Composable
private fun Label(text: String) = Label(AnnotatedString(text))

@Composable
private fun Label(text: AnnotatedString) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = LocalPodsjetnikColors.current.text,
        modifier = Modifier.padding(top = 10.dp),
    )
}

@Composable
private fun Helper(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = LocalPodsjetnikColors.current.faint, modifier = modifier)
}
