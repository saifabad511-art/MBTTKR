package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<ContentRequest>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfigRequest? = null
)

@JsonClass(generateAdapter = true)
data class ContentRequest(
    @Json(name = "parts") val parts: List<PartRequest>
)

@JsonClass(generateAdapter = true)
data class PartRequest(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfigRequest(
    @Json(name = "responseMimeType") val responseMimeType: String? = "application/json",
    @Json(name = "temperature") val temperature: Double? = 0.7,
    @Json(name = "imageConfig") val imageConfig: ImageConfigRequest? = null,
    @Json(name = "responseModalities") val responseModalities: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class ImageConfigRequest(
    @Json(name = "aspectRatio") val aspectRatio: String,
    @Json(name = "imageSize") val imageSize: String? = null
)

// --- Response Models ---

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<CandidateResponse>?
)

@JsonClass(generateAdapter = true)
data class CandidateResponse(
    @Json(name = "content") val content: ContentResponse?
)

@JsonClass(generateAdapter = true)
data class ContentResponse(
    @Json(name = "parts") val parts: List<PartResponse>?
)

@JsonClass(generateAdapter = true)
data class PartResponse(
    @Json(name = "text") val text: String?,
    @Json(name = "inlineData") val inlineData: InlineDataResponse? = null
)

@JsonClass(generateAdapter = true)
data class InlineDataResponse(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

// --- Structured Output model we expect from Gemini ---
@JsonClass(generateAdapter = true)
data class GeneratedIdeaJson(
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "technologies") val technologies: String, // Comma separated list of tech
    @Json(name = "difficulty") val difficulty: Int, // 1 to 5
    @Json(name = "estimatedDuration") val estimatedDuration: String, // e.g. "5 أيام"
    @Json(name = "steps") val steps: List<String> // Step-by-step instructions
)
