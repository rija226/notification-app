package hr.podsjetnik.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import hr.podsjetnik.R
import hr.podsjetnik.reminders.DeviceStatus
import hr.podsjetnik.reminders.PermissionChecker
import hr.podsjetnik.ui.theme.LocalPodsjetnikColors

/** Device status, re-read every time the screen resumes (back from system settings) — PRD FR-9. */
@Composable
fun rememberDeviceStatus(): Pair<DeviceStatus, () -> Unit> {
    val context = LocalContext.current
    var status by remember { mutableStateOf(PermissionChecker.status(context)) }
    LifecycleResumeEffect(Unit) {
        status = PermissionChecker.status(context)
        onPauseOrDispose { }
    }
    return status to { status = PermissionChecker.status(context) }
}

/** Opens the system screens that fix each problem. */
class DeviceFixes(private val context: Context, private val requestNotifications: () -> Unit) {
    private val appUri = "package:${context.packageName}".toUri()

    /** Runtime dialog first; if already granted (or permanently denied), the app's notification settings. */
    fun notifications(permissionGranted: Boolean) {
        if (permissionGranted) {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        } else {
            requestNotifications()
        }
    }

    /** Direct exemption dialog: allowed for a sideloaded app (PRD §10.4, handoff O6). */
    @SuppressLint("BatteryLife")
    fun battery() = context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, appUri))

    /** App info page: battery → Unrestricted, and the way into Samsung's sleeping-app lists. */
    fun appSettings() = context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, appUri))
}

@Composable
fun rememberDeviceFixes(onNotificationResult: () -> Unit): DeviceFixes {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onNotificationResult() }
    return remember(context) { DeviceFixes(context) { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) } }
}

/** Numbered Samsung steps (PRD §10.5), shared by onboarding and Postavke. */
@Composable
fun SamsungSteps(modifier: Modifier = Modifier) {
    val colors = LocalPodsjetnikColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        listOf(R.string.samsung_korak_1, R.string.samsung_korak_2, R.string.samsung_korak_3, R.string.samsung_korak_4).forEach {
            Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = colors.text)
        }
    }
}
