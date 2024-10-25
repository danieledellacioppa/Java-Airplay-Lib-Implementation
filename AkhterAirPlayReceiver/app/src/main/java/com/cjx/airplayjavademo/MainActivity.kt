package com.cjx.airplayjavademo

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cjx.airplayjavademo.compose.VideoDisplayComposable
import com.cjx.airplayjavademo.model.NALPacket
import com.cjx.airplayjavademo.model.PCMPacket
import com.cjx.airplayjavademo.player.AudioPlayer
import com.cjx.airplayjavademo.player.VideoPlayer
import com.cjx.airplayjavademo.tools.LogRepository
import com.cjx.airplayjavademo.tools.LogRepository.isConnectionActive
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
    private var mVideoPlayer: VideoPlayer? = null
    private var mAudioPlayer: AudioPlayer? = null
    private val mVideoCacheList = LinkedList<NALPacket>()
//    private var isConnectionActive by mutableStateOf(false)

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

        setContent {
            VideoDisplayComposable(this@MainActivity, isConnectionActive, versionName)
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

        Thread(Runnable {
            try {
                airPlayServer.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, "start-server-thread").start()
    }

    override fun onStop() {
        super.onStop()
        mAudioPlayer?.stopPlay()
        mAudioPlayer = null
        mVideoPlayer?.stopVideoPlay()
        mVideoPlayer = null
        airPlayServer.stop()
    }

    private val airplayDataConsumer = object : AirplayDataConsumer {
        override fun onVideo(video: ByteArray) {
            // Imposta lo stato della connessione attiva quando i primi pacchetti video sono ricevuti
            if (!isConnectionActive) {
                isConnectionActive = true
                Log.d(TAG, "Connection active: received first video packet.")
            }

            val nalPacket = NALPacket().apply {
                nalData = video
            }

            if (mVideoPlayer != null) {
                while (mVideoCacheList.isNotEmpty()) {
                    mVideoPlayer?.addPacker(mVideoCacheList.removeFirst())
                }
                mVideoPlayer?.addPacker(nalPacket)
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
            Log.i(TAG, "surfaceChanged: width:$width --- height:$height")
            mVideoPlayer = VideoPlayer(holder.surface, width, height).apply {
                start()
            }
        }
    }



    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceDestroyed: Surface destroyed.")

        //if worst comes to worst, we can try to disconnect from the wifi network
//        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//        val wifiInfo = wifiManager.connectionInfo
//        val networkId = wifiInfo.networkId

        // Rilascia tutte le risorse legate al VideoPlayer
        if (mVideoPlayer != null) {
//            wifiManager.disconnect()
//            wifiManager.disableNetwork(networkId)
//            wifiManager.isWifiEnabled = false
//            wifiManager.reconnect()
            mVideoPlayer?.release()  // Usa il nuovo metodo release per pulire le risorse
            mVideoPlayer = null  // Imposta a null il riferimento per indicare che non c'è un VideoPlayer attivo
        }
    }

}