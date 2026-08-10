package com.example.finfit.core.sync

data class OfflineQueueItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val entityType: SyncEntityType,
    val entityId: String,
    val actionType: SyncActionType,
    val payloadJson: String = "",
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val lastError: String? = null
)

data class SyncEngineState(
    val isOnline: Boolean = true,
    val pendingQueueCount: Int = 0,
    val isSyncingNow: Boolean = false,
    val lastSyncTimestamp: Long = System.currentTimeMillis()
)
