package com.clintmoyer.callscope.data

import android.content.Context
import android.provider.CallLog
import com.clintmoyer.callscope.domain.CallDirection
import com.clintmoyer.callscope.domain.CallRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

class AndroidCallLogProvider(
    private val context: Context,
) : CallLogProvider {
    override suspend fun loadCalls(): List<CallRecord> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.CACHED_LOOKUP_URI,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
        )

        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
            val numberIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val nameIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
            val lookupIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_LOOKUP_URI)
            val typeIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val dateIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val durationIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)

            buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIndex)
                    val number = cursor.getString(numberIndex).orEmpty()
                    val name = cursor.getString(nameIndex)
                    val lookup = cursor.getString(lookupIndex)
                    val display = name?.takeIf { it.isNotBlank() } ?: number.ifBlank { "Unknown caller" }
                    add(
                        CallRecord(
                            id = id,
                            contactId = lookup ?: number.ifBlank { id },
                            displayName = display,
                            phoneNumber = number,
                            direction = cursor.getInt(typeIndex).toDirection(),
                            startedAt = Instant.ofEpochMilli(cursor.getLong(dateIndex)),
                            duration = Duration.ofSeconds(cursor.getLong(durationIndex).coerceAtLeast(0L)),
                        ),
                    )
                }
            }
        } ?: emptyList()
    }

    private fun Int.toDirection(): CallDirection = when (this) {
        CallLog.Calls.INCOMING_TYPE -> CallDirection.Incoming
        CallLog.Calls.OUTGOING_TYPE -> CallDirection.Outgoing
        CallLog.Calls.MISSED_TYPE -> CallDirection.Missed
        CallLog.Calls.REJECTED_TYPE -> CallDirection.Rejected
        CallLog.Calls.BLOCKED_TYPE -> CallDirection.Blocked
        else -> CallDirection.Unknown
    }
}
