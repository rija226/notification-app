package hr.podsjetnik.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hr.podsjetnik.R
import hr.podsjetnik.ui.theme.LocalPodsjetnikColors
import java.time.LocalTime

/** Removable time chip "09:00 ×" (HANDOFF §6.6). [onRemove] null disables × (last remaining time). */
@Composable
fun TimeChip(time: LocalTime, onRemove: (() -> Unit)?) {
    val colors = LocalPodsjetnikColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.heightIn(min = 48.dp).background(colors.priC, RoundedCornerShape(12.dp)).padding(start = 14.dp),
    ) {
        Text(formatTime(time), style = MaterialTheme.typography.bodyLarge, color = colors.onPriC)
        IconButton(onClick = { onRemove?.invoke() }, enabled = onRemove != null) {
            Icon(
                painterResource(R.drawable.ic_close),
                stringResource(R.string.cd_ukloni_vrijeme, formatTime(time)),
                tint = if (onRemove != null) colors.onPriC else colors.faint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Dashed "+ Dodaj vrijeme" chip. */
@Composable
fun AddTimeChip(onClick: () -> Unit, enabled: Boolean = true) {
    val colors = LocalPodsjetnikColors.current
    val outline = colors.outline
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .drawBehind {
                drawRoundRect(
                    color = outline,
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                )
            }
            .padding(horizontal = 14.dp),
    ) {
        Icon(painterResource(R.drawable.ic_add), null, tint = colors.pri, modifier = Modifier.size(20.dp))
        Text(
            stringResource(R.string.dodaj_vrijeme),
            style = MaterialTheme.typography.labelLarge,
            color = colors.pri,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/** "−  2 dana prije  +" (HANDOFF §6.6). */
@Composable
fun Stepper(label: String, onMinus: () -> Unit, onPlus: () -> Unit, canMinus: Boolean, canPlus: Boolean) {
    val colors = LocalPodsjetnikColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RoundButton(R.drawable.ic_remove, stringResource(R.string.cd_dan_manje), canMinus, onMinus)
        Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.text)
        RoundButton(R.drawable.ic_add, stringResource(R.string.cd_dan_vise), canPlus, onPlus)
    }
}

@Composable
private fun RoundButton(icon: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalPodsjetnikColors.current
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp).border(BorderStroke(1.dp, colors.outline), CircleShape),
    ) {
        Icon(painterResource(icon), description, tint = if (enabled) colors.text else colors.faint)
    }
}

/** 24 h time picker dialog. [onClear] adds "Bez vremena" (one-off due time is optional). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
    onClear: (() -> Unit)? = null,
) {
    val state = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) { Text(stringResource(R.string.u_redu)) }
        },
        dismissButton = {
            Row {
                if (onClear != null) TextButton(onClick = onClear) { Text(stringResource(R.string.bez_vremena)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.odustani)) }
            }
        },
        text = { TimePicker(state = state) },
    )
}
