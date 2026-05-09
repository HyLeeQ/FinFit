package com.example.finfit.ui.assistant

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.finfit.data.remote.GeminiService
import com.example.finfit.data.remote.QuotaExceededException
import com.example.finfit.finance.model.*
import com.example.finfit.finance.repository.FirestoreRepository
import com.example.finfit.finance.util.LocalAIEngine
import com.example.finfit.finance.util.ParsedTransaction
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AssistantViewModel(
        private val firestoreRepository: FirestoreRepository,
        private val geminiService: GeminiService = GeminiService(),
        private val userId: String,
        var wallet: AppUserWallet?,
        var transactions: List<FinanceTransaction>,
        var schedule: List<SpendingScheduleItem>,
        var debtLoans: List<DebtLoan>,
        var savingsGoals: List<SavingsGoal>,
        var budgets: List<FinanceBudget>,
        var stepsToday: Int,
        private val initialHabit: UserHabit? = null
) : ViewModel() {

    private val _userHabit = MutableStateFlow(initialHabit ?: UserHabit())
    val userHabit = _userHabit.asStateFlow()

    private val _messages =
            MutableStateFlow<List<ChatMessage>>(
                    listOf(
                            ChatMessage(
                                    content =
                                            MessageContent.Text(
                                                    "Xin chào! 👋 Tôi là Trợ lý FinFit.\n\n" +
                                                            "Bạn có thể:\n" +
                                                            "• Nhắn \"ăn tối 20k\" → Tôi tự tạo giao dịch\n" +
                                                            "• Nhắn \"vay Nam 500k\" → Ghi nợ & cho vay\n" +
                                                            "• Nhắn \"tạo tiết kiệm mua xe 50 triệu\" → Lập mục tiêu\n" +
                                                            "• Hỏi tôi về tài chính hoặc sức khỏe đều dược"
                                            ),
                                    isUser = false
                            )
                    )
            )
    val messages = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val history = mutableListOf<Content>()

    fun addLocalMessage(content: MessageContent, isUser: Boolean = false) {
        _messages.value = _messages.value + ChatMessage(content = content, isUser = isUser)
    }

    fun removeLatestLocalMessage() {
        if (_messages.value.isNotEmpty()) {
            _messages.value = _messages.value.dropLast(1)
        }
    }

    fun updateMessage(id: String, newContent: MessageContent) {
        _messages.value =
                _messages.value.map { if (it.id == id) it.copy(content = newContent) else it }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        addLocalMessage(MessageContent.Text(text), true)

        // ── Local fast path: try multi-tx parse first ──────────
        val multiTxs = LocalAIEngine.parseMultiTransaction(text)
        if (multiTxs.size >= 2) {
            multiTxs.forEach { parsed ->
                addLocalMessage(MessageContent.TransactionCard(parsed), false)
            }
            return  // No API needed
        }

        // ── Local Q&A: answer simple queries without API ────────
        val localAnswer = LocalAIEngine.tryAnswerLocally(
            userText = text,
            wallet = wallet,
            transactions = transactions,
            budgets = budgets,
            savingsGoals = savingsGoals,
            debtLoans = debtLoans,
            schedule = schedule,
            habit = _userHabit.value
        )
        if (localAnswer.handled) {
            addLocalMessage(MessageContent.Text(localAnswer.text), false)
            return  // No API needed
        }

        // ── Fallback: call Gemini API ───────────────────────────
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val reply = processMessage(text)
                if (reply.isNotBlank() && reply != "SILENT_CONFIRM_UI") {
                    addLocalMessage(MessageContent.Text(reply), false)
                }
            } catch (e: QuotaExceededException) {
                addLocalMessage(
                    MessageContent.Text(
                        "⚠️ Trợ lý đang bận, vui lòng thử lại sau vài phút. (Đã đạt GH API)"
                    ),
                    false
                )
            } catch (e: Exception) {
                addLocalMessage(MessageContent.Text("Lỗi kết nối: ${e.message}"), false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ─── Local AI helpers (no API) ────────────────────────────────────────────

    /** Trả về danh sách chip gợi ý dựa trên giờ + lịch sử danh mục */
    fun getSmartSuggestions(): List<LocalAIEngine.QuickSuggestion> {
        val recentCats = transactions
            .sortedByDescending { it.timestamp.seconds }
            .take(10)
            .map { it.category }
            .distinct()
        return LocalAIEngine.getTimeSuggestions(recentCategories = recentCats)
    }

    /** Kiểm tra ngân sách sắp vượt/đã vượt và push proactive message */
    fun checkBudgetAlerts() {
        val alerts = LocalAIEngine.getBudgetAlerts(transactions, budgets, threshold = 0.85f)
        if (alerts.isEmpty()) return
        val lines = alerts.joinToString("\n") { alert ->
            val icon = if (alert.isExceeded) "🔴" else "🟡"
            val status = if (alert.isExceeded) "ĐÃ vượt" else "sắp đạt"
            "$icon **${alert.category}** $status hạn mức: ${formatCurrencyLocal(alert.spent)}/${formatCurrencyLocal(alert.limit)}"
        }
        addLocalMessage(
            MessageContent.Text("🚨 **Cảnh báo ngân sách tháng này:**\n$lines"),
            isUser = false
        )
    }

    /** Trả về insight nhanh (không cần API) */
    fun getLocalInsight(): String {
        val insight = LocalAIEngine.getSpendingInsight(transactions)
        val trendIcon = if (insight.comparedToLastWeek > 0) "⬆️" else "⬇️"
        val trendPct = String.format("%.1f", kotlin.math.abs(insight.comparedToLastWeek))
        return "📈 **Tổng quan nhanh:**\n" +
            "Hôm nay: ${formatCurrencyLocal(insight.todayTotal)}\n" +
            "Tuần này: ${formatCurrencyLocal(insight.weekTotal)} ($trendIcon${trendPct}% so với tuần trước)\n" +
            "Tháng này: ${formatCurrencyLocal(insight.monthTotal)}\n" +
            "Danh mục nhiều nhất: ${insight.topCategory} (${formatCurrencyLocal(insight.topCategoryAmount)})"
    }

    private fun formatCurrencyLocal(amount: Double): String = when {
        amount >= 1_000_000 -> "${String.format("%.1f", amount / 1_000_000)}tr"
        amount >= 1_000     -> "${(amount / 1_000).toInt()}k"
        else                -> "${amount.toInt()}đ"
    }

    /** Kiểm tra Thứ Hai chủ động */
    fun checkMondayProactive() {
        viewModelScope.launch {
            val now = java.util.Calendar.getInstance()
            // 2: Thứ 2 (trong java.util.Calendar)
            if (now.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.MONDAY) {
                val weekId =
                        "${now.get(java.util.Calendar.YEAR)}-${now.get(java.util.Calendar.WEEK_OF_YEAR)}"
                if (_userHabit.value.lastProactiveWeek != weekId) {
                    // Ghi nhận đã hỏi tuần này
                    val updatedHabit = _userHabit.value.copy(lastProactiveWeek = weekId)
                    _userHabit.value = updatedHabit
                    firestoreRepository.saveUserHabit(userId, updatedHabit)

                    // Gửi tin nhắn chủ động
                    addLocalMessage(
                            MessageContent.Text(
                                    "Chào buổi sáng Thứ Hai! 👋 Tuần mới bắt đầu rồi, bạn có kế hoạch đặc biệt nào cần dùng tiền không? Để tôi giúp bạn lên kế hoạch chi tiêu thông minh nhé!"
                            ),
                            false
                    )
                }
            }
        }
    }

    private suspend fun processMessage(userText: String): String {
        history.add(content("user") { text(userText) })

        val sysInstruct = buildSystemContext()
        val response = geminiService.getCompletion(sysInstruct, history)

        if (response.functionCalls.isNotEmpty()) {

            // Bỏ đi các item trong history tạm thời của lệnh function call này để tránh lỗi chuỗi
            if (history.isNotEmpty()) {
                history.removeLast() // pop userText
            }

            // Xử lý song song tất cả các lệnh mà bản model yêu cầu (Parallel Function Calling)
            response.functionCalls.forEach { funcCall ->
                executeFunctionCall(funcCall.name, funcCall.args)
            }

            // Bỏ qua bước gọi API lần 2 để báo cáo kết quả func.
            // Thay vào đó chỉ trả về mốc SILENT để show Card.
            return "SILENT_CONFIRM_UI"
        } else {
            val text = response.text ?: "..."
            history.add(content("model") { text(text) })
            return text
        }
    }

    private fun executeFunctionCall(name: String, args: Map<String, Any?>): String {
        return when (name) {
            "addTransaction" -> {
                val amount =
                        (args["amount"] as? Number)?.toDouble()
                                ?: (args["amount"] as? String)?.toDoubleOrNull() ?: 0.0
                val category = args["category"]?.toString() ?: "Khác"
                val note = args["note"]?.toString() ?: ""
                val type = args["type"]?.toString() ?: "EXPENSE"
                val walletSource = args["walletSource"]?.toString()

                var accountId: String? = null
                if (!walletSource.isNullOrBlank() && wallet != null) {
                    val searchLower = walletSource.lowercase()
                    val acc =
                            wallet?.accounts?.find {
                                it.name.lowercase().contains(searchLower) ||
                                        it.bankCode.lowercase().contains(searchLower)
                            }
                    accountId = acc?.id
                }

                val tx = ParsedTransaction(amount, category, note, type, 1.0f, accountId)
                addLocalMessage(MessageContent.TransactionCard(tx, false))
                "Đã hiển thị thẻ thu thập giao dịch để chờ người dùng bấm xác nhận."
            }
            "addDebtLoan" -> {
                val typeStr = args["type"]?.toString() ?: "DEBT"
                val type = if (typeStr == "LOAN") DebtLoanType.LOAN else DebtLoanType.DEBT
                val debt =
                        DebtLoan(
                                id = UUID.randomUUID().toString(),
                                personName = args["personName"]?.toString() ?: "Không tên",
                                amount = (args["amount"] as? Number)?.toDouble()
                                                ?: (args["amount"] as? String)?.toDoubleOrNull()
                                                        ?: 0.0,
                                type = type,
                                note = args["note"]?.toString() ?: ""
                        )
                addLocalMessage(MessageContent.DebtCard(debt, false))
                "Đơn nợ đã được show UI card để đợi user xác nhận."
            }
            "addGroupSplitBill" -> {
                val totalAmount =
                        (args["totalAmount"] as? Number)?.toDouble()
                                ?: (args["totalAmount"] as? String)?.toDoubleOrNull() ?: 0.0
                val participantCount =
                        (args["participantCount"] as? Number)?.toInt()
                                ?: (args["participantCount"] as? String)?.toIntOrNull() ?: 1
                val category = args["category"]?.toString() ?: "Ăn uống"
                val note = args["note"]?.toString() ?: ""
                val names = args["participants"] as? List<String> ?: emptyList()

                val sharePerPerson =
                        if (participantCount > 0) totalAmount / participantCount else 0.0
                val participantList =
                        if (names.isNotEmpty()) {
                            names.map {
                                TransactionParticipant(name = it, shareAmount = sharePerPerson)
                            }
                        } else {
                            List(participantCount.coerceAtLeast(1) - 1) {
                                TransactionParticipant(
                                        name = "Người ${it + 1}",
                                        shareAmount = sharePerPerson
                                )
                            }
                        }

                addLocalMessage(
                        MessageContent.SplitBillCard(
                                totalAmount,
                                participantCount,
                                category,
                                note,
                                false,
                                participantList
                        )
                )
                "Đã hiển thị thẻ thu thập Split Bill để chờ người dùng bấm xác nhận."
            }
            "addSavingsGoal" -> {
                val autoSavingAmount =
                        (args["autoSavingAmount"] as? Number)?.toDouble()
                                ?: (args["autoSavingAmount"] as? String)?.toDoubleOrNull() ?: 0.0
                val targetDateStr = args["targetDate"]?.toString()

                var timestamp: com.google.firebase.Timestamp? = null
                if (!targetDateStr.isNullOrBlank()) {
                    try {
                        val format =
                                java.text.SimpleDateFormat(
                                        "yyyy-MM-dd",
                                        java.util.Locale.getDefault()
                                )
                        val date = format.parse(targetDateStr)
                        if (date != null) {
                            timestamp = com.google.firebase.Timestamp(date)
                        }
                    } catch (e: Exception) {}
                }

                val goal =
                        SavingsGoal(
                                id = UUID.randomUUID().toString(),
                                goalName = args["name"]?.toString() ?: "Mục tiêu",
                                targetAmount = (args["targetAmount"] as? Number)?.toDouble()
                                                ?: (args["targetAmount"] as? String)
                                                        ?.toDoubleOrNull()
                                                        ?: 0.0,
                                autoSavingAmount = autoSavingAmount,
                                targetDate = timestamp
                        )
                addLocalMessage(MessageContent.SavingsCard(goal, false))
                "Mục tiêu tiết kiệm đã được hiện card lên. Vui lòng nhắn thông báo bạn đang đợi người dùng ấn Xác nhận ở card để bạn ghi nhận lên Firebase."
            }
            "addBudget" -> {
                val periodStr = args["period"]?.toString() ?: "MONTHLY"
                val period =
                        if (periodStr == "WEEKLY") BudgetPeriod.WEEKLY else BudgetPeriod.MONTHLY
                val budget =
                        FinanceBudget(
                                id = UUID.randomUUID().toString(),
                                amount = (args["amount"] as? Number)?.toDouble()
                                                ?: (args["amount"] as? String)?.toDoubleOrNull()
                                                        ?: 0.0,
                                category = args["category"]?.toString() ?: "Tất cả",
                                period = period
                        )
                addLocalMessage(MessageContent.BudgetCard(budget, false))
                "Hạn mức ngân sách đã hiển thị lên chờ xác nhận."
            }
            "addAutoSchedule" -> {
                val item =
                        SpendingScheduleItem(
                                id = UUID.randomUUID().toString(),
                                dayOfWeek = (args["dayOfWeek"] as? Number)?.toInt()
                                                ?: (args["dayOfWeek"] as? String)?.toIntOrNull()
                                                        ?: 1,
                                amount = (args["amount"] as? Number)?.toDouble()
                                                ?: (args["amount"] as? String)?.toDoubleOrNull()
                                                        ?: 0.0,
                                category = args["category"]?.toString() ?: "Ăn uống",
                                note = args["note"]?.toString() ?: "",
                                isAutoApply = true
                        )
                addLocalMessage(MessageContent.ScheduleCard(item, false))
                "Lịch trình đã hiển thị lên chờ xác nhận."
            }
            "addHeldFund" -> {
                val fundName = args["fundName"]?.toString() ?: "Quỹ chung"
                val amount =
                        (args["amount"] as? Number)?.toDouble()
                                ?: (args["amount"] as? String)?.toDoubleOrNull() ?: 0.0
                addLocalMessage(MessageContent.HeldFundCard(fundName, amount, false))
                "Quỹ giữ hộ đã được hiển thị lên thẻ xác nhận."
            }
            "updateUserHabit" -> {
                val min =
                        (args["minMealCost"] as? Number)?.toDouble() ?: _userHabit.value.minMealCost
                val max =
                        (args["maxMealCost"] as? Number)?.toDouble() ?: _userHabit.value.maxMealCost
                val routineNotes = args["routineNotes"]?.toString() ?: ""
                val generalNotes =
                        _userHabit.value.generalNotes +
                                (if (routineNotes.isNotBlank()) "\n- $routineNotes" else "")

                val newHabit =
                        _userHabit.value.copy(
                                minMealCost = min,
                                maxMealCost = max,
                                generalNotes = generalNotes
                        )
                addLocalMessage(MessageContent.HabitUpdateCard(newHabit, false))
                "Đã hiển thị thẻ cập nhật thói quen để chờ người dùng xác nhận."
            }
            "proposeWeeklyPlan" -> {
                val desc = args["planDescription"]?.toString() ?: "Kế hoạch tuần mới"
                val itemsJson = args["itemsJson"]?.toString() ?: "[]"
                val items = mutableListOf<SpendingScheduleItem>()
                try {
                    val array = org.json.JSONArray(itemsJson)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        items.add(
                                SpendingScheduleItem(
                                        id = UUID.randomUUID().toString(),
                                        dayOfWeek = obj.optInt("dayOfWeek", 1),
                                        amount = obj.optDouble("amount", 0.0),
                                        category = obj.optString("category", "Ăn uống"),
                                        note = obj.optString("note", "")
                                )
                        )
                    }
                } catch (e: Exception) {}

                addLocalMessage(MessageContent.WeeklyPlanCard(desc, items, false))
                "Đã đề xuất kế hoạch tuần mới để chờ người dùng xác nhận."
            }
            "depositSavings" -> {
                val goalName = args["goalName"]?.toString() ?: "Mục tiêu"
                val amount =
                        (args["amount"] as? Number)?.toDouble()
                                ?: (args["amount"] as? String)?.toDoubleOrNull() ?: 0.0
                val walletSource = args["walletSource"]?.toString()

                addLocalMessage(
                        MessageContent.DepositSavingsCard(goalName, amount, walletSource, false)
                )
                "Đã hiển thị thẻ nạp tiền tiết kiệm để chờ người dùng xác nhận."
            }
            "withdrawSavings" -> {
                val goalName = args["goalName"]?.toString() ?: "Mục tiêu"
                val amount =
                        (args["amount"] as? Number)?.toDouble()
                                ?: (args["amount"] as? String)?.toDoubleOrNull() ?: 0.0
                val destinationWallet = args["destinationWallet"]?.toString()
                val transferToSavingsGoal = args["transferToSavingsGoal"]?.toString()

                addLocalMessage(
                        MessageContent.WithdrawSavingsCard(
                                goalName,
                                amount,
                                destinationWallet,
                                transferToSavingsGoal,
                                false
                        )
                )
                "Đã hiển thị thẻ rút tiền tiết kiệm để chờ người dùng xác nhận."
            }
            else -> "Hàm không được hỗ trợ"
        }
    }

    fun confirmTransaction(
            transaction: FinanceTransaction,
            updatedWallet: AppUserWallet,
            msgId: String,
            parsedTx: ParsedTransaction
    ) {
        viewModelScope.launch {
            firestoreRepository.saveUserWallet(updatedWallet)
            firestoreRepository.addTransaction(userId, transaction)
            updateMessage(msgId, MessageContent.TransactionCard(parsedTx, true))
            addLocalMessage(MessageContent.Text("✅ Đã ghi giao dịch thành công!"))
        }
    }

    fun confirmBill(
            transaction: FinanceTransaction,
            updatedWallet: AppUserWallet,
            msgId: String,
            imageUri: Uri,
            amt: Double?
    ) {
        viewModelScope.launch {
            firestoreRepository.saveUserWallet(updatedWallet)
            firestoreRepository.addTransaction(userId, transaction)
            updateMessage(msgId, MessageContent.BillCard(imageUri, amt, true))
            addLocalMessage(MessageContent.Text("✅ Đã ghi nhận hoá đơn!"))
        }
    }

    fun confirmDebtLoan(debtLoan: DebtLoan, msgId: String) {
        viewModelScope.launch {
            firestoreRepository.saveDebtLoan(userId, debtLoan)
            updateMessage(msgId, MessageContent.DebtCard(debtLoan, true))
            addLocalMessage(MessageContent.Text("✅ Đã ghi nhận lịch sử vay/nợ vào hệ thống!"))
        }
    }

    fun confirmSavingsGoal(goal: SavingsGoal, msgId: String) {
        viewModelScope.launch {
            firestoreRepository.saveSavingsGoal(userId, goal)
            updateMessage(msgId, MessageContent.SavingsCard(goal, true))
            addLocalMessage(MessageContent.Text("✅ Đã thiết lập mục tiêu tiết kiệm mới!"))
        }
    }

    fun confirmBudget(budget: FinanceBudget, msgId: String) {
        viewModelScope.launch {
            firestoreRepository.saveBudget(userId, budget)
            updateMessage(msgId, MessageContent.BudgetCard(budget, true))
            addLocalMessage(MessageContent.Text("✅ Đã chốt hạn mức ngân sách!"))
        }
    }

    fun confirmSchedule(item: SpendingScheduleItem, msgId: String) {
        viewModelScope.launch {
            firestoreRepository.saveSpendingSchedule(userId, item)
            updateMessage(msgId, MessageContent.ScheduleCard(item, true))
            addLocalMessage(
                    MessageContent.Text("✅ Lịch trình tự động đã được lưu và sẽ chạy hàng tuần!")
            )
        }
    }

    fun confirmHeldFund(fundName: String, amount: Double, msgId: String) {
        viewModelScope.launch {
            wallet?.let { w ->
                val newFund =
                        HeldFundItem(
                                id = UUID.randomUUID().toString(),
                                name = fundName,
                                amount = amount
                        )
                val updatedWallet = w.copy(heldFunds = w.heldFunds + newFund)
                firestoreRepository.saveUserWallet(updatedWallet)
            }
            updateMessage(msgId, MessageContent.HeldFundCard(fundName, amount, true))
            addLocalMessage(
                    MessageContent.Text("✅ Đã thiết lập quỹ giữ hộ '$fundName' thành công!")
            )
        }
    }

    fun confirmSplitBill(
            totalAmount: Double,
            participants: List<TransactionParticipant>,
            category: String,
            note: String,
            msgId: String
    ) {
        viewModelScope.launch {
            val personalShare = totalAmount / (participants.size + 1)
            val totalGroupAmount = participants.sumOf { it.shareAmount }
            val groupOwes =
                    participants.filter { !it.isPaid }.sumOf { it.shareAmount - it.paidAmount }

            val tx =
                    FinanceTransaction(
                            id = UUID.randomUUID().toString(),
                            amount = personalShare,
                            type = TransactionType.EXPENSE,
                            category = category,
                            note = "Split Bill: $note",
                            isGroupPrepayment = true,
                            personalAmount = personalShare,
                            groupAmount = totalGroupAmount,
                            participantCount = participants.size + 1,
                            participants = participants,
                            timestamp = com.google.firebase.Timestamp.now()
                    )

            // Lưu các khoản nợ chưa trả vào DebtLoan để dễ theo dõi
            participants.forEach { p ->
                if (!p.isPaid || p.paidAmount < p.shareAmount) {
                    val remaining = p.shareAmount - p.paidAmount
                    if (remaining > 0) {
                        val debt =
                                DebtLoan(
                                        id = UUID.randomUUID().toString(),
                                        personName = p.name,
                                        amount = remaining,
                                        type = DebtLoanType.LOAN,
                                        note = "Từ Bill: $note",
                                        createdAt = com.google.firebase.Timestamp.now()
                                )
                        firestoreRepository.saveDebtLoan(userId, debt)
                    }
                }
            }

            wallet?.let { w ->
                val updatedAccounts = w.accounts.toMutableList()
                if (updatedAccounts.isNotEmpty()) {
                    var remainingToDeduct = totalAmount
                    for (i in updatedAccounts.indices) {
                        val acc = updatedAccounts[i]
                        if (acc.amount >= remainingToDeduct) {
                            updatedAccounts[i] = acc.copy(amount = acc.amount - remainingToDeduct)
                            remainingToDeduct = 0.0
                            break
                        } else {
                            remainingToDeduct -= acc.amount
                            updatedAccounts[i] = acc.copy(amount = 0.0)
                        }
                    }
                }

                // Thu nợ ngay lập tức cho những người đã trả
                val immediateRepayments = participants.sumOf { it.paidAmount }
                if (updatedAccounts.isNotEmpty()) {
                    // Cộng tiền trả vào tài khoản đầu tiên (giả định)
                    updatedAccounts[0] =
                            updatedAccounts[0].copy(
                                    amount = updatedAccounts[0].amount + immediateRepayments
                            )
                }

                val updatedWallet =
                        w.copy(
                                accounts = updatedAccounts,
                                groupPrepaidAmount = w.groupPrepaidAmount + groupOwes
                        )
                firestoreRepository.saveUserWallet(updatedWallet)
            }

            firestoreRepository.addTransaction(userId, tx)
            updateMessage(
                    msgId,
                    MessageContent.SplitBillCard(
                            totalAmount,
                            participants.size + 1,
                            category,
                            note,
                            true
                    )
            )

            val shareStr = com.example.finfit.finance.ui.utils.formatCurrency(groupOwes)
            if (groupOwes > 0) {
                addLocalMessage(
                        MessageContent.Text(
                                "✅ Đã chia bill thành công! Nhóm còn nợ bạn $shareStr. Các khoản nợ đã được ghi vào mục Ghi nợ."
                        )
                )
            } else {
                addLocalMessage(
                        MessageContent.Text("✅ Đã chia bill và ghi nhận mọi người đã trả đủ!")
                )
            }
        }
    }

    fun confirmUserHabit(habit: UserHabit, msgId: String) {
        viewModelScope.launch {
            firestoreRepository.saveUserHabit(userId, habit)
            _userHabit.value = habit
            updateMessage(msgId, MessageContent.HabitUpdateCard(habit, true))
            addLocalMessage(
                    MessageContent.Text(
                            "✅ Đã ghi nhớ thói quen của bạn! Tôi sẽ dùng nó để lên kế hoạch tốt hơn."
                    )
            )
        }
    }

    fun confirmWeeklyPlan(items: List<SpendingScheduleItem>, msgId: String, desc: String) {
        viewModelScope.launch {
            items.forEach { firestoreRepository.saveWeeklyScheduleItem(userId, it) }
            updateMessage(msgId, MessageContent.WeeklyPlanCard(desc, items, true))
            addLocalMessage(
                    MessageContent.Text(
                            "✅ Tuyệt vời! Tôi đã cập nhật toàn bộ kế hoạch vào Lịch trình chi tiêu của bạn."
                    )
            )
        }
    }

    fun confirmDepositSavings(
            goalName: String,
            amount: Double,
            sourceAccountId: String?,
            msgId: String
    ) {
        viewModelScope.launch {
            val goal = savingsGoals.find { it.goalName == goalName }
            if (goal != null) {
                val updatedGoal = goal.copy(currentAmount = goal.currentAmount + amount)
                firestoreRepository.saveSavingsGoal(userId, updatedGoal)
            }

            wallet?.let { w ->
                val sourceAcc = w.accounts.find { it.id == sourceAccountId }
                val updatedAccounts =
                        w.accounts.map {
                            if (it.id == sourceAccountId) it.copy(amount = it.amount - amount)
                            else it
                        }

                val updatedWallet = w.copy(accounts = updatedAccounts)
                firestoreRepository.saveUserWallet(updatedWallet)

                val tx =
                        FinanceTransaction(
                                id = UUID.randomUUID().toString(),
                                amount = amount,
                                type = TransactionType.TRANSFER,
                                category = "Tiết kiệm",
                                note = "Nạp tiền vào quỹ $goalName",
                                accountId = sourceAccountId,
                                timestamp = com.google.firebase.Timestamp.now()
                        )
                firestoreRepository.addTransaction(userId, tx)
            }

            updateMessage(
                    msgId,
                    MessageContent.DepositSavingsCard(
                            goalName,
                            amount,
                            wallet?.accounts?.find { it.id == sourceAccountId }?.name,
                            true
                    )
            )
            addLocalMessage(
                    MessageContent.Text(
                            "✅ Đã nạp thành công ${com.example.finfit.finance.ui.utils.formatCurrency(amount)} vào quỹ $goalName!"
                    )
            )
        }
    }

    fun confirmWithdrawSavings(
            goalName: String,
            amount: Double,
            destAccountId: String?,
            destGoalId: String?,
            msgId: String
    ) {
        viewModelScope.launch {
            val goal = savingsGoals.find { it.goalName == goalName }
            if (goal != null) {
                val updatedGoal = goal.copy(currentAmount = goal.currentAmount - amount)
                firestoreRepository.saveSavingsGoal(userId, updatedGoal)
            }

            if (destGoalId != null) {
                val destGoal = savingsGoals.find { it.id == destGoalId }
                if (destGoal != null) {
                    val updatedDestGoal =
                            destGoal.copy(currentAmount = destGoal.currentAmount + amount)
                    firestoreRepository.saveSavingsGoal(userId, updatedDestGoal)
                }
                val tx =
                        FinanceTransaction(
                                id = UUID.randomUUID().toString(),
                                amount = amount,
                                type = TransactionType.TRANSFER,
                                category = "Tiết kiệm",
                                note =
                                        "Chuyển tiền từ $goalName sang ${savingsGoals.find { it.id == destGoalId }?.goalName}",
                                timestamp = com.google.firebase.Timestamp.now()
                        )
                firestoreRepository.addTransaction(userId, tx)
            } else if (destAccountId != null) {
                wallet?.let { w ->
                    val updatedAccounts =
                            w.accounts.map {
                                if (it.id == destAccountId) it.copy(amount = it.amount + amount)
                                else it
                            }
                    val updatedWallet = w.copy(accounts = updatedAccounts)
                    firestoreRepository.saveUserWallet(updatedWallet)

                    val tx =
                            FinanceTransaction(
                                    id = UUID.randomUUID().toString(),
                                    amount = amount,
                                    type = TransactionType.INCOME,
                                    category = "Tiết kiệm",
                                    note = "Rút tiền từ quỹ $goalName về ví",
                                    accountId = destAccountId,
                                    timestamp = com.google.firebase.Timestamp.now()
                            )
                    firestoreRepository.addTransaction(userId, tx)
                }
            }

            updateMessage(
                    msgId,
                    MessageContent.WithdrawSavingsCard(
                            goalName,
                            amount,
                            wallet?.accounts?.find { it.id == destAccountId }?.name,
                            savingsGoals.find { it.id == destGoalId }?.goalName,
                            true
                    )
            )
            addLocalMessage(
                    MessageContent.Text("✅ Đã xử lý lệnh rút/chuyển tiền từ quỹ $goalName!")
            )
        }
    }

    private fun buildSystemContext(): String {
        val totalBal = wallet?.totalBalance ?: 0.0
        val debtsStr =
                debtLoans
                        .joinToString(", ") {
                            "${it.personName} (${if (it.type == DebtLoanType.DEBT) "mình nợ" else "nợ mình"} ${it.amount})"
                        }
                        .ifBlank { "Không có khoản nợ nào." }

        val goalsStr =
                savingsGoals
                        .joinToString(", ") {
                            "${it.goalName} (${it.currentAmount}/${it.targetAmount})"
                        }
                        .ifBlank { "Không có mục tiêu nào." }

        val habit = _userHabit.value
        val habitContext =
                "THÓI QUEN NGƯỜI DÙNG:\n" +
                        "- Giá bữa ăn: ${habit.minMealCost} - ${habit.maxMealCost} đ\n" +
                        "- Ghi chú: ${habit.generalNotes}\n" +
                        "- Cố định: ${habit.fixedCosts.joinToString { "Thứ ${it.dayOfWeek}: ${it.amount} (${it.category})" }}\n"

        val currentDateStr =
                java.text.SimpleDateFormat("EEEE, dd/MM/yyyy HH:mm", java.util.Locale("vi", "VN"))
                        .format(java.util.Date())

        return """
Bạn là Trợ lý AI của ứng dụng FinFit (Quản lý Tài chính & Sức khoẻ). Giao tiếp thân thiện bằng tiếng Việt.
- Thời gian hệ thống: $currentDateStr
- Số dư: $totalBal đ | Nợ: $debtsStr | Mục tiêu: $goalsStr
$habitContext

QUY TẮC GỌI TOOL & XỬ LÝ DỮ LIỆU (BẮT BUỘC TUÂN THỦ):

[NHÓM 1: ĐỊNH DẠNG TIỀN TỆ & TỪ LÓNG]
1. Quy đổi từ lóng: 'k'/'cành' = 1.000đ; 'lít'/'lốp' = 100.000đ; 'củ'/'tr'/'chai' = 1.000.000đ.
2. Viết tắt thập phân: 'nửa củ' = 500000; '1 củ rưỡi' = 1500000; 'trăm rưỡi' = 150000.
3. Rút gọn đơn vị: NẾU người dùng nói 'ăn sáng 30', 'đổ xăng 50' -> ngầm hiểu là 30.000đ, 50.000đ.
4. Chống nhầm số thứ tự: 'Thứ 2, Thứ 3, ..., Chủ nhật' là ngày. TUYỆT ĐỐI KHÔNG gộp vào số tiền.
5. Số âm: NẾU báo 'bị trừ 50k', đó là CHI (Expense) dương 50000, không truyền số âm vào tool.

[NHÓM 2: THỜI GIAN & LỊCH TRÌNH]
6. Thời gian tương đối: Tự động lùi ngày dựa vào Thời gian hệ thống nếu gặp từ 'hôm qua', 'hôm kia', 'sáng nay'.
7. Tương lai: 'Ngày mai', 'tuần sau' -> Dùng tool 'proposeWeeklyPlan' hoặc 'updateUserHabit', KHÔNG tạo giao dịch (addTransaction).
8. Lịch trình lặp lại: 'Mỗi sáng', 'thứ 2 hàng tuần' -> Ghi nhận vào Thói quen (Habit), không phải giao dịch đơn lẻ.
9. Xử lý hàng loạt: NẾU có nhiều nội dung (VD: sáng ăn 30k, chiều chạy 5km) -> Gọi đồng thời tất cả các tool tương ứng.

[NHÓM 3: GIAO DỊCH THU / CHI CƠ BẢN]
10. Phân biệt Thu/Chi: 'Nhận', 'lương', 'bán', 'được cho' = THU. 'Mua', 'đóng', 'phạt', 'mất' = CHI.
11. Đa mục đích: 'Đi siêu thị và gửi xe 200k' -> Gom vào 1 giao dịch (Sinh hoạt) và ghi chú chi tiết.
12. Phí giao dịch: 'Chuyển khoản phí 2k' -> Gộp luôn vào giao dịch chính hoặc ghi mục 'Phí'.
13. Hoàn tiền (Refund): 'Được trả lại 100k tiền thừa' -> Ghi nhận là THU.
14. Thiếu danh mục: NẾU không rõ mua gì ('nay tiêu 500k') -> Gán vào danh mục 'Khác'.
15. Không liên quan ví: 'Được tặng cái áo' -> Chỉ là câu chuyện, không tạo giao dịch.

[NHÓM 4: CHIA TIỀN, NỢ NẦN & THANH TOÁN HỘ]
16. Trả nợ toàn bộ: 'Đã trả nợ X' (không báo số tiền) -> Lấy toàn bộ số tiền X trong 'Số nợ tồn'.
17. Trả nợ một phần: 'Trả X 200k' -> Lấy đúng 200k tạo giao dịch trả nợ.
18. Thanh toán hộ (Split bill): 'Mua 2 vé phim 200k, Nam nợ mình 100k' -> Tạo CHI 100k (mình xem) + Tạo NỢ 100k (Nam nợ mình).
19. Campuchia (Chia đều): 'Đi ăn hết 500k chia 5 người' -> Chỉ tạo giao dịch CHI 100k của mình.
20. Cấn trừ nợ bằng hiện vật: 'X trả nợ bằng ly cafe' -> Ghi nhận bằng cách tạo giao dịch THU nhỏ ghi chú 'X trả nợ bằng hiện vật', hướng dẫn người dùng vào Ghi nợ để cập nhật thủ công.

[NHÓM 5: SỨC KHOẺ & TẬP LUYỆN (FINFIT ĐẶC THÙ)]
21. Ăn uống & Calories: Khi ghi nhận CHI ăn uống (phở, bún...) -> ghi bình thường vào addTransaction danh mục 'Ăn uống'.
22. Ghi nhận vận động: 'Chạy 5km', 'Gym 1 tiếng' -> Trả lời động viên, hướng dẫn vào tab Sức khoẻ để ghi nhận (chưa hỗ trợ qua chat).
23. Combo Tài chính - Sức khoẻ: 'Thuê sân cầu lông 100k rồi đánh 2 tiếng' -> Chỉ tạo CHI 100k (Thể thao), phần vận động hướng dẫn ghi tay.
24. Y tế: 'Mua thuốc 100k' -> Ghi CHI (Y tế).

[NHÓM 6: MỤC TIÊU TIẾT KIỆM (SAVINGS)]
25. Cất tiền: 'Bỏ lợn 100k', 'chuyển vào quỹ mua xe' -> Dùng tool 'depositSavings' để trích tiền từ ví vào tiết kiệm.
26. Rút tiền mục tiêu: 'Đập lợn mua điện thoại' -> Dùng tool 'withdrawSavings' để lấy tiền từ quỹ ra ví, hoặc luân chuyển qua quỹ khác.

[NHÓM 7: ĐÍNH CHÍNH & TRUY VẤN]
27. Đính chính / Sửa lỗi: 'À nhầm, 40k mới đúng' -> Thông báo cho người dùng vào Lịch sử giao dịch để sửa thủ công (chưa hỗ trợ tự động sửa qua chat).
28. Undo: 'Xoá giao dịch vừa nãy đi' -> Hướng dẫn người dùng vào tab Lịch sử giao dịch để xoá thủ công.
29. Truy vấn: 'Tháng này tiêu bao nhiêu rồi?' -> Dựa trên dữ liệu tôi có trong context (Số dư, Nợ, Mục tiêu) để trả lời, tuyệt đối không tạo giao dịch.
30. Ngoài luồng/Giả định: 'Nếu tôi có 1 tỷ', hoặc hỏi thời tiết, bài tập -> Trả lời bình thường, KHÔNG gọi tool giả mạo.
    """.trimIndent()
    }
}

class AssistantViewModelFactory(
        private val firestoreRepository: FirestoreRepository,
        private val userId: String,
        private val wallet: AppUserWallet?,
        private val transactions: List<FinanceTransaction>,
        private val schedule: List<SpendingScheduleItem>,
        private val debtLoans: List<DebtLoan>,
        private val savingsGoals: List<SavingsGoal>,
        private val budgets: List<FinanceBudget>,
        private val stepsToday: Int,
        private val habit: UserHabit? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AssistantViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AssistantViewModel(
                    firestoreRepository,
                    GeminiService(),
                    userId,
                    wallet,
                    transactions,
                    schedule,
                    debtLoans,
                    savingsGoals,
                    budgets,
                    stepsToday,
                    habit
            ) as
                    T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
