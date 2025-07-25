
package com.example.networkscanner.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.networkscanner.data.DeviceType
import com.example.networkscanner.data.HostInfo
import com.example.networkscanner.data.NetworkScanner
import com.example.networkscanner.data.ScanProgress
import com.example.networkscanner.data.VulnerabilityInfo
import com.example.networkscanner.data.VulnerabilitySeverity
import com.example.networkscanner.service.LiveScanNotificationService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScanUiState(
    val isScanning: Boolean = false,
    val scanProgress: ScanProgress = ScanProgress(),
    val discoveredHosts: List<HostInfo> = emptyList(),
    val vulnerabilities: List<VulnerabilityInfo> = emptyList(),
    val networkRange: String = "",
    val selectedHost: HostInfo? = null,
    val scanHistory: List<ScanResult> = emptyList(),
    val error: String? = null,
    val sortOption: SortOption = SortOption.IP_ADDRESS,
    val showTypographyChart: Boolean = false,
    val showLegalDisclaimer: Boolean = true, // Added
    val isVulnerabilityScanning: Boolean = false,
    val isInitializing: Boolean = true, // Added for initial loading
    val successMessage: String? = null, // Added for success banners
    val showSuccessBanner: Boolean = false, // Added for banner visibility control
    val showErrorBanner: Boolean = false // Added for error banner visibility control
)

enum class SortOption(val displayName: String) {
    IP_ADDRESS("IP Address"),
    HOSTNAME("Hostname"),
    SIGNAL_STRENGTH("Signal Strength"),
    RESPONSE_TIME("Response Time"),
    OPEN_PORTS("Open Ports"),
    LAST_SEEN("Last Seen"),
    DEVICE_TYPE("Device Type")
}

data class ScanResult(
    val timestamp: Long,
    val networkRange: String,
    val hostsFound: Int,
    val vulnerabilitiesFound: Int,
    val scanDuration: Long
)

class NetworkScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val PREFS_NAME = "NetworkScannerPrefs"
    private val PREF_KEY_DO_NOT_SHOW_LEGAL = "doNotShowLegalDisclaimer"
    private val sharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val networkScanner = NetworkScanner(application.applicationContext)
    private var vulnerabilityScanJob: Job? = null

    // BroadcastReceiver to receive updates from the service
    private val serviceBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                LiveScanNotificationService.ACTION_SCAN_PROGRESS -> {
                    handleScanProgressUpdate(intent)
                }
                LiveScanNotificationService.ACTION_HOST_DISCOVERED -> {
                    handleHostDiscovered(intent)
                }
                LiveScanNotificationService.ACTION_SCAN_COMPLETE -> {
                    handleScanComplete()
                }
            }
        }
    }


    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    init {
        val shouldShowDisclaimer = !sharedPreferences.getBoolean(PREF_KEY_DO_NOT_SHOW_LEGAL, false)
        _uiState.value = _uiState.value.copy(
            networkRange = networkScanner.getLocalNetworkRange(),
            showLegalDisclaimer = shouldShowDisclaimer,
            isInitializing = false
        )
        registerServiceBroadcastReceiver()
    }
    
    private fun registerServiceBroadcastReceiver() {
        val intentFilter = IntentFilter().apply {
            addAction(LiveScanNotificationService.ACTION_SCAN_PROGRESS)
            addAction(LiveScanNotificationService.ACTION_HOST_DISCOVERED)
            addAction(LiveScanNotificationService.ACTION_SCAN_COMPLETE)
        }
        LocalBroadcastManager.getInstance(getApplication())
            .registerReceiver(serviceBroadcastReceiver, intentFilter)
    }

    fun handleLegalDisclaimerAccepted(doNotShowAgain: Boolean) {
        if (doNotShowAgain) {
            sharedPreferences.edit().putBoolean(PREF_KEY_DO_NOT_SHOW_LEGAL, true).apply()
        }
        _uiState.value = _uiState.value.copy(showLegalDisclaimer = false)
    }

    fun handleLegalDisclaimerDeclined() {
        _uiState.value = _uiState.value.copy(showLegalDisclaimer = false)
    }

    private fun handleScanProgressUpdate(intent: Intent) {
        val operation = intent.getStringExtra(LiveScanNotificationService.EXTRA_SCAN_PROGRESS_OPERATION) ?: ""
        val completed = intent.getIntExtra(LiveScanNotificationService.EXTRA_SCAN_PROGRESS_COMPLETED, 0)
        val total = intent.getIntExtra(LiveScanNotificationService.EXTRA_SCAN_PROGRESS_TOTAL, 0)
        val isComplete = intent.getBooleanExtra(LiveScanNotificationService.EXTRA_SCAN_PROGRESS_IS_COMPLETE, false)

        val progress = ScanProgress(
            currentOperation = operation,
            completedHosts = completed,
            totalHosts = total,
            isComplete = isComplete
        )

        _uiState.value = _uiState.value.copy(scanProgress = progress)
    }

    private fun handleHostDiscovered(intent: Intent) {
        val ip = intent.getStringExtra(LiveScanNotificationService.EXTRA_HOST_IP) ?: return
        val hostname = intent.getStringExtra(LiveScanNotificationService.EXTRA_HOST_HOSTNAME) ?: ""
        val mac = intent.getStringExtra(LiveScanNotificationService.EXTRA_HOST_MAC) ?: ""
        val vendor = intent.getStringExtra(LiveScanNotificationService.EXTRA_HOST_VENDOR) ?: ""
        val deviceTypeName = intent.getStringExtra(LiveScanNotificationService.EXTRA_HOST_DEVICE_TYPE) ?: "UNKNOWN"
        val responseTime = intent.getLongExtra(LiveScanNotificationService.EXTRA_HOST_RESPONSE_TIME, 0L)
        val signalStrength = intent.getIntExtra(LiveScanNotificationService.EXTRA_HOST_SIGNAL_STRENGTH, 0)
        val lastSeen = intent.getLongExtra(LiveScanNotificationService.EXTRA_HOST_LAST_SEEN, System.currentTimeMillis())
        val isOnline = intent.getBooleanExtra(LiveScanNotificationService.EXTRA_HOST_IS_ONLINE, true)
        val openPortsStrings = intent.getStringArrayListExtra(LiveScanNotificationService.EXTRA_HOST_OPEN_PORTS) ?: arrayListOf()

        val deviceType = try {
            DeviceType.valueOf(deviceTypeName)
        } catch (e: IllegalArgumentException) {
            DeviceType.UNKNOWN
        }

        val openPorts = openPortsStrings.mapNotNull { it.toIntOrNull() }

        val host = HostInfo(
            ip = ip,
            hostname = hostname,
            macAddress = mac,
            vendor = vendor,
            deviceType = deviceType,
            responseTime = responseTime,
            signalStrength = signalStrength,
            lastSeen = lastSeen,
            isReachable = isOnline,
            openPorts = openPorts
        )

        // Add the discovered host to the list
        val currentHosts = _uiState.value.discoveredHosts.toMutableList()
        val existingIndex = currentHosts.indexOfFirst { it.ip == host.ip }
        if (existingIndex >= 0) {
            currentHosts[existingIndex] = host
        } else {
            currentHosts.add(host)
        }

        _uiState.value = _uiState.value.copy(discoveredHosts = currentHosts)
        applySorting()
    }

    private fun handleScanComplete() {
        val hostCount = _uiState.value.discoveredHosts.size
        _uiState.value = _uiState.value.copy(
            isScanning = false,
            scanProgress = ScanProgress(currentOperation = "Scan completed", isComplete = true)
        )
        showSuccessMessage("Scan completed! Found $hostCount ${if (hostCount == 1) "device" else "devices"}")
    }

    fun startNetworkScan(networkRange: String? = null) {
        if (_uiState.value.isScanning) return

        val range = networkRange ?: _uiState.value.networkRange

        _uiState.value = _uiState.value.copy(
            isScanning = true,
            error = null,
            discoveredHosts = emptyList(),
            vulnerabilities = emptyList()
        )

        LiveScanNotificationService.startScanService(getApplication(), range)
    }

    fun stopScan() {
        if (!_uiState.value.isScanning) return

        LiveScanNotificationService.stopScanService(getApplication())
        _uiState.value = _uiState.value.copy(
            isScanning = false,
            scanProgress = ScanProgress(currentOperation = "Scan stopped", isComplete = true)
        )
    }

    fun selectHost(host: HostInfo) {
        _uiState.value = _uiState.value.copy(selectedHost = host)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedHost = null)
    }

    fun updateNetworkRange(range: String) {
        _uiState.value = _uiState.value.copy(networkRange = range)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, showErrorBanner = false)
    }

    fun showSuccessMessage(message: String, autoDismiss: Boolean = true) {
        _uiState.value = _uiState.value.copy(
            successMessage = message,
            showSuccessBanner = true
        )
        
        if (autoDismiss) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(3000)
                dismissSuccessBanner()
            }
        }
    }

    fun showErrorMessage(message: String, autoDismiss: Boolean = true) {
        _uiState.value = _uiState.value.copy(
            error = message,
            showErrorBanner = true
        )
        
        if (autoDismiss) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(5000)
                dismissErrorBanner()
            }
        }
    }

    fun dismissSuccessBanner() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            showSuccessBanner = false
        )
    }

    fun dismissErrorBanner() {
        _uiState.value = _uiState.value.copy(
            error = null,
            showErrorBanner = false
        )
    }

    fun clearResults() {
        networkScanner.clearResults()
        _uiState.value = _uiState.value.copy(
            discoveredHosts = emptyList(),
            vulnerabilities = emptyList(),
            selectedHost = null
        )
    }

    fun autoDetectNetwork() {
        val detectedRange = networkScanner.getLocalNetworkRange()
        _uiState.value = _uiState.value.copy(networkRange = detectedRange)
    }

    fun getVulnerabilitiesByHost(hostIp: String): List<VulnerabilityInfo> {
        return _uiState.value.vulnerabilities.filter { it.host == hostIp }
    }

    fun getVulnerabilitiesBySeverity(severity: VulnerabilitySeverity): List<VulnerabilityInfo> {
        return _uiState.value.vulnerabilities.filter { it.severity == severity }
    }

    fun setSortOption(sortOption: SortOption) {
        _uiState.value = _uiState.value.copy(sortOption = sortOption)
        applySorting()
    }

    fun toggleTypographyChart() {
        _uiState.value = _uiState.value.copy(
            showTypographyChart = !_uiState.value.showTypographyChart
        )
    }

    fun scanHostVulnerabilities(hostIp: String) {
        if (_uiState.value.isVulnerabilityScanning) return

        _uiState.value = _uiState.value.copy(isVulnerabilityScanning = true)

        vulnerabilityScanJob?.cancel()
        vulnerabilityScanJob = viewModelScope.launch {
            try {
                val host = _uiState.value.discoveredHosts.find { it.ip == hostIp }
                host?.let {
                    val vulnerabilities = networkScanner.analyzeVulnerabilities(listOf(it))

                    val updatedVulns = _uiState.value.vulnerabilities.toMutableList()
                    updatedVulns.removeAll { vuln -> vuln.host == hostIp }
                    updatedVulns.addAll(vulnerabilities)

                    val updatedHosts = _uiState.value.discoveredHosts.map { hostInfo ->
                        if (hostInfo.ip == hostIp) {
                            hostInfo.copy(vulnerabilityCount = vulnerabilities.size)
                        } else hostInfo
                    }

                    _uiState.value = _uiState.value.copy(
                        vulnerabilities = updatedVulns.sortedByDescending { it.severity },
                        discoveredHosts = updatedHosts
                    )
                }
            } catch (e: Exception) {
                showErrorMessage("Failed to scan vulnerabilities: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isVulnerabilityScanning = false)
            }
        }
    }

    private fun applySorting() {
        val currentHosts = _uiState.value.discoveredHosts
        val sortedHosts = when (_uiState.value.sortOption) {
            SortOption.IP_ADDRESS -> currentHosts.sortedBy { host ->
                host.ip.split(".").map { part -> part.toIntOrNull() ?: 0 }.joinToString("") { "%03d".format(it) }
            }
            SortOption.HOSTNAME -> currentHosts.sortedBy { it.hostname.ifEmpty { it.ip } }
            SortOption.SIGNAL_STRENGTH -> currentHosts.sortedByDescending { it.signalStrength }
            SortOption.RESPONSE_TIME -> currentHosts.sortedBy { it.responseTime }
            SortOption.OPEN_PORTS -> currentHosts.sortedByDescending { it.openPorts.size }
            SortOption.LAST_SEEN -> currentHosts.sortedByDescending { it.lastSeen }
            SortOption.DEVICE_TYPE -> currentHosts.sortedBy { it.deviceType.name }
        }

        _uiState.value = _uiState.value.copy(discoveredHosts = sortedHosts)
    }

    override fun onCleared() {
        super.onCleared()
        vulnerabilityScanJob?.cancel()
        networkScanner.destroy()
        LocalBroadcastManager.getInstance(getApplication())
            .unregisterReceiver(serviceBroadcastReceiver)
    }
}
