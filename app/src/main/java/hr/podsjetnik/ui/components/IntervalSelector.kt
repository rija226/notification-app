package hr.podsjetnik.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hr.podsjetnik.R

/** "Ako kasni, podsjeti svakih" 1 sat / 2 sata / 4 sata (HANDOFF §6.6, §6.7). */
@Composable
fun IntervalSelector(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val options = listOf(1 to R.string.seg_1h, 2 to R.string.seg_2h, 4 to R.string.seg_4h)
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
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
