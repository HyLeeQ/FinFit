package com.example.finfit.finance.repository

import com.example.finfit.finance.model.AppBankAccount
import com.example.finfit.finance.model.FinanceTransaction
import com.example.finfit.finance.model.AppUserWallet
import com.example.finfit.finance.model.TransactionType
import com.example.finfit.finance.model.SavingsGoal
import com.example.finfit.finance.model.PaymentMethod
import com.example.finfit.finance.model.HeldFundItem
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.awaitClose
import com.example.finfit.finance.model.*
import java.util.UUID

class FirestoreRepository {
    companion object {
        var cachedWallet: AppUserWallet? = null
        var cachedTransactions: List<FinanceTransaction> = emptyList()
        var cachedGoals: List<SavingsGoal> = emptyList()
        var cachedBudgets: List<FinanceBudget> = emptyList()
        var cachedDebtLoans: List<DebtLoan> = emptyList()
        var cachedWeeklySchedule: List<SpendingScheduleItem> = emptyList()
    }

    internal val db = Firebase.firestore
    internal val usersCollection = db.collection("users")

    /** Lấy danh sách mục tiêu tiết kiệm */
    suspend fun getSavingsGoals(uid: String): List<SavingsGoal> {
        return try {
            val snapshot = usersCollection.document(uid).collection("savingsGoals").get().await()
            snapshot.documents.mapNotNull { doc ->
                SavingsGoal(
                    id           = doc.id,
                    goalName     = doc.getString("goalName") ?: "",
                    targetAmount = doc.getDouble("targetAmount") ?: 0.0,
                    currentAmount = doc.getDouble("currentAmount") ?: 0.0,
                    targetDate   = doc.getTimestamp("targetDate"),
                    iconEmoji    = doc.getString("iconEmoji") ?: "🎯",
                    colorHex     = doc.getLong("colorHex") ?: 0xFF3B82F6L,
                    createdAt    = doc.getTimestamp("createdAt") ?: Timestamp.now(),
                    autoSavingAmount = doc.getDouble("autoSavingAmount") ?: 0.0,
                    lastAutoSavingAt = doc.getTimestamp("lastAutoSavingAt")
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "getSavingsGoals: ${e.message}")
            emptyList()
        }
    }

    /** Quan sát danh sách mục tiêu tiết kiệm thời gian thực */
    fun observeSavingsGoals(uid: String): Flow<List<SavingsGoal>> = callbackFlow {
        val listener = usersCollection.document(uid).collection("savingsGoals").addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FirestoreError", "observeSavingsGoals: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot.documents.mapNotNull { doc ->
                SavingsGoal(
                    id           = doc.id,
                    goalName     = doc.getString("goalName") ?: "",
                    targetAmount = doc.getDouble("targetAmount") ?: 0.0,
                    currentAmount = doc.getDouble("currentAmount") ?: 0.0,
                    targetDate   = doc.getTimestamp("targetDate"),
                    iconEmoji    = doc.getString("iconEmoji") ?: "🎯",
                    colorHex     = doc.getLong("colorHex") ?: 0xFF3B82F6L,
                    createdAt    = doc.getTimestamp("createdAt") ?: Timestamp.now(),
                    autoSavingAmount = doc.getDouble("autoSavingAmount") ?: 0.0,
                    lastAutoSavingAt = doc.getTimestamp("lastAutoSavingAt")
                )
            }
            cachedGoals = list
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    /** Quan sát danh sách hạn mức chi tiêu thời gian thực */
    fun observeBudgets(uid: String): Flow<List<FinanceBudget>> = callbackFlow {
        val listener = usersCollection.document(uid).collection("budgets").addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FirestoreError", "observeBudgets: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot.documents.mapNotNull { doc ->
                FinanceBudget(
                    id        = doc.id,
                    amount    = doc.getDouble("amount") ?: 0.0,
                    period    = try { BudgetPeriod.valueOf(doc.getString("period") ?: "MONTHLY") } catch(e: Exception) { BudgetPeriod.MONTHLY },
                    category  = doc.getString("category") ?: "Tất cả",
                    startDate = doc.getTimestamp("startDate") ?: Timestamp.now()
                )
            }
            cachedBudgets = list
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    /** Lưu hạn mức chi tiêu (Thêm/Sửa) */
    suspend fun saveBudget(uid: String, budget: FinanceBudget) {
        try {
            val data = mapOf(
                "amount"    to budget.amount,
                "period"    to budget.period.name,
                "category"  to budget.category,
                "startDate" to budget.startDate
            )
            val docRef = if (budget.id.isBlank()) {
                usersCollection.document(uid).collection("budgets").document()
            } else {
                usersCollection.document(uid).collection("budgets").document(budget.id)
            }
            docRef.set(data, com.google.firebase.firestore.SetOptions.merge())
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "saveBudget: ${e.message}")
            throw e
        }
    }

    /** Xóa hạn mức chi tiêu */
    suspend fun deleteBudget(uid: String, budgetId: String) {
        try {
            usersCollection.document(uid).collection("budgets").document(budgetId).delete().await()
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "deleteBudget: ${e.message}")
            throw e
        }
    }

    /** Quan sát danh sách Nợ/Cho vay thời gian thực */
    fun observeDebtLoans(uid: String): Flow<List<DebtLoan>> = callbackFlow {
        val listener = usersCollection.document(uid).collection("debtLoans").addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FirestoreError", "observeDebtLoans: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot.documents.mapNotNull { doc ->
                DebtLoan(
                    id         = doc.id,
                    personName = doc.getString("personName") ?: "",
                    amount     = doc.getDouble("amount") ?: 0.0,
                    type       = try { DebtLoanType.valueOf(doc.getString("type") ?: "DEBT") } catch(e: Exception) { DebtLoanType.DEBT },
                    note       = doc.getString("note") ?: "",
                    dueDate    = doc.getTimestamp("dueDate"),
                    isPaid     = doc.getBoolean("isPaid") ?: false,
                    createdAt  = doc.getTimestamp("createdAt") ?: Timestamp.now()
                )
            }
            cachedDebtLoans = list
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    /** Lưu Nợ/Cho vay (Thêm/Sửa) */
    suspend fun saveDebtLoan(uid: String, debtLoan: DebtLoan) {
        try {
            val data = mapOf(
                "personName" to debtLoan.personName,
                "amount"     to debtLoan.amount,
                "type"       to debtLoan.type.name,
                "note"       to debtLoan.note,
                "dueDate"    to debtLoan.dueDate,
                "isPaid"     to debtLoan.isPaid,
                "createdAt"  to debtLoan.createdAt
            )
            val docRef = if (debtLoan.id.isBlank()) {
                usersCollection.document(uid).collection("debtLoans").document()
            } else {
                usersCollection.document(uid).collection("debtLoans").document(debtLoan.id)
            }
            docRef.set(data, com.google.firebase.firestore.SetOptions.merge())
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "saveDebtLoan: ${e.message}")
            throw e
        }
    }

    /** Cập nhật trạng thái đã thanh toán */
    suspend fun toggleDebtLoanPaidStatus(uid: String, debtLoanId: String, isPaid: Boolean) {
        try {
            usersCollection.document(uid).collection("debtLoans").document(debtLoanId).update("isPaid", isPaid).await()
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "toggleDebtLoanPaidStatus: ${e.message}")
            throw e
        }
    }

    /** Xóa Nợ/Cho vay */
    suspend fun deleteDebtLoan(uid: String, debtLoanId: String) {
        try {
            usersCollection.document(uid).collection("debtLoans").document(debtLoanId).delete().await()
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "deleteDebtLoan: ${e.message}")
            throw e
        }
    }

    /** Lưu mục tiêu tiết kiệm (Thêm/Sửa) */
    suspend fun saveSavingsGoal(uid: String, goal: SavingsGoal) {
        try {
            val data = mapOf(
                "goalName"     to goal.goalName,
                "targetAmount" to goal.targetAmount,
                "currentAmount" to goal.currentAmount,
                "targetDate"   to goal.targetDate,
                "iconEmoji"    to goal.iconEmoji,
                "colorHex"     to goal.colorHex,
                "createdAt"    to goal.createdAt,
                "autoSavingAmount" to goal.autoSavingAmount,
                "lastAutoSavingAt" to goal.lastAutoSavingAt
            )
            val docRef = if (goal.id.isBlank()) {
                usersCollection.document(uid).collection("savingsGoals").document()
            } else {
                usersCollection.document(uid).collection("savingsGoals").document(goal.id)
            }
            docRef.set(data, com.google.firebase.firestore.SetOptions.merge())
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "saveSavingsGoal: ${e.message}")
            throw e
        }
    }

    /** Xóa mục tiêu tiết kiệm */
    suspend fun deleteSavingsGoal(uid: String, goalId: String) {
        try {
            usersCollection.document(uid).collection("savingsGoals").document(goalId).delete()
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "deleteSavingsGoal: ${e.message}")
            throw e
        }
    }

    suspend fun saveSpendingSchedule(uid: String, item: SpendingScheduleItem) {
        try {
            val data = mapOf(
                "dayOfWeek" to item.dayOfWeek,
                "amount" to item.amount,
                "category" to item.category,
                "note" to item.note,
                "isAutoApply" to item.isAutoApply
            )
            val docRef = if (item.id.isBlank()) {
                usersCollection.document(uid).collection("schedules").document()
            } else {
                usersCollection.document(uid).collection("schedules").document(item.id)
            }
            docRef.set(data, com.google.firebase.firestore.SetOptions.merge())
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "saveSpendingSchedule: ${e.message}")
            throw e
        }
    }

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
            val generalSavings = doc.getDouble("generalSavings") ?: 0.0
            val card1Name  = doc.getString("card1Name")  ?: "THẺ CHÍNH"
            val card1Type  = doc.getString("card1Type")  ?: "Thẻ ngân hàng"
            val card1Color = (doc.getLong("card1Color") ?: 0L).toInt()
            val card2Name  = doc.getString("card2Name")  ?: "TIỀN MẶT"
            val card2Type  = doc.getString("card2Type")  ?: "Tiền mặt"
            val card2Color = (doc.getLong("card2Color") ?: 1L).toInt()

            // Đọc danh sách tiền giữ hộ
            @Suppress("UNCHECKED_CAST")
            val rawHeld = doc.get("heldFunds") as? List<Map<String, Any>> ?: emptyList()
            val heldFunds = rawHeld.map { m ->
                HeldFundItem(
                    id     = m["id"] as? String ?: "",
                    name   = m["name"] as? String ?: "",
                    amount = (m["amount"] as? Number)?.toDouble() ?: 0.0
                )
            }

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
                    isHidden   = m["isHidden"] as? Boolean ?: true
                )
            }

            // Đọc trạng thái ẩn/hiện và tự động lưu
            val isTotalBalanceHidden = doc.getBoolean("isTotalBalanceHidden") ?: true
            val autoSaveWeeklySurplus = doc.getBoolean("autoSaveWeeklySurplus") ?: false

            AppUserWallet(
                uid = uid,
                savingsAmount = savingsAmount,
                disposableAmount = disposableAmount,
                isSavingsHidden = isSavingsHidden,
                isDisposableHidden = isDisposableHidden,
                card1Name = card1Name, card1Type = card1Type, card1Color = card1Color,
                card2Name = card2Name, card2Type = card2Type, card2Color = card2Color,
                accounts = accounts,
                generalSavings = generalSavings,
                groupPrepaidAmount = doc.getDouble("groupPrepaidAmount") ?: 0.0,
                heldFunds = heldFunds,
                isTotalBalanceHidden = isTotalBalanceHidden,
                autoSaveWeeklySurplus = autoSaveWeeklySurplus
            )
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "getUserWallet err: ${e.message}")
            throw e
        }
    }

    /** Quan sát ví thời gian thực */
    fun observeUserWallet(uid: String): Flow<AppUserWallet?> = callbackFlow {
        val listener = usersCollection.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FirestoreError", "observeUserWallet: ${error.message}")
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot == null || !snapshot.exists()) {
                trySend(null)
                return@addSnapshotListener
            }

            try {
                val savingsAmount   = snapshot.getDouble("savingsAmount")   ?: 0.0
                val disposableAmount = snapshot.getDouble("disposableAmount") ?: 0.0
                val isSavingsHidden = snapshot.getBoolean("isSavingsHidden") ?: false
                val isDisposableHidden = snapshot.getBoolean("isDisposableHidden") ?: false
                val generalSavings = snapshot.getDouble("generalSavings") ?: 0.0
                val card1Name  = snapshot.getString("card1Name")  ?: "THẺ CHÍNH"
                val card1Type  = snapshot.getString("card1Type")  ?: "Thẻ ngân hàng"
                val card1Color = (snapshot.getLong("card1Color") ?: 0L).toInt()
                val card2Name  = snapshot.getString("card2Name")  ?: "TIỀN MẶT"
                val card2Type  = snapshot.getString("card2Type")  ?: "Tiền mặt"
                val card2Color = (snapshot.getLong("card2Color") ?: 1L).toInt()

                @Suppress("UNCHECKED_CAST")
                val rawHeld = snapshot.get("heldFunds") as? List<Map<String, Any>> ?: emptyList()
                val heldFunds = rawHeld.map { m ->
                    HeldFundItem(
                        id     = m["id"] as? String ?: "",
                        name   = m["name"] as? String ?: "",
                        amount = (m["amount"] as? Number)?.toDouble() ?: 0.0
                    )
                }

                @Suppress("UNCHECKED_CAST")
                val rawAccounts = snapshot.get("accounts") as? List<Map<String, Any>> ?: emptyList()
                val accounts = rawAccounts.map { m ->
                    AppBankAccount(
                        id         = m["id"] as? String ?: "",
                        bankCode   = m["bankCode"] as? String ?: "OTHER",
                        name       = m["name"] as? String ?: "",
                        amount     = (m["amount"] as? Number)?.toDouble() ?: 0.0,
                        colorIndex = (m["colorIndex"] as? Number)?.toInt() ?: 0,
                        isHidden   = m["isHidden"] as? Boolean ?: true
                    )
                }

                val isTotalBalanceHidden = snapshot.getBoolean("isTotalBalanceHidden") ?: true
                val autoSaveWeeklySurplus = snapshot.getBoolean("autoSaveWeeklySurplus") ?: false

                val newWallet = AppUserWallet(
                    uid = uid,
                    savingsAmount = savingsAmount,
                    disposableAmount = disposableAmount,
                    isSavingsHidden = isSavingsHidden,
                    isDisposableHidden = isDisposableHidden,
                    card1Name = card1Name, card1Type = card1Type, card1Color = card1Color,
                    card2Name = card2Name, card2Type = card2Type, card2Color = card2Color,
                    accounts = accounts,
                    generalSavings = generalSavings,
                    groupPrepaidAmount = snapshot.getDouble("groupPrepaidAmount") ?: 0.0,
                    heldFunds = heldFunds,
                    isTotalBalanceHidden = isTotalBalanceHidden,
                    autoSaveWeeklySurplus = autoSaveWeeklySurplus
                )
                cachedWallet = newWallet
                trySend(newWallet)
            } catch (e: Exception) {
                android.util.Log.e("FirestoreError", "observeUserWallet parse err: ${e.message}")
            }
        }
        awaitClose { listener.remove() }
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
                "accounts"           to accountsData,
                "generalSavings"     to wallet.generalSavings,
                "groupPrepaidAmount" to wallet.groupPrepaidAmount,
                "heldFunds"          to wallet.heldFunds.map {
                    mapOf("id" to it.id, "name" to it.name, "amount" to it.amount)
                },
                "isTotalBalanceHidden" to wallet.isTotalBalanceHidden,
                "autoSaveWeeklySurplus" to wallet.autoSaveWeeklySurplus
            )
            usersCollection.document(wallet.uid).set(data).await()
            android.util.Log.d("FirestoreSuccess", "Đã lưu UserWallet thành công")
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "saveUserWallet: ${e.message}")
            throw e
        }
    }

    /** Lấy danh sách giao dịch gần nhất */
    suspend fun getTransactions(uid: String, limit: Long = 50): List<FinanceTransaction> {
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
                        timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now(),
                        accountId = doc.getString("accountId"),
                        toAccountId = doc.getString("toAccountId"),
                        isGroupPrepayment = doc.getBoolean("isGroupPrepayment") ?: false,
                        personalAmount = doc.getDouble("personalAmount") ?: 0.0,
                        groupAmount = doc.getDouble("groupAmount") ?: 0.0,
                        participantCount = doc.getLong("participantCount")?.toInt() ?: 1,
                        participants = (doc.get("participants") as? List<Map<String, Any>>)?.map { m ->
                            TransactionParticipant(
                                name = m["name"] as? String ?: "",
                                shareAmount = (m["shareAmount"] as? Number)?.toDouble() ?: 0.0,
                                paidAmount = (m["paidAmount"] as? Number)?.toDouble() ?: 0.0,
                                isPaid = m["isPaid"] as? Boolean ?: false
                            )
                        } ?: emptyList()
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "getTransactions: ${e.message}")
            emptyList()
        }
    }

    /** Quan sát danh sách giao dịch thời gian thực */
    fun observeTransactions(uid: String, limit: Long = 50): Flow<List<FinanceTransaction>> = callbackFlow {
        val query = usersCollection.document(uid).collection("transactions")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FirestoreError", "observeTransactions: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val list = snapshot.documents.mapNotNull { doc ->
                try {
                    val typeStr = doc.getString("type") ?: "EXPENSE"
                    val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.EXPENSE }
                    FinanceTransaction(
                        id        = doc.id,
                        amount    = doc.getDouble("amount") ?: 0.0,
                        type      = type,
                        category  = doc.getString("category") ?: "",
                        note      = doc.getString("note") ?: "",
                        timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now(),
                        accountId = doc.getString("accountId"),
                        toAccountId = doc.getString("toAccountId"),
                        isGroupPrepayment = doc.getBoolean("isGroupPrepayment") ?: false,
                        personalAmount = doc.getDouble("personalAmount") ?: 0.0,
                        groupAmount = doc.getDouble("groupAmount") ?: 0.0,
                        participantCount = doc.getLong("participantCount")?.toInt() ?: 1,
                        participants = (doc.get("participants") as? List<Map<String, Any>>)?.map { m ->
                            TransactionParticipant(
                                name = m["name"] as? String ?: "",
                                shareAmount = (m["shareAmount"] as? Number)?.toDouble() ?: 0.0,
                                paidAmount = (m["paidAmount"] as? Number)?.toDouble() ?: 0.0,
                                isPaid = m["isPaid"] as? Boolean ?: false
                            )
                        } ?: emptyList()
                    )
                } catch (e: Exception) { null }
            }
            cachedTransactions = list
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    // --- End of Wallet methods ---


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
                "linkedGoalId"  to transaction.linkedGoalId,
                "accountId"     to transaction.accountId,
                "toAccountId"   to transaction.toAccountId,
                "isGroupPrepayment" to transaction.isGroupPrepayment,
                "personalAmount" to transaction.personalAmount,
                "groupAmount" to transaction.groupAmount,
                "participantCount" to transaction.participantCount,
                "participants" to transaction.participants.map { 
                    mapOf(
                        "name" to it.name,
                        "shareAmount" to it.shareAmount,
                        "paidAmount" to it.paidAmount,
                        "isPaid" to it.isPaid
                    )
                }
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
    /** Cập nhật giao dịch */
    suspend fun updateTransaction(uid: String, transaction: FinanceTransaction) {
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
                "linkedGoalId"  to transaction.linkedGoalId,
                "accountId"     to transaction.accountId,
                "toAccountId"   to transaction.toAccountId,
                "isGroupPrepayment" to transaction.isGroupPrepayment,
                "personalAmount" to transaction.personalAmount,
                "groupAmount" to transaction.groupAmount,
                "participantCount" to transaction.participantCount,
                "participants" to transaction.participants.map { 
                    mapOf(
                        "name" to it.name,
                        "shareAmount" to it.shareAmount,
                        "paidAmount" to it.paidAmount,
                        "isPaid" to it.isPaid
                    )
                }
            )
            usersCollection.document(uid)
                .collection("transactions")
                .document(transaction.id)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .await()
            android.util.Log.d("FirestoreSuccess", "Đã cập nhật giao dịch: ${transaction.id}")
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "updateTransaction err: ${e.message}")
            throw e
        }
    }

    /** Xóa giao dịch */
    suspend fun deleteTransaction(uid: String, txId: String) {
        try {
            usersCollection.document(uid).collection("transactions").document(txId).delete()
            android.util.Log.d("FirestoreSuccess", "Đã xóa giao dịch: $txId")
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "deleteTransaction err: ${e.message}")
            throw e
        }
    }

    // --- Lịch trình chi tiêu tuần (Weekly Schedule) ---

    /** Quan sát lịch trình chi tiêu tuần thời gian thực */
    fun observeWeeklySchedule(uid: String): Flow<List<SpendingScheduleItem>> = callbackFlow {
        val listener = usersCollection.document(uid).collection("spending_schedule")
            .addSnapshotListener { snapshot, e ->
                if (e != null) { close(e); return@addSnapshotListener }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    SpendingScheduleItem(
                        id          = doc.id,
                        dayOfWeek   = (doc.getLong("dayOfWeek") ?: 1L).toInt(),
                        amount      = doc.getDouble("amount") ?: 0.0,
                        category    = doc.getString("category") ?: "Khác",
                        note        = doc.getString("note") ?: "",
                        isAutoApply = doc.getBoolean("isAutoApply") ?: false
                    )
                } ?: emptyList()
                cachedWeeklySchedule = items
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    /** Lưu một mục lịch trình chi tiêu tuần */
    suspend fun saveWeeklyScheduleItem(uid: String, item: SpendingScheduleItem) {
        val data = mapOf(
            "dayOfWeek"   to item.dayOfWeek,
            "amount"      to item.amount,
            "category"    to item.category,
            "note"        to item.note,
            "isAutoApply" to item.isAutoApply
        )
        val docId = if (item.id.isBlank()) UUID.randomUUID().toString() else item.id
        usersCollection.document(uid).collection("spending_schedule").document(docId).set(data).await()
    }

    /** Xóa một mục lịch trình chi tiêu tuần */
    suspend fun deleteWeeklyScheduleItem(uid: String, id: String) {
        usersCollection.document(uid).collection("spending_schedule").document(id).delete().await()
    }

    // --- Thói quen người dùng (User Habits) ---

    /** Lấy thói quen người dùng */
    suspend fun getUserHabit(uid: String): UserHabit? {
        return try {
            val doc = usersCollection.document(uid).collection("config").document("habit").get().await()
            if (!doc.exists()) return null
            mapDocToUserHabit(doc)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "getUserHabit: ${e.message}")
            null
        }
    }

    /** Theo dõi thói quen người dùng thời gian thực */
    fun observeUserHabit(uid: String): Flow<UserHabit?> = callbackFlow {
        val listener = usersCollection.document(uid).collection("config").document("habit")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreError", "observeUserHabit: ${error.message}")
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(mapDocToUserHabit(snapshot))
            }
        awaitClose { listener.remove() }
    }

    private fun mapDocToUserHabit(doc: com.google.firebase.firestore.DocumentSnapshot): UserHabit {
        @Suppress("UNCHECKED_CAST")
        val rawRoutine = doc.get("routineSchedules") as? List<Map<String, Any>> ?: emptyList()
        val routines = rawRoutine.map { m ->
            RoutineSchedule(
                startDay = (m["startDay"] as? Number)?.toInt() ?: 1,
                endDay = (m["endDay"] as? Number)?.toInt() ?: 1,
                location = m["location"] as? String ?: "",
                note = m["note"] as? String ?: ""
            )
        }

        @Suppress("UNCHECKED_CAST")
        val rawFixed = doc.get("fixedCosts") as? List<Map<String, Any>> ?: emptyList()
        val fixed = rawFixed.map { m ->
            SpendingScheduleItem(
                id = m["id"] as? String ?: "",
                dayOfWeek = (m["dayOfWeek"] as? Number)?.toInt() ?: 1,
                amount = (m["amount"] as? Number)?.toDouble() ?: 0.0,
                category = m["category"] as? String ?: "",
                note = m["note"] as? String ?: "",
                isAutoApply = m["isAutoApply"] as? Boolean ?: false
            )
        }

        return UserHabit(
            minMealCost = doc.getDouble("minMealCost") ?: 0.0,
            maxMealCost = doc.getDouble("maxMealCost") ?: 0.0,
            routineSchedules = routines,
            fixedCosts = fixed,
            lastProactiveWeek = doc.getString("lastProactiveWeek") ?: "",
            generalNotes = doc.getString("generalNotes") ?: ""
        )
    }

    /** Lưu thói quen người dùng */
    suspend fun saveUserHabit(uid: String, habit: UserHabit) {
        try {
            val routineData = habit.routineSchedules.map {
                mapOf("startDay" to it.startDay, "endDay" to it.endDay, "location" to it.location, "note" to it.note)
            }
            val fixedData = habit.fixedCosts.map {
                mapOf("id" to it.id, "dayOfWeek" to it.dayOfWeek, "amount" to it.amount, "category" to it.category, "note" to it.note, "isAutoApply" to it.isAutoApply)
            }

            val data = mapOf(
                "minMealCost" to habit.minMealCost,
                "maxMealCost" to habit.maxMealCost,
                "routineSchedules" to routineData,
                "fixedCosts" to fixedData,
                "lastProactiveWeek" to habit.lastProactiveWeek,
                "generalNotes" to habit.generalNotes
            )
            usersCollection.document(uid).collection("config").document("habit")
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .await()
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "saveUserHabit: ${e.message}")
            throw e
        }
    }

}
