package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agent.model.AiProviderType
import com.example.agent.model.ModelRouteConfig
import com.example.agent.model.TokenTelemetry
import com.example.ui.theme.*

@Composable
fun SilentVengeModelRouterScreen(
    config: ModelRouteConfig,
    telemetry: TokenTelemetry,
    onUpdateConfig: (ModelRouteConfig) -> Unit,
    onTestConnection: (AiProviderType, String) -> Unit,
    connectionStatus: Pair<Boolean, String>?,
    isTestingConnection: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedProvider by remember(config) { mutableStateOf(config.selectedProvider) }
    var ollamaEndpoint by remember(config) { mutableStateOf(config.localOllamaEndpoint) }
    var ollamaModel by remember(config) { mutableStateOf(config.localOllamaModel) }
    var lmStudioEndpoint by remember(config) { mutableStateOf(config.localLmStudioEndpoint) }
    var lmStudioModel by remember(config) { mutableStateOf(config.localLmStudioModel) }
    var customApiKey by remember(config) { mutableStateOf(config.customApiKey) }
    var preferLocal by remember(config) { mutableStateOf(config.preferLocalWhenAvailable) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(14.dp)
            .testTag("router_screen_root"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Token Economy HUD
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
                        text = "LOW-TOKEN AGENT ECONOMY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = CyberSurface
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Tokens Consumed", fontSize = 10.sp, color = TextMuted)
                                Text(
                                    text = "${telemetry.tokensConsumed}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text("API Calls: ${telemetry.apiCallsCount}", fontSize = 9.sp, color = TextMuted)
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = CyberSurface
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Tokens Saved (Tools)", fontSize = 10.sp, color = TextMuted)
                                Text(
                                    text = "${telemetry.tokensSavedByDeterministicTools} 🛡️",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreen,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text("Zero-Token: ${telemetry.offlineOperationsCount}", fontSize = 9.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }
        }

        // 2. Active AI Model Provider Switcher
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
                        text = "SELECT CODING MODEL PROVIDER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    AiProviderType.entries.forEach { provider ->
                        val isSelected = selectedProvider == provider
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) CyberSurfaceVariant else CyberSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable {
                                    selectedProvider = provider
                                    onUpdateConfig(
                                        config.copy(
                                            selectedProvider = provider,
                                            preferLocalWhenAvailable = preferLocal
                                        )
                                    )
                                }
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) CyberCyan else androidx.compose.ui.graphics.Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedProvider = provider
                                        onUpdateConfig(
                                            config.copy(
                                                selectedProvider = provider,
                                                preferLocalWhenAvailable = preferLocal
                                            )
                                        )
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = CyberCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = provider.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) CyberCyan else TextPrimary
                                    )
                                    Text(
                                        text = if (provider.isLocal) "Offline / Zero cloud cost" else "High-capability Cloud Reasoning",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Prefer local checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = preferLocal,
                            onCheckedChange = {
                                preferLocal = it
                                onUpdateConfig(config.copy(preferLocalWhenAvailable = it))
                            },
                            colors = CheckboxDefaults.colors(checkedColor = NeonGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Use local models when available, cloud only when needed",
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // 3. Endpoint Configuration
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
                        text = "ENDPOINT CONFIGURATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    when (selectedProvider) {
                        AiProviderType.LOCAL_OLLAMA -> {
                            OutlinedTextField(
                                value = ollamaEndpoint,
                                onValueChange = { ollamaEndpoint = it },
                                label = { Text("Ollama Endpoint") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = ollamaModel,
                                onValueChange = { ollamaModel = it },
                                label = { Text("Model Tag (e.g. qwen2.5-coder:7b)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        AiProviderType.LOCAL_LM_STUDIO -> {
                            OutlinedTextField(
                                value = lmStudioEndpoint,
                                onValueChange = { lmStudioEndpoint = it },
                                label = { Text("LM Studio / OpenAI Endpoint") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = lmStudioModel,
                                onValueChange = { lmStudioModel = it },
                                label = { Text("Model Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        AiProviderType.CLOUD_GEMINI -> {
                            OutlinedTextField(
                                value = customApiKey,
                                onValueChange = { customApiKey = it },
                                label = { Text("Custom Gemini API Key (Optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Defaults to Google AI Studio Build environment key if left blank.",
                                fontSize = 10.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        AiProviderType.OFFLINE_DETERMINISTIC -> {
                            Text(
                                text = "Deterministic rule-based heuristic planner. Runs 100% locally with zero external network and 0 token cost.",
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Test connection button
                        Button(
                            onClick = {
                                val endpoint = when (selectedProvider) {
                                    AiProviderType.LOCAL_OLLAMA -> ollamaEndpoint
                                    AiProviderType.LOCAL_LM_STUDIO -> lmStudioEndpoint
                                    else -> ""
                                }
                                onTestConnection(selectedProvider, endpoint)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceVariant)
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = CyberCyan)
                            } else {
                                Icon(Icons.Default.NetworkPing, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyberCyan)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Ping", fontSize = 11.sp, color = CyberCyan)
                        }

                        // Save config button
                        Button(
                            onClick = {
                                onUpdateConfig(
                                    config.copy(
                                        selectedProvider = selectedProvider,
                                        localOllamaEndpoint = ollamaEndpoint,
                                        localOllamaModel = ollamaModel,
                                        localLmStudioEndpoint = lmStudioEndpoint,
                                        localLmStudioModel = lmStudioModel,
                                        customApiKey = customApiKey,
                                        preferLocalWhenAvailable = preferLocal
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                        ) {
                            Text("Apply Config", fontSize = 11.sp, color = CyberBlack, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (connectionStatus != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = connectionStatus.second,
                            fontSize = 11.sp,
                            color = if (connectionStatus.first) NeonGreen else TerminalRed,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
