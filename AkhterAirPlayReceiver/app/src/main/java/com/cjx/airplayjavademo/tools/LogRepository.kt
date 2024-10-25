package com.cjx.airplayjavademo.tools

import android.media.MediaDrm.LogMessage
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.time.LocalTime
import java.time.format.DateTimeFormatter


data class LogEntry(
    val time: String, // Ora in cui il log è stato aggiunto
    val tag: String,  // Tag del log
    val message: String // Messaggio del log
)

object LogRepository {
    // Lista di log osservabile dai Composable
    private val logMessages: SnapshotStateList<LogEntry> = mutableStateListOf()

    var isConnectionActive by mutableStateOf(false)

    fun setConnection(active: Boolean) {
        isConnectionActive = active
    }

    // Aggiunge un log con orario
    fun addLog(tag: String, message: String) {
        val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        logMessages.add(LogEntry(currentTime, tag, message))
    }

    // Restituisce la lista di log
    fun getLogs(): List<LogEntry> {
        return logMessages
    }
}