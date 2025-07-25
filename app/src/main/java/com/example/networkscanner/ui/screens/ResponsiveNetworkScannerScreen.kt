package com.example.networkscanner.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.networkscanner.data.HostInfo
import com.example.networkscanner.data.VulnerabilitySeverity
import com.example.networkscanner.ui.NetworkScannerViewModel
import com.example.networkscanner.ui.ScanUiState
import com.example.networkscanner.ui.SortOption
import com.example.networkscanner.ui.components.EmptyStateContent
import com.example.networkscanner.ui.components.EnhancedHostCard
import com.example.networkscanner.ui.components.LegalDisclaimerDialog
import com.example.networkscanner.ui.components.Material3TypographyChart
import com.example.networkscanner.ui.components.NetworkScannerHeader
import com.example.networkscanner.ui.components.ResponsiveStatisticsGrid
import com.example.networkscanner.ui.components.ResponsiveTabRow
import com.example.networkscanner.ui.components.ScanHistoryCard
import com.example.networkscanner.ui.components.ScanProgressCard
import com.example.networkscanner.ui.components.SortOptionsRow
import com.example.networkscanner.ui.components.VulnerabilityCard

@Composable
fun ResponsiveNetworkScannerScreen(
    viewModel: NetworkScannerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current

    // Determine screen size and layout
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val isTablet = screenWidth >= 840.dp
    val isLandscape = screenWidth > screenHeight

    // Scroll states
    val listState = rememberLazyListState()
    val gridState = rememberLazyStaggeredGridState()
    val isScrolled by remember {
        derivedStateOf { 
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 50 
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {

        if (isTablet) {
            TabletLayout(
                viewModel = viewModel,
                uiState = uiState,
                isScrolled = isScrolled, // isScrolled might not be relevant for Tablet's always expanded header
                listState = listState,
                gridState = gridState,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            PhoneLayout(
                viewModel = viewModel,
                uiState = uiState,
                isScrolled = isScrolled,
                listState = listState,
                // gridState is not used by PhoneLayout directly, but by TabletResultsPanel if we decide to use it there
                modifier = Modifier.fillMaxSize()
            )
        }

        // Typography Chart Overlay
        AnimatedVisibility(
            visible = uiState.showTypographyChart,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Material3TypographyChart(
                    onDismiss = viewModel::toggleTypographyChart,
                    modifier = Modifier.fillMaxWidth(0.95f)
                )
            }
        }
        
        // Legal Disclaimer Dialog
        if (uiState.showLegalDisclaimer) {
            LegalDisclaimerDialog(
                showDialog = true, // Controlled by the outer if
                onDismissRequest = { viewModel.handleLegalDisclaimerDeclined() },
                onConfirm = { doNotShowAgain -> viewModel.handleLegalDisclaimerAccepted(doNotShowAgain) }
            )
        }
    }
}

@Composable
private fun PhoneLayout(
    viewModel: NetworkScannerViewModel,
    uiState: ScanUiState,
    isScrolled: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(modifier = modifier) {
        // Main Content - now takes full screen
        Column(modifier = Modifier.fillMaxSize()) {
            // Add top padding to account for floating header
            val headerPadding = if (isScrolled) 72.dp else 220.dp
            Spacer(modifier = Modifier.height(headerPadding))
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), // Adjusted padding
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Progress Section
            if (uiState.isScanning || uiState.scanProgress.isComplete) {
                item {
                    ScanProgressCard(
                        progress = uiState.scanProgress,
                        isScanning = uiState.isScanning, // Added this line
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }


            // Statistics Row
            item {
                ResponsiveStatisticsGrid(
                    totalHosts = uiState.discoveredHosts.size,
                    vulnerabilities = uiState.vulnerabilities.size,
                    criticalVulns = uiState.vulnerabilities.count { it.severity == VulnerabilitySeverity.CRITICAL },
                    isScanning = uiState.isScanning,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Sort and Filter Options
            if (uiState.discoveredHosts.isNotEmpty()) {
                item {
                    SortOptionsRow(
                        currentSort = uiState.sortOption,
                        onSortChange = viewModel::setSortOption,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Tabs
            item {
                ResponsiveTabRow(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    hostCount = uiState.discoveredHosts.size,
                    vulnerabilityCount = uiState.vulnerabilities.size,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    items(uiState.discoveredHosts, key = { it.ip }) { host ->
                        EnhancedHostCard(
                            host = host,
                            isSelected = uiState.selectedHost?.ip == host.ip,
                            onHostSelected = viewModel::selectHost,
                            onVulnerabilityScan = { viewModel.scanHostVulnerabilities(host.ip) },
                            isVulnerabilityScanning = uiState.isVulnerabilityScanning,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                1 -> {
                    items(uiState.vulnerabilities) { vulnerability ->
                        VulnerabilityCard(
                            vulnerability = vulnerability,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                2 -> {
                    items(uiState.scanHistory) { scanResult ->
                        ScanHistoryCard(
                            scanResult = scanResult,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Empty state for tabs
            if ((selectedTab == 0 && uiState.discoveredHosts.isEmpty()) ||
                (selectedTab == 1 && uiState.vulnerabilities.isEmpty()) ||
                (selectedTab == 2 && uiState.scanHistory.isEmpty())) {
                item {
                    EmptyStateContent(
                        icon = when (selectedTab) {
                            0 -> Icons.Outlined.Devices
                            1 -> Icons.Outlined.Security
                            else -> Icons.Outlined.History
                        },
                        title = when (selectedTab) {
                            0 -> "No Hosts Found"
                            1 -> "No Vulnerabilities"
                            else -> "No Scan History"
                        },
                        description = when (selectedTab) {
                            0 -> "Start a network scan to discover devices"
                            1 -> "No security issues detected"
                            else -> "Your scan history will appear here"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                }
            }
        }
        }
        
        // Floating Collapsing Header - positioned above content
        CollapsingHeader(
            title = "Network Scanner Pro",
            isCollapsed = isScrolled,
            onTypographyClick = viewModel::toggleTypographyChart,
            modifier = Modifier.fillMaxWidth(),
            // Pass parameters for the embedded NetworkScannerHeader
            networkRange = uiState.networkRange,
            onNetworkRangeChange = viewModel::updateNetworkRange,
            onAutoDetect = viewModel::autoDetectNetwork,
            onStartScan = { viewModel.startNetworkScan() },
            onStopScan = viewModel::stopScan,
            isScanning = uiState.isScanning
        )
        
        // Fade overlay where header meets content - only shown when scrolled
        if (isScrolled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp) // Height when scrolled
                    .statusBarsPadding()
                    .offset(y = 56.dp) // Offset when scrolled
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.0f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.background.copy(alpha = 1.0f)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun TabletLayout(
    viewModel: NetworkScannerViewModel,
    uiState: ScanUiState,
    isScrolled: Boolean, // May not be used if header is always expanded
    listState: androidx.compose.foundation.lazy.LazyListState,
    gridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        // Left Panel - Controls and Stats
        Column(
            modifier = Modifier
                .width(400.dp)
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NetworkScannerHeader( // Direct usage for tablet
                networkRange = uiState.networkRange,
                onNetworkRangeChange = viewModel::updateNetworkRange,
                onAutoDetect = viewModel::autoDetectNetwork,
                onStartScan = { viewModel.startNetworkScan() },
                onStopScan = viewModel::stopScan,
                isScanning = uiState.isScanning
            )

            if (uiState.isScanning || uiState.scanProgress.isComplete) {
                ScanProgressCard(
                    progress = uiState.scanProgress,
                    isScanning = uiState.isScanning, // Added this line
                    modifier = Modifier.fillMaxWidth()
                )
            }
            ResponsiveStatisticsGrid(
                totalHosts = uiState.discoveredHosts.size,
                vulnerabilities = uiState.vulnerabilities.size,
                criticalVulns = uiState.vulnerabilities.count { it.severity == VulnerabilitySeverity.CRITICAL },
                isScanning = uiState.isScanning,
                modifier = Modifier.fillMaxWidth()
            )
            // Typography button for tablet
            OutlinedButton(onClick = viewModel::toggleTypographyChart, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.TextFields, contentDescription = "Typography Chart", modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Typography Chart")
            }
        }

        // Right Panel - Results
        TabletResultsPanel(
            viewModel = viewModel,
            uiState = uiState,
            listState = listState,
            gridState = gridState,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .statusBarsPadding() // Ensure this panel also respects status bar if it can go under
        )
    }
}

@Composable
private fun TabletResultsPanel(
    viewModel: NetworkScannerViewModel,
    uiState: ScanUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    gridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.padding(16.dp)) {
        if (uiState.discoveredHosts.isNotEmpty()) {
            SortOptionsRow(
                currentSort = uiState.sortOption,
                onSortChange = viewModel::setSortOption,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        ResponsiveTabRow(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            hostCount = uiState.discoveredHosts.size,
            vulnerabilityCount = uiState.vulnerabilities.size,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Content for TabletResultsPanel (Staggered Grid)
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(300.dp),
            state = gridState,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp,
            modifier = Modifier.weight(1f) // Fill available space
        ) {
            when (selectedTab) {
                0 -> {
                    items(uiState.discoveredHosts, key = { it.ip }) { host ->
                        EnhancedHostCard(
                            host = host,
                            isSelected = uiState.selectedHost?.ip == host.ip,
                            onHostSelected = viewModel::selectHost,
                            onVulnerabilityScan = { viewModel.scanHostVulnerabilities(host.ip) },
                            isVulnerabilityScanning = uiState.isVulnerabilityScanning,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                1 -> {
                    items(uiState.vulnerabilities) { vulnerability ->
                        VulnerabilityCard(
                            vulnerability = vulnerability
                        )
                    }
                }
                2 -> {
                    items(uiState.scanHistory) { scanResult ->
                        ScanHistoryCard(
                            scanResult = scanResult
                        )
                    }
                }
            }
            // Empty state for tabs
            if ((selectedTab == 0 && uiState.discoveredHosts.isEmpty()) ||
                (selectedTab == 1 && uiState.vulnerabilities.isEmpty()) ||
                (selectedTab == 2 && uiState.scanHistory.isEmpty())) {
                item {
                    EmptyStateContent(
                        icon = when (selectedTab) {
                            0 -> Icons.Outlined.Devices
                            1 -> Icons.Outlined.Security
                            else -> Icons.Outlined.History
                        },
                        title = when (selectedTab) {
                            0 -> "No Hosts Found"
                            1 -> "No Vulnerabilities"
                            else -> "No Scan History"
                        },
                        description = when (selectedTab) {
                            0 -> "Start a network scan to discover devices"
                            1 -> "No security issues detected"
                            else -> "Your scan history will appear here"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun CollapsingHeader(
    title: String,
    isCollapsed: Boolean,
    onTypographyClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Parameters for the embedded NetworkScannerHeader
    networkRange: String,
    onNetworkRangeChange: (String) -> Unit,
    onAutoDetect: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    isScanning: Boolean
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isCollapsed) 0.95f else 1f, // Slightly more opaque when collapsed
        animationSpec = tween(300),
        label = "header_alpha"
    )

    // Dynamic height: responsive to content and screen space  
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val maxExpandedHeight = minOf(200.dp, screenHeight * 0.25f) // Reduced height
    val collapsedHeight = 48.dp
    val animatedHeight by animateDpAsState(
        targetValue = if (isCollapsed) collapsedHeight else maxExpandedHeight,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "header_height"
    )

    val animatedPadding by animateDpAsState(
        targetValue = if (isCollapsed) 8.dp else 16.dp,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "header_padding"
    )
    
    val animatedCornerRadius by animateDpAsState(
        targetValue = if (isCollapsed) 24.dp else 16.dp,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "corner_radius"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = animatedPadding)
                .height(animatedHeight),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = animatedAlpha),
            shadowElevation = if (isCollapsed) 12.dp else 6.dp,
            shape = RoundedCornerShape(animatedCornerRadius)
        ) {
        AnimatedContent(
            targetState = isCollapsed,
            transitionSpec = {
                // Fade in/out, consider slide for content change
                fadeIn(animationSpec = tween(400)) togetherWith
                    fadeOut(animationSpec = tween(400))
            },
            label = "header_content",
            modifier = Modifier.fillMaxSize() // AnimatedContent should fill the Surface
        ) { collapsed ->
            if (collapsed) {
                // Collapsed state - compact title centered, minimized for space
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Network Scanner Pro",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Expanded state - adaptive and space-efficient
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Fixed title with consistent typography
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Compact scanner controls
                    NetworkScannerHeader(
                        networkRange = networkRange,
                        onNetworkRangeChange = onNetworkRangeChange,
                        onAutoDetect = onAutoDetect,
                        onStartScan = onStartScan,
                        onStopScan = onStopScan,
                        isScanning = isScanning,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        }
    }
}
