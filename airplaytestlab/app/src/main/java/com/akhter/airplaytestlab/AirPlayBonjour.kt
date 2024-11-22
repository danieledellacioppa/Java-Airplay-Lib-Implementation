package com.akhter.airplaytestlab

import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

class AirPlayBonjour(private var jmDNS: JmDNS?) {

    fun startBonjourService() {
        try {
            // Ottieni l'indirizzo IP locale
            val localIp = getLocalIpAddress()
            if (localIp != null) {
                jmDNS = JmDNS.create(InetAddress.getByName(localIp))

                // Configura i dettagli del servizio
                val serviceName = "AndroidScreencast"
                val serviceType = "_airplay._tcp.local." // Tipo di servizio Bonjour
                val port = 7000 // Porta del servizio
                val txtRecord = mapOf(
                    "deviceid" to "00:11:22:33:44:55", // MAC Address univoco
                    "model" to "AppleTV3,2", // Modello emulato
                    "features" to "0x5A7FFFF7,0x1E", // Specifica AirPlay
                    "srcvers" to "220.68", // Versione del protocollo
                )

                // Crea e registra il servizio
                val serviceInfo = ServiceInfo.create(serviceType, serviceName, port, 0, 0, txtRecord)
                jmDNS?.registerService(serviceInfo)
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

    fun stopBonjourService() {
        jmDNS?.unregisterAllServices()
        jmDNS?.close()
    }
}