package com.example.agent.security

import com.example.agent.model.PermissionLevel
import com.example.agent.model.SecurityActionRequest

data class SecurityCheckResult(
    val level: PermissionLevel,
    val isAllowedImmediately: Boolean,
    val reason: String,
    val request: SecurityActionRequest? = null
)

class SecurityPolicyEngine {

    // Denied critical patterns that should never be executed automatically
    private val denyPatterns = listOf(
        Regex("""\brm\s+-rf\s+(/|/\*|/system.*|/data.*|/boot.*)\b"""),
        Regex("""\bmkfs\b"""),
        Regex("""\bdd\s+if=.*of=/dev/.*"""),
        Regex("""\b:()\s*\{\s*:\|:&\s*\};\s*:""") // Fork bomb
    )

    // Patterns that require explicit user confirmation (PROMPT)
    private val promptPatterns = listOf(
        Regex("""\brm\s+-r.*"""),
        Regex("""\brm\s+"""),
        Regex("""\bsudo\b"""),
        Regex("""\bsu\b"""),
        Regex("""\bnethunter\b"""),
        Regex("""\bchroot\b"""),
        Regex("""\bchmod\s+[0-7]{3,4}\b"""),
        Regex("""\bchown\b"""),
        Regex("""\bcurl\b.*\|\s*(bash|sh)\b"""),
        Regex("""\bwget\b.*\|\s*(bash|sh)\b"""),
        Regex("""\bpkill\b"""),
        Regex("""\bkill\s+-9\b"""),
        Regex("""\bkillall\b"""),
        Regex("""\biptables\b"""),
        Regex("""\bnc\s+-l.*"""), // Listen socket
        Regex("""\b(reboot|poweroff|halt)\b""")
    )

    private val auditLog = mutableListOf<String>()

    fun getAuditLog(): List<String> = auditLog.toList()

    fun evaluateCommand(command: String): SecurityCheckResult {
        val trimmed = command.trim()

        // 1. Check DENY
        for (pattern in denyPatterns) {
            if (pattern.containsMatchIn(trimmed)) {
                val reason = "BLOCKED: Command matches restricted system safety blacklist ($pattern)"
                logAudit("DENIED: $command -> $reason")
                return SecurityCheckResult(
                    level = PermissionLevel.DENY,
                    isAllowedImmediately = false,
                    reason = reason
                )
            }
        }

        // 2. Check PROMPT
        for (pattern in promptPatterns) {
            if (pattern.containsMatchIn(trimmed)) {
                val reason = "Security policy requires user confirmation for privileged or destructive command: $trimmed"
                val req = SecurityActionRequest(
                    actionType = "EXECUTE_SHELL",
                    commandOrTarget = trimmed,
                    reason = reason,
                    level = PermissionLevel.PROMPT
                )
                logAudit("PROMPTED: $command -> $reason")
                return SecurityCheckResult(
                    level = PermissionLevel.PROMPT,
                    isAllowedImmediately = false,
                    reason = reason,
                    request = req
                )
            }
        }

        // 3. SAFE
        logAudit("SAFE: $command (Auto-approved)")
        return SecurityCheckResult(
            level = PermissionLevel.SAFE,
            isAllowedImmediately = true,
            reason = "Deterministic or non-destructive command"
        )
    }

    fun evaluateFileOperation(action: String, path: String): SecurityCheckResult {
        // Forbidden paths
        if (path.startsWith("/system") || path.startsWith("/vendor") || path.startsWith("/apex")) {
            val reason = "BLOCKED: Modifying read-only system partitions is prohibited."
            logAudit("DENIED FILE: $action on $path")
            return SecurityCheckResult(
                level = PermissionLevel.DENY,
                isAllowedImmediately = false,
                reason = reason
            )
        }

        if (action.uppercase() == "DELETE" || action.uppercase() == "WIPE") {
            val reason = "Destructive file deletion of '$path' requires authorization."
            val req = SecurityActionRequest(
                actionType = "DELETE_FILE",
                commandOrTarget = path,
                reason = reason,
                level = PermissionLevel.PROMPT
            )
            logAudit("PROMPTED FILE: $action on $path")
            return SecurityCheckResult(
                level = PermissionLevel.PROMPT,
                isAllowedImmediately = false,
                reason = reason,
                request = req
            )
        }

        if (action.uppercase() == "OVERWRITE") {
            val reason = "Overwriting existing code file '$path' requires authorization."
            val req = SecurityActionRequest(
                actionType = "OVERWRITE_FILE",
                commandOrTarget = path,
                reason = reason,
                level = PermissionLevel.PROMPT
            )
            logAudit("PROMPTED FILE: $action on $path")
            return SecurityCheckResult(
                level = PermissionLevel.PROMPT,
                isAllowedImmediately = false,
                reason = reason,
                request = req
            )
        }

        return SecurityCheckResult(
            level = PermissionLevel.SAFE,
            isAllowedImmediately = true,
            reason = "Safe file operation"
        )
    }

    private fun logAudit(entry: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        auditLog.add("[$timestamp] $entry")
        if (auditLog.size > 200) {
            auditLog.removeAt(0)
        }
    }
}
