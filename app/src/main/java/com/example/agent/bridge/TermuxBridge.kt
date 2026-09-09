package com.example.agent.bridge

import android.content.Context
import android.content.Intent
import com.example.executor.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * High-level Termux and local execution bridge utilizing the SafeShellWrapper and ExecutionService.
 * Supports callbacks for log streams, live output, and Termux result monitoring.
 */
class TermuxBridge(
    private val context: Context,
    val shellWrapper: SafeShellWrapper = SafeShellWrapper(context)
) {

    private val _logs = MutableStateFlow<List<TerminalLog>>(emptyList())
    val logs: StateFlow<List<TerminalLog>> = _logs.asStateFlow()

    fun addLog(type: LogType, text: String) {
        val newLog = TerminalLog(type = type, text = text)
        _logs.value = _logs.value + newLog
    }

    fun clearLogs() {
        _logs.value = emptyList()
        shellWrapper.clearLogs()
    }

    fun isTermuxInstalled(): Boolean = shellWrapper.isTermuxAvailable()

    fun isKaliNetHunterInstalled(): Boolean = shellWrapper.isKaliAvailable()

    /**
     * Executes a shell command locally with callback for line-by-line streaming.
     */
    suspend fun executeLocal(
        command: String,
        workDir: File = context.filesDir,
        onLine: ((String) -> Unit)? = null
    ): ExecutionOutput {
        val callback = object : ExecutionStreamCallback {
            override fun onStdoutLine(line: String) {
                addLog(LogType.STDOUT, line)
                onLine?.invoke(line)
            }

            override fun onStderrLine(line: String) {
                addLog(LogType.STDERR, line)
                onLine?.invoke(line)
            }
        }

        addLog(LogType.COMMAND, "$ $command")

        val result = shellWrapper.executeCommand(
            command = command,
            options = ExecutionOptions(workingDir = workDir),
            callback = callback
        )

        if (result.isSuccess) {
            addLog(LogType.SUCCESS, "✓ Exit 0 in ${result.durationMs}ms")
        } else {
            addLog(LogType.ERROR, "✗ Exit ${result.exitCode} in ${result.durationMs}ms")
        }

        return result
    }

    /**
     * Dispatches command to Termux via RUN_COMMAND intent protocol with result callback monitoring.
     */
    fun dispatchTermuxIntent(
        command: String,
        inBackground: Boolean = false,
        onResult: ((CommandResult) -> Unit)? = null
    ): Boolean {
        val callback = object : ExecutionStreamCallback {
            override fun onStdoutLine(line: String) {
                addLog(LogType.STDOUT, "[Termux] $line")
            }

            override fun onStderrLine(line: String) {
                addLog(LogType.STDERR, "[Termux] $line")
            }

            override fun onCommandCompleted(result: CommandResult) {
                if (result.isSuccess) {
                    addLog(LogType.SUCCESS, "✓ Termux command finished: ${result.command}")
                } else {
                    addLog(LogType.ERROR, "✗ Termux command failed (${result.exitCode}): ${result.stderr}")
                }
                onResult?.invoke(result)
            }

            override fun onError(throwable: Throwable) {
                addLog(LogType.ERROR, "Termux dispatch error: ${throwable.message}")
            }
        }

        val dispatched = shellWrapper.dispatchTermux(
            command = command,
            options = ExecutionOptions(runInBackground = inBackground),
            callback = callback
        )

        if (dispatched) {
            addLog(LogType.SUCCESS, "✓ Dispatched to Termux (Result Monitoring Active)")
        } else {
            addLog(LogType.SYSTEM, "ℹ Copied to clipboard (Termux app fallback).")
        }

        return dispatched
    }

    fun onTermuxResultIntent(intent: Intent): CommandResult? {
        return shellWrapper.onTermuxResultReceived(intent)
    }

    fun killCurrentProcess() {
        shellWrapper.killActiveProcess()
        addLog(LogType.SYSTEM, "⚠ Process terminated.")
    }
}
