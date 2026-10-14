package hr.podsjetnik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hr.podsjetnik.ui.theme.LocalPodsjetnikColors
import hr.podsjetnik.ui.theme.PodsjetnikTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PodsjetnikTheme {
                PlaceholderScreen()
            }
        }
    }
}

// ponytail: Phase 0 placeholder to check font, colors and insets; replaced by navigation in Phase 2.
@Composable
private fun PlaceholderScreen(modifier: Modifier = Modifier) {
    val colors = LocalPodsjetnikColors.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.danas_naslov),
            style = MaterialTheme.typography.headlineLarge,
            color = colors.text,
        )
        Text(
            text = stringResource(R.string.prazno_danas_tekst),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
        )
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlaceholderScreenPreview() {
    PodsjetnikTheme {
        PlaceholderScreen()
    }
}
