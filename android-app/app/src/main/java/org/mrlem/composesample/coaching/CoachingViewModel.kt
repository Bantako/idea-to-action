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
import org.mrlem.composesample.inbox.InboxEntryKey
import org.mrlem.composesample.settings.ApiKeyStore
import org.mrlem.composesample.step.StepDao
import org.mrlem.composesample.step.StepEntity
import org.mrlem.composesample.theme.ThemeDao
import org.mrlem.composesample.theme.ThemeEntity
import java.util.UUID

@HiltViewModel(assistedFactory = CoachingViewModel.Factory::class)
class CoachingViewModel @AssistedInject constructor(
    private val inboxDao: InboxDao,
    private val coachingMessageDao: CoachingMessageDao,
    private val claudeClient: ClaudeClient,
    private val apiKeyStore: ApiKeyStore,
    private val themeDao: ThemeDao,
    private val stepDao: StepDao,
    @Assisted private val key: InboxEntryKey,
) : UnidirectionalViewModel<CoachingViewState, CoachingViewAction, CoachingViewEffect>() {

    companion object {
        private val EXTRACTION_SYSTEM_PROMPT = """
            会話履歴をもとに、テーマとステップをJSONのみで出力してください。余計なテキストは不要です。

            {"title":"テーマタイトル","goal":"ゴール","weight":"light|medium|heavy","steps":[{"title":"ステップ","starterAction":"入口アクション（なければ空文字）","weight":"light|medium|heavy"}]}

            weight: light=1日で終わる, medium=終わりが見えている, heavy=ずっと取り組む
        """.trimIndent()
    }

    @AssistedFactory
    interface Factory {
        fun create(key: InboxEntryKey): CoachingViewModel
    }

    private val _state = MutableStateFlow(CoachingViewState())
    override val state = _state.stateIn(viewModelScope, WhileSubscribed(), _state.value)

    init {
        viewModelScope.launch {
            val entry = inboxDao.getById(key.entryId) ?: return@launch
            val saved = coachingMessageDao.getByContext(key.entryId, "launch")
            if (saved.isNotEmpty()) {
                val messages = saved.map { ChatMessage(role = it.role, content = it.content) }
                _state.update { it.copy(inboxText = entry.text, messages = messages) }
            } else {
                val initialMessage = ChatMessage(role = "user", content = entry.text)
                saveMessage(initialMessage)
                _state.update { it.copy(inboxText = entry.text, messages = listOf(initialMessage)) }
                sendToApi(listOf(initialMessage))
            }
        }

        actions
            .onEach { action ->
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
                        createThemeFromConversation()
                    }
                    is CoachingViewAction.ShowAddStep -> Unit
                    is CoachingViewAction.HideAddStep -> Unit
                    is CoachingViewAction.AddStep -> Unit
                }
            }
            .launchIn(viewModelScope)
    }

    private fun sendToApi(messages: List<ChatMessage>) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val apiKey = apiKeyStore.claudeApiKey
            if (apiKey.isBlank()) {
                _state.update { it.copy(loading = false, error = "設定画面でAPIキーを入力してください") }
                return@launch
            }
            claudeClient.send(apiKey, messages.map { ClaudeMessage(it.role, it.content) })
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

    private fun createThemeFromConversation() {
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
                    runCatching { parseAndSaveTheme(json) }
                        .onSuccess { themeId ->
                            _state.update { it.copy(loading = false) }
                            trigger(CoachingViewEffect.NavigateToTheme(themeId))
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

    private suspend fun parseAndSaveTheme(jsonText: String): String {
        val json = JSONObject(extractJson(jsonText))
        val themeId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val theme = ThemeEntity(
            id = themeId,
            title = json.getString("title"),
            goal = json.optString("goal").takeIf { it.isNotBlank() },
            weight = json.optString("weight", "medium").let {
                if (it in listOf("light", "medium", "heavy")) it else "medium"
            },
            status = "active",
            createdAt = now,
        )
        themeDao.insert(theme)
        inboxDao.archiveWithTheme(key.entryId, themeId)

        val stepsJson = json.optJSONArray("steps")
        if (stepsJson != null) {
            for (i in 0 until stepsJson.length()) {
                val s = stepsJson.getJSONObject(i)
                val step = StepEntity(
                    id = UUID.randomUUID().toString(),
                    themeId = themeId,
                    title = s.getString("title"),
                    starterAction = s.optString("starterAction").takeIf { it.isNotBlank() },
                    weight = s.optString("weight", "medium").let {
                        if (it in listOf("light", "medium", "heavy")) it else "medium"
                    },
                    status = "pending",
                    createdAt = now + i,
                )
                stepDao.insert(step)
            }
        }

        return themeId
    }

    private fun extractJson(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start >= 0 && end > start) text.substring(start, end + 1) else text
    }

    private suspend fun saveMessage(msg: ChatMessage) {
        coachingMessageDao.insert(
            CoachingMessageEntity(
                id = UUID.randomUUID().toString(),
                contextType = "launch",
                contextId = key.entryId,
                role = msg.role,
                content = msg.content,
                createdAt = System.currentTimeMillis(),
            )
        )
    }
}
