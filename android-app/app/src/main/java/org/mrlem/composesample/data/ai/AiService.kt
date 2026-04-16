package org.mrlem.composesample.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.mrlem.composesample.data.db.ProjectEntity

class AiService(
    private val httpClient: OkHttpClient,
    private val apiKey: String,
) {
    val isAvailable: Boolean get() = apiKey.isNotEmpty()

    /**
     * メモテキストに関連する既存プロジェクトのIDリストを返す（最大3件）。
     * APIキー未設定・エラー時は空リストを返す。
     */
    suspend fun suggestRelatedProjectIds(
        memoText: String,
        projects: List<ProjectEntity>,
    ): List<Long> {
        if (!isAvailable || projects.isEmpty()) return emptyList()
        return withContext(Dispatchers.IO) {
            try {
                val projectList = projects.joinToString("\n") { "- id:${it.id}, title:\"${it.title}\"" }
                val prompt = """
                    新しいメモ「$memoText」に関連する既存プロジェクトを最大3件選んでください。
                    $projectList
                    関連なければ空配列で返してください。
                    必ず次のJSON形式のみで返してください: {"related": [id1, id2]}
                """.trimIndent()
                val json = callApi(prompt) ?: return@withContext emptyList()
                val arr = json.getJSONArray("related")
                (0 until arr.length())
                    .map { arr.getLong(it) }
                    .filter { id -> projects.any { it.id == id } }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    private fun callApi(prompt: String): JSONObject? {
        val body = JSONObject().apply {
            put("model", "claude-haiku-4-5-20251001")
            put("max_tokens", 256)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .post(body)
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return null
        val responseText = response.body?.string() ?: return null
        val content = JSONObject(responseText)
            .getJSONArray("content")
            .getJSONObject(0)
            .getString("text")
            .trim()
        return JSONObject(content)
    }
}
