package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val type: String, // "DO" (ماذا أفعل), "INVENT" (ماذا أخترع), "DESIGN" (ماذا أصمم)
    val difficulty: Int, // 1 to 5
    val technologies: String, // Comma-separated list
    val progress: Int = 0, // 0 to 100
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val reminderTime: String? = null, // "18:00"
    val steps: String = "", // Pipe separated: "خطوة 1|خطوة 2"
    val completedSteps: String = "", // Pipe separated indices of completed steps, e.g., "0|2"
    val imageUrl: String? = null // local file path to the generated expressive image
)
