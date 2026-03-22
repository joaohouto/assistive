package com.joaohouto.assistivetouch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.joaohouto.assistivetouch.ui.theme.AssistiveTouchTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SettingsRepository(this)
        enableEdgeToEdge()
        setContent {
            AssistiveTouchTheme {
                MainScreen(
                    settings = settings,
                    hasOverlayPermission = { Settings.canDrawOverlays(this) },
                    hasAccessibilityService = { isAccessibilityServiceEnabled() },
                    requestOverlayPermission = {
                        startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                        )
                    },
                    openAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    isServiceRunning = { FloatingButtonService.isRunning },
                    toggleService = {
                        val svc = Intent(this, FloatingButtonService::class.java)
                        if (FloatingButtonService.isRunning) stopService(svc)
                        else startForegroundService(svc)
                    }
                )
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains(packageName, ignoreCase = true)
    }
}

// ── Main screen ───────────────────────────────────────────────────────────────

@Composable
private fun MainScreen(
    settings: SettingsRepository,
    hasOverlayPermission: () -> Boolean,
    hasAccessibilityService: () -> Boolean,
    requestOverlayPermission: () -> Unit,
    openAccessibilitySettings: () -> Unit,
    isServiceRunning: () -> Boolean,
    toggleService: () -> Unit
) {
    var overlayGranted       by remember { mutableStateOf(hasOverlayPermission()) }
    var accessibilityEnabled by remember { mutableStateOf(hasAccessibilityService()) }
    var serviceRunning       by remember { mutableStateOf(isServiceRunning()) }

    LaunchedEffect(Unit) {
        while (true) {
            overlayGranted       = hasOverlayPermission()
            accessibilityEnabled = hasAccessibilityService()
            serviceRunning       = isServiceRunning()
            delay(500)
        }
    }

    val allGranted = overlayGranted && accessibilityEnabled

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Text("AssistiveTouch", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Botão flutuante de acessibilidade",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            // ── Permissions ──────────────────────────────────────────────
            PermissionRow(
                label = "Sobreposição de apps",
                granted = overlayGranted,
                onRequest = requestOverlayPermission
            )
            Spacer(Modifier.height(8.dp))
            PermissionRow(
                label = "Serviço de acessibilidade",
                granted = accessibilityEnabled,
                onRequest = openAccessibilitySettings
            )

            Spacer(Modifier.height(16.dp))

            // ── Toggle ───────────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("AssistiveTouch", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.size(2.dp))
                        Text(
                            text = when {
                                !allGranted    -> "Permissões necessárias"
                                serviceRunning -> "Ativo"
                                else           -> "Inativo"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                !allGranted    -> MaterialTheme.colorScheme.error
                                serviceRunning -> MaterialTheme.colorScheme.primary
                                else           -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    Switch(
                        checked = serviceRunning && allGranted,
                        onCheckedChange = { if (allGranted) toggleService() },
                        enabled = allGranted
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Settings ─────────────────────────────────────────────────
            SettingsSection(settings)

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Settings section ──────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(settings: SettingsRepository) {
    var opacity    by remember { mutableFloatStateOf(settings.opacity) }
    var sizeDp     by remember { mutableIntStateOf(settings.buttonSizeDp) }
    var actions    by remember { mutableStateOf(settings.menuActions.toSet()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Configurações", style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // Opacity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Opacidade", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${(opacity * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Slider(
                value = opacity,
                onValueChange = { opacity = it },
                onValueChangeFinished = { settings.opacity = opacity },
                valueRange = 0.3f..1.0f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Size
            Text("Tamanho", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SettingsRepository.SIZE_OPTIONS.forEachIndexed { index, dp ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = SettingsRepository.SIZE_OPTIONS.size
                        ),
                        onClick = {
                            sizeDp = dp
                            settings.buttonSizeDp = dp
                        },
                        selected = sizeDp == dp
                    ) {
                        Text(SettingsRepository.SIZE_LABELS[index])
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // Actions
            Text("Ações do menu", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))

            MenuAction.entries.forEach { action ->
                val checked = action in actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { on ->
                            actions = if (on) actions + action else actions - action
                            settings.menuActions = actions.toList()
                        }
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        stringResource(action.labelRes()),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            AnimatedVisibility(visible = actions.isEmpty()) {
                Text(
                    "Selecione ao menos uma ação",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// ── Permission row ────────────────────────────────────────────────────────────

@Composable
private fun PermissionRow(label: String, granted: Boolean, onRequest: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (granted)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(
                        if (granted) android.R.drawable.checkbox_on_background
                        else android.R.drawable.checkbox_off_background
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (granted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(12.dp))
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
            if (!granted) {
                OutlinedButton(
                    onClick = onRequest,
                    modifier = Modifier.height(32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp, vertical = 0.dp
                    )
                ) {
                    Text("Ativar", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
