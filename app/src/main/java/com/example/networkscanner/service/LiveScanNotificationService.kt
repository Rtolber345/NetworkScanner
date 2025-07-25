
package com.example.networkscanner.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.networkscanner.MainActivity
import com.example.networkscanner.R
import com.example.networkscanner.data.NetworkScanner
import com.example.networkscanner.data.ScanProgress
import com.example.networkscanner.data.HostInfo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LiveScanNotificationService : Service() {

    @Inject
    lateinit var networkScanner: NetworkScanner

    private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var scanJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START_SCAN -> {
                val networkRange = intent.getStringExtra(EXTRA_NETWORK_RANGE) ?: return START_NOT_STICKY
                startForeground(NOTIFICATION_ID, createNotification(ScanProgress(isComplete = false, currentOperation = "Initializing scan...")))
                startScan(networkRange)
            }
            ACTION_STOP_SCAN -> {
                stopScan()
            }
        }
        return START_NOT_STICKY
    }

    private fun startScan(networkRange: String) {
        scanJob = serviceScope.launch {
            try {
                // Initial notification update
                updateNotificationProgress(ScanProgress(currentOperation = "Starting scan of $networkRange"))

                networkScanner.scanNetwork(
                    networkRange = networkRange,
                    onProgress = { progress ->
                        // Update notification with progress
                        updateNotificationProgress(progress)
                        // Broadcast progress to ViewModel
                        broadcastScanProgress(progress)
                    },
                    onHostDiscovered = { host ->
                        // Broadcast discovered host to ViewModel
                        broadcastHostDiscovered(host)
                    }
                )

                showScanCompleteNotification()
                broadcastScanComplete()

            } catch (_: CancellationException) {
                showScanStoppedNotification()
            } catch (e: Exception) {
                showScanErrorNotification(e.message ?: "An unknown error occurred")
            } finally {
                stopSelf()
            }
        }
    }

    private fun stopScan() {
        scanJob?.cancel()
        networkScanner.cancelScan()
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Live Scan",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live progress of a network scan"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotificationProgress(progress: ScanProgress) {
        notificationManager.notify(NOTIFICATION_ID, createNotification(progress))
    }

    private fun broadcastScanProgress(progress: ScanProgress) {
        val intent = Intent(ACTION_SCAN_PROGRESS).apply {
            putExtra(EXTRA_SCAN_PROGRESS_OPERATION, progress.currentOperation)
            putExtra(EXTRA_SCAN_PROGRESS_COMPLETED, progress.completedHosts)
            putExtra(EXTRA_SCAN_PROGRESS_TOTAL, progress.totalHosts)
            putExtra(EXTRA_SCAN_PROGRESS_IS_COMPLETE, progress.isComplete)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun broadcastHostDiscovered(host: HostInfo) {
        val intent = Intent(ACTION_HOST_DISCOVERED).apply {
            putExtra(EXTRA_HOST_IP, host.ip)
            putExtra(EXTRA_HOST_HOSTNAME, host.hostname)
            putExtra(EXTRA_HOST_MAC, host.macAddress)
            putExtra(EXTRA_HOST_VENDOR, host.vendor)
            putExtra(EXTRA_HOST_DEVICE_TYPE, host.deviceType.name)
            putExtra(EXTRA_HOST_RESPONSE_TIME, host.responseTime)
            putExtra(EXTRA_HOST_SIGNAL_STRENGTH, host.signalStrength)
            putExtra(EXTRA_HOST_LAST_SEEN, host.lastSeen)
            putExtra(EXTRA_HOST_IS_ONLINE, host.isReachable)
            putStringArrayListExtra(EXTRA_HOST_OPEN_PORTS, ArrayList(host.openPorts.map { it.toString() }))
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun broadcastScanComplete() {
        val intent = Intent(ACTION_SCAN_COMPLETE)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun showScanCompleteNotification() {
        val notification = createNotification(ScanProgress(isComplete = true, currentOperation = "Scan complete!"), showStopAction = false)
        notificationManager.notify(NOTIFICATION_ID, notification)
        stopForeground(STOP_FOREGROUND_DETACH)
    }

    private fun showScanStoppedNotification() {
        val notification = createNotification(ScanProgress(isComplete = true, currentOperation = "Scan stopped by user."), showStopAction = false)
        notificationManager.notify(NOTIFICATION_ID, notification)
        stopForeground(STOP_FOREGROUND_DETACH)
    }
    
    private fun showScanErrorNotification(errorMessage: String) {
        val notification = createNotification(ScanProgress(isComplete = true, currentOperation = "Error: $errorMessage"), showStopAction = false)
        notificationManager.notify(NOTIFICATION_ID, notification)
        stopForeground(STOP_FOREGROUND_DETACH)
    }

    private fun createNotification(progress: ScanProgress, showStopAction: Boolean = true): android.app.Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val stopIntent = Intent(this, LiveScanNotificationService::class.java).apply {
            action = ACTION_STOP_SCAN
        }.let {
            PendingIntent.getService(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Network Scan")
            .setContentText(progress.currentOperation)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(openAppIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(!progress.isComplete)

        if (!progress.isComplete) {
            val percentage = if (progress.totalHosts > 0) {
                (progress.completedHosts * 100 / progress.totalHosts)
            } else {
                0
            }
            builder.setProgress(100, percentage, false)
        }

        if (showStopAction && !progress.isComplete) {
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Scan", stopIntent)
        }

        return builder.build()
    }


    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scanJob?.cancel()
    }

    companion object {
        const val CHANNEL_ID = "LiveScanNotificationChannel"
        const val NOTIFICATION_ID = 1

        private const val ACTION_START_SCAN = "com.example.networkscanner.service.ACTION_START_SCAN"
        private const val ACTION_STOP_SCAN = "com.example.networkscanner.service.ACTION_STOP_SCAN"
        private const val EXTRA_NETWORK_RANGE = "com.example.networkscanner.service.EXTRA_NETWORK_RANGE"

        // Broadcast actions
        const val ACTION_SCAN_PROGRESS = "com.example.networkscanner.service.ACTION_SCAN_PROGRESS"
        const val ACTION_HOST_DISCOVERED = "com.example.networkscanner.service.ACTION_HOST_DISCOVERED"
        const val ACTION_SCAN_COMPLETE = "com.example.networkscanner.service.ACTION_SCAN_COMPLETE"

        // Broadcast extras for scan progress
        const val EXTRA_SCAN_PROGRESS_OPERATION = "EXTRA_SCAN_PROGRESS_OPERATION"
        const val EXTRA_SCAN_PROGRESS_COMPLETED = "EXTRA_SCAN_PROGRESS_COMPLETED"
        const val EXTRA_SCAN_PROGRESS_TOTAL = "EXTRA_SCAN_PROGRESS_TOTAL"
        const val EXTRA_SCAN_PROGRESS_IS_COMPLETE = "EXTRA_SCAN_PROGRESS_IS_COMPLETE"

        // Broadcast extras for host discovery
        const val EXTRA_HOST_IP = "EXTRA_HOST_IP"
        const val EXTRA_HOST_HOSTNAME = "EXTRA_HOST_HOSTNAME"
        const val EXTRA_HOST_MAC = "EXTRA_HOST_MAC"
        const val EXTRA_HOST_VENDOR = "EXTRA_HOST_VENDOR"
        const val EXTRA_HOST_DEVICE_TYPE = "EXTRA_HOST_DEVICE_TYPE"
        const val EXTRA_HOST_RESPONSE_TIME = "EXTRA_HOST_RESPONSE_TIME"
        const val EXTRA_HOST_SIGNAL_STRENGTH = "EXTRA_HOST_SIGNAL_STRENGTH"
        const val EXTRA_HOST_LAST_SEEN = "EXTRA_HOST_LAST_SEEN"
        const val EXTRA_HOST_IS_ONLINE = "EXTRA_HOST_IS_ONLINE"
        const val EXTRA_HOST_OPEN_PORTS = "EXTRA_HOST_OPEN_PORTS"

        fun startScanService(context: Context, networkRange: String) {
            val intent = Intent(context, LiveScanNotificationService::class.java).apply {
                action = ACTION_START_SCAN
                putExtra(EXTRA_NETWORK_RANGE, networkRange)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopScanService(context: Context) {
            val intent = Intent(context, LiveScanNotificationService::class.java).apply {
                action = ACTION_STOP_SCAN
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
