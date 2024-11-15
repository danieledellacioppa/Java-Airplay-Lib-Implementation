package com.cjx.airplayjavademo.compose

import airplayjavademo.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cjx.airplayjavademo.compose.logdisplay.LogScaffoldContent
import com.cjx.airplayjavademo.ui.theme.Gray40

@Composable
fun LogDisplayComposable(
    versionName: String,
    onToggleServer: () -> Boolean,  // Cambiato il tipo di ritorno per riflettere lo stato attuale
    onStopAudioPlayer: () -> Unit,
    onStopVideoPlayer: () -> Unit,
    showLog: Boolean,
    toggleLogVisibility: () -> Unit,
    isServerRunning: State<Boolean>, // Usa State invece di MutableState
    isServerStarting: State<Boolean>,
    isServerStopping: State<Boolean>
) {
    val scaffoldState = rememberScaffoldState()
    var showButtons by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        scaffoldState = scaffoldState,
        bottomBar = {
            Row(
                modifier = Modifier
//                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
//                Text(
//                    text = "toolbar",
//                    style = MaterialTheme.typography.h6,
//                    color = Color.White
//                )
                Spacer(modifier = Modifier.weight(1f))
                    Image(
                        modifier = Modifier.clickable { toggleLogVisibility() },
                        painter = painterResource(id = R.drawable.log),
                        contentDescription = "Toggle Log Visibility"
                    )
            }
        },
        drawerContent = {
            Column(modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Gray40, Gray40.copy(alpha = 0.8f))
                    )

                )
                .padding(16.dp)) {
                Text("Akhter Airplay Receiver Settings", style = MaterialTheme.typography.h6)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Buttons")
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = showButtons,
                        onCheckedChange = { showButtons = it }
                    )
                }
            }
        },
        drawerGesturesEnabled = true,
        drawerElevation = 16.dp,
        drawerShape = MaterialTheme.shapes.small.copy(
            topStart = CornerSize(0.dp),
            topEnd = CornerSize(16.dp),
            bottomEnd = CornerSize(16.dp),
            bottomStart = CornerSize(0.dp)
        )
    ) {
        LogScaffoldContent(
            it,
            coroutineScope,
            scaffoldState,
            versionName,
            showButtons,
            onToggleServer,
            onStopAudioPlayer,
            onStopVideoPlayer,
            toggleLogVisibility,
            isServerRunning,
            isServerStarting,
            isServerStopping,
            showLog
        )
    }
}

