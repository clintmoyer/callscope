package com.clintmoyer.callscope.domain

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class CallAnalytics(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    fun summarize(
        calls: List<CallRecord>,
        now: Instant = Instant.now(),
        range: DateRange = DateRange.All,
    ): AnalyticsSummary {
        val filtered = filterByRange(calls, range, now)
            .sortedByDescending { it.startedAt }
        if (filtered.isEmpty()) return AnalyticsSummary.Empty

        val totalDuration = filtered.fold(Duration.ZERO) { total, call -> total + call.duration }
        val contacts = buildContactStats(filtered)
        val daily = usageByDay(filtered)
        val hourly = usageByHour(filtered)

        return AnalyticsSummary(
            calls = filtered,
            contacts = contacts,
            totalCalls = filtered.size,
            totalDuration = totalDuration,
            averageDuration = totalDuration.dividedBy(filtered.size.toLong()),
            breakdown = breakdown(filtered),
            dailyUsage = daily.takeLast(30),
            weeklyUsage = daily.takeLast(7),
            monthlyUsage = usageByMonth(filtered, 12),
            yearlyUsage = usageByMonth(filtered, 60),
            hourlyUsage = hourly,
            longestCalls = filtered.sortedByDescending { it.duration }.take(8),
            busiestDays = daily.sortedWith(compareByDescending<DailyUsage> { it.callCount }.thenByDescending { it.totalDuration }).take(6),
            quietContacts = trendContacts(contacts, now).filter { it.previousCalls >= 2 && it.recentCalls == 0 }.take(5),
            moreActiveContacts = trendContacts(contacts, now).filter { it.recentCalls >= it.previousCalls + 2 }.take(5),
        )
    }

    fun filterByRange(calls: List<CallRecord>, range: DateRange, now: Instant = Instant.now()): List<CallRecord> {
        val start = when (range) {
            DateRange.Week -> now.minus(7, ChronoUnit.DAYS)
            DateRange.Month -> now.minus(30, ChronoUnit.DAYS)
            DateRange.Year -> now.minus(365, ChronoUnit.DAYS)
            DateRange.All -> Instant.MIN
        }
        return calls.filter { it.startedAt >= start }
    }

    fun rankContacts(contacts: List<ContactStats>, rank: ContactRank): List<ContactStats> {
        return when (rank) {
            ContactRank.CallTime -> contacts.sortedWith(compareByDescending<ContactStats> { it.totalDuration }.thenByDescending { it.totalCalls })
            ContactRank.CallCount -> contacts.sortedWith(compareByDescending<ContactStats> { it.totalCalls }.thenByDescending { it.totalDuration })
            ContactRank.MissedCalls -> contacts.sortedWith(compareByDescending<ContactStats> { it.breakdown.missed }.thenByDescending { it.lastCallAt })
            ContactRank.Recency -> contacts.sortedByDescending { it.lastCallAt }
        }
    }

    private fun buildContactStats(calls: List<CallRecord>): List<ContactStats> {
        return calls.groupBy { it.contactId }
            .map { (_, contactCalls) ->
                val sorted = contactCalls.sortedByDescending { it.startedAt }
                val representative = sorted.first()
                val totalDuration = contactCalls.fold(Duration.ZERO) { total, call -> total + call.duration }
                ContactStats(
                    contactId = representative.contactId,
                    displayName = representative.displayName,
                    phoneNumber = representative.phoneNumber,
                    totalCalls = contactCalls.size,
                    totalDuration = totalDuration,
                    averageDuration = if (contactCalls.isEmpty()) Duration.ZERO else totalDuration.dividedBy(contactCalls.size.toLong()),
                    lastCallAt = representative.startedAt,
                    breakdown = breakdown(contactCalls),
                    calls = sorted,
                    bestDay = bestDayLabel(contactCalls),
                    bestHourLabel = bestHourLabel(contactCalls),
                )
            }
            .let { rankContacts(it, ContactRank.CallTime) }
    }

    private fun breakdown(calls: List<CallRecord>): DirectionBreakdown {
        return DirectionBreakdown(
            incoming = calls.count { it.direction == CallDirection.Incoming },
            outgoing = calls.count { it.direction == CallDirection.Outgoing },
            missed = calls.count { it.direction == CallDirection.Missed },
            rejected = calls.count { it.direction == CallDirection.Rejected },
            blocked = calls.count { it.direction == CallDirection.Blocked },
            unknown = calls.count { it.direction == CallDirection.Unknown },
        )
    }

    private fun usageByDay(calls: List<CallRecord>): List<DailyUsage> {
        return calls.groupBy { it.startedAt.atZone(zoneId).toLocalDate() }
            .map { (date, dayCalls) ->
                DailyUsage(
                    date = date,
                    totalDuration = dayCalls.fold(Duration.ZERO) { total, call -> total + call.duration },
                    callCount = dayCalls.size,
                )
            }
            .sortedBy { it.date }
    }

    private fun usageByMonth(calls: List<CallRecord>, limit: Int): List<MonthlyUsage> {
        return calls.groupBy { YearMonth.from(it.startedAt.atZone(zoneId)) }
            .map { (month, monthCalls) ->
                MonthlyUsage(
                    month = month,
                    totalDuration = monthCalls.fold(Duration.ZERO) { total, call -> total + call.duration },
                    callCount = monthCalls.size,
                )
            }
            .sortedBy { it.month }
            .takeLast(limit)
    }

    private fun usageByHour(calls: List<CallRecord>): List<HourUsage> {
        val byHour = calls.groupBy { it.startedAt.atZone(zoneId).hour }
        return (0..23).map { hour ->
            val hourCalls = byHour[hour].orEmpty()
            HourUsage(
                hour = hour,
                callCount = hourCalls.size,
                totalDuration = hourCalls.fold(Duration.ZERO) { total, call -> total + call.duration },
            )
        }
    }

    private fun trendContacts(contacts: List<ContactStats>, now: Instant): List<ContactTrend> {
        val recentStart = now.minus(14, ChronoUnit.DAYS)
        val previousStart = now.minus(42, ChronoUnit.DAYS)
        return contacts.map { stats ->
            ContactTrend(
                stats = stats,
                recentCalls = stats.calls.count { it.startedAt >= recentStart },
                previousCalls = stats.calls.count { it.startedAt >= previousStart && it.startedAt < recentStart },
            )
        }.sortedByDescending { kotlin.math.abs(it.delta) }
    }

    private fun bestDayLabel(calls: List<CallRecord>): String {
        val day = calls.groupingBy { it.startedAt.atZone(zoneId).dayOfWeek }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: DayOfWeek.MONDAY
        return day.name.lowercase().replaceFirstChar { it.titlecase() }
    }

    private fun bestHourLabel(calls: List<CallRecord>): String {
        val hour = calls.groupingBy { it.startedAt.atZone(zoneId).hour }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: 9
        return when (hour) {
            0 -> "12 AM"
            in 1..11 -> "$hour AM"
            12 -> "12 PM"
            else -> "${hour - 12} PM"
        }
    }
}
