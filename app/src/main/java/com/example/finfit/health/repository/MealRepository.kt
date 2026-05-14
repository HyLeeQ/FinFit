package com.example.finfit.health.repository

import android.util.Log
import com.example.finfit.data.repository.AuthRepository
import com.example.finfit.health.model.FoodMealEntity
import com.example.finfit.health.model.MealItemEntity
import com.example.finfit.health.model.vision.VisionAiResult
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MealRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val authRepository = AuthRepository()

    /**
     * Saves a complete meal session header and all its items in an atomic batch.
     * Updates daily nutrition summary at the same time.
     */
    suspend fun saveMultiItemMealSession(
        meal: FoodMealEntity,
        items: List<MealItemEntity>
    ): VisionAiResult<String> {
        if (items.isEmpty()) return VisionAiResult.Error("No items to save")
        
        val userId = authRepository.getCurrentUser()?.uid ?: return VisionAiResult.Error("User not logged in")
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(meal.createdAt))
        
        return try {
            val historyDocRef = firestore.collection("users")
                .document(userId)
                .collection("health_history")
                .document(date)

            val mealDocRef = historyDocRef.collection("meals").document()
            
            // Header takes the image of the first item as its preview
            val mealWithPreview = meal.copy(
                id = mealDocRef.id,
                previewImageUrl = items.first().imageUrl
            )

            firestore.runBatch { batch ->
                // 1. Create the meal header
                batch.set(mealDocRef, mealWithPreview)

                // 2. Create all meal items in the subcollection
                items.forEach { item ->
                    val itemDocRef = mealDocRef.collection("items").document()
                    batch.set(itemDocRef, item.copy(id = itemDocRef.id))
                }

                // 3. Update daily nutrition summary atomically
                val summaryData = mapOf(
                    "caloriesIn" to FieldValue.increment(meal.totalCalories.toLong()),
                    "carbs" to FieldValue.increment(meal.totalCarbs.toLong()),
                    "protein" to FieldValue.increment(meal.totalProtein.toLong()),
                    "fat" to FieldValue.increment(meal.totalFat.toLong()),
                    "lastUpdated" to FieldValue.serverTimestamp(),
                    "syncStatus" to 2 // SYNCED_WITH_CLOUD
                )
                batch.set(historyDocRef, summaryData, SetOptions.merge())
            }.await()

            VisionAiResult.Success(mealDocRef.id)
        } catch (e: Exception) {
            Log.e("MealRepository", "Error saving multi-item meal", e)
            VisionAiResult.Error("Failed to save meal session")
        }
    }

    /**
     * Saves a complete meal session header and its first item in an atomic batch.
     * Updates daily nutrition summary at the same time.
     */
    suspend fun saveNewMealSession(
        meal: FoodMealEntity,
        firstItem: MealItemEntity
    ): VisionAiResult<String> {
        return saveMultiItemMealSession(meal, listOf(firstItem))
    }

    suspend fun getMealsByDate(date: String): VisionAiResult<List<FoodMealEntity>> {
        val userId = authRepository.getCurrentUser()?.uid ?: return VisionAiResult.Error("User not logged in")
        
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("health_history")
                .document(date)
                .collection("meals")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val meals = snapshot.toObjects(FoodMealEntity::class.java)
            VisionAiResult.Success(meals)
        } catch (e: Exception) {
            Log.e("MealRepository", "Error fetching meals", e)
            VisionAiResult.Error("Failed to fetch meals")
        }
    }

    suspend fun getMealDetails(mealId: String): VisionAiResult<Pair<FoodMealEntity, List<MealItemEntity>>> {
        val userId = authRepository.getCurrentUser()?.uid ?: return VisionAiResult.Error("User not logged in")
        return try {
            val snapshot = firestore.collectionGroup("meals")
                .whereEqualTo("id", mealId)
                .limit(1)
                .get()
                .await()
            
            if (snapshot.isEmpty) return VisionAiResult.Error("Không tìm thấy bữa ăn")
            
            val mealDoc = snapshot.documents[0]
            val meal = mealDoc.toObject(FoodMealEntity::class.java) ?: return VisionAiResult.Error("Lỗi parse dữ liệu")
            
            val itemsSnapshot = mealDoc.reference.collection("items")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .await()
            val items = itemsSnapshot.toObjects(MealItemEntity::class.java)
            
            VisionAiResult.Success(Pair(meal, items))
        } catch (e: Exception) {
            Log.e("MealRepository", "Error fetching meal detail", e)
            VisionAiResult.Error("Failed to fetch meal detail")
        }
    }

    fun observeMealsByDate(date: String): Flow<List<FoodMealEntity>> = callbackFlow {
        val userId = authRepository.getCurrentUser()?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(userId)
            .collection("health_history")
            .document(date)
            .collection("meals")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MealRepository", "Error observing meals", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    try {
                        val meals = snapshot.toObjects(FoodMealEntity::class.java)
                        trySend(meals)
                    } catch (e: Exception) {
                        Log.e("MealRepository", "Error parsing meals list", e)
                        trySend(emptyList())
                    }
                }
            }

        awaitClose { listener.remove() }
    }

    fun observeMealDetails(mealId: String): Flow<Pair<FoodMealEntity?, List<MealItemEntity>>> = callbackFlow {
        val userId = authRepository.getCurrentUser()?.uid
        if (userId == null) {
            close()
            return@callbackFlow
        }

        var itemsListener: com.google.firebase.firestore.ListenerRegistration? = null

        val queryListener = firestore.collectionGroup("meals")
            .whereEqualTo("id", mealId)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MealRepository", "Error observing meal query", error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null || snapshot.isEmpty) {
                    return@addSnapshotListener
                }

                val mealDoc = snapshot.documents[0]
                val meal = mealDoc.toObject(FoodMealEntity::class.java)
                
                itemsListener?.remove()
                itemsListener = mealDoc.reference.collection("items")
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
                    .addSnapshotListener { itemsSnapshot, itemsError ->
                        if (itemsError != null || itemsSnapshot == null) {
                            trySend(Pair(meal, emptyList()))
                            return@addSnapshotListener
                        }
                        try {
                            val items = itemsSnapshot.toObjects(MealItemEntity::class.java) ?: emptyList()
                            trySend(meal to items)
                        } catch (e: Exception) {
                            Log.e("MealRepository", "Error parsing meal items", e)
                            trySend(meal to emptyList())
                        }
                    }
            }

        awaitClose { 
            queryListener.remove()
            itemsListener?.remove()
        }
    }

    fun observeDailySummary(date: String): Flow<Map<String, Any>?> = callbackFlow {
        val userId = authRepository.getCurrentUser()?.uid
        if (userId == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(userId)
            .collection("health_history")
            .document(date)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MealRepository", "Error observing summary", error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.data)
            }

        awaitClose { listener.remove() }
    }
}
