package com.example.networkscanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.DeviceHub
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.SignalWifi4Bar
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.networkscanner.data.HostInfo
import com.example.networkscanner.data.VulnerabilitySeverity
import com.example.networkscanner.ui.NetworkScannerViewModel
import com.example.networkscanner.ui.SortOption
import com.example.networkscanner.ui.components.BannerContainer
import com.example.networkscanner.ui.components.EmptyStateContent
import com.example.networkscanner.ui.components.HostCard
import com.example.networkscanner.ui.components.NetworkScannerHeader
import com.example.networkscanner.ui.components.ScanHistoryCard
import com.example.networkscanner.ui.components.ScanProgressCard
import com.example.networkscanner.ui.components.VulnerabilityCard

@Composable
fun NetworkScannerScreen(
    viewModel: NetworkScannerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header Section
        NetworkScannerHeader(
            networkRange = uiState.networkRange,
            onNetworkRangeChange = viewModel::updateNetworkRange,
            onAutoDetect = viewModel::autoDetectNetwork,
            onStartScan = { viewModel.startNetworkScan() },
            onStopScan = viewModel::stopScan,
            isScanning = uiState.isScanning
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress Section
        if (uiState.isScanning || uiState.scanProgress.isComplete) {
            ScanProgressCard(
                progress = uiState.scanProgress,
                isScanning = uiState.isScanning, // Added isScanning parameter
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Statistics Cards
        ScanStatisticsRow(
            totalHosts = uiState.discoveredHosts.size,
            vulnerabilities = uiState.vulnerabilities.size,
            criticalVulns = uiState.vulnerabilities.count { it.severity == VulnerabilitySeverity.CRITICAL },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Sort Options (only show if we have hosts)
        if (uiState.discoveredHosts.isNotEmpty()) {
            SortOptionsRow(
                currentSort = uiState.sortOption,
                onSortChange = viewModel::setSortOption,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Tab Navigation
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Hosts (${uiState.discoveredHosts.size})") },
                icon = { Icon(Icons.Outlined.Devices, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Vulnerabilities (${uiState.vulnerabilities.size})") },
                icon = { Icon(Icons.Outlined.Security, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("History") },
                icon = { Icon(Icons.Outlined.History, contentDescription = null) }
            )
        }
        
        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> HostsTabContent(
                    hosts = uiState.discoveredHosts,
                    selectedHost = uiState.selectedHost,
                    onHostSelected = viewModel::selectHost,
                    hasScanned = uiState.isScanning || uiState.scanProgress.isComplete || uiState.discoveredHosts.isNotEmpty(),
                    modifier = Modifier.fillMaxSize()
                )
                1 -> VulnerabilitiesTabContent(
                    vulnerabilities = uiState.vulnerabilities,
                    modifier = Modifier.fillMaxSize()
                )
                2 -> HistoryTabContent(
                    scanHistory = uiState.scanHistory,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
    
    // Banner notifications
    BannerContainer(
        uiState = uiState,
        onDismissSuccess = { viewModel.dismissSuccessBanner() },
        onDismissError = { viewModel.dismissErrorBanner() }
    )
}

@Composable
fun ScanStatisticsRow(
    totalHosts: Int,
    vulnerabilities: Int,
    criticalVulns: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatisticCard(
            title = "Hosts Found",
            value = totalHosts.toString(),
            icon = Icons.Filled.Devices,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        
        StatisticCard(
            title = "Vulnerabilities",
            value = vulnerabilities.toString(),
            icon = Icons.Filled.Security,
            color = if (vulnerabilities == 0) MaterialTheme.colorScheme.primary 
                   else MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
        
        StatisticCard(
            title = "Critical",
            value = criticalVulns.toString(),
            icon = Icons.Filled.Warning,
            color = if (criticalVulns == 0) MaterialTheme.colorScheme.primary 
                   else MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatisticCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
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
fun HostsTabContent(
    hosts: List<HostInfo>,
    selectedHost: HostInfo?,
    onHostSelected: (HostInfo) -> Unit,
    hasScanned: Boolean,
    modifier: Modifier = Modifier
) {
    if (!hasScanned) {
        EmptyStateContent(
            icon = Icons.Outlined.Devices,
            title = "Ready to Scan",
            description = "Start a network scan to discover devices on your network",
            modifier = modifier
        )
    } else if (hosts.isEmpty()) {
        EmptyStateContent(
            icon = Icons.Outlined.Devices,
            title = "No Hosts Found",
            description = "The scan completed but no devices were discovered",
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(hosts, key = { it.ip }) { host ->
                HostCard(
                    host = host,
                    isSelected = selectedHost?.ip == host.ip,
                    onClick = { onHostSelected(host) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun VulnerabilitiesTabContent(
    vulnerabilities: List<com.example.networkscanner.data.VulnerabilityInfo>,
    modifier: Modifier = Modifier
) {
    if (vulnerabilities.isEmpty()) {
        EmptyStateContent(
            icon = Icons.Outlined.Security,
            title = "No Vulnerabilities Found",
            description = "Great! No obvious vulnerabilities were detected in the scan",
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(vulnerabilities) { vulnerability ->
                VulnerabilityCard(
                    vulnerability = vulnerability,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun HistoryTabContent(
    scanHistory: List<com.example.networkscanner.ui.ScanResult>,
    modifier: Modifier = Modifier
) {
    if (scanHistory.isEmpty()) {
        EmptyStateContent(
            icon = Icons.Outlined.History,
            title = "No Scan History",
            description = "Your completed scans will appear here",
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(scanHistory) { scanResult ->
                ScanHistoryCard(
                    scanResult = scanResult,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


@Composable
fun SortOptionsRow(
    currentSort: SortOption,
    onSortChange: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Sort,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sort by: ${currentSort.displayName}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            Box {
                FilledTonalButton(
                    onClick = { expanded = true }
                ) {
                    Text("Change")
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    SortOption.values().forEach { option ->
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (option) {
                                            SortOption.IP_ADDRESS -> Icons.Outlined.Router
                                            SortOption.HOSTNAME -> Icons.Outlined.Computer
                                            SortOption.SIGNAL_STRENGTH -> Icons.Outlined.SignalWifi4Bar
                                            SortOption.RESPONSE_TIME -> Icons.Outlined.Speed
                                            SortOption.OPEN_PORTS -> Icons.Outlined.Security
                                            SortOption.LAST_SEEN -> Icons.Outlined.Schedule
                                            SortOption.DEVICE_TYPE -> Icons.Outlined.DeviceHub
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(option.displayName)
                                }
                            },
                            onClick = {
                                onSortChange(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
