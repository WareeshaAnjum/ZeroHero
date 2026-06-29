package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_topic_addons")
data class StudyTopicAddon(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dossierId: Int,
    val topicName: String,
    val notes: String = "",
    val youtubeLinks: String = "", // Newline separated URLs
    val isCompleted: Boolean = false
)
