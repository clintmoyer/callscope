package com.clintmoyer.callscope.data

import com.clintmoyer.callscope.domain.CallDirection
import com.clintmoyer.callscope.domain.CallRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class SampleCallLogProvider(
    private val now: Instant = Instant.now().truncatedTo(ChronoUnit.HOURS),
) : CallLogProvider {
    override suspend fun loadCalls(): List<CallRecord> = withContext(Dispatchers.Default) {
        buildList {
            addPerson(
                name = "Maya Chen",
                number = "(415) 555-0184",
                minutes = listOf(38, 44, 51, 22, 18, 62, 33, 40),
                directions = listOf(CallDirection.Incoming, CallDirection.Outgoing),
                dayOffsets = listOf(1, 3, 5, 8, 13, 17, 21, 31),
            )
            addPerson(
                name = "Jordan Lee",
                number = "(212) 555-0140",
                minutes = listOf(8, 0, 0, 13, 5, 0, 6),
                directions = listOf(CallDirection.Missed, CallDirection.Incoming, CallDirection.Outgoing),
                dayOffsets = listOf(0, 2, 4, 6, 9, 11, 16),
            )
            addPerson(
                name = "Ari Patel",
                number = "(503) 555-0119",
                minutes = listOf(77, 54, 25, 91, 33),
                directions = listOf(CallDirection.Outgoing, CallDirection.Incoming),
                dayOffsets = listOf(7, 14, 22, 38, 57),
            )
            addPerson(
                name = "Sam Rivera",
                number = "(646) 555-0157",
                minutes = listOf(0, 0, 2, 0, 0),
                directions = listOf(CallDirection.Missed, CallDirection.Rejected),
                dayOffsets = listOf(1, 4, 9, 12, 18),
            )
            addPerson(
                name = "Nina Brooks",
                number = "(303) 555-0199",
                minutes = listOf(11, 14, 16, 19, 12, 21),
                directions = listOf(CallDirection.Incoming),
                dayOffsets = listOf(19, 23, 27, 31, 36, 41),
            )
            addPerson(
                name = "Unknown",
                number = "(800) 555-0101",
                minutes = listOf(0, 0),
                directions = listOf(CallDirection.Blocked, CallDirection.Missed),
                dayOffsets = listOf(3, 25),
            )
        }.sortedByDescending { it.startedAt }
    }

    private fun MutableList<CallRecord>.addPerson(
        name: String,
        number: String,
        minutes: List<Long>,
        directions: List<CallDirection>,
        dayOffsets: List<Long>,
    ) {
        minutes.forEachIndexed { index, minuteCount ->
            val direction = directions[index % directions.size]
            val idName = name.lowercase().replace(" ", "-")
            add(
                CallRecord(
                    id = "$idName-$index",
                    contactId = "$idName-$number",
                    displayName = name,
                    phoneNumber = number,
                    direction = direction,
                    startedAt = now
                        .minus(dayOffsets[index], ChronoUnit.DAYS)
                        .minus(((index * 3) % 10).toLong(), ChronoUnit.HOURS),
                    duration = Duration.ofMinutes(minuteCount),
                ),
            )
        }
    }
}
