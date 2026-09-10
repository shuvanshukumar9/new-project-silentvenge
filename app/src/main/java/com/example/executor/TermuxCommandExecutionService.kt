package com.example.executor

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.CommandResultDao
import com.example.data.CommandResultEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Internal backend service that executes shell commands in Termux, Kali NetHunter,
 * or local POSIX sandbox using ProcessBuilder, capturing stdout/stderr and
 * automatically persisting execution output history in the local Room database.
 *
 * This service is strictly an internal engine for AIOrchestrator and background workers,
 * completely decoupled from the primary chat UI navigation.
 */
internal class TermuxCommandExecutionService(
    private val context: Context,
    private val commandResultDao: CommandResultDao = AppDatabase.getDatabase(context).commandResultDao(),
    private val defaultScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    companion object {
        @Volatile
        private var INSTANCE: TermuxCommandExecutionService? = null

        /**
         * Returns or creates a singleton instance of the internal ProcessBuilder service.
         */
        fun getInstance(context: Context): TermuxCommandExecutionService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TermuxCommandExecutionService(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    @Volatile
    private var activeProcess: Process? = null

    /**
     * Kills any currently running ProcessBuilder shell process.
     */
    fun killActiveProcess(): Boolean {
        val process = activeProcess ?: return false
        return try {
            process.destroyForcibly()
            activeProcess = null
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Observable stream of all logged command execution outputs from Room.
     */
    val commandHistory: Flow<List<CommandResultEntity>> = commandResultDao.getAllCommandResults()

    /**
     * Observable stream of recent command execution outputs from Room.
     */
    fun getRecentHistory(limit: Int = 50): Flow<List<CommandResultEntity>> =
        commandResultDao.getRecentCommandResults(limit)

    /**
     * Executes a shell command using ProcessBuilder with configured Termux and Kali NetHunter
     * environment paths, captures stdout/stderr, and persists the CommandResult in Room database.
     *
     * @param command The shell command string to execute.
     * @param workingDir Optional working directory (defaults to app files directory).
     * @param timeoutMs Timeout for command execution in milliseconds.
     * @param environment Additional environment variables.
     * @param saveToDatabase Whether to automatically persist the result in Room database.
     * @param onStdoutLine Optional streaming callback for each line of stdout.
     * @param onStderrLine Optional streaming callback for each line of stderr.
     * @return The resulting CommandResult containing exitCode, stdout, stderr, durationMs, and backend.
     */
    suspend fun executeAndLog(
        command: String,
        workingDir: File? = null,
        timeoutMs: Long = 60_000L,
        environment: Map<String, String> = emptyMap(),
        saveToDatabase: Boolean = true,
        onStdoutLine: ((String) -> Unit)? = null,
        onStderrLine: ((String) -> Unit)? = null
    ): CommandResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val dir = workingDir ?: context.filesDir
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val stdoutBuffer = StringBuilder()
        val stderrBuffer = StringBuilder()
        var exitCode = -1

        try {
            val processBuilder = ProcessBuilder("sh", "-c", command)
                .directory(dir)

            val env = processBuilder.environment()
            val existingPath = env["PATH"] ?: "/system/bin:/system/xbin"
            // Inject Termux and Kali NetHunter binary paths into PATH
            env["PATH"] = "$existingPath:/data/data/com.termux/files/usr/bin:/data/local/nhsystem/kalifs/usr/bin"
            env["TERM"] = "xterm-256color"
            env["HOME"] = dir.absolutePath
            environment.forEach { (k, v) -> env[k] = v }

            val process = processBuilder.start()
            activeProcess = process

            val stdoutJob = defaultScope.launch(Dispatchers.IO) {
                try {
                    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            line?.let { l ->
                                stdoutBuffer.append(l).append("\n")
                                onStdoutLine?.invoke(l)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            val stderrJob = defaultScope.launch(Dispatchers.IO) {
                try {
                    BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            line?.let { l ->
                                stderrBuffer.append(l).append("\n")
                                onStderrLine?.invoke(l)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // Wait for completion or timeout
            val waitResult = waitForProcessWithTimeout(process, timeoutMs)
            exitCode = if (waitResult) {
                process.exitValue()
            } else {
                process.destroyForcibly()
                -99 // Timeout code
            }

            stdoutJob.join()
            stderrJob.join()

        } catch (e: Exception) {
            stderrBuffer.append("Execution error: ").append(e.localizedMessage ?: e.message).append("\n")
            exitCode = 1
        } finally {
            activeProcess = null
        }

        val duration = System.currentTimeMillis() - startTime
        val stdout = stdoutBuffer.toString().trimEnd()
        val stderr = stderrBuffer.toString().trimEnd()

        val result = CommandResult(
            command = command,
            exitCode = exitCode,
            stdout = stdout,
            stderr = stderr,
            durationMs = duration,
            executedVia = ExecutionBackend.LOCAL_PROCESS
        )

        if (saveToDatabase) {
            val status = when {
                exitCode == 0 -> "SUCCESS"
                exitCode == -99 -> "TIMEOUT"
                else -> "FAILED"
            }
            val entity = CommandResultEntity.fromCommandResult(
                result = result,
                workingDir = dir.absolutePath,
                customStatus = status
            )
            commandResultDao.insertCommandResult(entity)
        }

        result
    }

    /**
     * Clears all saved command output history in the Room database.
     */
    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        commandResultDao.clearAllCommandResults()
    }

    /**
     * Deletes a specific command output entry by its database ID.
     */
    suspend fun deleteHistoryEntry(id: Long) = withContext(Dispatchers.IO) {
        commandResultDao.deleteCommandResultById(id)
    }

    private fun waitForProcessWithTimeout(process: Process, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            try {
                process.exitValue()
                return true
            } catch (_: IllegalThreadStateException) {
                try {
                    Thread.sleep(50)
                } catch (_: InterruptedException) {
                    return false
                }
            }
        }
        return false
    }
}
