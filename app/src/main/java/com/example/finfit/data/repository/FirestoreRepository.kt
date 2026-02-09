package com.example.finfit.data.repository

import com.example.finfit.data.model.Transaction
import com.example.finfit.data.model.UserWallet
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db = Firebase.firestore
    private val usersCollection = db.collection("users")

    /**
     * Lấy thông tin ví của người dùng
     */
    suspend fun getUserWallet(uid: String): UserWallet? {
        return try {
            val document = usersCollection.document(uid).get().await()
            document.toObject(UserWallet::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Tạo hoặc cập nhật ví người dùng
     */
    suspend fun saveUserWallet(wallet: UserWallet) {
        try {
            usersCollection.document(wallet.uid).set(wallet).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Thêm một giao dịch mới (Chi tiêu/Thu nhập/Chuyển tiền)
     * Tự động cập nhật số dư ví tương ứng
     */
    suspend fun addTransaction(uid: String, transaction: Transaction) {
        val userRef = usersCollection.document(uid)
        val transactionRef = userRef.collection("transactions").document()
        
        db.runTransaction { firestoreTransaction ->
            // 1. Lấy dữ liệu ví hiện tại
            val walletDoc = firestoreTransaction.get(userRef)
            val wallet = walletDoc.toObject(UserWallet::class.java) ?: UserWallet(uid = uid)

            // 2. Tính toán số dư mới dựa trên loại giao dịch
            var newDisposable = wallet.disposableAmount
            var newSavings = wallet.savingsAmount

            when (transaction.type.name) {
                "EXPENSE" -> {
                    newDisposable -= transaction.amount
                }
                "INCOME" -> {
                    newDisposable += transaction.amount
                }
                "TRANSFER" -> {
                    // Chuyển từ tiền được dùng sang tiết kiệm
                    newDisposable -= transaction.amount
                    newSavings += transaction.amount
                }
            }

            // 3. Cập nhật ví và lưu giao dịch
            val updatedWallet = wallet.copy(
                disposableAmount = newDisposable,
                savingsAmount = newSavings
            )
            
            firestoreTransaction.set(userRef, updatedWallet)
            firestoreTransaction.set(transactionRef, transaction.copy(id = transactionRef.id))
        }.await()
    }
}
