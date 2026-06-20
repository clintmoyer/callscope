package com.clintmoyer.callscope.data

import com.clintmoyer.callscope.domain.CallRecord

interface CallLogProvider {
    suspend fun loadCalls(): List<CallRecord>
}
