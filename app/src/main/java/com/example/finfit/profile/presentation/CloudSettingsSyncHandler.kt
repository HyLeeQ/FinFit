package com.example.finfit.profile.presentation

import com.example.finfit.data.local.SetupPreferences
import com.example.finfit.data.local.ThemePreferences
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

object CloudSettingsSyncHandler {

    data class CloudUserPreferences(
        val currency: String = "VND",
        val isDarkMode: Boolean = true,
        val isPredictiveBudgetAlertEnabled: Boolean = true,
        val isEveningDigestEnabled: Boolean = true,
        val lastUpdatedTimestamp: Long = System.currentTimeMillis()
    )

    suspend fun backupSettingsToCloud(userId: String, prefs: CloudUserPreferences) {
        try {
            Firebase.firestore.collection("users").document(userId)
                .collection("settings").document("preferences")
                .set(prefs).await()
        } catch (_: Exception) {}
    }

    suspend fun restoreSettingsFromCloud(userId: String): CloudUserPreferences? {
        return try {
            val doc = Firebase.firestore.collection("users").document(userId)
                .collection("settings").document("preferences")
                .get().await()

            if (doc.exists()) {
                CloudUserPreferences(
                    currency = doc.getString("currency") ?: "VND",
                    isDarkMode = doc.getBoolean("isDarkMode") ?: true,
                    isPredictiveBudgetAlertEnabled = doc.getBoolean("isPredictiveBudgetAlertEnabled") ?: true,
                    isEveningDigestEnabled = doc.getBoolean("isEveningDigestEnabled") ?: true,
                    lastUpdatedTimestamp = doc.getLong("lastUpdatedTimestamp") ?: System.currentTimeMillis()
                )
            } else null
        } catch (_: Exception) { null }
    }
}
