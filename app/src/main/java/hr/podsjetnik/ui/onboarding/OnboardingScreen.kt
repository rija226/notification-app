package hr.podsjetnik.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import hr.podsjetnik.R
import hr.podsjetnik.ui.components.IconTile
import hr.podsjetnik.ui.components.SamsungSteps
import hr.podsjetnik.ui.components.rememberDeviceFixes
import hr.podsjetnik.ui.components.rememberDeviceStatus
import hr.podsjetnik.ui.theme.LocalPodsjetnikColors

private enum class Step { NOTIFICATIONS, BATTERY, SAMSUNG }

/**
 * First launch, 3 steps (HANDOFF §6.8 with PRD H10): notifications, battery, Samsung sleeping apps.
 * Steps already satisfied are skipped; "Kasnije" moves on. Missing items stay visible in Postavke.
 */
@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel) {
    val colors = LocalPodsjetnikColors.current
    val (status, recheck) = rememberDeviceStatus()
    val samsungConfirmed by viewModel.samsungConfirmed.collectAsStateWithLifecycle()
    var index by rememberSaveable { mutableIntStateOf(0) }
    val fixes = rememberDeviceFixes(onNotificationResult = {
        recheck()
        index++ // granted or not, move on (handoff §6.8)
    })
    val steps = Step.entries
    fun satisfied(step: Step) = when (step) {
        Step.NOTIFICATIONS -> status.notificationPermission
        Step.BATTERY -> status.batteryExempt
        Step.SAMSUNG -> samsungConfirmed
    }
    LaunchedEffect(index, status, samsungConfirmed) {
        var next = index
        while (next < steps.size && satisfied(steps[next])) next++
        if (next >= steps.size) viewModel.finish() else index = next
    }
    val step = steps.getOrNull(index) ?: return Box(Modifier.fillMaxSize().background(colors.bg))

    Column(modifier = Modifier.fillMaxSize().background(colors.bg).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            steps.indices.forEach { i ->
                Box(
                    Modifier
                        .padding(end = 6.dp)
                        .width(28.dp)
                        .height(4.dp)
                        .background(if (i <= index) colors.pri else colors.c2, RoundedCornerShape(2.dp)),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.onboarding_korak, index + 1, steps.size),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 32.dp),
        ) {
            Illustration(step)
            Spacer(Modifier.height(16.dp))
            val (title, text) = when (step) {
                Step.NOTIFICATIONS -> R.string.ob1_naslov to R.string.ob1_tekst
                Step.BATTERY -> R.string.ob3_naslov to R.string.ob3_tekst
                Step.SAMSUNG -> R.string.samsung_naslov to R.string.samsung_tekst
            }
            Text(stringResource(title), style = MaterialTheme.typography.headlineMedium, color = colors.text)
            Text(stringResource(text), style = MaterialTheme.typography.bodyLarge, color = colors.muted)
            when (step) {
                Step.BATTERY -> Text(stringResource(R.string.ob3_napomena_dijalog), style = MaterialTheme.typography.bodyMedium, color = colors.faint)
                Step.SAMSUNG -> SamsungSteps()
                Step.NOTIFICATIONS -> Unit
            }
        }
        val primary = when (step) {
            Step.NOTIFICATIONS -> R.string.ob1_gumb
            Step.BATTERY -> R.string.ob3_gumb
            Step.SAMSUNG -> R.string.samsung_gumb
        }
        Button(
            onClick = {
                when (step) {
                    Step.NOTIFICATIONS -> fixes.notifications(status.notificationPermission)
                    Step.BATTERY -> fixes.battery()
                    Step.SAMSUNG -> fixes.appSettings()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = colors.pri, contentColor = colors.onPri),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(stringResource(primary), style = MaterialTheme.typography.labelLarge) }
        if (step == Step.SAMSUNG) {
            OutlinedButton(onClick = viewModel::confirmSamsung, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 48.dp)) {
                Text(stringResource(R.string.samsung_potvrda), color = colors.text)
            }
        }
        TextButton(onClick = { index++ }, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)) {
            Text(stringResource(R.string.kasnije), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Light circle with a sample card, like screens 08a/08c. */
@Composable
private fun Illustration(step: Step) {
    val colors = LocalPodsjetnikColors.current
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(240.dp)) {
        Box(Modifier.size(220.dp).background(colors.priC.copy(alpha = 0.5f), CircleShape))
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(0.85f).background(colors.card, RoundedCornerShape(24.dp)).padding(16.dp),
        ) {
            when (step) {
                Step.NOTIFICATIONS -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp).background(colors.pri, CircleShape)) {
                            Icon(painterResource(R.drawable.ic_bell), null, tint = colors.onPri, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            stringResource(R.string.app_name),
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.text,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Text(stringResource(R.string.ob1_primjer_naslov), style = MaterialTheme.typography.titleMedium, color = colors.text)
                    Text(stringResource(R.string.ob1_primjer_tekst), style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(R.string.notif_akcija_odradjeno, R.string.notif_akcija_odgodi).forEach {
                            Text(
                                stringResource(it),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.text,
                                modifier = Modifier.background(colors.c1, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
                Step.BATTERY -> {
                    Text(stringResource(R.string.ob3_primjer_naslov), style = MaterialTheme.typography.labelMedium, color = colors.muted)
                    listOf(R.string.ob3_neograniceno, R.string.ob3_optimizirano, R.string.ob3_ograniceno).forEachIndexed { i, label ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (i == 0) colors.priC else colors.card, RoundedCornerShape(16.dp)),
                        ) {
                            RadioButton(selected = i == 0, onClick = null, modifier = Modifier.padding(12.dp))
                            Text(stringResource(label), style = MaterialTheme.typography.bodyLarge, color = if (i == 0) colors.onPriC else colors.muted)
                        }
                    }
                }
                Step.SAMSUNG -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(R.drawable.ic_moon, colors.ambC, colors.onAmbC)
                        Text(
                            stringResource(R.string.dozvola_samsung),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.text,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
        }
    }
}
