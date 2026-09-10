package com.example.executor

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.AppDatabase
import com.example.data.CommandResultDao
import com.example.data.CommandResultEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TermuxCommandExecutionServiceTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var dao: CommandResultDao
    private lateinit var service: TermuxCommandExecutionService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.commandResultDao()
        service = TermuxCommandExecutionService(context, dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testExecuteCommandAndSaveToRoom() = runBlocking {
        val stdoutLines = mutableListOf<String>()

        val result = service.executeAndLog(
            command = "echo 'Testing ProcessBuilder Room Logging'",
            saveToDatabase = true,
            onStdoutLine = { stdoutLines.add(it) }
        )

        // Verify CommandResult
        assertEquals(0, result.exitCode)
        assertTrue(result.isSuccess)
        assertTrue(result.stdout.contains("Testing ProcessBuilder Room Logging"))
        assertTrue(stdoutLines.any { it.contains("Testing ProcessBuilder Room Logging") })

        // Verify Room persistence
        val history = dao.getAllCommandResults().first()
        assertEquals(1, history.size)
        val entity = history[0]
        assertEquals("echo 'Testing ProcessBuilder Room Logging'", entity.command)
        assertEquals(0, entity.exitCode)
        assertEquals("SUCCESS", entity.status)
        assertTrue(entity.isSuccess)
        assertTrue(entity.stdout.contains("Testing ProcessBuilder Room Logging"))
        assertTrue(entity.timestamp > 0)
    }

    @Test
    fun testClearHistoryInRoom() = runBlocking {
        service.executeAndLog("echo 'Item 1'", saveToDatabase = true)
        service.executeAndLog("echo 'Item 2'", saveToDatabase = true)

        var count = dao.getCommandResultCount()
        assertEquals(2, count)

        service.clearHistory()
        count = dao.getCommandResultCount()
        assertEquals(0, count)
    }

    @Test
    fun testEntityConversion() {
        val result = CommandResult(
            command = "whoami",
            exitCode = 0,
            stdout = "u0_a123",
            stderr = "",
            durationMs = 45,
            executedVia = ExecutionBackend.LOCAL_PROCESS
        )
        val entity = CommandResultEntity.fromCommandResult(result, workingDir = "/data/data/com.example/files")
        assertEquals("whoami", entity.command)
        assertEquals(0, entity.exitCode)
        assertEquals("SUCCESS", entity.status)
        assertEquals("/data/data/com.example/files", entity.workingDir)

        val restored = entity.toCommandResult()
        assertEquals(result.command, restored.command)
        assertEquals(result.exitCode, restored.exitCode)
        assertEquals(result.stdout, restored.stdout)
        assertEquals(result.executedVia, restored.executedVia)
    }
}
