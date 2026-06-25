package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getActiveProjectsFlow(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedProjectsFlow(): Flow<List<Project>>

    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjectsFlow(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Int): Project?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Update
    suspend fun updateProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)

    @Query("DELETE FROM projects")
    suspend fun deleteAllProjects()

    @Query("SELECT COUNT(*) FROM projects WHERE isCompleted = 1")
    fun getCompletedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM projects WHERE isCompleted = 0")
    fun getActiveCountFlow(): Flow<Int>
}
