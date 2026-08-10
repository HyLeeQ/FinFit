package com.example.finfit.finance.util

/**
 * Parser NLP local: phân tích câu tiếng Việt để trích xuất thông tin giao dịch.
 * Không cần API, chạy hoàn toàn on-device.
 *
 * Ví dụ:
 *  "ăn tối 20k"       → amount=20000, category=Ăn uống, note=ăn tối, type=EXPENSE
 *  "lương 5tr"        → amount=5000000, category=Lương, type=INCOME
 *  "đổ xăng 80.000"   → amount=80000,  category=Di chuyển, type=EXPENSE
 *  "tiền điện 250k"   → amount=250000, category=Hoá đơn & Tiện ích, type=EXPENSE
 */
data class ParsedTransaction(
    val amount: Double,
    val category: String,
    val note: String,
    val type: String,   // "EXPENSE" | "INCOME"
    val confidence: Float,  // 0..1
    val accountId: String? = null
)

object SmartTransactionParser {

    // ─── Từ điển danh mục ──────────────────────────────────────────────────
    private val EXPENSE_KEYWORDS = mapOf(
        "Ăn uống" to listOf(
            "ăn", "uống", "cơm", "bữa", "sáng", "trưa", "tối", "khuya",
            "cafe", "cà phê", "trà", "coffee", "phở", "bún", "bánh",
            "pizza", "gà", "lẩu", "nướng", "hải sản", "sushi", "nhậu",
            "bia", "snack", "nước", "trà sữa", "sinh tố"
        ),
        "Di chuyển" to listOf(
            "xăng", "grab", "taxi", "xe", "uber", "gojek", "bus", "xe buýt",
            "vé", "tàu", "máy bay", "vé tàu", "đỗ xe", "bãi xe",
            "gửi xe", "grab food", "ship"
        ),
        "Mua sắm" to listOf(
            "mua", "sắm", "quần", "áo", "giày", "dép", "túi", "ví",
            "điện thoại", "laptop", "máy tính", "tai nghe", "sạc",
            "phụ kiện", "hàng", "online", "shopee", "lazada", "tiki"
        ),
        "Giải trí" to listOf(
            "phim", "cinema", "cgv", "lotte", "galaxy", "game", "netflix",
            "spotify", "youtube", "hát", "karaoke", "bar", "club",
            "du lịch", "đi chơi", "tham quan", "vui chơi"
        ),
        "Sức khỏe" to listOf(
            "thuốc", "khám", "bệnh viện", "phòng khám", "bác sĩ", "y tế",
            "thể dục", "gym", "yoga", "tập", "vitamin", "dược"
        ),
        "Hoá đơn & Tiện ích" to listOf(
            "điện", "nước", "internet", "wifi", "điện thoại", "bill",
            "hoá đơn", "hóa đơn", "tiền nhà", "thuê nhà", "rent",
            "gas", "truyền hình", "cáp", "phí"
        ),
        "Giáo dục" to listOf(
            "học", "sách", "khoá", "khóa", "lớp", "trường", "học phí",
            "gia sư", "online course", "udemy", "coursera"
        ),
        "Làm đẹp" to listOf(
            "cắt tóc", "tóc", "spa", "nail", "make up", "mỹ phẩm",
            "dưỡng da", "serum", "kem", "lotion"
        ),
        "Khác" to emptyList()
    )

    private val INCOME_KEYWORDS = mapOf(
        "Lương" to listOf("lương", "salary", "thu nhập", "tiền lương", "paycheck"),
        "Thưởng" to listOf("thưởng", "bonus", "thưởng tết"),
        "Freelance" to listOf("freelance", "làm thêm", "part time", "hoa hồng", "commission"),
        "Cho thuê" to listOf("cho thuê", "tiền thuê", "rent income"),
        "Đầu tư" to listOf("cổ phiếu", "crypto", "lợi nhuận", "lãi", "đầu tư"),
        "Thu khác" to emptyList()
    )

    private val INCOME_TRIGGERS = listOf(
        "nhận", "lĩnh", "được", "thu", "hoàn", "hoàn tiền", "refund",
        "lương", "thưởng", "bonus", "hoa hồng", "lãi", "cổ tức"
    )

    // ─── Phân tích số tiền ─────────────────────────────────────────────────
    private val AMOUNT_REGEX = Regex(
        """(\d[\d.,]*)""" +          // phần số
        """\s*(k|K|nghìn|ngàn|tr|triệu|m|M|đ|đồng|vnd|vn|d)?""",
        RegexOption.IGNORE_CASE
    )

    fun parseAmount(text: String): Double? {
        val matches = AMOUNT_REGEX.findAll(text).toList()
        if (matches.isEmpty()) return null

        var total = 0.0
        var foundValid = false
        for (match in matches) {
            val rawNum = match.groupValues[1].replace(",", ".").replace(".", "")
            val number = rawNum.toDoubleOrNull() ?: continue
            val unit = match.groupValues[2].lowercase()
            
            // Loại bỏ các con số ngẫu nhiên: Chỉ coi là tiền nếu có hậu tố (k, tr, đ) HOẶC số >= 1000
            // Hạn chế dính năm (VD: 2024) trừ khi nó là số tiền duy nhất hoặc rõ ràng. Nhưng tạm thời số >= 1000 cứ cộng vào.
            if (unit.isNotBlank() || number >= 1000) {
                val valInVND = when {
                    unit.startsWith("tr") || unit == "m" -> number * 1_000_000
                    unit == "k" || unit.startsWith("ng") -> number * 1_000
                    else -> number
                }
                
                // Mẹo nhỏ chống cộng nhầm năm (VD: 2024) nếu nó không có đơn vị và trùng khoảng năm hiện tại.
                if (unit.isBlank() && valInVND in 2000.0..2100.0 && matches.size > 1) {
                    continue // Khả năng cao đây là năm báo cáo "ngày 12 tháng 5 2024 ăn 50k", bỏ qua 2024.
                }

                total += valInVND
                foundValid = true
            }
        }
        return if (foundValid) total else null
    }

    // ─── Hàm chính ─────────────────────────────────────────────────────────
    fun parse(input: String): ParsedTransaction? {
        val lower = input.lowercase().trim()

        // ── Bộ lọc nhanh: không parse biểu thức toán hoặc câu hỏi ──────────
        // Nếu chứa toán tử hoặc dấu hỏi mà không có từ khóa tài chính → bỏ qua
        val mathChars = setOf('+', '-', '*', '/', '=', '?', '^', '%')
        val hasMathOp = lower.any { it in mathChars }

        // Chuẩn hoá text bằng cách thêm khoảng trắng ở 2 đầu và bỏ các dấu câu để tìm từ chính xác
        val searchString = " " + lower.replace(Regex("\\p{Punct}"), " ") + " "

        // ── Bộ lọc nhanh: không chuyển giao cục bộ nếu có từ khóa Nợ/Vay/Tiết kiệm/Kế hoạch ──
        val aiTriggers = listOf("vay", "nợ", "mượn", "tiết kiệm", "ngân sách", "mục tiêu", "trả", "vây", "mai", "sẽ", "kế hoạch", "định", "dự kiến")
        if (aiTriggers.any { searchString.contains(" $it ") }) return null

        // Phải có ít nhất 1 trong: đơn vị tiền (k/tr/đ/vnd) HOẶC từ khóa tài chính
        val hasMoneyUnit = Regex("""(\d)(k|K|nghìn|ngàn|tr|triệu|đ|vnd|m)\b""").containsMatchIn(lower)
        val hasFinanceKeyword = (EXPENSE_KEYWORDS.values.flatten() + INCOME_KEYWORDS.values.flatten() +
            INCOME_TRIGGERS).any { searchString.contains(" $it ") }

        if (hasMathOp && !hasMoneyUnit && !hasFinanceKeyword) return null
        if (!hasMoneyUnit && !hasFinanceKeyword) return null

        // 1. Tìm số tiền
        val amount = parseAmount(lower) ?: return null
        // Số tiền tối thiểu 1.000đ để tránh parse số lẻ ngẫu nhiên
        if (amount < 1_000) return null

        // 2. Xác định INCOME hay EXPENSE
        val isIncome = INCOME_TRIGGERS.any { searchString.contains(" $it ") }

        // 3. Xác định category
        val keywords = if (isIncome) INCOME_KEYWORDS else EXPENSE_KEYWORDS
        val category = keywords.entries.firstOrNull { (_, kws) ->
            kws.any { searchString.contains(" $it ") }
        }?.key ?: if (isIncome) "Thu khác" else "Khác"

        // 4. Tạo note từ phần text còn lại (bỏ số và đơn vị)
        val note = AMOUNT_REGEX.replace(lower, "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .replaceFirstChar { it.uppercaseChar() }
            .ifBlank { category }

        // 5. Tính confidence
        val hasExplicitKeyword = keywords.entries.any { (_, kws) ->
            kws.any { searchString.contains(" $it ") }
        }
        val confidence = if (hasExplicitKeyword) 0.9f else 0.6f

        return ParsedTransaction(
            amount = amount,
            category = category,
            note = note,
            type = if (isIncome) "INCOME" else "EXPENSE",
            confidence = confidence
        )
    }

    /** Trích xuất số tiền lớn nhất từ ảnh bill (output của OCR) */
    fun extractTotalFromBill(ocrText: String): Double? {
        val lines = ocrText.lines()
        // Tìm dòng nào có "tổng", "total", "thành tiền", "cộng"
        val totalLine = lines.firstOrNull { line ->
            val l = line.lowercase()
            l.contains("tổng") || l.contains("total") ||
            l.contains("thành tiền") || l.contains("cộng") ||
            l.contains("grand") || l.contains("amount due")
        }

        // Nếu tìm thấy → parse dòng đó
        if (totalLine != null) {
            val amt = parseAmount(totalLine)
            if (amt != null && amt > 0) return amt
        }

        // Fallback: lấy số lớn nhất trong toàn bộ text
        val allAmounts = AMOUNT_REGEX.findAll(ocrText)
            .mapNotNull { match ->
                val rawNum = match.groupValues[1].replace(",", ".").replace(".", "")
                val number = rawNum.toDoubleOrNull() ?: return@mapNotNull null
                val unit = match.groupValues[2].lowercase()
                when {
                    unit.startsWith("tr") || unit == "m" -> number * 1_000_000
                    unit == "k" || unit.startsWith("ng") -> number * 1_000
                    else -> number
                }
            }
            .filter { it > 1000 }  // Loại bỏ số quá nhỏ (số lượng, %)
            .toList()

        return allAmounts.maxOrNull()
    }

    /** Phân tích vay nợ local */
    fun parseDebt(input: String): com.example.finfit.finance.model.DebtLoan? {
        val lower = input.lowercase().trim()
        val searchString = " " + lower.replace(Regex("\\p{Punct}"), " ") + " "
        
        var isLoan = false
        var isDebt = false
        
        if (searchString.contains(" cho vay ") || searchString.contains(" cho mượn ")) {
            isLoan = true
        } else if (searchString.contains(" vay của ") || searchString.contains(" mượn của ")) {
            isDebt = true
        } else {
            val debtWords = listOf("nợ", "vay", "mượn")
            var index = -1
            for (w in debtWords) {
                index = searchString.indexOf(" $w ")
                if (index != -1) break
            }
            
            if (index != -1) {
                val prefix = searchString.substring(0, index).trim()
                val lastWordBefore = prefix.substringAfterLast(" ").trim()
                val selfPronouns = listOf("mình", "tôi", "tao", "t", "em", "anh", "chị", "cháu", "con", "")
                
                if (selfPronouns.contains(lastWordBefore)) {
                    isDebt = true
                } else {
                    isLoan = true
                }
            }
        }
        
        if (!isLoan && !isDebt) return null
        
        val amount = parseAmount(lower) ?: return null
        if (amount < 1000) return null
        
        var name = AMOUNT_REGEX.replace(lower, "")
        val allTriggers = listOf("cho vay", "cho mượn", "vay của", "mượn của", "cho", "của", "tôi", "mình", "tao", "t", "nợ", "vay", "mượn")
        for (t in allTriggers) {
            name = name.replace(Regex("\\b$t\\b", RegexOption.IGNORE_CASE), "")
        }
        name = name.replace(Regex("\\s+"), " ").trim().split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        
        if (name.isBlank()) name = "Người lạ"
        
        return com.example.finfit.finance.model.DebtLoan(
            personName = name,
            amount = amount,
            type = if (isLoan) com.example.finfit.finance.model.DebtLoanType.LOAN else com.example.finfit.finance.model.DebtLoanType.DEBT,
            note = "Ghi nhanh"
        )
    }

    sealed class ParsedCommand {
        data class DepositSavings(val goalName: String, val amount: Double) : ParsedCommand()
        data class WithdrawSavings(val goalName: String, val amount: Double) : ParsedCommand()
        data class AddSavingsGoal(val goalName: String, val targetAmount: Double) : ParsedCommand()
        data class AddBudget(val category: String, val amount: Double) : ParsedCommand()
        data class AddGroupSplitBill(val amount: Double, val participantCount: Int, val category: String) : ParsedCommand()
        data class AddHeldFund(val fundName: String, val amount: Double) : ParsedCommand()
    }

    private fun extractNameForCommand(input: String, removeWords: List<String>): String {
        var name = AMOUNT_REGEX.replace(input, "")
        for (w in removeWords) {
            name = name.replace(Regex("\\b$w\\b", RegexOption.IGNORE_CASE), "")
        }
        return name.replace(Regex("\\s+"), " ").trim().split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    fun parseCommand(input: String): ParsedCommand? {
        val lower = input.lowercase().trim()
        val searchString = " " + lower.replace(Regex("\\p{Punct}"), " ") + " "
        val amount = parseAmount(lower) ?: return null

        // 1. Chia tiền nhóm
        if (searchString.contains(" chia ") || searchString.contains(" split ") || searchString.contains(" campuchia ")) {
            val pCountRegex = Regex("""(\d+)\s*(người|ng|đứa|bạn)""")
            val countMatch = pCountRegex.find(lower)
            val count = countMatch?.groupValues?.get(1)?.toIntOrNull() ?: 2
            
            val category = EXPENSE_KEYWORDS.entries.firstOrNull { (_, kws) ->
                kws.any { searchString.contains(" $it ") }
            }?.key ?: "Khác"

            return ParsedCommand.AddGroupSplitBill(amount, count, category)
        }

        // 2. Ngân sách
        if (searchString.contains(" ngân sách ") || searchString.contains(" hạn mức ") || searchString.contains(" budget ")) {
            val category = EXPENSE_KEYWORDS.entries.firstOrNull { (_, kws) ->
                kws.any { searchString.contains(" $it ") }
            }?.key ?: "Tất cả"
            return ParsedCommand.AddBudget(category, amount)
        }

        // 3. Nạp tiết kiệm
        if (searchString.contains(" nạp ") || searchString.contains(" thêm ") || searchString.contains(" bỏ ống ")) {
            if (searchString.contains(" quỹ ") || searchString.contains(" tiết kiệm ") || searchString.contains(" mục tiêu ")) {
                var name = extractNameForCommand(lower, listOf("nạp", "thêm", "vào", "tiền", "quỹ", "tiết kiệm", "mục tiêu", "cho", "ống", "heo"))
                if (name.isBlank()) name = "Quỹ Tiết Kiệm"
                return ParsedCommand.DepositSavings(name, amount)
            }
        }

        // 4. Rút tiết kiệm
        if (searchString.contains(" rút ") || searchString.contains(" lấy ") || searchString.contains(" đập heo ")) {
            if (searchString.contains(" quỹ ") || searchString.contains(" tiết kiệm ") || searchString.contains(" mục tiêu ") || searchString.contains(" đập heo ")) {
                var name = extractNameForCommand(lower, listOf("rút", "lấy", "từ", "tiền", "quỹ", "tiết kiệm", "mục tiêu", "ra", "đập", "heo"))
                if (name.isBlank()) name = "Quỹ Tiết Kiệm"
                return ParsedCommand.WithdrawSavings(name, amount)
            }
        }

        // 5. Quỹ giữ hộ
        if (searchString.contains(" giữ hộ ") || searchString.contains(" thu quỹ ") || searchString.contains(" quỹ lớp ") || searchString.contains(" quỹ nhóm ")) {
            var name = extractNameForCommand(lower, listOf("thu", "tiền", "giữ", "hộ", "quỹ", "nhóm", "lớp"))
            if (name.isBlank()) name = "Quỹ giữ hộ"
            else name = "Quỹ $name"
            return ParsedCommand.AddHeldFund(name, amount)
        }
        
        // 6. Tạo mục tiêu tiết kiệm (phải check sau cùng để tránh đụng độ nạp/rút)
        if (searchString.contains(" tạo ") || searchString.contains(" lập ") || searchString.contains(" mục tiêu ") || searchString.contains(" tiết kiệm ")) {
            if (searchString.contains(" quỹ ") || searchString.contains(" tiết kiệm ") || searchString.contains(" mục tiêu ")) {
                var name = extractNameForCommand(lower, listOf("tạo", "lập", "tiền", "quỹ", "tiết kiệm", "mục tiêu", "mới", "để"))
                if (name.isBlank()) name = "Mục tiêu mới"
                return ParsedCommand.AddSavingsGoal(name, amount)
            }
        }

        return null
    }
}
