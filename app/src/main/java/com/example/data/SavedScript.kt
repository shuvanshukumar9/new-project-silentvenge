package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_scripts")
data class SavedScript(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val targetEnv: String, // "Termux", "Kali Linux", "Android Shell"
    val language: String,  // "bash", "python", "sh"
    val promptQuery: String,
    val scriptContent: String,
    val runCommand: String,
    val explanation: String,
    val requiredPackages: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val lastRunTimestamp: Long = 0L,
    val lastExitCode: Int? = null,
    val isFavorite: Boolean = false
)
