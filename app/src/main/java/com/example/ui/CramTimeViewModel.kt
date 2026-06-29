package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.model.Dossier
import com.example.data.model.StudyTopicAddon
import com.example.data.repository.DossierRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.net.Uri
import android.provider.OpenableColumns
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader

data class Attachment(
    val uri: String,
    val name: String,
    val mimeType: String,
    val base64Data: String? = null,
    val textContent: String? = null
)

sealed interface CramState {
    object Splash : CramState
    object Input : CramState
    data class Generating(val phase: String, val secondsElapsed: Int) : CramState
    data class DossierDetail(val dossier: Dossier) : CramState
}

class CramTimeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DossierRepository
    val allDossiers: StateFlow<List<Dossier>>
    val allAddons = MutableStateFlow<List<StudyTopicAddon>>(emptyList())

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DossierRepository(database.dossierDao())
        
        // Expose dossier list as StateFlow
        val mutableDossiers = MutableStateFlow<List<Dossier>>(emptyList())
        allDossiers = mutableDossiers.asStateFlow()
        
        viewModelScope.launch {
            repository.allDossiers.collect {
                mutableDossiers.value = it
            }
        }

        viewModelScope.launch {
            repository.getAllAddons().collect {
                allAddons.value = it
            }
        }
    }

    // --- Dynamic Time Clock ---
    private val _localTime = MutableStateFlow("")
    val localTime: StateFlow<String> = _localTime.asStateFlow()

    // --- Theme State ---
    val isDarkTheme = MutableStateFlow(true)

    fun toggleTheme() {
        isDarkTheme.value = !isDarkTheme.value
    }

    // --- Interactive Chat State for Refinement Loop ---
    data class ChatMessage(val sender: String, val message: String, val timestamp: Long = System.currentTimeMillis())
    val chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val isSendingChatMessage = MutableStateFlow(false)

    // --- State Management ---
    private val _uiState = MutableStateFlow<CramState>(CramState.Splash)
    val uiState: StateFlow<CramState> = _uiState.asStateFlow()

    // --- Study Attachments (Photos/Documents) State ---
    val attachments = MutableStateFlow<List<Attachment>>(emptyList())

    // --- Study Topic Addons (Notes & Shared YouTube Links) ---
    val activeAddons = MutableStateFlow<List<StudyTopicAddon>>(emptyList())
    private var addonsJob: Job? = null

    fun addAttachment(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            val contentResolver = context.contentResolver
            
            var name = "attachment_${System.currentTimeMillis()}"
            try {
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            name = it.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            
            try {
                if (mimeType.startsWith("image/")) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bytes = inputStream.readBytes()
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bitmap != null) {
                            val maxDim = 1024
                            val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                                val newWidth = if (ratio > 1) maxDim else (maxDim * ratio).toInt()
                                val newHeight = if (ratio > 1) (maxDim / ratio).toInt() else maxDim
                                Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                            } else {
                                bitmap
                            }
                            
                            val outputStream = ByteArrayOutputStream()
                            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                            val compressedBytes = outputStream.toByteArray()
                            val base64 = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
                            
                            val attachment = Attachment(
                                uri = uri.toString(),
                                name = name,
                                mimeType = "image/jpeg",
                                base64Data = base64
                            )
                            attachments.value = attachments.value + attachment
                        }
                    }
                } else if (mimeType.startsWith("text/") || mimeType == "application/json" || name.endsWith(".txt") || name.endsWith(".csv") || name.endsWith(".md")) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        val stringBuilder = java.lang.StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            stringBuilder.append(line).append("\n")
                        }
                        val text = stringBuilder.toString()
                        val attachment = Attachment(
                            uri = uri.toString(),
                            name = name,
                            mimeType = mimeType,
                            textContent = text
                        )
                        attachments.value = attachments.value + attachment
                    }
                } else {
                    val attachment = Attachment(
                        uri = uri.toString(),
                        name = name,
                        mimeType = mimeType,
                        textContent = "[Study Document: $name]"
                    )
                    attachments.value = attachments.value + attachment
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeAttachment(attachment: Attachment) {
        attachments.value = attachments.value.filter { it.uri != attachment.uri }
    }

    fun clearAttachments() {
        attachments.value = emptyList()
    }

    // --- Form Inputs ---
    val topic = MutableStateFlow("")
    val selectedMode = MutableStateFlow("Exam") // "Exam", "Interview", "Presentation"
    val deadlineText = MutableStateFlow("12 Hours") // "3 Hours", "12 Hours", "24 Hours", "3 Days", "7 Days"
    val companyBriefing = MutableStateFlow("")
    val slideCount = MutableStateFlow(5)

    // --- Active Timer for current Cram session ---
    private val _countdownTimer = MutableStateFlow("")
    val countdownTimer: StateFlow<String> = _countdownTimer.asStateFlow()

    private var clockJob: Job? = null
    private var countdownJob: Job? = null

    init {
        startLocalClock()
    }

    private fun startLocalClock() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            while (true) {
                _localTime.value = sdf.format(Date())
                delay(1000)
            }
        }
    }

    fun selectMode(mode: String) {
        selectedMode.value = mode
    }

    fun selectDeadline(deadline: String) {
        deadlineText.value = deadline
    }

    fun setSlideCount(count: Int) {
        slideCount.value = count
    }

    fun loadDossier(dossier: Dossier) {
        _uiState.value = CramState.DossierDetail(dossier)
        startCountdownTimer(dossier.deadlineText, dossier.timestamp)

        addonsJob?.cancel()
        addonsJob = viewModelScope.launch {
            repository.getAddonsForDossier(dossier.id).collect {
                activeAddons.value = it
            }
        }
    }

    fun deleteDossier(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
            if (_uiState.value is CramState.DossierDetail) {
                val active = (_uiState.value as CramState.DossierDetail).dossier
                if (active.id == id) {
                    goBackToInput()
                }
            }
        }
    }

    fun goBackToInput() {
        _uiState.value = CramState.Input
        countdownJob?.cancel()
        _countdownTimer.value = ""
        addonsJob?.cancel()
        activeAddons.value = emptyList()
    }

    fun saveTopicAddon(dossierId: Int, topicName: String, notes: String, youtubeLinks: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getAddonByTopic(dossierId, topicName)
            val toSave = if (existing != null) {
                existing.copy(notes = notes, youtubeLinks = youtubeLinks)
            } else {
                StudyTopicAddon(
                    dossierId = dossierId,
                    topicName = topicName,
                    notes = notes,
                    youtubeLinks = youtubeLinks
                )
            }
            repository.saveStudyTopicAddon(toSave)
        }
    }

    fun toggleTopicCompletion(dossierId: Int, topicName: String, isCompleted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getAddonByTopic(dossierId, topicName)
            val toSave = if (existing != null) {
                existing.copy(isCompleted = isCompleted)
            } else {
                StudyTopicAddon(
                    dossierId = dossierId,
                    topicName = topicName,
                    isCompleted = isCompleted
                )
            }
            repository.saveStudyTopicAddon(toSave)
        }
    }

    private fun startCountdownTimer(deadline: String, startTimeMillis: Long) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            val durationMillis = when (deadline) {
                "3 Hours" -> TimeUnit.HOURS.toMillis(3)
                "12 Hours" -> TimeUnit.HOURS.toMillis(12)
                "24 Hours" -> TimeUnit.HOURS.toMillis(24)
                "3 Days" -> TimeUnit.DAYS.toMillis(3)
                "7 Days" -> TimeUnit.DAYS.toMillis(7)
                else -> TimeUnit.HOURS.toMillis(12)
            }
            val endTime = startTimeMillis + durationMillis

            while (true) {
                val remaining = endTime - System.currentTimeMillis()
                if (remaining <= 0) {
                    _countdownTimer.value = "00:00:00:00"
                    break
                }

                val days = TimeUnit.MILLISECONDS.toDays(remaining)
                val hours = TimeUnit.MILLISECONDS.toHours(remaining) % 24
                val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60
                val seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60

                _countdownTimer.value = String.format(
                    Locale.getDefault(),
                    "%02dD %02dH %02dM %02dS",
                    days, hours, minutes, seconds
                )
                delay(1000)
            }
        }
    }

    // --- Generate Cram Dossier ---
    fun generateCramMaterial() {
        if (topic.value.trim().isEmpty()) return

        val topicVal = topic.value.trim()
        val modeVal = selectedMode.value
        val deadlineVal = deadlineText.value
        val companyVal = companyBriefing.value.trim()
        val slidesVal = slideCount.value

        viewModelScope.launch {
            var seconds = 0
            val stages = when (modeVal) {
                "Exam" -> listOf(
                    "Scoping syllabus boundaries...",
                    "Analyzing priority concept matrices...",
                    "Injecting high-yield question vectors...",
                    "Structuring interactive flashcards...",
                    "Finalizing Preparation Material JSON..."
                )
                "Interview" -> listOf(
                    "Assessing corporate briefing parameters...",
                    "Synthesizing tech stack expectations...",
                    "Formulating tough technical/behavioral probes...",
                    "Fusing optimal response blueprints...",
                    "Formatting mock interview split-pane JSON..."
                )
                else -> listOf(
                    "Drafting slide structural flow outline...",
                    "Synthesizing key narrative takeaways...",
                    "Calculating high-impact slide summaries...",
                    "Creating slide deck layout blueprint...",
                    "Compiling slide deck presentation JSON..."
                )
            }

            _uiState.value = CramState.Generating(stages[0], 0)

            val loadingJob = launch {
                while (true) {
                    delay(1000)
                    seconds++
                    val stageIdx = (seconds / 3).coerceAtMost(stages.size - 1)
                    _uiState.value = CramState.Generating(stages[stageIdx], seconds)
                }
            }

            val resultJson = callGeminiAPI(topicVal, modeVal, deadlineVal, companyVal, slidesVal)
            loadingJob.cancel()

            if (resultJson != null) {
                val dossier = Dossier(
                    topic = topicVal,
                    mode = modeVal,
                    deadlineText = deadlineVal,
                    materialText = resultJson,
                    companyBriefing = companyVal,
                    slideCount = slidesVal,
                    timestamp = System.currentTimeMillis()
                )
                val newId = withContext(Dispatchers.IO) {
                    repository.insert(dossier)
                }
                val savedDossier = dossier.copy(id = newId.toInt())
                loadDossier(savedDossier)
            } else {
                // Return to input but retain topic on failure
                _uiState.value = CramState.Input
            }
        }
    }

    private suspend fun callGeminiAPI(
        topic: String,
        mode: String,
        deadline: String,
        companyBrief: String,
        slideCount: Int
    ): String? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Return high-yield offline mock material if API key is not yet set
            return@withContext getLocalFallbackData(topic, mode, deadline, companyBrief, slideCount)
        }

        val prompt = when (mode) {
            "Exam" -> """
                You are a premium AI Exam Syllabus architect. Generate high-yield, high-impact last hour/minute preparation material for this topic: "$topic".
                The student has only $deadline remaining, so emphasize highly critical, frequently tested details!
                
                You must respond with a raw JSON object (strictly NO markdown formatting, NO code blocks, NO text before or after the JSON) matching this exact format:
                {
                  "summary": "A 2-sentence urgent, high-yield summary of what is essential to master.",
                  "syllabus": [
                    {
                      "topic": "Subtopic Title 1",
                      "importance": "CRITICAL",
                      "content": "Deep, condensed high-yield explanation of this subtopic.",
                      "studyNotes": "Extremely detailed study notes, concepts, explanations, cheat-sheets, key definitions, or code/formulas created by the AI to help the student prepare thoroughly for this specific topic.",
                      "youtubeVideos": [
                        {
                          "title": "Clear and appealing Title of a recommended YouTube tutorial or explanation",
                          "url": "A precise YouTube search query link for the best explanation of this topic, formatted as: https://www.youtube.com/results?search_query=... (replace spaces with +)"
                        }
                      ]
                    }
                  ],
                  "flashcards": [
                    {
                      "question": "Ultra high-yield exam question testing a key differentiator?",
                      "answer": "Perfect structural answer that secures full marks on the exam."
                    }
                  ]
                }
                Ensure you provide exactly 3-5 subtopics in 'syllabus' and 4-6 high-quality 'flashcards'. Each syllabus subtopic MUST have comprehensive, thorough 'studyNotes' and 2-3 extremely relevant 'youtubeVideos' search-query links.
            """.trimIndent()

            "Interview" -> """
                You are a master Technical Recruiter. Generate a high-utility Interview prep briefing for a Candidate preparing for this topic: "$topic".
                ${if (companyBrief.isNotEmpty()) "Target Company details: $companyBrief" else ""}
                The interview is in $deadline, so focus on high-signal interview questions and corporate strategies.
                
                You must respond with a raw JSON object (strictly NO markdown formatting, NO code blocks, NO text before or after the JSON) matching this exact format:
                {
                  "companyBriefing": "Concise brief of company profile, engineering culture expectations, and core values.",
                  "roleStrategy": "Three vital strategies to stand out in the interview for this specialized topic.",
                  "questions": [
                    {
                      "question": "A tough technical or behavioral question matching this role?",
                      "idealResponse": "A highly professional, structured answer (using STAR method if behavioral) showcasing deep capability.",
                      "talkingPoints": [
                        "Key structural keyword or framework 1",
                        "Vital technical metric or story point 2"
                      ]
                    }
                  ]
                }
                Provide 4-5 tough questions with detailed idealResponses and talkingPoints.
            """.trimIndent()

            else -> """
                You are an award-winning UI Presentation Pitch Designer. Outline a slide deck briefing for this topic: "$topic" containing exactly $slideCount slides.
                The presentation is in $deadline, so keep slides concise, compelling, and high-signal.
                
                You must respond with a raw JSON object (strictly NO markdown formatting, NO code blocks, NO text before or after the JSON) matching this exact format:
                {
                  "title": "Compelling Main Presentation Title",
                  "subtitle": "High-impact subtitle or hook statement.",
                  "slides": [
                    {
                      "slideNumber": 1,
                      "heading": "Slide Title",
                      "bulletPoints": [
                        "Powerful bullet argument 1",
                        "Data-driven callout 2",
                        "Strategic vision takeaway 3"
                      ],
                      "visualTip": "Suggested visual asset, graphic composition, or layout structure description."
                    }
                  ]
                }
                Provide exactly $slideCount slides mapping slideNumber from 1 to $slideCount.
            """.trimIndent()
        }

        // Process text and image attachments to enrich study context
        val textDocsContext = attachments.value
            .filter { it.textContent != null }
            .joinToString("\n\n") { "=== ATTACHMENT: ${it.name} ===\n${it.textContent}" }

        val imageInstructions = if (attachments.value.any { it.base64Data != null }) {
            "\n\n[NOTE] The user has attached photos (images) as primary sources of study notes/worksheets. Analyze the attached images and prioritize extracting their visible formulas, diagrams, definitions, syllabus items, and study text to construct this high-yield prep material!"
        } else {
            ""
        }

        val enrichedPrompt = if (textDocsContext.isNotEmpty()) {
            prompt + imageInstructions + "\n\nAdditional primary source document context:\n" + textDocsContext
        } else {
            prompt + imageInstructions
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", enrichedPrompt)
                        })
                        // Add each base64 image as an inlineData part
                        attachments.value.forEach { attachment ->
                            if (attachment.base64Data != null) {
                                put(JSONObject().apply {
                                    put("inlineData", JSONObject().apply {
                                        put("mimeType", attachment.mimeType)
                                        put("data", attachment.base64Data)
                                    })
                                })
                            }
                        }
                    })
                })
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonRequest.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val bodyString = response.body?.string() ?: return@withContext null
            
            val jsonResponse = JSONObject(bodyString)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val firstPart = parts?.optJSONObject(0)
            val rawText = firstPart?.optString("text") ?: return@withContext null

            // Clean markdown code blocks from the response text
            var cleaned = rawText.trim()
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substringAfter("```json").substringBeforeLast("```").trim()
            } else if (cleaned.startsWith("```")) {
                cleaned = cleaned.substringAfter("```").substringBeforeLast("```").trim()
            }
            cleaned
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getLocalFallbackData(
        topic: String,
        mode: String,
        deadline: String,
        companyBrief: String,
        slideCount: Int
    ): String {
        // Return a perfectly mock-formatted JSON matching each mode when Offline or No API key is loaded.
        return when (mode) {
            "Exam" -> {
                JSONObject().apply {
                    put("summary", "Essential cheat-sheet for $topic. Focus deeply on architectural design patterns, trade-offs, and critical system scaling thresholds within this short $deadline window.")
                    put("syllabus", JSONArray().apply {
                        put(JSONObject().apply {
                            put("topic", "Core Fundamentals & Lifecycle")
                            put("importance", "CRITICAL")
                            put("content", "Master state storage, lazy allocation, and custom drawing parameters. Make sure to identify edge-cases where resources are recycled and avoid memory leaks.")
                            put("studyNotes", "### Core Fundamentals & Lifecycle Notes\n\n1. **State Storage & Restoration**\n- Always remember to use `rememberSaveable` for state you want to survive configuration changes (like screen rotations).\n- Use customized `Saver` implementations if storing complex objects that aren't natively supported by Bundle types.\n\n2. **Avoid Common Memory Leaks**\n- Never leak references of localized `Context` objects inside static singletons or background threads.\n- Ensure `DisposableEffect` is employed to clean up listeners or clean references when recompositions complete.")
                            put("youtubeVideos", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("title", "Android Core Lifecycle Deep Dive")
                                    put("url", "https://www.youtube.com/results?search_query=Android+Core+Lifecycle+Deep+Dive")
                                })
                                put(JSONObject().apply {
                                    put("title", "Avoid Memory Leaks in Jetpack Compose")
                                    put("url", "https://www.youtube.com/results?search_query=Avoid+Memory+Leaks+in+Jetpack+Compose")
                                })
                            })
                        })
                        put(JSONObject().apply {
                            put("topic", "Performance Scaling Limits")
                            put("importance", "HIGH")
                            put("content", "Evaluate network batch boundaries, cache hit-ratios, and multi-thread concurrency models. Understand read-modify-write patterns and standard transaction scopes.")
                            put("studyNotes", "### Performance Scaling Limits Study Guide\n\n- **Database Transactions**: Read-modify-write patterns must be completed within a localized database transaction scope to guarantee transactional integrity.\n- **Network Batching**: Consolidate micro-network updates to prevent battery depletion and radio wake-up overhead.\n- **Recomposition Counts**: Use standard metrics analysis with tools like Layout Inspector to identify and fix unnecessary recompositions.")
                            put("youtubeVideos", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("title", "Optimizing Database Transactions in SQLite")
                                    put("url", "https://www.youtube.com/results?search_query=Optimizing+Database+Transactions+in+SQLite")
                                })
                            })
                        })
                        put(JSONObject().apply {
                            put("topic", "Differentiating Features")
                            put("importance", "MEDIUM")
                            put("content", "Explore customized adaptive layout classes, touch accessibility bounds (minimum 48dp), and type-safe route serializations.")
                            put("studyNotes", "### Differentiating Features & Accessibility Guide\n\n- **Touch Targets**: All clickable and interactive elements MUST adhere to minimum touch bounds of at least 48dp x 48dp. Under Compose, Material Design 3 includes this behavior natively through Material Components.\n- **Serialization Routing**: Jetpack Navigation Compose now supports type-safe navigation classes natively. Avoid using unstable raw strings for navigation parameters where possible.")
                            put("youtubeVideos", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("title", "Material 3 Adaptive Layouts & Touch Targets")
                                    put("url", "https://www.youtube.com/results?search_query=Material+3+Adaptive+Layouts+Touch+Targets")
                                })
                            })
                        })
                    })
                    put("flashcards", JSONArray().apply {
                        put(JSONObject().apply {
                            put("question", "What is the primary factor affecting memory leaks during local database resource creation?")
                            put("answer", "Failing to scope connection handlers to application contexts, causing lifecycle binds on transient UI activities.")
                        })
                        put(JSONObject().apply {
                            put("question", "How is Touch Accessibility target dimensions calculated in custom Compose layouts?")
                            put("answer", "Applying minimumInteractiveComponentSize modifier ensures interactive elements maintain a baseline of 48dp target.")
                        })
                        put(JSONObject().apply {
                            put("question", "What defines standard database optimistic locking conflicts?")
                            put("answer", "Tracking records with a version integer or timestamp, preventing concurrent writes from overwriting changes without validation.")
                        })
                    })
                }.toString()
            }
            "Interview" -> {
                JSONObject().apply {
                    put("companyBriefing", "Engineering first-culture focused heavily on low-latency transactions, reliable distributed storage, and responsive mobile architectures. ${if (companyBrief.isNotEmpty()) "Targeting: $companyBrief" else ""}")
                    put("roleStrategy", "1. Frame response with clear STAR methodology structure.\n2. Quantify performance achievements (e.g. reduced load times by 40%).\n3. Show clear trade-off awareness between quick rollout and perfect code cleanups.")
                    put("questions", JSONArray().apply {
                        put(JSONObject().apply {
                            put("question", "How would you handle high-density network updates in an offline-first system?")
                            put("idealResponse", "Implement a buffered local storage tier using Room, syncing asynchronously with adaptive exponential backoffs to prevent connection flooding under poor networks.")
                            put("talkingPoints", JSONArray().apply {
                                put("Buffered database persistence")
                                put("Exponential backoff retry")
                                put("Network connection state listeners")
                            })
                        })
                        put(JSONObject().apply {
                            put("question", "Describe your experience debugging high-cpu bottlenecks.")
                            put("idealResponse", "Profile the execution using Systrace/Profiler to identify nested recompositions, offloading intensive parsing logic to a background coroutine dispatcher.")
                            put("talkingPoints", JSONArray().apply {
                                put("Profiler profiling maps")
                                put("Recomposition counters")
                                put("Background coroutine offloading")
                            })
                        })
                    })
                }.toString()
            }
            else -> {
                JSONObject().apply {
                    put("title", "Deep-Dive Mastering of $topic")
                    put("subtitle", "Strategic overview and tactical action points for immediate execution.")
                    put("slides", JSONArray().apply {
                        for (i in 1..slideCount) {
                            put(JSONObject().apply {
                                put("slideNumber", i)
                                put("heading", "Phase $i: Specialized Action Blueprint")
                                put("bulletPoints", JSONArray().apply {
                                    put("Identify critical subcomponents and scope boundaries.")
                                    put("Execute core architecture patterns cleanly with zero downtime.")
                                    put("Measure performance outcomes and deploy incremental polish.")
                                })
                                put("visualTip", "Clean horizontal split-panel: Left text arguments, Right subtle neon dashboard wireframe illustrating state flow.")
                                put("imageSearchKeyword", "technology_design_abstract")
                            })
                        }
                    })
                }.toString()
            }
        }
    }

    suspend fun callGeminiAPIDirect(prompt: String): String? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            delay(1000)
            return@withContext null
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonRequest.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val bodyString = response.body?.string() ?: return@withContext null
            
            val jsonResponse = JSONObject(bodyString)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val firstPart = parts?.optJSONObject(0)
            val rawText = firstPart?.optString("text") ?: return@withContext null

            var cleaned = rawText.trim()
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substringAfter("```json").substringBeforeLast("```").trim()
            } else if (cleaned.startsWith("```")) {
                cleaned = cleaned.substringAfter("```").substringBeforeLast("```").trim()
            }
            cleaned
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun sendChatMessage(userText: String, dossier: Dossier) {
        if (userText.trim().isEmpty()) return
        viewModelScope.launch {
            val userMsg = ChatMessage("User", userText)
            chatMessages.value = chatMessages.value + userMsg
            isSendingChatMessage.value = true

            // Formulate chat context
            val contextPrompt = """
                You are Zero Hour's premium exam and study architect. Your job is to update and refine the last hour/minute preparation dossier according to user requests.
                
                ORIGINAL TOPIC: ${dossier.topic}
                MODE: ${dossier.mode}
                DEADLINE: ${dossier.deadlineText}
                
                ORIGINAL MATERIAL:
                ${dossier.materialText}
                
                USER'S REFINEMENT REQUEST:
                "$userText"
                
                Your response MUST be the FULL, UPDATED last hour/minute preparation material JSON object (strictly NO markdown formatting, NO code blocks, NO text before or after the JSON) matching the exact schema format expected for ${dossier.mode}.
                Do not talk to the user. Simply return the perfect complete new JSON so the layout parses it perfectly.
            """.trimIndent()

            val refinedJson = callGeminiAPIDirect(contextPrompt)
            isSendingChatMessage.value = false

            if (refinedJson != null) {
                val updatedDossier = dossier.copy(materialText = refinedJson)
                withContext(Dispatchers.IO) {
                    repository.update(updatedDossier)
                }
                _uiState.value = CramState.DossierDetail(updatedDossier)
                
                val assistantMsg = ChatMessage("Assistant", "Refinement complete! Dossier material updated.")
                chatMessages.value = chatMessages.value + assistantMsg
            } else {
                val errorMsg = ChatMessage("Assistant", "Failed to refine dossier material. Please check connection.")
                chatMessages.value = chatMessages.value + errorMsg
            }
        }
    }
}
