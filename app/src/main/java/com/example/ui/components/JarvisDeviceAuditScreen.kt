package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.DeviceAuditReport
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TerminalRed
import com.example.ui.theme.TerminalYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun JarvisDeviceAuditScreen(
    auditReport: DeviceAuditReport,
    onRefreshAudit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📱", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DEVICE & DEV ECOSYSTEM AUDITOR",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = NeonGreen
                    )
                }
                Text(
                    text = "Hardware telemetry, RAM, storage & Termux/Kali status",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberSurfaceVariant)
                    .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                    .clickable { onRefreshAudit() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = NeonGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Refresh",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = NeonGreen
                    )
                }
            }
        }

        // Overall Health Score Hero Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CyberSurface)
                .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "JARVIS SYSTEM READINESS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when {
                        auditReport.healthScore >= 80 -> "OPTIMAL DEV ENVIRONMENT"
                        auditReport.healthScore >= 60 -> "MODERATE - RECOMMENDATIONS AVAILABLE"
                        else -> "DEGRADED - CRITICAL FIXES NEEDED"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${auditReport.deviceModel} • Android ${auditReport.androidVersion} (API ${auditReport.sdkInt})",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            // Score Circle
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            auditReport.healthScore >= 80 -> NeonGreen.copy(alpha = 0.15f)
                            auditReport.healthScore >= 60 -> TerminalYellow.copy(alpha = 0.15f)
                            else -> TerminalRed.copy(alpha = 0.15f)
                        }
                    )
                    .border(
                        2.dp,
                        when {
                            auditReport.healthScore >= 80 -> NeonGreen
                            auditReport.healthScore >= 60 -> TerminalYellow
                            else -> TerminalRed
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${auditReport.healthScore}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = when {
                            auditReport.healthScore >= 80 -> NeonGreen
                            auditReport.healthScore >= 60 -> TerminalYellow
                            else -> TerminalRed
                        }
                    )
                    Text(
                        text = "/100",
                        fontSize = 9.sp,
                        color = TextMuted
                    )
                }
            }
        }

        // Telemetry Grid: RAM & Storage
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // RAM Card
            val ramUsedPercent = if (auditReport.totalRamMb > 0)
                ((auditReport.totalRamMb - auditReport.availRamMb).toFloat() / auditReport.totalRamMb.toFloat())
            else 0.5f

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = "RAM",
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SYSTEM RAM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyan
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${auditReport.availRamMb} MB free",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "of ${auditReport.totalRamMb} MB total",
                    fontSize = 10.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { ramUsedPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (ramUsedPercent > 0.85f) TerminalRed else CyberCyan,
                    trackColor = CyberSurfaceVariant
                )
            }

            // Storage Card
            val storageUsedPercent = if (auditReport.storageTotalGb > 0)
                ((auditReport.storageTotalGb - auditReport.storageFreeGb) / auditReport.storageTotalGb).toFloat()
            else 0.5f

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Storage",
                        tint = NeonGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "STORAGE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = NeonGreen
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${auditReport.storageFreeGb} GB free",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "of ${auditReport.storageTotalGb} GB total",
                    fontSize = 10.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { storageUsedPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (storageUsedPercent > 0.9f) TerminalRed else NeonGreen,
                    trackColor = CyberSurfaceVariant
                )
            }
        }

        // Ecosystem Detection Status
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CyberSurface)
                .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "DEVELOPER APPS & EXTENSIONS DETECTED",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            val apps = listOf(
                Triple("Termux Terminal (com.termux)", auditReport.termuxInstalled, "Required for background commands"),
                Triple("Termux:API (com.termux.api)", auditReport.termuxApiInstalled, "Hardware sensor & telephony access"),
                Triple("Kali NetHunter (com.offsec.nethunter)", auditReport.kaliNetHunterInstalled, "Penetration testing & chroot")
            )

            apps.forEach { (name, installed, desc) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = desc,
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (installed) NeonGreen.copy(alpha = 0.2f) else TerminalRed.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (installed) "INSTALLED" else "MISSING",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (installed) NeonGreen else TerminalRed
                        )
                    }
                }
            }
        }

        // Insights & Recommendations
        if (auditReport.recommendations.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberSurface)
                    .border(1.dp, TerminalYellow.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "JARVIS ACTIONABLE RECOMMENDATIONS:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TerminalYellow
                )
                Spacer(modifier = Modifier.height(6.dp))

                auditReport.recommendations.forEach { rec ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("⚡ ", fontSize = 12.sp, color = TerminalYellow)
                        Text(rec, fontSize = 11.sp, color = TextPrimary, lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}
