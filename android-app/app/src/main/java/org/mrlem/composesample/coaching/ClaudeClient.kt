package org.mrlem.composesample.coaching

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class ClaudeMessage(val role: String, val content: String)

@Singleton
class ClaudeClient @Inject constructor(private val httpClient: OkHttpClient) {

    companion object {
        private const val MODEL = "claude-haiku-4-5-20251001"
        private const val MAX_TOKENS = 1024
        private const val ENDPOINT = "https://api.anthropic.com/v1/messages"
        const val LAUNCH_SYSTEM_PROMPT = """あなたはアクションコーチです。
ユーザーの「やりたいこと」から、取り組みのゴールと最初のスモールステップを一緒に考えます。

進め方：
- 質問は一度に一つだけ、短く
- ゴール・テーマの重さ（1日で終わる / 終わりが見えている / ずっと取り組む）・最初にできる小さな行動を引き出す
- ユーザーが「テーマを作成する」と思えたら、その旨を伝える

日本語で話してください。"""

        const val REVIEW_SYSTEM_PROMPT = """あなたはアクションコーチです。
ユーザーが今日取り組んだことを振り返り、次のステップを一緒に考えます。

進め方：
- 着手できたことを短く認める
- 次にやると良さそうな小さな行動を1〜2個提案する
- ステップ提案は必ず以下のフォーマットで末尾に含める：
  【提案ステップ】
  - （ステップタイトル）

日本語で話してください。"""

        const val INBOX_BATCH_SYSTEM_PROMPT = """あなたはアクションコーチです。
ユーザーの思いつきメモから、取り組みのゴールと最初のスモールステップを一緒に考えます。

進め方：
- 複数のメモがある場合、まとめて1つのテーマにするか、分けて複数のテーマにするかを提案する
- 質問は一度に一つだけ、短く
- ゴール・テーマの重さ・最初にできる小さな行動を引き出す

日本語で話してください。"""

        const val MORNING_SUGGEST_SYSTEM_PROMPT = """あなたはアクションコーチです。
ユーザーのアクティブなテーマとステップ一覧を見て、今日着手するのに良さそなステップを2〜3個提案してください。

出力形式（JSON）：
[{"stepId": "...", "reason": "一言理由"}]

- 重さの軽いものを優先する
- 最近着手していないものを優先する
- 理由は短く、背中を押す言葉で

日本語で話してください。"""

        const val THEME_FOCUS_SYSTEM_PROMPT = """あなたはアクションコーチです。
ユーザーのアクティブなテーマ一覧と直近の着手状況を見て、今集中すべきテーマを一緒に考えます。

進め方：
- しばらく着手していないテーマについて理由を聞く
- 「今週これに集中してみては？」と1〜2個に絞る提案をする
- アーカイブを勧めるときは押しつけず「一旦お休みにする？」くらいの言い方で

日本語で話してください。"""
    }

    suspend fun send(apiKey: String, messages: List<ClaudeMessage>, systemPrompt: String = LAUNCH_SYSTEM_PROMPT): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().apply {
                put("model", MODEL)
                put("max_tokens", MAX_TOKENS)
                put("system", systemPrompt)
                put("messages", JSONArray().also { arr ->
                    messages.forEach { msg ->
                        arr.put(JSONObject().apply {
                            put("role", msg.role)
                            put("content", msg.content)
                        })
                    }
                })
            }.toString()

            val request = Request.Builder()
                .url(ENDPOINT)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: error("empty response")
            if (!response.isSuccessful) error("API error ${response.code}: $responseBody")

            JSONObject(responseBody)
                .getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
        }
    }
}
