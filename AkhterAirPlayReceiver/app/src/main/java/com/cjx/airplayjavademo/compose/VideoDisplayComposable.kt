package com.cjx.airplayjavademo.compose

import android.view.SurfaceView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import android.view.SurfaceHolder
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cjx.airplayjavademo.tools.LogRepository

@Composable
fun VideoDisplayComposable(
    callback: SurfaceHolder.Callback,
    isConnectionActive: Boolean
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
            LogDisplayComposable()
        }
    }
}

@Composable
fun LogDisplayComposable() {
    val logMessages = remember { LogRepository.getLogs() }

    // Layout principale che mostra il logo fisso in alto e la LazyColumn che scorre
    Column(modifier = Modifier.fillMaxSize()) {

        // Mostra il logo ASCII sempre in testa
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = logoAscii,
                style = TextStyle(fontSize = 6.sp, color = Color.Gray) // Font molto piccolo
            )
        }

        // La LazyColumn con i log che può essere scrollata
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(logMessages.size) { index ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mostra i log con uno stile normale
                    Text(
                        text = logMessages[index],
                        style = TextStyle(fontSize = 16.sp) // Font normale per i log
                    )
                }
            }
        }
    }
}