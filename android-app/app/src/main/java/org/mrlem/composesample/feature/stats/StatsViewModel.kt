package org.mrlem.composesample.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mrlem.composesample.data.db.UsageLogEntity
import org.mrlem.composesample.domain.UsageLogRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class StatsState(
    val eventCounts: List<Pair<String, Int>> = emptyList(),
    val dailyBreakdown: List<Pair<String, List<Pair<String, Int>>>> = emptyList(),
    val csvContent: String = "",
    val isLoading: Boolean = true,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val usageLogRepository: UsageLogRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StatsState())
    val state: StateFlow<StatsState> = _state.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
            val logs = usageLogRepository.queryFrom(sevenDaysAgo)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            val eventCounts = logs
                .groupingBy { it.event }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .map { it.key to it.value }

            val dailyBreakdown = logs
                .groupBy { dateFormat.format(Date(it.timestamp)) }
                .entries
                .sortedByDescending { it.key }
                .map { (date, dayLogs) ->
                    val counts = dayLogs
                        .groupingBy { it.event }
                        .eachCount()
                        .entries
                        .sortedByDescending { it.value }
                        .map { it.key to it.value }
                    date to counts
                }

            val csv = buildCsvContent(logs)

            _state.value = StatsState(
                eventCounts = eventCounts,
                dailyBreakdown = dailyBreakdown,
                csvContent = csv,
                isLoading = false,
            )
        }
    }

    private fun buildCsvContent(logs: List<UsageLogEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("timestamp,event,metadata")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        logs.forEach { log ->
            sb.appendLine("${dateFormat.format(Date(log.timestamp))},${log.event},${log.metadata ?: ""}")
        }
        return sb.toString()
    }
}
