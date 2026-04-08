package org.mrlem.composesample.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.mrlem.composesample.coaching.ClaudeClient
import org.mrlem.composesample.coaching.ClaudeMessage
import org.mrlem.composesample.settings.ApiKeyStore
import org.mrlem.composesample.step.StepDao
import org.mrlem.composesample.theme.ThemeDao
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class SuggestedStep(
    val stepId: String,
    val stepTitle: String,
    val themeName: String,
    val reason: String,
)

data class MorningSuggestState(
    val loading: Boolean = false,
    val error: String? = null,
    val suggestions: List<SuggestedStep> = emptyList(),
    val acceptedIds: Set<String> = emptySet(),
)

@HiltViewModel
class MorningSuggestViewModel @Inject constructor(
    private val themeDao: ThemeDao,
    private val stepDao: StepDao,
    private val scheduledStepDao: ScheduledStepDao,
    private val claudeClient: ClaudeClient,
    private val apiKeyStore: ApiKeyStore,
) : ViewModel() {

    private val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private val _state = MutableStateFlow(MorningSuggestState())
    val state: StateFlow<MorningSuggestState> = _state.asStateFlow()

    fun suggest() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, suggestions = emptyList()) }
            val apiKey = apiKeyStore.claudeApiKey
            if (apiKey.isBlank()) {
                _state.update { it.copy(loading = false, error = "設定画面でAPIキーを入力してください") }
                return@launch
            }

            val themes = themeDao.getActive()
            if (themes.isEmpty()) {
                _state.update { it.copy(loading = false, error = "アクティブなテーマがありません") }
                return@launch
            }

            val contextLines = themes.flatMap { theme ->
                val steps = stepDao.getActiveByTheme(theme.id)
                steps.map { step -> """{"stepId":"${step.id}","theme":"${theme.title}","step":"${step.title}","weight":"${step.weight}"}""" }
            }
            if (contextLines.isEmpty()) {
                _state.update { it.copy(loading = false, error = "ステップがありません") }
                return@launch
            }

            val prompt = "以下のステップ一覧から今日着手するのに良いものを2〜3個提案してください：\n${contextLines.joinToString("\n")}"
            claudeClient.send(apiKey, listOf(ClaudeMessage("user", prompt)), ClaudeClient.MORNING_SUGGEST_SYSTEM_PROMPT)
                .onSuccess { text ->
                    val suggestions = parseSuggestions(text, themes.associate { it.id to it.title }, stepDao)
                    _state.update { it.copy(loading = false, suggestions = suggestions) }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.message) }
                }
        }
    }

    fun accept(stepId: String) {
        viewModelScope.launch {
            scheduledStepDao.insert(
                ScheduledStepEntity(
                    id = UUID.randomUUID().toString(),
                    stepId = stepId,
                    date = today,
                )
            )
            stepDao.markToday(stepId)
            _state.update { it.copy(acceptedIds = it.acceptedIds + stepId) }
        }
    }

    private suspend fun parseSuggestions(
        text: String,
        themeMap: Map<String, String>,
        stepDao: StepDao,
    ): List<SuggestedStep> {
        val start = text.indexOf('[')
        val end = text.lastIndexOf(']')
        if (start < 0 || end <= start) return emptyList()
        return runCatching {
            val arr = JSONArray(text.substring(start, end + 1))
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.getJSONObject(i)
                val stepId = obj.getString("stepId")
                val reason = obj.optString("reason", "")
                val step = stepDao.getById(stepId) ?: return@mapNotNull null
                SuggestedStep(
                    stepId = stepId,
                    stepTitle = step.title,
                    themeName = themeMap[step.themeId] ?: "",
                    reason = reason,
                )
            }
        }.getOrDefault(emptyList())
    }
}
