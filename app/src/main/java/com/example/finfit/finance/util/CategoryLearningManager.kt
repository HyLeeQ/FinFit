package com.example.finfit.finance.util

import com.example.finfit.finance.model.CategoryRule
import java.util.Locale

object CategoryLearningManager {

    // Default learned rules dictionary
    private val DEFAULT_PATTERNS = mapOf(
        // Ăn uống
        "cafe" to "Ăn uống", "coffee" to "Ăn uống", "highlands" to "Ăn uống", "phúc long" to "Ăn uống",
        "starbucks" to "Ăn uống", "trà sữa" to "Ăn uống", "cơm" to "Ăn uống", "phở" to "Ăn uống",
        "bún" to "Ăn uống", "bánh mì" to "Ăn uống", "lẩu" to "Ăn uống", "nướng" to "Ăn uống",
        "nhà hàng" to "Ăn uống", "quán ăn" to "Ăn uống", "kfc" to "Ăn uống", "lotteria" to "Ăn uống",
        "mcdonald" to "Ăn uống", "pizza" to "Ăn uống", "buffet" to "Ăn uống", "ăn sáng" to "Ăn uống",
        "ăn trưa" to "Ăn uống", "ăn tối" to "Ăn uống", "snack" to "Ăn uống", "ăn vặt" to "Ăn uống",

        // Mua sắm & Đi chợ
        "winmart" to "Mua sắm", "coopmart" to "Mua sắm", "bách hóa xanh" to "Mua sắm",
        "circle k" to "Mua sắm", "familymart" to "Mua sắm", "7 eleven" to "Mua sắm",
        "shopee" to "Mua sắm", "lazada" to "Mua sắm", "tiki" to "Mua sắm", "tiktok shop" to "Mua sắm",
        "siêu thị" to "Mua sắm", "đi chợ" to "Mua sắm", "quần áo" to "Mua sắm", "giày" to "Mua sắm",
        "uniql" to "Mua sắm", "zara" to "Mua sắm",

        // Di chuyển
        "grab" to "Di chuyển", "be " to "Di chuyển", "be bike" to "Di chuyển", "be car" to "Di chuyển",
        "gojek" to "Di chuyển", "xanh sm" to "Di chuyển", "taxi" to "Di chuyển", "xăng" to "Di chuyển",
        "petrolimex" to "Di chuyển", "gửi xe" to "Di chuyển", "vé xe" to "Di chuyển", "vé máy bay" to "Di chuyển",
        "thu phí" to "Di chuyển", "vETC" to "Di chuyển", "ePass" to "Di chuyển",

        // Nhà cửa & Tiện ích
        "tiền điện" to "Nhà cửa", "tiền nước" to "Nhà cửa", "tiền mạng" to "Nhà cửa",
        "tiền nhà" to "Nhà cửa", "tiền trọ" to "Nhà cửa", "internet" to "Nhà cửa", "viettel" to "Nhà cửa",
        "fpt" to "Nhà cửa", "vnpt" to "Nhà cửa", "điện lực" to "Nhà cửa", "phí quản lý" to "Nhà cửa",

        // Giải trí
        "netflix" to "Giải trí", "spotify" to "Giải trí", "youtube" to "Giải trí",
        "cgv" to "Giải trí", "lotte cinema" to "Giải trí", "bhd" to "Giải trí", "galaxy cinema" to "Giải trí",
        "game" to "Giải trí", "steam" to "Giải trí", "playstation" to "Giải trí", "bida" to "Giải trí",
        "karaoke" to "Giải trí", "du lịch" to "Giải trí", "khách sạn" to "Giải trí",

        // Sức khỏe
        "thuốc" to "Sức khỏe", "pharmacity" to "Sức khỏe", "long châu" to "Sức khỏe",
        "an khang" to "Sức khỏe", "khám bệnh" to "Sức khỏe", "bệnh viện" to "Sức khỏe",
        "nha khoa" to "Sức khỏe", "gym" to "Sức khỏe", "yoga" to "Sức khỏe", "cắt tóc" to "Sức khỏe",
        "spa" to "Sức khỏe", "mỹ phẩm" to "Sức khỏe",

        // Giáo dục
        "học phí" to "Giáo dục", "sách" to "Giáo dục", "khóa học" to "Giáo dục", "tiếng anh" to "Giáo dục",

        // Thu nhập
        "lương" to "Lương", "salary" to "Lương", "thưởng" to "Thưởng", "bonus" to "Thưởng",
        "lãi" to "Đầu tư", "cổ tức" to "Đầu tư", "bán đồ" to "Thu nhập phụ"
    )

    // Bộ nhớ đệm các rule do người dùng custom
    private val dynamicRules = mutableMapOf<String, String>()

    fun initUserRules(rules: List<CategoryRule>) {
        dynamicRules.clear()
        rules.forEach { rule ->
            if (rule.keyword.isNotBlank() && rule.category.isNotBlank()) {
                dynamicRules[rule.keyword.lowercase().trim()] = rule.category
            }
        }
    }

    /**
     * Tự động dự đoán danh mục phù hợp nhất từ nội dung ghi chú hoặc text OCR
     */
    fun predictCategory(noteOrMerchant: String): String? {
        val text = noteOrMerchant.lowercase().trim()
        if (text.isBlank()) return null

        // 1. Ưu tiên các rule người dùng đã dạy học
        for ((keyword, cat) in dynamicRules) {
            if (text.contains(keyword)) return cat
        }

        // 2. Tìm trong danh sách mặc định
        for ((keyword, cat) in DEFAULT_PATTERNS) {
            if (text.contains(keyword)) return cat
        }

        return null
    }

    /**
     * Học pattern mới khi người dùng sửa danh mục
     */
    fun learnPattern(note: String, category: String): CategoryRule? {
        val cleanNote = note.lowercase().trim()
        if (cleanNote.length < 2 || category.isBlank()) return null

        // Tách các từ khóa có nghĩa (lấy cụm từ hoặc từ chính)
        val candidateKeyword = extractMainKeyword(cleanNote)
        if (candidateKeyword.isNotBlank()) {
            dynamicRules[candidateKeyword] = category
            return CategoryRule(
                id = java.util.UUID.randomUUID().toString(),
                keyword = candidateKeyword,
                category = category,
                confidenceCount = 1
            )
        }
        return null
    }

    private fun extractMainKeyword(text: String): String {
        val words = text.split(" ", ",", "-", ".", "/")
            .map { it.trim() }
            .filter { it.length >= 2 && !it.all { char -> char.isDigit() } }
        
        return if (words.size >= 2) {
            words.take(2).joinToString(" ")
        } else if (words.isNotEmpty()) {
            words.first()
        } else {
            text
        }
    }
}
