package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agent.model.AgentTask
import com.example.agent.model.ModelRouteConfig
import com.example.agent.model.SecurityActionRequest
import com.example.agent.model.TaskState
import com.example.agent.model.TaskStep
import com.example.agent.model.TokenTelemetry
import com.example.ui.theme.*

@Composable
fun SilentVengeAgentScreen(
    currentTask: AgentTask?,
    taskState: TaskState,
    pendingSecurityAction: SecurityActionRequest?,
    currentSteps: List<TaskStep>,
    modelConfig: ModelRouteConfig,
    tokenTelemetry: TokenTelemetry,
    onRunGoal: (String) -> Unit,
    onConfirmSecurity: (Boolean) -> Unit,
    onStopTask: () -> Unit,
    onNavigateToTerminal: () -> Unit,
    modifier: Modifier = Modifier
) {
    var goalInput by remember { mutableStateOf("") }
    val quickGoals = listOf(
        "Build standalone Python HTTP server with unit tests",
        "Scan workspace, check syntax and run tests",
        "Create Bash system backup script with error handling",
        "Build Android project and verify debug compilation"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(14.dp)
            .testTag("agent_screen_root"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Hero HUD Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (taskState == TaskState.IDLE) NeonGreen else CyberCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SILENTVENGE MINI AGENT",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        // Model Tag
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (modelConfig.selectedProvider.isLocal) CyberSurfaceVariant else CyberSurface
                        ) {
                            Text(
                                text = modelConfig.selectedProvider.displayName,
                                fontSize = 10.sp,
                                color = if (modelConfig.selectedProvider.isLocal) NeonGreen else CyberCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Token Economy telemetry
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = CyberSurface
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Tokens Saved (Tools)", fontSize = 10.sp, color = TextMuted)
                                Text(
                                    text = "${tokenTelemetry.tokensSavedByDeterministicTools} 🛡️",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreen,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = CyberSurface
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Zero-Token Operations", fontSize = 10.sp, color = TextMuted)
                                Text(
                                    text = "${tokenTelemetry.offlineOperationsCount} local",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Pending Security Action Banner (PROMPT confirmation)
        if (pendingSecurityAction != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, TerminalRed, RoundedCornerShape(12.dp))
                        .testTag("security_prompt_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1016)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Security Alert", tint = TerminalRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ACTION REQUIRES AUTHORIZATION",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TerminalRed,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = pendingSecurityAction.reason,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = pendingSecurityAction.commandOrTarget,
                                fontSize = 11.sp,
                                color = CyberCyan,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onConfirmSecurity(false) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalRed),
                                modifier = Modifier.testTag("security_deny_button")
                            ) {
                                Text("Deny & Abort", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onConfirmSecurity(true) },
                                colors = ButtonDefaults.buttonColors(containerColor = TerminalRed),
                                modifier = Modifier.testTag("security_allow_button")
                            ) {
                                Text("Authorize Once", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // 3. Goal Input & Quick Chips
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "AUTONOMOUS GOAL PIPELINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = goalInput,
                        onValueChange = { goalInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("goal_input_field"),
                        placeholder = { Text("Enter high-level goal: e.g. 'Create a Python REST API with tests'", fontSize = 12.sp, color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = CyberCyan
                        ),
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Goal Chips
                    Text("Quick Goals:", fontSize = 10.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        quickGoals.forEach { q ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CyberSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { goalInput = q }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(q, fontSize = 11.sp, color = TextPrimary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (taskState != TaskState.IDLE && taskState != TaskState.COMPLETED && taskState != TaskState.FAILED) {
                            Button(
                                onClick = onStopTask,
                                colors = ButtonDefaults.buttonColors(containerColor = TerminalRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("stop_task_button")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop Pipeline", fontSize = 11.sp)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Button(
                            onClick = {
                                if (goalInput.isNotBlank()) {
                                    onRunGoal(goalInput)
                                }
                            },
                            enabled = goalInput.isNotBlank() && (taskState == TaskState.IDLE || taskState == TaskState.COMPLETED || taskState == TaskState.FAILED),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("run_goal_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = CyberBlack, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Execute Goal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberBlack)
                        }
                    }
                }
            }
        }

        // 4. Autonomous State Machine Stepper Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STATE MACHINE PIPELINE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen,
                            fontFamily = FontFamily.Monospace
                        )
                        if (currentTask != null && currentTask.iterationCount > 0) {
                            Text(
                                text = "Iteration ${currentTask.iterationCount}/${currentTask.maxIterations}",
                                fontSize = 11.sp,
                                color = CyberCyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Step sequence
                    val stepsList = listOf(
                        TaskState.INSPECTING,
                        TaskState.PLANNING,
                        TaskState.CODING,
                        TaskState.SAVING,
                        TaskState.BUILDING,
                        TaskState.TESTING,
                        TaskState.ANALYZING_ERRORS,
                        TaskState.PATCHING,
                        TaskState.COMPLETED
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        stepsList.forEach { step ->
                            val isActive = taskState == step
                            val isPassed = taskState.stepIndex > step.stepIndex || taskState == TaskState.COMPLETED

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isActive) CyberSurfaceVariant else CyberSurface)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isPassed) "✓" else step.icon,
                                    fontSize = 12.sp,
                                    color = if (isPassed) NeonGreen else if (isActive) CyberCyan else TextMuted
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = step.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isPassed) NeonGreen else if (isActive) CyberCyan else TextMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (isActive) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = CyberCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Execution Step Timeline
        if (currentSteps.isNotEmpty()) {
            item {
                Text(
                    text = "EXECUTION DETAILS & STEP AUDIT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            items(currentSteps) { step ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = CyberDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = if (step.status == "SUCCESS") "✓" else if (step.status == "FAILED") "✗" else "⏳",
                            fontSize = 13.sp,
                            color = if (step.status == "SUCCESS") NeonGreen else if (step.status == "FAILED") TerminalRed else CyberCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = step.stepName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "[${step.toolUsed}]",
                                    fontSize = 10.sp,
                                    color = CyberCyan,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = step.details,
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // 6. Navigation Link to Terminal
        item {
            OutlinedButton(
                onClick = onNavigateToTerminal,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
            ) {
                Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Live Terminal & Termux Output Stream", fontSize = 11.sp)
            }
        }
    }
}
