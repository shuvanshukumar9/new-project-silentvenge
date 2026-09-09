package com.example.data

import kotlinx.coroutines.flow.Flow

class ScriptRepository(private val scriptDao: ScriptDao) {
    val allScripts: Flow<List<SavedScript>> = scriptDao.getAllScripts()
    val favoriteScripts: Flow<List<SavedScript>> = scriptDao.getFavoriteScripts()

    suspend fun insertScript(script: SavedScript): Long {
        return scriptDao.insertScript(script)
    }

    suspend fun updateScript(script: SavedScript) {
        scriptDao.updateScript(script)
    }

    suspend fun deleteScript(script: SavedScript) {
        scriptDao.deleteScript(script)
    }

    suspend fun getScriptById(id: Long): SavedScript? {
        return scriptDao.getScriptById(id)
    }
}
