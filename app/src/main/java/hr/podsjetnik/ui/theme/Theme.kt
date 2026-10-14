package hr.podsjetnik.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/** App theme. Dynamic color is deliberately off: status colors are semantic (HANDOFF §2). */
@Composable
fun PodsjetnikTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkPodsjetnikColors else LightPodsjetnikColors
    CompositionLocalProvider(LocalPodsjetnikColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(darkTheme),
            typography = PodsjetnikTypography,
            content = content,
        )
    }
}
