package com.clintmoyer.callscope.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.clintmoyer.callscope.data.CallAnalyticsRepository
import com.clintmoyer.callscope.domain.AnalyticsSummary
import com.clintmoyer.callscope.domain.CallAnalytics
import com.clintmoyer.callscope.domain.ContactRank
import com.clintmoyer.callscope.domain.DateRange
import com.clintmoyer.callscope.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab {
    Calls,
    People,
    Insights,
    Settings,
}

data class CallScopeUiState(
    val summary: AnalyticsSummary = AnalyticsSummary.Empty,
    val selectedTab: AppTab = AppTab.Calls,
    val selectedContactId: String? = null,
    val contactRank: ContactRank = ContactRank.CallTime,
    val dateRange: DateRange = DateRange.Month,
    val themeMode: ThemeMode = ThemeMode.System,
    val canReadCallLog: Boolean = false,
    val usingSampleData: Boolean = true,
    val productionCallCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
)

class CallScopeViewModel(
    private val repository: CallAnalyticsRepository,
    private val analytics: CallAnalytics = CallAnalytics(),
) : ViewModel() {
    private val _state = MutableStateFlow(CallScopeUiState())
    val state: StateFlow<CallScopeUiState> = _state.asStateFlow()

    fun refresh(canReadCallLog: Boolean) {
        _state.update { it.copy(isLoading = true, canReadCallLog = canReadCallLog, error = null) }
        viewModelScope.launch {
            runCatching {
                repository.loadSummary(canReadCallLog, _state.value.dateRange)
            }.onSuccess { snapshot ->
                _state.update {
                    it.copy(
                        summary = snapshot.summary,
                        usingSampleData = snapshot.usingSampleData,
                        productionCallCount = snapshot.productionCallCount,
                        isLoading = false,
                    )
                }
            }.onFailure { throwable ->
                _state.update {
                    it.copy(
                        summary = AnalyticsSummary.Empty,
                        usingSampleData = true,
                        isLoading = false,
                        error = throwable.message ?: "Unable to read call analytics",
                    )
                }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _state.update { it.copy(selectedTab = tab, selectedContactId = null) }
    }

    fun selectContact(contactId: String) {
        _state.update { it.copy(selectedContactId = contactId) }
    }

    fun clearContact() {
        _state.update { it.copy(selectedContactId = null) }
    }

    fun setRank(rank: ContactRank) {
        _state.update { it.copy(contactRank = rank) }
    }

    fun setTheme(mode: ThemeMode) {
        _state.update { it.copy(themeMode = mode) }
    }

    fun setDateRange(range: DateRange, canReadCallLog: Boolean) {
        _state.update { it.copy(dateRange = range) }
        refresh(canReadCallLog)
    }

    fun rankedContacts() = analytics.rankContacts(_state.value.summary.contacts, _state.value.contactRank)
}

class CallScopeViewModelFactory(
    private val repository: CallAnalyticsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CallScopeViewModel::class.java)) {
            return CallScopeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class ${modelClass.name}")
    }
}
