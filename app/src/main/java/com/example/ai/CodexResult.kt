package com.example.ai

data class CodexResult(
    val title: String,
    val targetEnv: String, // "Termux", "Kali Linux", "Android Shell"
    val language: String,  // "bash", "python", "sh"
    val code: String,
    val runCommand: String,
    val summary: String,
    val requiredPackages: String = "",
    val executionInstructions: String = ""
)
