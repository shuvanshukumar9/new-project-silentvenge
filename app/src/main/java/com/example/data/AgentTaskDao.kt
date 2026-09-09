package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.agent.model.AgentTask
import com.example.agent.model.ServerDaemon
import com.example.agent.model.TaskStep
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentTaskDao {

    @Query("SELECT * FROM agent_tasks ORDER BY updatedAt DESC")
    fun getAllTasks(): Flow<List<AgentTask>>

    @Query("SELECT * FROM agent_tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): AgentTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: AgentTask): Long

    @Update
    suspend fun updateTask(task: AgentTask)

    @Query("DELETE FROM agent_tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: Long)

    // Steps
    @Query("SELECT * FROM task_steps WHERE taskId = :taskId ORDER BY stepIndex ASC")
    fun getStepsForTask(taskId: Long): Flow<List<TaskStep>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: TaskStep): Long

    @Query("DELETE FROM task_steps WHERE taskId = :taskId")
    suspend fun deleteStepsForTask(taskId: Long)

    // Daemons
    @Query("SELECT * FROM server_daemons ORDER BY isRunning DESC, startedAt DESC")
    fun getAllDaemons(): Flow<List<ServerDaemon>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDaemon(daemon: ServerDaemon): Long

    @Update
    suspend fun updateDaemon(daemon: ServerDaemon)

    @Query("DELETE FROM server_daemons WHERE id = :daemonId")
    suspend fun deleteDaemon(daemonId: Long)
}
