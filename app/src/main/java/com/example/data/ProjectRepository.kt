package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val chatSessionDao: ChatSessionDao,
    context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mubtakir_prefs", Context.MODE_PRIVATE)

    val activeProjects: Flow<List<Project>> = projectDao.getActiveProjectsFlow()
    val completedProjects: Flow<List<Project>> = projectDao.getCompletedProjectsFlow()
    val allProjects: Flow<List<Project>> = projectDao.getAllProjectsFlow()
    val activeCount: Flow<Int> = projectDao.getActiveCountFlow()
    val completedCount: Flow<Int> = projectDao.getCompletedCountFlow()

    // --- Chat Sessions ---
    val allChatSessions: Flow<List<ChatSession>> = chatSessionDao.getAllSessionsFlow()

    suspend fun insertChatSession(session: ChatSession): Long {
        return chatSessionDao.insertSession(session)
    }

    suspend fun updateChatSession(session: ChatSession) {
        chatSessionDao.updateSession(session)
    }

    suspend fun deleteChatSession(session: ChatSession) {
        chatSessionDao.deleteSession(session)
    }

    suspend fun getChatSessionById(id: Int): ChatSession? {
        return chatSessionDao.getSessionById(id)
    }

    suspend fun clearAllChatSessions() {
        chatSessionDao.deleteAllSessions()
    }

    suspend fun clearAllProjects() {
        projectDao.deleteAllProjects()
    }

    suspend fun insertProject(project: Project): Long {
        return projectDao.insertProject(project)
    }

    suspend fun updateProject(project: Project) {
        projectDao.updateProject(project)
    }

    suspend fun deleteProject(project: Project) {
        projectDao.deleteProject(project)
    }

    suspend fun getProjectById(id: Int): Project? {
        return projectDao.getProjectById(id)
    }

    // --- Profile Interests and Skills Storage ---
    fun getInterests(): Set<String> {
        return prefs.getStringSet("interests", setOf("تطوير التطبيقات", "الذكاء الاصطناعي", "التصميم البصري")) ?: emptySet()
    }

    fun saveInterests(interests: Set<String>) {
        prefs.edit().putStringSet("interests", interests).apply()
    }

    fun getSkills(): Set<String> {
        return prefs.getStringSet("skills", setOf("Kotlin", "Jetpack Compose", "UI/UX", "Figma")) ?: emptySet()
    }

    fun saveSkills(skills: Set<String>) {
        prefs.edit().putStringSet("skills", skills).apply()
    }

    // --- Cloud Sync Simulated Status ---
    fun isCloudSyncEnabled(): Boolean {
        return prefs.getBoolean("cloud_sync_enabled", true)
    }

    fun setCloudSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("cloud_sync_enabled", enabled).apply()
    }

    fun getLastSyncTime(): Long {
        return prefs.getLong("last_sync_time", System.currentTimeMillis() - 4 * 60 * 1000)
    }

    fun setLastSyncTime(time: Long) {
        prefs.edit().putLong("last_sync_time", time).apply()
    }

    // --- Dark Theme Storage ---
    fun isDarkModeEnabled(): Boolean? {
        if (!prefs.contains("dark_mode_enabled")) return null
        return prefs.getBoolean("dark_mode_enabled", false)
    }

    fun saveDarkModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode_enabled", enabled).apply()
    }

    // --- AI Evaluation Storage ---
    fun getAiEvaluation(): String? {
        return prefs.getString("ai_evaluation", null)
    }

    fun saveAiEvaluation(json: String) {
        prefs.edit().putString("ai_evaluation", json).apply()
    }

    // --- Code Analysis Storage ---
    fun getCodeAnalysis(): String? {
        return prefs.getString("code_analysis", null)
    }

    fun saveCodeAnalysis(json: String) {
        prefs.edit().putString("code_analysis", json).apply()
    }

    // --- Generated Idea Storage ---
    fun getSavedGeneratedIdea(): String? {
        return prefs.getString("saved_generated_idea", null)
    }

    fun saveGeneratedIdea(json: String?) {
        if (json == null) {
            prefs.edit().remove("saved_generated_idea").apply()
        } else {
            prefs.edit().putString("saved_generated_idea", json).apply()
        }
    }

    suspend fun resetRepository() {
        chatSessionDao.deleteAllSessions()
        projectDao.deleteAllProjects()
        prefs.edit().clear().apply()
    }
}
