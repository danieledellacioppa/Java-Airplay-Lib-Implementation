package com.akhter.airplaytestlab.compose.logdisplay

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ScaffoldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akhter.airplaytestlab.R
import com.akhter.airplaytestlab.ServerState
import com.akhter.airplaytestlab.tools.LogRepository
import com.akhter.airplaytestlab.compose.LogColumn
import com.akhter.airplaytestlab.compose.appleAscii
import com.akhter.airplaytestlab.compose.logoAscii
import com.akhter.airplaytestlab.compose.minecraftFont
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun LogScaffoldContent(
    it: PaddingValues,
    coroutineScope: CoroutineScope,
    scaffoldState: ScaffoldState,
    versionName: String,
    nameOnNetwork: String,
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
            .background(Color(Gray.toArgb()))
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
            val expandedAppleSP = 4

            val expandedLogoAscii = remember { expandSpaces(logoAscii, expandedLogoSP) }
            val expandedAppleAscii = remember { expandSpaces(appleAscii, expandedAppleSP) }

            val generalTextSize = 16.sp

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                   horizontalAlignment = Alignment.CenterHorizontally,
                     verticalArrangement = Arrangement.Center
                ) {
                    Column (
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ){
                        Text(
                            text = "AirPlay Receiver",
                            style = TextStyle(
                                fontSize = generalTextSize,
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
                                    fontSize = generalTextSize,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontFamily = minecraftFont
                                )
                            )
                            Text(
                                text = versionName,
                                style = TextStyle(
                                    fontSize = generalTextSize,
                                    color = Color.Black.copy(alpha = 0.8f),
                                    fontFamily = minecraftFont
                                )
                            )
                        }
                    }
                    Box {
                        Text(
                            text = expandedLogoAscii,
                            style = TextStyle(fontSize = 2.sp, color = Color.Red),
                            modifier = Modifier.align(Alignment.Center)
                        )
                        Text(
                            text = expandedAppleAscii,
                            style = TextStyle(fontSize = 2.sp, color = Color.Yellow),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )

                    }
                    Spacer(modifier = Modifier.width(3.dp))
                    Column (
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ){
                        val nameTextColor = if (serverState.value == ServerState.RUNNING) Color(0xFF00BB00) else Color.White.copy(alpha = 0.7f)
                        val displayText = if (serverState.value == ServerState.RUNNING) {
                            "You can now cast from your iPhone to:"
                        } else {
                            "Name on Network"
                        }

                        Text(
                            text = displayText,
                            style = TextStyle(
                                fontSize = generalTextSize,
                                color = Color.White.copy(alpha = 0.7f),
                                fontFamily = minecraftFont
                            )
                        )
                        Text(
                            text = nameOnNetwork,
                            style = TextStyle(
                                fontSize = generalTextSize,
                                color = nameTextColor,
                                fontFamily = minecraftFont
                            )
                        )
                    }

                }
                Spacer(modifier = Modifier.height(3.dp))
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
                                backgroundColor,
                                textColor
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
