package com.example.agent.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "server_daemons")
data class ServerDaemon(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val command: String,
    val port: Int,
    val pid: Long = 0,
    val isRunning: Boolean = false,
    val startedAt: Long = 0L,
    val logSnippet: String = ""
)
