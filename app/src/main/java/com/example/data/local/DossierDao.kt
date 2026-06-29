package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Dossier
import com.example.data.model.StudyTopicAddon
import kotlinx.coroutines.flow.Flow

@Dao
interface DossierDao {
    @Query("SELECT * FROM dossiers ORDER BY timestamp DESC")
    fun getAllDossiers(): Flow<List<Dossier>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDossier(dossier: Dossier): Long

    @Query("DELETE FROM dossiers WHERE id = :id")
    suspend fun deleteDossierById(id: Int)

    @Query("DELETE FROM dossiers")
    suspend fun deleteAllDossiers()

    // --- Study Topic Addons (Notes & Links) ---
    @Query("SELECT * FROM study_topic_addons")
    fun getAllAddons(): Flow<List<StudyTopicAddon>>

    @Query("SELECT * FROM study_topic_addons WHERE dossierId = :dossierId")
    fun getAddonsForDossier(dossierId: Int): Flow<List<StudyTopicAddon>>

    @Query("SELECT * FROM study_topic_addons WHERE dossierId = :dossierId AND topicName = :topicName LIMIT 1")
    suspend fun getAddonByTopic(dossierId: Int, topicName: String): StudyTopicAddon?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyTopicAddon(addon: StudyTopicAddon): Long

    @Query("DELETE FROM study_topic_addons WHERE dossierId = :dossierId")
    suspend fun deleteAddonsForDossier(dossierId: Int)
}
