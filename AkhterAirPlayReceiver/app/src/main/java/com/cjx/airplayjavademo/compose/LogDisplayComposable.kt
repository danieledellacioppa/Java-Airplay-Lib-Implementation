package com.cjx.airplayjavademo.compose

import airplayjavademo.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cjx.airplayjavademo.tools.LogRepository
import com.cjx.airplayjavademo.ui.theme.Gray40
import kotlinx.coroutines.launch


@Composable
fun LogDisplayComposable(
    versionName: String,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
    onStopAudioPlayer: () -> Unit,
    onStopVideoPlayer: () -> Unit,
    showLog: Boolean,
    toggleLogVisibility: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    var showButtons by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        scaffoldState = scaffoldState,
        drawerContent = {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Settings", style = MaterialTheme.typography.h6)
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
        }
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
                        painter = painterResource(R.drawable.airplayakhter),
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
                    "Start Server" to onStartServer,
                    "Stop Server" to onStopServer,
                    "Stop Audio" to onStopAudioPlayer,
                    "Stop Video" to onStopVideoPlayer,
                    "Toggle Log" to toggleLogVisibility
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(buttons.size) { index ->
                            val (label, action) = buttons[index]
                            Button(
                                onClick = action,
                                modifier = Modifier.size(60.dp, 25.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFF464545),
                                    contentColor = Color.Gray
                                ),
                                contentPadding = PaddingValues(1.dp)
                            ) {
                                Text(label, color = Color.Gray, fontSize = 6.sp)
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
}

// Funzione per espandere gli spazi nel logo ASCII
fun expandSpaces(original: String, sp: Int = 4): String {
    return original.replace(" ", " ".repeat(sp))
}
