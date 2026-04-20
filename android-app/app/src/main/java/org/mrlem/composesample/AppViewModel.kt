package org.mrlem.composesample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.mrlem.composesample.domain.UsageLogRepository
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val usageLogRepository: UsageLogRepository,
) : ViewModel() {

    fun onTabOpened(tab: Int) {
        val event = when (tab) {
            0 -> "tab_opened:today"
            1 -> "tab_opened:capture"
            2 -> "tab_opened:projects"
            else -> return
        }
        viewModelScope.launch { usageLogRepository.record(event) }
    }
}
