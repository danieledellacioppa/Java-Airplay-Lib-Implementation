package com.cjx.airplayjavademo.compose

import android.view.SurfaceView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import android.view.SurfaceHolder

@Composable
fun VideoDisplayComposable(callback: SurfaceHolder.Callback) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                SurfaceView(context).apply {
                    holder.addCallback(callback) // Usa il callback passato come parametro
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}