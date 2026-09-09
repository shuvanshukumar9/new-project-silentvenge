package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.voice.VoiceLanguage

@Composable
fun SettingsDialog(
    initialApiKey: String,
    targetEnv: String,
    isAutoExecute: Boolean,
    isTtsEnabled: Boolean,
    voiceLanguage: VoiceLanguage,
    onVoiceLanguageChange: (VoiceLanguage) -> Unit,
    onSave: (apiKey: String, targetEnv: String, autoExecute: Boolean, ttsEnabled: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKey by remember { mutableStateOf(initialApiKey) }
    var selectedEnv by remember { mutableStateOf(targetEnv) }
    var autoExec by remember { mutableStateOf(isAutoExecute) }
    var tts by remember { mutableStateOf(isTtsEnabled) }
    var currentLang by remember { mutableStateOf(voiceLanguage) }

    val envOptions = listOf("Termux", "Kali Linux", "Android Shell")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberDark,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "CODEX SETTINGS",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = NeonGreen
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "TARGET ENVIRONMENT",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    envOptions.forEach { env ->
                        FilterChip(
                            selected = selectedEnv == env,
                            onClick = { selectedEnv = env },
                            label = { Text(env, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                selectedLabelColor = CyberCyan
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (selectedEnv == env) CyberCyan else CyberBorder,
                                borderWidth = 1.dp,
                                enabled = true,
                                selected = selectedEnv == env
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "VOICE INPUT LANGUAGE",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VoiceLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = currentLang == lang,
                            onClick = {
                                currentLang = lang
                                onVoiceLanguageChange(lang)
                            },
                            label = { Text(lang.displayName, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonGreen.copy(alpha = 0.2f),
                                selectedLabelColor = NeonGreen
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (currentLang == lang) NeonGreen else CyberBorder,
                                borderWidth = 1.dp,
                                enabled = true,
                                selected = currentLang == lang
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Auto-Execute Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Execute on Voice",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Execute script automatically right after voice prompt",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = autoExec,
                        onCheckedChange = { autoExec = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonGreen,
                            checkedTrackColor = CyberSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // TTS Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Voice Response (TTS)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Speak back script summary & status",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = tts,
                        onCheckedChange = { tts = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberCyan,
                            checkedTrackColor = CyberSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "OPTIONAL GEMINI API KEY",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    placeholder = { Text("Inherited from .env / Secrets", color = TextMuted, fontSize = 11.sp) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TextPrimary
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CyberSurface,
                        unfocusedContainerColor = CyberSurface,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("api_key_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(apiKey, selectedEnv, autoExec, tts)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_settings_button")
            ) {
                Text("SAVE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}
