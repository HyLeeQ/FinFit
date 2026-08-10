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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import com.example.finfit.finance.model.*
import com.example.finfit.finance.domain.repository.IFinanceRepository
import java.util.UUID

@Suppress("UNCHECKED_CAST")
private fun Any?.toMapList(): List<Map<String, Any>> =
    (this as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()

class FirestoreRepository : IFinanceRepository {
    companion object {
        var cachedWallet: AppUserWallet? = null
        var cachedTransactions: List<FinanceTransaction> = emptyList()
        var cachedGoals: List<SavingsGoal> = emptyList()
        var cachedBudgets: List<FinanceBudget> = emptyList()
        var cachedDebtLoans: List<DebtLoan> = emptyList()
        var cachedWeeklySchedule: List<SpendingScheduleItem> = emptyList()
        var cachedRecurringTransactions: List<RecurringTransaction> = emptyList()
        var cachedSavedGroups: List<SavedSplitGroup> = emptyList()
        var cachedCategoryRules: List<CategoryRule> = emptyList()
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
                    lastAutoSavingAt = doc.getTimestamp("lastAutoSavingAt"),
                    strategy     = try { SavingStrategy.valueOf(doc.getString("strategy") ?: "FIXED_SCHEDULE") } catch(e: Exception) { SavingStrategy.FIXED_SCHEDULE },
                    strategyValue = doc.getDouble("strategyValue") ?: 0.0,
                    priority     = try { GoalPriority.valueOf(doc.getString("priority") ?: "MEDIUM") } catch(e: Exception) { GoalPriority.MEDIUM },
                    linkedHeldFundId = doc.getString("linkedHeldFundId")
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "getSavingsGoals: ${e.message}")
            emptyList()
        }
    }

    /** Quan sát danh sách mục tiêu tiết kiệm thời gian thực */
    override fun observeSavingsGoals(uid: String): Flow<List<SavingsGoal>> = callbackFlow {
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
                    lastAutoSavingAt = doc.getTimestamp("lastAutoSavingAt"),
                    strategy     = try { SavingStrategy.valueOf(doc.getString("strategy") ?: "FIXED_SCHEDULE") } catch(e: Exception) { SavingStrategy.FIXED_SCHEDULE },
                    strategyValue = doc.getDouble("strategyValue") ?: 0.0,
                    priority     = try { GoalPriority.valueOf(doc.getString("priority") ?: "MEDIUM") } catch(e: Exception) { GoalPriority.MEDIUM },
                    linkedHeldFundId = doc.getString("linkedHeldFundId")
                )
            }
            cachedGoals = list
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    /** Quan sát danh sách hạn mức chi tiêu thời gian thực */
    override fun observeBudgets(uid: String): Flow<List<FinanceBudget>> = callbackFlow {
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
                    startDate = doc.getTimestamp("startDate") ?: Timestamp.now(),
                    isRollover = doc.getBoolean("isRollover") ?: false,
                    rolloverAmount = doc.getDouble("rolloverAmount") ?: 0.0,
                    isEnvelope = doc.getBoolean("isEnvelope") ?: false,
                    envelopeAllocated = doc.getDouble("envelopeAllocated") ?: 0.0
                )
            }
            cachedBudgets = list
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    /** Lưu hạn mức chi tiêu (Thêm/Sửa) */
    override suspend fun saveBudget(uid: String, budget: FinanceBudget) {
        try {
            val data = mapOf(
                "amount"            to budget.amount,
                "period"            to budget.period.name,
                "category"          to budget.category,
                "startDate"         to budget.startDate,
                "isRollover"        to budget.isRollover,
                "rolloverAmount"    to budget.rolloverAmount,
                "isEnvelope"        to budget.isEnvelope,
                "envelopeAllocated" to budget.envelopeAllocated
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
    override suspend fun deleteBudget(uid: String, budgetId: String) {
        try {
            usersCollection.document(uid).collection("budgets").document(budgetId).delete().await()
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "deleteBudget: ${e.message}")
            throw e
        }
    }

    /** Quan sát danh sách Nợ/Cho vay thời gian thực */
    override fun observeDebtLoans(uid: String): Flow<List<DebtLoan>> = callbackFlow {
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
                    personPhone = doc.getString("personPhone") ?: "",
                    amount     = doc.getDouble("amount") ?: 0.0,
                    paidAmount = doc.getDouble("paidAmount") ?: 0.0,
                    type       = try { DebtLoanType.valueOf(doc.getString("type") ?: "DEBT") } catch(e: Exception) { DebtLoanType.DEBT },
                    note       = doc.getString("note") ?: "",
                    dueDate    = doc.getTimestamp("dueDate"),
                    isPaid     = doc.getBoolean("isPaid") ?: false,
                    createdAt  = doc.getTimestamp("createdAt") ?: Timestamp.now(),
                    interestRate = doc.getDouble("interestRate") ?: 0.0,
                    isInstallment = doc.getBoolean("isInstallment") ?: false,
                    totalInstallments = (doc.getLong("totalInstallments") ?: 1L).toInt(),
                    paidInstallments = (doc.getLong("paidInstallments") ?: 0L).toInt(),
                    installmentAmount = doc.getDouble("installmentAmount") ?: 0.0
                )
            }
            cachedDebtLoans = list
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    /** Lưu Nợ/Cho vay (Thêm/Sửa) */
    override suspend fun saveDebtLoan(uid: String, debtLoan: DebtLoan) {
        try {
            val data = mapOf(
                "personName"        to debtLoan.personName,
                "personPhone"       to debtLoan.personPhone,
                "amount"            to debtLoan.amount,
                "paidAmount"        to debtLoan.paidAmount,
                "type"              to debtLoan.type.name,
                "note"              to debtLoan.note,
                "dueDate"           to debtLoan.dueDate,
                "isPaid"            to debtLoan.isPaid,
                "createdAt"         to debtLoan.createdAt,
                "interestRate"      to debtLoan.interestRate,
                "isInstallment"     to debtLoan.isInstallment,
                "totalInstallments" to debtLoan.totalInstallments,
                "paidInstallments"  to debtLoan.paidInstallments,
                "installmentAmount" to debtLoan.installmentAmount
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
    override suspend fun toggleDebtLoanPaidStatus(uid: String, debtLoanId: String, isPaid: Boolean) {
        try {
            usersCollection.document(uid).collection("debtLoans").document(debtLoanId).update("isPaid", isPaid).await()
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "toggleDebtLoanPaidStatus: ${e.message}")
            throw e
        }
    }

    /** Xóa Nợ/Cho vay */
    override suspend fun deleteDebtLoan(uid: String, debtLoanId: String) {
        try {
            usersCollection.document(uid).collection("debtLoans").document(debtLoanId).delete().await()
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "deleteDebtLoan: ${e.message}")
            throw e
        }
    }

    /** Lưu mục tiêu tiết kiệm (Thêm/Sửa) */
    override suspend fun saveSavingsGoal(uid: String, goal: SavingsGoal) {
        try {
            val data = mapOf(
                "goalName"          to goal.goalName,
                "targetAmount"      to goal.targetAmount,
                "currentAmount"     to goal.currentAmount,
                "targetDate"        to goal.targetDate,
                "iconEmoji"         to goal.iconEmoji,
                "colorHex"          to goal.colorHex,
                "createdAt"         to goal.createdAt,
                "autoSavingAmount"  to goal.autoSavingAmount,
                "lastAutoSavingAt"  to goal.lastAutoSavingAt,
                "strategy"          to goal.strategy.name,
                "strategyValue"     to goal.strategyValue,
                "priority"          to goal.priority.name,
                "linkedHeldFundId"  to goal.linkedHeldFundId
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
    override suspend fun deleteSavingsGoal(uid: String, goalId: String) {
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
    override suspend fun getUserWallet(uid: String): AppUserWallet? {
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
                    id                  = m["id"] as? String ?: "",
                    bankCode            = m["bankCode"] as? String ?: "OTHER",
                    name                = m["name"] as? String ?: "",
                    amount              = (m["amount"] as? Number)?.toDouble() ?: 0.0,
                    colorIndex          = (m["colorIndex"] as? Number)?.toInt() ?: 0,
                    isHidden            = m["isHidden"] as? Boolean ?: true,
                    purpose             = try { AccountPurpose.valueOf(m["purpose"] as? String ?: "DAILY_SPENDING") } catch(e: Exception) { AccountPurpose.DAILY_SPENDING },
                    lowBalanceThreshold = (m["lowBalanceThreshold"] as? Number)?.toDouble() ?: 0.0,
                    accountNumber       = m["accountNumber"] as? String ?: ""
                )
            }

            // Đọc trạng thái ẩn/hiện và tự động lưu
            val isTotalBalanceHidden = doc.getBoolean("isTotalBalanceHidden") ?: true
            val autoSaveWeeklySurplus = doc.getBoolean("autoSaveWeeklySurplus") ?: false

                // Đọc danh sách khoản trả trước nhóm
                @Suppress("UNCHECKED_CAST")
                val rawGroupPrepaid = doc.get("groupPrepaidItems") as? List<Map<String, Any>> ?: emptyList()
                val groupPrepaidItems = rawGroupPrepaid.map { m ->
                    GroupPrepaidItem(
                        id = m["id"] as? String ?: "",
                        transactionId = m["transactionId"] as? String ?: "",
                        description = m["description"] as? String ?: "",
                        totalAmount = (m["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                        groupOwedAmount = (m["groupOwedAmount"] as? Number)?.toDouble() ?: 0.0,
                        collectedAmount = (m["collectedAmount"] as? Number)?.toDouble() ?: 0.0,
                        participantCount = (m["participantCount"] as? Number)?.toInt() ?: 1,
                        participants = m["participants"].toMapList().map { p ->
                            TransactionParticipant(
                                name = p["name"] as? String ?: "",
                                shareAmount = (p["shareAmount"] as? Number)?.toDouble() ?: 0.0,
                                paidAmount = (p["paidAmount"] as? Number)?.toDouble() ?: 0.0,
                                isPaid = p["isPaid"] as? Boolean ?: false
                            )
                        },
                        createdAt = m["createdAt"] as? Timestamp ?: Timestamp.now(),
                        isFullyCollected = m["isFullyCollected"] as? Boolean ?: false
                    )
                }
                // Migration: nếu có groupPrepaidAmount cũ nhưng chưa có items
                val oldGroupPrepaid = doc.getDouble("groupPrepaidAmount") ?: 0.0
                val finalGroupPrepaidItems = if (groupPrepaidItems.isEmpty() && oldGroupPrepaid > 0) {
                    listOf(GroupPrepaidItem(
                        id = java.util.UUID.randomUUID().toString(),
                        description = "Khoản cũ chưa phân loại",
                        groupOwedAmount = oldGroupPrepaid,
                        createdAt = Timestamp.now()
                    ))
                } else groupPrepaidItems

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
                groupPrepaidItems = finalGroupPrepaidItems,
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
    override fun observeUserWallet(uid: String): Flow<AppUserWallet?> = callbackFlow {
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
                        id                  = m["id"] as? String ?: "",
                        bankCode            = m["bankCode"] as? String ?: "OTHER",
                        name                = m["name"] as? String ?: "",
                        amount              = (m["amount"] as? Number)?.toDouble() ?: 0.0,
                        colorIndex          = (m["colorIndex"] as? Number)?.toInt() ?: 0,
                        isHidden            = m["isHidden"] as? Boolean ?: true,
                        purpose             = try { AccountPurpose.valueOf(m["purpose"] as? String ?: "DAILY_SPENDING") } catch(e: Exception) { AccountPurpose.DAILY_SPENDING },
                        lowBalanceThreshold = (m["lowBalanceThreshold"] as? Number)?.toDouble() ?: 0.0,
                        accountNumber       = m["accountNumber"] as? String ?: ""
                    )
                }

                val isTotalBalanceHidden = snapshot.getBoolean("isTotalBalanceHidden") ?: true
                val autoSaveWeeklySurplus = snapshot.getBoolean("autoSaveWeeklySurplus") ?: false

                // Đọc danh sách khoản trả trước nhóm
                @Suppress("UNCHECKED_CAST")
                val rawGroupPrepaid = snapshot.get("groupPrepaidItems") as? List<Map<String, Any>> ?: emptyList()
                val groupPrepaidItems = rawGroupPrepaid.map { m ->
                    GroupPrepaidItem(
                        id = m["id"] as? String ?: "",
                        transactionId = m["transactionId"] as? String ?: "",
                        description = m["description"] as? String ?: "",
                        totalAmount = (m["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                        groupOwedAmount = (m["groupOwedAmount"] as? Number)?.toDouble() ?: 0.0,
                        collectedAmount = (m["collectedAmount"] as? Number)?.toDouble() ?: 0.0,
                        participantCount = (m["participantCount"] as? Number)?.toInt() ?: 1,
                        participants = m["participants"].toMapList().map { p ->
                            TransactionParticipant(
                                name = p["name"] as? String ?: "",
                                shareAmount = (p["shareAmount"] as? Number)?.toDouble() ?: 0.0,
                                paidAmount = (p["paidAmount"] as? Number)?.toDouble() ?: 0.0,
                                isPaid = p["isPaid"] as? Boolean ?: false
                            )
                        },
                        createdAt = m["createdAt"] as? Timestamp ?: Timestamp.now(),
                        isFullyCollected = m["isFullyCollected"] as? Boolean ?: false
                    )
                }
                val oldGroupPrepaid = snapshot.getDouble("groupPrepaidAmount") ?: 0.0
                val finalGroupPrepaidItems = if (groupPrepaidItems.isEmpty() && oldGroupPrepaid > 0) {
                    listOf(GroupPrepaidItem(
                        id = java.util.UUID.randomUUID().toString(),
                        description = "Khoản cũ chưa phân loại",
                        groupOwedAmount = oldGroupPrepaid,
                        createdAt = Timestamp.now()
                    ))
                } else groupPrepaidItems

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
                    groupPrepaidItems = finalGroupPrepaidItems,
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
    override suspend fun saveUserWallet(wallet: AppUserWallet) {
        try {
            val accountsData = wallet.accounts.map { acc ->
                mapOf(
                    "id"                  to acc.id,
                    "bankCode"            to acc.bankCode,
                    "name"                to acc.name,
                    "amount"              to acc.amount,
                    "colorIndex"          to acc.colorIndex,
                    "isHidden"            to acc.isHidden,
                    "purpose"             to acc.purpose.name,
                    "lowBalanceThreshold" to acc.lowBalanceThreshold,
                    "accountNumber"       to acc.accountNumber
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
                "groupPrepaidAmount" to 0.0,
                "groupPrepaidItems"  to wallet.groupPrepaidItems.map { item ->
                    mapOf(
                        "id" to item.id,
                        "transactionId" to item.transactionId,
                        "description" to item.description,
                        "totalAmount" to item.totalAmount,
                        "groupOwedAmount" to item.groupOwedAmount,
                        "collectedAmount" to item.collectedAmount,
                        "participantCount" to item.participantCount,
                        "participants" to item.participants.map { p ->
                            mapOf("name" to p.name, "shareAmount" to p.shareAmount,
                                  "paidAmount" to p.paidAmount, "isPaid" to p.isPaid)
                        },
                        "createdAt" to item.createdAt,
                        "isFullyCollected" to item.isFullyCollected
                    )
                },
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
                        participants = doc.get("participants").toMapList().map { m ->
                            TransactionParticipant(
                                name = m["name"] as? String ?: "",
                                shareAmount = (m["shareAmount"] as? Number)?.toDouble() ?: 0.0,
                                paidAmount = (m["paidAmount"] as? Number)?.toDouble() ?: 0.0,
                                isPaid = m["isPaid"] as? Boolean ?: false
                            )
                        }
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "getTransactions: ${e.message}")
            emptyList()
        }
    }

    /** Quan sát lịch sử giao dịch thời gian thực */
    override fun observeTransactions(uid: String, limit: Long): Flow<List<FinanceTransaction>> = callbackFlow {
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
                        participants = doc.get("participants").toMapList().map { m ->
                            TransactionParticipant(
                                name = m["name"] as? String ?: "",
                                shareAmount = (m["shareAmount"] as? Number)?.toDouble() ?: 0.0,
                                paidAmount = (m["paidAmount"] as? Number)?.toDouble() ?: 0.0,
                                isPaid = m["isPaid"] as? Boolean ?: false
                            )
                        }
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
    override suspend fun addTransaction(uid: String, transaction: FinanceTransaction) {
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
            val docRef = usersCollection.document(uid).collection("transactions").document(transaction.id)
            docRef.set(data).await()
            android.util.Log.d("FirestoreSuccess", "Đã thêm giao dịch: ${transaction.id}")
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "addTransaction err: ${e.message}")
            throw e
        }
    }
    /** Cập nhật giao dịch */
    override suspend fun updateTransaction(uid: String, transaction: FinanceTransaction) {
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
    override suspend fun deleteTransaction(uid: String, txId: String) {
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
    /**
     * Prefetch các dữ liệu quan trọng trong SplashScreen để MainScreen
     * không phải chờ khi vừa navigate vào.
     * Kết quả được cache vào companion object fields.
     */
    suspend fun prefetchUserData(uid: String) {
        try {
            // Timeout tối đa 5s nếu mạng chậm
            withTimeout(5000L) {
                coroutineScope {
                    val walletJob = async {
                        try {
                            val w = getUserWallet(uid)
                            if (w != null) cachedWallet = w
                        } catch (_: Exception) {}
                    }
                    val txJob = async {
                        try {
                            val list = getTransactions(uid, limit = 50)
                            cachedTransactions = list
                        } catch (_: Exception) {}
                    }
                    val goalsJob = async {
                        try {
                            val list = getSavingsGoals(uid)
                            cachedGoals = list
                        } catch (_: Exception) {}
                    }
                    walletJob.await()
                    txJob.await()
                    goalsJob.await()
                }
            }
        } catch (_: Exception) {
            // Timeout hoặc lỗi mạng → vào app bình thường, data sẽ load sau
        }
    }

    // ─── Giao dịch định kỳ (Recurring Transactions) ──────────────────────────

    /** Quan sát danh sách giao dịch định kỳ */
    override fun observeRecurringTransactions(uid: String): Flow<List<RecurringTransaction>> = callbackFlow {
        val listener = usersCollection.document(uid).collection("recurringTransactions")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot.documents.mapNotNull { doc ->
                    RecurringTransaction(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        type = try { TransactionType.valueOf(doc.getString("type") ?: "EXPENSE") } catch (e: Exception) { TransactionType.EXPENSE },
                        category = doc.getString("category") ?: "Hóa đơn & Tiện ích",
                        accountId = doc.getString("accountId"),
                        frequency = try { RecurringFrequency.valueOf(doc.getString("frequency") ?: "MONTHLY") } catch (e: Exception) { RecurringFrequency.MONTHLY },
                        dueDay = (doc.getLong("dueDay") ?: 1L).toInt(),
                        isAutoGenerated = doc.getBoolean("isAutoGenerated") ?: false,
                        isActive = doc.getBoolean("isActive") ?: true,
                        lastAppliedDate = doc.getTimestamp("lastAppliedDate"),
                        note = doc.getString("note") ?: ""
                    )
                }
                cachedRecurringTransactions = list
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /** Lưu giao dịch định kỳ */
    override suspend fun saveRecurringTransaction(uid: String, item: RecurringTransaction) {
        try {
            val data = mapOf(
                "name"            to item.name,
                "amount"          to item.amount,
                "type"            to item.type.name,
                "category"        to item.category,
                "accountId"       to item.accountId,
                "frequency"       to item.frequency.name,
                "dueDay"          to item.dueDay,
                "isAutoGenerated" to item.isAutoGenerated,
                "isActive"        to item.isActive,
                "lastAppliedDate" to item.lastAppliedDate,
                "note"            to item.note
            )
            val docRef = if (item.id.isBlank()) {
                usersCollection.document(uid).collection("recurringTransactions").document()
            } else {
                usersCollection.document(uid).collection("recurringTransactions").document(item.id)
            }
            docRef.set(data, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "saveRecurringTransaction: ${e.message}")
            throw e
        }
    }

    /** Xóa giao dịch định kỳ */
    override suspend fun deleteRecurringTransaction(uid: String, id: String) {
        try {
            usersCollection.document(uid).collection("recurringTransactions").document(id).delete().await()
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "deleteRecurringTransaction: ${e.message}")
            throw e
        }
    }

    // ─── Nhóm chia bill cố định (Saved Split Groups) ───────────────────────────

    /** Quan sát danh sách nhóm chia bill */
    override fun observeSavedSplitGroups(uid: String): Flow<List<SavedSplitGroup>> = callbackFlow {
        val listener = usersCollection.document(uid).collection("savedSplitGroups")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot.documents.mapNotNull { doc ->
                    @Suppress("UNCHECKED_CAST")
                    val members = doc.get("members") as? List<String> ?: emptyList()
                    SavedSplitGroup(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        members = members,
                        createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
                    )
                }
                cachedSavedGroups = list
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /** Lưu nhóm chia bill */
    override suspend fun saveSplitGroup(uid: String, group: SavedSplitGroup) {
        try {
            val data = mapOf(
                "name"      to group.name,
                "members"   to group.members,
                "createdAt" to group.createdAt
            )
            val docRef = if (group.id.isBlank()) {
                usersCollection.document(uid).collection("savedSplitGroups").document()
            } else {
                usersCollection.document(uid).collection("savedSplitGroups").document(group.id)
            }
            docRef.set(data, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "saveSavedSplitGroup: ${e.message}")
            throw e
        }
    }

    /** Xóa nhóm chia bill */
    override suspend fun deleteSplitGroup(uid: String, groupId: String) {
        try {
            usersCollection.document(uid).collection("savedSplitGroups").document(groupId).delete().await()
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "deleteSavedSplitGroup: ${e.message}")
            throw e
        }
    }

    // ─── Luật học phân loại danh mục (Category Rules) ────────────────────────

    /** Quan sát danh sách luật phân loại tự động */
    override fun observeCategoryRules(uid: String): Flow<List<CategoryRule>> = callbackFlow {
        val listener = usersCollection.document(uid).collection("categoryRules")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot.documents.mapNotNull { doc ->
                    CategoryRule(
                        id = doc.id,
                        keyword = doc.getString("keyword") ?: "",
                        category = doc.getString("category") ?: "",
                        confidenceCount = (doc.getLong("confidenceCount") ?: 1L).toInt()
                    )
                }
                cachedCategoryRules = list
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /** Lưu luật phân loại danh mục mới học được */
    override suspend fun saveCategoryRule(uid: String, rule: CategoryRule) {
        try {
            val data = mapOf(
                "keyword"         to rule.keyword,
                "category"        to rule.category,
                "confidenceCount" to rule.confidenceCount
            )
            val docRef = if (rule.id.isBlank()) {
                usersCollection.document(uid).collection("categoryRules").document()
            } else {
                usersCollection.document(uid).collection("categoryRules").document(rule.id)
            }
            docRef.set(data, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: Exception) {
            android.util.Log.e("FirestoreError", "saveCategoryRule: ${e.message}")
        }
    }
}

