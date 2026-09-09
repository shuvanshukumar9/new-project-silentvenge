package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TerminalRed
import com.example.ui.theme.TerminalYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GuidesScreen(
    isTermuxInstalled: Boolean,
    isKaliInstalled: Boolean,
    onVoiceSampleClick: (String) -> Unit,
    onNotify: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("guides_screen_container")
    ) {
        // Status Overview
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberDark),
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(CyberBorder, CyberCyan.copy(alpha = 0.3f)))
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "DEVICE ENVIRONMENT STATUS",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Termux Package (com.termux)", fontSize = 12.sp, color = TextPrimary)
                    Text(
                        text = if (isTermuxInstalled) "INSTALLED ✓" else "NOT INSTALLED (Sandbox Active)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isTermuxInstalled) NeonGreen else TerminalYellow
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Kali NetHunter (com.offsec)", fontSize = 12.sp, color = TextPrimary)
                    Text(
                        text = if (isKaliInstalled) "INSTALLED ✓" else "VIA PROOT / TERMUX",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isKaliInstalled) NeonGreen else TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section: How Voice Automation Works
        Text(
            text = "HOW VOICE AUTOMATION WORKS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = NeonGreen
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberDark),
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(CyberBorder, CyberBorder))
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                StepItem(
                    number = "1",
                    title = "Tap Voice Mic & Speak",
                    description = "Speak naturally in Hindi, Hinglish, or English. E.g. \"Termux update karo aur python install karo\"."
                )
                Spacer(modifier = Modifier.height(10.dp))
                StepItem(
                    number = "2",
                    title = "Mini Codex Analyzes & Writes Code",
                    description = "The AI designs an optimized Bash script, Python file, or shell command sequence."
                )
                Spacer(modifier = Modifier.height(10.dp))
                StepItem(
                    number = "3",
                    title = "Auto-Execution & Live Console",
                    description = "When Auto-Execute is enabled, it automatically executes the script in the built-in Terminal or sends it to Termux!"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Sample Voice Prompts (Tap to try)
        Text(
            text = "TRY VOICE COMMANDS (हिन्दी व हिंगलिश - TAP TO TRY)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = TerminalYellow
        )
        Spacer(modifier = Modifier.height(8.dp))

        val voiceSamples = listOf(
            "टर्मक्स अपडेट करो और पाइथन इनस्टॉल करो" to "🇮🇳 हिन्दी",
            "लोकलहोस्ट और नेटवर्क पोर्ट स्कैनर बनाओ" to "🇮🇳 हिन्दी",
            "फोन की स्टोरेज और टर्मक्स कैशे साफ़ करो" to "🇮🇳 हिन्दी",
            "काली नेटहंटर के टूल्स और एनवायरनमेंट चेक करो" to "🇮🇳 हिन्दी",
            "पोर्ट 8080 पर तुरंत लोकल वेब सर्वर चालू करो" to "🇮🇳 हिन्दी",
            "बैटरी स्टेटस और सीपीयू टेम्परेचर मॉनिटर करो" to "🇮🇳 हिन्दी",
            "Termux me python update karo aur pip upgrade karo" to "🌐 Hinglish",
            "Write a python port scanner for localhost" to "🌐 English",
            "Start a python http server daemon on port 8080" to "🌐 English"
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            voiceSamples.forEach { (sample, tag) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberSurface)
                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                        .clickable { onVoiceSampleClick(sample) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎙️",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "\"$sample\"",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CyberSurfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NeonGreen
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Termux Setup for External App Permission
        Text(
            text = "TERMUX BACKGROUND EXECUTION SETUP",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = CyberCyan
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberDark),
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(CyberBorder, CyberBorder))
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "To allow Termux to accept RUN_COMMAND intents automatically from external apps like KaliDroid Codex:",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                val setupCode = "mkdir -p ~/.termux && echo \"allow-external-apps = true\" >> ~/.termux/termux.properties"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberSurfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = setupCode,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = NeonGreen,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Termux config", setupCode))
                            onNotify("Termux config command copied!")
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = CyberCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Then run 'termux-reload-settings' in Termux.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun StepItem(number: String, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(CyberSurfaceVariant)
                .border(1.dp, CyberCyan, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = CyberCyan
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}
