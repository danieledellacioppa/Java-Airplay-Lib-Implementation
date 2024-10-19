package com.forteur

import android.os.Bundle
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    private lateinit var airPlayRtspServer: AirPlayRtspServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Avvia il server RTSP con Bonjour
        airPlayRtspServer = AirPlayRtspServer("@prova", 5001, 7001)
        airPlayRtspServer.startServer()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ferma il server RTSP e Bonjour
        airPlayRtspServer.stopServer()
    }
}