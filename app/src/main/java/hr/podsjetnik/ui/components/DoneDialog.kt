package hr.podsjetnik.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import hr.podsjetnik.R
import hr.podsjetnik.ui.DoneDialogState
import hr.podsjetnik.ui.theme.LocalPodsjetnikColors
import kotlinx.coroutines.delay

private const val AUTO_DISMISS_MS = 4_000L

/** Odrađeno confirmation for Stalni (HANDOFF §6.4, screen 04). Auto-dismisses after ~4 s. */
@Composable
fun DoneDialog(state: DoneDialogState, onUndo: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalPodsjetnikColors.current
    LaunchedEffect(state.occurrenceId) {
        delay(AUTO_DISMISS_MS)
        onDismiss()
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.sheet, RoundedCornerShape(28.dp))
                .padding(24.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp).background(colors.grnC, CircleShape)) {
                Icon(painterResource(R.drawable.ic_check), null, tint = colors.grn, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.size(16.dp))
            Text(stringResource(R.string.odradjeno_naslov), style = MaterialTheme.typography.headlineMedium, color = colors.text)
            Text(state.title, style = MaterialTheme.typography.bodyLarge, color = colors.muted)
            Spacer(Modifier.size(20.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().background(colors.c1, RoundedCornerShape(20.dp)).padding(16.dp),
            ) {
                Text(stringResource(R.string.sljedeci_termin), style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                Text(formatDate(state.nextDue, state.today), style = MaterialTheme.typography.headlineLarge, color = colors.text)
                val weekday = stringArrayResource(R.array.dani_dugo)[state.nextDue.dayOfWeek.value - 1]
                val detail = state.rule?.let { stringResource(R.string.sljedeci_termin_opis, weekday, ruleText(it).replaceFirstChar { c -> c.lowercase() }) }
                    ?: weekday
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                state.earlyAt?.let {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                        Icon(painterResource(R.drawable.ic_bell), null, tint = colors.muted, modifier = Modifier.size(16.dp))
                        Text(
                            stringResource(R.string.rani_podsjetnik_kad, formatDate(it.toLocalDate(), state.today), formatTime(it.toLocalTime())),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.size(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onUndo) {
                    Icon(painterResource(R.drawable.ic_undo), null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.ponisti), style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.pri, contentColor = colors.onPri),
                ) { Text(stringResource(R.string.u_redu), style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}
