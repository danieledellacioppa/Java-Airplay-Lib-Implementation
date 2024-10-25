package com.cjx.airplayjavademo.compose

import airplayjavademo.R
import android.view.SurfaceView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import android.view.SurfaceHolder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cjx.airplayjavademo.tools.LogRepository
import com.cjx.airplayjavademo.ui.theme.Gray40
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun VideoDisplayComposable(
    callback: SurfaceHolder.Callback,
    isConnectionActive: Boolean,
    versionName: String
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isConnectionActive) {
            // Controlliamo se il SurfaceView è già attivo prima di inizializzarlo nuovamente
            AndroidView(
                factory = { context ->
                    SurfaceView(context).apply {
                        holder.addCallback(callback)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    // Logica di aggiornamento del SurfaceView, se necessario
                }
            )
        } else {
            LogDisplayComposable(versionName)
        }
    }
}

@Composable
fun LogDisplayComposable(versionName: String) {
    val logMessages = remember { LogRepository.getLogs() }

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
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            )
            {
                Text(
                    text = logoAscii,
                    style = TextStyle(fontSize = 4.sp, color = Color.Yellow) // Font molto piccolo, testo giallo
                )
                Column()
                {
                    Text(
                        text = "AirPlay Receiver",
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = Color.Yellow,
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
                                fontSize = 16.sp,
                                color = Color.Red,
                                fontFamily = minecraftFont
                            ) // Font molto piccolo, testo giallo
                        )
                        Text(
                            text = versionName,
                            style = TextStyle(
                                fontSize = 16.sp,
                                color = Color(0xFFFF9800),
                                fontFamily = minecraftFont
                            ) // Font molto piccolo, testo giallo
                        )
                    }

                }
            }


        }

        // La LazyColumn con i log che può essere scrollata
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp) // Stacca la LazyColumn dai bordi
                .clip(RoundedCornerShape(16.dp)) // Angoli arrotondati
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x55AAEA00),
                            Color(0xFF464545)
                        )
                    )
                )
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
                            .align(Alignment.CenterVertically),
                        style = TextStyle(
                            color = Color.Yellow,
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
}

val minecraftFont = FontFamily(
    Font(R.font.minecraft)
)

val kaushanScriptFont = FontFamily(
    Font(R.font.kaushanscript_regular)
)

val bankprinterFont = FontFamily(
    Font(R.font.bankprinter)
)

val alata = FontFamily(
    Font(R.font.alata)
)