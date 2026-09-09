package com.example.jarvis

enum class JarvisAgentType(
    val code: String,
    val title: String,
    val subtitle: String,
    val iconName: String
) {
    JARVIS_CORE("CORE", "JARVIS Core", "Master Intelligence & Coordinator", "🧠"),
    CODEX_ARCHITECT("ARCHITECT", "Codex Architect", "Automated Script & Payload Builder", "🛠️"),
    BUG_HUNTER("BUG_HUNTER", "Bug Hunter & Logcat", "Root-Cause Crash & Error Doctor", "🐞"),
    FILE_INSPECTOR("FILE_INSPECTOR", "Document & Code Reader", "Static Analysis & Project Auditor", "📄"),
    SYSTEM_AUDITOR("SYSTEM_AUDITOR", "Device & App Auditor", "Telemetry, RAM, Storage & Health", "📱")
}

data class JarvisChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val senderType: JarvisAgentType,
    val message: String,
    val isUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val codeSnippet: String? = null,
    val codeLanguage: String? = null,
    val runCommand: String? = null
)

data class BugDiagnosis(
    val title: String,
    val severity: BugSeverity,
    val rootCause: String,
    val affectedComponent: String,
    val solutionExplanation: String,
    val fixScript: String,
    val runCommand: String,
    val language: String = "bash"
)

enum class BugSeverity(val label: String, val colorHex: Long) {
    CRITICAL("CRITICAL CRASH", 0xFFFF3B30),
    HIGH("HIGH SEVERITY", 0xFFFF9500),
    MEDIUM("WARNING", 0xFFFFCC00),
    INFO("INFORMATIONAL", 0xFF30D158)
}

data class FileInspectionResult(
    val fileName: String,
    val fileSize: Long,
    val lineCount: Int,
    val fileType: String,
    val summary: String,
    val securityAudit: String,
    val detectedBugs: List<String>,
    val optimizationTips: List<String>,
    val refactoredCode: String? = null
)

data class DeviceAuditReport(
    val androidVersion: String,
    val sdkInt: Int,
    val deviceModel: String,
    val cpuArch: String,
    val totalRamMb: Long,
    val availRamMb: Long,
    val storageFreeGb: Double,
    val storageTotalGb: Double,
    val termuxInstalled: Boolean,
    val termuxApiInstalled: Boolean,
    val kaliNetHunterInstalled: Boolean,
    val healthScore: Int,
    val insights: List<String>,
    val recommendations: List<String>
)
