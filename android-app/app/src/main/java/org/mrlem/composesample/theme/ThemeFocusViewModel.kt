package org.mrlem.composesample.theme

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mrlem.android.core.feature.ui.UnidirectionalViewModel
import org.mrlem.composesample.coaching.ChatMessage
import org.mrlem.composesample.coaching.ClaudeClient
import org.mrlem.composesample.coaching.ClaudeMessage
import org.mrlem.composesample.coaching.CoachingMessageDao
import org.mrlem.composesample.coaching.CoachingMessageEntity
import org.mrlem.composesample.coaching.CoachingViewAction
import org.mrlem.composesample.coaching.CoachingViewState
import org.mrlem.composesample.coaching.ThemeOption
import org.mrlem.composesample.settings.ApiKeyStore
import org.mrlem.composesample.step.StepDao
import org.mrlem.composesample.today.ScheduledStepDao
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ThemeFocusViewModel @Inject constructor(
    private val themeDao: ThemeDao,
    private val stepDao: StepDao,
    private val scheduledStepDao: ScheduledStepDao,
    private val coachingMessageDao: CoachingMessageDao,
    private val claudeClient: ClaudeClient,
    private val apiKeyStore: ApiKeyStore,
) : UnidirectionalViewModel<CoachingViewState, CoachingViewAction, Unit>() {

    private val contextId = "theme_focus"
    private val _state = MutableStateFlow(CoachingViewState())
    override val state = _state.stateIn(viewModelScope, WhileSubscribed(), _state.value)

    init {
        viewModelScope.launch {
            val saved = coachingMessageDao.getByContext(contextId, "theme_focus")
            if (saved.isNotEmpty()) {
                val messages = saved.map { ChatMessage(role = it.role, content = it.content) }
                val themes = themeDao.getActive()
                _state.update {
                    it.copy(
                        messages = messages,
                        availableThemes = themes.map { t -> ThemeOption(t.id, t.title) },
                    )
                }
            } else {
                val contextMsg = buildContextMessage()
                val initialMessage = ChatMessage(role = "user", content = contextMsg)
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
                else -> Unit
            }
        }.launchIn(viewModelScope)
    }

    private suspend fun buildContextMessage(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val themes = themeDao.getActive()
        val lines = themes.map { theme ->
            val steps = stepDao.getActiveByTheme(theme.id)
            val allScheduled = steps.flatMap { step ->
                scheduledStepDao.getByStep(step.id)
            }
            val lastDate = allScheduled.maxOfOrNull { it.date } ?: "未着手"
            "- ${theme.title}（重さ:${theme.weight}）　最終着手: $lastDate　ステップ数: ${steps.size}"
        }
        return if (lines.isEmpty()) {
            "アクティブなテーマがありません。"
        } else {
            "アクティブなテーマ一覧です。今集中すべきテーマを整理するのを手伝ってください：\n${lines.joinToString("\n")}"
        }
    }

    private fun sendToApi(messages: List<ChatMessage>) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val apiKey = apiKeyStore.claudeApiKey
            if (apiKey.isBlank()) {
                _state.update { it.copy(loading = false, error = "設定画面でAPIキーを入力してください") }
                return@launch
            }
            claudeClient.send(apiKey, messages.map { ClaudeMessage(it.role, it.content) }, ClaudeClient.THEME_FOCUS_SYSTEM_PROMPT)
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

    private suspend fun saveMessage(msg: ChatMessage) {
        coachingMessageDao.insert(
            CoachingMessageEntity(
                id = UUID.randomUUID().toString(),
                contextType = "theme_focus",
                contextId = contextId,
                role = msg.role,
                content = msg.content,
                createdAt = System.currentTimeMillis(),
            )
        )
    }
}
