package com.akhter.airplaytestlab.tools

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class MessageType(val type: String, val color: Color)

data class LogEntry(
    val time: String, // Ora in cui il log è stato aggiunto
    val tag: String,  // Tag del log
    val message: String, // Messaggio del log
    val type: MessageType = MessageType("default", Color.Black) // Tipo di messaggio (INFO, WARNING, ERROR)
)


object LogRepository {

    // Lista di log osservabile dai Composable
    private val logMessages: SnapshotStateList<LogEntry> = mutableStateListOf()

    var isConnectionActive by mutableStateOf(false)

    fun setConnection(active: Boolean) {
        isConnectionActive = active
    }

    // Aggiunge un log con orario
    fun addLog(tag: String, message: String, type: Char = 'I') {
        val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val color = when (type) {
            'I' -> Color.White
            'W' -> Color.Yellow
            'E' -> Color.Red
            else -> Color.Black
        }
        logMessages.add(LogEntry(currentTime, tag, message, MessageType(type.toString(), color)))

        when (type) {
            'I' -> Log.i(tag, message)
            'W' -> Log.w(tag, message)
            'E' -> Log.e(tag, message)
            else -> Log.d(tag, message)
        }
    }

    // Restituisce la lista di log
    fun getLogs(): List<LogEntry> {
        return logMessages
    }
}