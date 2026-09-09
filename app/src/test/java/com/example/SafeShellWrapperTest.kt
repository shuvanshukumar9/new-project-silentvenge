package com.example.executor

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SafeShellWrapperTest {

    private lateinit var context: Context
    private lateinit var shellWrapper: SafeShellWrapper

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        shellWrapper = SafeShellWrapper(context)
    }

    @Test
    fun testExecuteCommandSuccess() = runBlocking {
        val stdoutLines = mutableListOf<String>()
        var started = false
        var completed = false

        val callback = object : ExecutionStreamCallback {
            override fun onCommandStarted(command: String, backend: ExecutionBackend) {
                started = true
                assertEquals(ExecutionBackend.LOCAL_PROCESS, backend)
            }

            override fun onStdoutLine(line: String) {
                stdoutLines.add(line)
            }

            override fun onCommandCompleted(result: CommandResult) {
                completed = true
                assertTrue(result.isSuccess)
                assertEquals(0, result.exitCode)
            }
        }

        val result = shellWrapper.executeCommand("echo 'Hello SilentVenge'", callback = callback)

        assertTrue(started)
        assertTrue(completed)
        assertTrue(result.isSuccess)
        assertTrue(result.stdout.contains("Hello SilentVenge"))
        assertTrue(stdoutLines.any { it.contains("Hello SilentVenge") })
    }

    @Test
    fun testExecuteScript() = runBlocking {
        val scriptContent = """
            #!/bin/sh
            echo "Line 1"
            echo "Line 2"
        """.trimIndent()

        val output = shellWrapper.executeScript("test_run.sh", scriptContent)

        assertEquals(0, output.exitCode)
        assertTrue(output.stdout.contains("Line 1"))
        assertTrue(output.stdout.contains("Line 2"))
    }

    @Test
    fun testNonZeroExitCode() = runBlocking {
        var errorReported = false
        val callback = object : ExecutionStreamCallback {
            override fun onCommandCompleted(result: CommandResult) {
                assertFalse(result.isSuccess)
                assertNotEquals(0, result.exitCode)
                errorReported = true
            }
        }

        val result = shellWrapper.executeCommand("exit 42", callback = callback)
        assertEquals(42, result.exitCode)
        assertTrue(errorReported)
    }
}
