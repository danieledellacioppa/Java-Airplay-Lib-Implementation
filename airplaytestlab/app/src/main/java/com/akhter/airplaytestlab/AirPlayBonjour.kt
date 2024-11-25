package com.akhter.airplaytestlab

import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

class AirPlayBonjour(nameOnNetwork: String) {
    private var jmDNS: JmDNS? = null
    private val nameOnNetwork = nameOnNetwork

    private val AIRPLAY_SERVICE_TYPE: String = "._airplay._tcp.local"
    private val AIRTUNES_SERVICE_TYPE: String = "._raop._tcp.local"

    private var airPlayService: ServiceInfo? = null
    private var airTunesService: ServiceInfo? = null

    fun start(airPlayPort: Int = 7000, airTunesPort: Int = 49152) {
        try {
            // Ottieni l'indirizzo IP locale
            val localIp = getLocalIpAddress()
            if (localIp != null) {
                jmDNS = JmDNS.create(InetAddress.getByName(localIp))

                // Configura i dettagli del servizio
                val serverName = nameOnNetwork // Nome del servizio
                val serviceType = AIRPLAY_SERVICE_TYPE // Tipo del servizio
//                val port = 7000 // Porta del servizio
//                val txtRecord = mapOf(
//                    "deviceid" to "00:11:22:33:44:55", // MAC Address univoco
//                    "model" to "AppleTV3,2", // Modello emulato
//                    "features" to "0x5A7FFFF7,0x1E", // Specifica AirPlay
//                    "srcvers" to "220.68", // Versione del protocollo
//                )

                // Crea e registra il servizio
//                val serviceInfo = ServiceInfo.create(serviceType, serverName, port, 0, 0, txtRecord)
//                jmDNS?.registerService(serviceInfo)

                airPlayService = ServiceInfo.create(
                    serviceType,
                    serverName,
                    airPlayPort,
                    0,
                    0,
                    airPlayMDNSProps()
                )
                jmDNS?.registerService(airPlayService)

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getLocalIpAddress(): String? {
        // Ottieni l'IP locale del dispositivo
        // Semplice metodo per test; usa una libreria migliore per progetti più complessi
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun stop() {
        jmDNS?.unregisterAllServices()
        jmDNS?.close()
    }

    private fun airPlayMDNSProps(): Map<String, String> {
        val airPlayMDNSProps = HashMap<String, String>()
        airPlayMDNSProps["deviceid"] = "01:02:03:04:05:06"
        airPlayMDNSProps["features"] = "0x5A7FFFF7,0x1E"
        airPlayMDNSProps["srcvers"] = "220.68"
        airPlayMDNSProps["flags"] = "0x4"
        airPlayMDNSProps["vv"] = "2"
        airPlayMDNSProps["model"] = "AppleTV2,1"
        airPlayMDNSProps["rhd"] = "5.6.0.0"
        airPlayMDNSProps["pw"] = "false"
        airPlayMDNSProps["pk"] = "b07727d6f6cd6e08b58ede525ec3cdeaa252ad9f683feb212ef8a205246554e7"
        airPlayMDNSProps["pi"] = "2e388006-13ba-4041-9a67-25dd4a43d536"
        return airPlayMDNSProps
    }
}