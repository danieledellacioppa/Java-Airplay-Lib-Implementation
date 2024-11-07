package com.cjx.airplayjavademo.compose

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cjx.airplayjavademo.tools.LogEntry
import com.cjx.airplayjavademo.tools.LogRepository
import com.cjx.airplayjavademo.ui.theme.Gray40


@Composable
fun LogDisplayComposable(
    versionName: String,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
    onStopAudioPlayer: () -> Unit,
    onStopVideoPlayer: () -> Unit
) {
    val logMessages = remember { LogRepository.getLogs() }
    // Crea un LazyListState per monitorare e controllare lo scroll della LazyColumn
    val listState = rememberLazyListState()

    val expandedLogoSP = 2
    val expandedAppleSP = 1

    // Applica la trasformazione per espandere gli spazi nel logo ASCII
    val expandedLogoAscii = remember { expandSpaces(logoAscii, expandedLogoSP) }
    val expandedAppleAscii = remember { expandSpaces(appleAscii, expandedAppleSP) }

    val buttons = listOf(
        "Start Server" to onStartServer,
        "Stop Server" to onStopServer,
        "Stop Audio" to onStopAudioPlayer,
        "Stop Video" to onStopVideoPlayer
    )

    // Layout principale con sfondo verde
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Gray40)
    ) {

        // Mostra il logo ASCII sempre in testa con testo giallo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                )
                {
                    Box(
                        modifier = Modifier
//                            .width(100.dp) // Imposta la larghezza della sovrapposizione
//                            .height(50.dp) // Imposta l'altezza della sovrapposizione
                    ) {
                        Text(
                            text = expandedLogoAscii,
                            style = TextStyle(
                                fontSize = expandedLogoSP.sp,
                                color = Color.Gray
                            ),
                            modifier = Modifier.align(Alignment.Center) // Centra il logo grigio
                        )
                        Text(
                            text = expandedAppleAscii,
                            style = TextStyle(
                                fontSize = expandedAppleSP.sp,
                                color = Color.White
                            ),
                            modifier = Modifier.align(Alignment.BottomCenter) // Sovrappone il logo bianco
                        )
                    }

                }
                Spacer(modifier = Modifier.height(3.dp))
                Column()
                {
                    Text(
                        text = "AirPlay Receiver",
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            fontFamily = minecraftFont
                        ) // Font molto piccolo, testo giallo
                    )
                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    )
                    {
                        Text(
                            text = "Beta Test v",
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = Color.Gray,
                                fontFamily = minecraftFont
                            ) // Font molto piccolo, testo giallo
                        )
                        Text(
                            text = versionName,
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = Color.Black.copy(alpha = 0.4f),
                                fontFamily = minecraftFont
                            ) // Font molto piccolo, testo giallo
                        )
                    }

                }
                // LazyRow per i pulsanti sotto il logo
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier
//                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val paddingValues = PaddingValues(2.dp)
                    items(buttons.size) { index ->
                        val (label, action) = buttons[index]
                        Button(
                            onClick = action,
                            modifier = Modifier.size(60.dp, 25.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF464545),
                                contentColor = Color.Gray
                            ),
                            contentPadding = paddingValues,
                        ) {
                            Text(
                                label,
//                                modifier = Modifier.size(20.dp, 6.dp),
                                color = Color.Gray,
                                style = TextStyle(fontSize = 8.sp, fontFamily = minecraftFont)
                            )
                        }
                    }
                }
            }
        }
        // La LazyColumn con i log che può essere scrollata
        LogColumn(listState, logMessages)
    }

    // Effettua lo scroll verso l'ultimo elemento quando la lista cambia
    LaunchedEffect(logMessages.size) {
        listState.animateScrollToItem(logMessages.size - 1)
    }
}



fun expandSpaces(original: String, sp: Int = 4): String {

    if (sp == 2) {
        return original.replace(" ", "    ")
    }
    else if (sp == 1) {
        return original.replace(" ", "  ")
    }
    return original.replace(" ", "        ")
}

