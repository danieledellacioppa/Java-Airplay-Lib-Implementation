package com.akhter.airplaytestlab

import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.toArgb
import com.akhter.airplaytestlab.aplib.rtsp.AudioStreamInfo
import com.akhter.airplaytestlab.aplib.rtsp.VideoStreamInfo
import com.akhter.airplaytestlab.apserver.AirPlayServer
import com.akhter.airplaytestlab.apserver.AirplayDataConsumer
import com.akhter.airplaytestlab.compose.VideoDisplayComposable
import com.akhter.airplaytestlab.model.NALPacket
import com.akhter.airplaytestlab.player.VideoPlayer
import com.akhter.airplaytestlab.tools.LogRepository
import com.akhter.airplaytestlab.tools.LogRepository.isConnectionActive
import com.akhter.airplaytestlab.ui.theme.BioAuthenticatorTheme
import com.akhter.airplaytestlab.ui.theme.Gray40
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.LinkedList

class MainActivity : ComponentActivity(), SurfaceHolder.Callback {


    private val _serverState = MutableStateFlow(ServerState.STOPPED)
    val serverState: StateFlow<ServerState> get() = _serverState

    private var mVideoPlayer: VideoPlayer? = null
    private val mVideoCacheList = LinkedList<NALPacket>()


    private var showLog = MutableStateFlow(true)

    private val versionName = "1.0.0"
    private val nameOnNetwork = "Airplay Test Lab"

//    private val airPlayBonjour = AirPlayBonjour(nameOnNetwork)
    private lateinit var airPlayServer: AirPlayServer

    private val TAG = "MainActivity"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogRepository.addLog("MainActivity", "onCreate")
        window.navigationBarColor = Gray40.toArgb()
        setContent {
            BioAuthenticatorTheme {
                val serverState = serverState.collectAsState()
                val showLogState = showLog.collectAsState()

                VideoDisplayComposable(
                    this@MainActivity,
                    isConnectionActive,
                    versionName,
                    nameOnNetwork,
                    ::toggleServer,
                    ::stopAudioPlayer,
                    ::stopVideoPlayer,
                    showLogState.value,
                    ::toggleLogVisibility,
                    serverState
                )
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
//            airPlayBonjour.start()
            airPlayServer = AirPlayServer(nameOnNetwork, 7000, 49152, airplayDataConsumer)
            airPlayServer.start()
        }
    }


    private val airplayDataConsumer = object : AirplayDataConsumer {

        override fun onVideo(video: ByteArray?) {
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

        override fun onVideoFormat(videoStreamInfo: VideoStreamInfo?) {
            LogRepository.addLog(TAG, "Video format received.")
            TODO ("Not yet implemented")
        }

        override fun onAudio(audio: ByteArray?) {
            LogRepository.addLog(TAG, "Audio received.")
            TODO("Not yet implemented")
        }

        override fun onAudioFormat(audioInfo: AudioStreamInfo?) {
            LogRepository.addLog(TAG, "Audio format received.")
            TODO("Not yet implemented")
        }
    }



    override fun onDestroy() {
        super.onDestroy()
//        airPlayBonjour.stop()
        airPlayServer.stop()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        LogRepository.addLog(TAG, "Surface created.")
        TODO("Not yet implemented")
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
        LogRepository.addLog(TAG, "Surface destroyed.")
         // Rilascia tutte le risorse legate al VideoPlayer
        if (mVideoPlayer != null) {
            mVideoPlayer?.release()  // Usa il nuovo metodo release per pulire le risorse
            mVideoPlayer = null  // Imposta a null il riferimento per indicare che non c'è un VideoPlayer attivo
        }
    }

    private fun toggleServer() {
    TODO("Not yet implemented")
    }

    private fun stopAudioPlayer() {
    TODO("Not yet implemented")
    }

    private fun stopVideoPlayer() {
    TODO("Not yet implemented")
    }

    // Funzione per alternare la visibilità del log
    fun toggleLogVisibility() {
        showLog.value = !showLog.value
    }
}