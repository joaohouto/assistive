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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
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
                    requestPermission = {
                        startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                        )
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
}

@Composable
private fun MainScreen(
    hasOverlayPermission: () -> Boolean,
    requestPermission: () -> Unit,
    isServiceRunning: () -> Boolean,
    toggleService: () -> Unit
) {
    var permissionGranted by remember { mutableStateOf(hasOverlayPermission()) }
    var serviceRunning by remember { mutableStateOf(isServiceRunning()) }

    // Poll state on resume (permission can be granted in Settings and user returns)
    LaunchedEffect(Unit) {
        while (true) {
            permissionGranted = hasOverlayPermission()
            serviceRunning = isServiceRunning()
            delay(500)
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "AssistiveTouch",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Botão flutuante de acessibilidade",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (!permissionGranted) {
                PermissionCard(onGrant = requestPermission)
            } else {
                ServiceToggleCard(
                    running = serviceRunning,
                    onToggle = {
                        toggleService()
                        serviceRunning = !serviceRunning
                    }
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(onGrant: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Permissão necessária",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "O AssistiveTouch precisa de permissão para exibir sobre outros apps.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onGrant) {
                Text("Conceder Permissão")
            }
        }
    }
}

@Composable
private fun ServiceToggleCard(running: Boolean, onToggle: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "AssistiveTouch",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.size(2.dp))
                Text(
                    text = if (running) "Ativo" else "Inativo",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (running)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = running, onCheckedChange = { onToggle() })
        }
    }
}
