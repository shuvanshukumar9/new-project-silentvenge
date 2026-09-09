package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.theme.TerminalRed
import com.example.ui.theme.TerminalYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.voice.VoiceLanguage
import com.example.voice.VoiceState

@Composable
fun VoiceDeck(
    promptText: String,
    onPromptChange: (String) -> Unit,
    onGenerate: (String) -> Unit,
    voiceState: VoiceState,
    selectedLanguage: VoiceLanguage,
    onLanguageChange: (VoiceLanguage) -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    isGenerating: Boolean,
    isAutoExecute: Boolean,
    modifier: Modifier = Modifier
) {
    val isListening = voiceState is VoiceState.Listening
    val rmsLevel = if (voiceState is VoiceState.Listening) voiceState.rmsDb else 0f
    val isHindi = selectedLanguage == VoiceLanguage.HINDI

    val quickPrompts = if (isHindi) {
        listOf(
            "⚡ टर्मक्स अपडेट" to "टर्मक्स को अपडेट और अपग्रेड करो और पाइथन इनस्टॉल करो",
            "🐍 पोर्ट स्कैनर" to "पाइथन में लोकलहोस्ट और कॉमन पोर्ट्स का स्कैनर बनाओ",
            "🧹 स्टोरेज साफ़ करें" to "फोन की स्टोरेज और टर्मक्स कैशे साफ़ करो",
            "🌐 वेब सर्वर चालू करें" to "पोर्ट 8080 पर पाइथन वेब सर्वर चालू करो",
            "🛡️ काली टूल्स चेक" to "काली नेटहंटर के टूल्स और एनवायरनमेंट चेक करो",
            "🔋 बैटरी स्टेटस" to "बैटरी परसेंटेज और सिस्टम मॉनिटर करो"
        )
    } else {
        listOf(
            "⚡ Termux Update Karo" to "Termux ko update aur upgrade karo with python",
            "🐍 Python Port Scanner" to "write a python port scanner for localhost and common ports",
            "🧹 Storage Saaf Karo" to "clean storage and temporary cache files in termux",
            "🌐 HTTP Server Chalao" to "start a python http file server daemon on port 8080",
            "🛡️ Kali NetHunter Tools" to "check kali nethunter security tools and environment",
            "📦 Git Auto Clone & Run" to "git clone and run automation script"
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberDark)
            .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Row: Language Toggle & Auto-Run Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hindi / English Toggle Pills
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isHindi) NeonGreen.copy(alpha = 0.25f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (isHindi) NeonGreen else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onLanguageChange(VoiceLanguage.HINDI) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🇮🇳 हिन्दी",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isHindi) NeonGreen else TextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (!isHindi) CyberCyan.copy(alpha = 0.25f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (!isHindi) CyberCyan else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onLanguageChange(VoiceLanguage.HINGLISH) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🌐 English",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isHindi) CyberCyan else TextMuted
                    )
                }
            }

            // Engine status indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isListening) TerminalRed else if (isGenerating) TerminalYellow else NeonGreen)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = when {
                        isListening -> if (isHindi) "सुन रहा हूँ..." else "LISTENING..."
                        isGenerating -> "CODEX REASONING..."
                        isAutoExecute -> if (isHindi) "ऑटो-रन सक्रिय" else "AUTO-RUN ON"
                        else -> if (isHindi) "वॉइस तैयार" else "VOICE READY"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = when {
                        isListening -> TerminalRed
                        isGenerating -> TerminalYellow
                        else -> NeonGreen
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Voice Waveform & Glowing Mic Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isHindi) "प्राकृतिक हिन्दी वॉइस ऑटोमेशन" else "NATURAL VOICE CODEX",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyberCyan
                )

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (voiceState) {
                        is VoiceState.Listening -> if (isHindi) "बोलिए (जैसे 'टर्मक्स अपडेट करो')..." else "Speak now (e.g. 'Termux update karo')..."
                        is VoiceState.Processing -> if (isHindi) "आवाज़ प्रोसेस हो रही है..." else "Processing voice command..."
                        is VoiceState.Recognized -> "\"${voiceState.text}\""
                        is VoiceState.Error -> voiceState.error
                        VoiceState.Idle -> if (isHindi) "माइक दबाकर हिन्दी में बोलें" else "Tap mic & speak voice command"
                    },
                    fontSize = 12.sp,
                    color = if (voiceState is VoiceState.Error) TerminalRed else TextSecondary,
                    maxLines = 2
                )
            }

            // Glowing Mic Trigger
            VoiceMicButton(
                isListening = isListening,
                rmsLevel = rmsLevel,
                isGenerating = isGenerating,
                onClick = {
                    if (isListening) onStopListening() else onStartListening()
                }
            )
        }

        // Animated Soundwave when listening
        if (isListening) {
            Spacer(modifier = Modifier.height(12.dp))
            VoiceWaveformVisualizer(rmsLevel = rmsLevel)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Input Field & Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = promptText,
                onValueChange = onPromptChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("prompt_input_field"),
                placeholder = {
                    Text(
                        if (isHindi) "हिन्दी या हिंगलिश में बोलें या टाइप करें..." else "Ask Codex in Hindi or English...",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                },
                singleLine = false,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = CyberCyan,
                    focusedContainerColor = CyberSurface,
                    unfocusedContainerColor = CyberSurface
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { onGenerate(promptText) },
                enabled = promptText.isNotBlank() && !isGenerating,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (promptText.isNotBlank() && !isGenerating)
                            Brush.linearGradient(listOf(NeonGreen, CyberCyan))
                        else
                            Brush.linearGradient(listOf(CyberSurface, CyberSurface))
                    )
                    .testTag("submit_prompt_button")
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = CyberCyan
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Submit Prompt",
                        tint = if (promptText.isNotBlank()) Color.Black else TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Preset Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(quickPrompts) { (label, command) ->
                SuggestionChip(
                    onClick = {
                        onPromptChange(command)
                        onGenerate(command)
                    },
                    label = {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = CyberSurface
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        borderColor = CyberBorder,
                        borderWidth = 1.dp,
                        enabled = true
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}

@Composable
fun VoiceMicButton(
    isListening: Boolean,
    rmsLevel: Float,
    isGenerating: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 500 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .size(56.dp)
            .scale(if (isListening) pulseScale else 1f)
            .clip(CircleShape)
            .background(
                brush = when {
                    isListening -> Brush.radialGradient(listOf(TerminalRed, Color(0xFFB71C1C)))
                    isGenerating -> Brush.radialGradient(listOf(TerminalYellow, Color(0xFFF57F17)))
                    else -> Brush.radialGradient(listOf(NeonGreen, CyberCyan))
                }
            )
            .border(
                width = 2.dp,
                color = if (isListening) Color.White else CyberCyan,
                shape = CircleShape
            )
            .clickable { onClick() }
            .testTag("voice_mic_button"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
            contentDescription = if (isListening) "Stop Listening" else "Start Voice Command",
            tint = if (isListening) Color.White else Color.Black,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun VoiceWaveformVisualizer(rmsLevel: Float) {
    val bars = 9
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CyberSurface)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until bars) {
            val animHeight by infiniteTransition.animateFloat(
                initialValue = 4f,
                targetValue = (10f + (i * 3 % 16) + (rmsLevel.coerceAtLeast(0f) * 2)).coerceIn(6f, 24f),
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 200 + (i * 40), easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$i"
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(animHeight.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                NeonGreen,
                                CyberCyan
                            )
                        )
                    )
            )
        }
    }
}
