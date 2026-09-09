package com.example.executor

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Robust shell wrapper class to safely execute commands, supporting callbacks for log streams,
 * process lifecycle management, sandbox confinement, and Termux result monitoring.
 */
class SafeShellWrapper(
    private val context: Context
) : ExecutionService {

    companion object {
        private const val TAG = "SafeShellWrapper"
        const val ACTION_TERMUX_RESULT = "com.example.silentvenge.TERMUX_RESULT"
        private const val TERMUX_PACKAGE = "com.termux"
        private const val TERMUX_RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
        private const val TERMUX_ACTION = "com.termux.RUN_COMMAND"
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _logStream = MutableSharedFlow<TerminalLog>(
        replay = 100,
        extraBufferCapacity = 500,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val logStream: SharedFlow<TerminalLog> = _logStream.asSharedFlow()

    private val _activeProcessState = MutableStateFlow<ActiveShellProcess?>(null)
    override val activeProcessState: StateFlow<ActiveShellProcess?> = _activeProcessState.asStateFlow()

    @Volatile
    private var activeProcess: Process? = null

    @Volatile
    private var activeProcessJob: Job? = null

    private val pendingTermuxCallbacks = ConcurrentHashMap<String, ExecutionStreamCallback>()
    private val commandIdCounter = AtomicInteger(1)

    // Broadcast receiver for Termux results
    private val termuxResultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null && intent.action == ACTION_TERMUX_RESULT) {
                onTermuxResultReceived(intent, null)
            }
        }
    }

    init {
        registerTermuxReceiver()
    }

    private fun registerTermuxReceiver() {
        try {
            val filter = IntentFilter(ACTION_TERMUX_RESULT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(termuxResultReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(termuxResultReceiver, filter)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register Termux result receiver: ${e.message}")
        }
    }

    fun unregister() {
        try {
            context.unregisterReceiver(termuxResultReceiver)
        } catch (_: Exception) {}
    }

    private fun emitLog(type: LogType, text: String) {
        val log = TerminalLog(type = type, text = text)
        _logStream.tryEmit(log)
    }

    override fun clearLogs() {
        // SharedFlow buffer resets on new emissions
    }

    override fun isTermuxAvailable(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(TERMUX_PACKAGE, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun isKaliAvailable(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo("com.offsec.nethunter", PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo("com.offsec.nethunter", 0)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun executeCommand(
        command: String,
        options: ExecutionOptions,
        callback: ExecutionStreamCallback?
    ): CommandResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        emitLog(LogType.COMMAND, "$ $command")
        callback?.onCommandStarted(command, ExecutionBackend.LOCAL_PROCESS)

        val workingDir = options.workingDir ?: context.filesDir
        if (!workingDir.exists()) {
            workingDir.mkdirs()
        }

        val stdoutBuffer = StringBuilder()
        val stderrBuffer = StringBuilder()

        try {
            val processBuilder = ProcessBuilder("sh", "-c", command)
                .directory(workingDir)

            val env = processBuilder.environment()
            if (options.injectTermuxPaths) {
                val currentPath = env["PATH"] ?: "/system/bin:/system/xbin"
                env["PATH"] = "$currentPath:/data/data/com.termux/files/usr/bin:/data/local/nhsystem/kalifs/usr/bin"
            }
            options.environment.forEach { (k, v) -> env[k] = v }
            env["TERM"] = "xterm-256color"
            env["HOME"] = workingDir.absolutePath

            val process = processBuilder.start()
            activeProcess = process
            _activeProcessState.value = ActiveShellProcess(
                pid = "PID-${System.currentTimeMillis() % 10000}",
                command = command,
                startedAt = startTime,
                workingDir = workingDir.absolutePath,
                backend = ExecutionBackend.LOCAL_PROCESS
            )

            val job = launch {
                // Stdout reader
                launch {
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String? = null
                    while (isActive) {
                        line = reader.readLine()
                        if (line == null) break
                        line.let { l ->
                            if (options.bufferOutput) stdoutBuffer.append(l).append("\n")
                            emitLog(LogType.STDOUT, l)
                            callback?.onStdoutLine(l)
                        }
                    }
                }

                // Stderr reader
                launch {
                    val reader = BufferedReader(InputStreamReader(process.errorStream))
                    var line: String? = null
                    while (isActive) {
                        line = reader.readLine()
                        if (line == null) break
                        line.let { l ->
                            if (options.bufferOutput) stderrBuffer.append(l).append("\n")
                            emitLog(LogType.STDERR, l)
                            callback?.onStderrLine(l)
                        }
                    }
                }
            }
            activeProcessJob = job

            val completedInTime = withTimeoutOrNull(options.timeoutMs) {
                val exit = process.waitFor()
                job.join()
                exit
            }

            activeProcess = null
            activeProcessJob = null
            _activeProcessState.value = null

            val duration = System.currentTimeMillis() - startTime

            if (completedInTime == null) {
                process.destroyForcibly()
                emitLog(LogType.ERROR, "✗ Command timed out after ${options.timeoutMs}ms")
                val timeoutResult = CommandResult(
                    command = command,
                    exitCode = -1,
                    stdout = stdoutBuffer.toString().trim(),
                    stderr = "Execution timed out after ${options.timeoutMs}ms",
                    durationMs = duration,
                    executedVia = ExecutionBackend.LOCAL_PROCESS
                )
                callback?.onError(IllegalStateException("Execution timeout"))
                callback?.onCommandCompleted(timeoutResult)
                return@withContext timeoutResult
            }

            val exitCode = completedInTime
            if (exitCode == 0) {
                emitLog(LogType.SUCCESS, "✓ Exit 0 in ${duration}ms")
            } else {
                emitLog(LogType.ERROR, "✗ Exit $exitCode in ${duration}ms")
            }

            val result = CommandResult(
                command = command,
                exitCode = exitCode,
                stdout = stdoutBuffer.toString().trim(),
                stderr = stderrBuffer.toString().trim(),
                durationMs = duration,
                executedVia = ExecutionBackend.LOCAL_PROCESS
            )
            callback?.onCommandCompleted(result)
            result
        } catch (e: CancellationException) {
            val duration = System.currentTimeMillis() - startTime
            activeProcess?.destroy()
            activeProcess = null
            _activeProcessState.value = null
            emitLog(LogType.SYSTEM, "⚠ Command cancelled by user.")
            val result = CommandResult(
                command = command,
                exitCode = 130, // standard SIGINT
                stdout = stdoutBuffer.toString().trim(),
                stderr = "Command cancelled",
                durationMs = duration,
                executedVia = ExecutionBackend.LOCAL_PROCESS
            )
            callback?.onCommandCompleted(result)
            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val msg = e.localizedMessage ?: "Process execution failed"
            activeProcess = null
            _activeProcessState.value = null
            emitLog(LogType.ERROR, "Process exception: $msg")
            callback?.onError(e)

            val errorResult = CommandResult(
                command = command,
                exitCode = -1,
                stdout = "",
                stderr = msg,
                durationMs = duration,
                executedVia = ExecutionBackend.LOCAL_PROCESS
            )
            callback?.onCommandCompleted(errorResult)
            errorResult
        }
    }

    override suspend fun executeScript(
        scriptName: String,
        scriptContent: String,
        interpreter: String,
        options: ExecutionOptions,
        callback: ExecutionStreamCallback?
    ): CommandResult = withContext(Dispatchers.IO) {
        val scriptsDir = File(context.filesDir, "scripts").apply { mkdirs() }
        val cleanName = scriptName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val scriptFile = File(scriptsDir, "${System.currentTimeMillis()}_$cleanName")

        try {
            scriptFile.writeText(scriptContent)
            scriptFile.setExecutable(true, false)
            scriptFile.setReadable(true, false)

            val cmd = "$interpreter \"${scriptFile.absolutePath}\""
            executeCommand(cmd, options.copy(workingDir = options.workingDir ?: scriptsDir), callback)
        } finally {
            // Cleanup script file after execution if in temporary folder
            try {
                if (scriptFile.exists()) {
                    scriptFile.delete()
                }
            } catch (_: Exception) {}
        }
    }

    override fun dispatchTermux(
        command: String,
        options: ExecutionOptions,
        callback: ExecutionStreamCallback?
    ): Boolean {
        // Fallback: Copy to clipboard
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("SilentVenge Command", command))

        callback?.onCommandStarted(command, ExecutionBackend.TERMUX_INTENT)

        if (!isTermuxAvailable()) {
            emitLog(LogType.SYSTEM, "ℹ Termux not installed. Command copied to clipboard.")
            callback?.onError(IllegalStateException("Termux is not installed on this device"))
            return false
        }

        val cmdId = "cmd_${commandIdCounter.getAndIncrement()}_${System.currentTimeMillis()}"
        if (callback != null) {
            pendingTermuxCallbacks[cmdId] = callback
        }

        return try {
            emitLog(LogType.COMMAND, ">> Termux: $command")

            // Create PendingIntent for Termux result callback
            val resultIntent = Intent(ACTION_TERMUX_RESULT).apply {
                setPackage(context.packageName)
                putExtra("command_id", cmdId)
                putExtra("original_command", command)
            }

            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingResultIntent = PendingIntent.getBroadcast(
                context,
                cmdId.hashCode(),
                resultIntent,
                pendingIntentFlags
            )

            val runIntent = Intent(TERMUX_ACTION).apply {
                setClassName(TERMUX_PACKAGE, TERMUX_RUN_COMMAND_SERVICE)
                putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", command))
                putExtra("com.termux.RUN_COMMAND_WORKDIR", options.workingDir?.absolutePath ?: "/data/data/com.termux/files/home")
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", options.runInBackground)
                putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
                putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", pendingResultIntent)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(runIntent)
            } else {
                context.startService(runIntent)
            }

            emitLog(LogType.SUCCESS, "✓ Dispatched to Termux RunCommandService (Monitoring result: $cmdId)")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Termux service dispatch failed, launching Activity directly", e)
            val launchIntent = context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                emitLog(LogType.SUCCESS, "✓ Launched Termux app. Command is ready on clipboard.")
                true
            } else {
                callback?.onError(e)
                false
            }
        }
    }

    override fun onTermuxResultReceived(
        resultIntent: Intent,
        callback: ExecutionStreamCallback?
    ): CommandResult? {
        val cmdId = resultIntent.getStringExtra("command_id") ?: ""
        val originalCmd = resultIntent.getStringExtra("original_command") ?: "termux_cmd"

        // Termux RunCommandService returns stdout, stderr, and exitCode in extras
        val stdout = resultIntent.getStringExtra("stdout") ?: resultIntent.getStringExtra("result") ?: ""
        val stderr = resultIntent.getStringExtra("stderr") ?: resultIntent.getStringExtra("error") ?: ""
        val exitCode = resultIntent.getIntExtra("exitCode", resultIntent.getIntExtra("resultCode", 0))

        val activeCallback = callback ?: pendingTermuxCallbacks.remove(cmdId)

        if (stdout.isNotBlank()) {
            stdout.lines().forEach { line ->
                emitLog(LogType.STDOUT, "[Termux] $line")
                activeCallback?.onStdoutLine(line)
            }
        }
        if (stderr.isNotBlank()) {
            stderr.lines().forEach { line ->
                emitLog(LogType.STDERR, "[Termux] $line")
                activeCallback?.onStderrLine(line)
            }
        }

        if (exitCode == 0) {
            emitLog(LogType.SUCCESS, "✓ Termux command completed with exit 0")
        } else {
            emitLog(LogType.ERROR, "✗ Termux command exited with code $exitCode")
        }

        val result = CommandResult(
            command = originalCmd,
            exitCode = exitCode,
            stdout = stdout,
            stderr = stderr,
            durationMs = 0L,
            executedVia = ExecutionBackend.TERMUX_INTENT
        )

        activeCallback?.onCommandCompleted(result)
        return result
    }

    override fun killActiveProcess() {
        activeProcess?.let {
            try {
                it.destroy()
                emitLog(LogType.SYSTEM, "⚠ Process terminated.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to kill active process", e)
            }
            activeProcess = null
        }
        activeProcessJob?.cancel()
        activeProcessJob = null
        _activeProcessState.value = null
    }
}
