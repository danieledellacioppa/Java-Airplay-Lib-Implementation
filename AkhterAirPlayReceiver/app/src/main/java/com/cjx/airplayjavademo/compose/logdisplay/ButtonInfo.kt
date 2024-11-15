package com.cjx.airplayjavademo.compose.logdisplay

data class ButtonInfo(
    val id: String,
    val label: String,
    val action: () -> Unit
)

