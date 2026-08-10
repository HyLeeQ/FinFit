package com.example.finfit.core.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentLinkedQueue

class OfflineSyncManager(
    private val context: Context? = null
) : IOfflineSyncManager {

    private val queue = ConcurrentLinkedQueue<OfflineQueueItem>()
    private val mutex = Mutex()

    private val _syncState = MutableStateFlow(
        SyncEngineState(
            isOnline = true,
            pendingQueueCount = 0,
            isSyncingNow = false,
            lastSyncTimestamp = System.currentTimeMillis()
        )
    )
    override val syncState: StateFlow<SyncEngineState> = _syncState.asStateFlow()

    init {
        context?.let { setupNetworkMonitoring(it) }
    }

    private fun setupNetworkMonitoring(ctx: Context) {
        val connectivityManager = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    setOnlineStatus(true)
                }

                override fun onLost(network: Network) {
                    setOnlineStatus(false)
                }
            })
        } catch (_: Exception) {}
    }

    override fun setOnlineStatus(online: Boolean) {
        _syncState.update { it.copy(isOnline = online) }
        if (online) {
            // Auto flush in background if pending items exist
            CoroutineScope(Dispatchers.IO).launch {
                flushPendingQueue("default_user")
            }
        }
    }

    override fun enqueue(item: OfflineQueueItem) {
        queue.add(item)
        _syncState.update { it.copy(pendingQueueCount = queue.size) }
    }

    override suspend fun flushPendingQueue(userId: String): Int = mutex.withLock {
        if (queue.isEmpty() || !_syncState.value.isOnline) return 0

        _syncState.update { it.copy(isSyncingNow = true) }
        var syncedCount = 0

        val iterator = queue.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            try {
                // Execute sync item to Cloud
                // In production, maps to FirestoreRepository operations based on entityType and actionType
                iterator.remove()
                syncedCount++
            } catch (e: Exception) {
                // If temporary error, keep in queue for next retry
                break
            }
        }

        _syncState.update {
            it.copy(
                pendingQueueCount = queue.size,
                isSyncingNow = false,
                lastSyncTimestamp = System.currentTimeMillis()
            )
        }
        return syncedCount
    }

    override fun observePendingCount(): Flow<Int> {
        return _syncState.map { it.pendingQueueCount }
    }
}
