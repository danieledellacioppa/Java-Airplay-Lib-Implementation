package com.cjx.airplayjavademo

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import com.cjx.airplayjavademo.compose.VideoDisplayComposable
import com.cjx.airplayjavademo.model.NALPacket
import com.cjx.airplayjavademo.model.PCMPacket
import com.cjx.airplayjavademo.player.AudioPlayer
import com.cjx.airplayjavademo.player.VideoPlayer
import com.cjx.airplayjavademo.tools.LogRepository
import com.cjx.airplayjavademo.tools.LogRepository.isConnectionActive
import com.cjx.airplayjavademo.ui.theme.BioAuthenticatorTheme
import com.cjx.airplayjavademo.ui.theme.Gray40
import com.github.serezhka.jap2lib.rtsp.AudioStreamInfo
import com.github.serezhka.jap2lib.rtsp.VideoStreamInfo
import com.github.serezhka.jap2server.AirPlayServer
import com.github.serezhka.jap2server.AirplayDataConsumer
import java.util.LinkedList

/**
 * # MainActivity
 *
 * The `MainActivity` class handles the initialization of the AirPlay server for mirroring video and audio
 * streams from an iOS device to an Android device. It sets up a SurfaceView to display the video content
 * and uses custom AudioPlayer and VideoPlayer classes to manage the playback.
 *
 * The activity starts the AirPlay server and listens for incoming mirroring data via the `AirplayDataConsumer` interface,
 * which processes video and audio streams.
 *
 * ## Main Features
 * - Initializes and starts the AirPlay server.
 * - Uses a `SurfaceView` for video display.
 * - Handles video and audio streams using `VideoPlayer` and `AudioPlayer`.
 * - Caches incoming video packets when the player is not ready and plays them once ready.
 *
 * ## Lifecycle Management
 * - `onCreate(Bundle?)`: Initializes the SurfaceView and AirPlay server, and starts audio playback.
 * - `onStop()`: Stops the AirPlay server and releases resources.
 *
 * ## Surface Callbacks
 * - `surfaceCreated(SurfaceHolder)`: Currently unused.
 * - `surfaceChanged(SurfaceHolder, int, int, int)`: Initializes the `VideoPlayer` when the Surface is available and starts video playback.
 * - `surfaceDestroyed(SurfaceHolder)`: Currently unused.
 *
 * ## AirplayDataConsumer
 * - `onVideo(ByteArray)`: Receives video data packets, caches them if the video player is not ready, and adds them to the player once it is ready.
 * - `onVideoFormat(VideoStreamInfo)`: Receives video format information (currently unused).
 * - `onAudio(ByteArray)`: Receives audio data packets and adds them to the audio player.
 * - `onAudioFormat(AudioStreamInfo)`: Receives audio format information (currently unused).
 *
 * ## Usage
 * This activity sets up and manages the AirPlay server for mirroring video and audio streams, while ensuring the
 * proper lifecycle management of video and audio resources.
 */




class MainActivity : ComponentActivity(), SurfaceHolder.Callback {

    private lateinit var airPlayServer: AirPlayServer
    private var serverThread: Thread? = null // Thread per avviare il server manualmente
    private var mVideoPlayer: VideoPlayer? = null
    private var mAudioPlayer: AudioPlayer? = null
    private val mVideoCacheList = LinkedList<NALPacket>()
//    private var isConnectionActive by mutableStateOf(false)
    private var showLog = mutableStateOf(false)


    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val versionName = try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }

        window.navigationBarColor = Gray40.toArgb()
        setContent {
            BioAuthenticatorTheme {
                VideoDisplayComposable(
                    this@MainActivity,
                    isConnectionActive,
                    versionName,
                    ::startServer,
                    ::stopServer,
                    ::stopAudioPlayer,
                    ::stopVideoPlayer,
                    showLog.value,
                    ::toggleLogVisibility
                )
            }
        }
        LogRepository.addLog(TAG, "onCreate: AirPlay server initialized. Version: $versionName")
        mAudioPlayer = AudioPlayer().apply {
            start()
        }

        //retrieve uname -a and print it via Android Shell
        val process = Runtime.getRuntime().exec("getprop ro.product.product.model")
        var deviceModel = process.inputStream.bufferedReader().readText()
        LogRepository.addLog(TAG, "Device model: $deviceModel")

        //remove the newline character from the device model string
        deviceModel = deviceModel.replace("\n", "")

        airPlayServer = AirPlayServer("AKHTER:$deviceModel", 7000, 49152, airplayDataConsumer)

    }

    fun startServer() {
        if (serverThread == null || !serverThread!!.isAlive) {
            serverThread = Thread(Runnable {
                try {
                    airPlayServer.start()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, "start-server-thread")
            serverThread?.start()
            LogRepository.addLog(TAG, "AirPlay server started with threadId: ${serverThread?.id}")
        }
    }

    fun stopServer() {
        try {
            LogRepository.addLog(TAG, "Stopping AirPlay server with threadId: ${serverThread?.id}")
            airPlayServer.stop() // Ferma il server AirPlay
            serverThread?.join() // Attende la terminazione del thread
            serverThread = null
            LogRepository.addLog(TAG, "AirPlay server stopped and thread terminated.")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopAudioPlayer() {
        mAudioPlayer?.stopPlayer()
        mAudioPlayer = null
        LogRepository.addLog(TAG, "AudioPlayer stopped.")
    }

    fun stopVideoPlayer() {
        mVideoPlayer?.stopPlayer()
        mVideoPlayer = null
        LogRepository.addLog(TAG, "VideoPlayer stopped.")
    }

    // Funzione per alternare la visibilità del log
    fun toggleLogVisibility() {
        showLog.value = !showLog.value
    }


    override fun onStop() {
        super.onStop()
        mAudioPlayer?.stopPlay()
        mAudioPlayer = null
        mVideoPlayer?.stopPlayer()
        mVideoPlayer = null
        airPlayServer.stop()
    }

    private val airplayDataConsumer = object : AirplayDataConsumer {
        override fun onVideo(video: ByteArray) {
            // Imposta lo stato della connessione attiva quando i primi pacchetti video sono ricevuti
            if (!isConnectionActive) {
                isConnectionActive = true
                Log.d(TAG, "Connection active: received first video packet.")
                LogRepository.addLog(TAG, "Connection active: received first video packet.")
            }

            val nalPacket = NALPacket().apply {
                nalData = video
            }

            if (mVideoPlayer != null) {
                while (mVideoCacheList.isNotEmpty()) {
                    mVideoPlayer?.addPacket(mVideoCacheList.removeFirst())
                }
                mVideoPlayer?.addPacket(nalPacket)
            } else {
                mVideoCacheList.add(nalPacket)
            }
        }

        override fun onVideoFormat(videoStreamInfo: VideoStreamInfo) {
            // Potresti usare anche questo punto per rilevare la connessione
        }

        override fun onAudio(audio: ByteArray) {
            // Anche qui puoi impostare lo stato della connessione
            if (!isConnectionActive) {
                isConnectionActive = true
                Log.d(TAG, "Connection active: received first audio packet.")
            }

            val pcmPacket = PCMPacket().apply {
                data = audio
            }

            mAudioPlayer?.addPacker(pcmPacket)
        }

        override fun onAudioFormat(audioInfo: AudioStreamInfo) {
            // Implement if needed
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceCreated: Surface created.")
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (mVideoPlayer == null) {
            LogRepository.addLog(TAG, "surfaceChanged: width:$width --- height:$height")
            mVideoPlayer = VideoPlayer(holder.surface).apply {
                start()
            }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceDestroyed: Surface destroyed.")

        // Rilascia tutte le risorse legate al VideoPlayer
        if (mVideoPlayer != null) {
            mVideoPlayer?.release()  // Usa il nuovo metodo release per pulire le risorse
            mVideoPlayer = null  // Imposta a null il riferimento per indicare che non c'è un VideoPlayer attivo
        }
    }

}