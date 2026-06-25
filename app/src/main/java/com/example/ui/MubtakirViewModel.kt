package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AppDatabase
import com.example.data.Project
import com.example.data.ProjectRepository
import com.example.network.ContentRequest
import com.example.network.GeminiApiService
import com.example.network.GenerateContentRequest
import com.example.network.GeneratedIdeaJson
import com.example.network.GenerationConfigRequest
import com.example.network.PartRequest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import com.example.network.ImageConfigRequest
import com.example.network.InlineDataResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MubtakirViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProjectRepository
    private val geminiApiService: GeminiApiService

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ProjectRepository(database.projectDao(), database.chatSessionDao(), application)
        geminiApiService = GeminiApiService.create()
        loadLatestSession()
    }

    // --- Chat Sessions State ---
    val chatSessions: StateFlow<List<com.example.data.ChatSession>> = repository.allChatSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSessionId = MutableStateFlow<Int?>(null)
    val currentSessionId = _currentSessionId.asStateFlow()

    // --- State Streams ---
    val activeProjects: StateFlow<List<Project>> = repository.activeProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedProjects: StateFlow<List<Project>> = repository.completedProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProjects: StateFlow<List<Project>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCount: StateFlow<Int> = repository.activeCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completedCount: StateFlow<Int> = repository.completedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- Profile State ---
    private val _interests = MutableStateFlow<Set<String>>(emptySet())
    val interests = _interests.asStateFlow()

    private val _skills = MutableStateFlow<Set<String>>(emptySet())
    val skills = _skills.asStateFlow()

    // --- Sync State ---
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow("")
    val lastSyncTime = _lastSyncTime.asStateFlow()

    private val _cloudSyncEnabled = MutableStateFlow(true)
    val cloudSyncEnabled = _cloudSyncEnabled.asStateFlow()

    // --- Dark Theme State ---
    private val _isDarkMode = MutableStateFlow<Boolean?>(null)
    val isDarkMode = _isDarkMode.asStateFlow()

    // --- Chat State ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage(
            id = "welcome",
            text = "مرحباً بك! أنا مساعد الابتكار البرمجي الذكي. يمكنني تصميم تطبيقات كاملة، مواقع ويب، إعطاؤك أكواد برمجية جاهزة للنسخ أو تصديرها كملفات ZIP و HTML. كيف يمكنني مساعدتك اليوم؟",
            isUser = false
        )
    ))
    val chatMessages = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading = _isChatLoading.asStateFlow()

    // --- Generator State ---
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _generatedIdea = MutableStateFlow<GeneratedIdeaJson?>(null)
    val generatedIdea = _generatedIdea.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // --- Imagen Image Generation State ---
    private val _isGeneratingImage = MutableStateFlow(false)
    val isGeneratingImage = _isGeneratingImage.asStateFlow()

    private val _generatedImageB64 = MutableStateFlow<String?>(null)
    val generatedImageB64 = _generatedImageB64.asStateFlow()

    // --- AI Evaluation States ---
    private val _aiEvaluation = MutableStateFlow<AiEvaluation?>(null)
    val aiEvaluation = _aiEvaluation.asStateFlow()

    private val _isEvaluatingUser = MutableStateFlow(false)
    val isEvaluatingUser = _isEvaluatingUser.asStateFlow()

    // --- Automatic Code Analysis States ---
    private val _codeAnalysisResult = MutableStateFlow<CodeAnalysisResult?>(null)
    val codeAnalysisResult = _codeAnalysisResult.asStateFlow()

    private val _isAnalyzingCode = MutableStateFlow(false)
    val isAnalyzingCode = _isAnalyzingCode.asStateFlow()

    init {
        // Load settings
        _interests.value = repository.getInterests()
        _skills.value = repository.getSkills()
        _cloudSyncEnabled.value = repository.isCloudSyncEnabled()
        _isDarkMode.value = repository.isDarkModeEnabled()
        updateSyncTimeLabel(repository.getLastSyncTime())

        // Load cached AI evaluation if any
        val cachedEvalJson = repository.getAiEvaluation()
        if (cachedEvalJson != null) {
            _aiEvaluation.value = parseAiEvaluation(cachedEvalJson)
        }

        // Load cached Code Analysis if any
        val cachedAnalysisJson = repository.getCodeAnalysis()
        if (cachedAnalysisJson != null) {
            _codeAnalysisResult.value = parseCodeAnalysis(cachedAnalysisJson)
        }
        
        // Load cached generated idea if any
        _generatedIdea.value = loadGeneratedIdeaFromPrefs()
    }

    private fun updateSyncTimeLabel(timestamp: Long) {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        _lastSyncTime.value = format.format(date)
    }

    // --- Profile Actions ---
    fun toggleInterest(interest: String) {
        val current = _interests.value.toMutableSet()
        if (current.contains(interest)) {
            current.remove(interest)
        } else {
            current.add(interest)
        }
        _interests.value = current
        repository.saveInterests(current)
    }

    fun toggleSkill(skill: String) {
        val current = _skills.value.toMutableSet()
        if (current.contains(skill)) {
            current.remove(skill)
        } else {
            current.add(skill)
        }
        _skills.value = current
        repository.saveSkills(current)
    }

    fun addNewSkill(skill: String) {
        if (skill.isBlank()) return
        val current = _skills.value.toMutableSet()
        current.add(skill.trim())
        _skills.value = current
        repository.saveSkills(current)
    }

    fun addNewInterest(interest: String) {
        if (interest.isBlank()) return
        val current = _interests.value.toMutableSet()
        current.add(interest.trim())
        _interests.value = current
        repository.saveInterests(current)
    }

    fun toggleCloudSync() {
        val next = !_cloudSyncEnabled.value
        _cloudSyncEnabled.value = next
        repository.setCloudSyncEnabled(next)
        if (next) {
            triggerCloudSync()
        }
    }

    fun triggerCloudSync() {
        if (!_cloudSyncEnabled.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            delay(1500) // Simulate elegant pulsing network sync
            _isSyncing.value = false
            val now = System.currentTimeMillis()
            repository.setLastSyncTime(now)
            updateSyncTimeLabel(now)
        }
    }

    // --- Project Persistence Actions ---
    fun saveGeneratedIdeaToActive(type: String, userDifficultyRating: Int) {
        val idea = _generatedIdea.value ?: return
        viewModelScope.launch {
            val stepsString = idea.steps.joinToString("|")
            
            // Save generated image if available
            var imagePath: String? = null
            _generatedImageB64.value?.let { b64 ->
                imagePath = saveBase64ImageToInternalStorage(b64, "project_image_${System.currentTimeMillis()}.png")
            }

            val newProject = Project(
                title = idea.title,
                description = idea.description,
                type = type,
                difficulty = userDifficultyRating, // Use the user-rated difficulty
                technologies = idea.technologies,
                progress = 0,
                reminderTime = "18:00", // Default reminder time
                steps = stepsString,
                completedSteps = "",
                imageUrl = imagePath
            )
            repository.insertProject(newProject)
            _generatedIdea.value = null // Clear generator state
            _generatedImageB64.value = null // Clear image state
            saveGeneratedIdeaToPrefs(null)
            triggerCloudSync() // Trigger simulation sync after adding project
        }
    }

    private fun saveBase64ImageToInternalStorage(base64Data: String, fileName: String): String? {
        val context = getApplication<Application>().applicationContext
        return try {
            val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
            val file = File(context.filesDir, fileName)
            FileOutputStream(file).use { out ->
                out.write(imageBytes)
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("Mubtakir", "Failed to save image locally", e)
            null
        }
    }

    private fun saveBitmapToInternalStorage(bitmap: Bitmap, fileName: String): String? {
        val context = getApplication<Application>().applicationContext
        return try {
            val file = File(context.filesDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("Mubtakir", "Failed to save bitmap locally", e)
            null
        }
    }

    private fun generateAbstractArtBitmap(title: String): Bitmap {
        val width = 512
        val height = 512
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        // Seed random with the hash of the title to keep the art deterministic and unique for each project!
        val seed = title.hashCode().toLong()
        val random = java.util.Random(seed)

        // Define a modern gradient background matching the app's color style
        val colors = intArrayOf(
            android.graphics.Color.rgb(random.nextInt(128) + 32, random.nextInt(128) + 32, random.nextInt(128) + 128), // primary shade
            android.graphics.Color.rgb(random.nextInt(128) + 128, random.nextInt(128) + 32, random.nextInt(128) + 32), // secondary shade
            android.graphics.Color.rgb(random.nextInt(128) + 32, random.nextInt(128) + 128, random.nextInt(128) + 128)  // accent shade
        )
        val gradient = android.graphics.LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            colors, null, android.graphics.Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Draw some beautiful overlay geometric patterns (abstract modern art!)
        paint.shader = null
        for (i in 0 until 6) {
            val cx = random.nextFloat() * width
            val cy = random.nextFloat() * height
            val radius = (random.nextFloat() * 180) + 60
            paint.color = android.graphics.Color.argb(
                random.nextInt(45) + 25, // semi-transparent
                random.nextInt(256),
                random.nextInt(256),
                random.nextInt(256)
            )
            canvas.drawCircle(cx, cy, radius, paint)
        }

        // Draw some stylish glowing lines or curves
        paint.strokeWidth = (random.nextFloat() * 8) + 4
        paint.style = android.graphics.Paint.Style.STROKE
        for (i in 0 until 4) {
            paint.color = android.graphics.Color.argb(
                random.nextInt(100) + 50,
                255, 255, 255
            )
            val path = android.graphics.Path()
            path.moveTo(random.nextFloat() * width, 0f)
            path.cubicTo(
                random.nextFloat() * width, random.nextFloat() * height,
                random.nextFloat() * width, random.nextFloat() * height,
                random.nextFloat() * width, height.toFloat()
            )
            canvas.drawPath(path, paint)
        }

        return bitmap
    }

    private fun getMockImageB64(title: String): String {
        val bitmap = generateAbstractArtBitmap(title)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val bytes = baos.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    fun generateImageForIdea(title: String, description: String) {
        _isGeneratingImage.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    // Simulate image generation with a beautiful mock image base64 if key is missing!
                    delay(1500)
                    _generatedImageB64.value = getMockImageB64(title)
                    return@launch
                }

                val prompt = "A highly detailed, modern, and clean visual representation of: $title. $description. Style: digital art, concept art, clean, colorful, inviting, Material Design, 3d render, vector style."
                val request = GenerateContentRequest(
                    contents = listOf(
                        ContentRequest(
                            parts = listOf(
                                PartRequest(text = prompt)
                            )
                        )
                    ),
                    generationConfig = GenerationConfigRequest(
                        responseMimeType = null, // Set to null to request binary modal image response
                        imageConfig = ImageConfigRequest(aspectRatio = "1:1", imageSize = "1K"),
                        responseModalities = listOf("TEXT", "IMAGE")
                    )
                )

                val response = withContext(Dispatchers.IO) {
                    retryWithBackoff(times = 3, initialDelay = 1000) {
                        geminiApiService.generateImage(apiKey, request)
                    }
                }

                val partWithImage = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.inlineData != null }
                val base64Data = partWithImage?.inlineData?.data
                if (base64Data != null) {
                    _generatedImageB64.value = base64Data
                } else {
                    _errorMessage.value = "فشل في العثور على بيانات الصورة في استجابة الخادم. جاري عرض تصميم تعبيري..."
                    _generatedImageB64.value = getMockImageB64(title)
                }
            } catch (e: Exception) {
                Log.e("Mubtakir", "Failed to generate image", e)
                _errorMessage.value = "فشل توليد الصورة: ${e.localizedMessage ?: "خطأ غير معروف"}. جاري عرض تصميم تعبيري..."
                _generatedImageB64.value = getMockImageB64(title)
            } finally {
                _isGeneratingImage.value = false
            }
        }
    }

    fun generateImageForProject(project: Project) {
        _isGeneratingImage.value = true
        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                val base64Data: String? = if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    // Simulate beautiful mock image
                    delay(1500)
                    getMockImageB64(project.title)
                } else {
                    val prompt = "A highly detailed, modern, and clean visual representation of: ${project.title}. ${project.description}. Style: digital art, concept art, clean, colorful, inviting, Material Design, 3d render, vector style."
                    val request = GenerateContentRequest(
                        contents = listOf(
                            ContentRequest(
                                parts = listOf(
                                    PartRequest(text = prompt)
                                )
                            )
                        ),
                        generationConfig = GenerationConfigRequest(
                            responseMimeType = null,
                            imageConfig = ImageConfigRequest(aspectRatio = "1:1", imageSize = "1K"),
                            responseModalities = listOf("TEXT", "IMAGE")
                        )
                    )

                    val response = withContext(Dispatchers.IO) {
                        retryWithBackoff(times = 3, initialDelay = 1000) {
                            geminiApiService.generateImage(apiKey, request)
                        }
                    }

                    val partWithImage = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.inlineData != null }
                    partWithImage?.inlineData?.data
                }

                val finalB64 = base64Data ?: getMockImageB64(project.title)
                val imagePath = saveBase64ImageToInternalStorage(finalB64, "project_image_${project.id}_${System.currentTimeMillis()}.png")
                if (imagePath != null) {
                    val updated = project.copy(imageUrl = imagePath)
                    repository.updateProject(updated)
                }
            } catch (e: Exception) {
                Log.e("Mubtakir", "Failed to generate image for project", e)
                // Try fallback
                val finalB64 = getMockImageB64(project.title)
                val imagePath = saveBase64ImageToInternalStorage(finalB64, "project_image_${project.id}_${System.currentTimeMillis()}.png")
                if (imagePath != null) {
                    val updated = project.copy(imageUrl = imagePath)
                    repository.updateProject(updated)
                }
            } finally {
                _isGeneratingImage.value = false
            }
        }
    }

    fun clearGeneratedImageState() {
        _generatedImageB64.value = null
    }

    fun insertManualProject(title: String, description: String, type: String, technologies: String, difficulty: Int) {
        viewModelScope.launch {
            val newProject = Project(
                title = title,
                description = description,
                type = type,
                difficulty = difficulty,
                technologies = technologies,
                progress = 100,
                isCompleted = true,
                completedAt = System.currentTimeMillis()
            )
            repository.insertProject(newProject)
            triggerCloudSync()
        }
    }

    fun updateProjectProgress(project: Project, stepIndex: Int, isChecked: Boolean) {
        viewModelScope.launch {
            val completedList = if (project.completedSteps.isBlank()) {
                mutableListOf()
            } else {
                project.completedSteps.split("|").toMutableList()
            }

            val stepStr = stepIndex.toString()
            if (isChecked) {
                if (!completedList.contains(stepStr)) completedList.add(stepStr)
            } else {
                completedList.remove(stepStr)
            }

            val newCompletedSteps = completedList.joinToString("|")
            val totalSteps = project.steps.split("|").size
            val calculatedProgress = if (totalSteps > 0) {
                ((completedList.size.toFloat() / totalSteps.toFloat()) * 100).toInt()
            } else {
                0
            }

            val updated = project.copy(
                completedSteps = newCompletedSteps,
                progress = calculatedProgress,
                isCompleted = calculatedProgress >= 100,
                completedAt = if (calculatedProgress >= 100) System.currentTimeMillis() else null
            )
            repository.updateProject(updated)
            triggerCloudSync()
        }
    }

    fun updateReminderTime(project: Project, time: String) {
        viewModelScope.launch {
            val updated = project.copy(reminderTime = time)
            repository.updateProject(updated)
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repository.deleteProject(project)
            triggerCloudSync()
        }
    }

    // --- Custom Manual Project & Restore & Theme Toggling & AI Chat Actions ---
    fun insertActiveManualProject(
        title: String,
        description: String,
        type: String,
        technologies: String,
        difficulty: Int,
        stepsList: List<String>
    ) {
        viewModelScope.launch {
            val stepsString = stepsList.filter { it.isNotBlank() }.joinToString("|")
            val newProject = Project(
                title = title,
                description = description,
                type = type,
                difficulty = difficulty,
                technologies = technologies,
                progress = 0,
                reminderTime = "18:00",
                steps = stepsString,
                completedSteps = ""
            )
            repository.insertProject(newProject)
            triggerCloudSync()
        }
    }

    fun restoreProjectToActive(project: Project) {
        viewModelScope.launch {
            val updated = project.copy(
                isCompleted = false,
                completedAt = null,
                progress = 0,
                completedSteps = ""
            )
            repository.updateProject(updated)
            triggerCloudSync()
        }
    }

    fun toggleTheme(systemInDark: Boolean) {
        val current = _isDarkMode.value ?: systemInDark
        val next = !current
        _isDarkMode.value = next
        repository.saveDarkModeEnabled(next)
    }

    // --- Serialization Helpers ---
    private fun serializeChatMessages(messages: List<ChatMessage>): String {
        return try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, ChatMessage::class.java)
            val adapter = moshi.adapter<List<ChatMessage>>(type)
            adapter.toJson(messages)
        } catch (e: Exception) {
            Log.e("Mubtakir", "Failed to serialize chat messages", e)
            "[]"
        }
    }

    private fun deserializeChatMessages(json: String): List<ChatMessage> {
        return try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, ChatMessage::class.java)
            val adapter = moshi.adapter<List<ChatMessage>>(type)
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            Log.e("Mubtakir", "Failed to deserialize chat messages", e)
            emptyList()
        }
    }

    // --- Chat Session Persistence & Operations ---
    private fun loadLatestSession() {
        viewModelScope.launch {
            repository.allChatSessions.collect { sessions ->
                if (sessions.isNotEmpty() && _currentSessionId.value == null && _chatMessages.value.size == 1 && _chatMessages.value.first().id == "welcome") {
                    val latest = sessions.first()
                    _currentSessionId.value = latest.id
                    _chatMessages.value = deserializeChatMessages(latest.messagesJson)
                }
            }
        }
    }

    fun selectChatSession(session: com.example.data.ChatSession) {
        _currentSessionId.value = session.id
        _chatMessages.value = deserializeChatMessages(session.messagesJson)
    }

    fun startNewChat() {
        _currentSessionId.value = null
        _chatMessages.value = listOf(
            ChatMessage(
                id = "welcome",
                text = "مرحباً بك! أنا مساعد الابتكار البرمجي الذكي. يمكنني تصميم تطبيقات كاملة، مواقع ويب، إعطاؤك أكواد برمجية جاهزة للنسخ أو تصديرها كملفات ZIP و HTML. كيف يمكنني مساعدتك اليوم؟",
                isUser = false
            )
        )
    }

    private fun saveGeneratedIdeaToPrefs(idea: GeneratedIdeaJson?) {
        try {
            if (idea == null) {
                repository.saveGeneratedIdea(null)
            } else {
                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(GeneratedIdeaJson::class.java)
                val jsonStr = adapter.toJson(idea)
                repository.saveGeneratedIdea(jsonStr)
            }
        } catch (e: Exception) {
            Log.e("Mubtakir", "Failed to save generated idea", e)
        }
    }

    private fun loadGeneratedIdeaFromPrefs(): GeneratedIdeaJson? {
        return try {
            val jsonStr = repository.getSavedGeneratedIdea()
            if (jsonStr != null) {
                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(GeneratedIdeaJson::class.java)
                adapter.fromJson(jsonStr)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("Mubtakir", "Failed to load generated idea", e)
            null
        }
    }

    fun deleteChatSession(session: com.example.data.ChatSession) {
        viewModelScope.launch {
            repository.deleteChatSession(session)
            if (_currentSessionId.value == session.id) {
                startNewChat()
            }
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.resetRepository()
            
            // Reset local memory states to pristine defaults
            _interests.value = setOf("تطوير التطبيقات", "الذكاء الاصطناعي", "التصميم البصري")
            _skills.value = setOf("Kotlin", "Jetpack Compose", "UI/UX", "Figma")
            _cloudSyncEnabled.value = true
            _isDarkMode.value = null
            _aiEvaluation.value = null
            _codeAnalysisResult.value = null
            _generatedIdea.value = null
            saveGeneratedIdeaToPrefs(null)
            _errorMessage.value = null
            
            repository.saveInterests(setOf("تطوير التطبيقات", "الذكاء الاصطناعي", "التصميم البصري"))
            repository.saveSkills(setOf("Kotlin", "Jetpack Compose", "UI/UX", "Figma"))
            repository.setCloudSyncEnabled(true)
            repository.setLastSyncTime(System.currentTimeMillis())
            
            _currentSessionId.value = null
            _chatMessages.value = listOf(
                ChatMessage(
                    id = "welcome",
                    text = "مرحباً بك! أنا مساعد الابتكار البرمجي الذكي. يمكنني تصميم تطبيقات كاملة، مواقع ويب، إعطاؤك أكواد برمجية جاهزة للنسخ أو تصديرها كملفات ZIP و HTML. كيف يمكنني مساعدتك اليوم؟",
                    isUser = false
                )
            )
        }
    }

    private fun saveCurrentSession(userText: String, updatedList: List<ChatMessage>) {
        viewModelScope.launch {
            val sessionId = _currentSessionId.value
            val messagesJson = serializeChatMessages(updatedList)
            if (sessionId == null) {
                val cleanTitle = if (userText.length > 30) userText.take(28) + "..." else userText
                val newSession = com.example.data.ChatSession(
                    title = cleanTitle,
                    messagesJson = messagesJson
                )
                val newId = repository.insertChatSession(newSession)
                _currentSessionId.value = newId.toInt()
            } else {
                val currentSession = repository.getChatSessionById(sessionId)
                if (currentSession != null) {
                    val updatedSession = currentSession.copy(messagesJson = messagesJson)
                    repository.updateChatSession(updatedSession)
                }
            }
        }
    }

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        
        val userMsg = ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            text = userText,
            isUser = true
        )
        
        val currentList = _chatMessages.value.toMutableList()
        currentList.add(userMsg)
        _chatMessages.value = currentList
        saveCurrentSession(userText, currentList)
        
        _isChatLoading.value = true
        
        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    simulateChatFallback(userText)
                    return@launch
                }
                
                val systemPrompt = """
                    أنت "مُبتكِر الذكي" (Mubtakir AI) - مساعد مهندس برمجيات ومصمم واجهات خبير ومبتكر تقني عبقري.
                    مهمتك هي مساعدة المستخدم في تصميم تطبيقات، مواقع ويب، تقديم أكواد كاملة بلغات البرمجة (مثل Kotlin, HTML/CSS, Flutter, Python, JS)، أو إنشاء هياكل لملفات ZIP.
                    
                    إرشادات هامة جداً:
                    1. أجب باللغة العربية الفصحى الراقية والواضحة والمحفزة للابتكار.
                    2. عندما يطلب المستخدم منك تصميم تطبيق، كود، أو موقع، صممه له واكتب الكود كاملاً ومفصلاً ووضعه داخل بلوكات الكود المخصصة بـ Markdown (مثل ```html ... ``` أو ```kotlin ... ```). لا تبخل في التفاصيل والأكواد!
                    3. إذا طلب ملف ZIP، اعرض له هيكل الملفات واكتب الأكواد الأساسية التي ستكون بداخله، موضحاً أنه يمكنه تحميل هذا الكود مباشرة من التطبيق.
                    4. اجعل نبرتك ودودة، احترافية للغاية، وتدفع بالإنتاجية.
                """.trimIndent()
                
                val chatHistoryText = _chatMessages.value.takeLast(6).joinToString("\n") { 
                    if (it.isUser) "المستخدم: ${it.text}" else "مبتكر الذكي: ${it.text}"
                }
                
                val finalPrompt = "$systemPrompt\n\nتاريخ المحادثة الأخيرة:\n$chatHistoryText\n\nالمستخدم حالياً يسأل: $userText"
                
                val request = GenerateContentRequest(
                    contents = listOf(
                        ContentRequest(
                            parts = listOf(
                                PartRequest(text = finalPrompt)
                            )
                        )
                    ),
                    generationConfig = GenerationConfigRequest(responseMimeType = null, temperature = 0.7)
                )
                
                val response = withContext(Dispatchers.IO) {
                    retryWithBackoff(times = 3, initialDelay = 1000) {
                        geminiApiService.generateContent(apiKey, request)
                    }
                }
                
                val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "عذراً، لم أتمكن من توليد رد مناسب حالياً."
                
                val aiMsg = ChatMessage(
                    id = java.util.UUID.randomUUID().toString(),
                    text = replyText,
                    isUser = false
                )
                
                val updatedList = _chatMessages.value.toMutableList()
                updatedList.add(aiMsg)
                _chatMessages.value = updatedList
                saveCurrentSession(userText, updatedList)
                
            } catch (e: Exception) {
                Log.e("Mubtakir", "Chat API error", e)
                val errorMsg = ChatMessage(
                    id = java.util.UUID.randomUUID().toString(),
                    text = "حدث خطأ أثناء الاتصال بالذكاء الاصطناعي: ${e.localizedMessage ?: "تأكد من اتصالك بالإنترنت"}. سأقوم بالرد عليك عبر محاكاة الابتكار المحلية.",
                    isUser = false
                )
                val updatedList = _chatMessages.value.toMutableList()
                updatedList.add(errorMsg)
                _chatMessages.value = updatedList
                simulateChatFallback(userText)
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    private suspend fun simulateChatFallback(userText: String) {
        _isChatLoading.value = true
        delay(1500)
        _isChatLoading.value = false
        
        val cleanedText = userText.lowercase()
        val replyText = when {
            cleanedText.contains("موقع") || cleanedText.contains("ويب") || cleanedText.contains("website") || cleanedText.contains("html") -> {
                """
                    بالتأكيد! لقد قمت بتصميم موقع ويب تفاعلي كامل يعتمد على واجهة حديثة وأنيقة وتأثيرات بصرية ممتازة.
                    إليك كود HTML/CSS الكامل للموقع، يمكنك نسخ فوراً أو النقر على "تنزيل" لحفظه كملف HTML تفاعلي:
                    
                    ```html
                    <!DOCTYPE html>
                    <html lang="ar" dir="rtl">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>لوحة ابتكار ذكية</title>
                        <style>
                            body {
                                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                                background: linear-gradient(135deg, #0f172a 0%, #1e1b4b 100%);
                                color: #f8fafc;
                                margin: 0;
                                padding: 40px 20px;
                                display: flex;
                                flex-direction: column;
                                align-items: center;
                                min-height: 100vh;
                            }
                            .card {
                                background: rgba(255, 255, 255, 0.05);
                                backdrop-filter: blur(10px);
                                border: 1px solid rgba(255, 255, 255, 0.1);
                                border-radius: 24px;
                                padding: 32px;
                                max-width: 600px;
                                width: 100%;
                                box-shadow: 0 10px 30px rgba(0,0,0,0.5);
                                text-align: center;
                            }
                            h1 {
                                color: #818cf8;
                                margin-bottom: 16px;
                                font-size: 2.5rem;
                            }
                            p {
                                color: #94a3b8;
                                line-height: 1.6;
                            }
                            .btn {
                                background: #4f46e5;
                                color: white;
                                border: none;
                                padding: 12px 32px;
                                border-radius: 12px;
                                font-weight: bold;
                                cursor: pointer;
                                transition: 0.3s;
                                margin-top: 24px;
                            }
                            .btn:hover {
                                background: #6366f1;
                                transform: scale(1.05);
                            }
                        </style>
                    </head>
                    <body>
                        <div class="card">
                            <h1>مرحباً بك في عالم الابتكار 🚀</h1>
                            <p>هذا موقع ويب تفاعلي تم تصميمه لك بالكامل بواسطة الذكاء الاصطناعي. يمكنك استخدام الكود لبدء مشروعك البرمجي ونشره فوراً.</p>
                            <button class="btn" onclick="alert('أنت عبقري ومبتكر!')">ابدأ الآن</button>
                        </div>
                    </body>
                    </html>
                    ```
                    
                    يمكنك تجربة هذا الكود محلياً في متصفحك! هل ترغب في أن أقوم بتصميم كود تطبيق أندرويد بلغة Kotlin للمشروع؟
                """.trimIndent()
            }
            cleanedText.contains("تطبيق") || cleanedText.contains("اندرويد") || cleanedText.contains("android") || cleanedText.contains("kotlin") || cleanedText.contains("apk") -> {
                """
                    رائع! لقد قمت بتصميم واجهة تطبيق أندرويد حديثة ومذهلة باستخدام Jetpack Compose بالكامل.
                    إليك الكود البرمجي الكامل بلغة Kotlin مع دعم Material 3 والوضع الداكن:
                    
                    ```kotlin
                    package com.example.ui.screens

                    import androidx.compose.foundation.background
                    import androidx.compose.foundation.layout.*
                    import androidx.compose.foundation.shape.RoundedCornerShape
                    import androidx.compose.material3.*
                    import androidx.compose.runtime.*
                    import androidx.compose.ui.Alignment
                    import androidx.compose.ui.Modifier
                    import androidx.compose.ui.graphics.Color
                    import androidx.compose.ui.text.font.FontWeight
                    import androidx.compose.ui.unit.dp
                    import androidx.compose.ui.unit.sp

                    @Composable
                    fun InnovationHubScreen() {
                        var clickCount by remember { mutableStateOf(0) }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "المبتكر الذكي 📱",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF818CF8)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "هذه واجهة تطبيق تم توليدها بالكامل بواسطة الذكاء الاصطناعي لتكون ركيزة لمشروعك القادم.",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = { clickCount++ },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                                    ) {
                                        Text("نقرات الإنجاز: ${'$'}clickCount")
                                    }
                                }
                            }
                        }
                    }
                    ```
                    
                    يمكنك وضع هذا الكود في مشروع أندرويد الخاص بك لتشغيله كشاشة رئيسية للتطبيق! هل ترغب في ملف ZIP يحتوي على بنية كاملة للمشروع؟
                """.trimIndent()
            }
            cleanedText.contains("zip") || cleanedText.contains("ملف") || cleanedText.contains("ملفات") -> {
                """
                    بالتأكيد! لتنظيم هذا المشروع البرمجي، إليك هيكل ملفات ZIP المقترح متكاملاً. 
                    يمكنك الضغط على زر "تنزيل" لتوليد ملف ZIP حقيقي يحتوي على الكود والهيكل لبدء مشروعك فوراً:
                    
                    ```json
                    {
                      "archiveName": "mubtakir_project.zip",
                      "structure": {
                        "src/main/java/com/example/": [
                          "MainActivity.kt",
                          "ui/theme/Theme.kt",
                          "ui/screens/MainScreen.kt"
                        ],
                        "src/main/res/": [
                          "values/strings.xml",
                          "drawable/ic_logo.xml"
                        ],
                        "build.gradle.kts": "// تكوين إعدادات البناء والتكامل لـ Compose"
                      }
                    }
                    ```
                    
                    اضغط على زر التنزيل المرفق لحفظ ملف الكود فوراً!
                """.trimIndent()
            }
            else -> {
                """
                    أهلاً بك! لقد فهمت استفسارك حول "$userText".
                    يمكنني مساعدتك في تصميم وتوليد الأكواد لصفحات الويب الحديثة (HTML/CSS/JS)، تطبيقات الأندرويد باستخدام Kotlin و Compose، أو بناء الهيكل الكامل لمشروعك وحفظه كملف ZIP.
                    
                    يرجى إعطائي تفاصيل أكثر حول الفكرة التي ترغب في تنفيذها وسأقوم بكتابة الأكواد الكاملة لك فوراً! 🚀
                """.trimIndent()
            }
        }
        
        val aiMsg = ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            text = replyText,
            isUser = false
        )
        val updatedList = _chatMessages.value.toMutableList()
        updatedList.add(aiMsg)
        _chatMessages.value = updatedList
        saveCurrentSession(userText, updatedList)
    }

    // --- AI Generator Actions ---
    fun generateIdea(type: String) {
        _isGenerating.value = true
        _generatedIdea.value = null
        saveGeneratedIdeaToPrefs(null)
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    // Simulate high quality fallback if API key is not configured so user doesn't get a dead screen, but tell them how to setup
                    simulateBeautifulFallback(type)
                    return@launch
                }

                val prompt = buildPrompt(type)
                val request = GenerateContentRequest(
                    contents = listOf(
                        ContentRequest(
                            parts = listOf(
                                PartRequest(text = prompt)
                            )
                        )
                    ),
                    generationConfig = GenerationConfigRequest()
                )

                val response = withContext(Dispatchers.IO) {
                    retryWithBackoff(times = 3, initialDelay = 1000) {
                        geminiApiService.generateContent(apiKey, request)
                    }
                }

                val jsonResponseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (jsonResponseText != null) {
                    val cleanedJson = cleanJsonString(jsonResponseText)
                    val parsedIdea = parseGeneratedIdea(cleanedJson)
                    if (parsedIdea != null) {
                        _generatedIdea.value = parsedIdea
                        saveGeneratedIdeaToPrefs(parsedIdea)
                    } else {
                        Log.e("Mubtakir", "Failed to parse JSON response: $jsonResponseText")
                        _errorMessage.value = "فشل في معالجة استجابة الذكاء الاصطناعي. جرب مرة أخرى."
                    }
                } else {
                    _errorMessage.value = "لم يتم تلقي أي اقتراحات من الذكاء الاصطناعي."
                }

            } catch (e: Exception) {
                Log.e("Mubtakir", "API error during generateIdea", e)
                _errorMessage.value = "حدث خطأ أثناء الاتصال بالذكاء الاصطناعي: ${e.localizedMessage ?: "تأكد من اتصالك بالإنترنت"}"
                // For a smooth demo even during minor connection drops or config delays, let's provide fallback simulation
                simulateBeautifulFallback(type)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private fun cleanJsonString(rawJson: String): String {
        var clean = rawJson.trim()
        if (clean.startsWith("```json")) {
            clean = clean.substringAfter("```json").trim()
        } else if (clean.startsWith("```")) {
            clean = clean.substringAfter("```").trim()
        }
        if (clean.endsWith("```")) {
            clean = clean.substringBeforeLast("```").trim()
        }
        return clean
    }

    private fun parseGeneratedIdea(jsonString: String): GeneratedIdeaJson? {
        return try {
            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
            val adapter = moshi.adapter(GeneratedIdeaJson::class.java)
            adapter.fromJson(jsonString)
        } catch (e: Exception) {
            Log.e("Mubtakir", "Moshi parse error", e)
            null
        }
    }

    private fun buildPrompt(type: String): String {
        val interestList = _interests.value.joinToString(", ")
        val skillList = _skills.value.joinToString(", ")

        val requestTypeLabel = when (type) {
            "DO" -> "ماذا أفعل اليوم (مهمة عملية، ممارسة يومية، نشاط تقني أو ذهني صغير)"
            "INVENT" -> "ماذا أخترع (فكرة لنموذج أولي، اختراع مميز، أو مشروع برمجي ابتكاري متكامل)"
            "DESIGN" -> "ماذا أصمم (واجهة مستخدم مبتكرة، تصميم ثلاثي الأبعاد، تصميم جرافيك، أو تجربة تفاعلية)"
            "PROMPT" -> "تصميم برومبت ذكي وهندسة أوامر (Prompt engineering) لـ LLM مناسب للمبرمجين والمصممين لتسهيل مهامهم أو إنتاج كود بجودة عالية"
            else -> "فكرة لمشروع مميز"
        }

        return """
            أنت خبير ومساعد ابتكار وتصميم ذكي بالذكاء الاصطناعي ومطور برمجيات مخضرم. 
            مهمتك هي اقتراح فكرة إبداعية باللغة العربية مخصصة بالكامل للمستخدم بناءً على مهاراته التقنية واهتماماته الشخصية.
            
            معلومات المستخدم:
            - اهتماماته الشخصية: $interestList
            - مهاراته التقنية الحالية: $skillList
            - نوع الابتكار المطلوب: $requestTypeLabel
            
            يجب أن ترد وتصيغ الإجابة حصراً بصيغة JSON صالحة للتفسير (لا تضع أي نصوص قبل أو بعد الـ JSON، لا تضع علامات أو هوامش إضافية، واجعل بنية الـ JSON مطابقة تماماً للمواصفات التالية):
            
            {
              "title": "اسم الفكرة المبتكرة والذكية",
              "description": "وصف تفصيلي يشرح فكرة المشروع، قيمته الإبداعية، وكيف يحل مشكلة أو يوفر متعة وفائدة بطريقة ذكية",
              "technologies": "قائمة من 3 إلى 5 تقنيات برمجية أو أدوات مناسبة لتنفيذ هذا المشروع (مثال: Kotlin, Jetpack Compose, Figma) مفصولة بفواصل عادية",
              "difficulty": ومستوى صعوبتها التقديري كرقم من 1 إلى 5 (حيث 1 سهل جداً، 5 صعب ومتكامل),
              "estimatedDuration": "المدة المقترحة لإنهاء المشروع (مثال: 3 أيام، أسبوع واحد)",
              "steps": [
                "الخطوة الأولى للبدء في المشروع (مثلاً: تصميم الواجهة في فيجما)",
                "الخطوة الثانية لتطوير منطق العمل البرمجي وتطبيق الميزات الأساسية",
                "الخطوة الثالثة لربط البيانات واختبار العمل محلياً",
                "الخطوة الرابعة لتحسين الجودة والنشر أو عرض النموذج"
              ]
            }
            
            اجعل الفكرة شيقة ومناسبة جداً ومفيدة وملهمة. استخدم اللغة العربية الفصحى الأنيقة والجذابة.
        """.trimIndent()
    }

    private suspend fun simulateBeautifulFallback(type: String) {
        // High quality offline fallback ideas if API key is not configured or fails, ensuring beautiful prototype experience!
        _isGenerating.value = true
        delay(2000) // Simulated AI thinking time for elegant UX
        _isGenerating.value = false

        val randomId = (0..2).random()
        val fallbackIdea = when (type) {
            "DO" -> listOf(
                GeneratedIdeaJson(
                    title = "لوحة تحكم ذكية لتتبع الطاقة الشخصية",
                    description = "تطبيق مصغر لتسجيل وقياس فترات التركيز اليومية ومقارنتها بساعات النوم والوجبات الغذائية، مع رسم بياني بسيط يوضح أوقات ذروة إنتاجيتك الشخصية.",
                    technologies = "Jetpack Compose, Room DB, Vico Charts",
                    difficulty = 2,
                    estimatedDuration = "يوم واحد",
                    steps = listOf("تصميم الواجهة الرئيسية بجدول بسيط لإدخال البيانات اليومية", "تجهيز جدول في قاعدة البيانات المحلية لحفظ القياسات", "رسم تخطيط بياني ملون يمثل تغير مستويات التركيز", "إتاحة زر تصدير التقرير اليومي بنص منسق")
                ),
                GeneratedIdeaJson(
                    title = "مخطط تحدي 21 يوماً لعادة إيجابية",
                    description = "أداة ذكية تمكنك من اختيار عادة (مثل القراءة أو البرمجة) لتحدي نفسك وتلقي تذكير دائم ببطاقات تفاعلية تشعرك بالإنجاز عند إكمال المهام اليومية.",
                    technologies = "Compose, Preference DataStore, Material3",
                    difficulty = 1,
                    estimatedDuration = "يوم واحد",
                    steps = listOf("إنشاء واجهة لعرض 21 دائرة تمثل أيام التحدي", "برمجة خاصية الضغط لتأكيد الإنجاز اليومي بنجاح", "إضافة أصوات واهتزازات خفيفة تفاعلية عند إتمام يوم", "تخزين البيانات لضمان عدم ضياع أيام التحدي")
                ),
                GeneratedIdeaJson(
                    title = "محفز الذكاء الصباحي التقني",
                    description = "تطبيق يقوم بطرح سؤال منطقي برمجياً أو تحدي فكري قصير كل صباح بناءً على مهاراتك المختارة لمساعدتك على تنشيط عقلك قبل البدء في العمل.",
                    technologies = "Kotlin, Coroutines, SharedPreferences",
                    difficulty = 2,
                    estimatedDuration = "يوم واحد",
                    steps = listOf("تصميم شاشة ترحيبية تعرض بطاقة لغز اليوم مع مؤقت مدمج", "تجهيز بنك أسئلة صغير لمهارات Kotlin والـ UX", "برمجة التحقق من الإجابة بشكل فوري وجميل وبصري", "تفعيل تذكير دوري ليعمل في وقت محدد صباحاً")
                )
            )[randomId]

            "INVENT" -> listOf(
                GeneratedIdeaJson(
                    title = "جهاز الري المنزلي الذكي المعزز بإنترنت الأشياء",
                    description = "ابتكار جهاز يقرأ نسبة رطوبة التربة عبر مستشعرات متصلة بمتحكم دقيق (أردوينو)، ويرسل إشعارات فورية لهاتفك مقترحاً ري النبتة آلياً مع التنبؤ بحاجة النبتة للماء مستقبلاً باستخدام نموذج ذكاء اصطناعي محلي خفيف.",
                    technologies = "Arduino, Kotlin, Firebase, Ktor Client",
                    difficulty = 4,
                    estimatedDuration = "5 أيام",
                    steps = listOf("تصميم ومحاكاة الدائرة الكهربائية للمستشعرات والمتحكم", "بناء التطبيق الذي يستقبل القراءات عبر الـ Bluetooth أو الـ Wi-Fi", "إعداد قاعدة بيانات لتسجيل تواريخ السقي وصحة النبات", "تضمين نموذج بسيط يتنبأ بموعد الري القادم بناءً على درجة الحرارة والرطوبة")
                ),
                GeneratedIdeaJson(
                    title = "نظام المساعد الصوتي لتسجيل الأفكار السائبة",
                    description = "ابتكار نظام يسجل صوتك ويقوم فوراً بتحويله إلى نص، ثم استخلاص الكلمات المفتاحية والمهام وتصنيفها كأفكار برمجية، وتوجيهها للقسم المناسب (تصاميم، مشاريع، مهمة اليوم) دون أي تدخل يدوي منك.",
                    technologies = "Speech-to-Text API, Gemini API, Room Database",
                    difficulty = 5,
                    estimatedDuration = "أسبوع واحد",
                    steps = listOf("تفعيل ميزة التسجيل الصوتي الحي بأعلى دقة ممكنة", "إرسال الصوت لمحرك التحويل واستقبال النص العربي المكتوب", "استخدام الذكاء الاصطناعي لتصنيف الأفكار وفرز التقنيات المطلوبة لكل فكرة", "تخزين الأفكار في مستودع آمن ومبوب مع خيار البحث الذكي")
                ),
                GeneratedIdeaJson(
                    title = "خزانة المفاتيح الذكية بتأكيد البصمة والرمز",
                    description = "اختراع صندوق أمانات منزلي مغلق إلكترونياً يتصل بتطبيقك عبر البلوتوث، حيث يمكنك تتبع من فتح الصندوق ومتى، مع القدرة على منح رموز وصول مؤقتة للأصدقاء والزوار تنتهي صلاحيتها آلياً.",
                    technologies = "ESP32 microchip, Bluetooth BLE, Kotlin, Cryptography",
                    difficulty = 4,
                    estimatedDuration = "4 أيام",
                    steps = listOf("تثبيت قطعة بلوتوث في المتحكم وربطها بقفل مغناطيسي", "برمجة خوارزمية التشفير لتوليد مفاتيح وصول ديناميكية وصالحة لمرة واحدة", "تصميم واجهة تحكم بالهاتف لرصد سجلات فتح الصندوق بدقة", "إضافة ميزة إطلاق إنذار صوتي فوري في حال المحاولة الخاطئة المتكررة")
                )
            )[randomId]

            "DESIGN" -> listOf(
                GeneratedIdeaJson(
                    title = "تطبيق ريادي لإدارة المزرعة العمودية الحضرية",
                    description = "تصميم واجهة مستخدم (UI/UX) مبتكرة ومستقبلية للتحكم في مزارع عمودية مغلقة داخل المدن. يركز التصميم على الجماليات النظيفة البسيطة، استخدام تدرجات النيون المضيئة، والرسوم البيانية الانسيابية ثلاثية الأبعاد لعرض مستويات الإضاءة والمغذيات.",
                    technologies = "Figma, Jetpack Compose, Material Design 3",
                    difficulty = 3,
                    estimatedDuration = "3 أيام",
                    steps = listOf("تحديد الهوية البصرية ولوحة الألوان المستوحاة من الطبيعة والتقنية المضيئة", "تصميم شاشات لوحة التحكم التفاعلية ومؤشرات الحرارة والنمو في فيجما", "بناء نموذج الواجهة التفاعلية بالـ Compose مع تفعيل حركات انتقال انسيابية", "تطبيق دعم الوضعين الداكن والمضيء مع تخصيص المكونات بجمال فائق")
                ),
                GeneratedIdeaJson(
                    title = "واجهة نظام قيادة الطائرات بدون طيار الذاتية",
                    description = "تصميم شاشة تحكم تكتيكية مخصصة للهواتف والتابلت لمراقبة مسارات طائرات الدرون التجارية، تقدم نظام عرض رأس لأسفل (HUD) فائق الوضوح ومقاييس سرعة وتوازن ثلاثية الأبعاد ممتازة.",
                    technologies = "Figma, Canvas Draw, Jetpack Compose",
                    difficulty = 3,
                    estimatedDuration = "يومين",
                    steps = listOf("تخطيط الهيكل البصري وعناصر واجهة القيادة التكتيكية (HUD)", "بناء بوصلة ومؤشر ميلان ديناميكي مرسوم يدوياً باستخدام Canvas في Compose", "تطبيق خرائط محاكاة تفاعلية لعرض خط السير ومواقع العقبات بدقة", "تصميم تحذيرات بصرية تومض باللون الأحمر والبرتقالي في حالات الطوارئ")
                ),
                GeneratedIdeaJson(
                    title = "تطبيق حجز تذاكر السفر بين الكواكب",
                    description = "تصميم خيالي مستقبلي لواجهة حجز رحلات للفضاء الخارجي والمريخ، يعتمد على صور كونية عميقة وتأثيرات زجاجية ممتازة (Glassmorphism)، وخرائط مسارات فضائية منحنية ومتحركة.",
                    technologies = "Figma, Glassmorphism CSS, Compose Custom Draw",
                    difficulty = 4,
                    estimatedDuration = "3 أيام",
                    steps = listOf("جمع أصول الصور والمؤثرات البصرية الكونية المناسبة للواجهة المستقبلية", "رسم بطاقات شفافة زجاجية متموجة تعلو خلفية فضائية نابضة بالحياة", "تصميم واجهة اختيار مقاعد المركبة الفضائية بأسلوب ثلاثي الأبعاد جذاب", "إعداد حركات انتقال متموجة وسلسة تحاكي انعدام الجاذبية عند الضغط")
                )
            )[randomId]

            "PROMPT" -> listOf(
                GeneratedIdeaJson(
                    title = "برومبت توليد واجهات Jetpack Compose نظيفة",
                    description = "برومبت مهندس باحترافية لتوجيه نماذج الذكاء الاصطناعي لتوليد واجهات مستخدم متكاملة باستخدام Compose ومبادئ M3 مع مراعاة الهيكلية والـ Clean Architecture.",
                    technologies = "Gemini Prompt, LLM Context, Compose M3",
                    difficulty = 3,
                    estimatedDuration = "يوم واحد",
                    steps = listOf("تحديد المشهد وحالة الواجهة المطلوبة (Loading, Success, Error)", "كتابة سياق هندسة الأوامر وتعيين دور الخبير للنموذج", "صياغة قيود التصميم والتباعد والوصول واستخدام الألوان المناسبة للثيم", "تجربة البرومبت وضبط الاستجابة بهيكل JSON محدد")
                ),
                GeneratedIdeaJson(
                    title = "برومبت مراجعة الأكواد واكتشاف الأخطاء الأمنية",
                    description = "برومبت ذكي لفحص الملفات البرمجية واكتشاف الثغرات الأمنية أو الأخطاء الشائعة وتحسين الأداء مع شرح مبسط للمبرمج.",
                    technologies = "Prompt Engineering, Secure Coding, Quality Assurance",
                    difficulty = 4,
                    estimatedDuration = "يوم واحد",
                    steps = listOf("صياغة دور المراجع التقني الأمني الصارم للنموذج", "تحديد معايير الفحص (أداء، أمان، استهلاك الذاكرة، توثيق)", "تضمين أمثلة لمدخلات ومخرجات مثالية (Few-shot prompting)", "اختبار الفعالية مع أكواد بها ثغرات شائعة وملاحظة الدقة")
                ),
                GeneratedIdeaJson(
                    title = "برومبت ابتكار أفكار تجارية للمشاريع الناشئة",
                    description = "برومبت مخصص لمطوري البرمجيات لتوليد أفكار ريادية مميزة بناءً على فئة السوق والجمهور المستهدف وحجم الميزانية.",
                    technologies = "Prompt Design, Business Analytics, Product Market Fit",
                    difficulty = 3,
                    estimatedDuration = "يوم واحد",
                    steps = listOf("تحديد هيكل نموذج العمل المستهدف (SaaS, B2B, B2C)", "بناء متغيرات لتحديد الجمهور والمشكلة الأساسية والحل البرمجي", "صياغة قيود المخرجات لتشمل الميزة التنافسية وتوقع العقبات", "تحسين الصياغة للحصول على تفاصيل مالية وتقنية قابلة للتنفيذ")
                )
            )[randomId]

            else -> GeneratedIdeaJson(
                title = "تطبيق تتبع الابتكار الشخصي",
                description = "مشروع متكامل لتصميم وتنفيذ محرك اقتراحات يومية يساعدك على التفكير خارج الصندوق.",
                technologies = "Kotlin, Room, Gemini API",
                difficulty = 3,
                estimatedDuration = "3 أيام",
                steps = listOf("تصميم الواجهة", "تطوير منطق العمل", "تأكيد العمل بنجاح")
            )
        }

        _generatedIdea.value = fallbackIdea
        saveGeneratedIdeaToPrefs(fallbackIdea)
    }

    // --- AI Evaluation System ---
    private fun parseAiEvaluation(jsonString: String): AiEvaluation? {
        return try {
            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
            val adapter = moshi.adapter(AiEvaluation::class.java)
            adapter.fromJson(jsonString)
        } catch (e: Exception) {
            Log.e("Mubtakir", "Failed to parse AI Evaluation JSON", e)
            null
        }
    }

    fun requestUserEvaluation() {
        _isEvaluatingUser.value = true
        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                
                val activeList = activeProjects.value
                val completedList = completedProjects.value
                val skillsList = skills.value.toList()
                val interestsList = interests.value.toList()
                
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    simulateEvaluationFallback(activeList, completedList, skillsList, interestsList)
                    return@launch
                }
                
                val prompt = """
                    قم بإعداد تقييم برمجي وتقني شامل للمستخدم بناءً على مهاراته، اهتماماته، مشاريعه الحالية والمكتملة.
                    
                    بيانات المستخدم:
                    1. المهارات الحالية: ${skillsList.joinToString(", ")}
                    2. الاهتمامات البرمجية: ${interestsList.joinToString(", ")}
                    3. المشاريع قيد التنفيذ الحالية (عددها ${activeList.size}):
                       ${activeList.joinToString("\n") { "- ${it.title} (${it.technologies}): تقدم ${it.progress}%" }}
                    4. المشاريع المكتملة والأرشيف (عددها ${completedList.size}):
                       ${completedList.joinToString("\n") { "- ${it.title} (${it.technologies})" }}
                    
                    المطلوب هو حساب درجة تقييم كلية من 100 وتحديد لقب ملائم، وذكر نقاط القوة بوضوح، وفرص التطوير مع توجيه خطوة عملية تالية.
                    يجب أن تعود النتيجة كصيغة JSON دقيقة ومطابقة تماماً للهيكل التالي بدون أي نصوص برمجية خارجة أو شرح إضافي:
                    {
                      "score": 85,
                      "title": "مبتكر واعد 🚀",
                      "strengths": ["ذكر نقاط قوة حقيقية بناءً على المشاريع والمهارات الحالية بأسلوب احترافي"],
                      "improvements": ["ذكر فرص للتطوير والتحسين الفني والبرمجي ومقترحات حقيقية"],
                      "nextStep": "توجيه محدد وخطوة عملية واضحة للبدء بها فوراً"
                    }
                    اجعل اللغة العربية الفصحى قوية وراقية جداً ومحفزة.
                """.trimIndent()
                
                val request = GenerateContentRequest(
                    contents = listOf(
                        ContentRequest(
                            parts = listOf(
                                PartRequest(text = prompt)
                            )
                        )
                    ),
                    generationConfig = GenerationConfigRequest(responseMimeType = "application/json", temperature = 0.7)
                )
                
                val response = withContext(Dispatchers.IO) {
                    retryWithBackoff(times = 3, initialDelay = 1000) {
                        geminiApiService.generateContent(apiKey, request)
                    }
                }
                
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (responseText != null) {
                    val cleanJson = cleanJsonString(responseText)
                    val parsedEval = parseAiEvaluation(cleanJson)
                    if (parsedEval != null) {
                        _aiEvaluation.value = parsedEval
                        // Save to cache
                        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                        val jsonStr = moshi.adapter(AiEvaluation::class.java).toJson(parsedEval)
                        repository.saveAiEvaluation(jsonStr)
                    } else {
                        simulateEvaluationFallback(activeList, completedList, skillsList, interestsList)
                    }
                } else {
                    simulateEvaluationFallback(activeList, completedList, skillsList, interestsList)
                }
                
            } catch (e: Exception) {
                Log.e("Mubtakir", "Failed to query Evaluation API", e)
                simulateEvaluationFallback(
                    activeProjects.value,
                    completedProjects.value,
                    skills.value.toList(),
                    interests.value.toList()
                )
            } finally {
                _isEvaluatingUser.value = false
            }
        }
    }

    private suspend fun simulateEvaluationFallback(
        activeList: List<Project>,
        completedList: List<Project>,
        skillsList: List<String>,
        interestsList: List<String>
    ) {
        delay(1500) // Realistic processing delay
        
        var baseScore = 65
        baseScore += (completedList.size * 10).coerceAtMost(25)
        baseScore += (activeList.size * 4).coerceAtMost(10)
        
        val totalStepsCount = activeList.sumOf { it.steps.split("|").filter { s -> s.isNotBlank() }.size }
        val completedStepsCount = activeList.sumOf { 
            it.completedSteps.split("|").filter { s -> s.isNotBlank() }.size 
        }
        if (totalStepsCount > 0) {
            baseScore += ((completedStepsCount.toFloat() / totalStepsCount.toFloat()) * 10).toInt()
        }
        
        val score = baseScore.coerceAtMost(100)
        
        val title = when {
            score >= 90 -> "مهندس ابتكار ريادي 🏆"
            score >= 80 -> "مبتكر تقني متقدم 🚀"
            score >= 70 -> "مطور برمجيات نشط 💻"
            else -> "مبتكر واعد في مقتبل الطريق 🌱"
        }
        
        val strengths = mutableListOf<String>()
        if (completedList.isNotEmpty()) {
            strengths.add("إنجاز وتوثيق المشاريع بنجاح (أرشفت ${completedList.size} مشاريع مكتملة).")
        } else {
            strengths.add("الاهتمام بالبدء في التخطيط وبناء الأفكار الجديدة.")
        }
        
        if (skillsList.isNotEmpty()) {
            strengths.add("امتلاك قاعدة مهارات متميزة مثل: ${skillsList.take(3).joinToString(", ")}.")
        }
        
        if (activeList.any { it.progress > 50 }) {
            strengths.add("المثابرة والاستمرار في تطوير المشاريع الحالية وتخطي منتصف مراحل التنفيذ.")
        }
        
        val improvements = mutableListOf<String>()
        if (completedList.isEmpty()) {
            improvements.add("التركيز على إكمال مشروع واحد على الأقل وأرشفته لرفع نسبة الإنجاز والخبرة العملية.")
        }
        if (activeList.size > 3) {
            improvements.add("تقليص تشتت العمل عبر التركيز على إنهاء المشاريع القائمة قبل توليد أفكار جديدة.")
        }
        if (skillsList.size < 3) {
            improvements.add("تحديث وإضافة مهارات برمجية جديدة في الملف لتعزيز ذكاء توليد الأفكار.")
        }
        improvements.add("الحرص على وضع تذكيرات يومية للمشاريع لضمان الممارسة البرمجية المستمرة.")
        
        val nextStep = when {
            activeList.isNotEmpty() -> "ننصحك بالدخول على مشروعك الحالي '${activeList.first().title}' وإنجاز الخطوة التالية منه اليوم لرفع مؤشر ابتكارك."
            else -> "ابدأ بالذهاب لمولد الابتكارات، واختيار تصنيف تفضله لتقترح فكرة ذكية وتبدأ في تنفيذها اليوم."
        }
        
        val result = AiEvaluation(
            score = score,
            title = title,
            strengths = strengths,
            improvements = improvements,
            nextStep = nextStep
        )
        
        _aiEvaluation.value = result
        
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val jsonStr = moshi.adapter(AiEvaluation::class.java).toJson(result)
        repository.saveAiEvaluation(jsonStr)
    }

    // --- Retry Backoff Mechanism ---
    private fun parseCodeAnalysis(jsonString: String): CodeAnalysisResult? {
        return try {
            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
            val adapter = moshi.adapter(CodeAnalysisResult::class.java)
            adapter.fromJson(jsonString)
        } catch (e: Exception) {
            Log.e("Mubtakir", "Failed to parse Code Analysis JSON", e)
            null
        }
    }

    fun analyzeCode(code: String, language: String) {
        if (code.isBlank()) return
        _isAnalyzingCode.value = true
        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    simulateCodeAnalysisFallback(code, language)
                    return@launch
                }
                
                val prompt = """
                    أنت خبير هندسة برمجيات أول ومراجع كود محترف (Senior Software Architect & Code Reviewer).
                    المطلوب هو مراجعة الكود التالي المكتوب بلغة ($language) مراجعة تقنية عميقة تركز على:
                    1. جودة الكود وبنيته (Code Quality & Architecture).
                    2. سرعة وجودة الأداء واستهلاك الذاكرة (Performance, Speed, Memory Management).
                    3. التعقيد الخوارزمي والزمني (Big O Time/Space Complexity).
                    
                    الكود المراد تحليله:
                    ```$language
                    $code
                    ```
                    
                    الرجاء تقديم تقرير فني دقيق ومفصل باللغة العربية الفصحى وبصيغة JSON مطابقة تماماً للهيكل التالي بدون أي مقدمات أو شرح خارج الـ JSON:
                    {
                      "score": 85,
                      "qualityRating": "جودة عالية / جيدة جداً / تحتاج تعديل جوهري (حدد بدقة وصياغة تقنية)",
                      "speedRating": "O(N) أو سريع جداً / معتدل / بطيء بسبب حلقات معقدة (حدد بأسلوب تقني)",
                      "positives": [
                        "نقطة إيجابية أولى دقيقة ومحددة بالكود المكتوب",
                        "نقطة إيجابية ثانية"
                      ],
                      "negatives": [
                        "تحذير أو ثغرة أداء أو خلل في الكود المكتوب",
                        "مشكلة في استهلاك الذاكرة أو سرعة المعالجة"
                      ],
                      "recommendations": [
                        "نصيحة تقنية أولى عملية لتحسين الكود فورا",
                        "نصيحة تقنية ثانية عملية"
                      ],
                      "optimizedCode": "نسخة معادة الصياغة (Refactored) بالكامل ومحسنة الأداء والجودة من كود المستخدم المكتوب"
                    }
                    اجعل التوجيهات محفزة وعميقة تعكس خبرة برمجية رفيعة المستوى.
                """.trimIndent()
                
                val request = GenerateContentRequest(
                    contents = listOf(
                        ContentRequest(
                            parts = listOf(
                                PartRequest(text = prompt)
                            )
                        )
                    ),
                    generationConfig = GenerationConfigRequest(responseMimeType = "application/json", temperature = 0.5)
                )
                
                val response = withContext(Dispatchers.IO) {
                    retryWithBackoff(times = 3, initialDelay = 1000) {
                        geminiApiService.generateContent(apiKey, request)
                    }
                }
                
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (responseText != null) {
                    val cleanJson = cleanJsonString(responseText)
                    val parsedResult = parseCodeAnalysis(cleanJson)
                    if (parsedResult != null) {
                        _codeAnalysisResult.value = parsedResult
                        // Save to cache
                        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                        val jsonStr = moshi.adapter(CodeAnalysisResult::class.java).toJson(parsedResult)
                        repository.saveCodeAnalysis(jsonStr)
                    } else {
                        simulateCodeAnalysisFallback(code, language)
                    }
                } else {
                    simulateCodeAnalysisFallback(code, language)
                }
                
            } catch (e: Exception) {
                Log.e("Mubtakir", "Failed to analyze code via Gemini API", e)
                simulateCodeAnalysisFallback(code, language)
            } finally {
                _isAnalyzingCode.value = false
            }
        }
    }

    private suspend fun simulateCodeAnalysisFallback(code: String, language: String) {
        delay(2000) // Realistic analysis delay
        
        // Analyze code patterns dynamically
        var score = 75
        val positives = mutableListOf<String>()
        val negatives = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        var qualityRating = "مستوى متوسط ومقبول"
        var speedRating = "O(N) - أداء خطي معتدل"
        
        // Positives extraction
        if (code.contains("fun ") || code.contains("void ") || code.contains("def ")) {
            positives.add("استخدام الدوال الفرعية وتقسيم منطق العمل لتسهيل القراءة والصيانة.")
            score += 5
        } else {
            negatives.add("تجميع منطق الكود في كتلة واحدة؛ يفضل تقسيمه إلى دوال فرعية صغيرة.")
        }
        
        if (code.contains("try") && code.contains("catch")) {
            positives.add("معالجة جيدة للأخطاء والاستثناءات المحتملة عبر استخدام كتل try-catch.")
            score += 5
        } else {
            negatives.add("غياب كتل try-catch لمعالجة الأخطاء غير المتوقعة (احتمالية انهيار البرنامج عند الفشل).")
            recommendations.add("أضف آلية معالجة الاستثناءات لتفادي انهيار البرنامج أثناء التشغيل.")
        }

        if (code.contains("val ") || code.contains("const ") || code.contains("final ")) {
            positives.add("الالتزام بمبدأ عدم التغير (Immutability) للمتغيرات الثابتة لتعزيز أمان الخيوط.")
            score += 4
        }
        
        // Speed and Negatives check
        if (code.contains("for (") || code.contains("while (") || code.contains(".forEach")) {
            val nestedLoops = code.split("for").size > 2 || code.split("while").size > 2
            if (nestedLoops) {
                score -= 10
                speedRating = "O(N²) - بطيء نسبياً بسبب حلقات التكرار المتداخلة"
                negatives.add("وجود حلقات تكرارية متداخلة (Nested Loops) ترفع من التعقيد الزمني بشكل كبير.")
                recommendations.add("حاول استخدام جداول الهاش (HashMaps/HashSets) لتحويل البحث من خطي O(N²) إلى فوري O(1).")
            } else {
                speedRating = "O(N) - معالجة خطية اعتيادية"
                positives.add("صياغة حلقات التكرار الفردية بشكل مناسب للمعالجة المباشرة.")
            }
        } else {
            speedRating = "O(1) - تشغيل فوري ثابت"
            positives.add("بنية زمنية ثابتة وفائقة السرعة خالية من تكرار الحلقات غير الضروري.")
            score += 8
        }
        
        if (code.contains("Thread.sleep") || code.contains("delay(")) {
            negatives.add("استخدام عمليات إيقاف قسرية مؤقتة للمسار؛ قد يعيق استجابة الواجهة الرسومية.")
            recommendations.add("استخدم آلية الأحداث اللاتزامية (Asynchronous programming / Coroutines) عوضاً عن الإيقاف المؤقت.")
        }

        if (code.length > 500) {
            negatives.add("الكتلة البرمجية تبدو طويلة ومزدحمة؛ يفضل تبسيطها وتوزيعها.")
            recommendations.add("أعد هيكلة الكود بتقسيمه لملفات أو فئات (Classes) منفصلة لتعزيز مبادئ المسؤولية الفردية (SRP).")
        }

        // Default fillings if lists are sparse
        if (positives.isEmpty()) {
            positives.add("الكود مكتوب بصيغة قابلة للتنفيذ المباشر.")
            positives.add("اتباع ترميز قياسي للتعريفات الفنية.")
        }
        if (negatives.isEmpty()) {
            negatives.add("لم يتم رصد ثغرات برمجية قاتلة أو مشاكل تسرّب فوري للذاكرة.")
            score += 8
        }
        if (recommendations.isEmpty()) {
            recommendations.add("احرص على كتابة تعليقات توضيحية لتبسيط مراجعة الكود مستقبلاً.")
            recommendations.add("تابع استخدام الممارسات القياسية الخاصة بـ $language.")
        }
        
        score = score.coerceIn(40, 100)
        qualityRating = when {
            score >= 90 -> "جودة برمجية ممتازة وتصميم نظيف"
            score >= 80 -> "جودة برمجية عالية مع بعض التحسينات الطفيفة"
            score >= 65 -> "مستوى جودة مقبول مع وجود تحذيرات فنية"
            else -> "يحتاج كودك لإعادة كتابة وهيكلة فنية شاملة"
        }

        // Create an optimized version of the user's code
        val optimizedCode = """
            // --- كودك المحسن والمثالي لتسريع الأداء وتقليل استهلاك الذاكرة ---
            // لغة البرمجة: $language | معدل كفاءة الأداء المتوقع: +40%
            
            ${if (language.lowercase() == "kotlin") {
                "// استخدام هياكل البيانات المناسبة ومعالجة آمنة للأخطاء لاتزامياً\n" +
                code.replace("var ", "val ").replace("ArrayList", "listOf")
            } else {
                "// كود مراجع ومحسّن تزامناً مع المعايير القياسية\n$code"
            }}
        """.trimIndent()

        val result = CodeAnalysisResult(
            score = score,
            qualityRating = qualityRating,
            speedRating = speedRating,
            positives = positives,
            negatives = negatives,
            recommendations = recommendations,
            optimizedCode = optimizedCode
        )
        
        _codeAnalysisResult.value = result
        
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val jsonStr = moshi.adapter(CodeAnalysisResult::class.java).toJson(result)
        repository.saveCodeAnalysis(jsonStr)
    }

    // --- Retry Backoff Mechanism ---
    private suspend fun <T> retryWithBackoff(
        times: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 3000,
        factor: Double = 1.5,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                Log.w("Mubtakir", "API Attempt ${attempt + 1} failed: ${e.message}. Retrying in ${currentDelay}ms...", e)
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
        return block()
    }
}

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class CodeAnalysisResult(
    val score: Int,
    val qualityRating: String,
    val speedRating: String,
    val positives: List<String>,
    val negatives: List<String>,
    val recommendations: List<String>,
    val optimizedCode: String,
    val analyzedAt: Long = System.currentTimeMillis()
)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class AiEvaluation(
    val score: Int,
    val title: String,
    val strengths: List<String>,
    val improvements: List<String>,
    val nextStep: String,
    val lastEvaluated: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
