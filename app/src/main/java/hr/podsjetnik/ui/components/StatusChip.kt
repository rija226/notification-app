package hr.podsjetnik.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import hr.podsjetnik.domain.OccurrenceState
import hr.podsjetnik.domain.StatusKind
import hr.podsjetnik.ui.theme.LocalPodsjetnikColors

/** Status chip with fixed semantic colors (HANDOFF §7.1). Icons are added in Phase 5. */
@Composable
fun StatusChip(state: OccurrenceState, modifier: Modifier = Modifier) {
    val colors = LocalPodsjetnikColors.current
    val (background, content) = when (state.kind) {
        StatusKind.OVERDUE -> colors.redS to colors.onRedS
        StatusKind.TODAY -> colors.ambC to colors.onAmbC
        StatusKind.UPCOMING, StatusKind.LATER -> colors.neuC to colors.onNeuC
        StatusKind.DONE -> colors.grnC to colors.onGrnC
    }
    Chip(statusText(state), background, content, modifier)
}

@Composable
fun Chip(text: String, background: Color, content: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        modifier = modifier
            .background(background, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
