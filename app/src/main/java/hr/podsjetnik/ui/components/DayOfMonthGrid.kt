package hr.podsjetnik.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import hr.podsjetnik.R
import hr.podsjetnik.ui.theme.LocalPodsjetnikColors

/** 7-column grid of days 1..28 (HANDOFF §6.6). 29–31 are deliberately absent. */
@Composable
fun DayOfMonthGrid(selected: Int?, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalPodsjetnikColors.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..28).chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { day ->
                    val isSelected = day == selected
                    val description = stringResource(R.string.cd_svakog_dan, day)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) colors.pri else Color.Transparent)
                            .selectable(selected = isSelected, onClick = { onSelect(day) })
                            .semantics { contentDescription = description },
                    ) {
                        Text(
                            text = day.toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) colors.onPri else colors.text,
                        )
                    }
                }
            }
        }
    }
}
