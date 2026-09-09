package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {
    @Query("SELECT * FROM saved_scripts ORDER BY timestamp DESC")
    fun getAllScripts(): Flow<List<SavedScript>>

    @Query("SELECT * FROM saved_scripts WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteScripts(): Flow<List<SavedScript>>

    @Query("SELECT * FROM saved_scripts WHERE id = :id LIMIT 1")
    suspend fun getScriptById(id: Long): SavedScript?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: SavedScript): Long

    @Update
    suspend fun updateScript(script: SavedScript)

    @Delete
    suspend fun deleteScript(script: SavedScript)

    @Query("DELETE FROM saved_scripts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
