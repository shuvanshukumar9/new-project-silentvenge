package com.example.agent

import android.content.Context
import android.util.Log
import com.example.agent.bridge.TermuxBridge
import com.example.agent.model.AgentTask
import com.example.agent.model.PermissionLevel
import com.example.agent.model.SecurityActionRequest
import com.example.agent.model.TaskState
import com.example.agent.model.TaskStep
import com.example.agent.router.ModelPlanResult
import com.example.agent.router.ModelRouter
import com.example.agent.security.SecurityPolicyEngine
import com.example.agent.tools.DeterministicToolExecutor
import com.example.data.AgentTaskDao
import com.example.executor.ExecutionOutput
import com.example.executor.LogType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SilentVengeAgentManager(
    private val context: Context,
    private val taskDao: AgentTaskDao,
    val toolExecutor: DeterministicToolExecutor,
    val modelRouter: ModelRouter,
    val securityEngine: SecurityPolicyEngine,
    val bridge: TermuxBridge
) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var activeJob: Job? = null

    // State flows
    private val _currentTask = MutableStateFlow<AgentTask?>(null)
    val currentTask: StateFlow<AgentTask?> = _currentTask.asStateFlow()

    private val _currentTaskState = MutableStateFlow(TaskState.IDLE)
    val currentTaskState: StateFlow<TaskState> = _currentTaskState.asStateFlow()

    private val _pendingSecurityAction = MutableStateFlow<SecurityActionRequest?>(null)
    val pendingSecurityAction: StateFlow<SecurityActionRequest?> = _pendingSecurityAction.asStateFlow()

    private val _currentSteps = MutableStateFlow<List<TaskStep>>(emptyList())
    val currentSteps: StateFlow<List<TaskStep>> = _currentSteps.asStateFlow()

    private val _taskQueue = MutableStateFlow<List<AgentTask>>(emptyList())
    val taskQueue: StateFlow<List<AgentTask>> = _taskQueue.asStateFlow()

    private val _isStepByStepMode = MutableStateFlow(false)
    val isStepByStepMode: StateFlow<Boolean> = _isStepByStepMode.asStateFlow()

    private var continuationCallback: ((Boolean) -> Unit)? = null

    fun toggleStepByStepMode(enabled: Boolean) {
        _isStepByStepMode.value = enabled
    }

    /**
     * Enqueue a task to the pipeline queue. If idle, starts immediately.
     */
    fun enqueueGoal(goal: String, projectPath: String = "") {
        if (goal.isBlank()) return
        val workspace = if (projectPath.isNotBlank()) File(projectPath) else toolExecutor.defaultWorkspace
        val queuedTask = AgentTask(
            goal = goal,
            projectPath = workspace.absolutePath,
            state = TaskState.QUEUED.name,
            maxIterations = 3
        )
        if (_currentTaskState.value == TaskState.IDLE || 
            _currentTaskState.value == TaskState.COMPLETED || 
            _currentTaskState.value == TaskState.FAILED) {
            runGoal(goal, projectPath)
        } else {
            _taskQueue.value = _taskQueue.value + queuedTask
            bridge.addLog(LogType.SYSTEM, "⏳ Task queued in pipeline: '$goal' (Position #${_taskQueue.value.size})")
        }
    }

    fun removeQueuedTask(taskIndex: Int) {
        val current = _taskQueue.value.toMutableList()
        if (taskIndex in current.indices) {
            val removed = current.removeAt(taskIndex)
            _taskQueue.value = current
            bridge.addLog(LogType.SYSTEM, "Removed queued task: '${removed.goal}'")
        }
    }

    fun clearQueue() {
        val count = _taskQueue.value.size
        _taskQueue.value = emptyList()
        if (count > 0) {
            bridge.addLog(LogType.SYSTEM, "Cleared $count pending pipeline tasks.")
        }
    }

    /**
     * Entry point: takes an autonomous goal and executes the pipeline:
     * INSPECT -> PLAN -> CODE -> SAVE -> BUILD -> TEST -> ANALYZE -> FIX -> REBUILD -> REPORT
     */
    fun runGoal(goal: String, projectPath: String = "") {
        if (goal.isBlank()) return

        activeJob?.cancel()
        activeJob = scope.launch {
            val workspace = if (projectPath.isNotBlank()) File(projectPath) else toolExecutor.defaultWorkspace
            val task = AgentTask(
                goal = goal,
                projectPath = workspace.absolutePath,
                state = TaskState.INSPECTING.name,
                maxIterations = 3
            )
            val taskId = taskDao.insertTask(task)
            val taskWithId = task.copy(id = taskId)
            _currentTask.value = taskWithId
            _currentSteps.value = emptyList()

            executeAutonomousPipeline(taskWithId)
        }
    }

    private suspend fun executeAutonomousPipeline(initialTask: AgentTask) {
        var task = initialTask
        val workspace = File(task.projectPath)

        try {
            bridge.addLog(LogType.SYSTEM, "━".repeat(42))
            bridge.addLog(LogType.SYSTEM, "⚡ SilentVenge Autonomous Pipeline started for goal: '${task.goal}'")

            // ==========================================
            // STEP 1: INSPECT (Deterministic - 0 tokens!)
            // ==========================================
            updateState(task, TaskState.INSPECTING)
            addStep(task.id, 1, "Inspect Project", "RUNNING", "DeterministicToolExecutor", "Scanning workspace tree...")
            
            val projectInfo = toolExecutor.inspectProject(workspace.absolutePath)
            modelRouter.recordDeterministicSaving(300) // saved by deterministic inspection!
            
            bridge.addLog(LogType.STDOUT, "Project Type: ${projectInfo.projectType.label} (${projectInfo.fileCount} files)")
            completeStep(1, "Scanned: ${projectInfo.projectType.label}, ${projectInfo.fileCount} files")

            // ==========================================
            // STEP 2: PLAN (Low-Token Model Router)
            // ==========================================
            updateState(task, TaskState.PLANNING)
            addStep(task.id, 2, "Generate Plan", "RUNNING", "ModelRouter", "Synthesizing minimal JSON execution plan...")
            
            val plan = modelRouter.generatePlan(
                goal = task.goal,
                projectSummary = "${projectInfo.projectType.label} with ${projectInfo.fileCount} files"
            )
            bridge.addLog(LogType.STDOUT, "Plan: ${plan.planSteps.joinToString(" -> ")}")
            completeStep(2, "Plan ready: ${plan.planSteps.size} steps, target: ${plan.suggestedFileName}")

            // ==========================================
            // STEP 3: CODE (Low-Token Model Router)
            // ==========================================
            updateState(task, TaskState.CODING)
            addStep(task.id, 3, "Synthesize Code", "RUNNING", "ModelRouter", "Writing source code for ${plan.suggestedFileName}...")
            
            val codeResult = modelRouter.generateCode(task.goal, plan)
            task = task.copy(generatedCode = codeResult.code)
            bridge.addLog(LogType.STDOUT, "Synthesized ${codeResult.code.lines().size} lines of ${plan.targetLanguage}")
            completeStep(3, "Synthesized ${codeResult.code.lines().size} lines of code")

            // ==========================================
            // STEP 4: SAVE (Deterministic with Security Policy)
            // ==========================================
            updateState(task, TaskState.SAVING)
            addStep(task.id, 4, "Write to Disk", "RUNNING", "DeterministicToolExecutor", "Saving to ${plan.suggestedFileName}...")
            
            val targetFile = File(workspace, plan.suggestedFileName)
            val secCheck = securityEngine.evaluateFileOperation(
                if (targetFile.exists()) "OVERWRITE" else "CREATE",
                targetFile.absolutePath
            )

            if (!secCheck.isAllowedImmediately && secCheck.request != null) {
                // Pause for confirmation
                val confirmed = requestSecurityConfirmation(secCheck.request)
                if (!confirmed) {
                    failTask(task, "User denied file write operation.")
                    return
                }
            }

            toolExecutor.editFile(targetFile.absolutePath, codeResult.code)
            modelRouter.recordDeterministicSaving(100)
            bridge.addLog(LogType.SUCCESS, "✓ Saved ${targetFile.name} to disk.")
            completeStep(4, "Saved ${targetFile.name} (${targetFile.length()} bytes)")

            // ==========================================
            // STEP 5: BUILD & STEP 6: TEST (Autonomous Iteration Loop)
            // ==========================================
            var iteration = 0
            var buildAndTestSuccess = false
            var currentCode = codeResult.code
            var lastError = ""

            while (iteration < task.maxIterations && !buildAndTestSuccess) {
                iteration++
                task = task.copy(iterationCount = iteration)
                taskDao.updateTask(task)

                // 5. BUILD
                updateState(task, TaskState.BUILDING)
                addStep(task.id, 5, "Build Project (Iter $iteration)", "RUNNING", "TermuxBridge", "$ ${plan.buildCommand}")
                
                val buildSec = securityEngine.evaluateCommand(plan.buildCommand)
                if (!buildSec.isAllowedImmediately && buildSec.request != null) {
                    val ok = requestSecurityConfirmation(buildSec.request)
                    if (!ok) {
                        failTask(task, "Build command rejected by user policy.")
                        return
                    }
                }

                val buildOutput = toolExecutor.runBuild(plan.buildCommand, workspace.absolutePath)
                modelRouter.recordDeterministicSaving(250)

                if (buildOutput.exitCode != 0) {
                    val err = toolExecutor.extractErrors(buildOutput)
                    lastError = "Build failed (exit ${buildOutput.exitCode}):\n$err"
                    bridge.addLog(LogType.ERROR, lastError)
                    completeStep(5, "Build failed with exit ${buildOutput.exitCode}", exitCode = buildOutput.exitCode)
                } else {
                    completeStep(5, "Build passed (Exit 0 in ${buildOutput.durationMs}ms)", exitCode = 0)

                    // 6. TEST
                    updateState(task, TaskState.TESTING)
                    addStep(task.id, 6, "Run Tests (Iter $iteration)", "RUNNING", "TermuxBridge", "$ ${plan.testCommand}")
                    
                    val testSec = securityEngine.evaluateCommand(plan.testCommand)
                    if (!testSec.isAllowedImmediately && testSec.request != null) {
                        val ok = requestSecurityConfirmation(testSec.request)
                        if (!ok) {
                            failTask(task, "Test command rejected by user policy.")
                            return
                        }
                    }

                    val testOutput = toolExecutor.runTests(plan.testCommand, workspace.absolutePath)
                    modelRouter.recordDeterministicSaving(250)

                    if (testOutput.exitCode == 0) {
                        buildAndTestSuccess = true
                        completeStep(6, "Tests passed (Exit 0 in ${testOutput.durationMs}ms)", exitCode = 0)
                        bridge.addLog(LogType.SUCCESS, "✓ All tests executed successfully!")
                        break
                    } else {
                        val err = toolExecutor.extractErrors(testOutput)
                        lastError = "Test failed (exit ${testOutput.exitCode}):\n$err"
                        bridge.addLog(LogType.ERROR, lastError)
                        completeStep(6, "Test failed with exit ${testOutput.exitCode}", exitCode = testOutput.exitCode)
                    }
                }

                // If failed and more iterations remain: ANALYZE & PATCH
                if (!buildAndTestSuccess && iteration < task.maxIterations) {
                    // 7. ANALYZE
                    updateState(task, TaskState.ANALYZING_ERRORS)
                    addStep(task.id, 7, "Analyze Error (Iter $iteration)", "RUNNING", "ModelRouter", "Diagnosing failure logs...")
                    bridge.addLog(LogType.SYSTEM, "🐞 Analyzing failure: $lastError")
                    completeStep(7, "Analyzed failure logs")

                    // 8. PATCH
                    updateState(task, TaskState.PATCHING)
                    addStep(task.id, 8, "Apply Fix (Iter $iteration)", "RUNNING", "ModelRouter", "Synthesizing minimal bugfix patch...")
                    
                    val fixResult = modelRouter.generateFix(currentCode, lastError, plan.suggestedFileName)
                    currentCode = fixResult.patchedCode
                    toolExecutor.editFile(targetFile.absolutePath, currentCode)
                    bridge.addLog(LogType.SUCCESS, "✓ Patch applied to ${targetFile.name}. Rebuilding...")
                    completeStep(8, "Applied patch: ${fixResult.changeSummary}")
                }
            }

            // ==========================================
            // STEP 9: REPORT RESULT
            // ==========================================
            if (buildAndTestSuccess) {
                updateState(task, TaskState.COMPLETED)
                val summary = "Successfully implemented and verified '${task.goal}' in $iteration iteration(s). File '${plan.suggestedFileName}' is built, tested, and ready."
                task = task.copy(
                    isCompleted = true,
                    lastOutput = summary,
                    state = TaskState.COMPLETED.name,
                    updatedAt = System.currentTimeMillis()
                )
                taskDao.updateTask(task)
                _currentTask.value = task
                bridge.addLog(LogType.SUCCESS, "🎉 GOAL COMPLETED: $summary")
            } else {
                failTask(task, "Execution failed after $iteration iteration(s). Last error: $lastError")
            }

        } catch (e: Exception) {
            Log.e("SilentVenge", "Pipeline error", e)
            failTask(task, "Pipeline exception: ${e.localizedMessage}")
        } finally {
            checkAndDrainNextQueuedTask()
        }
    }

    private fun checkAndDrainNextQueuedTask() {
        val queue = _taskQueue.value
        if (queue.isNotEmpty()) {
            val nextTask = queue.first()
            _taskQueue.value = queue.drop(1)
            bridge.addLog(LogType.SYSTEM, "🚀 Starting next queued task: '${nextTask.goal}'")
            runGoal(nextTask.goal, nextTask.projectPath)
        }
    }

    private suspend fun requestSecurityConfirmation(req: SecurityActionRequest): Boolean = withContext(Dispatchers.Main) {
        _pendingSecurityAction.value = req
        _currentTaskState.value = TaskState.PAUSED_NEEDS_CONFIRMATION
        bridge.addLog(LogType.SYSTEM, "⚠️ Paused: User approval needed for ${req.actionType} (${req.commandOrTarget})")

        kotlin.coroutines.suspendCoroutine { cont ->
            continuationCallback = { approved ->
                _pendingSecurityAction.value = null
                continuationCallback = null
                cont.resumeWith(Result.success(approved))
            }
        }
    }

    fun confirmSecurityAction(approved: Boolean) {
        continuationCallback?.invoke(approved)
    }

    private suspend fun updateState(task: AgentTask, state: TaskState) {
        _currentTaskState.value = state
        val updated = task.copy(state = state.name, currentStepIndex = state.stepIndex, updatedAt = System.currentTimeMillis())
        _currentTask.value = updated
        taskDao.updateTask(updated)
    }

    private suspend fun addStep(taskId: Long, index: Int, name: String, status: String, tool: String, details: String) {
        val step = TaskStep(
            taskId = taskId,
            stepIndex = index,
            stepName = name,
            status = status,
            toolUsed = tool,
            details = details
        )
        val id = taskDao.insertStep(step)
        val stepWithId = step.copy(id = id)
        _currentSteps.value = _currentSteps.value.filterNot { it.stepIndex == index } + stepWithId
    }

    private suspend fun completeStep(index: Int, details: String, exitCode: Int? = null) {
        val existing = _currentSteps.value.find { it.stepIndex == index }
        if (existing != null) {
            val updated = existing.copy(
                status = if (exitCode != null && exitCode != 0) "FAILED" else "SUCCESS",
                details = details,
                exitCode = exitCode
            )
            taskDao.insertStep(updated)
            _currentSteps.value = _currentSteps.value.map { if (it.stepIndex == index) updated else it }
        }
    }

    private suspend fun failTask(task: AgentTask, error: String) {
        _currentTaskState.value = TaskState.FAILED
        val failed = task.copy(
            state = TaskState.FAILED.name,
            errorSummary = error,
            updatedAt = System.currentTimeMillis()
        )
        _currentTask.value = failed
        taskDao.updateTask(failed)
        bridge.addLog(LogType.ERROR, "✗ Goal failed: $error")
    }

    fun stopCurrentTask() {
        activeJob?.cancel()
        activeJob = null
        bridge.killCurrentProcess()
        _currentTaskState.value = TaskState.IDLE
        bridge.addLog(LogType.SYSTEM, "Task stopped by user.")
    }
}
