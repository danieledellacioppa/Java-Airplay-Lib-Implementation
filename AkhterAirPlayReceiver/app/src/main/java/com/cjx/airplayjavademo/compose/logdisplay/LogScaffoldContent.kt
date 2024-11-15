package com.cjx.airplayjavademo.compose.logdisplay

import airplayjavademo.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.IconButton
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cjx.airplayjavademo.ServerState
import com.cjx.airplayjavademo.compose.LogColumn
import com.cjx.airplayjavademo.compose.appleAscii
import com.cjx.airplayjavademo.compose.logoAscii
import com.cjx.airplayjavademo.compose.minecraftFont
import com.cjx.airplayjavademo.tools.LogRepository
import com.cjx.airplayjavademo.ui.theme.Gray40
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun LogScaffoldContent(
    it: PaddingValues,
    coroutineScope: CoroutineScope,
    scaffoldState: ScaffoldState,
    versionName: String,
    showButtons: Boolean,
    onToggleServer: () -> Unit,
    onStopAudioPlayer: () -> Unit,
    onStopVideoPlayer: () -> Unit,
    toggleLogVisibility: () -> Unit,
    serverState: State<ServerState>,
    showLog: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Gray40)
            .padding(it)
    ) {
        // Ingranaggio in alto a destra
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = {
                coroutineScope.launch {
                    scaffoldState.drawerState.open()
                }
            }) {
                Image(
                    painter = painterResource(R.drawable.settings),
                    contentDescription = "Settings",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Logo ASCII e informazioni versione
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Codice del logo ASCII e version name...
            // Applica la trasformazione per espandere gli spazi nel logo ASCII

            val expandedLogoSP = 4
            val expandedAppleSP = 2

            val expandedLogoAscii = remember { expandSpaces(logoAscii, expandedLogoSP) }
            val expandedAppleAscii = remember { expandSpaces(appleAscii, expandedAppleSP) }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        Text(
                            text = expandedLogoAscii,
                            style = TextStyle(fontSize = 2.sp, color = Color.Gray),
                            modifier = Modifier.align(Alignment.Center)
                        )
                        Text(
                            text = expandedAppleAscii,
                            style = TextStyle(fontSize = 1.sp, color = Color.White),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "AirPlay Receiver",
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontFamily = minecraftFont
                    )
                )
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Beta Test v",
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontFamily = minecraftFont
                        )
                    )
                    Text(
                        text = versionName,
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = Color.Black.copy(alpha = 0.4f),
                            fontFamily = minecraftFont
                        )
                    )
                }
            }
        }

        // Mostra la lista dei pulsanti solo se showButtons è true
        if (showButtons) {
            val buttons = listOf(
                ButtonInfo(
                    id = "toggle_server",
                    label = "Toggle Server",
                    action = onToggleServer
                ),
                ButtonInfo(
                    id = "toggle_log",
                    label = "Toggle Log",
                    action = toggleLogVisibility
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(
                        8.dp,
                        Alignment.CenterHorizontally
                    ),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(buttons.size) { index ->
                        val buttonInfo = buttons[index]
                        val (backgroundColor, textColor, labelText) = if (buttonInfo.id == "toggle_server") {
                            when (serverState.value) {
                                ServerState.STARTING -> Triple(Color(0xFFBB5600), Color.White, "Server is Starting")
                                ServerState.RUNNING -> Triple(Color(0xFF00BB00), Color.White, "Server is Running")
                                ServerState.STOPPING -> Triple(Color(0x98164545), Color.White, "Server is Stopping")
                                ServerState.STOPPED -> Triple(Color(0xFF464545), Color.Gray, "Server is Stopped")
                            }
                        } else {
                            // Colori e testo statici per altri bottoni
                            Triple(Color(0xFF464545), Color.Gray, buttonInfo.label)
                        }


                        Button(
                            onClick = { buttonInfo.action.invoke() },
                            modifier = Modifier.size(60.dp, 25.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = backgroundColor,
                                contentColor = textColor
                            ),
                            contentPadding = PaddingValues(1.dp)
                        ) {
                            Text(
                                labelText,
                                color = textColor,
                                fontSize = 4.sp,
                                style = TextStyle(fontFamily = minecraftFont)
                            )
                        }
                    }
                }
            }
        }

        // Mostra LogColumn se showLog è true
        if (showLog) {
            val logMessages = remember { LogRepository.getLogs() }
            LogColumn(rememberLazyListState(), logMessages)
        }

    }
}

// Funzione per espandere gli spazi nel logo ASCII
fun expandSpaces(original: String, sp: Int = 4): String {
    return original.replace(" ", " ".repeat(sp))
}
