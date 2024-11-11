package com.cjx.airplayjavademo.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

@Composable
fun LogColumn(
    listState: LazyListState,
    logMessages: List<LogEntry>
) {

    // Aggiungi un LaunchedEffect che si attiva ogni volta che la dimensione di logMessages cambia
    // per scorrere automaticamente fino all'ultimo elemento

    LaunchedEffect(logMessages.size) {
        if (logMessages.isNotEmpty()) {
            listState.animateScrollToItem(logMessages.size - 1)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp) // Stacca la LazyColumn dai bordi
            .clip(RoundedCornerShape(6.dp)) // Angoli arrotondati
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x11DADADA),
                        Color.Transparent
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
//                        .width(50.dp) // Fissare la larghezza per la pallina
//                        .height(25.dp) // Fissare l'altezza per la pallina
                        .align(Alignment.CenterVertically)
                        .padding(start = 10.dp)
                ) {
                    Text(
                        text = logMessages[index].time, // Ora del log
                        style = TextStyle(
                            color = Color(0xFFFF6600), // Testo arancione
                            fontSize = 4.sp,
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
                        fontSize = 4.sp,
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
                        ),
                    style = TextStyle(
                        color = logMessages[index].type.color, // Colore del testo
                        fontSize = 4.sp,
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