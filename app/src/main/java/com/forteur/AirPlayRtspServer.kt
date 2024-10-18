package com.forteur

import android.util.Log
import com.github.serezhka.jap2lib.AirPlay
import com.github.serezhka.jap2lib.AirPlayBonjour
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.net.ServerSocket
import java.net.Socket

class AirPlayRtspServer(private val serverName: String, private val airPlayPort: Int, private val airTunesPort: Int) {

    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val airPlay = AirPlay() // Instanzia l'oggetto AirPlay
    private lateinit var airPlayBonjour: AirPlayBonjour

    // Avvia il server RTSP e il servizio Bonjour
    fun startServer() {
        CoroutineScope(Dispatchers.IO).launch {
            airPlayBonjour = AirPlayBonjour(serverName)
            try {
                airPlayBonjour.start(airPlayPort, airTunesPort)
                Log.d(TAG, "Bonjour service started with server name: $serverName on ports $airPlayPort and $airTunesPort")

                serverSocket = ServerSocket(airPlayPort)
                Log.d(TAG, "RTSP Server started on port $airPlayPort")

                isRunning = true
                while (isRunning) {
                    val clientSocket = serverSocket?.accept()
                    clientSocket?.let {
                        handleClient(it)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in RTSP Server: ${e.message}")
            }
        }
    }

    // Gestisci il client che si connette
    private fun handleClient(clientSocket: Socket) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reader = clientSocket.getInputStream()
                val writer = clientSocket.getOutputStream()
                handleRtspRequest(reader, writer)
                clientSocket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling client: ${e.message}")
            }
        }
    }

    // Gestisci le richieste RTSP
    private fun handleRtspRequest(inputStream: InputStream, outputStream: OutputStream) {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val requestLine = reader.readLine() ?: return
        Log.d(TAG, "Received RTSP request: $requestLine")

        val uri = requestLine.split(" ")[1]
        val requestContent = inputStream.readBytes()

        when (uri) {
            "/info" -> {
                airPlay.info(outputStream)
                sendRtspOk(outputStream)
            }
            "/pair-setup" -> {
                airPlay.pairSetup(outputStream)
                sendRtspOk(outputStream)
            }
            "/pair-verify" -> {
                airPlay.pairVerify(ByteArrayInputStream(requestContent), outputStream)
                sendRtspOk(outputStream)
            }
            "/fp-setup" -> {
                airPlay.fairPlaySetup(ByteArrayInputStream(requestContent), outputStream)
                sendRtspOk(outputStream)
            }
            "/feedback" -> {
                sendRtspOk(outputStream)
            }
            else -> {
                Log.w(TAG, "Unknown RTSP request: $uri")
                sendRtspNotImplemented(outputStream)
            }
        }
    }

    // RTSP setup per video e audio
    fun setupRtsp(inputStream: InputStream, outputStream: OutputStream, videoDataPort: Int, videoEventPort: Int, videoTimingPort: Int, audioDataPort: Int, audioControlPort: Int) {
        airPlay.rtspSetupEncryption(inputStream)
        airPlay.rtspSetupVideo(outputStream, videoDataPort, videoEventPort, videoTimingPort)
        airPlay.rtspSetupAudio(outputStream, audioDataPort, audioControlPort)

        if (airPlay.isFairPlayVideoDecryptorReady()) {
            Log.d(TAG, "FairPlay Video Decryptor is ready, start listening on videoDataPort $videoDataPort")
            // Avvia la ricezione dati video
        }

        if (airPlay.isFairPlayAudioDecryptorReady()) {
            Log.d(TAG, "FairPlay Audio Decryptor is ready, start listening on audioDataPort $audioDataPort")
            // Avvia la ricezione dati audio
        }
    }

    private fun sendRtspOk(outputStream: OutputStream) {
        val response = "RTSP/1.0 200 OK\r\nCSeq: 1\r\n\r\n"
        outputStream.write(response.toByteArray())
    }

    private fun sendRtspNotImplemented(outputStream: OutputStream) {
        val response = "RTSP/1.0 501 Not Implemented\r\nCSeq: 1\r\n\r\n"
        outputStream.write(response.toByteArray())
    }

    fun stopServer() {
        CoroutineScope(Dispatchers.IO).launch {
            airPlayBonjour.stop()
            isRunning = false
            serverSocket?.close()
            Log.d(TAG, "RTSP Server and Bonjour service stopped")
        }
    }

    companion object {
        private const val TAG = "AirPlayRtspServer"
    }
}