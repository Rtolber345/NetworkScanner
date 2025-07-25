package com.example.networkscanner.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
// import android.text.format.Formatter // No longer needed for formatIpAddress
import kotlinx.coroutines.*
import java.io.IOException
import java.net.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

data class HostInfo(
    val ip: String,
    val hostname: String = "",
    val osInfo: String = "", // Added OS information
    val isReachable: Boolean = false,
    val openPorts: List<Int> = emptyList(),
    val services: Map<Int, ServiceInfo> = emptyMap(),
    val responseTime: Long = 0,
    val macAddress: String = "",
    val vendor: String = "",
    val signalStrength: Int = -1, // WiFi signal strength in dBm (-1 if unknown)
    val deviceType: DeviceType = DeviceType.UNKNOWN,
    val lastSeen: Long = System.currentTimeMillis(),
    val vulnerabilityCount: Int = 0
)

enum class DeviceType {
    ROUTER, COMPUTER, MOBILE, PRINTER, IOT, SERVER, UNKNOWN
}

data class ServiceInfo(
    val port: Int,
    val protocol: String = "tcp",
    val service: String = "",
    val version: String = "",
    val banner: String = ""
)

data class ScanProgress(
    val currentHost: String = "",
    val completedHosts: Int = 0,
    val totalHosts: Int = 0,
    val currentOperation: String = "",
    val isComplete: Boolean = false
)

data class VulnerabilityInfo(
    val host: String,
    val port: Int,
    val service: String,
    val severity: VulnerabilitySeverity,
    val description: String,
    val recommendation: String
)

enum class VulnerabilitySeverity {
    CRITICAL, HIGH, MEDIUM, LOW, INFO
}

class NetworkScanner(private val context: Context) : CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = Dispatchers.IO + job

    private val discoveredHosts = ConcurrentHashMap<String, HostInfo>()
    private var scanJob: Job? = null
    
    // Common ports to scan
    private val commonPorts = listOf(
        21, 22, 23, 25, 53, 80, 110, 135, 139, 143, 443, 993, 995, 
        1433, 3306, 3389, 5432, 5900, 8080, 8443, 9200, 27017
    )

    private val serviceDatabase = mapOf(
        21 to "FTP",
        22 to "SSH",
        23 to "Telnet",
        25 to "SMTP",
        53 to "DNS",
        80 to "HTTP",
        110 to "POP3",
        135 to "RPC",
        139 to "NetBIOS",
        143 to "IMAP",
        443 to "HTTPS",
        993 to "IMAPS",
        995 to "POP3S",
        1433 to "MSSQL",
        3306 to "MySQL",
        3389 to "RDP",
        5432 to "PostgreSQL",
        5900 to "VNC",
        8080 to "HTTP-Alt",
        8443 to "HTTPS-Alt",
        9200 to "Elasticsearch",
        27017 to "MongoDB"
    )

    fun getLocalNetworkRange(): String {
        try {
            // Primary method: Use ConnectivityManager
            val connectivityManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            connectivityManager?.activeNetwork?.let { network ->
                connectivityManager.getLinkProperties(network)?.let { props ->
                    for (linkAddress in props.linkAddresses) {
                        val address = linkAddress.address
                        if (address is Inet4Address && !address.isLoopbackAddress) {
                            val prefixLength = linkAddress.prefixLength
                            // Use existing getNetworkAddress to find the base network address
                            val networkBaseAddress = getNetworkAddress(address, prefixLength.toShort())
                            
                            // Check if it's a common private range
                            if (networkBaseAddress.startsWith("192.168.") ||
                                networkBaseAddress.startsWith("10.") ||
                                (networkBaseAddress.startsWith("172.") &&
                                 (address.address[1].toInt() and 0xFF) >= 16 && // Check 172.16.0.0 - 172.31.255.255
                                 (address.address[1].toInt() and 0xFF) <= 31)
                            ) {
                                return "$networkBaseAddress/$prefixLength"
                            }
                        }
                    }
                }
            }
            
            // Fallback 1: Try WiFiManager (though dhcpInfo is deprecated, it's a fallback)
            // This part is kept as a fallback but warnings for dhcpInfo will persist if this code path is hit.
            // Consider removing if ConnectivityManager is reliable enough across target devices.
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiManager?.dhcpInfo?.let { dhcp ->
                 // dhcp.ipAddress and dhcp.netmask are Ints.
                 // We need a non-deprecated way to convert these Ints to IP String and Netmask String if needed.
                 // android.text.format.Formatter.formatIpAddress(Int) is deprecated.
                 // A manual conversion or using InetAddress.getByAddress can be done.
                if (dhcp.ipAddress != 0 && dhcp.netmask != 0) {
                    val ipBytes = ByteArray(4) { i -> (dhcp.ipAddress shr (i * 8) and 0xFF).toByte() }
                    val netmaskBytes = ByteArray(4) { i -> (dhcp.netmask shr (i * 8) and 0xFF).toByte() }
                    try {
                        val ipStr = InetAddress.getByAddress(ipBytes).hostAddress ?: ""
                        val netmaskStr = InetAddress.getByAddress(netmaskBytes).hostAddress ?: ""
                        if (ipStr.isNotEmpty() && netmaskStr.isNotEmpty()) {
                           val range = calculateNetworkRange(ipStr, netmaskStr)
                           // More specific check to avoid returning default for the actual default network
                           if (range != "192.168.1.0/24" || !ipStr.startsWith("192.168.1.")) {
                               return range
                           }
                        }
                    } catch (e: UnknownHostException) {
                        // IP address bytes were invalid
                    }
                }
            }

            // Fallback 2: Use NetworkInterface to find active IPv4 interfaces (existing logic)
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in networkInterfaces) {
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                
                for (interfaceAddress in networkInterface.interfaceAddresses) {
                    val address = interfaceAddress.address
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        val networkPrefixLength = interfaceAddress.networkPrefixLength
                        val networkAddress = getNetworkAddress(address, networkPrefixLength)
                        if (networkAddress.startsWith("192.168.") || 
                            networkAddress.startsWith("10.") || 
                            (networkAddress.startsWith("172.") &&
                             (address.address[1].toInt() and 0xFF) >= 16 &&
                             (address.address[1].toInt() and 0xFF) <= 31)
                            ) {
                             // More specific check to avoid returning default for the actual default network
                            if ("$networkAddress/$networkPrefixLength" != "192.168.1.0/24" || !networkAddress.startsWith("192.168.1.")) {
                                return "$networkAddress/$networkPrefixLength"
                            }
                        }
                    }
                }
            }
            
            return "192.168.1.0/24" // Final fallback
        } catch (e: Exception) {
            // Log.e("NetworkScanner", "Error getting network range", e) // Consider logging
            return "192.168.1.0/24" // Error fallback
        }
    }
    
    private fun getNetworkAddress(address: Inet4Address, prefixLength: Short): String {
        val addressBytes = address.address
        val mask = (-1 shl (32 - prefixLength)).toInt() // Create a mask from prefix length
        
        var networkInt = 0
        for(i in 0..3) {
            networkInt = networkInt or ((addressBytes[i].toInt() and 0xFF) shl ( (3-i) * 8))
        }
        networkInt = networkInt and mask
        
        // Convert the masked integer back to an IP string
        return String.format("%d.%d.%d.%d",
            (networkInt ushr 24) and 0xFF,
            (networkInt ushr 16) and 0xFF,
            (networkInt ushr 8) and 0xFF,
            networkInt and 0xFF)
    }

    // This function might still be used by the WifiManager fallback if not removed
    private fun calculateNetworkRange(ip: String, netmask: String): String {
        return try {
            val ipAddress = InetAddress.getByName(ip) as Inet4Address
            val netmaskAddress = InetAddress.getByName(netmask) as Inet4Address
            
            val ipBytes = ipAddress.address
            val netmaskBytes = netmaskAddress.address
            
            val networkBytes = ByteArray(4)
            for (i in 0..3) {
                networkBytes[i] = (ipBytes[i].toInt() and netmaskBytes[i].toInt()).toByte()
            }
            
            val networkAddress = InetAddress.getByAddress(networkBytes)
            var cidr = 0
            for (byte in netmaskBytes) {
                var b = byte.toInt() and 0xFF
                while (b > 0) {
                    b = b shr 1
                    cidr++
                }
            }
             // A more accurate way to calculate CIDR from netmask bytes:
            var tempCidr = 0
            for (byteVal in netmaskBytes) {
                var v = byteVal.toInt() and 0xFF
                while (v and 0x80 != 0) { // Check most significant bit
                    tempCidr++
                    v = v shl 1
                }
            }
            if (tempCidr > 0) cidr = tempCidr // Use if valid, else old method might be rough

            val calculatedHostAddress = networkAddress.hostAddress
            if (calculatedHostAddress == null) { // hostAddress can be null
                 return "192.168.1.0/24" // Fallback if hostAddress is null
            }
            "$calculatedHostAddress/$cidr"
        } catch (e: Exception) {
            "192.168.1.0/24"
        }
    }

    suspend fun scanNetwork(
        networkRange: String,
        onProgress: (ScanProgress) -> Unit = {},
        onHostDiscovered: (HostInfo) -> Unit = {}
    ): List<HostInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val hosts = generateHostList(networkRange)
                val totalHosts = hosts.size
                var completedHosts = 0
                
                onProgress(ScanProgress(
                    currentOperation = "Starting network scan...",
                    totalHosts = totalHosts
                ))

                // Discover active hosts first
                val activeHosts = mutableListOf<String>()
                
                // Use smaller chunks for better progress updates and memory efficiency
                hosts.chunked(25).forEach { chunk ->
                    val jobs = chunk.map { host ->
                        async {
                            try {
                                if (isHostReachable(host, 500)) { // Reduced timeout for faster scanning
                                    synchronized(activeHosts) {
                                        if (!activeHosts.contains(host)) {
                                            activeHosts.add(host)
                                            val hostInfo = HostInfo(ip = host, isReachable = true)
                                            discoveredHosts[host] = hostInfo
                                            onHostDiscovered(hostInfo)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                // Skip unreachable hosts silently for performance
                            } finally {
                                synchronized(this@NetworkScanner) {
                                    completedHosts++
                                    onProgress(ScanProgress(
                                        currentHost = host,
                                        completedHosts = completedHosts,
                                        totalHosts = totalHosts,
                                        currentOperation = "Discovering hosts..."
                                    ))
                                }
                            }
                        }
                    }
                    jobs.awaitAll()
                }

                // Detailed scan of active hosts
                val detailedResults = mutableListOf<HostInfo>()
                
                activeHosts.forEachIndexed { index, host ->
                    onProgress(ScanProgress(
                        currentHost = host,
                        completedHosts = index, // This should reflect detailed scan progress
                        totalHosts = activeHosts.size,
                        currentOperation = "Scanning ports..."
                    ))
                    
                    val detailedHost = scanHostDetailed(host)
                    detailedResults.add(detailedHost)
                    discoveredHosts[host] = detailedHost // Update with detailed info
                    onHostDiscovered(detailedHost)
                }

                onProgress(ScanProgress(
                    completedHosts = activeHosts.size,
                    totalHosts = activeHosts.size,
                    currentOperation = "Scan completed",
                    isComplete = true
                ))

                detailedResults
            } catch (e: Exception) {
                // Log.e("NetworkScanner", "Error during scanNetwork", e)
                throw e // Rethrow to be handled by ViewModel
            }
        }
    }

    private fun generateHostList(networkRange: String): List<String> {
        val hosts = mutableListOf<String>()
        
        try {
            val parts = networkRange.split("/")
            if (parts.size != 2) throw IllegalArgumentException("Invalid network range format")
            val networkIp = parts[0]
            val cidr = parts[1].toInt()
            
            val ipAddr = InetAddress.getByName(networkIp)
            if (ipAddr !is Inet4Address) throw IllegalArgumentException("Only IPv4 is supported")

            val ipInt = ipAddr.address.fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xFF) }
            val networkMask = (-1 shl (32 - cidr))
            val networkBaseInt = ipInt and networkMask
            val broadcastInt = networkBaseInt or networkMask.inv()

            val hostBits = 32 - cidr
            // Limit number of hosts to generate to avoid OutOfMemoryError for large ranges like /16
            val maxHostsToGenerate = if (hostBits > 8) 254 * 2 else (1 shl hostBits) -2 // Limit for /17 to /23, full for /24+


            if (cidr < 16 || cidr > 30) { // Only scan reasonable CIDR ranges /16 to /30
                 throw IllegalArgumentException("CIDR out of supported range (16-30)")
            }


            for (i in 1 until (1 shl hostBits) -1) { // Iterate through host bits
                if (hosts.size >= maxHostsToGenerate && hostBits > 8) break // Apply limit

                val currentHostInt = networkBaseInt + i
                if (currentHostInt == broadcastInt) continue // Skip broadcast

                val hostBytes = ByteArray(4)
                hostBytes[0] = (currentHostInt ushr 24 and 0xFF).toByte()
                hostBytes[1] = (currentHostInt ushr 16 and 0xFF).toByte()
                hostBytes[2] = (currentHostInt ushr 8 and 0xFF).toByte()
                hostBytes[3] = (currentHostInt and 0xFF).toByte()
                hosts.add(InetAddress.getByAddress(hostBytes).hostAddress ?: continue)
            }
        } catch (e: Exception) {
            // Fallback to common /24 range if parsing fails
            // Log.w("NetworkScanner", "Failed to parse networkRange '$networkRange', using default.", e)
            val defaultIpPrefix = "192.168.1."
            for (i in 1..254) {
                hosts.add("$defaultIpPrefix$i")
            }
        }
        
        return hosts.distinct() // Ensure no duplicates
    }

    private suspend fun isHostReachable(host: String, timeout: Int = 1000): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val address = InetAddress.getByName(host)
                address.isReachable(timeout)
            } catch (e: Exception) {
                false
            }
        }
    }

    private suspend fun scanHostDetailed(host: String): HostInfo {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val openPorts = mutableListOf<Int>()
                val services = mutableMapOf<Int, ServiceInfo>()

                val inetAddress = InetAddress.getByName(host)
                val hostname = try {
                    val canonical = inetAddress.canonicalHostName
                    val hostName = inetAddress.hostName
                    // Use canonical if different from IP, otherwise use hostName
                    when {
                        canonical != host && canonical.isNotEmpty() -> canonical
                        hostName != host && hostName.isNotEmpty() -> hostName
                        else -> ""
                    }
                } catch (e: Exception) {
                    ""
                }

                // Detect OS and device type from hostname patterns
                val (osInfo, deviceType) = detectOSAndDeviceType(hostname, host)

                // Scan common ports in smaller batches for better performance
                commonPorts.chunked(8).forEach { portChunk ->
                    val portJobs = portChunk.map { port ->
                        async {
                            try {
                                if (isPortOpen(host, port, 1500)) { // Reduced timeout
                                    synchronized(openPorts) {
                                        openPorts.add(port)
                                    }
                                    val banner = getBanner(host, port)
                                    val service = ServiceInfo(
                                        port = port,
                                        service = serviceDatabase[port] ?: "Unknown",
                                        banner = banner
                                    )
                                    synchronized(services) {
                                        services[port] = service
                                    }
                                }
                            } catch (e: Exception) {
                                // Skip failed port scans
                            }
                        }
                    }
                    portJobs.awaitAll()
                }

                val responseTime = System.currentTimeMillis() - startTime
                
                // Further refine device type based on open ports
                val refinedDeviceType = refineDeviceTypeFromPorts(deviceType, openPorts, services)

                HostInfo(
                    ip = host,
                    hostname = hostname,
                    osInfo = osInfo,
                    isReachable = true,
                    openPorts = openPorts.sorted(),
                    services = services,
                    responseTime = responseTime,
                    deviceType = refinedDeviceType,
                    lastSeen = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                HostInfo(ip = host, isReachable = false)
            }
        }
    }

    private fun detectOSAndDeviceType(hostname: String, ip: String): Pair<String, DeviceType> {
        val lowercaseHostname = hostname.lowercase()
        
        return when {
            // Apple devices
            lowercaseHostname.contains("macbook") || lowercaseHostname.contains("imac") || 
            lowercaseHostname.contains("mac-") || lowercaseHostname.contains("apple") -> 
                "macOS" to DeviceType.COMPUTER
                
            lowercaseHostname.contains("iphone") || lowercaseHostname.contains("ipad") || 
            lowercaseHostname.contains("ipod") -> 
                "iOS" to DeviceType.MOBILE
                
            // Windows devices
            lowercaseHostname.contains("win") || lowercaseHostname.contains("pc-") || 
            lowercaseHostname.contains("desktop") || lowercaseHostname.contains("laptop") ||
            lowercaseHostname.contains("workstation") -> 
                "Windows" to DeviceType.COMPUTER
                
            // Android devices
            lowercaseHostname.contains("android") || lowercaseHostname.contains("samsung") ||
            lowercaseHostname.contains("pixel") || lowercaseHostname.contains("nexus") -> 
                "Android" to DeviceType.MOBILE
                
            // Network equipment
            lowercaseHostname.contains("router") || lowercaseHostname.contains("gateway") ||
            lowercaseHostname.contains("linksys") || lowercaseHostname.contains("netgear") ||
            lowercaseHostname.contains("asus") || lowercaseHostname.contains("tplink") -> 
                "Network Device" to DeviceType.ROUTER
                
            // Printers
            lowercaseHostname.contains("printer") || lowercaseHostname.contains("canon") ||
            lowercaseHostname.contains("epson") || lowercaseHostname.contains("hp") ||
            lowercaseHostname.contains("brother") -> 
                "Printer" to DeviceType.PRINTER
                
            // IoT devices
            lowercaseHostname.contains("iot") || lowercaseHostname.contains("smart") ||
            lowercaseHostname.contains("alexa") || lowercaseHostname.contains("nest") ||
            lowercaseHostname.contains("ring") -> 
                "IoT Device" to DeviceType.IOT
                
            // Server indicators
            lowercaseHostname.contains("server") || lowercaseHostname.contains("srv") ||
            lowercaseHostname.contains("nas") || lowercaseHostname.contains("database") -> 
                "Server" to DeviceType.SERVER
                
            // Linux patterns
            lowercaseHostname.contains("ubuntu") || lowercaseHostname.contains("debian") ||
            lowercaseHostname.contains("centos") || lowercaseHostname.contains("fedora") ||
            lowercaseHostname.contains("linux") -> 
                "Linux" to DeviceType.COMPUTER
                
            // Generic computer patterns
            lowercaseHostname.contains("computer") || lowercaseHostname.contains("host") -> 
                "Computer" to DeviceType.COMPUTER
                
            // If hostname is empty or generic, try to infer from IP patterns
            hostname.isEmpty() -> inferFromIP(ip)
            
            else -> "Unknown" to DeviceType.UNKNOWN
        }
    }
    
    private fun inferFromIP(ip: String): Pair<String, DeviceType> {
        val lastOctet = ip.substringAfterLast(".").toIntOrNull() ?: 0
        return when {
            lastOctet == 1 -> "Gateway/Router" to DeviceType.ROUTER
            lastOctet in 2..10 -> "Network Device" to DeviceType.ROUTER
            lastOctet in 200..254 -> "DHCP Client" to DeviceType.COMPUTER
            else -> "Unknown" to DeviceType.UNKNOWN
        }
    }
    
    private fun refineDeviceTypeFromPorts(
        currentType: DeviceType, 
        openPorts: List<Int>, 
        services: Map<Int, ServiceInfo>
    ): DeviceType {
        // Don't override if we already have a specific type
        if (currentType != DeviceType.UNKNOWN) return currentType
        
        return when {
            // SSH + web services often indicate servers
            (22 in openPorts && (80 in openPorts || 443 in openPorts)) -> DeviceType.SERVER
            
            // RDP indicates Windows computer
            3389 in openPorts -> DeviceType.COMPUTER
            
            // VNC indicates computer
            5900 in openPorts -> DeviceType.COMPUTER
            
            // Common printer ports
            (631 in openPorts || 9100 in openPorts) -> DeviceType.PRINTER
            
            // Database ports indicate servers
            (3306 in openPorts || 5432 in openPorts || 1433 in openPorts || 27017 in openPorts) -> DeviceType.SERVER
            
            // Web servers
            (80 in openPorts || 443 in openPorts || 8080 in openPorts) -> {
                // Check if it's a router admin interface or web server
                if (openPorts.size <= 3) DeviceType.ROUTER else DeviceType.SERVER
            }
            
            // Few or no open ports, likely end-user device
            openPorts.isEmpty() -> DeviceType.COMPUTER
            
            else -> DeviceType.UNKNOWN
        }
    }

    private suspend fun isPortOpen(host: String, port: Int, timeout: Int = 2000): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeout)
                    true
                }
            } catch (e: IOException) {
                false // Port is closed or filtered
            } catch (e: Exception) {
                false // Other errors
            }
        }
    }

    private suspend fun getBanner(host: String, port: Int): String {
        return withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), 1000) // Reduced timeout
                    socket.soTimeout = 1000 // Reduced read timeout
                    
                    // Try sending a simple HTTP GET for common web ports, or just read for others
                    if (port == 80 || port == 8080) {
                        socket.outputStream.write("GET / HTTP/1.0\r\n\r\n".toByteArray())
                    } else if (port == 443 || port == 8443) {
                        // Basic SSL/TLS handshake might reveal something, but true banner grabbing is complex.
                        // For now, just attempt a read after connect for non-HTTP ports.
                    }


                    val buffer = ByteArray(1024)
                    val bytesRead = socket.getInputStream().read(buffer)
                    if (bytesRead > 0) {
                        String(buffer, 0, bytesRead).trim().replace("\r\n".toRegex(), "\n")
                    } else {
                        ""
                    }
                }
            } catch (e: Exception) {
                "" // Error or no banner
            }
        }
    }

    fun analyzeVulnerabilities(hosts: List<HostInfo>): List<VulnerabilityInfo> {
        val vulnerabilities = mutableListOf<VulnerabilityInfo>()

        hosts.forEach { host ->
            host.services.values.forEach { service ->
                val vulns = checkServiceVulnerabilities(host.ip, service)
                vulnerabilities.addAll(vulns)
            }
        }

        return vulnerabilities.sortedByDescending { it.severity }
    }

    private fun checkServiceVulnerabilities(host: String, service: ServiceInfo): List<VulnerabilityInfo> {
        val vulnerabilities = mutableListOf<VulnerabilityInfo>()

        // Basic vulnerability checks based on service type
        when (service.service.lowercase()) {
            "telnet" -> {
                vulnerabilities.add(VulnerabilityInfo(
                    host = host, port = service.port, service = service.service,
                    severity = VulnerabilitySeverity.HIGH,
                    description = "Telnet service uses unencrypted communication.",
                    recommendation = "Disable Telnet and use SSH for secure remote access."
                ))
            }
            "ftp" -> {
                vulnerabilities.add(VulnerabilityInfo(
                    host = host, port = service.port, service = service.service,
                    severity = VulnerabilitySeverity.MEDIUM,
                    description = "FTP service may transmit credentials in plain text.",
                    recommendation = "Use SFTP or FTPS for secure file transfer. Ensure anonymous access is disabled if not needed."
                ))
            }
             "http" -> {
                if (service.port != 80 && service.port != 8080) { // Non-standard port
                     vulnerabilities.add(VulnerabilityInfo(
                        host = host, port = service.port, service = service.service,
                        severity = VulnerabilitySeverity.INFO,
                        description = "HTTP service running on a non-standard port.",
                        recommendation = "Ensure this is intended. Consider using HTTPS."
                    ))
                } else {
                    vulnerabilities.add(VulnerabilityInfo(
                        host = host, port = service.port, service = service.service,
                        severity = VulnerabilitySeverity.LOW,
                        description = "HTTP service transmits data unencrypted.",
                        recommendation = "Implement HTTPS for secure communication (SSL/TLS)."
                    ))
                }
            }
            "ssh" -> {
                 // Example: Check banner for old versions (simplified)
                if (service.banner.contains("OpenSSH_6", ignoreCase = true) || service.banner.contains("OpenSSH_5", ignoreCase = true)) {
                    vulnerabilities.add(VulnerabilityInfo(
                        host = host, port = service.port, service = service.service,
                        severity = VulnerabilitySeverity.MEDIUM,
                        description = "Potentially outdated SSH version (${service.banner}).",
                        recommendation = "Ensure SSH server is updated to the latest version. Disable weak ciphers and use key-based authentication."
                    ))
                } else {
                     vulnerabilities.add(VulnerabilityInfo(
                        host = host, port = service.port, service = service.service,
                        severity = VulnerabilitySeverity.INFO,
                        description = "SSH service detected. Review configuration.",
                        recommendation = "Ensure strong configuration: disable root login, use key-based authentication, use strong ciphers and MACs, regularly update."
                    ))
                }
            }
            // Add more service-specific checks here
        }
        return vulnerabilities
    }

    fun getDiscoveredHosts(): Map<String, HostInfo> = discoveredHosts.toMap()

    fun clearResults() {
        discoveredHosts.clear()
    }

    fun cancelScan() {
        scanJob?.cancel() // This should effectively stop ongoing coroutines
    }

    fun destroy() {
        job.cancel() // Cancels all coroutines started in this scope
    }
}
