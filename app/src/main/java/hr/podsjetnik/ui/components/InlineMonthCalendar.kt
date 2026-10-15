package hr.podsjetnik.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hr.podsjetnik.R
import hr.podsjetnik.ui.theme.LocalPodsjetnikColors
import java.time.LocalDate
import java.time.YearMonth

/** Month calendar, Monday first, today ringed, selected filled (HANDOFF §6.6, screen 06b). */
@Composable
fun InlineMonthCalendar(selected: LocalDate, today: LocalDate, onSelect: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalPodsjetnikColors.current
    var month by rememberSaveable(selected) { mutableStateOf(YearMonth.from(selected).toString()) }
    val shown = YearMonth.parse(month)
    val monthName = stringArrayResource(R.array.mjeseci)[shown.monthValue - 1]
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.kalendar_mjesec, monthName, shown.year),
                style = MaterialTheme.typography.titleMedium,
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { month = shown.minusMonths(1).toString() }) {
                Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.cd_prethodni_mjesec), tint = colors.text)
            }
            IconButton(onClick = { month = shown.plusMonths(1).toString() }) {
                Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.cd_sljedeci_mjesec), tint = colors.text)
            }
        }
        val letters = stringArrayResource(R.array.dani_slova)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            letters.forEach { letter ->
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
                    Text(letter, style = MaterialTheme.typography.labelMedium, color = colors.faint)
                }
            }
        }
        val leading = shown.atDay(1).dayOfWeek.value - 1
        val cells = List(leading) { null } + (1..shown.lengthOfMonth()).map { shown.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { date ->
                    if (date == null) {
                        Spacer(Modifier.size(44.dp))
                    } else {
                        val isSelected = date == selected
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) colors.pri else colors.card)
                                .then(if (date == today && !isSelected) Modifier.border(BorderStroke(1.dp, colors.pri), CircleShape) else Modifier)
                                .selectable(selected = isSelected, onClick = { onSelect(date) }),
                        ) {
                            Text(
                                date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = when {
                                    isSelected -> colors.onPri
                                    date == today -> colors.pri
                                    else -> colors.text
                                },
                            )
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.size(44.dp)) }
            }
        }
    }
}
