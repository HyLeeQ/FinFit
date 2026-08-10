package com.example.finfit.ui.assistant

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.finfit.data.remote.GeminiService
import com.example.finfit.data.remote.QuotaExceededException
import com.example.finfit.finance.model.*
import com.example.finfit.finance.repository.FirestoreRepository
import com.example.finfit.finance.ui.logic.BudgetLogic
import com.example.finfit.finance.ui.utils.formatCurrency
import com.example.finfit.finance.util.FinancialHealthCalculator
import com.example.finfit.finance.util.LocalAIEngine
import com.example.finfit.finance.util.ParsedTransaction
import com.example.finfit.finance.util.SmartTransactionParser
import com.example.finfit.health.model.HealthUiState
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
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
    var healthState: HealthUiState,
    private val initialHabit: UserHabit? = null
) : ViewModel() {

    private val _userHabit = MutableStateFlow(initialHabit ?: UserHabit())
    val userHabit = _userHabit.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                content = MessageContent.Text(
                    "Xin chào bạn! Mình là **Fitie** 👋 — Người bạn đồng hành tài chính và sức khỏe của bạn.\n\n" +
                    "Bạn có thể trò chuyện với mình như một người bạn:\n" +
                    "• *\"Nay ăn trưa 45k\"* hoặc *\"lương về 15tr\"* → Mình ghi chép và tính toán ngay.\n" +
                    "• *\"Tháng này mình tiêu có ổn không?\"* → Mình phân tích chi tiết dòng tiền & thói quen.\n" +
                    "• *\"Hết tiền rồi 😭\"* → Mình lắng nghe và tư vấn cách thắt chặt chi tiêu.\n" +
                    "• Hỏi về điểm sức khỏe tài chính, nợ nần, tiết kiệm hoặc chia bill đều được nè!"
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
        _messages.value = _messages.value.map { if (it.id == id) it.copy(content = newContent) else it }
    }

    /** Kiểm tra xem tin nhắn có chứa yếu tố cảm xúc, câu hỏi tư vấn hoặc phân tích không */
    private fun hasEmotionalOrAdvisoryIntent(text: String): Boolean {
        val lower = text.lowercase().trim()
        val advisoryKeywords = listOf(
            "hết tiền", "cháy túi", "sao", "tại sao", "thế nào", "như thế nào", "ổn không", "hợp lý không",
            "tư vấn", "lời khuyên", "buồn", "lo", "sợ", "vui", "sướng", "tiếc", "phí", "đắt", "rẻ",
            "tiết kiệm", "được không", "có nên", "làm sao", "giúp", "giải thích", "phân tích", "báo cáo",
            "tổng kết", "bao nhiêu", "đâu hết", "vơi", "vung tay", "nhiều quá", "ít quá", "😭", "😢", "😂", "🥳", "🥺", "💸", "🤦"
        )
        return advisoryKeywords.any { lower.contains(it) } || lower.endsWith("?")
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        addLocalMessage(MessageContent.Text(text), true)

        val isEmotionalOrAdvisory = hasEmotionalOrAdvisoryIntent(text)

        // Nếu là câu lệnh hành động THUẦN TÚY (không mang cảm xúc/câu hỏi), thử đi nhanh qua Local Path
        if (!isEmotionalOrAdvisory) {
            // 1. Multi-tx parse
            val multiTxs = LocalAIEngine.parseMultiTransaction(text)
            if (multiTxs.size >= 2) {
                addLocalMessage(MessageContent.Text("Mình đã bóc tách được các giao dịch của bạn rồi nè! 👇"), false)
                multiTxs.forEach { parsed ->
                    addLocalMessage(MessageContent.TransactionCard(parsed), false)
                }
                return
            }

            // 2. Debt parse
            val parsedDebt = SmartTransactionParser.parseDebt(text)
            if (parsedDebt != null) {
                val actionWord = if (parsedDebt.type == DebtLoanType.DEBT) "vay/mượn" else "cho vay"
                addLocalMessage(MessageContent.Text("Đã ghi nhận khoản $actionWord với **${parsedDebt.personName}** (${formatCurrency(parsedDebt.amount)}). Bạn kiểm tra lại thẻ bên dưới nhé!"), false)
                addLocalMessage(MessageContent.DebtCard(parsedDebt, false))
                return
            }

            // 3. Command parse (Deposit, Budget, Savings, etc)
            val parsedCommand = SmartTransactionParser.parseCommand(text)
            if (parsedCommand != null) {
                when (parsedCommand) {
                    is SmartTransactionParser.ParsedCommand.DepositSavings -> {
                        addLocalMessage(MessageContent.Text("Sẵn sàng nạp tiền vào quỹ **${parsedCommand.goalName}** (${formatCurrency(parsedCommand.amount)}) rồi nè! ✨"), false)
                        addLocalMessage(MessageContent.DepositSavingsCard(parsedCommand.goalName, parsedCommand.amount, null, false))
                    }
                    is SmartTransactionParser.ParsedCommand.WithdrawSavings -> {
                        addLocalMessage(MessageContent.Text("Đã tạo phiếu rút tiền từ quỹ **${parsedCommand.goalName}** (${formatCurrency(parsedCommand.amount)})."), false)
                        addLocalMessage(MessageContent.WithdrawSavingsCard(parsedCommand.goalName, parsedCommand.amount, null, null, false))
                    }
                    is SmartTransactionParser.ParsedCommand.AddSavingsGoal -> {
                        val goal = SavingsGoal(
                            goalName = parsedCommand.goalName,
                            targetAmount = parsedCommand.targetAmount
                        )
                        addLocalMessage(MessageContent.Text("Ý tưởng tuyệt vời! Đã lên mục tiêu tiết kiệm **${goal.goalName}** (${formatCurrency(goal.targetAmount)}) cho bạn 🎯"), false)
                        addLocalMessage(MessageContent.SavingsCard(goal, false))
                    }
                    is SmartTransactionParser.ParsedCommand.AddBudget -> {
                        val budget = FinanceBudget(
                            amount = parsedCommand.amount,
                            category = parsedCommand.category
                        )
                        addLocalMessage(MessageContent.Text("Đã thiết lập hạn mức ngân sách **${budget.category}** (${formatCurrency(budget.amount)}) để bạn kiểm soát chi tiêu tốt hơn! 🛡️"), false)
                        addLocalMessage(MessageContent.BudgetCard(budget, false))
                    }
                    is SmartTransactionParser.ParsedCommand.AddGroupSplitBill -> {
                        addLocalMessage(MessageContent.Text("Đã tạo thẻ chia bill ${formatCurrency(parsedCommand.amount)} cho ${parsedCommand.participantCount} người. Bạn xác nhận bên dưới nhé! 👥"), false)
                        addLocalMessage(MessageContent.SplitBillCard(
                            totalAmount = parsedCommand.amount,
                            participantCount = parsedCommand.participantCount,
                            category = parsedCommand.category,
                            note = "Ghi nhanh qua trợ lý",
                            initialParticipants = emptyList(),
                            confirmed = false
                        ))
                    }
                    is SmartTransactionParser.ParsedCommand.AddHeldFund -> {
                        addLocalMessage(MessageContent.Text("Đã tạo thẻ quỹ giữ hộ **${parsedCommand.fundName}** (${formatCurrency(parsedCommand.amount)})."), false)
                        addLocalMessage(MessageContent.HeldFundCard(parsedCommand.fundName, parsedCommand.amount, false))
                    }
                }
                return
            }

            // 4. Single tx parse (rất rõ ràng, ví dụ "ăn sáng 30k")
            val singleTx = SmartTransactionParser.parse(text)
            if (singleTx != null && text.trim().split(" ").size <= 5) {
                addLocalMessage(MessageContent.Text("Mình đã ghi nhận khoản **${singleTx.category}** (${formatCurrency(singleTx.amount)}) rồi nhé! 👇"), false)
                addLocalMessage(MessageContent.TransactionCard(singleTx), false)
                return
            }
        }

        // ── Luôn chuyển lên Gemini với Rich System Context cho các câu hỏi cảm xúc, phân tích hoặc câu phức tạp ──
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val reply = processMessage(text)
                if (reply.isNotBlank() && reply != "SILENT_CONFIRM_UI") {
                    addLocalMessage(MessageContent.Text(reply), false)
                }
            } catch (e: QuotaExceededException) {
                // Khi chạm quota, mới dùng local Q&A fallback
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
                } else {
                    addLocalMessage(
                        MessageContent.Text("⚠️ Fitie đang nhận được nhiều yêu cầu cùng lúc. Bạn thử hỏi lại sau ít phút nhé!"),
                        false
                    )
                }
            } catch (e: Exception) {
                addLocalMessage(MessageContent.Text("Lỗi kết nối: ${e.message}"), false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun processMessage(userText: String): String {
        history.add(content("user") { text(userText) })

        val sysInstruct = buildSystemContext()
        val response = geminiService.getCompletion(sysInstruct, history)

        if (response.functionCalls.isNotEmpty()) {
            if (history.isNotEmpty()) {
                history.removeLast() // pop userText for tool state
            }

            val textFeedbackList = mutableListOf<String>()
            response.functionCalls.forEach { funcCall ->
                val feedback = executeFunctionCall(funcCall.name, funcCall.args)
                if (feedback.isNotBlank()) {
                    textFeedbackList.add(feedback)
                }
            }

            return if (textFeedbackList.isNotEmpty()) {
                textFeedbackList.joinToString("\n\n")
            } else {
                "Mình đã chuẩn bị sẵn thẻ thông tin theo yêu cầu của bạn ở bên dưới nhé!"
            }
        } else {
            val text = response.text ?: "..."
            history.add(content("model") { text(text) })
            return text
        }
    }

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
            "$icon **${alert.category}** $status hạn mức: ${formatCurrency(alert.spent)}/${formatCurrency(alert.limit)}"
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
        val trendPct = String.format(Locale.US, "%.1f", kotlin.math.abs(insight.comparedToLastWeek))
        return "📈 **Tổng quan nhanh:**\n" +
            "Hôm nay: ${formatCurrency(insight.todayTotal)}\n" +
            "Tuần này: ${formatCurrency(insight.weekTotal)} ($trendIcon${trendPct}% so với tuần trước)\n" +
            "Tháng này: ${formatCurrency(insight.monthTotal)}\n" +
            "Danh mục nhiều nhất: ${insight.topCategory} (${formatCurrency(insight.topCategoryAmount)})"
    }

    /** Kiểm tra Thứ Hai chủ động */
    fun checkMondayProactive() {
        viewModelScope.launch {
            val now = Calendar.getInstance()
            if (now.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
                val weekId = "${now.get(Calendar.YEAR)}-${now.get(Calendar.WEEK_OF_YEAR)}"
                if (_userHabit.value.lastProactiveWeek != weekId) {
                    val updatedHabit = _userHabit.value.copy(lastProactiveWeek = weekId)
                    _userHabit.value = updatedHabit
                    firestoreRepository.saveUserHabit(userId, updatedHabit)

                    addLocalMessage(
                        MessageContent.Text(
                            "Chào buổi sáng Thứ Hai! 👋 Tuần mới bắt đầu rồi, bạn có kế hoạch chi tiêu nào cần Fitie hỗ trợ cân đối không nè?"
                        ),
                        false
                    )
                }
            }
        }
    }

    private fun executeFunctionCall(name: String, args: Map<String, Any?>): String {
        return when (name) {
            "addTransaction" -> {
                val amount = (args["amount"] as? Number)?.toDouble()
                    ?: (args["amount"] as? String)?.toDoubleOrNull() ?: 0.0
                val category = args["category"]?.toString() ?: "Khác"
                val note = args["note"]?.toString() ?: ""
                val type = args["type"]?.toString() ?: "EXPENSE"
                val walletSource = args["walletSource"]?.toString()

                var accountId: String? = null
                if (!walletSource.isNullOrBlank() && wallet != null) {
                    val searchLower = walletSource.lowercase()
                    val acc = wallet?.accounts?.find {
                        it.name.lowercase().contains(searchLower) || it.bankCode.lowercase().contains(searchLower)
                    }
                    accountId = acc?.id
                }

                val tx = ParsedTransaction(amount, category, note, type, 1.0f, accountId)
                addLocalMessage(MessageContent.TransactionCard(tx, false))

                val formattedAmt = formatCurrency(amount)
                val typeLabel = if (type == "INCOME") "khoản thu" else "khoản chi"
                "Mình đã tạo thẻ ghi nhận $typeLabel **$category** ($formattedAmt) rồi nè! Bạn bấm xác nhận bên dưới để ví tự cập nhật nhé ✨"
            }

            "addDebtLoan" -> {
                val typeStr = args["type"]?.toString() ?: "DEBT"
                val type = if (typeStr == "LOAN") DebtLoanType.LOAN else DebtLoanType.DEBT
                val personName = args["personName"]?.toString() ?: "Không tên"
                val amount = (args["amount"] as? Number)?.toDouble()
                    ?: (args["amount"] as? String)?.toDoubleOrNull() ?: 0.0
                val note = args["note"]?.toString() ?: ""

                val debt = DebtLoan(
                    id = UUID.randomUUID().toString(),
                    personName = personName,
                    amount = amount,
                    type = type,
                    note = note
                )
                addLocalMessage(MessageContent.DebtCard(debt, false))
                val label = if (type == DebtLoanType.DEBT) "bạn mượn từ $personName" else "cho $personName mượn"
                "Đã lập phiếu ghi nợ: $label (${formatCurrency(amount)}). Bạn kiểm tra và xác nhận thẻ bên dưới nhé!"
            }

            "addSavingsGoal" -> {
                val nameGoal = args["name"]?.toString() ?: "Mục tiêu mới"
                val targetAmount = (args["targetAmount"] as? Number)?.toDouble()
                    ?: (args["targetAmount"] as? String)?.toDoubleOrNull() ?: 0.0
                val autoSaving = (args["autoSavingAmount"] as? Number)?.toDouble()
                    ?: (args["autoSavingAmount"] as? String)?.toDoubleOrNull() ?: 0.0

                val goal = SavingsGoal(
                    goalName = nameGoal,
                    targetAmount = targetAmount,
                    autoSavingAmount = autoSaving
                )
                addLocalMessage(MessageContent.SavingsCard(goal, false))
                "Mục tiêu **$nameGoal** đích đến ${formatCurrency(targetAmount)} đã sẵn sàng! Chúc bạn sớm hoàn thành mục tiêu 🎯"
            }

            "addBudget" -> {
                val amount = (args["amount"] as? Number)?.toDouble()
                    ?: (args["amount"] as? String)?.toDoubleOrNull() ?: 0.0
                val category = args["category"]?.toString() ?: "Tất cả"
                val periodStr = args["period"]?.toString() ?: "MONTHLY"
                val period = try { BudgetPeriod.valueOf(periodStr) } catch(e: Exception) { BudgetPeriod.MONTHLY }

                val budget = FinanceBudget(
                    amount = amount,
                    category = category,
                    period = period
                )
                addLocalMessage(MessageContent.BudgetCard(budget, false))
                "Đã tạo hạn mức ngân sách **$category** (${formatCurrency(amount)}). Mình sẽ theo dõi và nhắc bạn khi chạm trần nhé! 🛡️"
            }

            "addGroupSplitBill" -> {
                val totalAmount = (args["totalAmount"] as? Number)?.toDouble()
                    ?: (args["totalAmount"] as? String)?.toDoubleOrNull() ?: 0.0
                val participantCount = (args["participantCount"] as? Number)?.toInt()
                    ?: (args["participantCount"] as? String)?.toIntOrNull() ?: 1
                val category = args["category"]?.toString() ?: "Ăn uống"
                val note = args["note"]?.toString() ?: ""
                @Suppress("UNCHECKED_CAST")
                val names = args["participants"] as? List<String> ?: emptyList()

                val sharePerPerson = if (participantCount > 0) totalAmount / participantCount else 0.0
                val participantList = if (names.isNotEmpty()) {
                    names.map { TransactionParticipant(name = it, shareAmount = sharePerPerson) }
                } else {
                    List(participantCount.coerceAtLeast(1) - 1) { TransactionParticipant(name = "Người ${it + 1}", shareAmount = sharePerPerson) }
                }

                addLocalMessage(
                    MessageContent.SplitBillCard(
                        totalAmount = totalAmount,
                        participantCount = participantCount,
                        category = category,
                        note = note,
                        initialParticipants = participantList,
                        confirmed = false
                    )
                )
                "Đã lập bảng chia bill ${formatCurrency(totalAmount)} cho $participantCount người (mỗi người ${formatCurrency(sharePerPerson)}). Kiểm tra chi tiết bên dưới nhé!"
            }

            "depositSavings" -> {
                val goalName = args["goalName"]?.toString() ?: "Mục tiêu"
                val amount = (args["amount"] as? Number)?.toDouble()
                    ?: (args["amount"] as? String)?.toDoubleOrNull() ?: 0.0
                val walletSource = args["walletSource"]?.toString()

                addLocalMessage(MessageContent.DepositSavingsCard(goalName, amount, walletSource, false))
                "Đã tạo lệnh trích ${formatCurrency(amount)} vào quỹ **$goalName**. Bấm xác nhận để chuyển tiền nha! 💰"
            }

            "withdrawSavings" -> {
                val goalName = args["goalName"]?.toString() ?: "Mục tiêu"
                val amount = (args["amount"] as? Number)?.toDouble()
                    ?: (args["amount"] as? String)?.toDoubleOrNull() ?: 0.0
                val destinationWallet = args["destinationWallet"]?.toString()
                val transferToSavingsGoal = args["transferToSavingsGoal"]?.toString()

                addLocalMessage(MessageContent.WithdrawSavingsCard(goalName, amount, destinationWallet, transferToSavingsGoal, false))
                "Đã tạo phiếu rút ${formatCurrency(amount)} từ quỹ **$goalName**."
            }

            "querySpendingAnalytics" -> {
                val period = args["period"]?.toString() ?: "THIS_MONTH"
                val categoryFilter = args["category"]?.toString()
                formatSpendingAnalyticsResponse(period, categoryFilter)
            }

            "getFinancialHealthDetails" -> {
                formatFinancialHealthDetailsResponse()
            }

            "explainBudgetStatus" -> {
                val category = args["category"]?.toString()
                formatBudgetStatusResponse(category)
            }

            "compareSpendingPeriods" -> {
                val p1 = args["period1"]?.toString() ?: "THIS_MONTH"
                val p2 = args["period2"]?.toString() ?: "LAST_MONTH"
                formatComparePeriodsResponse(p1, p2)
            }

            "updateUserHabit" -> {
                val min = (args["minMealCost"] as? Number)?.toDouble() ?: _userHabit.value.minMealCost
                val max = (args["maxMealCost"] as? Number)?.toDouble() ?: _userHabit.value.maxMealCost
                val routineNotes = args["routineNotes"]?.toString() ?: ""
                val generalNotes = _userHabit.value.generalNotes + (if (routineNotes.isNotBlank()) "\n- $routineNotes" else "")
                val newHabit = _userHabit.value.copy(minMealCost = min, maxMealCost = max, generalNotes = generalNotes)
                addLocalMessage(MessageContent.HabitUpdateCard(newHabit, false))
                "Fitie đã cập nhật thói quen chi tiêu của bạn để tư vấn chính xác hơn rồi nhé!"
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
                } catch (_: Exception) {}

                addLocalMessage(MessageContent.WeeklyPlanCard(desc, items, false))
                "Fitie đã lên kế hoạch tuần chi tiết dựa trên thói quen của bạn. Bạn xem và xác nhận nhé!"
            }

            else -> "Đã nhận yêu cầu và xử lý thành công."
        }
    }

    // ─── Analytics Tool Formatters ─────────────────────────────────────────────

    private fun formatSpendingAnalyticsResponse(period: String, categoryFilter: String?): String {
        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH)
        val currentYear = now.get(Calendar.YEAR)
        val tempCal = Calendar.getInstance()

        val filtered = transactions.filter { tx ->
            tx.type == TransactionType.EXPENSE || tx.type == TransactionType.GROUP_PREPAYMENT
        }.filter { tx ->
            tempCal.time = tx.timestamp.toDate()
            when (period) {
                "THIS_MONTH" -> tempCal.get(Calendar.MONTH) == currentMonth && tempCal.get(Calendar.YEAR) == currentYear
                "LAST_MONTH" -> {
                    val lastM = if (currentMonth == 0) 11 else currentMonth - 1
                    val lastY = if (currentMonth == 0) currentYear - 1 else currentYear
                    tempCal.get(Calendar.MONTH) == lastM && tempCal.get(Calendar.YEAR) == lastY
                }
                "THIS_WEEK" -> tempCal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR) && tempCal.get(Calendar.YEAR) == currentYear
                else -> abs(System.currentTimeMillis() - tx.timestamp.toDate().time) <= 7L * 24 * 3600 * 1000
            }
        }.filter { tx ->
            if (categoryFilter.isNullOrBlank() || categoryFilter == "Tất cả" || categoryFilter == "ALL") true
            else tx.category.equals(categoryFilter, ignoreCase = true)
        }

        val totalSpent = filtered.sumOf { if (it.isGroupPrepayment) it.personalAmount else it.amount }
        val categoryBreakdown = filtered.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { if (it.isGroupPrepayment) it.personalAmount else it.amount } }
            .entries.sortedByDescending { it.value }

        val periodLabel = when(period) {
            "THIS_MONTH" -> "tháng này"
            "LAST_MONTH" -> "tháng trước"
            "THIS_WEEK"  -> "tuần này"
            else         -> "7 ngày qua"
        }

        val sb = StringBuilder()
        sb.append("📊 **Tổng quan chi tiêu $periodLabel:**\n")
        sb.append("• **Tổng số tiền đã chi**: ${formatCurrency(totalSpent)}\n")
        sb.append("• **Số lượng giao dịch**: ${filtered.size}\n\n")

        if (categoryBreakdown.isNotEmpty()) {
            sb.append("🏷️ **Chi tiết các danh mục chính:**\n")
            categoryBreakdown.take(5).forEach { (cat, amt) ->
                val pct = if (totalSpent > 0) (amt / totalSpent * 100).toInt() else 0
                sb.append("  • **$cat**: ${formatCurrency(amt)} ($pct%)\n")
            }
        } else {
            sb.append("Chưa có ghi nhận chi tiêu nào trong kỳ này.")
        }
        return sb.toString()
    }

    private fun formatFinancialHealthDetailsResponse(): String {
        val result = FinancialHealthCalculator.calculate(
            wallet = wallet,
            transactions = transactions,
            budgets = budgets,
            goals = savingsGoals,
            debtLoans = debtLoans
        )

        val sb = StringBuilder()
        sb.append("🏆 **Báo Cáo Sức Khỏe Tài Chính: ${result.totalScore}/100đ (${result.grade})**\n")
        sb.append("_${result.summary}_\n\n")
        sb.append("📋 **Chi tiết 4 trụ cột đánh giá:**\n")

        result.pillars.forEach { p ->
            val icon = if (p.score >= 20) "🟢" else if (p.score >= 14) "🟡" else "🔴"
            sb.append("$icon **${p.name}**: ${p.score}/${p.maxScore}đ — *${p.status}*\n")
            sb.append("  • ${p.description}\n")
            sb.append("  • 💡 _Lời khuyên_: ${p.advice}\n\n")
        }
        return sb.toString().trim()
    }

    private fun formatBudgetStatusResponse(category: String?): String {
        if (budgets.isEmpty()) {
            return "🛡️ Bạn chưa thiết lập ngân sách nào. Hãy đặt hạn mức chi tiêu tháng (ví dụ: *\"đặt ngân sách ăn uống 3 triệu\"*) để mình hỗ trợ bạn kiểm soát dòng tiền nhé!"
        }

        val sb = StringBuilder()
        sb.append("🛡️ **Tình trạng ngân sách hiện tại:**\n\n")

        val targetBudgets = if (category.isNullOrBlank() || category == "ALL" || category == "Tất cả") budgets
        else budgets.filter { it.category.equals(category, ignoreCase = true) }

        targetBudgets.forEach { b ->
            val pace = BudgetLogic.calculateSpendingPace(b, transactions)
            val spentRatio = if (pace.totalBudget > 0) pace.spentSoFar / pace.totalBudget else 0.0
            val isOver = pace.spentSoFar > pace.totalBudget
            val isPaceWarning = pace.isProjectedToOverspend
            val icon = if (isOver) "🔴" else if (isPaceWarning) "🟡" else "🟢"
            val status = if (isOver) "ĐÃ VƯỢT" else if (isPaceWarning) "Cảnh báo vượt" else "An toàn"
            val totalLimit = b.amount + b.rolloverAmount

            sb.append("$icon **${b.category}** (${b.period.name.lowercase()}): **$status**\n")
            sb.append("  • Đã chi: ${formatCurrency(pace.spentSoFar)} / ${formatCurrency(totalLimit)} (${(spentRatio * 100).toInt()}%)\n")
            sb.append("  • Dự báo cuối tháng: ${formatCurrency(pace.projectedMonthEndSpent)}\n")
            sb.append("  • Lời khuyên: ${pace.paceSummary}\n\n")
        }
        return sb.toString().trim()
    }

    private fun formatComparePeriodsResponse(p1: String, p2: String): String {
        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH)
        val currentYear = now.get(Calendar.YEAR)
        val tempCal = Calendar.getInstance()

        val thisMonthExpenses = transactions.filter {
            (it.type == TransactionType.EXPENSE || it.type == TransactionType.GROUP_PREPAYMENT) &&
            run {
                tempCal.time = it.timestamp.toDate()
                tempCal.get(Calendar.MONTH) == currentMonth && tempCal.get(Calendar.YEAR) == currentYear
            }
        }

        val lastMonth = if (currentMonth == 0) 11 else currentMonth - 1
        val lastYear = if (currentMonth == 0) currentYear - 1 else currentYear
        val lastMonthExpenses = transactions.filter {
            (it.type == TransactionType.EXPENSE || it.type == TransactionType.GROUP_PREPAYMENT) &&
            run {
                tempCal.time = it.timestamp.toDate()
                tempCal.get(Calendar.MONTH) == lastMonth && tempCal.get(Calendar.YEAR) == lastYear
            }
        }

        val t1 = thisMonthExpenses.sumOf { if (it.isGroupPrepayment) it.personalAmount else it.amount }
        val t2 = lastMonthExpenses.sumOf { if (it.isGroupPrepayment) it.personalAmount else it.amount }

        val diff = t1 - t2
        val pctChange = if (t2 > 0) (diff / t2 * 100).toInt() else 0
        val trendEmoji = if (diff > 0) "🔺 Tăng" else "🔻 Giảm"

        return """
            📈 **So sánh chi tiêu Tháng này vs Tháng trước:**
            • **Tháng này**: ${formatCurrency(t1)}
            • **Tháng trước**: ${formatCurrency(t2)}
            • **Biến động**: $trendEmoji ${formatCurrency(abs(diff))} ($pctChange%)

            ${if (diff > 0) "⚠️ Chi tiêu tháng này đang có xu hướng tăng cao hơn. Hãy rà soát lại các khoản ăn ngoài và giải trí cuối tuần nhé!" else "👏 Rất tốt! Bạn đang kiểm soát chi tiêu tiết kiệm hơn so với tháng trước."}
        """.trimIndent()
    }

    // ─── System Prompt Builder ────────────────────────────────────────────────

    private fun buildSystemContext(): String {
        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH)
        val currentYear = now.get(Calendar.YEAR)
        val tempCal = Calendar.getInstance()

        val totalBal = wallet?.totalBalance ?: 0.0
        val accountsOverview = wallet?.accounts?.joinToString(", ") { 
            "${com.example.finfit.core.security.DataAnonymizer.anonymizeBankAccount(it)}: ${formatCurrency(it.amount)}" 
        } ?: "Chưa có"

        // Thu chi tháng này
        val thisMonthIncome = transactions.filter {
            it.type == TransactionType.INCOME &&
            run { tempCal.time = it.timestamp.toDate(); tempCal.get(Calendar.MONTH) == currentMonth && tempCal.get(Calendar.YEAR) == currentYear }
        }.sumOf { it.amount }

        val thisMonthExpense = transactions.filter {
            (it.type == TransactionType.EXPENSE || it.type == TransactionType.GROUP_PREPAYMENT) &&
            run { tempCal.time = it.timestamp.toDate(); tempCal.get(Calendar.MONTH) == currentMonth && tempCal.get(Calendar.YEAR) == currentYear }
        }.sumOf { if (it.isGroupPrepayment) it.personalAmount else it.amount }

        // Top 5 danh mục tháng này
        val topCategories = transactions.filter {
            (it.type == TransactionType.EXPENSE || it.type == TransactionType.GROUP_PREPAYMENT) &&
            run { tempCal.time = it.timestamp.toDate(); tempCal.get(Calendar.MONTH) == currentMonth && tempCal.get(Calendar.YEAR) == currentYear }
        }.groupBy { it.category }
        .mapValues { (_, list) -> list.sumOf { if (it.isGroupPrepayment) it.personalAmount else it.amount } }
        .entries.sortedByDescending { it.value }
        .take(5)
        .joinToString("; ") { (cat, amt) -> "$cat: ${formatCurrency(amt)} (${if (thisMonthExpense > 0) (amt/thisMonthExpense*100).toInt() else 0}%)" }

        val debtsStr = debtLoans.mapIndexed { idx, it ->
            "${com.example.finfit.core.security.DataAnonymizer.anonymizeDebtPersonName(it, idx + 1)} (${if (it.type == DebtLoanType.DEBT) "mình nợ" else "họ nợ"} ${formatCurrency(it.amount - it.paidAmount)})"
        }.joinToString(", ").ifBlank { "Không có nợ tồn đọng" }

        val goalsStr = savingsGoals.joinToString(", ") {
            "${it.goalName}: ${formatCurrency(it.currentAmount)}/${formatCurrency(it.targetAmount)}"
        }.ifBlank { "Chưa có mục tiêu" }

        val budgetsStr = budgets.joinToString(", ") {
            "${it.category}: hạn mức ${formatCurrency(it.amount + it.rolloverAmount)}"
        }.ifBlank { "Chưa đặt ngân sách" }

        // Tính điểm sức khỏe tài chính
        val healthScoreResult = FinancialHealthCalculator.calculate(wallet, transactions, budgets, savingsGoals, debtLoans)

        val habit = _userHabit.value
        val habitContext = "Thói quen ăn uống: ${formatCurrency(habit.minMealCost)} - ${formatCurrency(habit.maxMealCost)}/bữa. Ghi chú: ${habit.generalNotes}"

        val healthContext = "Sức khoẻ hôm nay: ${healthState.steps}/${healthState.stepGoal} bước, ${healthState.caloriesIn} kcal, ${healthState.waterConsumedMl}ml nước, ${healthState.sleepHours}h ngủ (Điểm: ${healthState.totalHealthScore}/100)"

        val currentDateStr = SimpleDateFormat("EEEE, dd/MM/yyyy HH:mm", Locale("vi", "VN")).format(Date())

        return """
BẠN LÀ FITIE — Trợ lý AI Tài chính & Sức khỏe thông minh, ấm áp, tận tâm và chuyên nghiệp của ứng dụng FinFit.

[THỜI GIAN & DỮ LIỆU THỰC TẾ CỦA NGƯỜI DÙNG HIỆN TẠI]:
- Thời gian: $currentDateStr
- Tổng số dư: ${formatCurrency(totalBal)} (Chi tiết: $accountsOverview)
- Thu nhập tháng này: ${formatCurrency(thisMonthIncome)} | Chi tiêu tháng này: ${formatCurrency(thisMonthExpense)}
- Top danh mục chi nhiều nhất tháng: ${if (topCategories.isNotBlank()) topCategories else "Chưa có"}
- Ngân sách tháng: $budgetsStr
- Mục tiêu tiết kiệm: $goalsStr
- Nợ & Cho vay: $debtsStr
- Điểm sức khỏe tài chính: ${healthScoreResult.totalScore}/100 (${healthScoreResult.grade})
- $habitContext
- $healthContext

[PHONG CÁCH GIAO TIẾP & TÍNH CÁCH (BẮT BUỘC)]:
1. Xưng hô: Bạn - Mình (hoặc Fitie), giọng điệu thân thiện, ấm áp, tinh tế, dí dỏm nhẹ nhàng.
2. ĐỒNG CẢM TRƯỚC TIÊN: Nếu người dùng than vãn ("hết tiền rồi 😭", "lại tiêu lố rồi 🤦‍♂️"), chia sẻ an ủi hoặc động viên chân thành TRƯỚC TIÊN, không bao giờ phán xét hay trả lời cộc lốc!
3. CHIA VUI KHI CÓ TIN TỐT: Nếu người dùng báo lương về, nhận thưởng, hoàn thành mục tiêu -> Hãy chúc mừng hào hứng và khuyên trích ngay 15-20% tiết kiệm.

[HƯỚNG DẪN XỬ LÝ 3 NHÓM Ý ĐỊNH]:
• NHÓM 1: Ý ĐỊNH HÀNH ĐỘNG (Ghi giao dịch, vay mượn, tạo quỹ, chia bill...):
  - Hãy gọi tool tương ứng để mở Card cho người dùng.
  - KÈM THEO lời nói ngắn gọn, tươi vui.
  - LIÊN KẾT DINH DƯỠNG (CROSS-MODULE): Nếu giao dịch là món ăn (phở, bún, bánh mì, trà sữa, cơm, pizza...), hãy tinh tế ước tính calo (VD: Phở ~450 kcal, Bánh mì ~350 kcal) và hỏi gợi ý: "Bạn có muốn mình ghi nhận ~X kcal món này vào Nhật ký Dinh dưỡng hôm nay luôn không nè? 🍜".
• NHÓM 2: Ý ĐỊNH TƯ VẤN & PHÂN TÍCH ('tháng này tiêu có ổn không?', 'sao tiền vơi nhanh thế?', 'nên ăn ngoài hay tự nấu?'):
  - TUYỆT ĐỐI KHÔNG TRẢ LỜI NGẮN 1-2 CÂU.
  - Hãy trả lời có cấu trúc 4 phần rõ ràng:
    1. 🌟 Đánh giá tổng quan (Tốt / Trung bình / Báo động).
    2. 📊 Dẫn chứng số liệu thực tế từ hệ thống được cung cấp ở trên (Tổng chi, top danh mục tốn nhất, % ngân sách, calo nạp vào).
    3. 🔍 Phân tích nguyên nhân & thói quen (VD: ăn ngoài cuối tuần tăng vọt, tự nấu ăn giúp tiết kiệm ~30k/bữa).
    4. 💡 Khuyến nghị hành động tiếp theo cụ thể.
• NHÓM 3: TÂM SỰ / TRÒ CHUYỆN:
  - Chia sẻ kiến thức tài chính (quy tắc 50/30/20, 6 chiếc lọ) hoặc thói quen sống lành mạnh, cân bằng chi tiêu và sức khỏe.

[QUY TẮC QUY ĐỔI TIỀN & TIẾNG LÓNG]:
- 'k'/'cành' = 1.000đ; 'lít'/'lốp' = 100.000đ; 'củ'/'tr' = 1.000.000đ; 'trăm rưỡi' = 150.000đ; 'nửa củ' = 500.000đ.
- 'ăn sáng 30', 'đổ xăng 50' -> hiểu là 30.000đ, 50.000đ.
- 'Thứ 2... Chủ nhật' là ngày, KHÔNG gộp vào số tiền.
        """.trimIndent()
    }

    // ─── Confirmation Callbacks (Preserved 100%) ──────────────────────────────

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
            addLocalMessage(MessageContent.Text("✅ Đã ghi giao dịch thành công! Số dư ví đã được cập nhật tự động."))
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
            addLocalMessage(MessageContent.Text("✅ Đã ghi nhận hoá đơn thành công!"))
        }
    }

    fun confirmDebtLoan(debtLoan: DebtLoan, msgId: String) {
        viewModelScope.launch {
            firestoreRepository.saveDebtLoan(userId, debtLoan)
            updateMessage(msgId, MessageContent.DebtCard(debtLoan, true))
            addLocalMessage(MessageContent.Text("✅ Đã ghi nhận khoản ${if (debtLoan.type == DebtLoanType.DEBT) "vay nợ" else "cho vay"} với ${debtLoan.personName}!"))
        }
    }

    fun confirmSavingsGoal(goal: SavingsGoal, msgId: String) {
        viewModelScope.launch {
            firestoreRepository.saveSavingsGoal(userId, goal)
            updateMessage(msgId, MessageContent.SavingsCard(goal, true))
            addLocalMessage(MessageContent.Text("✅ Đã thiết lập mục tiêu tiết kiệm **${goal.goalName}**!"))
        }
    }

    fun confirmBudget(budget: FinanceBudget, msgId: String) {
        viewModelScope.launch {
            firestoreRepository.saveBudget(userId, budget)
            updateMessage(msgId, MessageContent.BudgetCard(budget, true))
            addLocalMessage(MessageContent.Text("✅ Đã chốt hạn mức ngân sách **${budget.category}**!"))
        }
    }

    fun confirmSchedule(item: SpendingScheduleItem, msgId: String) {
        viewModelScope.launch {
            firestoreRepository.saveSpendingSchedule(userId, item)
            updateMessage(msgId, MessageContent.ScheduleCard(item, true))
            addLocalMessage(MessageContent.Text("✅ Lịch trình tự động đã được lưu và sẽ chạy hàng tuần!"))
        }
    }

    fun confirmHeldFund(fundName: String, amount: Double, msgId: String) {
        viewModelScope.launch {
            wallet?.let { w ->
                val newFund = HeldFundItem(id = UUID.randomUUID().toString(), name = fundName, amount = amount)
                val updatedWallet = w.copy(heldFunds = w.heldFunds + newFund)
                firestoreRepository.saveUserWallet(updatedWallet)
            }
            updateMessage(msgId, MessageContent.HeldFundCard(fundName, amount, true))
            addLocalMessage(MessageContent.Text("✅ Đã thiết lập quỹ giữ hộ '$fundName' thành công!"))
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
            val groupOwes = participants.filter { !it.isPaid }.sumOf { it.shareAmount - it.paidAmount }

            val tx = FinanceTransaction(
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

            participants.forEach { p ->
                if (!p.isPaid || p.paidAmount < p.shareAmount) {
                    val remaining = p.shareAmount - p.paidAmount
                    if (remaining > 0) {
                        val debt = DebtLoan(
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

                val immediateRepayments = participants.sumOf { it.paidAmount }
                if (updatedAccounts.isNotEmpty()) {
                    updatedAccounts[0] = updatedAccounts[0].copy(amount = updatedAccounts[0].amount + immediateRepayments)
                }

                val updatedWallet = w.copy(
                    accounts = updatedAccounts,
                    groupPrepaidItems = if (groupOwes > 0) {
                        w.groupPrepaidItems + GroupPrepaidItem(
                            id = UUID.randomUUID().toString(),
                            transactionId = tx.id,
                            description = "Split Bill: $note",
                            totalAmount = totalAmount,
                            groupOwedAmount = groupOwes,
                            participantCount = participants.size + 1,
                            participants = participants,
                            createdAt = com.google.firebase.Timestamp.now()
                        )
                    } else w.groupPrepaidItems
                )
                firestoreRepository.saveUserWallet(updatedWallet)
            }

            firestoreRepository.addTransaction(userId, tx)
            updateMessage(msgId, MessageContent.SplitBillCard(totalAmount, participants.size + 1, category, note, true))

            val shareStr = formatCurrency(groupOwes)
            if (groupOwes > 0) {
                addLocalMessage(MessageContent.Text("✅ Đã chia bill thành công! Nhóm còn nợ bạn $shareStr. Các khoản nợ đã được đồng bộ vào Ghi nợ."))
            } else {
                addLocalMessage(MessageContent.Text("✅ Đã chia bill và ghi nhận mọi người đã thanh toán đủ!"))
            }
        }
    }

    fun confirmUserHabit(habit: UserHabit, msgId: String) {
        viewModelScope.launch {
            firestoreRepository.saveUserHabit(userId, habit)
            _userHabit.value = habit
            updateMessage(msgId, MessageContent.HabitUpdateCard(habit, true))
            addLocalMessage(MessageContent.Text("✅ Fitie đã ghi nhớ thói quen của bạn để lên kế hoạch tốt hơn!"))
        }
    }

    fun confirmWeeklyPlan(items: List<SpendingScheduleItem>, msgId: String, desc: String) {
        viewModelScope.launch {
            items.forEach { firestoreRepository.saveWeeklyScheduleItem(userId, it) }
            updateMessage(msgId, MessageContent.WeeklyPlanCard(desc, items, true))
            addLocalMessage(MessageContent.Text("✅ Tuyệt vời! Kế hoạch chi tiêu tuần đã được đồng bộ vào lịch trình của bạn."))
        }
    }

    fun confirmDepositSavings(goalName: String, amount: Double, sourceAccountId: String?, msgId: String) {
        viewModelScope.launch {
            val goal = savingsGoals.find { it.goalName == goalName }
            if (goal != null) {
                val updatedGoal = goal.copy(currentAmount = goal.currentAmount + amount)
                firestoreRepository.saveSavingsGoal(userId, updatedGoal)
            }

            wallet?.let { w ->
                val updatedAccounts = w.accounts.map {
                    if (it.id == sourceAccountId) it.copy(amount = it.amount - amount) else it
                }
                val updatedWallet = w.copy(accounts = updatedAccounts)
                firestoreRepository.saveUserWallet(updatedWallet)

                val tx = FinanceTransaction(
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

            updateMessage(msgId, MessageContent.DepositSavingsCard(goalName, amount, wallet?.accounts?.find { it.id == sourceAccountId }?.name, true))
            addLocalMessage(MessageContent.Text("✅ Đã nạp thành công ${formatCurrency(amount)} vào quỹ **$goalName**!"))
        }
    }

    fun confirmWithdrawSavings(goalName: String, amount: Double, destAccountId: String?, destGoalId: String?, msgId: String) {
        viewModelScope.launch {
            val goal = savingsGoals.find { it.goalName == goalName }
            if (goal != null) {
                val updatedGoal = goal.copy(currentAmount = (goal.currentAmount - amount).coerceAtLeast(0.0))
                firestoreRepository.saveSavingsGoal(userId, updatedGoal)
            }

            if (!destGoalId.isNullOrBlank()) {
                val destGoal = savingsGoals.find { it.id == destGoalId }
                if (destGoal != null) {
                    val updatedDestGoal = destGoal.copy(currentAmount = destGoal.currentAmount + amount)
                    firestoreRepository.saveSavingsGoal(userId, updatedDestGoal)
                }
            } else if (!destAccountId.isNullOrBlank()) {
                wallet?.let { w ->
                    val updatedAccounts = w.accounts.map {
                        if (it.id == destAccountId) it.copy(amount = it.amount + amount) else it
                    }
                    val updatedWallet = w.copy(accounts = updatedAccounts)
                    firestoreRepository.saveUserWallet(updatedWallet)
                }
            }

            updateMessage(msgId, MessageContent.WithdrawSavingsCard(goalName, amount, wallet?.accounts?.find { it.id == destAccountId }?.name, savingsGoals.find { it.id == destGoalId }?.goalName, true))
            addLocalMessage(MessageContent.Text("✅ Đã xử lý lệnh rút/chuyển tiền từ quỹ **$goalName**!"))
        }
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
    private val healthState: HealthUiState,
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
                healthState,
                habit
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
