package com.example.finfit.finance.repository

import com.example.finfit.finance.model.BankAccount
import com.example.finfit.finance.model.Transaction
import com.example.finfit.finance.model.UserWallet
import com.example.finfit.finance.model.TransactionType
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db = Firebase.firestore
    private val usersCollection = db.collection("users")

    /** Lấy thông tin ví (bao gồm danh sách tài khoản) */
    suspend fun getUserWallet(uid: String): UserWallet? {
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
                BankAccount(
                    id         = m["id"] as? String ?: "",
                    bankCode   = m["bankCode"] as? String ?: "OTHER",
                    name       = m["name"] as? String ?: "",
                    amount     = (m["amount"] as? Number)?.toDouble() ?: 0.0,
                    colorIndex = (m["colorIndex"] as? Number)?.toInt() ?: 0,
                    isHidden   = m["isHidden"] as? Boolean ?: false
                )
            }

            UserWallet(
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
            null
        }
    }

    /** Lưu ví (bao gồm danh sách tài khoản) */
    suspend fun saveUserWallet(wallet: UserWallet) {
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
            usersCollection.document(wallet.uid).set(data).await()
            android.util.Log.d("FirestoreSuccess", "Đã lưu UserWallet")
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "saveUserWallet: ${e.message}")
            throw e
        }
    }

    /** Lấy danh sách giao dịch gần nhất */
    suspend fun getTransactions(uid: String, limit: Long = 20): List<Transaction> {
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
                    Transaction(
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

    /** Thêm giao dịch mới */
    suspend fun addTransaction(uid: String, transaction: Transaction) {
        try {
            val ref = usersCollection.document(uid).collection("transactions").document()
            val data = mapOf(
                "id"        to ref.id,
                "amount"    to transaction.amount,
                "type"      to transaction.type.name,
                "category"  to transaction.category,
                "note"      to transaction.note,
                "timestamp" to transaction.timestamp
            )
            ref.set(data).await()
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "addTransaction: ${e.message}")
            throw e
        }
    }
}
