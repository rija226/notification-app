package hr.podsjetnik.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import hr.podsjetnik.R
import hr.podsjetnik.domain.TaskType
import hr.podsjetnik.ui.theme.LocalPodsjetnikColors

/** 40 dp tile with the task type icon: repeat = Stalni, calendar = Povremeni (HANDOFF §6.1). */
@Composable
fun TypeTile(type: TaskType, background: Color, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val colors = LocalPodsjetnikColors.current
    val description = stringResource(if (type == TaskType.RECURRING) R.string.cd_tip_stalni else R.string.cd_tip_povremeni)
    IconTile(
        icon = if (type == TaskType.RECURRING) R.drawable.ic_repeat else R.drawable.ic_today,
        background = background,
        tint = colors.muted,
        size = size,
        modifier = modifier.semantics { contentDescription = description },
    )
}

@Composable
fun IconTile(@DrawableRes icon: Int, background: Color, tint: Color, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size).background(background, RoundedCornerShape(12.dp)),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Section header with colored dot, label, count badge and optional hint (HANDOFF §6.1). */
@Composable
fun SectionHeader(
    label: String,
    dot: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
    count: Int? = null,
    badge: Pair<Color, Color>? = null,
    hint: String? = null,
) {
    val colors = LocalPodsjetnikColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Box(Modifier.size(8.dp).background(dot, CircleShape))
        Text(label, style = MaterialTheme.typography.titleSmall, color = labelColor)
        if (count != null && badge != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.heightIn(min = 22.dp).background(badge.first, CircleShape).padding(horizontal = 8.dp),
            ) {
                Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = badge.second)
            }
        }
        if (hint != null) Text(hint, style = MaterialTheme.typography.bodyMedium, color = colors.faint)
    }
}

/** "Odrađeno" button: tonal on cards, filled in Detalji (HANDOFF §7.4). */
@Composable
fun DoneButton(onClick: () -> Unit, modifier: Modifier = Modifier, filled: Boolean = false) {
    val colors = LocalPodsjetnikColors.current
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (filled) colors.grnS else colors.grnC,
            contentColor = if (filled) colors.onGrnS else colors.onGrnC,
        ),
    ) {
        Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(20.dp))
        Text(
            stringResource(R.string.akcija_odradjeno),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Big circle + title + text, used by empty states and the Odrađeno dialog. */
@Composable
fun EmptyState(
    @DrawableRes icon: Int,
    title: String,
    text: String?,
    circle: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPodsjetnikColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(104.dp).background(circle, CircleShape)) {
            Icon(painterResource(icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(52.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, color = colors.text, textAlign = TextAlign.Center)
        if (text != null) {
            Text(text, style = MaterialTheme.typography.bodyLarge, color = colors.muted, textAlign = TextAlign.Center)
        }
    }
}
