package org.mrlem.composesample.coaching

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
import org.mrlem.composesample.settings.ApiKeyStore
import org.mrlem.composesample.step.StepDao
import org.mrlem.composesample.step.StepEntity
import org.mrlem.composesample.theme.ThemeDao
import org.mrlem.composesample.today.ScheduledStepDao
import org.mrlem.composesample.today.ScheduledStepEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ReviewCoachingViewModel @Inject constructor(
    private val scheduledStepDao: ScheduledStepDao,
    private val stepDao: StepDao,
    private val themeDao: ThemeDao,
    private val coachingMessageDao: CoachingMessageDao,
    private val claudeClient: ClaudeClient,
    private val apiKeyStore: ApiKeyStore,
) : UnidirectionalViewModel<CoachingViewState, CoachingViewAction, Unit>() {

    private val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private val _state = MutableStateFlow(CoachingViewState())
    override val state = _state.stateIn(viewModelScope, WhileSubscribed(), _state.value)

    init {
        viewModelScope.launch {
            val scheduledSteps = scheduledStepDao.getByDate(today)
            val themes = scheduledSteps.mapNotNull { ss ->
                val step = stepDao.getById(ss.stepId) ?: return@mapNotNull null
                themeDao.getById(step.themeId)
            }.distinctBy { it.id }
            _state.update { it.copy(availableThemes = themes.map { t -> ThemeOption(t.id, t.title) }) }

            val saved = coachingMessageDao.getByContext(today, "review")
            if (saved.isNotEmpty()) {
                val messages = saved.map { ChatMessage(role = it.role, content = it.content) }
                _state.update { it.copy(messages = messages) }
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
                is CoachingViewAction.CreateTheme -> Unit
                is CoachingViewAction.ShowAddStep -> {
                    val lastAssistant = _state.value.messages.lastOrNull { it.role == "assistant" }
                    val suggestions = if (lastAssistant != null) extractSuggestedSteps(lastAssistant.content, _state.value.availableThemes) else emptyList()
                    _state.update { it.copy(showAddStep = true, suggestedSteps = suggestions) }
                }
                is CoachingViewAction.HideAddStep -> _state.update { it.copy(showAddStep = false) }
                is CoachingViewAction.AddStep -> {
                    val text = action.title.trim()
                    if (text.isNotEmpty()) {
                        val stepId = UUID.randomUUID().toString()
                        val tomorrow = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(
                            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time
                        )
                        stepDao.insert(
                            StepEntity(
                                id = stepId,
                                themeId = action.themeId,
                                title = text,
                                createdAt = System.currentTimeMillis(),
                            )
                        )
                        scheduledStepDao.insert(
                            ScheduledStepEntity(
                                id = UUID.randomUUID().toString(),
                                stepId = stepId,
                                date = tomorrow,
                            )
                        )
                        stepDao.markToday(stepId)
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun extractSuggestedSteps(text: String, themes: List<ThemeOption>): List<SuggestedStep> {
        val listItemRegex = Regex("""^[-•・*]\s+(.+)""")
        val numberedRegex = Regex("""^\d+[.)、]\s*(.+)""")
        val fallbackThemeId = themes.firstOrNull()?.id ?: return emptyList()

        // テーマ名が含まれる行を検出してセクションのテーマを追跡する
        fun detectThemeInLine(line: String): ThemeOption? =
            themes.firstOrNull { theme -> line.contains(theme.title) }

        val result = mutableListOf<SuggestedStep>()
        var currentThemeId = fallbackThemeId

        for (line in text.lines()) {
            val trimmed = line.trim()
            detectThemeInLine(trimmed)?.let { currentThemeId = it.id }

            val stepTitle = listItemRegex.matchEntire(trimmed)?.groupValues?.get(1)
                ?: numberedRegex.matchEntire(trimmed)?.groupValues?.get(1)
            if (!stepTitle.isNullOrEmpty()) {
                result += SuggestedStep(title = stepTitle, themeId = currentThemeId)
            }
        }

        if (result.isNotEmpty()) return result

        // フォールバック: 文末が「。」または「する」で終わる行
        return text.lines()
            .map { it.trim() }
            .filter { line -> line.length in 5..60 && (line.endsWith("。") || line.endsWith("する") || line.endsWith("みる")) }
            .take(5)
            .map { SuggestedStep(title = it, themeId = fallbackThemeId) }
    }

    private suspend fun buildContextMessage(): String {
        val scheduledSteps = scheduledStepDao.getByDate(today)
        val lines = scheduledSteps.mapNotNull { ss ->
            val step = stepDao.getById(ss.stepId) ?: return@mapNotNull null
            val theme = themeDao.getById(step.themeId) ?: return@mapNotNull null
            val status = when {
                ss.done -> "完了"
                ss.started -> "着手"
                else -> "未着手"
            }
            val memoSuffix = if (ss.memo != null) "（メモ: ${ss.memo}）" else ""
            "- [${theme.title}] ${step.title}: $status$memoSuffix"
        }
        return if (lines.isEmpty()) {
            "今日の振り返りをサポートしてください。今日はスケジュールされたステップはありませんでした。"
        } else {
            "今日の振り返りをサポートしてください。以下が今日取り組んだ（予定の）ステップです：\n${lines.joinToString("\n")}"
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
            claudeClient.send(apiKey, messages.map { ClaudeMessage(it.role, it.content) }, ClaudeClient.REVIEW_SYSTEM_PROMPT)
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
                contextType = "review",
                contextId = today,
                role = msg.role,
                content = msg.content,
                createdAt = System.currentTimeMillis(),
            )
        )
    }
}
