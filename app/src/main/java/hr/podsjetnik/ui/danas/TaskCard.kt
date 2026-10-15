package hr.podsjetnik.ui.danas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hr.podsjetnik.R
import hr.podsjetnik.domain.StatusKind
import hr.podsjetnik.ui.components.DoneButton
import hr.podsjetnik.ui.components.StatusChip
import hr.podsjetnik.ui.components.TypeTile
import hr.podsjetnik.ui.components.dateTimeText
import hr.podsjetnik.ui.components.reminderLabel
import hr.podsjetnik.ui.components.ruleText
import hr.podsjetnik.ui.components.shortDateText
import hr.podsjetnik.ui.theme.LocalPodsjetnikColors
import java.time.LocalDate

/** Danas card (HANDOFF §6.1): row 1 opens Detalji, row 2 = status, next reminder, Odrađeno. */
@Composable
fun TaskCard(card: DanasCard, today: LocalDate, onOpen: () -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalPodsjetnikColors.current
    val shape = RoundedCornerShape(20.dp)
    val (surface, outline, tile) = when (card.state.kind) {
        StatusKind.OVERDUE -> Triple(colors.redCard, colors.redLine, colors.card)
        StatusKind.TODAY -> Triple(colors.card, colors.ambLine, colors.c1)
        else -> Triple(colors.card, colors.line, colors.c1)
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surface, shape)
            .border(BorderStroke(1.dp, outline), shape)
            .padding(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        ) {
            TypeTile(card.type, tile)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(card.title, style = MaterialTheme.typography.titleMedium, color = colors.text)
                val meta = card.rule?.let { stringResource(R.string.meta_pravilo_datum, ruleText(it), shortDateText(card.dueDate, today)) }
                    ?: dateTimeText(card.dueDate, card.dueTime, today)
                Text(meta, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusChip(card.state)
            card.nextReminder?.let { next ->
                val label = reminderLabel(next, today)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_bell), null, tint = colors.muted, modifier = Modifier.size(18.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            DoneButton(onClick = onDone)
        }
    }
}
