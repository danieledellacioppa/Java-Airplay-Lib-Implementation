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


    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(logMessages.size) { index ->
            BasicText(text = logMessages[index])
        }
    }
}