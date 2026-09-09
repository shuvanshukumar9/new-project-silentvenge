package com.example.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiCodexService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateScript(
        prompt: String,
        preferredEnv: String = "Termux",
        customApiKey: String? = null
    ): CodexResult = withContext(Dispatchers.IO) {
        val key = if (!customApiKey.isNullOrBlank()) customApiKey else BuildConfig.GEMINI_API_KEY

        if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
            Log.d("GeminiCodex", "No Gemini API key supplied, using smart offline Codex generator.")
            return@withContext generateOfflineTemplate(prompt, preferredEnv)
        }

        try {
            val systemPrompt = """
                You are KaliDroid Codex, an autonomous voice-controlled terminal and automation assistant for Android devices, Termux, and Kali Linux NetHunter.
                NATURAL LANGUAGE SPECIFICATION:
                - You have native proficiency in Hindi (Devanagari script: 'टर्मक्स अपडेट करो', 'पोर्ट स्कैनर बनाओ', 'स्टोरेज साफ़ करो'), Romanized Hindi / Hinglish ('termux update karo', 'python me local port scanner likho', 'mobile storage saaf karne ka script'), and English.
                - Understand intent precisely regardless of whether the user speaks in Hindi, Hinglish, or English.
                - When the user asks in Hindi or Hinglish, provide the 'title' and 'summary' in natural, conversational Hindi or Hinglish so the Text-to-Speech engine can speak back naturally.
                
                Preferred Environment: $preferredEnv
                
                You MUST respond with a VALID JSON object (and no additional surrounding text) matching this schema:
                {
                  "title": "Short title describing the automation (in Hindi/Hinglish if requested in Hindi)",
                  "targetEnv": "$preferredEnv",
                  "language": "bash" or "python" or "sh",
                  "code": "Full executable code or bash commands without placeholder comments",
                  "runCommand": "Single command to run it, e.g. 'bash script.sh' or 'python3 script.py' or 'pkg update && pkg install ...'",
                  "summary": "Clear, concise 1-2 sentence explanation of what will execute (in Hindi/Hinglish if requested in Hindi)",
                  "requiredPackages": "Comma-separated packages, e.g. python, git, curl, nmap",
                  "executionInstructions": "Brief note on execution in Termux or Kali"
                }
            """.trimIndent()

            val requestBodyJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "$systemPrompt\n\nUser Request: $prompt")
                            })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)

                val genConfig = JSONObject().apply {
                    put("temperature", 0.2)
                    put("topP", 0.9)
                }
                put("generationConfig", genConfig)
            }

            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$key"
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val rawBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.w("GeminiCodex", "Gemini API error: ${response.code} $rawBody")
                return@withContext generateOfflineTemplate(prompt, preferredEnv)
            }

            val responseJson = JSONObject(rawBody)
            val candidates = responseJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: ""

            parseCodexJson(text, prompt, preferredEnv)
        } catch (e: Exception) {
            Log.e("GeminiCodex", "Generation failed, falling back to smart template", e)
            generateOfflineTemplate(prompt, preferredEnv)
        }
    }

    private fun parseCodexJson(rawText: String, originalPrompt: String, preferredEnv: String): CodexResult {
        try {
            var cleaned = rawText.trim()
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.removePrefix("```json")
            } else if (cleaned.startsWith("```")) {
                cleaned = cleaned.removePrefix("```")
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.removeSuffix("```")
            }
            cleaned = cleaned.trim()

            // Find first { and last }
            val firstBrace = cleaned.indexOf('{')
            val lastBrace = cleaned.lastIndexOf('}')
            if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                cleaned = cleaned.substring(firstBrace, lastBrace + 1)
            }

            val obj = JSONObject(cleaned)
            return CodexResult(
                title = obj.optString("title", "Termux Automation Script"),
                targetEnv = obj.optString("targetEnv", preferredEnv),
                language = obj.optString("language", "bash"),
                code = obj.optString("code", "#!/bin/bash\necho 'Automation script ready'"),
                runCommand = obj.optString("runCommand", "bash script.sh"),
                summary = obj.optString("summary", "Automation script created based on your voice command."),
                requiredPackages = obj.optString("requiredPackages", ""),
                executionInstructions = obj.optString("executionInstructions", "Run in Termux or in-app terminal console.")
            )
        } catch (e: Exception) {
            Log.e("GeminiCodex", "Failed to parse json from: $rawText", e)
            return generateOfflineTemplate(originalPrompt, preferredEnv)
        }
    }

    fun generateOfflineTemplate(prompt: String, preferredEnv: String): CodexResult {
        val lower = prompt.lowercase()
        val isHindiPrompt = prompt.any { it in '\u0900'..'\u097F' } ||
                lower.contains("karo") || lower.contains("banao") || lower.contains("likho") ||
                lower.contains("chalao") || lower.contains("saaf") || lower.contains("hai")

        return when {
            lower.contains("nmap") || lower.contains("port") || lower.contains("scan") ||
                    lower.contains("पोर्ट") || lower.contains("स्कैन") -> {
                CodexResult(
                    title = if (isHindiPrompt) "लोकलहोस्ट पोर्ट स्कैनर (Python)" else "Network & Port Scanner (Python)",
                    targetEnv = "Kali Linux",
                    language = "python",
                    code = """
                        #!/usr/bin/env python3
                        import socket
                        import sys
                        from datetime import datetime

                        target = "127.0.0.1"
                        common_ports = [21, 22, 53, 80, 443, 8080, 9000]

                        print(f"[+] Starting Scan on {target}")
                        print(f"[+] Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
                        print("-" * 45)

                        open_ports = []
                        for port in common_ports:
                            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                            s.settimeout(0.6)
                            res = s.connect_ex((target, port))
                            if res == 0:
                                print(f"[OPEN] Port {port} is active")
                                open_ports.append(port)
                            s.close()

                        print("-" * 45)
                        print(f"[✓] Scan Complete. {len(open_ports)} open port(s) discovered.")
                    """.trimIndent(),
                    runCommand = "python3 port_scanner.py",
                    summary = if (isHindiPrompt)
                        "लोकलहोस्ट और नेटवर्क पोर्ट्स स्कैन करने के लिए ऑटोमेटेड पाइथन स्क्रिप्ट तैयार है।"
                    else
                        "Localhost & common network ports scan karne ke liye automated Python script.",
                    requiredPackages = "python3",
                    executionInstructions = "Execute in Kali terminal or in-app sandbox runner."
                )
            }
            lower.contains("clean") || lower.contains("storage") || lower.contains("cache") || lower.contains("memory") ||
                    lower.contains("saaf") || lower.contains("साफ") || lower.contains("साफ़") || lower.contains("स्टोरेज") || lower.contains("कैशे") -> {
                CodexResult(
                    title = if (isHindiPrompt) "टर्मक्स और स्टोरेज क्लीनर" else "Android / Termux Storage & Cache Cleaner",
                    targetEnv = "Termux",
                    language = "bash",
                    code = """
                        #!/bin/bash
                        echo "[*] === Termux & Android Cleaner ==="
                        echo "[+] Checking current disk usage..."
                        df -h | head -n 8

                        echo "[+] Cleaning temporary /tmp directory..."
                        if [ -d "${'$'}PREFIX/tmp" ]; then
                            rm -rf "${'$'}PREFIX/tmp"/* 2>/dev/null
                            echo "[✓] Termux tmp cache cleared"
                        fi

                        echo "[+] Cleaning apt/pkg package caches..."
                        if command -v pkg &> /dev/null; then
                            pkg clean -y 2>/dev/null || true
                        fi

                        echo "[+] Finding large files (>50MB) in home directory..."
                        find ~ -type f -size +50M 2>/dev/null | head -n 5

                        echo "[✓] Storage cleanup completed successfully."
                    """.trimIndent(),
                    runCommand = "bash clean_storage.sh",
                    summary = if (isHindiPrompt)
                        "स्टोरेज और पैकेज कैशे साफ़ करने के लिए ऑटोमेशन क्लीनअप स्क्रिप्ट तैयार है।"
                    else
                        "Storage aur temporary package cache clean karne ke liye automated cleanup script.",
                    requiredPackages = "coreutils, findutils",
                    executionInstructions = "Run directly in Termux or Android Shell to free storage."
                )
            }
            lower.contains("battery") || lower.contains("temp") || lower.contains("cpu") ||
                    lower.contains("बैटरी") || lower.contains("तापमान") || lower.contains("सिस्टम") -> {
                CodexResult(
                    title = if (isHindiPrompt) "बैटरी और सिस्टम मॉनिटर" else "Battery & System Hardware Monitor",
                    targetEnv = "Termux",
                    language = "bash",
                    code = """
                        #!/bin/bash
                        echo "[*] === Device Hardware & Battery Status ==="
                        echo "[+] Date: $(date)"
                        echo "[+] Uptime: $(uptime 2>/dev/null || echo 'N/A')"
                        
                        if command -v termux-battery-status &> /dev/null; then
                            echo "[+] Termux Battery Status:"
                            termux-battery-status
                        else
                            echo "[+] Reading battery capacity from Android sysfs:"
                            cat /sys/class/power_supply/battery/capacity 2>/dev/null || echo "Battery: Available via Termux:API"
                        fi

                        echo "[+] Memory Status:"
                        cat /proc/meminfo | head -n 4
                        echo "[✓] System monitor check completed."
                    """.trimIndent(),
                    runCommand = "bash monitor.sh",
                    summary = if (isHindiPrompt)
                        "बैटरी स्टेटस और सीपीयू मेमोरी मॉनिटर करने का स्क्रिप्ट तैयार है।"
                    else
                        "Battery aur system health check karne ka automation script.",
                    requiredPackages = "termux-api, bash",
                    executionInstructions = "Run in Termux or in-app console."
                )
            }
            lower.contains("kali") || lower.contains("nethunter") || lower.contains("hack") || lower.contains("tool") ||
                    lower.contains("काली") || lower.contains("नेटहंटर") || lower.contains("टूल्स") -> {
                CodexResult(
                    title = if (isHindiPrompt) "काली नेटहंटर टूल्स चेकर" else "Kali NetHunter Essential Toolkit Checker",
                    targetEnv = "Kali Linux",
                    language = "bash",
                    code = """
                        #!/bin/bash
                        echo "[*] ========================================="
                        echo "[*]   Kali NetHunter Android Environment     "
                        echo "[*] ========================================="
                        echo "[+] Kernel Release: $(uname -r)"
                        echo "[+] Architecture: $(uname -m)"
                        echo "[+] User: $(whoami)"

                        tools=("nmap" "curl" "git" "python3" "net-tools" "tcpdump" "openssh")
                        echo -e "\n[+] Verifying installed security tools:"
                        for tool in "${'$'}{tools[@]}"; do
                            if command -v "${'$'}tool" &> /dev/null; then
                                echo -e "  [INSTALLED] ${'$'}tool -> $(which "${'$'}tool")"
                            else
                                echo -e "  [MISSING]   ${'$'}tool (Install via: apt install ${'$'}tool)"
                            fi
                        done

                        echo -e "\n[✓] Environment inspection complete."
                    """.trimIndent(),
                    runCommand = "bash kali_check.sh",
                    summary = if (isHindiPrompt)
                        "काली नेटहंटर के टूल्स और एनवायरनमेंट जांचने के लिए स्क्रिप्ट तैयार है।"
                    else
                        "Kali NetHunter security tools aur system readiness test karne ki automation script.",
                    requiredPackages = "apt, bash",
                    executionInstructions = "Run in Kali NetHunter chroot or in-app terminal."
                )
            }
            lower.contains("server") || lower.contains("http") || lower.contains("web") ||
                    lower.contains("सर्वर") || lower.contains("वेब") || lower.contains("पोर्ट 8080") -> {
                CodexResult(
                    title = if (isHindiPrompt) "लोकल पाइथन वेब सर्वर (Port 8080)" else "Quick Python HTTP File Server Daemon",
                    targetEnv = "Termux",
                    language = "python",
                    code = """
                        #!/usr/bin/env python3
                        import http.server
                        import socketserver
                        import os

                        PORT = 8080
                        Handler = http.server.SimpleHTTPRequestHandler

                        print(f"[*] Starting local HTTP server on port {PORT}...")
                        print(f"[*] Serving files from: {os.getcwd()}")
                        print(f"[*] Access URL: http://127.0.0.1:{PORT}")

                        try:
                            with socketserver.TCPServer(("", PORT), Handler) as httpd:
                                print("[✓] Server is live. Press Ctrl+C or Kill button to stop.")
                                httpd.serve_forever()
                        except KeyboardInterrupt:
                            print("\n[!] Server stopped by user.")
                    """.trimIndent(),
                    runCommand = "python3 server.py",
                    summary = if (isHindiPrompt)
                        "पोर्ट 8080 पर तुरंत लोकल फाइल सर्वर चालू करने के लिए पाइथन स्क्रिप्ट तैयार है।"
                    else
                        "Port 8080 par instant local web server start karne ke liye Python script.",
                    requiredPackages = "python3",
                    executionInstructions = "Run to share local files or host web dashboard."
                )
            }
            lower.contains("git") || lower.contains("clone") || lower.contains("repo") ||
                    lower.contains("गिट") || lower.contains("क्लोन") -> {
                CodexResult(
                    title = if (isHindiPrompt) "गिट ऑटो-सिंक और रनर" else "Git Auto-Sync & Automation Runner",
                    targetEnv = "Termux",
                    language = "bash",
                    code = """
                        #!/bin/bash
                        echo "[*] === Git Automation Sync ==="
                        if ! command -v git &> /dev/null; then
                            echo "[!] Git not found. Installing git..."
                            pkg install git -y
                        fi

                        echo "[+] Current Git Version: $(git --version)"
                        echo "[+] Active Directory: $(pwd)"
                        echo "[+] Status:"
                        git status 2>/dev/null || echo "Current folder is not a git repo yet."
                        echo "[✓] Git automation check complete."
                    """.trimIndent(),
                    runCommand = "bash git_sync.sh",
                    summary = if (isHindiPrompt)
                        "गिट रिपोजिटरी को क्लोन और मैनेज करने के लिए ऑटोमेशन स्क्रिप्ट।"
                    else
                        "Git repositories manage aur automate karne ke liye helper script.",
                    requiredPackages = "git",
                    executionInstructions = "Run in Termux environment."
                )
            }
            else -> {
                // Default Termux / Android setup & automation script
                CodexResult(
                    title = if (isHindiPrompt) "टर्मक्स कोर ऑटोमेशन और सेटअप" else "Termux Core Automation & Setup",
                    targetEnv = if (preferredEnv.isNotBlank()) preferredEnv else "Termux",
                    language = "bash",
                    code = """
                        #!/bin/bash
                        echo "[*] KaliDroid Codex: Running automated task..."
                        echo "[+] Host OS: $(uname -o 2>/dev/null || echo 'Android/Linux')"
                        echo "[+] User: $(id -un 2>/dev/null || echo 'u0_a...')"
                        echo "[+] Current Date: $(date)"
                        echo "----------------------------------------"

                        # Ensure packages are up to date
                        if command -v pkg &> /dev/null; then
                            echo "[+] Updating Termux package lists..."
                            pkg update -y
                            pkg install -y curl wget python
                        elif command -v apt &> /dev/null; then
                            echo "[+] Updating Kali Linux repositories..."
                            apt update -y
                        fi

                        echo "----------------------------------------"
                        echo "[✓] Automation task executed successfully."
                    """.trimIndent(),
                    runCommand = "bash setup_codex.sh",
                    summary = if (isHindiPrompt)
                        "टर्मक्स और काली एनवायरनमेंट को अपडेट और आवश्यक पैकेज इनस्टॉल करने का स्क्रिप्ट तैयार है।"
                    else
                        "Termux / Kali environment update aur core dependencies install karne ka automation script.",
                    requiredPackages = "bash, curl, python",
                    executionInstructions = "Execute in Termux, Kali or local Android Terminal runner."
                )
            }
        }
    }
}
