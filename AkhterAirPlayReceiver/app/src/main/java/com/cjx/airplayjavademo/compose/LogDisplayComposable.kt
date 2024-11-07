package com.cjx.airplayjavademo.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
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
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(buttons.size) { index ->
                        val (label, action) = buttons[index]
                        Button(onClick = action) {
                            Text(
                                label,
                                style = TextStyle(fontSize = 12.sp, fontFamily = minecraftFont)
                            )
                        }
                    }
                }
            }


        }

        // La LazyColumn con i log che può essere scrollata
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp) // Stacca la LazyColumn dai bordi
                .clip(RoundedCornerShape(6.dp)) // Angoli arrotondati
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x55AAEA00),
                            Color(0xFF464545)
                        )
                    )
                ),
            state = listState
        ) {
            items(logMessages.size) { index ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.Start
                ) {

                    // Imposta larghezza fissa per il punto
                    Box(
                        modifier = Modifier
                            .width(50.dp) // Fissare la larghezza per la pallina
                            .height(25.dp) // Fissare l'altezza per la pallina
                            .align(Alignment.CenterVertically)
                    ) {
                        Text(
                            text = logMessages[index].time, // Ora del log
                            style = TextStyle(
                                color = Color(0xFFFF6600), // Testo arancione
                                fontSize = 12.sp,
                                fontFamily = alata,
                                shadow = Shadow(
                                    color = Color.Black,
                                    offset = Offset(2f, 2f),
                                    blurRadius = 3f
                                )
                            ),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Mostra il tag con testo rosso
                    Text(
                        text = logMessages[index].tag, // Tag del log
                        modifier = Modifier
                            .padding(start = 3.dp) // Margine per distanziare il testo dal punto
                            .align(Alignment.CenterVertically),
                        style = TextStyle(
                            color = Color.Cyan,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace, // Imposta font monospace
                            shadow = Shadow(
                                color = Color.Black,
                                offset = Offset(2f, 2f),
                                blurRadius = 3f
                            )
                        ),
                        fontFamily = FontFamily.Monospace
                    )

                    // Mostra i log con testo giallo
                    Text(
                        text = logMessages[index].message, // Messaggio del log
                        modifier = Modifier
                            .padding(start = 3.dp) // Margine per distanziare il testo dal punto
                            .align(Alignment.CenterVertically)
                            .clip(RoundedCornerShape(3.dp))
                            //sfondo sfumato trasparente
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        logMessages[index].type.color.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            )
                        ,
                        style = TextStyle(
                            color = logMessages[index].type.color, // Colore del testo
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace, // Imposta font monospace
//                            lineHeight = 4.sp,
                            shadow = Shadow(
                                color = Color.Black,
                                offset = Offset(2f, 2f),
                                blurRadius = 3f
                            )
                        ),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
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

