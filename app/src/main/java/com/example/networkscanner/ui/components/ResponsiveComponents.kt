package com.example.networkscanner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.DeviceHub
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SignalWifi4Bar
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.networkscanner.data.DeviceType
import com.example.networkscanner.data.HostInfo
import com.example.networkscanner.ui.SortOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SortOptionsRow(
    currentSort: SortOption,
    onSortChange: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAllOptions by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sort by: ${currentSort.displayName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                TextButton(
                    onClick = { showAllOptions = !showAllOptions }
                ) {
                    Text(if (showAllOptions) "Less" else "More")
                    Icon(
                        imageVector = if (showAllOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            AnimatedVisibility(
                visible = showAllOptions,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sortOptions = listOf(
                        SortOption.IP_ADDRESS, SortOption.HOSTNAME, SortOption.SIGNAL_STRENGTH,
                        SortOption.RESPONSE_TIME, SortOption.OPEN_PORTS, SortOption.LAST_SEEN, 
                        SortOption.DEVICE_TYPE
                    )
                    
                    sortOptions.chunked(2).forEach { rowOptions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowOptions.forEach { option ->
                                FilterChip(
                                    onClick = { onSortChange(option) },
                                    label = { Text(option.displayName) },
                                    selected = currentSort == option,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = when (option) {
                                                SortOption.IP_ADDRESS -> Icons.Outlined.Router
                                                SortOption.HOSTNAME -> Icons.Outlined.Computer
                                                SortOption.SIGNAL_STRENGTH -> Icons.Filled.SignalWifi4Bar
                                                SortOption.RESPONSE_TIME -> Icons.Outlined.Speed
                                                SortOption.OPEN_PORTS -> Icons.Outlined.Security
                                                SortOption.LAST_SEEN -> Icons.Outlined.Schedule
                                                SortOption.DEVICE_TYPE -> Icons.Outlined.DeviceHub
                                                else -> Icons.Outlined.Router
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Add empty space if odd number of options in row
                            if (rowOptions.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResponsiveStatisticsGrid(
    totalHosts: Int,
    vulnerabilities: Int,
    criticalVulns: Int,
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isCompact = screenWidth < 600.dp
    
    if (isCompact) {
        // Compact layout - vertical
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HostsFoundCard(
                hostCount = totalHosts,
                isAnimating = isScanning,
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                AnimatedStatisticCard(
                    title = "Vulnerabilities",
                    value = vulnerabilities.toString(),
                    icon = Icons.Filled.Security,
                    color = if (vulnerabilities == 0) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.error,
                    isAnimating = isScanning,
                    modifier = Modifier.width(120.dp)
                )
                
                AnimatedStatisticCard(
                    title = "Critical",
                    value = criticalVulns.toString(),
                    icon = Icons.Filled.Warning,
                    color = if (criticalVulns == 0) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.error,
                    isAnimating = isScanning,
                    modifier = Modifier.width(120.dp)
                )
            }
        }
    } else {
        // Expanded layout - horizontal with symmetric sizing
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            HostsFoundCard(
                hostCount = totalHosts,
                isAnimating = isScanning,
                modifier = Modifier.width(120.dp)
            )
            
            AnimatedStatisticCard(
                title = "Vulnerabilities",
                value = vulnerabilities.toString(),
                icon = Icons.Filled.Security,
                color = if (vulnerabilities == 0) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.error,
                isAnimating = isScanning,
                modifier = Modifier.width(120.dp)
            )
            
            AnimatedStatisticCard(
                title = "Critical",
                value = criticalVulns.toString(),
                icon = Icons.Filled.Warning,
                color = if (criticalVulns == 0) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.error,
                isAnimating = isScanning,
                modifier = Modifier.width(120.dp)
            )
        }
    }
}

@Composable
private fun AnimatedStatisticCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isAnimating: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "stat_animation")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isAnimating) 0.7f else 1f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = pulseAlpha)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isAnimating) 8.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun HostsFoundCard(
    hostCount: Int,
    isAnimating: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hosts_animation")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isAnimating) 0.7f else 1f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = pulseAlpha)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isAnimating) 8.dp else 4.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Main host count info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Devices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = hostCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "Hosts Found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            
            // Right side - Device info using this app
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.PhoneAndroid,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Scanner Device",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = android.os.Build.MODEL.take(10),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Android ${android.os.Build.VERSION.RELEASE}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun EnhancedHostCard(
    host: HostInfo,
    isSelected: Boolean,
    onHostSelected: (HostInfo) -> Unit,
    onVulnerabilityScan: () -> Unit,
    isVulnerabilityScanning: Boolean,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    
    Card(
        modifier = modifier
            .clickable { onHostSelected(host) },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.secondaryContainer 
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Host header with device type indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Device icon based on type
                Icon(
                    imageVector = when (host.deviceType) {
                        DeviceType.ROUTER -> Icons.Filled.Router
                        DeviceType.COMPUTER -> Icons.Filled.Computer
                        DeviceType.MOBILE -> Icons.Filled.PhoneAndroid
                        DeviceType.PRINTER -> Icons.Filled.Print
                        DeviceType.IOT -> Icons.Filled.Sensors
                        DeviceType.SERVER -> Icons.Filled.Storage
                        DeviceType.UNKNOWN -> Icons.Filled.DeviceUnknown
                    },
                    contentDescription = null,
                    tint = when (host.deviceType) {
                        DeviceType.ROUTER -> MaterialTheme.colorScheme.primary
                        DeviceType.SERVER -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.size(28.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    // Prioritize hostname display with enhanced hierarchy
                    if (host.hostname.isNotEmpty() && host.hostname != host.ip) {
                        // Use hostname as primary identifier with larger text
                        Text(
                            text = host.hostname,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Show device type right below hostname
                        Text(
                            text = host.deviceType.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // IP address becomes secondary, smaller text
                        Text(
                            text = host.ip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Normal
                        )
                    } else {
                        // Fallback to IP with device type
                        Text(
                            text = host.ip,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = host.deviceType.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Show OS information if available
                    if (host.osInfo.isNotEmpty() && host.osInfo != "Unknown") {
                        Text(
                            text = host.osInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Status indicators
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    // Connection status
                    Badge(
                        containerColor = if (host.isReachable) 
                            MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.outline
                    ) {
                        Text(if (host.isReachable) "Online" else "Offline")
                    }
                    
                    // Signal strength (if available)
                    if (host.signalStrength > -100 && host.signalStrength != -1) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    host.signalStrength > -50 -> Icons.Filled.SignalWifi4Bar
                                    host.signalStrength > -65 -> Icons.Filled.SignalWifi4Bar
                                    host.signalStrength > -80 -> Icons.Filled.SignalWifi4Bar
                                    else -> Icons.Filled.SignalWifi4Bar
                                },
                                contentDescription = "Signal Strength",
                                tint = when {
                                    host.signalStrength > -50 -> MaterialTheme.colorScheme.primary
                                    host.signalStrength > -65 -> Color(0xFFFF9800)
                                    else -> MaterialTheme.colorScheme.error
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${host.signalStrength}dBm",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Device info row
            if (host.openPorts.isNotEmpty() || host.responseTime > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (host.openPorts.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Security,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${host.openPorts.size} ports",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    if (host.responseTime > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${host.responseTime}ms",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Enhanced device information section
            Spacer(modifier = Modifier.height(12.dp))
            
            // Device category and manufacturer info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Device Type:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = host.deviceType.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (host.macAddress.isNotEmpty()) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "MAC Address:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = host.macAddress.take(8) + "...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            
            // Network information
            if (host.responseTime > 0 || host.signalStrength > -100) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (host.responseTime > 0) {
                        Column {
                            Text(
                                text = "Response Time:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Speed,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = when {
                                        host.responseTime < 50 -> MaterialTheme.colorScheme.primary
                                        host.responseTime < 100 -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${host.responseTime}ms",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    if (host.signalStrength > -100 && host.signalStrength != -1) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Signal Strength:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when {
                                        host.signalStrength > -50 -> Icons.Filled.SignalWifi4Bar
                                        host.signalStrength > -65 -> Icons.Filled.SignalWifi4Bar
                                        host.signalStrength > -80 -> Icons.Filled.SignalWifi4Bar
                                        else -> Icons.Filled.SignalWifi4Bar
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = when {
                                        host.signalStrength > -50 -> MaterialTheme.colorScheme.primary
                                        host.signalStrength > -65 -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${host.signalStrength}dBm",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            
            // Services preview with enhanced display
            if (host.services.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "Active Services (${host.services.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (host.openPorts.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Security,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (host.openPorts.size > 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${host.openPorts.size} open ports",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(host.services.values.take(5).toList()) { service ->
                        AssistChip(
                            onClick = { },
                            label = { 
                                Text(
                                    text = "${service.port}/${service.service}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                ) 
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                    
                    if (host.services.size > 5) {
                        item {
                            AssistChip(
                                onClick = { },
                                label = { 
                                    Text(
                                        text = "+${host.services.size - 5} more",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium
                                    ) 
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            )
                        }
                    }
                }
            }
            
            // Action buttons
            if (host.isReachable) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Vulnerability scan button
                    OutlinedButton(
                        onClick = onVulnerabilityScan,
                        enabled = !isVulnerabilityScanning,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isVulnerabilityScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Outlined.Security,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scan Vulnerabilities")
                    }
                    
                    // Vulnerability count badge
                    if (host.vulnerabilityCount > 0) {
                        AssistChip(
                            onClick = { },
                            label = { Text("${host.vulnerabilityCount} issues") },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        )
                    }
                }
            }
            
            // Last seen timestamp
            if (host.lastSeen > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Last seen: ${dateFormat.format(Date(host.lastSeen))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun ResponsiveTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    hostCount: Int,
    vulnerabilityCount: Int,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp.dp < 600.dp
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(28.dp) // Material 3 expressive rounded corners
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.clip(RoundedCornerShape(28.dp)),
            containerColor = Color.Transparent
        ) {
        Tab(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            text = { 
                if (isCompact) {
                    Text("Hosts")
                } else {
                    Text("Hosts ($hostCount)")
                }
            },
            icon = { Icon(Icons.Outlined.Devices, contentDescription = null) }
        )
        Tab(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            text = { 
                if (isCompact) {
                    Text("Vulnerabilities")
                } else {
                    Text("Vulnerabilities ($vulnerabilityCount)")
                }
            },
            icon = { Icon(Icons.Outlined.Security, contentDescription = null) }
        )
        Tab(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            text = { Text("History") },
            icon = { Icon(Icons.Outlined.History, contentDescription = null) }
        )
        }
    }
}