package com.clintmoyer.callscope.data

import com.clintmoyer.callscope.domain.AnalyticsSummary
import com.clintmoyer.callscope.domain.CallAnalytics
import com.clintmoyer.callscope.domain.DateRange
import java.time.Instant

class CallAnalyticsRepository(
    private val productionProvider: CallLogProvider,
    private val sampleProvider: CallLogProvider,
    private val analytics: CallAnalytics = CallAnalytics(),
) {
    suspend fun loadSummary(
        canReadCallLog: Boolean,
        range: DateRange,
        now: Instant = Instant.now(),
    ): RepositorySnapshot {
        val productionCalls = if (canReadCallLog) productionProvider.loadCalls() else emptyList()
        val usingSampleData = !canReadCallLog || productionCalls.isEmpty()
        val calls = if (usingSampleData) sampleProvider.loadCalls() else productionCalls
        return RepositorySnapshot(
            summary = analytics.summarize(calls, now, range),
            usingSampleData = usingSampleData,
            productionCallCount = productionCalls.size,
        )
    }
}

data class RepositorySnapshot(
    val summary: AnalyticsSummary,
    val usingSampleData: Boolean,
    val productionCallCount: Int,
)
