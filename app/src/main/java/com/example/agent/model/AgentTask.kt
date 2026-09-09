package com.example.agent.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskState(val label: String, val stepIndex: Int, val icon: String) {
    IDLE("Ready", 0, "⏸️"),
    QUEUED("Queued in Pipeline", 0, "⏳"),
    INSPECTING("Inspecting Project", 1, "🔍"),
    PLANNING("Generating Plan", 2, "📋"),
    CODING("Synthesizing Code", 3, "💻"),
    SAVING("Writing to Disk", 4, "💾"),
    BUILDING("Building Project", 5, "🔨"),
    TESTING("Running Tests", 6, "🧪"),
    ANALYZING_ERRORS("Analyzing Failures", 7, "🐞"),
    PATCHING("Applying Patch", 8, "🩹"),
    COMPLETED("Completed", 9, "✅"),
    FAILED("Failed", 9, "❌"),
    PAUSED_NEEDS_CONFIRMATION("Awaiting Approval", 0, "⚠️")
}

enum class PermissionLevel {
    SAFE,    // Deterministic read-only, tests, status check: auto-allowed
    PROMPT,  // Destructive file ops, shell execution, root, network listen: requires user approval
    DENY     // Forbidden system paths, dangerous wildcard deletes
}

data class SecurityActionRequest(
    val id: String = System.currentTimeMillis().toString(),
    val actionType: String,      // e.g., "DELETE_FILE", "EXECUTE_SHELL", "KILL_PROCESS", "OVERWRITE_CODE"
    val commandOrTarget: String, // e.g., "rm -rf build/", "/data/data/com.termux/..."
    val reason: String,
    val level: PermissionLevel,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "agent_tasks")
data class AgentTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goal: String,
    val projectPath: String,
    val state: String = TaskState.IDLE.name,
    val currentStepIndex: Int = 0,
    val iterationCount: Int = 0,
    val maxIterations: Int = 3,
    val planJson: String = "",
    val generatedCode: String = "",
    val lastOutput: String = "",
    val errorSummary: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "task_steps")
data class TaskStep(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val stepIndex: Int,
    val stepName: String,
    val status: String, // "SUCCESS", "RUNNING", "FAILED", "SKIPPED"
    val toolUsed: String,
    val details: String,
    val exitCode: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)
