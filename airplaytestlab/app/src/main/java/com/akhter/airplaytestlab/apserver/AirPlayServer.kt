package com.akhter.airplaytestlab.apserver

import com.akhter.airplaytestlab.aplib.AirPlayBonjour
import com.akhter.airplaytestlab.tools.LogRepository

class AirPlayServer(
    serverName : String,
    airPlayPort : Int = 7000,
    airTunesPort : Int = 49152,
    airplayDataConsumer : AirplayDataConsumer
) {
    private val airPlayBonjour = AirPlayBonjour(serverName)
//    private val controlServer = ControlServer(airPlayPort, airTunesPort, airplayDataConsumer)
//    private var controlServerThread: Thread? = null
    private val TAG = "AirPlayServer"
    val serverName = serverName
    val airPlayPort = airPlayPort
    val airTunesPort = airTunesPort

    @Throws(Exception::class)
    fun start() {
        airPlayBonjour.start(airPlayPort, airTunesPort)
        LogRepository.addLog(TAG, "$serverName started on ports: $airPlayPort, $airTunesPort", 'I')

//        controlServerThread = Thread(controlServer).apply { start() }
    }


    fun stop() {
        airPlayBonjour.stop() // Ferma Bonjour
        LogRepository.addLog(TAG, "$serverName stopped", 'I')

//        controlServerThread?.let { thread ->
//            controlServer.stop()
//            try {
//                thread.join() // Attende la terminazione
//                Log.d(TAG, "ControlServer stopped")
//                LogRepository.addLog(TAG, "ControlServer stopped", 'W')
//            } catch (e: InterruptedException) {
//                Thread.currentThread().interrupt() // Gestisce l'interruzione
//            }
//        }
    }

}