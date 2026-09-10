package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.executor.CommandResult
import com.example.executor.ExecutionBackend

/**
 * Room database entity for logging the output history of executed tasks/commands,
 * including execution status, timestamp, duration, backend, and detailed output.
 */
@Entity(tableName = "command_results")
data class CommandResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val command: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long,
    val executedVia: String, // e.g. "LOCAL_PROCESS", "TERMUX_INTENT", "SIMULATED_SAFE"
    val isSuccess: Boolean,
    val status: String,      // "SUCCESS", "FAILED", "TIMEOUT", "TERMINATED"
    val workingDir: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromCommandResult(
            result: CommandResult,
            workingDir: String = "",
            customStatus: String? = null
        ): CommandResultEntity {
            val status = customStatus ?: if (result.isSuccess) "SUCCESS" else "FAILED"
            return CommandResultEntity(
                command = result.command,
                exitCode = result.exitCode,
                stdout = result.stdout,
                stderr = result.stderr,
                durationMs = result.durationMs,
                executedVia = result.executedVia.name,
                isSuccess = result.isSuccess,
                status = status,
                workingDir = workingDir,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    fun toCommandResult(): CommandResult {
        val backend = try {
            ExecutionBackend.valueOf(executedVia)
        } catch (_: Exception) {
            ExecutionBackend.LOCAL_PROCESS
        }
        return CommandResult(
            command = command,
            exitCode = exitCode,
            stdout = stdout,
            stderr = stderr,
            durationMs = durationMs,
            executedVia = backend
        )
    }
}
