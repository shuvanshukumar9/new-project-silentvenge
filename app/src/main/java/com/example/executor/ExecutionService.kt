package com.example.executor

import android.content.Intent
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Result of command execution.
 */
data class CommandResult(
    val command: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long,
    val executedVia: ExecutionBackend
) {
    val isSuccess: Boolean get() = exitCode == 0
}

/**
 * Backward compatibility typealias for agent tools and pipelines.
 */
typealias ExecutionOutput = CommandResult

/**
 * Indicates which backend handled the execution.
 */
enum class ExecutionBackend {
    LOCAL_PROCESS,
    TERMUX_INTENT,
    SIMULATED_SAFE
}

/**
 * Details of an active shell process running in the pipeline.
 */
data class ActiveShellProcess(
    val pid: String,
    val command: String,
    val startedAt: Long,
    val workingDir: String,
    val backend: ExecutionBackend
)

/**
 * Callbacks for monitoring real-time command streaming and results.
 */
interface ExecutionStreamCallback {
    fun onCommandStarted(command: String, backend: ExecutionBackend) {}
    fun onStdoutLine(line: String) {}
    fun onStderrLine(line: String) {}
    fun onCommandCompleted(result: CommandResult) {}
    fun onError(throwable: Throwable) {}
}

/**
 * Options configuring command execution.
 */
data class ExecutionOptions(
    val workingDir: File? = null,
    val environment: Map<String, String> = emptyMap(),
    val timeoutMs: Long = 60_000L,
    val runInBackground: Boolean = false,
    val bufferOutput: Boolean = true,
    val injectTermuxPaths: Boolean = true
)

/**
 * Core interface for executing system commands, shell scripts, and Termux integrations.
 */
interface ExecutionService {

    /**
     * Observable stream of logs generated across execution calls.
     */
    val logStream: Flow<TerminalLog>

    /**
     * Observable state of the currently active process, or null if idle.
     */
    val activeProcessState: Flow<ActiveShellProcess?>

    /**
     * Executes a command safely with callbacks for stdout, stderr, and result status.
     */
    suspend fun executeCommand(
        command: String,
        options: ExecutionOptions = ExecutionOptions(),
        callback: ExecutionStreamCallback? = null
    ): CommandResult

    /**
     * Executes a multi-line script by writing it safely to a temporary executable file.
     */
    suspend fun executeScript(
        scriptName: String,
        scriptContent: String,
        interpreter: String = "sh",
        options: ExecutionOptions = ExecutionOptions(),
        callback: ExecutionStreamCallback? = null
    ): CommandResult

    /**
     * Dispatches a command to Termux via RUN_COMMAND intent protocol, supporting
     * result broadcast monitoring.
     */
    fun dispatchTermux(
        command: String,
        options: ExecutionOptions = ExecutionOptions(),
        callback: ExecutionStreamCallback? = null
    ): Boolean

    /**
     * Handles result intents returned by Termux RunCommandService or broadcast receivers.
     */
    fun onTermuxResultReceived(resultIntent: Intent, callback: ExecutionStreamCallback? = null): CommandResult?

    /**
     * Checks if Termux is installed on the host device.
     */
    fun isTermuxAvailable(): Boolean

    /**
     * Checks if Kali NetHunter is installed.
     */
    fun isKaliAvailable(): Boolean

    /**
     * Terminates any currently running active local process.
     */
    fun killActiveProcess()

    /**
     * Clears all stored execution logs.
     */
    fun clearLogs()
}
