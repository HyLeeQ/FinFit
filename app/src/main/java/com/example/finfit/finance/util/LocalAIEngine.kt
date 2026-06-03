package com.example.finfit.finance.util

import com.example.finfit.finance.model.*
import java.util.Calendar

/**
 * LocalAIEngine — Tất cả logic AI không cần API key.
 * Chạy hoàn toàn on-device.
 *
 * Chức năng:
 *  1. getTimeSuggestions()       — Gợi ý chip theo giờ trong ngày
 *  2. tryAnswerLocally()         — Trả lời trực tiếp các câu hỏi đơn giản
 *  3. parseMultiTransaction()    — Tách nhiều giao dịch từ 1 câu
 *  4. getBudgetAlerts()          — Cảnh báo ngân sách vượt/sắp vượt
 *  5. getSpendingInsight()       — Thống kê chi tiêu nhanh (hôm nay/tuần/tháng)
 *  6. detectRecurring()          — Phát hiện giao dịch lặp lại thường xuyên
 */
object LocalAIEngine {

    // ─── 1. Time-aware quick suggestions ────────────────────────────────────

    data class QuickSuggestion(val label: String, val text: String, val emoji: String)

    fun getTimeSuggestions(
        hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        recentCategories: List<String> = emptyList()
    ): List<QuickSuggestion> {
        val timeBased = when (hour) {
            in 5..9   -> listOf(
                QuickSuggestion("Ăn sáng",   "ăn sáng 30k",       "🍳"),
                QuickSuggestion("Cà phê",    "cà phê 25k",         "☕"),
                QuickSuggestion("Xe buýt",   "xe buýt 7k",         "🚌"),
                QuickSuggestion("Grab",      "grab đi làm 45k",    "🚗"),
            )
            in 10..13 -> listOf(
                QuickSuggestion("Ăn trưa",   "ăn trưa 50k",        "🍜"),
                QuickSuggestion("Trà sữa",   "trà sữa 35k",        "🧋"),
                QuickSuggestion("Snack",     "ăn vặt 20k",         "🍿"),
                QuickSuggestion("Grab food", "grab food 60k",      "📱"),
            )
            in 14..17 -> listOf(
                QuickSuggestion("Xăng",      "đổ xăng 100k",       "⛽"),
                QuickSuggestion("Mua sắm",   "mua đồ 200k",        "🛍️"),
                QuickSuggestion("Cà phê",    "cà phê chiều 30k",   "☕"),
                QuickSuggestion("Thể thao",  "gym 100k",           "🏋️"),
            )
            in 18..21 -> listOf(
                QuickSuggestion("Ăn tối",    "ăn tối 80k",         "🍖"),
                QuickSuggestion("Đi chơi",   "đi chơi 150k",       "🎮"),
                QuickSuggestion("Điện nước", "tiền điện 300k",     "💡"),
                QuickSuggestion("Nhậu",      "nhậu với bạn 200k",  "🍺"),
            )
            else -> listOf(
                QuickSuggestion("Ăn đêm",    "ăn khuya 40k",       "🌙"),
                QuickSuggestion("Grab",      "grab về nhà 50k",    "🚗"),
            )
        }

        // Thêm gợi ý dựa trên danh mục hay dùng
        val contextual = recentCategories
            .take(2)
            .mapNotNull { cat ->
                CATEGORY_QUICK_FILL[cat]?.let { (label, text, emoji) ->
                    QuickSuggestion(label, text, emoji)
                }
            }

        // Luôn có gợi ý nhanh cho các tính năng phổ biến
        val universal = listOf(
            QuickSuggestion("Số dư",     "số dư hiện tại",            "💰"),
            QuickSuggestion("Hôm nay",   "tôi tiêu bao nhiêu hôm nay", "📊"),
            QuickSuggestion("Nợ tôi",    "danh sách nợ",              "💳"),
            QuickSuggestion("Lịch hôm nay", "lịch hôm nay có gì",     "📅"),
            QuickSuggestion("Tiết kiệm", "tiến độ mục tiêu tiết kiệm", "🎯"),
            QuickSuggestion("So sánh",   "so sánh tháng trước",        "📈"),
        )

        return (timeBased + contextual + universal).distinctBy { it.label }.take(6)
    }

    private val CATEGORY_QUICK_FILL = mapOf(
        "Ăn uống"   to Triple("Ăn uống",   "ăn sáng 30k",   "🍳"),
        "Di chuyển" to Triple("Xăng xe",   "đổ xăng 80k",   "⛽"),
        "Mua sắm"   to Triple("Mua sắm",   "mua đồ 100k",   "🛍️"),
        "Giải trí"  to Triple("Giải trí",  "giải trí 100k", "🎮"),
        "Sức khỏe"  to Triple("Thuốc",     "mua thuốc 50k", "💊"),
    )

    // ─── 2. Local query answering ────────────────────────────────────────────

    data class LocalAnswer(val text: String, val handled: Boolean)

    fun tryAnswerLocally(
        userText: String,
        wallet: AppUserWallet?,
        transactions: List<FinanceTransaction>,
        budgets: List<FinanceBudget>,
        savingsGoals: List<SavingsGoal>,
        debtLoans: List<DebtLoan>,
        schedule: List<SpendingScheduleItem> = emptyList(),
        habit: UserHabit? = null
    ): LocalAnswer {
        val lower = userText.lowercase().trim()

        // ── Bộ lọc tránh tryAnswerLocally chặn nhầm lệnh tạo/hành động ──
        val hasAmount = SmartTransactionParser.parseAmount(lower) != null || lower.any { it.isDigit() }
        val isActionOrCreation = matchesAny(lower, "tạo", "thêm", "đặt", "ghi", "trả", "thiết lập", "lập", "thu", "nhận", "cho")
        val isExplicitQuery = matchesAny(lower, "danh sách", "tổng", "xem", "tra cứu", "kiểm tra", "báo cáo", "tình hình", "tiến độ", "bao nhiêu", "còn lại", "lịch sử")

        if ((hasAmount || isActionOrCreation) && !isExplicitQuery) {
            return LocalAnswer("", false)
        }

        // ── Số dư ────────────────────────────────────────────────
        if (matchesAny(lower, "số dư", "còn bao nhiêu tiền", "balance", "tiền còn", "trong ví")) {
            if (wallet == null) return LocalAnswer("Chưa có dữ liệu ví.", true)
            val total = wallet.totalBalance
            val accountsStr = wallet.accounts.joinToString("\n") {
                "  • ${it.displayName}: ${formatCurrency(it.amount)}"
            }.ifBlank { "  Chưa có tài khoản nào." }
            return LocalAnswer(
                "💰 **Số dư hiện tại:** ${formatCurrency(total)}\n\n$accountsStr",
                true
            )
        }

        // ── Chi tiêu hôm nay ─────────────────────────────────────
        if (matchesAny(lower, "hôm nay", "today", "tiêu hôm nay", "chi hôm nay")) {
            val todayExpenses = filterByPeriod(transactions, Period.TODAY, TransactionType.EXPENSE)
            val total = todayExpenses.sumOf { it.amount }
            if (total == 0.0) return LocalAnswer("Hôm nay bạn chưa ghi nhận khoản chi nào 🎉", true)
            val breakdown = topCategories(todayExpenses)
            return LocalAnswer(
                "📊 **Chi tiêu hôm nay:** ${formatCurrency(total)}\n\n$breakdown",
                true
            )
        }

        // ── Chi tiêu tuần này ────────────────────────────────────
        if (matchesAny(lower, "tuần này", "this week", "tiêu tuần này", "7 ngày")) {
            val weekExpenses = filterByPeriod(transactions, Period.WEEK, TransactionType.EXPENSE)
            val total = weekExpenses.sumOf { it.amount }
            val dailyAvg = if (total > 0) total / 7 else 0.0
            val breakdown = topCategories(weekExpenses)
            return LocalAnswer(
                "📅 **Chi tiêu tuần này:** ${formatCurrency(total)}\n" +
                "Trung bình/ngày: ${formatCurrency(dailyAvg)}\n\n$breakdown",
                true
            )
        }

        // ── Chi tiêu tháng này ───────────────────────────────────
        if (matchesAny(lower, "tháng này", "this month", "tiêu tháng", "tháng hiện tại")) {
            val monthExpenses = filterByPeriod(transactions, Period.MONTH, TransactionType.EXPENSE)
            val total = monthExpenses.sumOf { it.amount }
            val breakdown = topCategories(monthExpenses)
            return LocalAnswer(
                "📆 **Chi tiêu tháng này:** ${formatCurrency(total)}\n\n$breakdown",
                true
            )
        }

        // ── Thu nhập tháng ──────────────────────────────────────
        if (matchesAny(lower, "thu nhập", "lương tháng", "income", "kiếm được bao nhiêu")) {
            val monthIncome = filterByPeriod(transactions, Period.MONTH, TransactionType.INCOME)
            val total = monthIncome.sumOf { it.amount }
            return LocalAnswer(
                "💵 **Thu nhập tháng này:** ${formatCurrency(total)}\n" +
                "(Tổng từ ${monthIncome.size} giao dịch)",
                true
            )
        }

        // ── Mục tiêu tiết kiệm (chi tiết + ETA) ────────────────
        if (matchesAny(lower, "tiết kiệm", "mục tiêu", "savings", "goal", "bao lâu", "còn thiếu")) {
            if (savingsGoals.isEmpty()) {
                return LocalAnswer("Bạn chưa có mục tiêu tiết kiệm nào. Nhắn 'tạo tiết kiệm mua xe 50 triệu' để bắt đầu! 🎯", true)
            }
            val lines = savingsGoals.map { g ->
                val pct = if (g.targetAmount > 0) (g.currentAmount / g.targetAmount * 100).toInt() else 0
                val remaining = (g.targetAmount - g.currentAmount).coerceAtLeast(0.0)
                val bar = buildProgressBar(pct)
                val etaStr = if (g.autoSavingAmount > 0 && remaining > 0) {
                    val weeks = kotlin.math.ceil(remaining / g.autoSavingAmount).toInt()
                    " · ~$weeks tuần nữa"
                } else if (remaining > 0) " · chưa có kế hoạch nạp"
                else " · ✅ Đạt rồi!"
                "${g.iconEmoji} **${g.goalName}** $pct%\n  $bar\n  ${formatCurrency(g.currentAmount)}/${formatCurrency(g.targetAmount)} (còn thiếu ${formatCurrency(remaining)})$etaStr"
            }
            return LocalAnswer("🎯 **Mục tiêu tiết kiệm:**\n\n${lines.joinToString("\n\n")}", true)
        }

        // ── Nợ: ai nợ nhiều nhất? ───────────────────────────────
        if (matchesAny(lower, "ai nợ nhiều", "nợ lớn nhất", "nợ nhiều nhất")) {
            val open = debtLoans.filter { it.type == DebtLoanType.LOAN && !it.isPaid }
            if (open.isEmpty()) return LocalAnswer("Hiện không có ai nợ bạn. 😊", true)
            val top = open.maxByOrNull { it.amount }!!
            val rank = open.sortedByDescending { it.amount }
                .mapIndexed { i, d -> "  ${i+1}. ${d.personName}: ${formatCurrency(d.amount)}" }
                .joinToString("\n")
            return LocalAnswer("🏆 **${top.personName}** đang nợ bạn nhiều nhất (${formatCurrency(top.amount)})\n\nXếp hạng:\n$rank", true)
        }

        // ── Nợ: đến hạn sắp tới ────────────────────────────────
        if (matchesAny(lower, "đến hạn", "hạn trả", "sắp đến hạn", "due")) {
            val now = System.currentTimeMillis()
            val upcoming = debtLoans
                .filter { !it.isPaid && it.dueDate != null }
                .filter { it.dueDate!!.seconds * 1000 > now }
                .sortedBy { it.dueDate!!.seconds }
                .take(5)
            if (upcoming.isEmpty()) return LocalAnswer("Không có khoản nợ nào có hạn trả sắp tới.", true)
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val lines = upcoming.map { d ->
                val daysLeft = ((d.dueDate!!.seconds * 1000 - now) / 86_400_000).toInt()
                val icon = if (daysLeft <= 3) "🔴" else if (daysLeft <= 7) "🟡" else "🟢"
                "$icon ${d.personName} (${if (d.type == DebtLoanType.DEBT) "Bạn nợ" else "Nợ bạn"}): ${formatCurrency(d.amount)} — hạn ${sdf.format(java.util.Date(d.dueDate.seconds * 1000))} (còn ${daysLeft}d)"
            }
            return LocalAnswer("📅 **Khoản nợ sắp đến hạn:**\n${lines.joinToString("\n")}", true)
        }

        // ── Nợ: tổng quan đầy đủ ───────────────────────────────
        if (matchesAny(lower, "nợ", "cho vay", "debt", "vay", "mượn")) {
            val youOwe    = debtLoans.filter { it.type == DebtLoanType.DEBT && !it.isPaid }
            val owedToYou = debtLoans.filter { it.type == DebtLoanType.LOAN && !it.isPaid }
            if (youOwe.isEmpty() && owedToYou.isEmpty()) {
                return LocalAnswer("✅ Bạn không có khoản nợ nào đang mở. Tài chính sạch!", true)
            }
            val sb = StringBuilder()
            if (youOwe.isNotEmpty()) {
                val total = youOwe.sumOf { it.amount }
                sb.appendLine("❗ **Bạn đang nợ** (${formatCurrency(total)}):")
                youOwe.sortedByDescending { it.amount }.forEach {
                    val note = if (it.note.isNotBlank()) " — ${it.note}" else ""
                    sb.appendLine("  • ${it.personName}: ${formatCurrency(it.amount)}$note")
                }
            }
            if (owedToYou.isNotEmpty()) {
                val total = owedToYou.sumOf { it.amount }
                sb.appendLine("\n💵 **Người khác nợ bạn** (${formatCurrency(total)}):")
                owedToYou.sortedByDescending { it.amount }.forEach {
                    val note = if (it.note.isNotBlank()) " — ${it.note}" else ""
                    sb.appendLine("  • ${it.personName}: ${formatCurrency(it.amount)}$note")
                }
            }
            val netBalance = owedToYou.sumOf { it.amount } - youOwe.sumOf { it.amount }
            val netIcon = if (netBalance >= 0) "✅" else "⚠️"
            sb.append("\n$netIcon Net: ${if (netBalance >= 0) "+" else ""}${formatCurrency(netBalance)}")
            return LocalAnswer(sb.toString().trim(), true)
        }

        // ── Ngân sách ───────────────────────────────────────────
        if (matchesAny(lower, "ngân sách", "budget", "hạn mức", "còn bao nhiêu để tiêu")) {
            if (budgets.isEmpty()) {
                return LocalAnswer("Bạn chưa thiết lập hạn mức ngân sách. Nhắn 'đặt ngân sách ăn uống 3 triệu/tháng' để bắt đầu!", true)
            }
            val monthExpenses = filterByPeriod(transactions, Period.MONTH, TransactionType.EXPENSE)
            val lines = budgets.map { budget ->
                val spent = if (budget.category == "Tất cả") {
                    monthExpenses.sumOf { it.amount }
                } else {
                    monthExpenses.filter { it.category == budget.category }.sumOf { it.amount }
                }
                val pct = if (budget.amount > 0) (spent / budget.amount * 100).toInt() else 0
                val icon = if (pct >= 100) "🔴" else if (pct >= 80) "🟡" else "🟢"
                "$icon ${budget.category}: ${formatCurrency(spent)}/${formatCurrency(budget.amount)} ($pct%)"
            }
            return LocalAnswer("📊 **Tình trạng ngân sách:**\n${lines.joinToString("\n")}", true)
        }

        // ── Lịch hôm nay / tuần ────────────────────────────────
        if (matchesAny(lower, "lịch hôm nay", "hôm nay có gì", "lịch chi tiêu", "schedule", "kế hoạch hôm nay")) {
            val cal = Calendar.getInstance()
            val todayDow = cal.get(Calendar.DAY_OF_WEEK).let {
                if (it == Calendar.SUNDAY) 7 else it - 1   // 1=Th2 … 7=CN
            }
            val todayItems = schedule.filter { it.dayOfWeek == todayDow }
            val habitFixed = habit?.fixedCosts?.filter { it.dayOfWeek == todayDow } ?: emptyList()
            val all = todayItems + habitFixed
            if (all.isEmpty()) return LocalAnswer("📅 Hôm nay không có lịch chi tiêu cố định nào. Tự do chi tiêu! 😄", true)
            val lines = all.map { "  • ${it.category}: ${formatCurrency(it.amount)}${if (it.note.isNotBlank()) " (${it.note})" else ""}"
            }
            val total = all.sumOf { it.amount }
            return LocalAnswer("📅 **Lịch hôm nay** (dự kiến ${formatCurrency(total)}):\n${lines.joinToString("\n")}", true)
        }

        // ── Quỹ giữ hộ ─────────────────────────────────────────
        if (matchesAny(lower, "quỹ", "giữ hộ", "held fund", "quỹ nhóm")) {
            val funds = wallet?.heldFunds ?: emptyList()
            if (funds.isEmpty()) return LocalAnswer("Bạn chưa có quỹ giữ hộ nào.", true)
            val total = funds.sumOf { it.amount }
            val lines = funds.joinToString("\n") { "  • ${it.name}: ${formatCurrency(it.amount)}" }
            return LocalAnswer("🏦 **Quỹ giữ hộ** (Tổng: ${formatCurrency(total)}):\n$lines", true)
        }

        // ── So sánh tháng trước ─────────────────────────────────
        if (matchesAny(lower, "tháng trước", "so sánh", "hơn tháng trước", "last month")) {
            val thisMonth = filterByPeriod(transactions, Period.MONTH,      TransactionType.EXPENSE)
            val lastMonth = filterByPeriod(transactions, Period.LAST_MONTH, TransactionType.EXPENSE)
            val thisTotal = thisMonth.sumOf { it.amount }
            val lastTotal = lastMonth.sumOf { it.amount }
            val diff = thisTotal - lastTotal
            val pct  = if (lastTotal > 0) (diff / lastTotal * 100).toInt() else 0
            val icon = if (diff > 0) "⬆️ Tăng" else "⬇️ Giảm"
            return LocalAnswer(
                "📊 **So sánh chi tiêu:**\n" +
                "Tháng này: ${formatCurrency(thisTotal)}\n" +
                "Tháng trước: ${formatCurrency(lastTotal)}\n" +
                "$icon ${kotlin.math.abs(pct)}% (${formatCurrency(kotlin.math.abs(diff))})",
                true
            )
        }

        // ── Chi tiêu lớn nhất ───────────────────────────────────
        if (matchesAny(lower, "chi tiêu lớn nhất", "đắt nhất", "tốn nhất", "top chi tiêu", "lớn nhất")) {
            val month = filterByPeriod(transactions, Period.MONTH, TransactionType.EXPENSE)
            if (month.isEmpty()) return LocalAnswer("Tháng này chưa có giao dịch nào.", true)
            val top5 = month.sortedByDescending { it.amount }.take(5)
            val lines = top5.mapIndexed { i, tx ->
                "  ${i+1}. ${tx.category}${if (tx.note.isNotBlank()) " (${tx.note})" else ""}: ${formatCurrency(tx.amount)}"
            }
            return LocalAnswer("💸 **Top chi tiêu tháng này:**\n${lines.joinToString("\n")}", true)
        }

        // ── Tỉ lệ tiết kiệm ────────────────────────────────────
        if (matchesAny(lower, "tỉ lệ tiết kiệm", "savings rate", "tiết kiệm được bao nhiêu %", "bao nhiêu phần trăm")) {
            val income  = filterByPeriod(transactions, Period.MONTH, TransactionType.INCOME).sumOf { it.amount }
            val expense = filterByPeriod(transactions, Period.MONTH, TransactionType.EXPENSE).sumOf { it.amount }
            if (income == 0.0) return LocalAnswer("Chưa có dữ liệu thu nhập tháng này.", true)
            val saved   = (income - expense).coerceAtLeast(0.0)
            val rate    = (saved / income * 100).toInt()
            val emoji   = if (rate >= 20) "🌟" else if (rate >= 10) "👍" else "⚠️"
            return LocalAnswer(
                "$emoji **Tỉ lệ tiết kiệm tháng này: $rate%**\n" +
                "Thu nhập: ${formatCurrency(income)}\n" +
                "Chi tiêu: ${formatCurrency(expense)}\n" +
                "Tiết kiệm được: ${formatCurrency(saved)}",
                true
            )
        }

        return LocalAnswer("", false)
    }

    private fun buildProgressBar(pct: Int): String {
        val filled = (pct / 10).coerceIn(0, 10)
        return "[" + "█".repeat(filled) + "░".repeat(10 - filled) + "] $pct%"
    }

    // ─── 3. Multi-transaction parsing ────────────────────────────────────────

    /**
     * Tách câu "sáng ăn 30k chiều grab 50k" thành 2 ParsedTransaction riêng biệt.
     * Dùng khi detect có nhiều mốc thời gian hoặc từ nối (rồi, và, sau đó, xong).
     */
    fun parseMultiTransaction(text: String): List<ParsedTransaction> {
        val splitRegex = Regex(
            """(?:(?:^|(?<=\s))(?:sáng|trưa|chiều|tối|khuya|buổi|lúc|sau đó|rồi|và|xong|tiếp theo))""",
            RegexOption.IGNORE_CASE
        )

        val segments = splitRegex.split(text)
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 3 }

        if (segments.size <= 1) return emptyList()

        return segments.mapNotNull { segment ->
            SmartTransactionParser.parse(segment)
        }
    }

    // ─── 4. Budget alerts ────────────────────────────────────────────────────

    data class BudgetAlert(
        val category: String,
        val spent: Double,
        val limit: Double,
        val isExceeded: Boolean
    )

    fun getBudgetAlerts(
        transactions: List<FinanceTransaction>,
        budgets: List<FinanceBudget>,
        threshold: Float = 0.85f
    ): List<BudgetAlert> {
        val monthExpenses = filterByPeriod(transactions, Period.MONTH, TransactionType.EXPENSE)
        return budgets.mapNotNull { budget ->
            val spent = if (budget.category == "Tất cả") {
                monthExpenses.sumOf { it.amount }
            } else {
                monthExpenses.filter { it.category == budget.category }.sumOf { it.amount }
            }
            val ratio = if (budget.amount > 0) spent / budget.amount else 0.0
            if (ratio >= threshold) {
                BudgetAlert(
                    category = budget.category,
                    spent = spent,
                    limit = budget.amount,
                    isExceeded = ratio >= 1.0
                )
            } else null
        }
    }

    // ─── 5. Spending insight summary ────────────────────────────────────────

    data class SpendingInsight(
        val todayTotal: Double,
        val weekTotal: Double,
        val monthTotal: Double,
        val topCategory: String,
        val topCategoryAmount: Double,
        val savingsRate: Double,       // % thu nhập được tiết kiệm
        val comparedToLastWeek: Double // % thay đổi so với tuần trước (+ tăng / - giảm)
    )

    fun getSpendingInsight(transactions: List<FinanceTransaction>): SpendingInsight {
        val today   = filterByPeriod(transactions, Period.TODAY, TransactionType.EXPENSE)
        val week    = filterByPeriod(transactions, Period.WEEK,  TransactionType.EXPENSE)
        val month   = filterByPeriod(transactions, Period.MONTH, TransactionType.EXPENSE)
        val lastWeek = filterByPeriod(transactions, Period.LAST_WEEK, TransactionType.EXPENSE)
        val monthIncome = filterByPeriod(transactions, Period.MONTH, TransactionType.INCOME)

        val monthTotal   = month.sumOf { it.amount }
        val weekTotal    = week.sumOf { it.amount }
        val lastWeekTotal = lastWeek.sumOf { it.amount }

        val topEntry = month.groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .maxByOrNull { it.value }

        val income = monthIncome.sumOf { it.amount }
        val savingsRate = if (income > 0) ((income - monthTotal) / income * 100).coerceIn(-100.0, 100.0) else 0.0

        val weekChange = if (lastWeekTotal > 0) ((weekTotal - lastWeekTotal) / lastWeekTotal * 100) else 0.0

        return SpendingInsight(
            todayTotal   = today.sumOf { it.amount },
            weekTotal    = weekTotal,
            monthTotal   = monthTotal,
            topCategory  = topEntry?.key ?: "Chưa có",
            topCategoryAmount = topEntry?.value ?: 0.0,
            savingsRate  = savingsRate,
            comparedToLastWeek = weekChange
        )
    }

    // ─── 6. Recurring transaction detection ──────────────────────────────────

    data class RecurringPattern(
        val category: String,
        val averageAmount: Double,
        val occurrencesPerMonth: Int,
        val note: String
    )

    fun detectRecurring(transactions: List<FinanceTransaction>): List<RecurringPattern> {
        val month = filterByPeriod(transactions, Period.MONTH, TransactionType.EXPENSE)
        return month.groupBy { it.category }
            .filter { (_, txs) -> txs.size >= 3 } // Ít nhất 3 lần/tháng
            .map { (cat, txs) ->
                RecurringPattern(
                    category = cat,
                    averageAmount = txs.sumOf { it.amount } / txs.size,
                    occurrencesPerMonth = txs.size,
                    note = txs.mapNotNull { it.note.ifBlank { null } }.firstOrNull() ?: ""
                )
            }
            .sortedByDescending { it.occurrencesPerMonth }
            .take(5)
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private enum class Period { TODAY, WEEK, LAST_WEEK, MONTH, LAST_MONTH }

    private fun filterByPeriod(
        transactions: List<FinanceTransaction>,
        period: Period,
        type: TransactionType
    ): List<FinanceTransaction> {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        val from: Long = when (period) {
            Period.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.timeInMillis
            }
            Period.WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.timeInMillis
            }
            Period.LAST_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.add(Calendar.WEEK_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.timeInMillis
            }
            Period.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.timeInMillis
            }
            Period.LAST_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.timeInMillis
            }
        }

        val to: Long = when (period) {
            Period.LAST_WEEK -> {
                val c2 = Calendar.getInstance()
                c2.set(Calendar.DAY_OF_WEEK, c2.firstDayOfWeek)
                c2.set(Calendar.HOUR_OF_DAY, 0); c2.timeInMillis
            }
            Period.LAST_MONTH -> {
                val c2 = Calendar.getInstance()
                c2.set(Calendar.DAY_OF_MONTH, 1)
                c2.set(Calendar.HOUR_OF_DAY, 0); c2.timeInMillis
            }
            else -> now
        }

        return transactions.filter { tx ->
            tx.type == type &&
            tx.timestamp.seconds * 1000 in from..to
        }
    }

    private fun topCategories(txs: List<FinanceTransaction>): String {
        if (txs.isEmpty()) return ""
        return txs.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
            .take(4)
            .joinToString("\n") { (cat, amt) -> "  • $cat: ${formatCurrency(amt)}" }
    }

    private fun matchesAny(text: String, vararg keywords: String): Boolean =
        keywords.any { text.contains(it) }

    private fun formatCurrency(amount: Double): String {
        return when {
            amount >= 1_000_000 -> "${String.format("%.1f", amount / 1_000_000)}tr đ"
            amount >= 1_000     -> "${(amount / 1_000).toInt()}k đ"
            else                -> "${amount.toInt()} đ"
        }
    }
}
