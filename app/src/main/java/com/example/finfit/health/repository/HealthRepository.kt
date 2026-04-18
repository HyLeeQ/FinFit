package com.example.finfit.health.repository

import android.content.Context
import android.util.Log
import android.provider.Settings
import android.widget.Toast
import com.example.finfit.data.repository.AuthRepository
import com.example.finfit.health.model.HealthEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.finfit.health.data.HealthSyncWorker

class HealthRepository(private val context: Context) {

    private val healthDao = HealthDatabase.getDatabase(context).healthDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val currentDeviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    private val prefs = context.getSharedPreferences("HealthPrefs", Context.MODE_PRIVATE)

    /**
     * Đồng bộ từ Cloud về Local (Conflict Resolution).
     * Rule: Giữ giá trị steps lớn hơn.
     */
    suspend fun syncCloudToLocal() {
        val user = AuthRepository().getCurrentUser() ?: return
        val uid = user.uid

        val hasClaimedLocal = prefs.getBoolean("hasClaimedPrimaryDevice_$uid", false)

        try {
            val userDocRef = firestore.collection("users").document(uid)
            val userDoc = userDocRef.get().await()
            val cloudDeviceId = userDoc.getString("primaryDeviceId")

            if (!hasClaimedLocal) {
                // Đăng nhập máy mới -> Trở thành Primary Device
                userDocRef.set(hashMapOf("primaryDeviceId" to currentDeviceId), SetOptions.merge()).await()
                prefs.edit().putBoolean("hasClaimedPrimaryDevice_$uid", true).apply()
                // Xoá trắng Local để ăn 100% dữ liệu từ mây xuống
                healthDao.deleteAll()
            } else {
                // Đã claim trước đó. Kiểm tra xem có bị chiếm quyền không
                if (cloudDeviceId != null && cloudDeviceId != currentDeviceId) {
                    forceLogoutAndWipeLocalData()
                    return
                }
            }

            // Bắt đầu đối chiếu dữ liệu
            val snapshot = userDocRef.collection("health_history").get().await()

            if (snapshot.isEmpty) return

            for (doc in snapshot.documents) {
                val date = doc.id
                val cloudSteps = doc.getLong("steps")?.toInt() ?: 0
                val cloudCaloriesOut = doc.getLong("caloriesOut")?.toInt()
                    ?: doc.getLong("calories")?.toInt() ?: 0  // Backward compat
                val cloudCaloriesIn = doc.getLong("caloriesIn")?.toInt() ?: 0
                val cloudActiveMinutes = doc.getLong("activeMinutes")?.toInt() ?: 0
                val cloudWaterConsumed = doc.getLong("waterConsumed")?.toInt() ?: 0
                val cloudWaterGoal = doc.getLong("waterGoal")?.toInt() ?: 0
                val cloudSleepHours = doc.getDouble("sleepHours")?.toFloat() ?: 0f
                val cloudStepGoal = doc.getLong("stepGoal")?.toInt() ?: 1000

                val localRecord = healthDao.getHealthByDate(date)

                // Nếu Local chưa có -> Lưu vào ngay với trạng thái SYNCED
                if (localRecord == null) {
                    healthDao.insertHealth(
                        HealthEntity(
                            date = date,
                            steps = cloudSteps,
                            stepGoal = cloudStepGoal,
                            caloriesOut = cloudCaloriesOut,
                            caloriesIn = cloudCaloriesIn,
                            activeMinutes = cloudActiveMinutes,
                            waterConsumed = cloudWaterConsumed,
                            waterGoal = cloudWaterGoal,
                            sleepHours = cloudSleepHours,
                            syncStatus = 2, // Đã đồng bộ
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                } else {
                    // Xung đột (Conflict Resolution)
                    if (cloudSteps > localRecord.steps) {
                        healthDao.insertHealth(
                            localRecord.copy(
                                steps = cloudSteps,
                                caloriesOut = maxOf(cloudCaloriesOut, localRecord.caloriesOut),
                                caloriesIn = maxOf(cloudCaloriesIn, localRecord.caloriesIn),
                                activeMinutes = maxOf(cloudActiveMinutes, localRecord.activeMinutes),
                                waterConsumed = maxOf(cloudWaterConsumed, localRecord.waterConsumed),
                                waterGoal = maxOf(cloudWaterGoal, localRecord.waterGoal),
                                sleepHours = maxOf(cloudSleepHours, localRecord.sleepHours),
                                syncStatus = 2,
                                lastUpdated = System.currentTimeMillis()
                            )
                        )
                    } else if (cloudSteps < localRecord.steps && localRecord.syncStatus == 2) {
                        healthDao.updateSyncStatus(date, 0)
                    }
                }
            }
            Log.d("HealthRepo", "Cloud sync complete.")
        } catch (e: Exception) {
            Log.e("HealthRepo", "Cloud sync failed: ${e.message}", e)
        }
    }

    suspend fun forceLogoutAndWipeLocalData() {
        Log.d("HealthRepo", "forceLogoutAndWipeLocalData triggered: Kicked out by primary device.")
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Tài khoản đã đăng nhập ở thiết bị khác. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show()
        }
        // Xoá Local DB
        healthDao.deleteAll()
        // Xoá các Preferences
        context.getSharedPreferences("StepTrackerPrefs", Context.MODE_PRIVATE).edit().clear().apply()
        prefs.edit().clear().apply()
        // Đăng xuất văng về login
        AuthRepository().signOut()
    }

    /**
     * Nút bấm xoá thủ công
     */
    suspend fun wipeAllHealthData() {
        val user = AuthRepository().getCurrentUser() ?: return
        val uid = user.uid

        try {
            // 1. Xoá DB Cục bộ + Xoá biến đếm vòng quay (SharedPrefs)
            healthDao.deleteAll()
            context.getSharedPreferences("StepTrackerPrefs", Context.MODE_PRIVATE).edit().clear().apply()
            prefs.edit().clear().apply()

            // 2. Xoá subcollection trên Firestore
            val userDocRef = firestore.collection("users").document(uid)
            val snapshot = userDocRef.collection("health_history").get().await()
            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()

            // 3. Xoá cờ primaryDeviceId
            userDocRef.update(
                mapOf("primaryDeviceId" to com.google.firebase.firestore.FieldValue.delete())
            ).await()

            // 4. Bắn văng về trang Login và hiển thị thông báo an toàn
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Đã xoá toàn bộ lịch sử Sức Khỏe", Toast.LENGTH_SHORT).show()
                AuthRepository().signOut()
            }
        } catch (e: Exception) {
            Log.e("HealthRepo", "wipeAllHealthData failed: ${e.message}", e)
        }
    }

    /**
     * Cập nhật lượng nước tiêu thụ và tự động tính toán mục tiêu
     */
    suspend fun updateWaterConsumption(amount: Int) {
        val user = AuthRepository().getCurrentUser() ?: return
        val uid = user.uid
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // 1. Calculate Goal based on weight
        var baseGoal = 2000
        try {
            val userDoc = firestore.collection("users").document(uid).get().await()
            val weight = userDoc.getDouble("weight")
            if (weight != null && weight > 0) {
                val localRecord = healthDao.getHealthByDate(date)
                val steps = localRecord?.steps ?: 0
                baseGoal = (weight * 35).toInt() + (steps / 1000 * 100)
            }
        } catch (e: Exception) {
            Log.e("HealthRepo", "Failed to fetch weight from firestore, using default 2000", e)
        }

        // 2. Trực tiếp cập nhật Nước vào DB
        val localRecord = healthDao.getHealthByDate(date)
        if (localRecord == null) {
            // Chưa có record ngày hôm nay thì tạo mới
            healthDao.insertHealth(
                HealthEntity(
                    date = date,
                    waterConsumed = amount,
                    waterGoal = baseGoal,
                    syncStatus = 0,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        } else {
            // Gọi updateWaterConsumption Partial Update
            healthDao.updateWaterConsumption(date, amount, baseGoal)
        }

        // 3. Kích hoạt Worker đẩy lên Cloud
        triggerOneTimeSync()
    }

    /**
     * Cập nhật lượng calo nạp vào (cho module thực phẩm sau này)
     */
    suspend fun updateCaloriesIn(amount: Int) {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val localRecord = healthDao.getHealthByDate(date)
        if (localRecord == null) {
            healthDao.insertHealth(
                HealthEntity(
                    date = date,
                    caloriesIn = amount,
                    syncStatus = 0,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        } else {
            healthDao.updateCaloriesIn(date, amount)
        }
        triggerOneTimeSync()
    }

    /**
     * Kích hoạt Worker OneTime đẩy lên Cloud
     */
    private fun triggerOneTimeSync() {
        val syncRequest = OneTimeWorkRequestBuilder<HealthSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "HealthOneTimeSync",
            androidx.work.ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    /**
     * Reset chỉ bước chân trong ngày, giữ nguyên nước/caloriesIn/sleep.
     * Đồng thời reset lại sensor state trong StepCounterManager.
     */
    suspend fun resetTodaySteps() {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        healthDao.resetStepData(date)
        
        // Reset ngầm định Firestore (Set steps = 0)
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            try {
                val db = FirebaseFirestore.getInstance()
                db.collection("users").document(user.uid)
                    .collection("health_history").document(date)
                    .update(
                        mapOf(
                            "steps" to 0,
                            "caloriesOut" to 0,
                            "activeMinutes" to 0,
                            "syncStatus" to 0,
                            "lastUpdated" to System.currentTimeMillis()
                        )
                    ).await()
            } catch (e: Exception) {
                // Ignore errors if document doesn't exist yet
            }
        }
        
        triggerOneTimeSync()
    }

    /**
     * Kiểm tra xem user đã đạt thành tựu 1000 bước bao giờ chưa (Global flag).
     */
    suspend fun checkFirst1000StepsAchieved(): Boolean {
        val user = AuthRepository().getCurrentUser() ?: return false
        val uid = user.uid
        val localKey = "first_1000_steps_achieved_$uid"

        if (prefs.getBoolean(localKey, false)) return true

        return try {
            val userDoc = firestore.collection("users").document(uid).get().await()
            val achieved = userDoc.getBoolean("isFirst1000StepsAchieved") ?: false
            if (achieved) {
                prefs.edit().putBoolean(localKey, true).apply()
            }
            achieved
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Ghi nhận thành tựu 1000 bước.
     */
    suspend fun markFirst1000StepsAchieved() {
        val user = AuthRepository().getCurrentUser() ?: return
        val uid = user.uid
        val localKey = "first_1000_steps_achieved_$uid"

        prefs.edit().putBoolean(localKey, true).apply()
        try {
            firestore.collection("users").document(uid)
                .set(mapOf("isFirst1000StepsAchieved" to true), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e("HealthRepo", "Failed to mark achievement on cloud", e)
        }
    }

    /**
     * Kiểm tra xem user đã xem pháo hoa chúc mừng chưa
     */
    suspend fun checkFirst1000StepsCelebrated(): Boolean {
        val user = AuthRepository().getCurrentUser() ?: return false
        val uid = user.uid
        val localKey = "first_1000_steps_celebrated_$uid"
        return prefs.getBoolean(localKey, false)
    }

    /**
     * Ghi nhận đã xem pháo hoa
     */
    suspend fun markFirst1000StepsCelebrated() {
        val user = AuthRepository().getCurrentUser() ?: return
        val uid = user.uid
        val localKey = "first_1000_steps_celebrated_$uid"
        prefs.edit().putBoolean(localKey, true).apply()
    }
}
