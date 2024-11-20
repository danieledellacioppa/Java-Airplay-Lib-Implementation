package com.akhter.airplaytestlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.akhter.airplaytestlab.ui.theme.AirplaytestlabTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

class MainActivity : ComponentActivity() {
    private var jmDNS: JmDNS? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AirplaytestlabTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            startBonjourService()
        }
    }

    private fun startBonjourService() {
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

    override fun onDestroy() {
        super.onDestroy()
        jmDNS?.unregisterAllServices()
        jmDNS?.close()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AirplaytestlabTheme {
        Greeting("Android")
    }
}