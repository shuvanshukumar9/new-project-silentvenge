package com.example

import com.example.ai.GeminiCodexService
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testHindiNaturalLanguageOfflineTemplates() {
        val service = GeminiCodexService()

        // Test Hindi port scanner query
        val portScanResult = service.generateOfflineTemplate("लोकलहोस्ट और नेटवर्क पोर्ट स्कैन करो", "Kali Linux")
        assertTrue(portScanResult.code.contains("socket"))
        assertTrue(portScanResult.summary.contains("स्कैन") || portScanResult.summary.contains("पोर्ट्स"))

        // Test Hindi storage cleaner query
        val cleanResult = service.generateOfflineTemplate("फोन की स्टोरेज और कैशे साफ़ करो", "Termux")
        assertTrue(cleanResult.code.contains("clean") || cleanResult.code.contains("rm -rf"))
        assertTrue(cleanResult.summary.contains("साफ़") || cleanResult.summary.contains("स्टोरेज"))

        // Test Hindi server query
        val serverResult = service.generateOfflineTemplate("पोर्ट 8080 पर वेब सर्वर चालू करो", "Termux")
        assertTrue(serverResult.code.contains("http.server") || serverResult.code.contains("8080"))

        // Test Hindi battery query
        val batteryResult = service.generateOfflineTemplate("बैटरी स्टेटस और सीपीयू चेक करो", "Termux")
        assertTrue(batteryResult.code.contains("battery"))

        // Test Hinglish query
        val hinglishResult = service.generateOfflineTemplate("termux update karo aur python install karo", "Termux")
        assertTrue(hinglishResult.code.contains("update") || hinglishResult.code.contains("pkg"))
    }
}
