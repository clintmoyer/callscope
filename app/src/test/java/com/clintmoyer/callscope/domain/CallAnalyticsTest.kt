package com.clintmoyer.callscope.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class CallAnalyticsTest {
    private val now = Instant.parse("2026-06-19T12:00:00Z")
    private val analytics = CallAnalytics(ZoneId.of("UTC"))

    @Test
    fun summarizesCallCountsAndDuration() {
        val summary = analytics.summarize(
            listOf(
                call("1", "alice", CallDirection.Incoming, 1, 10),
                call("2", "alice", CallDirection.Outgoing, 2, 20),
                call("3", "bob", CallDirection.Missed, 3, 0),
            ),
            now,
        )

        assertEquals(3, summary.totalCalls)
        assertEquals(Duration.ofMinutes(30), summary.totalDuration)
        assertEquals(Duration.ofMinutes(10), summary.averageDuration)
        assertEquals(1, summary.breakdown.incoming)
        assertEquals(1, summary.breakdown.outgoing)
        assertEquals(1, summary.breakdown.missed)
    }

    @Test
    fun ranksContactsByCallTimeCountMissesAndRecency() {
        val summary = analytics.summarize(
            listOf(
                call("1", "alice", CallDirection.Incoming, 5, 50),
                call("2", "bob", CallDirection.Outgoing, 1, 5),
                call("3", "bob", CallDirection.Missed, 2, 0),
                call("4", "bob", CallDirection.Missed, 3, 0),
                call("5", "cara", CallDirection.Outgoing, 0, 1),
            ),
            now,
        )

        assertEquals("Alice", analytics.rankContacts(summary.contacts, ContactRank.CallTime).first().displayName)
        assertEquals("Bob", analytics.rankContacts(summary.contacts, ContactRank.CallCount).first().displayName)
        assertEquals("Bob", analytics.rankContacts(summary.contacts, ContactRank.MissedCalls).first().displayName)
        assertEquals("Cara", analytics.rankContacts(summary.contacts, ContactRank.Recency).first().displayName)
    }

    @Test
    fun filtersCallsByDateRange() {
        val calls = listOf(
            call("1", "alice", CallDirection.Incoming, 2, 5),
            call("2", "alice", CallDirection.Incoming, 20, 5),
            call("3", "alice", CallDirection.Incoming, 400, 5),
        )

        assertEquals(1, analytics.filterByRange(calls, DateRange.Week, now).size)
        assertEquals(2, analytics.filterByRange(calls, DateRange.Month, now).size)
        assertEquals(2, analytics.filterByRange(calls, DateRange.Year, now).size)
        assertEquals(3, analytics.filterByRange(calls, DateRange.All, now).size)
    }

    @Test
    fun detectsQuietAndMoreActiveContacts() {
        val summary = analytics.summarize(
            listOf(
                call("1", "quiet", CallDirection.Incoming, 20, 3),
                call("2", "quiet", CallDirection.Outgoing, 24, 4),
                call("3", "active", CallDirection.Incoming, 2, 8),
                call("4", "active", CallDirection.Outgoing, 3, 9),
                call("5", "active", CallDirection.Incoming, 4, 7),
            ),
            now,
        )

        assertTrue(summary.quietContacts.any { it.stats.displayName == "Quiet" })
        assertTrue(summary.moreActiveContacts.any { it.stats.displayName == "Active" })
    }

    private fun call(
        id: String,
        person: String,
        direction: CallDirection,
        daysAgo: Long,
        minutes: Long,
    ): CallRecord {
        return CallRecord(
            id = id,
            contactId = person,
            displayName = person.replaceFirstChar { it.titlecase() },
            phoneNumber = "555-$id",
            direction = direction,
            startedAt = now.minus(Duration.ofDays(daysAgo)),
            duration = Duration.ofMinutes(minutes),
        )
    }
}
