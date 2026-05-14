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
    private val sleepLogDao = HealthDatabase.getDatabase(context).sleepLogDao()
    private val waterLogDao = HealthDatabase.getDatabase(context).waterLogDao()
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
                val cloudCarbs = doc.getLong("carbs")?.toInt() ?: 0
                val cloudProtein = doc.getLong("protein")?.toInt() ?: 0
                val cloudFat = doc.getLong("fat")?.toInt() ?: 0
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
                            carbs = cloudCarbs,
                            protein = cloudProtein,
                            fat = cloudFat,
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
                                carbs = maxOf(cloudCarbs, localRecord.carbs),
                                protein = maxOf(cloudProtein, localRecord.protein),
                                fat = maxOf(cloudFat, localRecord.fat),
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

                // Sync embedded sleep and water logs
                val cloudSleepSessions = doc.get("sleepSessions") as? List<Map<String, Any>>
                if (cloudSleepSessions != null) {
                    cloudSleepSessions.forEach { map ->
                        val id = map["id"] as? String ?: return@forEach
                        val bedTime = map["bedTimeTimestamp"] as? Long ?: return@forEach
                        val wakeTime = map["wakeTimeTimestamp"] as? Long ?: return@forEach
                        val quality = (map["sleepQuality"] as? Number)?.toInt() ?: 3
                        val isDeleted = map["isDeleted"] as? Boolean ?: false
                        
                        sleepLogDao.insertSleepSession(
                            com.example.finfit.health.model.SleepSessionEntity(
                                id = id,
                                date = date,
                                bedTimeTimestamp = bedTime,
                                wakeTimeTimestamp = wakeTime,
                                sleepQuality = quality,
                                isDeleted = isDeleted
                            )
                        )
                    }
                }

                val cloudWaterLogs = doc.get("waterLogs") as? List<Map<String, Any>>
                if (cloudWaterLogs != null) {
                    cloudWaterLogs.forEach { map ->
                        val id = map["id"] as? String ?: return@forEach
                        val amount = (map["amountMl"] as? Number)?.toInt() ?: return@forEach
                        val timestamp = map["timestamp"] as? Long ?: return@forEach
                        val isDeleted = map["isDeleted"] as? Boolean ?: false
                        val drinkType = map["drinkType"] as? String ?: "WATER"
                        val caffeineMg = (map["caffeineMg"] as? Number)?.toInt() ?: 0
                        val source = map["source"] as? String ?: "MANUAL"
                        val contextSteps = (map["contextSteps"] as? Number)?.toInt() ?: 0
                        val timezoneOffset = (map["timezoneOffset"] as? Number)?.toInt() ?: 25200
                        val createdAt = map["createdAt"] as? Long ?: timestamp
                        val updatedAt = map["updatedAt"] as? Long ?: System.currentTimeMillis()
                        
                        waterLogDao.insertLog(
                            com.example.finfit.health.model.WaterLogEntity(
                                id = id,
                                date = date,
                                amountMl = amount,
                                timestamp = timestamp,
                                drinkType = drinkType,
                                caffeineMg = caffeineMg,
                                source = source,
                                contextSteps = contextSteps,
                                timezoneOffset = timezoneOffset,
                                createdAt = createdAt,
                                syncStatus = 2, // SYNCED
                                isDeleted = isDeleted,
                                updatedAt = updatedAt
                            )
                        )
                    }
                }
            }
            Log.d("HealthRepo", "Cloud sync complete.")
        } catch (e: Exception) {
            Log.e("HealthRepo", "Cloud sync failed: ${e.message}", e)
        }
    }

    /**
     * Đồng bộ từ Local lên Cloud (Push to Firebase).
     * Được gọi bởi HealthSyncWorker hoặc Force Sync thủ công.
     */
    suspend fun pushLocalToCloud() {
        val user = AuthRepository().getCurrentUser() ?: return
        val uid = user.uid

        // Kiểm tra Primary Device Auth trước khi Sync
        val userDoc = firestore.collection("users").document(uid).get().await()
        val cloudDeviceId = userDoc.getString("primaryDeviceId")
        if (cloudDeviceId != null && cloudDeviceId != currentDeviceId) {
            Log.e("HealthRepo", "Device ID mismatch! Kicking out current device.")
            forceLogoutAndWipeLocalData()
            return
        }

        // Lấy dữ liệu UNSYNCED
        val unsyncedRecords = healthDao.getUnsyncedRecords()
        if (unsyncedRecords.isEmpty()) return

        // Đánh dấu SYNCING
        unsyncedRecords.forEach {
            healthDao.updateSyncStatus(it.date, 1)
        }

        try {
            val batch = firestore.batch()
            unsyncedRecords.forEach { entity ->
                val docRef = firestore.collection("users").document(uid)
                    .collection("health_history").document(entity.date)

                // Fetch detailed logs
                val sleepSessions = sleepLogDao.getSleepSessionsForDate(entity.date)
                val waterLogs = waterLogDao.getLogsByDate(entity.date)

                val sleepSessionsArray = sleepSessions.map {
                    mapOf(
                        "id" to it.id,
                        "bedTimeTimestamp" to it.bedTimeTimestamp,
                        "wakeTimeTimestamp" to it.wakeTimeTimestamp,
                        "sleepQuality" to it.sleepQuality,
                        "isDeleted" to it.isDeleted
                    )
                }

                val waterLogsArray = waterLogs.map {
                    mapOf(
                        "id" to it.id,
                        "amountMl" to it.amountMl,
                        "timestamp" to it.timestamp,
                        "isDeleted" to it.isDeleted,
                        "drinkType" to it.drinkType,
                        "caffeineMg" to it.caffeineMg,
                        "source" to it.source,
                        "contextSteps" to it.contextSteps,
                        "timezoneOffset" to it.timezoneOffset,
                        "createdAt" to it.createdAt,
                        "updatedAt" to it.updatedAt
                    )
                }

                val data = hashMapOf(
                    "steps" to entity.steps,
                    "stepGoal" to entity.stepGoal,
                    "caloriesOut" to entity.caloriesOut,
                    "caloriesIn" to entity.caloriesIn,
                    "carbs" to entity.carbs,
                    "protein" to entity.protein,
                    "fat" to entity.fat,
                    "activeMinutes" to entity.activeMinutes,
                    "waterConsumed" to entity.waterConsumed,
                    "waterGoal" to entity.waterGoal,
                    "sleepHours" to entity.sleepHours,
                    "sleepSessions" to sleepSessionsArray,
                    "waterLogs" to waterLogsArray,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                batch.set(docRef, data, SetOptions.merge())
            }

            // Thực thi commit
            batch.commit().await()

            // Cập nhật trạng thái SYNCED
            unsyncedRecords.forEach {
                healthDao.updateSyncStatus(it.date, 2)
            }
            Log.d("HealthRepo", "Local push to Cloud complete.")
        } catch (e: Exception) {
            // Lỗi mạng -> Đặt lại UNSYNCED
            unsyncedRecords.forEach {
                healthDao.updateSyncStatus(it.date, 0)
            }
            throw e
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

    // ====================================================================
    // WATER REMINDER & SLEEP PREFERENCES (PHASE 1)
    // ====================================================================

    fun isWaterReminderEnabled(): Boolean {
        return prefs.getBoolean("water_reminder_enabled", false)
    }

    fun setWaterReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("water_reminder_enabled", enabled).apply()
    }

    /** Trả về phút trong ngày của giờ đi ngủ (Mặc định 22:00 = 1320) */
    fun getBedTimeMinute(): Int {
        return prefs.getInt("bed_time_minute", 22 * 60)
    }

    /** Trả về phút trong ngày của giờ thức dậy (Mặc định 08:00 = 480) */
    fun getWakeTimeMinute(): Int {
        return prefs.getInt("wake_time_minute", 8 * 60)
    }
}
