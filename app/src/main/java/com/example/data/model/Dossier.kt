package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dossiers")
data class Dossier(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topic: String,
    val mode: String, // "Exam", "Interview", "Presentation"
    val deadlineText: String,
    val materialText: String, // Store generated markdown/JSON content
    val companyBriefing: String = "", // Used for Interview mode
    val slideCount: Int = 5, // Used for Presentation mode
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getCompletionProgress(allAddons: List<StudyTopicAddon>): Pair<Int, Int> {
        val syllabusArray = try {
            org.json.JSONObject(materialText).optJSONArray("syllabus") ?: org.json.JSONArray()
        } catch (e: Exception) {
            org.json.JSONArray()
        }
        val totalTopics = syllabusArray.length()
        if (totalTopics == 0) return Pair(0, 0)

        var completedCount = 0
        val dossierAddons = allAddons.filter { it.dossierId == id }
        for (i in 0 until totalTopics) {
            val topicObj = syllabusArray.optJSONObject(i) ?: continue
            val topicName = topicObj.optString("topic", "")
            val isCompleted = dossierAddons.find { it.topicName == topicName }?.isCompleted == true
            if (isCompleted) {
                completedCount++
            }
        }
        return Pair(completedCount, totalTopics)
    }
}
