package com.example.networkscanner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SignalWifi4Bar
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.networkscanner.data.HostInfo
import com.example.networkscanner.data.ScanProgress
import com.example.networkscanner.data.VulnerabilityInfo
import com.example.networkscanner.data.VulnerabilitySeverity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun NetworkScannerHeader(
    networkRange: String,
    onNetworkRangeChange: (String) -> Unit,
    onAutoDetect: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = networkRange,
                onValueChange = onNetworkRangeChange,
                label = { Text("Network Range (CIDR)") },
                placeholder = { Text("192.168.1.0/24") },
                leadingIcon = { Icon(Icons.Outlined.Router, contentDescription = null) },
                modifier = Modifier.weight(1f),
                enabled = !isScanning,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onAutoDetect,
                enabled = !isScanning,
                modifier = Modifier.height(56.dp) // Match TextField height
            ) {
                Icon(Icons.Outlined.Settings, contentDescription = "Auto-detect network", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Auto")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Column {
            Button(
                onClick = if (isScanning) onStopScan else onStartScan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(
                    imageVector = if (isScanning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isScanning) "Stop Scan" else "Start Scan")
            }
            
            // Squiggly loading line when scanning
            if (isScanning) {
                Spacer(modifier = Modifier.height(8.dp))
                ExpressiveLoadingIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3f
                )
            }
        }
    }
}

@Composable
fun ScanProgressCard(
    progress: ScanProgress,
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = progress.currentOperation,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isScanning && progress.totalHosts == 0 && !progress.isComplete) {
                // Material 3 Expressive squiggly loading indicator
                ExpressiveLoadingIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            } else if (progress.totalHosts > 0) {
                // Determinate progress with wavy indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${progress.completedHosts}/${progress.totalHosts}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    if (progress.currentHost.isNotEmpty()) {
                        Text(
                            text = progress.currentHost,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (isScanning) {
                    // Material 3 Expressive wavy progress indicator
                    ExpressiveWavyProgressIndicator(
                        progress = progress.completedHosts.toFloat() / progress.totalHosts.toFloat(),
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { progress.completedHosts.toFloat() / progress.totalHosts.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f)
                    )
                }
            } else if (!isScanning && progress.isComplete) {
                // Completed scan
                LinearProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f)
                )
            } else if (!isScanning && !progress.isComplete && progress.completedHosts > 0 && progress.totalHosts > 0) {
                // Stopped scan with some progress
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${progress.completedHosts}/${progress.totalHosts} (Stopped)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.completedHosts.toFloat() / progress.totalHosts.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                    trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                )
            }
        }
    }
}


@Composable
fun HostCard(
    host: HostInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = when {
                        host.openPorts.isNotEmpty() -> Icons.Filled.DevicesOther // More generic for devices
                        host.isReachable -> Icons.Filled.Wifi // Icon for network connection/reachable host
                        else -> Icons.Outlined.WifiOff
                    },
                    contentDescription = "Host Status",
                    tint = when {
                        host.openPorts.isNotEmpty() -> MaterialTheme.colorScheme.primary
                        host.isReachable -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(32.dp) // Slightly larger icon
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (host.hostname.isNotBlank() && host.hostname != host.ip) {
                        Text(
                            text = host.hostname,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = host.ip,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    } else {
                        Text(
                            text = host.ip,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (host.macAddress.isNotBlank()) {
                        Text(
                            text = "MAC: ${host.macAddress}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Badge(
                    containerColor = if (host.isReachable) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        if (host.isReachable) "Online" else "Offline",
                        color = if (host.isReachable) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (host.osInfo.isNotBlank()) {
                    DetailItem(icon = Icons.Outlined.Computer, text = "OS: ${host.osInfo}")
                }
                if (host.openPorts.isNotEmpty()) {
                    DetailItem(icon = Icons.Outlined.Security, text = "Ports: ${host.openPorts.joinToString { it.toString() }}")
                }
                if (host.responseTime > 0) {
                    DetailItem(icon = Icons.Outlined.Timer, text = "Ping: ${host.responseTime}ms")
                }
                if (host.signalStrength > -100 && host.signalStrength != -1) {
                    val signalColor = when {
                        host.signalStrength > -50 -> MaterialTheme.colorScheme.primary
                        host.signalStrength > -65 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                    DetailItem(icon = Icons.Outlined.SignalWifi4Bar, text = "Signal: ${host.signalStrength}dBm", iconTint = signalColor)
                }
            }
        }
    }
}

@Composable
private fun DetailItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = iconTint)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


@Composable
fun VulnerabilityCard(
    vulnerability: VulnerabilityInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = when (vulnerability.severity) {
                VulnerabilitySeverity.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                VulnerabilitySeverity.HIGH -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                VulnerabilitySeverity.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer
                VulnerabilitySeverity.LOW -> MaterialTheme.colorScheme.secondaryContainer
                VulnerabilitySeverity.INFO -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = when (vulnerability.severity) {
                        VulnerabilitySeverity.CRITICAL -> Icons.Filled.Dangerous
                        VulnerabilitySeverity.HIGH -> Icons.Filled.Warning
                        VulnerabilitySeverity.MEDIUM -> Icons.Filled.Info
                        VulnerabilitySeverity.LOW -> Icons.Outlined.Info
                        VulnerabilitySeverity.INFO -> Icons.Outlined.Lightbulb
                    },
                    contentDescription = "Severity Icon",
                    tint = when (vulnerability.severity) {
                        VulnerabilitySeverity.CRITICAL, VulnerabilitySeverity.HIGH -> MaterialTheme.colorScheme.error
                        VulnerabilitySeverity.MEDIUM -> MaterialTheme.colorScheme.tertiary
                        VulnerabilitySeverity.LOW -> MaterialTheme.colorScheme.secondary
                        VulnerabilitySeverity.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${vulnerability.host}:${vulnerability.port}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant // Ensure contrast
                    )
                    Text(
                        text = vulnerability.service,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Badge(
                    containerColor = when (vulnerability.severity) {
                        VulnerabilitySeverity.CRITICAL, VulnerabilitySeverity.HIGH -> MaterialTheme.colorScheme.error
                        VulnerabilitySeverity.MEDIUM -> MaterialTheme.colorScheme.tertiary
                        VulnerabilitySeverity.LOW -> MaterialTheme.colorScheme.secondary
                        VulnerabilitySeverity.INFO -> MaterialTheme.colorScheme.outline
                    },
                    contentColor = when (vulnerability.severity) {
                        VulnerabilitySeverity.CRITICAL, VulnerabilitySeverity.HIGH -> MaterialTheme.colorScheme.onError
                        VulnerabilitySeverity.MEDIUM -> MaterialTheme.colorScheme.onTertiary
                        VulnerabilitySeverity.LOW -> MaterialTheme.colorScheme.onSecondary
                        VulnerabilitySeverity.INFO -> MaterialTheme.colorScheme.inverseOnSurface
                    }
                ) {
                    Text(vulnerability.severity.name)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = vulnerability.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant // Ensure contrast
            )
            if (vulnerability.recommendation.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Recommendation:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = vulnerability.recommendation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScanHistoryCard(
    scanResult: com.example.networkscanner.ui.ScanResult,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = "Scan History",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = scanResult.networkRange,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateFormat.format(Date(scanResult.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.fillMaxWidth()
            ) {
                ScanStatChip(label = "Hosts", value = scanResult.hostsFound.toString(), icon = Icons.Outlined.Devices)
                ScanStatChip(label = "Vulnerabilities", value = scanResult.vulnerabilitiesFound.toString(), icon = Icons.Outlined.Security)
                ScanStatChip(label = "Time", value = "${scanResult.scanDuration / 1000}s", icon = Icons.Outlined.Timer)
            }
        }
    }
}

@Composable
fun ScanStatChip(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LegalDisclaimerDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (doNotShowAgain: Boolean) -> Unit
) {
    if (showDialog) {
        var doNotShowAgainChecked by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = { Icon(Icons.Filled.Warning, contentDescription = "Legal") },
            title = { Text("Legal Disclaimer") },
            text = {
                Column {
                    Text(
                        "By using this application, you acknowledge and agree that you are " +
                        "accessing and using it at your own risk. This tool is intended for " +
                        "educational and network analysis purposes only."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "It is not designed for, and must not be used for, any malicious activities, " +
                        "unauthorized access, or any action that could disrupt or harm networks or devices."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("The developers are not responsible for any misuse of this application.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { doNotShowAgainChecked = !doNotShowAgainChecked }
                            .padding(vertical = 8.dp)
                    ) {
                        Checkbox(
                            checked = doNotShowAgainChecked,
                            onCheckedChange = { doNotShowAgainChecked = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Do not show again")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { onConfirm(doNotShowAgainChecked) }) {
                    Text("Accept")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissRequest) {
                    Text("Decline")
                }
            }
        )
    }
}

@Composable
fun ExpressiveWavyProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
    strokeWidth: Float = 6f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wavy_progress")
    val animatedPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )
    
    Canvas(modifier = modifier.height(strokeWidth.dp)) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        
        // Draw track (background wavy line)
        drawWavyLine(
            width = width,
            centerY = centerY,
            color = trackColor,
            strokeWidth = strokeWidth,
            phase = 0f,
            progress = 1f
        )
        
        // Draw progress (animated wavy line)
        drawWavyLine(
            width = width,
            centerY = centerY,
            color = color,
            strokeWidth = strokeWidth,
            phase = animatedPhase,
            progress = progress
        )
    }
}

@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Float = 4f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_indicator")
    
    val animatedPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (4 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loading_phase"
    )
    
    val animatedAmplitude by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loading_amplitude"
    )
    
    Canvas(modifier = modifier.height(strokeWidth.dp)) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        
        // Draw animated squiggly line that moves across the screen
        drawSquigglyLoadingLine(
            width = width,
            centerY = centerY,
            color = color,
            strokeWidth = strokeWidth,
            phase = animatedPhase,
            amplitude = animatedAmplitude
        )
    }
}

private fun DrawScope.drawWavyLine(
    width: Float,
    centerY: Float,
    color: Color,
    strokeWidth: Float,
    phase: Float,
    progress: Float
) {
    val path = Path()
    val progressWidth = width * progress
    val amplitude = strokeWidth * 0.8f
    val frequency = 0.02f
    
    var startX = 0f
    path.moveTo(startX, centerY + amplitude * sin(startX * frequency + phase))
    
    while (startX < progressWidth) {
        val x = startX + 2f
        val y = centerY + amplitude * sin(x * frequency + phase)
        path.lineTo(x, y)
        startX = x
    }
    
    drawPath(
        path = path,
        color = color,
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round
        )
    )
}

private fun DrawScope.drawSquigglyLoadingLine(
    width: Float,
    centerY: Float,
    color: Color,
    strokeWidth: Float,
    phase: Float,
    amplitude: Float
) {
    val path = Path()
    val waveLength = width * 0.4f
    val actualAmplitude = strokeWidth * amplitude
    val frequency = 0.03f
    
    // Create a moving wave pattern
    val waveStart = (-waveLength + (phase % (2 * PI).toFloat() / (2 * PI).toFloat()) * (width + waveLength * 2))
    
    var startX = waveStart
    if (startX < width) {
        path.moveTo(maxOf(0f, startX), centerY + actualAmplitude * sin(startX * frequency + phase))
        
        while (startX < width + waveLength && startX < width) {
            val x = startX + 3f
            val y = centerY + actualAmplitude * sin(x * frequency + phase) * 
                    sin(x * frequency * 0.5f + phase * 0.7f) // Double wave for more expressiveness
            
            if (x >= 0) {
                path.lineTo(kotlin.math.min(width, x), y)
            }
            startX = x
        }
        
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round
            )
        )
    }
}

@Composable
fun EmptyStateContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
fun ExpressiveCircularLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Float = 6f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "circular_loading")
    
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val strokeWidthAnimation by infiniteTransition.animateFloat(
        initialValue = strokeWidth * 0.5f,
        targetValue = strokeWidth * 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stroke_width"
    )
    
    Canvas(
        modifier = modifier.size(48.dp)
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = kotlin.math.min(centerX, centerY) - strokeWidthAnimation / 2
        
        drawArc(
            color = color,
            startAngle = rotationAngle,
            sweepAngle = 270f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidthAnimation,
                cap = StrokeCap.Round
            )
        )
    }
}

@Composable
fun AnimatedBanner(
    message: String,
    isVisible: Boolean,
    isError: Boolean = false,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(durationMillis = 300, easing = EaseOutCubic)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(durationMillis = 300, easing = EaseInCubic)
        ) + fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isError) 
                    MaterialTheme.colorScheme.errorContainer 
                else 
                    MaterialTheme.colorScheme.tertiaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isError) Icons.Filled.Error else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (isError) 
                        MaterialTheme.colorScheme.onErrorContainer 
                    else 
                        MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isError) 
                        MaterialTheme.colorScheme.onErrorContainer 
                    else 
                        MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        tint = if (isError) 
                            MaterialTheme.colorScheme.onErrorContainer 
                        else 
                            MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BannerContainer(
    uiState: com.example.networkscanner.ui.ScanUiState,
    onDismissSuccess: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Success Banner
        AnimatedBanner(
            message = uiState.successMessage ?: "",
            isVisible = uiState.showSuccessBanner && uiState.successMessage != null,
            isError = false,
            onDismiss = onDismissSuccess
        )
        
        // Error Banner
        AnimatedBanner(
            message = uiState.error ?: "",
            isVisible = uiState.showErrorBanner && uiState.error != null,
            isError = true,
            onDismiss = onDismissError
        )
    }
}
