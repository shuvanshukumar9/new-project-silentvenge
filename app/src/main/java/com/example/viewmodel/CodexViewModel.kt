package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.CodexResult
import com.example.ai.GeminiCodexService
import com.example.data.AppDatabase
import com.example.data.CommandResultEntity
import com.example.data.SavedScript
import com.example.data.ScriptRepository
import com.example.executor.AutomationExecutor
import com.example.executor.CommandResult
import com.example.executor.ExecutionState
import com.example.executor.LogType
import com.example.executor.TerminalLog
import com.example.executor.TermuxCommandExecutionService
import com.example.voice.VoiceAutomationManager
import com.example.voice.VoiceLanguage
import com.example.voice.VoiceState
import com.example.jarvis.BugDiagnosis
import com.example.jarvis.DeviceAuditReport
import com.example.jarvis.FileInspectionResult
import com.example.jarvis.JarvisAgentType
import com.example.jarvis.JarvisBrainService
import com.example.jarvis.JarvisChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.agent.SilentVengeAgentManager
import com.example.agent.bridge.TermuxBridge
import com.example.agent.model.*
import com.example.agent.router.ModelRouter
import com.example.agent.security.SecurityPolicyEngine
import com.example.agent.tools.DeterministicToolExecutor
import com.example.ai.AIOrchestrator
import com.example.executor.ActiveShellProcess
import java.io.File

enum class CodexTab(val title: String, val icon: String) {
    AGENT("AGENT", "⚡"),
    PIPELINE("PIPELINE", "📊"),
    TERMINAL("TERMINAL", "🖥️"),
    TOOLS("TOOLS", "🛠️"),
    ROUTER("ROUTER", "🔀"),
    ARCH("ARCH & SPECS", "📐"),
    HISTORY("LIBRARY", "📚")
}

class CodexViewModel(application: Application) : AndroidViewModel(application) {

    // Internal Services & Database (Strictly internal to AIOrchestrator and CodexViewModel)
    private val database = AppDatabase.getDatabase(application)
    private val repository = ScriptRepository(database.scriptDao())
    private val codexService = GeminiCodexService()
    private val jarvisBrain = JarvisBrainService(application)
    private val executor = AutomationExecutor()
    val voiceManager = VoiceAutomationManager(application)

    // SilentVenge Core Architecture & Internal Services
    val termuxBridge = TermuxBridge(application)
    internal val termuxExecutionService = TermuxCommandExecutionService.getInstance(application)
    internal val aiOrchestrator = AIOrchestrator(
        jarvisBrain = jarvisBrain,
        termuxExecutionService = termuxExecutionService,
        executor = executor,
        termuxBridge = termuxBridge
    )
    val toolExecutor = DeterministicToolExecutor(application, termuxBridge)
    val securityEngine = SecurityPolicyEngine()
    val modelRouter = ModelRouter(codexService)
    val agentManager = SilentVengeAgentManager(
        context = application,
        taskDao = database.agentTaskDao(),
        toolExecutor = toolExecutor,
        modelRouter = modelRouter,
        securityEngine = securityEngine,
        bridge = termuxBridge
    )

    val currentAgentTask: StateFlow<AgentTask?> = agentManager.currentTask
    val agentTaskState: StateFlow<TaskState> = agentManager.currentTaskState
    val pendingSecurityAction: StateFlow<SecurityActionRequest?> = agentManager.pendingSecurityAction
    val currentSteps: StateFlow<List<TaskStep>> = agentManager.currentSteps
    val modelConfig: StateFlow<ModelRouteConfig> = modelRouter.config
    val tokenTelemetry: StateFlow<TokenTelemetry> = modelRouter.telemetry
    val bridgeLogs: StateFlow<List<TerminalLog>> = termuxBridge.logs
    val activeShellProcess: StateFlow<ActiveShellProcess?> = termuxBridge.shellWrapper.activeProcessState
    val pipelineQueue: StateFlow<List<AgentTask>> = agentManager.taskQueue

    val serverDaemons: StateFlow<List<ServerDaemon>> = database.agentTaskDao().getAllDaemons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val commandHistory: StateFlow<List<CommandResultEntity>> = termuxExecutionService.commandHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isExecutingCommand = MutableStateFlow(false)
    val isExecutingCommand: StateFlow<Boolean> = _isExecutingCommand.asStateFlow()

    private val _workspaceFiles = MutableStateFlow<List<FileEntry>>(emptyList())
    val workspaceFiles: StateFlow<List<FileEntry>> = _workspaceFiles.asStateFlow()

    private val _projectInfo = MutableStateFlow<ProjectInfo?>(null)
    val projectInfo: StateFlow<ProjectInfo?> = _projectInfo.asStateFlow()

    private val _searchResults = MutableStateFlow<List<String>>(emptyList())
    val searchResults: StateFlow<List<String>> = _searchResults.asStateFlow()

    private val _connectionStatus = MutableStateFlow<Pair<Boolean, String>?>(null)
    val connectionStatus: StateFlow<Pair<Boolean, String>?> = _connectionStatus.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    val savedScripts: StateFlow<List<SavedScript>> = repository.allScripts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val terminalLogs: StateFlow<List<TerminalLog>> = executor.logs
    val executionState: StateFlow<ExecutionState> = executor.executionState
    val voiceState: StateFlow<VoiceState> = voiceManager.voiceState
    val voiceLanguage: StateFlow<VoiceLanguage> = voiceManager.selectedLanguage

    private val _activeTab = MutableStateFlow(CodexTab.AGENT)
    val activeTab: StateFlow<CodexTab> = _activeTab.asStateFlow()

    private val _promptInput = MutableStateFlow("")
    val promptInput: StateFlow<String> = _promptInput.asStateFlow()

    private val _terminalInput = MutableStateFlow("")
    val terminalInput: StateFlow<String> = _terminalInput.asStateFlow()

    private val _selectedTargetEnv = MutableStateFlow("Termux")
    val selectedTargetEnv: StateFlow<String> = _selectedTargetEnv.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // JARVIS Chat & Multi-Agent States
    private val _jarvisMessages = MutableStateFlow<List<JarvisChatMessage>>(emptyList())
    val jarvisMessages: StateFlow<List<JarvisChatMessage>> = _jarvisMessages.asStateFlow()

    private val _selectedJarvisAgent = MutableStateFlow(JarvisAgentType.JARVIS_CORE)
    val selectedJarvisAgent: StateFlow<JarvisAgentType> = _selectedJarvisAgent.asStateFlow()

    private val _isJarvisThinking = MutableStateFlow(false)
    val isJarvisThinking: StateFlow<Boolean> = _isJarvisThinking.asStateFlow()

    // Bug Hunter States
    private val _bugDiagnosis = MutableStateFlow<BugDiagnosis?>(null)
    val bugDiagnosis: StateFlow<BugDiagnosis?> = _bugDiagnosis.asStateFlow()

    private val _isDiagnosingBug = MutableStateFlow(false)
    val isDiagnosingBug: StateFlow<Boolean> = _isDiagnosingBug.asStateFlow()

    private val _logcatCaptured = MutableStateFlow<String?>(null)
    val logcatCaptured: StateFlow<String?> = _logcatCaptured.asStateFlow()

    private val _isCapturingLogcat = MutableStateFlow(false)
    val isCapturingLogcat: StateFlow<Boolean> = _isCapturingLogcat.asStateFlow()

    // File Inspector States
    private val _fileInspectionResult = MutableStateFlow<FileInspectionResult?>(null)
    val fileInspectionResult: StateFlow<FileInspectionResult?> = _fileInspectionResult.asStateFlow()

    private val _isInspectingFile = MutableStateFlow(false)
    val isInspectingFile: StateFlow<Boolean> = _isInspectingFile.asStateFlow()

    // Device Audit States
    private val _deviceAuditReport = MutableStateFlow(jarvisBrain.auditDeviceEnvironment())
    val deviceAuditReport: StateFlow<DeviceAuditReport> = _deviceAuditReport.asStateFlow()

    private val _currentScript = MutableStateFlow<CodexResult?>(null)
    val currentScript: StateFlow<CodexResult?> = _currentScript.asStateFlow()

    private val _isAutoExecuteOnVoice = MutableStateFlow(true)
    val isAutoExecuteOnVoice: StateFlow<Boolean> = _isAutoExecuteOnVoice.asStateFlow()

    private val _isTtsEnabled = MutableStateFlow(true)
    val isTtsEnabled: StateFlow<Boolean> = _isTtsEnabled.asStateFlow()

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _showDevConsole = MutableStateFlow(false)
    val showDevConsole: StateFlow<Boolean> = _showDevConsole.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val isTermuxInstalled: Boolean by lazy { executor.isTermuxInstalled(application) }
    val isKaliInstalled: Boolean by lazy { executor.isKaliInstalled(application) }

    init {
        // Observe voice recognizer results
        viewModelScope.launch {
            voiceManager.voiceState.collect { state ->
                if (state is VoiceState.Recognized) {
                    _promptInput.value = state.text
                    sendJarvisMessage(state.text)
                }
            }
        }

        // Initialize with default Termux setup script so UI is immediately active
        val initialResult = codexService.generateOfflineTemplate("setup termux packages", "Termux")
        _currentScript.value = initialResult
        executor.addLog(LogType.SYSTEM, "⚡ KaliDroid Codex Initialized.")
        executor.addLog(LogType.SYSTEM, "Target: Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
        executor.addLog(LogType.SYSTEM, "Termux: ${if (isTermuxInstalled) "Detected" else "Stand-alone Sandbox Ready"}")

        // Initialize JARVIS Core greeting
        _jarvisMessages.value = listOf(
            JarvisChatMessage(
                senderType = JarvisAgentType.JARVIS_CORE,
                message = "Good day, sir. JARVIS online and all core systems operational. Sub-agents (Codex Architect, Bug Hunter, File Inspector, Device Auditor) are ready.\n\nआप हिन्दी, Hinglish, या English में कोई भी कमांड दे सकते हैं। मैं कोड लिखने, डिवाइस के बग्स फिक्स करने, और फाइल्स को ऑडिट करने के लिए तैयार हूँ।",
                isUser = false
            )
        )

        // Initialize workspace files
        refreshWorkspaceFiles()
    }

    fun setTab(tab: CodexTab) {
        _activeTab.value = tab
    }

    fun setPrompt(text: String) {
        _promptInput.value = text
    }

    fun setTerminalInput(text: String) {
        _terminalInput.value = text
    }

    fun setTargetEnv(env: String) {
        _selectedTargetEnv.value = env
    }

    fun toggleAutoExecuteOnVoice() {
        _isAutoExecuteOnVoice.value = !_isAutoExecuteOnVoice.value
    }

    fun toggleTts() {
        _isTtsEnabled.value = !_isTtsEnabled.value
        voiceManager.isTtsEnabled = _isTtsEnabled.value
    }

    fun setVoiceLanguage(language: VoiceLanguage) {
        voiceManager.setLanguage(language)
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
    }

    fun setShowSettingsDialog(show: Boolean) {
        _showSettingsDialog.value = show
    }

    fun setShowDevConsole(show: Boolean) {
        _showDevConsole.value = show
    }

    fun clearChatMessages() {
        _jarvisMessages.value = listOf(
            JarvisChatMessage(
                senderType = JarvisAgentType.JARVIS_CORE,
                message = "Hello! I am your AI assistant. How can I help you today? You can ask me questions, have me write scripts, or execute tasks in the background.",
                isUser = false
            )
        )
    }

    fun clearTerminal() {
        executor.clearLogs()
        termuxBridge.clearLogs()
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showSnackbar(msg: String) {
        _snackbarMessage.value = msg
    }

    fun startVoiceListening() {
        voiceManager.startListening()
    }

    fun stopVoiceListening() {
        voiceManager.stopListening()
    }

    private fun processVoicePrompt(voiceText: String) {
        viewModelScope.launch {
            generateCode(voiceText, autoRunAfter = _isAutoExecuteOnVoice.value)
        }
    }

    fun generateCode(prompt: String = _promptInput.value, autoRunAfter: Boolean = false) {
        if (prompt.isBlank()) {
            showSnackbar("Please speak or type a prompt first.")
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            executor.addLog(LogType.SYSTEM, "━".repeat(40))
            executor.addLog(LogType.SYSTEM, "🤖 Codex AI generating script for: \"$prompt\"")

            val result = codexService.generateScript(
                prompt = prompt,
                preferredEnv = _selectedTargetEnv.value,
                customApiKey = _customApiKey.value.takeIf { it.isNotBlank() }
            )

            _currentScript.value = result
            _isGenerating.value = false

            // Auto-save script to Room
            val entity = SavedScript(
                title = result.title,
                targetEnv = result.targetEnv,
                language = result.language,
                promptQuery = prompt,
                scriptContent = result.code,
                runCommand = result.runCommand,
                explanation = result.summary,
                requiredPackages = result.requiredPackages
            )
            repository.insertScript(entity)

            // Voice synthesis of summary if TTS enabled
            if (_isTtsEnabled.value) {
                val speechSummary = "${result.title}. ${result.summary}"
                voiceManager.speak(speechSummary)
            }

            if (autoRunAfter) {
                showSnackbar("Voice prompt processed! Auto-executing script...")
                _activeTab.value = CodexTab.TERMINAL
                executeCurrentScriptLocally()
            } else {
                showSnackbar("Generated: ${result.title}")
            }
        }
    }

    fun executeCurrentScriptLocally() {
        val script = _currentScript.value ?: return
        _activeTab.value = CodexTab.TERMINAL
        viewModelScope.launch {
            val exitCode = executor.executeScriptLocally(
                context = getApplication(),
                scriptName = script.title,
                code = script.code,
                language = script.language
            )

            if (_isTtsEnabled.value) {
                val isHindi = voiceLanguage.value == VoiceLanguage.HINDI ||
                        script.title.any { it in '\u0900'..'\u097F' } ||
                        script.summary.any { it in '\u0900'..'\u097F' }
                if (exitCode == 0) {
                    val msg = if (isHindi) "स्क्रिप्ट एग्जीक्यूशन सफलतापूर्वक पूरा हुआ।" else "Execution completed successfully with exit code zero."
                    voiceManager.speak(msg)
                } else {
                    val msg = if (isHindi) "एग्जीक्यूशन कोड $exitCode के साथ समाप्त हुआ।" else "Execution ended with exit code $exitCode."
                    voiceManager.speak(msg)
                }
            }
        }
    }

    fun dispatchToTermux() {
        val script = _currentScript.value ?: return
        val result = executor.dispatchToTermux(
            context = getApplication(),
            command = script.runCommand.ifBlank { script.code },
            scriptCode = script.code,
            inBackground = false
        )
        showSnackbar(result.message)
        _activeTab.value = CodexTab.TERMINAL
    }

    fun runTerminalCommand(command: String = _terminalInput.value) {
        if (command.isBlank()) return
        _terminalInput.value = ""
        viewModelScope.launch {
            executor.executeCommandLocally(getApplication(), command)
        }
    }

    fun killProcess() {
        executor.killCurrentProcess()
        showSnackbar("Process stopped.")
    }

    fun clearTerminalLogs() {
        executor.clearLogs()
        termuxBridge.clearLogs()
    }

    fun loadSavedScript(script: SavedScript) {
        _currentScript.value = CodexResult(
            title = script.title,
            targetEnv = script.targetEnv,
            language = script.language,
            code = script.scriptContent,
            runCommand = script.runCommand,
            summary = script.explanation,
            requiredPackages = script.requiredPackages
        )
        _selectedTargetEnv.value = script.targetEnv
        _promptInput.value = script.promptQuery
        _activeTab.value = CodexTab.AGENT
        showSnackbar("Loaded: ${script.title}")
    }

    fun toggleFavorite(script: SavedScript) {
        viewModelScope.launch {
            repository.updateScript(script.copy(isFavorite = !script.isFavorite))
        }
    }

    fun deleteSavedScript(script: SavedScript) {
        viewModelScope.launch {
            repository.deleteScript(script)
            showSnackbar("Deleted script: ${script.title}")
        }
    }

    fun updateCurrentCode(newCode: String) {
        _currentScript.value = _currentScript.value?.copy(code = newCode)
    }

    fun updateCurrentCommand(newCommand: String) {
        _currentScript.value = _currentScript.value?.copy(runCommand = newCommand)
    }

    fun shareCurrentScript(context: Context) {
        val script = _currentScript.value ?: return
        executor.shareScript(context, script.title, script.code)
    }

    // ==========================================
    // JARVIS Multi-Agent & Chat Methods
    // ==========================================

    fun selectJarvisAgent(agent: JarvisAgentType) {
        _selectedJarvisAgent.value = agent
    }

    fun sendJarvisMessage(userText: String) {
        if (userText.isBlank()) return
        val trimmed = userText.trim()
        val currentAgent = _selectedJarvisAgent.value
        val userMsg = JarvisChatMessage(
            senderType = currentAgent,
            message = trimmed,
            isUser = true
        )
        _jarvisMessages.value = _jarvisMessages.value + userMsg

        viewModelScope.launch {
            _isJarvisThinking.value = true
            try {
                val orchestrationResult = aiOrchestrator.processChatInput(
                    userInput = trimmed,
                    selectedAgent = currentAgent,
                    conversationHistory = _jarvisMessages.value,
                    customApiKey = _customApiKey.value.takeIf { it.isNotBlank() },
                    autoExecute = _isAutoExecuteOnVoice.value,
                    onCommandExecuting = { executingMsg ->
                        _jarvisMessages.value = _jarvisMessages.value + executingMsg
                    }
                )

                // Update or append the resulting response
                val existingIndex = _jarvisMessages.value.indexOfFirst { it.id == orchestrationResult.chatMessage.id }
                if (existingIndex >= 0) {
                    _jarvisMessages.value = _jarvisMessages.value.map { msg ->
                        if (msg.id == orchestrationResult.chatMessage.id) orchestrationResult.chatMessage else msg
                    }
                } else {
                    _jarvisMessages.value = _jarvisMessages.value + orchestrationResult.chatMessage
                }

                if (orchestrationResult.requiresAutoExecution && !orchestrationResult.executedCommand.isNullOrBlank()) {
                    executeTaskCommandInBackground(orchestrationResult.chatMessage.id, orchestrationResult.executedCommand)
                }

                if (_isTtsEnabled.value) {
                    voiceManager.speak(orchestrationResult.chatMessage.message.take(200))
                }
                refreshWorkspaceFiles()
            } catch (e: Exception) {
                _jarvisMessages.value = _jarvisMessages.value + JarvisChatMessage(
                    senderType = currentAgent,
                    message = "Error processing request: ${e.localizedMessage ?: "Unknown error"}",
                    isUser = false,
                    isError = true
                )
            } finally {
                _isJarvisThinking.value = false
            }
        }
    }

    fun executeTaskCommandInBackground(messageId: String, cmd: String) {
        viewModelScope.launch {
            _jarvisMessages.value = _jarvisMessages.value.map { msg ->
                if (msg.id == messageId) msg.copy(isRunning = true, taskStatus = "Executing task...") else msg
            }

            val result = aiOrchestrator.executeCommandAndFormat(
                command = cmd,
                initialMessageId = messageId
            )

            _jarvisMessages.value = _jarvisMessages.value.map { msg ->
                if (msg.id == messageId) result.chatMessage else msg
            }
            refreshWorkspaceFiles()
        }
    }

    fun speakJarvisText(text: String) {
        voiceManager.speak(text)
    }

    // ==========================================
    // Bug Hunter & Logcat Doctor Methods
    // ==========================================

    fun diagnoseBug(errorLog: String) {
        if (errorLog.isBlank()) {
            showSnackbar("Please provide an error log or stack trace.")
            return
        }

        viewModelScope.launch {
            _isDiagnosingBug.value = true
            val diagnosis = jarvisBrain.diagnoseBug(
                errorLog = errorLog,
                customApiKey = _customApiKey.value.takeIf { it.isNotBlank() }
            )
            _bugDiagnosis.value = diagnosis
            _isDiagnosingBug.value = false

            if (_isTtsEnabled.value) {
                val voiceSummary = "JARVIS diagnosis: ${diagnosis.title}. ${diagnosis.rootCause}"
                voiceManager.speak(voiceSummary)
            }
            showSnackbar("Bug diagnosed: ${diagnosis.title}")
        }
    }

    fun captureLogcat() {
        viewModelScope.launch {
            _isCapturingLogcat.value = true
            showSnackbar("Capturing device logcat...")
            val logcat = jarvisBrain.captureDeviceLogcat(lines = 120)
            _logcatCaptured.value = logcat
            _isCapturingLogcat.value = false
            showSnackbar("Logcat captured (${logcat.lines().size} lines).")
        }
    }

    fun executeFixFromBugHunter(script: String, runCmd: String) {
        _currentScript.value = CodexResult(
            title = _bugDiagnosis.value?.title ?: "Automated Bug Fix",
            targetEnv = _selectedTargetEnv.value,
            language = _bugDiagnosis.value?.language ?: "bash",
            code = script,
            runCommand = runCmd,
            summary = _bugDiagnosis.value?.solutionExplanation ?: "Automated patch by JARVIS Bug Hunter",
            requiredPackages = "",
            executionInstructions = "Automated bug patch execution"
        )
        _activeTab.value = CodexTab.TERMINAL
        showSnackbar("Executing patch: $runCmd")
        executeCurrentScriptLocally()
    }

    // ==========================================
    // Document & Code Inspector Methods
    // ==========================================

    fun inspectFile(fileName: String, content: String) {
        if (content.isBlank()) {
            showSnackbar("File content is empty.")
            return
        }

        viewModelScope.launch {
            _isInspectingFile.value = true
            val result = jarvisBrain.inspectFile(
                fileName = fileName,
                content = content,
                customApiKey = _customApiKey.value.takeIf { it.isNotBlank() }
            )
            _fileInspectionResult.value = result
            _isInspectingFile.value = false
            showSnackbar("Audit complete for $fileName")
        }
    }

    // ==========================================
    // Device & App Telemetry Methods
    // ==========================================

    fun refreshDeviceAudit() {
        _deviceAuditReport.value = jarvisBrain.auditDeviceEnvironment()
        showSnackbar("Device telemetry updated.")
    }

    // ==========================================
    // SilentVenge Autonomous Agent Methods
    // ==========================================

    fun runAgentGoal(goal: String) {
        if (goal.isBlank()) return
        viewModelScope.launch {
            agentManager.runGoal(goal)
            refreshWorkspaceFiles()
        }
    }

    fun enqueueAgentGoal(goal: String) {
        if (goal.isBlank()) return
        viewModelScope.launch {
            agentManager.enqueueGoal(goal)
            showSnackbar("Goal added to pipeline queue")
        }
    }

    fun removeQueuedTask(index: Int) {
        agentManager.removeQueuedTask(index)
        showSnackbar("Removed queued task")
    }

    fun clearTaskQueue() {
        agentManager.clearQueue()
        showSnackbar("Pipeline queue cleared")
    }

    fun confirmSecurityAction(approved: Boolean) {
        agentManager.confirmSecurityAction(approved)
    }

    fun stopAgentTask() {
        agentManager.stopCurrentTask()
    }

    fun executeTerminalCommand(cmd: String) {
        if (cmd.isBlank()) return
        viewModelScope.launch {
            _isExecutingCommand.value = true
            termuxBridge.addLog(LogType.COMMAND, "$ $cmd")
            executor.addLog(LogType.COMMAND, "$ $cmd")
            try {
                termuxExecutionService.executeAndLog(
                    command = cmd,
                    saveToDatabase = true,
                    onStdoutLine = {
                        termuxBridge.addLog(LogType.STDOUT, it)
                        executor.addLog(LogType.STDOUT, it)
                    },
                    onStderrLine = {
                        termuxBridge.addLog(LogType.STDERR, it)
                        executor.addLog(LogType.STDERR, it)
                    }
                )
                refreshWorkspaceFiles()
            } finally {
                _isExecutingCommand.value = false
            }
        }
    }

    fun clearCommandHistory() {
        viewModelScope.launch {
            termuxExecutionService.clearHistory()
            showSnackbar("Command history cleared")
        }
    }

    fun dispatchToTermux(cmd: String) {
        if (cmd.isBlank()) return
        termuxBridge.dispatchTermuxIntent(cmd)
        showSnackbar("Dispatched command to Termux")
    }

    fun killTerminalProcess() {
        val killedServiceProcess = termuxExecutionService.killActiveProcess()
        termuxBridge.killCurrentProcess()
        executor.killCurrentProcess()
        _isExecutingCommand.value = false
        showSnackbar(if (killedServiceProcess) "ProcessBuilder process killed." else "Process killed.")
    }

    fun refreshWorkspaceFiles() {
        viewModelScope.launch {
            val list = toolExecutor.listFiles()
            _workspaceFiles.value = list
            _projectInfo.value = toolExecutor.inspectProject()
        }
    }

    fun createWorkspaceFile(name: String, content: String) {
        viewModelScope.launch {
            val success = toolExecutor.writeFile(name, content)
            if (success) {
                showSnackbar("Created file $name")
                refreshWorkspaceFiles()
            } else {
                showSnackbar("Failed to create file $name")
            }
        }
    }

    fun deleteWorkspaceFile(path: String) {
        viewModelScope.launch {
            val policy = securityEngine.evaluateFileOperation("DELETE", path)
            if (policy.level != PermissionLevel.DENY) {
                val success = toolExecutor.deleteFile(path)
                if (success) {
                    showSnackbar("Deleted $path")
                    refreshWorkspaceFiles()
                } else {
                    showSnackbar("Failed to delete $path")
                }
            } else {
                showSnackbar("Deletion rejected by security policy: ${policy.reason}")
            }
        }
    }

    fun searchCodeInWorkspace(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            val matches = toolExecutor.searchCode(query = query)
            _searchResults.value = matches
            showSnackbar("Found ${matches.size} matches")
        }
    }

    fun startServerDaemon(name: String, command: String, port: Int) {
        viewModelScope.launch {
            val daemon = toolExecutor.startServer(name, command, port)
            database.agentTaskDao().insertDaemon(daemon)
            showSnackbar("Server '$name' started on port $port")
        }
    }

    fun stopServerDaemon(id: Long) {
        viewModelScope.launch {
            toolExecutor.stopServer(id)
            database.agentTaskDao().deleteDaemon(id)
            showSnackbar("Server stopped")
        }
    }

    fun updateModelRouteConfig(config: ModelRouteConfig) {
        modelRouter.updateConfig(config)
        showSnackbar("Model configuration updated: ${config.selectedProvider.displayName}")
    }

    fun testModelConnection(provider: AiProviderType, endpoint: String) {
        viewModelScope.launch {
            _isTestingConnection.value = true
            val (ok, msg) = modelRouter.testConnection(provider, endpoint)
            _connectionStatus.value = Pair(ok, msg)
            _isTestingConnection.value = false
            showSnackbar(msg)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
        executor.killCurrentProcess()
        termuxBridge.killCurrentProcess()
    }
}
