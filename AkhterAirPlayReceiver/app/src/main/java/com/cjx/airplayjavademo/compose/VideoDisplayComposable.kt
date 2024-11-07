package com.cjx.airplayjavademo.compose

import airplayjavademo.R
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.cjx.airplayjavademo.ui.theme.Gray40
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun VideoDisplayComposable(
    callback: SurfaceHolder.Callback,
    isConnectionActive: Boolean,
    versionName: String,
    onStartServer: () -> Unit, // Funzione per avviare il server
    onStopServer: () -> Unit, // Funzione per fermare il server
    onStopAudioPlayer: () -> Unit, // Funzione per fermare l'audio player
    onStopVideoPlayer: () -> Unit, // Funzione per fermare il video player
    showLog: Boolean, // Nuovo parametro per la visibilità del log
    toggleLogVisibility: () -> Unit // Funzione per alternare la visibilità
)  {
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color(Gray40.toArgb())) // Sfondo grigio
    ) {
        if (isConnectionActive) {
            // Controlliamo se il SurfaceView è già attivo prima di inizializzarlo nuovamente
            AndroidView(
                factory = { context ->
                    SurfaceView(context).apply {
                        holder.addCallback(callback)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(2f / 3f, true)
                    .align(Alignment.Center)
                ,
                update = { view ->
                    // Logica di aggiornamento del SurfaceView, se necessario
                }
            )

            // Stato per gestire l'ora attuale
            var currentTime by remember { mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))) }

            // Aggiorna l'ora ogni secondo
            LaunchedEffect(Unit) {
                while (true) {
                    currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                    delay(1000L) // Aggiorna ogni secondo
                }
            }

            // Aggiungiamo un piccolo composable per mostrare il tempo attuale
            Text(
                text = currentTime,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                style = TextStyle(
                    color = Color.White,
                    fontSize = 16.sp
                )
            )
        } else {
            LogDisplayComposable(versionName, onStartServer, onStopServer, onStopAudioPlayer, onStopVideoPlayer, showLog, toggleLogVisibility)
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