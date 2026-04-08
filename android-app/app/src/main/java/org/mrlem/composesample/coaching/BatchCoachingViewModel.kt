package org.mrlem.composesample.coaching

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.mrlem.android.core.feature.ui.UnidirectionalViewModel
import org.mrlem.composesample.inbox.InboxDao
import org.mrlem.composesample.settings.ApiKeyStore
import org.mrlem.composesample.step.StepDao
import org.mrlem.composesample.step.StepEntity
import org.mrlem.composesample.theme.ThemeDao
import org.mrlem.composesample.theme.ThemeEntity
import java.util.UUID

@HiltViewModel(assistedFactory = BatchCoachingViewModel.Factory::class)
class BatchCoachingViewModel @AssistedInject constructor(
    private val inboxDao: InboxDao,
    private val coachingMessageDao: CoachingMessageDao,
    private val claudeClient: ClaudeClient,
    private val apiKeyStore: ApiKeyStore,
    private val themeDao: ThemeDao,
    private val stepDao: StepDao,
    @Assisted private val key: BatchCoachingKey,
) : UnidirectionalViewModel<CoachingViewState, CoachingViewAction, CoachingViewEffect>() {

    companion object {
        private val EXTRACTION_SYSTEM_PROMPT = """
            会話履歴をもとに、テーマとステップをJSONのみで出力してください。余計なテキストは不要です。

            {"themes":[{"title":"テーマタイトル","goal":"ゴール","weight":"light|medium|heavy","steps":[{"title":"ステップ","starterAction":""}]}]}

            weight: light=1日で終わる, medium=終わりが見えている, heavy=ずっと取り組む
        """.trimIndent()
    }

    @AssistedFactory
    interface Factory {
        fun create(key: BatchCoachingKey): BatchCoachingViewModel
    }

    private val contextId = key.entryIdsJoined

    private val _state = MutableStateFlow(CoachingViewState())
    override val state = _state.stateIn(viewModelScope, WhileSubscribed(), _state.value)

    init {
        viewModelScope.launch {
            val saved = coachingMessageDao.getByContext(contextId, "inbox_batch")
            if (saved.isNotEmpty()) {
                val messages = saved.map { ChatMessage(role = it.role, content = it.content) }
                _state.update { it.copy(messages = messages) }
            } else {
                val entries = key.entryIds.mapNotNull { inboxDao.getById(it) }
                val combinedText = entries.joinToString("\n") { "・${it.text}" }
                val initialMessage = ChatMessage(
                    role = "user",
                    content = "以下の思いつきメモをまとめて整理してください：\n$combinedText",
                )
                saveMessage(initialMessage)
                _state.update { it.copy(messages = listOf(initialMessage)) }
                sendToApi(listOf(initialMessage))
            }
        }

        actions.onEach { action ->
            when (action) {
                is CoachingViewAction.Send -> {
                    val text = action.text.trim()
                    if (text.isEmpty() || _state.value.loading) return@onEach
                    val userMsg = ChatMessage(role = "user", content = text)
                    saveMessage(userMsg)
                    val updated = _state.value.messages + userMsg
                    _state.update { it.copy(messages = updated, error = null) }
                    sendToApi(updated)
                }
                is CoachingViewAction.CreateTheme -> {
                    if (_state.value.loading) return@onEach
                    createThemesFromConversation()
                }
                else -> Unit
            }
        }.launchIn(viewModelScope)
    }

    private fun sendToApi(messages: List<ChatMessage>) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val apiKey = apiKeyStore.claudeApiKey
            if (apiKey.isBlank()) {
                _state.update { it.copy(loading = false, error = "設定画面でAPIキーを入力してください") }
                return@launch
            }
            claudeClient.send(apiKey, messages.map { ClaudeMessage(it.role, it.content) }, ClaudeClient.INBOX_BATCH_SYSTEM_PROMPT)
                .onSuccess { text ->
                    val assistantMsg = ChatMessage(role = "assistant", content = text)
                    saveMessage(assistantMsg)
                    _state.update { it.copy(messages = it.messages + assistantMsg, loading = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.message) }
                }
        }
    }

    private fun createThemesFromConversation() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val apiKey = apiKeyStore.claudeApiKey
            if (apiKey.isBlank()) {
                _state.update { it.copy(loading = false, error = "設定画面でAPIキーを入力してください") }
                return@launch
            }
            val extractionMessages = _state.value.messages.map { ClaudeMessage(it.role, it.content) } +
                    ClaudeMessage(role = "user", content = "以上の会話をもとに、テーマとステップをJSONで生成してください。")
            claudeClient.send(apiKey, extractionMessages, EXTRACTION_SYSTEM_PROMPT)
                .onSuccess { json ->
                    runCatching { parseAndSaveThemes(json) }
                        .onSuccess { firstThemeId ->
                            _state.update { it.copy(loading = false) }
                            if (firstThemeId != null) trigger(CoachingViewEffect.NavigateToTheme(firstThemeId))
                        }
                        .onFailure { e ->
                            _state.update { it.copy(loading = false, error = "テーマの生成に失敗しました: ${e.message}") }
                        }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.message) }
                }
        }
    }

    private suspend fun parseAndSaveThemes(jsonText: String): String? {
        val start = jsonText.indexOf('{')
        val end = jsonText.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val json = JSONObject(jsonText.substring(start, end + 1))
        val themesJson = json.optJSONArray("themes") ?: return null
        val now = System.currentTimeMillis()
        var firstThemeId: String? = null

        for (i in 0 until themesJson.length()) {
            val t = themesJson.getJSONObject(i)
            val themeId = UUID.randomUUID().toString()
            if (firstThemeId == null) firstThemeId = themeId
            themeDao.insert(
                ThemeEntity(
                    id = themeId,
                    title = t.getString("title"),
                    goal = t.optString("goal").takeIf { it.isNotBlank() },
                    weight = t.optString("weight", "medium").let { if (it in listOf("light", "medium", "heavy")) it else "medium" },
                    status = "active",
                    createdAt = now + i,
                )
            )
            val stepsJson = t.optJSONArray("steps")
            if (stepsJson != null) {
                for (j in 0 until stepsJson.length()) {
                    val s = stepsJson.getJSONObject(j)
                    stepDao.insert(
                        StepEntity(
                            id = UUID.randomUUID().toString(),
                            themeId = themeId,
                            title = s.getString("title"),
                            starterAction = s.optString("starterAction").takeIf { it.isNotBlank() },
                            weight = "medium",
                            status = "pending",
                            createdAt = now + i * 100 + j,
                        )
                    )
                }
            }
        }

        // archive all inbox entries
        key.entryIds.forEach { id -> inboxDao.archive(id) }
        return firstThemeId
    }

    private suspend fun saveMessage(msg: ChatMessage) {
        coachingMessageDao.insert(
            CoachingMessageEntity(
                id = UUID.randomUUID().toString(),
                contextType = "inbox_batch",
                contextId = contextId,
                role = msg.role,
                content = msg.content,
                createdAt = System.currentTimeMillis(),
            )
        )
    }
}
