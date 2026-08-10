package com.example.finfit.core.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface IOfflineSyncManager {
    val syncState: StateFlow<SyncEngineState>
    fun enqueue(item: OfflineQueueItem)
    suspend fun flushPendingQueue(userId: String): Int
    fun observePendingCount(): Flow<Int>
    fun setOnlineStatus(online: Boolean)
}
