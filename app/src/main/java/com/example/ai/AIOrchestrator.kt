package com.example.ai

import com.example.BuildConfig
import com.example.agent.bridge.TermuxBridge
import com.example.executor.AutomationExecutor
import com.example.executor.LogType
import com.example.executor.TermuxCommandExecutionService
import com.example.jarvis.JarvisAgentType
import com.example.jarvis.JarvisBrainService
import com.example.jarvis.JarvisChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Result of the interpretation and execution performed by [AIOrchestrator].
 */
data class OrchestrationResult(
    val chatMessage: JarvisChatMessage,
    val executedCommand: String? = null,
    val commandOutput: String? = null,
    val executionDurationMs: Long = 0,
    val requiresAutoExecution: Boolean = false
)

/**
 * AIOrchestrator interprets user chat input, determines whether a shell/automation command
 * is needed, executes it via internal backend services ([TermuxCommandExecutionService], [AutomationExecutor], [TermuxBridge]),
 * and formats the output into a clean, human-readable [JarvisChatMessage].
 */
internal class AIOrchestrator(
    private val jarvisBrain: JarvisBrainService,
    private val termuxExecutionService: TermuxCommandExecutionService,
    private val executor: AutomationExecutor,
    private val termuxBridge: TermuxBridge
) {

    /**
     * Checks if a valid Gemini API key is configured either in the custom user settings or BuildConfig.
     */
    fun validateApiKey(customApiKey: String?): ApiKeyValidationResult {
        val key = if (!customApiKey.isNullOrBlank()) customApiKey.trim() else BuildConfig.GEMINI_API_KEY.trim()
        if (key.isBlank()) {
            return ApiKeyValidationResult.Missing(
                "Gemini API key is not configured. Please tap the top-right menu (⋮) -> Settings to add your Gemini API key, or define GEMINI_API_KEY in the environment."
            )
        }
        if (key == "MY_GEMINI_API_KEY" || key.length < 15) {
            return ApiKeyValidationResult.Invalid(
                "Configured Gemini API key appears invalid or is using the template placeholder. Please verify your API key in Settings (⋮ -> Settings)."
            )
        }
        return ApiKeyValidationResult.Valid(key)
    }

    sealed class ApiKeyValidationResult {
        data class Valid(val key: String) : ApiKeyValidationResult()
        data class Missing(val userMessage: String) : ApiKeyValidationResult()
        data class Invalid(val userMessage: String) : ApiKeyValidationResult()
    }

    /**
     * Determines whether user text is an explicit or implicit shell command request.
     */
    fun determineCommandIntent(input: String): String? {
        val trimmed = input.trim()
        val lower = trimmed.lowercase()

        // 1. Explicit execution prefixes
        if (trimmed.startsWith("$ ")) return trimmed.removePrefix("$ ").trim()
        if (lower.startsWith("run ")) return trimmed.substring(4).trim()
        if (lower.startsWith("exec ")) return trimmed.substring(5).trim()
        if (lower.startsWith("execute ")) return trimmed.substring(8).trim()

        // 2. Direct Unix commands (e.g. ls, pwd, ping, whoami, df, free, echo, etc.)
        val directUnixCommands = listOf(
            "ls", "pwd", "date", "whoami", "uname", "df", "free",
            "uptime", "top", "ps", "echo", "cat", "ping", "mkdir",
            "touch", "rm", "cp", "mv", "grep", "which", "id"
        )
        for (cmd in directUnixCommands) {
            if (lower == cmd || lower.startsWith("$cmd ")) {
                return trimmed
            }
        }

        // 3. Natural language intents for system/network operations
        if (lower == "show files" || lower == "list files" || lower == "view files" ||
            lower == "files dikhao" || lower == "files list karo") {
            return "ls -la"
        }
        if (lower == "where am i" || lower == "current directory" || lower == "working directory") {
            return "pwd"
        }
        if (lower == "check storage" || lower == "disk space" || lower == "storage space") {
            return "df -h"
        }
        if (lower == "check memory" || lower == "check ram" || lower == "free memory") {
            return "free -m"
        }
        if (lower.startsWith("ping ") && !lower.contains("-c")) {
            return "$trimmed -c 3"
        }

        return null
    }

    /**
     * Executes a shell command via backend execution services and formats the result into a human-readable chat response.
     */
    suspend fun executeCommandAndFormat(
        command: String,
        initialMessageId: String? = null,
        onStdoutLine: ((String) -> Unit)? = null,
        onStderrLine: ((String) -> Unit)? = null
    ): OrchestrationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val stdoutBuilder = StringBuilder()
        val stderrBuilder = StringBuilder()

        // Log to existing backend terminal & bridge
        termuxBridge.addLog(LogType.COMMAND, "$ $command")
        executor.addLog(LogType.COMMAND, "$ $command")

        try {
            val result = termuxExecutionService.executeAndLog(
                command = command,
                onStdoutLine = { line ->
                    stdoutBuilder.append(line).append("\n")
                    termuxBridge.addLog(LogType.STDOUT, line)
                    executor.addLog(LogType.STDOUT, line)
                    onStdoutLine?.invoke(line)
                },
                onStderrLine = { line ->
                    stderrBuilder.append(line).append("\n")
                    termuxBridge.addLog(LogType.STDERR, line)
                    executor.addLog(LogType.STDERR, line)
                    onStderrLine?.invoke(line)
                }
            )

            val duration = System.currentTimeMillis() - startTime
            val rawOutput = stdoutBuilder.toString().ifBlank {
                stderrBuilder.toString().ifBlank {
                    if (result.isSuccess) "Completed with exit code ${result.exitCode}"
                    else "Failed with exit code ${result.exitCode}"
                }
            }.trim()

            val readableSummary = formatExecutionSummary(
                command = command,
                isSuccess = result.isSuccess,
                exitCode = result.exitCode,
                durationMs = duration,
                output = rawOutput
            )

            val chatMsg = JarvisChatMessage(
                id = initialMessageId ?: java.util.UUID.randomUUID().toString(),
                senderType = JarvisAgentType.JARVIS_CORE,
                message = readableSummary,
                isUser = false,
                runCommand = command,
                taskStatus = if (result.isSuccess) "Completed in ${duration}ms" else "Failed (exit ${result.exitCode})",
                commandOutput = rawOutput,
                executionDurationMs = duration,
                isRunning = false,
                isError = !result.isSuccess
            )

            OrchestrationResult(
                chatMessage = chatMsg,
                executedCommand = command,
                commandOutput = rawOutput,
                executionDurationMs = duration
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val errorMsg = e.localizedMessage ?: "Unknown execution failure"
            termuxBridge.addLog(LogType.ERROR, "Command execution failed: $errorMsg")
            executor.addLog(LogType.ERROR, "Command execution failed: $errorMsg")

            val chatMsg = JarvisChatMessage(
                id = initialMessageId ?: java.util.UUID.randomUUID().toString(),
                senderType = JarvisAgentType.JARVIS_CORE,
                message = "Execution encountered an error: $errorMsg",
                isUser = false,
                runCommand = command,
                taskStatus = "Execution Error",
                commandOutput = errorMsg,
                executionDurationMs = duration,
                isRunning = false,
                isError = true
            )

            OrchestrationResult(
                chatMessage = chatMsg,
                executedCommand = command,
                commandOutput = errorMsg,
                executionDurationMs = duration
            )
        }
    }

    /**
     * Interprets incoming user chat input, routes to either shell execution, device diagnostics,
     * or AI conversational reasoning, and returns an [OrchestrationResult].
     */
    suspend fun processChatInput(
        userInput: String,
        selectedAgent: JarvisAgentType = JarvisAgentType.JARVIS_CORE,
        conversationHistory: List<JarvisChatMessage> = emptyList(),
        customApiKey: String? = null,
        autoExecute: Boolean = false,
        onCommandExecuting: ((JarvisChatMessage) -> Unit)? = null
    ): OrchestrationResult = withContext(Dispatchers.IO) {
        val trimmed = userInput.trim()
        val lower = trimmed.lowercase()

        // 1. Direct or inferred shell command intent
        val command = determineCommandIntent(trimmed)
        if (command != null) {
            val initialMsg = JarvisChatMessage(
                senderType = JarvisAgentType.JARVIS_CORE,
                message = "Executing task in background: `$command`",
                isUser = false,
                runCommand = command,
                isRunning = true,
                taskStatus = "Executing task..."
            )
            onCommandExecuting?.invoke(initialMsg)

            return@withContext executeCommandAndFormat(
                command = command,
                initialMessageId = initialMsg.id
            )
        }

        // 2. Hardware telemetry / device health audit intent
        if (lower.contains("audit") || lower.contains("telemetry") || lower.contains("device health") ||
            (lower.contains("ram") && (lower.contains("check") || lower.contains("status") || lower.contains("free")))) {
            val report = jarvisBrain.auditDeviceEnvironment()
            val auditMessage = buildString {
                append("Device Status Summary:\n\n")
                append("• Model: ${report.deviceModel}\n")
                append("• OS: Android ${report.androidVersion} (API ${report.sdkInt})\n")
                append("• RAM: ${report.availRamMb} MB available / ${report.totalRamMb} MB total\n")
                append("• Storage: ${report.storageFreeGb} GB free / ${report.storageTotalGb} GB total\n")
                append("• Health Score: ${report.healthScore} / 100\n\n")
                if (report.recommendations.isNotEmpty()) {
                    append("💡 Recommendation: ${report.recommendations.first()}")
                }
            }

            val chatMsg = JarvisChatMessage(
                senderType = JarvisAgentType.SYSTEM_AUDITOR,
                message = auditMessage,
                isUser = false,
                taskStatus = "Health Score: ${report.healthScore}/100"
            )
            return@withContext OrchestrationResult(chatMessage = chatMsg)
        }

        // 3. Logcat & bug crash diagnosis intent
        if (lower.contains("logcat") || lower.contains("diagnose") || lower.contains("crash") ||
            (lower.contains("why") && (lower.contains("error") || lower.contains("fail")))) {
            val logcat = jarvisBrain.captureDeviceLogcat(lines = 50)
            val diagnosis = jarvisBrain.diagnoseBug(
                errorLog = logcat,
                customApiKey = customApiKey
            )

            val diagMessage = buildString {
                append("Automated Diagnosis:\n\n")
                append("• Issue: ${diagnosis.title}\n")
                append("• Severity: ${diagnosis.severity.label}\n")
                append("• Cause: ${diagnosis.rootCause}\n")
                append("• Solution: ${diagnosis.solutionExplanation}")
            }

            val chatMsg = JarvisChatMessage(
                senderType = JarvisAgentType.BUG_HUNTER,
                message = diagMessage,
                isUser = false,
                codeSnippet = diagnosis.fixScript,
                codeLanguage = diagnosis.language,
                runCommand = diagnosis.runCommand,
                taskStatus = "Severity: ${diagnosis.severity.label}"
            )
            return@withContext OrchestrationResult(
                chatMessage = chatMsg,
                executedCommand = diagnosis.runCommand.takeIf { autoExecute },
                requiresAutoExecution = autoExecute && !diagnosis.runCommand.isNullOrBlank()
            )
        }

        // 4. Conversational / LLM reasoning with Gemini or offline agent
        // Validate API key state before sending network requests
        val keyValidation = validateApiKey(customApiKey)
        if (keyValidation !is ApiKeyValidationResult.Valid) {
            val warningPrompt = when (keyValidation) {
                is ApiKeyValidationResult.Missing -> keyValidation.userMessage
                is ApiKeyValidationResult.Invalid -> keyValidation.userMessage
                else -> "Gemini API key is not configured."
            }

            // Fallback response with helpful setup guidance
            val offlineFallback = jarvisBrain.chatWithJarvis(
                userMessage = trimmed,
                agent = selectedAgent,
                history = conversationHistory,
                customApiKey = customApiKey
            )

            val formattedMessage = buildString {
                append("⚠️ **API Key Notice:** $warningPrompt\n\n")
                append(offlineFallback.message)
            }

            val chatMsg = offlineFallback.copy(
                message = formattedMessage,
                taskStatus = "Offline Mode (API Key Missing/Invalid)",
                isError = false
            )

            return@withContext OrchestrationResult(
                chatMessage = chatMsg,
                executedCommand = offlineFallback.runCommand,
                requiresAutoExecution = autoExecute && !offlineFallback.runCommand.isNullOrBlank()
            )
        }

        val response = jarvisBrain.chatWithJarvis(
            userMessage = trimmed,
            agent = selectedAgent,
            history = conversationHistory,
            customApiKey = customApiKey
        )

        val shouldAutoRun = autoExecute && !response.runCommand.isNullOrBlank()
        OrchestrationResult(
            chatMessage = response,
            executedCommand = response.runCommand,
            requiresAutoExecution = shouldAutoRun
        )
    }

    /**
     * Formats raw execution output into a clear, conversational human-readable message.
     */
    private fun formatExecutionSummary(
        command: String,
        isSuccess: Boolean,
        exitCode: Int,
        durationMs: Long,
        output: String
    ): String {
        return buildString {
            if (isSuccess) {
                append("Task completed successfully in ${durationMs}ms.\n\n")
            } else {
                append("Task encountered an issue (Exit Code: $exitCode) in ${durationMs}ms.\n\n")
            }

            val lines = output.lines()
            if (lines.size <= 8 && output.isNotBlank()) {
                append(output)
            } else if (output.isNotBlank()) {
                append(lines.take(6).joinToString("\n"))
                append("\n... (${lines.size - 6} more lines in output card)")
            } else {
                append("No standard output produced.")
            }
        }.trim()
    }
}
