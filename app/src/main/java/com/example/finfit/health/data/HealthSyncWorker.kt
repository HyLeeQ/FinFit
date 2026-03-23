package com.example.finfit.health.data

import android.content.Context
import android.util.Log
import android.provider.Settings
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.finfit.data.repository.AuthRepository
import com.example.finfit.health.repository.HealthDatabase
import com.example.finfit.health.repository.HealthRepository
import com.example.finfit.health.repository.StepCounterManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

class HealthSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val user = AuthRepository().getCurrentUser() ?: return Result.success()
        val uid = user.uid

        val stepDao = HealthDatabase.getDatabase(applicationContext).stepDao()
        val firestore = FirebaseFirestore.getInstance()
        val currentDeviceId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)

        return try {
            // Ép xả bộ nhớ đệm bước chân xuống Room trước khi thực sự đồng bộ
            StepCounterManager.getInstance(applicationContext).flushToDatabase()

            // Kiểm tra Primary Device Auth trước khi Sync
            val userDoc = firestore.collection("users").document(uid).get().await()
            val cloudDeviceId = userDoc.getString("primaryDeviceId")
            if (cloudDeviceId != null && cloudDeviceId != currentDeviceId) {
                Log.e("HealthSyncWorker", "Device ID mismatch! Kicking out current device.")
                HealthRepository(applicationContext).forceLogoutAndWipeLocalData()
                return Result.failure()
            }

            // 1. Lấy dữ liệu chưa được đồng bộ (UNSYNCED)
            val unsyncedSteps = stepDao.getUnsyncedSteps()
            if (unsyncedSteps.isEmpty()) {
                Log.d("HealthSyncWorker", "No unsynced data found.")
                return Result.success()
            }

            Log.d("HealthSyncWorker", "Syncing ${unsyncedSteps.size} records to Firestore...")

            // 2. Đánh dấu trạng thái đang đồng bộ (SYNCING = 1) để tránh overlap
            unsyncedSteps.forEach {
                stepDao.updateSyncStatus(it.date, 1) // SYNCING
            }

            // 3. Chuẩn bị Firebase Batch Write
            val batch = firestore.batch()
            
            unsyncedSteps.forEach { stepEntity ->
                val docRef = firestore.collection("users").document(uid)
                    .collection("health_history").document(stepEntity.date)

                val data = hashMapOf(
                    "steps" to stepEntity.steps,
                    "calories" to stepEntity.calories,
                    "activeMinutes" to stepEntity.activeMinutes,
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                batch.set(docRef, data, com.google.firebase.firestore.SetOptions.merge())
            }

            // 4. Thực thi commit (nếu mất mạng ở đây sẽ throw exception)
            batch.commit().await()

            // 5. Đẩy thành công -> Cập nhật trạng thái thành SYNCED = 2
            unsyncedSteps.forEach {
                stepDao.updateSyncStatus(it.date, 2) // SYNCED
            }

            Log.d("HealthSyncWorker", "Sync successful.")
            Result.success()

        } catch (e: Exception) {
            Log.e("HealthSyncWorker", "Sync failed: ${e.message}", e)
            // Lỗi mạng hoặc Firebase -> Đặt lại thành UNSYNCED = 0 và return retry()
            try {
                val stepDaoFallback = HealthDatabase.getDatabase(applicationContext).stepDao()
                stepDaoFallback.getUnsyncedSteps().forEach {
                    if (it.syncStatus == 1) stepDaoFallback.updateSyncStatus(it.date, 0)
                }
            } catch (fallbackE: Exception) {
                // Ignore fallback error
            }

            Result.retry()
        }
    }
}
