package hr.podsjetnik.ui.components

import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import hr.podsjetnik.R
import hr.podsjetnik.ui.theme.LocalPodsjetnikColors

/** Extended FAB "Novi zadatak" on Danas and Svi zadaci (HANDOFF §6). */
@Composable
fun NewTaskFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalPodsjetnikColors.current
    val label = stringResource(R.string.fab_novi_zadatak)
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
        text = { Text(label) },
        containerColor = colors.priC,
        contentColor = colors.onPriC,
        // The text slot isn't exposed to accessibility (uiautomator showed an empty node); name the button explicitly.
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = label },
    )
}
