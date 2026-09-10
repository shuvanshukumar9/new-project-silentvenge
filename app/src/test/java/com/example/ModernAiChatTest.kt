package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.jarvis.JarvisAgentType
import com.example.jarvis.JarvisChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModernAiChatTest {

    @Test
    fun testJarvisChatMessageDefaults() {
        val userMsg = JarvisChatMessage(
            message = "Hello AI",
            isUser = true
        )

        assertEquals("Hello AI", userMsg.message)
        assertTrue(userMsg.isUser)
        assertEquals(JarvisAgentType.JARVIS_CORE, userMsg.senderType)
        assertNull(userMsg.taskStatus)
        assertNull(userMsg.commandOutput)
        assertNull(userMsg.runCommand)
        assertFalse(userMsg.isRunning)
        assertFalse(userMsg.isError)
        assertEquals(0L, userMsg.executionDurationMs)
    }

    @Test
    fun testJarvisChatMessageWithBackgroundExecutionOutput() {
        val taskMsg = JarvisChatMessage(
            senderType = JarvisAgentType.JARVIS_CORE,
            message = "Task completed successfully.",
            isUser = false,
            runCommand = "ls -la",
            taskStatus = "Completed in 32ms",
            commandOutput = "total 12\ndrwxr-xr-x 2 u0_a123 u0_a123 4096 workspace",
            executionDurationMs = 32L,
            isRunning = false,
            isError = false
        )

        assertNotNull(taskMsg.id)
        assertFalse(taskMsg.isUser)
        assertEquals("ls -la", taskMsg.runCommand)
        assertEquals("Completed in 32ms", taskMsg.taskStatus)
        assertTrue(taskMsg.commandOutput!!.contains("workspace"))
        assertEquals(32L, taskMsg.executionDurationMs)
        assertFalse(taskMsg.isRunning)
        assertFalse(taskMsg.isError)
    }

    @Test
    fun testJarvisChatMessageErrorState() {
        val errorMsg = JarvisChatMessage(
            senderType = JarvisAgentType.JARVIS_CORE,
            message = "Task encountered an error (exit code 127).",
            isUser = false,
            runCommand = "unknown_command",
            taskStatus = "Failed (exit 127)",
            commandOutput = "sh: unknown_command: not found",
            executionDurationMs = 15L,
            isRunning = false,
            isError = true
        )

        assertTrue(errorMsg.isError)
        assertEquals("Failed (exit 127)", errorMsg.taskStatus)
        assertTrue(errorMsg.commandOutput!!.contains("not found"))
    }
}
