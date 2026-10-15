package hr.podsjetnik.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hr.podsjetnik.R
import hr.podsjetnik.ui.theme.LocalPodsjetnikColors
import java.time.LocalDateTime

/** Odgodi bottom sheet (HANDOFF §6.3, screen 03). Snooze never changes the due date or status. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnoozeSheet(
    title: String,
    statusLabel: String,
    overdue: Boolean,
    now: LocalDateTime,
    onSnooze: (LocalDateTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalPodsjetnikColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.sheet,
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).navigationBarsPadding()) {
            Text(stringResource(R.string.odgodi_naslov), style = MaterialTheme.typography.titleLarge, color = colors.text)
            Text(
                stringResource(R.string.odgodi_podnaslov, title, statusLabel.replaceFirstChar { it.lowercase() }),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                snoozeOptions(now).forEach { option ->
                    val (icon, label) = when (option.kind) {
                        SnoozeKind.ONE_HOUR -> R.drawable.ic_clock to R.string.odgodi_1h
                        SnoozeKind.TONIGHT -> R.drawable.ic_moon to R.string.odgodi_veceras
                        SnoozeKind.TOMORROW_MORNING -> R.drawable.ic_sun to R.string.odgodi_sutra
                    }
                    val whenText = if (option.at.toLocalDate() == now.toLocalDate()) {
                        stringResource(R.string.odgodi_danas_u, formatTime(option.at.toLocalTime()))
                    } else {
                        dateTimeText(option.at.toLocalDate(), option.at.toLocalTime(), now.toLocalDate())
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 68.dp)
                            .clickable { onSnooze(option.at) }
                            .padding(vertical = 6.dp),
                    ) {
                        IconTile(icon, colors.c1, colors.text, size = 52.dp)
                        Column {
                            Text(stringResource(label), style = MaterialTheme.typography.titleMedium, color = colors.text)
                            Text(whenText, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth().background(colors.c1, RoundedCornerShape(16.dp)).padding(16.dp),
            ) {
                Icon(painterResource(R.drawable.ic_info), null, tint = colors.muted, modifier = Modifier.size(20.dp))
                Text(
                    stringResource(if (overdue) R.string.odgodi_napomena_kasni else R.string.odgodi_napomena),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.odustani), style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}
