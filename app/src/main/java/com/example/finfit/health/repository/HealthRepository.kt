package com.example.finfit.health.repository

import android.content.Context
import android.util.Log
import android.provider.Settings
import android.widget.Toast
import com.example.finfit.data.repository.AuthRepository
import com.example.finfit.health.model.StepEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class HealthRepository(private val context: Context) {

    private val stepDao = HealthDatabase.getDatabase(context).stepDao()
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
                // Xoá trắng cữ trên Local để ăn 100% dữ liệu từ mây xuống (Nối tiếp bước chân mượt mà)
                stepDao.deleteAllSteps()
            } else {
                // Đã claim trước đó. Kiểm tra xem có bị chiếm quyền không (tức là người dùng mua máy khác, đăng nhập)
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
                val cloudCalories = doc.getLong("calories")?.toInt() ?: 0
                val cloudActiveMinutes = doc.getLong("activeMinutes")?.toInt() ?: 0

                val localStep = stepDao.getStepsByDate(date)

                // Nếu Local chưa có -> Lưu vào ngay với trạng thái SYNCED
                if (localStep == null) {
                    stepDao.insertSteps(
                        StepEntity(
                            date = date,
                            steps = cloudSteps,
                            calories = cloudCalories,
                            activeMinutes = cloudActiveMinutes,
                            syncStatus = 2, // Đã đồng bộ
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                } else {
                    // Xung đột (Conflict Resolution)
                    // Nếu Cloud lớn hơn Local -> Ghi đè Local
                    if (cloudSteps > localStep.steps) {
                        stepDao.insertSteps(
                            localStep.copy(
                                steps = cloudSteps,
                                calories = maxOf(cloudCalories, localStep.calories), // Tránh đè calo nhỏ hơn
                                activeMinutes = maxOf(cloudActiveMinutes, localStep.activeMinutes),
                                syncStatus = 2, // Ghi đè thành công nên bằng Cloud -> SYNCED
                                lastUpdated = System.currentTimeMillis()
                            )
                        )
                    } else if (cloudSteps < localStep.steps && localStep.syncStatus == 2) {
                        // Nếu Local lớn hơn mà status vẫn là 2 (tức là offline chạy bộ thêm)
                        // Thì đánh dấu UNSYNCED để worker đẩy ngược lên mây
                        stepDao.updateSyncStatus(date, 0)
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
        stepDao.deleteAllSteps()
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
            stepDao.deleteAllSteps()
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
}
