package com.akhter.airplaytestlab

import android.os.Bundle
import android.view.SurfaceHolder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.toArgb
import com.akhter.airplaytestlab.compose.VideoDisplayComposable
import com.akhter.airplaytestlab.tools.LogRepository
import com.akhter.airplaytestlab.tools.LogRepository.isConnectionActive
import com.akhter.airplaytestlab.ui.theme.BioAuthenticatorTheme
import com.akhter.airplaytestlab.ui.theme.Gray40
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), SurfaceHolder.Callback {


    private val _serverState = MutableStateFlow(ServerState.STOPPED)
    val serverState: StateFlow<ServerState> get() = _serverState

    private var showLog = MutableStateFlow(false)

    private val versionName = "1.0.0"
    private val nameOnNetwork = "Airplay Test Lab"

    private val airPlayBonjour = AirPlayBonjour(nameOnNetwork)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
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
            airPlayBonjour.startBonjourService()
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        airPlayBonjour.stopBonjourService()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        TODO("Not yet implemented")
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        TODO("Not yet implemented")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        TODO("Not yet implemented")
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