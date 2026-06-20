package com.clintmoyer.callscope.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

enum class CallDirection {
    Incoming,
    Outgoing,
    Missed,
    Rejected,
    Blocked,
    Unknown,
}

enum class ContactRank {
    CallTime,
    CallCount,
    MissedCalls,
    Recency,
}

enum class DateRange {
    Week,
    Month,
    Year,
    All,
}

data class CallRecord(
    val id: String,
    val contactId: String,
    val displayName: String,
    val phoneNumber: String,
    val direction: CallDirection,
    val startedAt: Instant,
    val duration: Duration,
) {
    val isConversation: Boolean
        get() = direction == CallDirection.Incoming || direction == CallDirection.Outgoing
}

data class DirectionBreakdown(
    val incoming: Int = 0,
    val outgoing: Int = 0,
    val missed: Int = 0,
    val rejected: Int = 0,
    val blocked: Int = 0,
    val unknown: Int = 0,
) {
    val total: Int = incoming + outgoing + missed + rejected + blocked + unknown
}

data class ContactStats(
    val contactId: String,
    val displayName: String,
    val phoneNumber: String,
    val totalCalls: Int,
    val totalDuration: Duration,
    val averageDuration: Duration,
    val lastCallAt: Instant,
    val breakdown: DirectionBreakdown,
    val calls: List<CallRecord>,
    val bestDay: String,
    val bestHourLabel: String,
)

data class DailyUsage(
    val date: LocalDate,
    val totalDuration: Duration,
    val callCount: Int,
)

data class MonthlyUsage(
    val month: YearMonth,
    val totalDuration: Duration,
    val callCount: Int,
)

data class HourUsage(
    val hour: Int,
    val callCount: Int,
    val totalDuration: Duration,
)

data class ContactTrend(
    val stats: ContactStats,
    val recentCalls: Int,
    val previousCalls: Int,
) {
    val delta: Int = recentCalls - previousCalls
}

data class AnalyticsSummary(
    val calls: List<CallRecord>,
    val contacts: List<ContactStats>,
    val totalCalls: Int,
    val totalDuration: Duration,
    val averageDuration: Duration,
    val breakdown: DirectionBreakdown,
    val dailyUsage: List<DailyUsage>,
    val weeklyUsage: List<DailyUsage>,
    val monthlyUsage: List<MonthlyUsage>,
    val yearlyUsage: List<MonthlyUsage>,
    val hourlyUsage: List<HourUsage>,
    val longestCalls: List<CallRecord>,
    val busiestDays: List<DailyUsage>,
    val quietContacts: List<ContactTrend>,
    val moreActiveContacts: List<ContactTrend>,
) {
    companion object {
        val Empty = AnalyticsSummary(
            calls = emptyList(),
            contacts = emptyList(),
            totalCalls = 0,
            totalDuration = Duration.ZERO,
            averageDuration = Duration.ZERO,
            breakdown = DirectionBreakdown(),
            dailyUsage = emptyList(),
            weeklyUsage = emptyList(),
            monthlyUsage = emptyList(),
            yearlyUsage = emptyList(),
            hourlyUsage = emptyList(),
            longestCalls = emptyList(),
            busiestDays = emptyList(),
            quietContacts = emptyList(),
            moreActiveContacts = emptyList(),
        )
    }
}
