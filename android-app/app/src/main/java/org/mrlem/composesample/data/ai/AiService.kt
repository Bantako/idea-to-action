package org.mrlem.composesample.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.mrlem.composesample.data.db.NodeEntity

class AiService(
    private val httpClient: OkHttpClient,
    private val apiKey: String,
) {
    val isAvailable: Boolean get() = apiKey.isNotEmpty()

    /**
     * 新しいノードに関連する既存ノードのIDリストを返す（最大3件）。
     * APIキー未設定・エラー時は空リストを返す。
     */
    suspend fun suggestRelatedNodeIds(
        newTitle: String,
        existingNodes: List<NodeEntity>,
    ): List<Long> {
        if (!isAvailable || existingNodes.isEmpty()) return emptyList()
        return withContext(Dispatchers.IO) {
            try {
                val nodeList = existingNodes.joinToString("\n") { "- id:${it.id}, title:\"${it.title}\"" }
                val prompt = """
                    新しいアイデア「$newTitle」に関連する既存ノードを最大3件選んでください。
                    $nodeList
                    関連なければ空配列で返してください。
                    必ず次のJSON形式のみで返してください: {"related": [id1, id2]}
                """.trimIndent()
                val json = callApi(prompt) ?: return@withContext emptyList()
                val arr = json.getJSONArray("related")
                (0 until arr.length())
                    .map { arr.getLong(it) }
                    .filter { id -> existingNodes.any { it.id == id } }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    /**
     * READYなノードを着手しやすい順に並べ替えて返す。
     * APIキー未設定・エラー時は元の順序のまま返す。
     */
    suspend fun rankReadyNodes(nodes: List<NodeEntity>): List<NodeEntity> {
        if (!isAvailable || nodes.size <= 1) return nodes
        return withContext(Dispatchers.IO) {
            try {
                val nodeList = nodes.joinToString("\n") { "- id:${it.id}, title:\"${it.title}\"" }
                val prompt = """
                    今日着手できるノードを着手しやすい順に並べてください。
                    $nodeList
                    必ず次のJSON形式のみで返してください: {"ordered": [id1, id2, ...]}
                    全IDを含めてください。
                """.trimIndent()
                val json = callApi(prompt) ?: return@withContext nodes
                val arr = json.getJSONArray("ordered")
                val ids = (0 until arr.length()).map { arr.getLong(it) }
                val nodeMap = nodes.associateBy { it.id }
                val sorted = ids.mapNotNull { nodeMap[it] }
                val sortedIds = sorted.map { it.id }.toSet()
                sorted + nodes.filter { it.id !in sortedIds }
            } catch (_: Exception) {
                nodes
            }
        }
    }

    /**
     * 選択されたノード群にふさわしいテーマ名を 2〜3 候補返す。
     * APIキー未設定・エラー時は空リストを返す。
     */
    suspend fun suggestThemeNames(nodeTitles: List<String>): List<String> {
        if (!isAvailable || nodeTitles.isEmpty()) return emptyList()
        return withContext(Dispatchers.IO) {
            try {
                val list = nodeTitles.joinToString("\n") { "- \"$it\"" }
                val prompt = """
                    以下のアイデア群をひとまとまりのテーマとして表す短い名前を2〜3個提案してください。
                    $list
                    必ず次のJSON形式のみで返してください: {"names": ["名前1", "名前2", "名前3"]}
                """.trimIndent()
                val json = callApi(prompt) ?: return@withContext emptyList()
                val arr = json.getJSONArray("names")
                (0 until arr.length()).map { arr.getString(it) }
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
