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
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class HealthSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val user = AuthRepository().getCurrentUser() ?: return Result.success()
        val uid = user.uid

        val repository = HealthRepository(applicationContext)

        return try {
            // Ép xả bộ nhớ đệm bước chân xuống Room trước khi thực sự đồng bộ
            StepCounterManager.getInstance(applicationContext).flushToDatabase()

            // Thực thi đẩy dữ liệu lên Firebase (hàm này đã bao gồm check Device ID)
            repository.pushLocalToCloud()

            Log.d("HealthSyncWorker", "Sync successful.")
            Result.success()

        } catch (e: Exception) {
            Log.e("HealthSyncWorker", "Sync failed: ${e.message}", e)
            Result.retry()
        }
    }
}
