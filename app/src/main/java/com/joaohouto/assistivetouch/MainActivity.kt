package com.joaohouto.assistivetouch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.joaohouto.assistivetouch.ui.theme.AssistiveTouchTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AssistiveTouchTheme {
                MainScreen(
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
                        if (FloatingButtonService.isRunning) {
                            stopService(Intent(this, FloatingButtonService::class.java))
                        } else {
                            startForegroundService(Intent(this, FloatingButtonService::class.java))
                        }
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

@Composable
private fun MainScreen(
    hasOverlayPermission: () -> Boolean,
    hasAccessibilityService: () -> Boolean,
    requestOverlayPermission: () -> Unit,
    openAccessibilitySettings: () -> Unit,
    isServiceRunning: () -> Boolean,
    toggleService: () -> Unit
) {
    var overlayGranted by remember { mutableStateOf(hasOverlayPermission()) }
    var accessibilityEnabled by remember { mutableStateOf(hasAccessibilityService()) }
    var serviceRunning by remember { mutableStateOf(isServiceRunning()) }

    LaunchedEffect(Unit) {
        while (true) {
            overlayGranted = hasOverlayPermission()
            accessibilityEnabled = hasAccessibilityService()
            serviceRunning = isServiceRunning()
            delay(500)
        }
    }

    val allGranted = overlayGranted && accessibilityEnabled

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("AssistiveTouch", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Botão flutuante de acessibilidade",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(40.dp))

            // ── Permission checklist ──────────────────────────────────────
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

            Spacer(Modifier.height(24.dp))

            // ── Toggle ────────────────────────────────────────────────────
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
                                !allGranted -> "Permissões necessárias"
                                serviceRunning -> "Ativo"
                                else -> "Inativo"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                !allGranted -> MaterialTheme.colorScheme.error
                                serviceRunning -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
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
        }
    }
}

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
                    tint = if (granted)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
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
