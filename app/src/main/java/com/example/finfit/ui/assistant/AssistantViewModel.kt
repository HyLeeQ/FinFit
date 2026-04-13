package com.example.finfit.ui.assistant

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.finfit.data.remote.GeminiService
import com.example.finfit.data.remote.QuotaExceededException
import com.example.finfit.finance.model.*
import com.example.finfit.finance.repository.FirestoreRepository
import com.example.finfit.finance.util.ParsedTransaction
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.FunctionResponsePart
import com.google.ai.client.generativeai.type.TextPart
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

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

    private val _messages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage(
            content = MessageContent.Text("Xin chào! 👋 Tôi là Trợ lý FinFit.\n\n" +
                        "Bạn có thể:\n" +
                        "• Nhắn \"ăn tối 20k\" → Tôi tự tạo giao dịch\n" +
                        "• Nhắn \"vay Nam 500k\" → Ghi nợ & cho vay\n" +
                        "• Nhắn \"tạo tiết kiệm mua xe 50 triệu\" → Lập mục tiêu\n" +
                        "• Hỏi tôi về số dư ví, tài chính hiện tại bất kỳ"),
            isUser = false
        )
    ))
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
        _messages.value = _messages.value.map {
            if (it.id == id) it.copy(content = newContent) else it
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        addLocalMessage(MessageContent.Text(text), true)
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val reply = processMessage(text)
                if (reply.isNotBlank() && reply != "SILENT_CONFIRM_UI") {
                    addLocalMessage(MessageContent.Text(reply), false)
                }
            } catch (e: QuotaExceededException) {
                addLocalMessage(MessageContent.Text("⚠️ Trợ lý đang bận, vui lòng thử lại sau vài phút. (Đã đạt GH API)"), false)
            } catch (e: Exception) {
                addLocalMessage(MessageContent.Text("Lỗi kết nối: ${e.message}"), false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Kiểm tra Thứ Hai chủ động */
    fun checkMondayProactive() {
        viewModelScope.launch {
            val now = java.util.Calendar.getInstance()
            // 2: Thứ 2 (trong java.util.Calendar)
            if (now.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.MONDAY) {
                val weekId = "${now.get(java.util.Calendar.YEAR)}-${now.get(java.util.Calendar.WEEK_OF_YEAR)}"
                if (_userHabit.value.lastProactiveWeek != weekId) {
                    // Ghi nhận đã hỏi tuần này
                    val updatedHabit = _userHabit.value.copy(lastProactiveWeek = weekId)
                    _userHabit.value = updatedHabit
                    firestoreRepository.saveUserHabit(userId, updatedHabit)

                    // Gửi tin nhắn chủ động
                    addLocalMessage(MessageContent.Text("Chào buổi sáng Thứ Hai! 👋 Tuần mới bắt đầu rồi, bạn có kế hoạch đặc biệt nào cần dùng tiền không? Để tôi giúp bạn lên kế hoạch chi tiêu thông minh nhé!"), false)
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
                val amount = (args["amount"] as? Number)?.toDouble() ?: (args["amount"] as? String)?.toDoubleOrNull() ?: 0.0
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
                "Đã hiển thị thẻ thu thập giao dịch để chờ người dùng bấm xác nhận."
            }
            "addDebtLoan" -> {
                val typeStr = args["type"]?.toString() ?: "DEBT"
                val type = if (typeStr == "LOAN") DebtLoanType.LOAN else DebtLoanType.DEBT
                val debt = DebtLoan(
                    id = UUID.randomUUID().toString(),
                    personName = args["personName"]?.toString() ?: "Không tên",
                    amount = (args["amount"] as? Number)?.toDouble() ?: (args["amount"] as? String)?.toDoubleOrNull() ?: 0.0,
                    type = type,
                    note = args["note"]?.toString() ?: ""
                )
                addLocalMessage(MessageContent.DebtCard(debt, false))
                "Đơn nợ đã được show UI card để đợi user xác nhận."
            }
            "addGroupSplitBill" -> {
                val totalAmount = (args["totalAmount"] as? Number)?.toDouble() ?: (args["totalAmount"] as? String)?.toDoubleOrNull() ?: 0.0
                val participantCount = (args["participantCount"] as? Number)?.toInt() ?: (args["participantCount"] as? String)?.toIntOrNull() ?: 1
                val category = args["category"]?.toString() ?: "Ăn uống"
                val note = args["note"]?.toString() ?: ""
                
                addLocalMessage(MessageContent.SplitBillCard(totalAmount, participantCount, category, note, false))
                "Đã hiển thị thẻ thu thập Split Bill để chờ người dùng bấm xác nhận."
            }
            "addSavingsGoal" -> {
                val autoSavingAmount = (args["autoSavingAmount"] as? Number)?.toDouble() ?: (args["autoSavingAmount"] as? String)?.toDoubleOrNull() ?: 0.0
                val targetDateStr = args["targetDate"]?.toString()
                
                var timestamp: com.google.firebase.Timestamp? = null
                if (!targetDateStr.isNullOrBlank()) {
                    try {
                        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        val date = format.parse(targetDateStr)
                        if (date != null) {
                            timestamp = com.google.firebase.Timestamp(date)
                        }
                    } catch(e: Exception) {}
                }

                val goal = SavingsGoal(
                    id = UUID.randomUUID().toString(),
                    goalName = args["name"]?.toString() ?: "Mục tiêu",
                    targetAmount = (args["targetAmount"] as? Number)?.toDouble() ?: (args["targetAmount"] as? String)?.toDoubleOrNull() ?: 0.0,
                    autoSavingAmount = autoSavingAmount,
                    targetDate = timestamp
                )
                addLocalMessage(MessageContent.SavingsCard(goal, false))
                "Mục tiêu tiết kiệm đã được hiện card lên. Vui lòng nhắn thông báo bạn đang đợi người dùng ấn Xác nhận ở card để bạn ghi nhận lên Firebase."
            }
            "addBudget" -> {
                val periodStr = args["period"]?.toString() ?: "MONTHLY"
                val period = if (periodStr == "WEEKLY") BudgetPeriod.WEEKLY else BudgetPeriod.MONTHLY
                val budget = FinanceBudget(
                    id = UUID.randomUUID().toString(),
                    amount = (args["amount"] as? Number)?.toDouble() ?: (args["amount"] as? String)?.toDoubleOrNull() ?: 0.0,
                    category = args["category"]?.toString() ?: "Tất cả",
                    period = period
                )
                addLocalMessage(MessageContent.BudgetCard(budget, false))
                "Hạn mức ngân sách đã hiển thị lên chờ xác nhận."
            }
            "addAutoSchedule" -> {
                val item = SpendingScheduleItem(
                    id = UUID.randomUUID().toString(),
                    dayOfWeek = (args["dayOfWeek"] as? Number)?.toInt() ?: (args["dayOfWeek"] as? String)?.toIntOrNull() ?: 1,
                    amount = (args["amount"] as? Number)?.toDouble() ?: (args["amount"] as? String)?.toDoubleOrNull() ?: 0.0,
                    category = args["category"]?.toString() ?: "Ăn uống",
                    note = args["note"]?.toString() ?: "",
                    isAutoApply = true
                )
                addLocalMessage(MessageContent.ScheduleCard(item, false))
                "Lịch trình đã hiển thị lên chờ xác nhận."
            }
            "addHeldFund" -> {
                val fundName = args["fundName"]?.toString() ?: "Quỹ chung"
                val amount = (args["amount"] as? Number)?.toDouble() ?: (args["amount"] as? String)?.toDoubleOrNull() ?: 0.0
                addLocalMessage(MessageContent.HeldFundCard(fundName, amount, false))
                "Quỹ giữ hộ đã được hiển thị lên thẻ xác nhận."
            }
            "updateUserHabit" -> {
                val min = (args["minMealCost"] as? Number)?.toDouble() ?: _userHabit.value.minMealCost
                val max = (args["maxMealCost"] as? Number)?.toDouble() ?: _userHabit.value.maxMealCost
                val routineNotes = args["routineNotes"]?.toString() ?: ""
                val generalNotes = _userHabit.value.generalNotes + (if(routineNotes.isNotBlank()) "\n- $routineNotes" else "")
                
                val newHabit = _userHabit.value.copy(
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
                        items.add(SpendingScheduleItem(
                            id = UUID.randomUUID().toString(),
                            dayOfWeek = obj.optInt("dayOfWeek", 1),
                            amount = obj.optDouble("amount", 0.0),
                            category = obj.optString("category", "Ăn uống"),
                            note = obj.optString("note", "")
                        ))
                    }
                } catch(e: Exception) {}
                
                addLocalMessage(MessageContent.WeeklyPlanCard(desc, items, false))
                "Đã đề xuất kế hoạch tuần mới để chờ người dùng xác nhận."
            }
            else -> "Hàm không được hỗ trợ"
        }
    }

    fun confirmTransaction(transaction: FinanceTransaction, updatedWallet: AppUserWallet, msgId: String, parsedTx: ParsedTransaction) {
        viewModelScope.launch {
            firestoreRepository.saveUserWallet(updatedWallet)
            firestoreRepository.addTransaction(userId, transaction)
            updateMessage(msgId, MessageContent.TransactionCard(parsedTx, true))
            addLocalMessage(MessageContent.Text("✅ Đã ghi giao dịch thành công!"))
        }
    }
    
    fun confirmBill(transaction: FinanceTransaction, updatedWallet: AppUserWallet, msgId: String, imageUri: Uri, amt: Double?) {
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

    fun confirmSplitBill(totalAmount: Double, participantCount: Int, category: String, note: String, msgId: String) {
        viewModelScope.launch {
            val myShare = totalAmount / participantCount
            val groupOwes = totalAmount - myShare

            val tx = FinanceTransaction(
                id = UUID.randomUUID().toString(),
                amount = myShare,
                type = TransactionType.EXPENSE,
                category = category,
                note = "Split Bill: $note",
                isGroupPrepayment = true,
                personalAmount = myShare,
                groupAmount = groupOwes,
                participantCount = participantCount,
                timestamp = com.google.firebase.Timestamp.now()
            )

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
                val updatedWallet = w.copy(
                    accounts = updatedAccounts,
                    groupPrepaidAmount = w.groupPrepaidAmount + groupOwes
                )
                firestoreRepository.saveUserWallet(updatedWallet)
            }

            firestoreRepository.addTransaction(userId, tx)
            updateMessage(msgId, MessageContent.SplitBillCard(totalAmount, participantCount, category, note, true))
            
            val shareStr = com.example.finfit.finance.ui.utils.formatCurrency(groupOwes)
            addLocalMessage(MessageContent.Text("✅ Đã chia bill thành công! Nhóm nợ bạn $shareStr."))
        }
    }

    fun confirmUserHabit(habit: UserHabit, msgId: String) {
        viewModelScope.launch {
            firestoreRepository.saveUserHabit(userId, habit)
            _userHabit.value = habit
            updateMessage(msgId, MessageContent.HabitUpdateCard(habit, true))
            addLocalMessage(MessageContent.Text("✅ Đã ghi nhớ thói quen của bạn! Tôi sẽ dùng nó để lên kế hoạch tốt hơn."))
        }
    }

    fun confirmWeeklyPlan(items: List<SpendingScheduleItem>, msgId: String, desc: String) {
        viewModelScope.launch {
            items.forEach { firestoreRepository.saveWeeklyScheduleItem(userId, it) }
            updateMessage(msgId, MessageContent.WeeklyPlanCard(desc, items, true))
            addLocalMessage(MessageContent.Text("✅ Tuyệt vời! Tôi đã cập nhật toàn bộ kế hoạch vào Lịch trình chi tiêu của bạn."))
        }
    }

    private fun buildSystemContext(): String {
        val totalBal = wallet?.totalBalance ?: 0.0
        val debtsStr = debtLoans.joinToString(", ") { "${it.personName} (${if(it.type==DebtLoanType.DEBT) "mình nợ" else "nợ mình"} ${it.amount})" }.ifBlank { "Không có khoản nợ nào." }
        val goalsStr = savingsGoals.joinToString(", ") { "${it.goalName} (${it.currentAmount}/${it.targetAmount})" }.ifBlank { "Không có mục tiêu nào." }
        
        val habit = _userHabit.value
        val habitContext = "THÓI QUEN NGƯỜI DÙNG:\n" +
                "- Giá bữa ăn: ${habit.minMealCost} - ${habit.maxMealCost} đ\n" +
                "- Ghi chú lịch trình/thói quen: ${habit.generalNotes}\n" +
                "- Lịch trình cố định: ${habit.fixedCosts.joinToString { "Thứ ${it.dayOfWeek}: ${it.amount} (${it.category})" }}\n"

        return "Bạn là Trợ lý FinFit. Thân thiện, bằng tiếng Việt.\n" +
               habitContext +
               "- Số dư ví tổng của KH: $totalBal đ.\n" +
               "- Số nợ tồn: $debtsStr\n" +
               "- Các mục tiêu hiện tại: $goalsStr\n" +
               "- KHI LẬP KẾ HOẠCH TUẦN: Hãy ưu tiên điền các thói quen cố định đã có sẵn. Nếu người dùng báo thay đổi (vd: thứ 3 không học trường), hãy dựa vào thói quen ăn uống/xăng xe để tính toán lại số tiền tiết kiệm nhất. Các hoạt động ăn hàng, vui chơi là hoạt động ĐỘT XUẤT, không được lặp lại vào tuần sau.\n" +
               "- Lệnh tạo giao dịch, nợ, ngân sách -> Gọi Tool ngay lập tức thay vì phân tích dong dài.\n" +
               "- LƯU Ý QUAN TRỌNG VỀ SỐ TIỀN: Trong tiếng Việt, 'Thứ 2, Thứ 3, ..., Thứ 7' dùng để chỉ ngày trong tuần. TUYỆT ĐỐI KHÔNG được nhầm lẫn số thứ tự này với số tiền (amount). Ví dụ: 'Thứ 4 ăn 20k' thì số tiền là 20000, KHÔNG phải 4020000 hay 4000000.\n" +
               "- NẾU người dùng chỉ báo lịch trình đi lại/ăn uống (ví dụ: T2-T4 ở trọ, T5 về quê) -> Sử dụng tool 'updateUserHabit' để ghi nhớ thói quen thay vì tạo giao dịch đơn lẻ, trừ khi họ nói rõ số tiền vừa chi ra.\n" +
               "- NẾU người dùng báo 'Đã trả nợ cho X' hoặc 'X trả nợ mình' -> TỰ ĐỘNG tìm số tiền của X trong mục 'Số nợ tồn' để làm tham số 'amount'. Sau đó gọi tool addTransaction (Chi/Thu), category 'Trả nợ'/'Thu nợ'. Tuyệt đối KHÔNG được hỏi lại số tiền nếu X đã có trong 'Số nợ tồn'.\n" +
               "- NẾU nội dung KHÔNG liên quan tài chính/sức khoẻ hoặc bạn không hiểu, hãy trả lời trò chuyện bình thường, tuyệt đối KHÔNG gọi tool/hàm tạo giao dịch giả mạo."
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
            return AssistantViewModel(firestoreRepository, GeminiService(), userId, wallet, transactions, schedule, debtLoans, savingsGoals, budgets, stepsToday, habit) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
