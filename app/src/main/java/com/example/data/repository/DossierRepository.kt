package com.example.data.repository

import com.example.data.local.DossierDao
import com.example.data.model.Dossier
import com.example.data.model.StudyTopicAddon
import kotlinx.coroutines.flow.Flow

class DossierRepository(private val dossierDao: DossierDao) {
    val allDossiers: Flow<List<Dossier>> = dossierDao.getAllDossiers()

    suspend fun insert(dossier: Dossier): Long {
        return dossierDao.insertDossier(dossier)
    }

    suspend fun deleteById(id: Int) {
        dossierDao.deleteDossierById(id)
        dossierDao.deleteAddonsForDossier(id)
    }

    suspend fun update(dossier: Dossier) {
        dossierDao.insertDossier(dossier)
    }

    suspend fun deleteAll() {
        dossierDao.deleteAllDossiers()
    }

    // --- Study Topic Addons (Notes & Links) ---
    fun getAllAddons(): Flow<List<StudyTopicAddon>> {
        return dossierDao.getAllAddons()
    }

    fun getAddonsForDossier(dossierId: Int): Flow<List<StudyTopicAddon>> {
        return dossierDao.getAddonsForDossier(dossierId)
    }

    suspend fun getAddonByTopic(dossierId: Int, topicName: String): StudyTopicAddon? {
        return dossierDao.getAddonByTopic(dossierId, topicName)
    }

    suspend fun saveStudyTopicAddon(addon: StudyTopicAddon) {
        dossierDao.insertStudyTopicAddon(addon)
    }
}
