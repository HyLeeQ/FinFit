package com.example.finfit.finance.repository

import com.example.finfit.finance.model.AppBankAccount
import com.example.finfit.finance.model.FinanceTransaction
import com.example.finfit.finance.model.AppUserWallet
import com.example.finfit.finance.model.TransactionType
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreRepository {
    private val db = Firebase.firestore
    private val usersCollection = db.collection("users")

    /** Lấy thông tin ví (bao gồm danh sách tài khoản) */
    suspend fun getUserWallet(uid: String): AppUserWallet? {
        return try {
            val doc = usersCollection.document(uid).get().await()
            if (!doc.exists()) return null

            // Đọc field cơ bản
            val savingsAmount   = doc.getDouble("savingsAmount")   ?: 0.0
            val disposableAmount = doc.getDouble("disposableAmount") ?: 0.0
            val isSavingsHidden = doc.getBoolean("isSavingsHidden") ?: false
            val isDisposableHidden = doc.getBoolean("isDisposableHidden") ?: false
            val card1Name  = doc.getString("card1Name")  ?: "THẺ CHÍNH"
            val card1Type  = doc.getString("card1Type")  ?: "Thẻ ngân hàng"
            val card1Color = (doc.getLong("card1Color") ?: 0L).toInt()
            val card2Name  = doc.getString("card2Name")  ?: "TIỀN MẶT"
            val card2Type  = doc.getString("card2Type")  ?: "Tiền mặt"
            val card2Color = (doc.getLong("card2Color") ?: 1L).toInt()

            // Đọc danh sách tài khoản
            @Suppress("UNCHECKED_CAST")
            val rawAccounts = doc.get("accounts") as? List<Map<String, Any>> ?: emptyList()
            val accounts = rawAccounts.map { m ->
                AppBankAccount(
                    id         = m["id"] as? String ?: "",
                    bankCode   = m["bankCode"] as? String ?: "OTHER",
                    name       = m["name"] as? String ?: "",
                    amount     = (m["amount"] as? Number)?.toDouble() ?: 0.0,
                    colorIndex = (m["colorIndex"] as? Number)?.toInt() ?: 0,
                    isHidden   = m["isHidden"] as? Boolean ?: false
                )
            }

            AppUserWallet(
                uid = uid,
                savingsAmount = savingsAmount,
                disposableAmount = disposableAmount,
                isSavingsHidden = isSavingsHidden,
                isDisposableHidden = isDisposableHidden,
                card1Name = card1Name, card1Type = card1Type, card1Color = card1Color,
                card2Name = card2Name, card2Type = card2Type, card2Color = card2Color,
                accounts = accounts
            )
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "getUserWallet err: ${e.message}")
            throw e // TUYỆT ĐỐI không trả về null khi lỗi, phải báo lỗi để tránh mất dữ liệu!
        }
    }

    /** Lưu ví (bao gồm danh sách tài khoản) */
    suspend fun saveUserWallet(wallet: AppUserWallet) {
        try {
            val accountsData = wallet.accounts.map { acc ->
                mapOf(
                    "id"         to acc.id,
                    "bankCode"   to acc.bankCode,
                    "name"       to acc.name,
                    "amount"     to acc.amount,
                    "colorIndex" to acc.colorIndex,
                    "isHidden"   to acc.isHidden
                )
            }
            val data = mapOf(
                "uid"                to wallet.uid,
                "savingsAmount"      to wallet.savingsAmount,
                "disposableAmount"   to wallet.disposableAmount,
                "isSavingsHidden"    to wallet.isSavingsHidden,
                "isDisposableHidden" to wallet.isDisposableHidden,
                "card1Name"          to wallet.card1Name,
                "card1Type"          to wallet.card1Type,
                "card1Color"         to wallet.card1Color,
                "card2Name"          to wallet.card2Name,
                "card2Type"          to wallet.card2Type,
                "card2Color"         to wallet.card2Color,
                "accounts"           to accountsData
            )
            usersCollection.document(wallet.uid).set(data)
            android.util.Log.d("FirestoreSuccess", "Đã lưu UserWallet")
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "saveUserWallet: ${e.message}")
            throw e
        }
    }

    /** Lấy danh sách giao dịch gần nhất */
    suspend fun getTransactions(uid: String, limit: Long = 20): List<FinanceTransaction> {
        return try {
            val snapshot = usersCollection
                .document(uid)
                .collection("transactions")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    val typeStr = doc.getString("type") ?: "EXPENSE"
                    val type = try {
                        TransactionType.valueOf(typeStr)
                    } catch (e: Exception) {
                        TransactionType.EXPENSE
                    }
                    FinanceTransaction(
                        id        = doc.id,
                        amount    = doc.getDouble("amount") ?: 0.0,
                        type      = type,
                        category  = doc.getString("category") ?: "",
                        note      = doc.getString("note") ?: "",
                        timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now()
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "getTransactions: ${e.message}")
            emptyList()
        }
    }

    /** Xóa giao dịch */
    suspend fun deleteTransaction(uid: String, transactionId: String) {
        try {
            usersCollection.document(uid).collection("transactions").document(transactionId).delete().await()
            android.util.Log.d("FirestoreSuccess", "Đã xóa giao dịch: $transactionId")
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "deleteTransaction err: ${e.message}")
            throw e
        }
    }

    /** Cập nhật giao dịch */
    suspend fun updateTransaction(uid: String, updatedTransaction: FinanceTransaction) {
        try {
            val data = mapOf(
                "amount"    to updatedTransaction.amount,
                "type"      to updatedTransaction.type.name,
                "category"  to updatedTransaction.category,
                "note"      to updatedTransaction.note,
                "timestamp" to updatedTransaction.timestamp
            )
            usersCollection.document(uid)
                .collection("transactions")
                .document(updatedTransaction.id)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .await()
            android.util.Log.d("FirestoreSuccess", "Đã cập nhật giao dịch: ${updatedTransaction.id}")
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "updateTransaction err: ${e.message}")
            throw e
        }
    }

    /** Thêm giao dịch mới */
    suspend fun addTransaction(uid: String, transaction: FinanceTransaction) {
        try {
            val data = mapOf(
                "amount"        to transaction.amount,
                "type"          to transaction.type.name,
                "category"      to transaction.category,
                "note"          to transaction.note,
                "paymentMethod" to transaction.paymentMethod.name,
                "timestamp"     to transaction.timestamp,
                "isFromOCR"     to transaction.isFromOCR,
                "imageUrl"      to transaction.imageUrl,
                "linkedGoalId"  to transaction.linkedGoalId
            )
            usersCollection.document(uid)
                .collection("transactions")
                .document(transaction.id.ifBlank { UUID.randomUUID().toString() })
                .set(data)
                .await()
            android.util.Log.d("FirestoreSuccess", "Đã thêm giao dịch mới: ${transaction.id}")
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "addTransaction err: ${e.message}")
            throw e
        }
    }
}
