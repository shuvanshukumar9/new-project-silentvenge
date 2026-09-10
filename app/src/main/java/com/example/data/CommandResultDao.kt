package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for logged command output history.
 */
@Dao
interface CommandResultDao {

    @Query("SELECT * FROM command_results ORDER BY timestamp DESC")
    fun getAllCommandResults(): Flow<List<CommandResultEntity>>

    @Query("SELECT * FROM command_results ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentCommandResults(limit: Int = 50): Flow<List<CommandResultEntity>>

    @Query("SELECT * FROM command_results WHERE id = :id")
    suspend fun getCommandResultById(id: Long): CommandResultEntity?

    @Query("SELECT * FROM command_results WHERE command LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchCommandResults(query: String): Flow<List<CommandResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommandResult(result: CommandResultEntity): Long

    @Query("DELETE FROM command_results WHERE id = :id")
    suspend fun deleteCommandResultById(id: Long)

    @Query("DELETE FROM command_results")
    suspend fun clearAllCommandResults()

    @Query("SELECT COUNT(*) FROM command_results")
    suspend fun getCommandResultCount(): Int
}
