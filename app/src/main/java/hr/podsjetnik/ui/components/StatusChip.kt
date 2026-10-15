package hr.podsjetnik.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import hr.podsjetnik.R
import hr.podsjetnik.domain.OccurrenceState
import hr.podsjetnik.domain.StatusKind
import hr.podsjetnik.ui.theme.LocalPodsjetnikColors

/**
 * Status chip with fixed semantic colors and an icon, so status is never color-only
 * (HANDOFF §4.2, §7.1, §9): overdue = alert, today = clock, upcoming = no icon.
 */
@Composable
fun StatusChip(state: OccurrenceState, modifier: Modifier = Modifier) {
    val colors = LocalPodsjetnikColors.current
    when (state.kind) {
        StatusKind.OVERDUE -> Chip(statusText(state), colors.redS, colors.onRedS, modifier, R.drawable.ic_alert)
        StatusKind.TODAY -> Chip(statusText(state), colors.ambC, colors.onAmbC, modifier, R.drawable.ic_clock)
        StatusKind.UPCOMING, StatusKind.LATER -> Chip(statusText(state), colors.neuC, colors.onNeuC, modifier)
        StatusKind.DONE -> Chip(statusText(state), colors.grnC, colors.onGrnC, modifier, R.drawable.ic_check)
    }
}

@Composable
fun Chip(
    text: String,
    background: Color,
    content: Color,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .background(background, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        if (icon != null) Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = content)
    }
}
