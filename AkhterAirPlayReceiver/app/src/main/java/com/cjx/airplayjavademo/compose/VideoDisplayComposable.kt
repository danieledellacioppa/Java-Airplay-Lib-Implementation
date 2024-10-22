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
            // Mostra l'AndroidView (SurfaceView) quando la connessione è attiva
            AndroidView(
                factory = { context ->
                    SurfaceView(context).apply {
                        holder.addCallback(callback)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Mostra i log quando la connessione non è attiva
            LogDisplayComposable()
        }
    }
}

@Composable
fun LogDisplayComposable() {
    // Usa remember e derive lo stato dai log del LogRepository
    val logMessages by remember {
        derivedStateOf { LogRepository.logs }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(logMessages.size) { index ->
            BasicText(text = logMessages[index])
        }
    }
}