package com.example.finfit.finance.ui.logic

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.finfit.finance.ui.screens.EXPENSE_CATEGORIES
import com.example.finfit.finance.ui.screens.INCOME_CATEGORIES
import com.example.finfit.finance.ui.screens.TRANSFER_CATEGORIES
import java.text.SimpleDateFormat
import java.util.Locale

// ─── Dữ liệu tổng hợp quỹ cá nhân ───────────────────────────────────────────

/**
 * Kết quả tính toán phân bổ quỹ, được tách ra để tránh tính lại mỗi recompose.
 */
data class CalculatedFunds(
    val personal: Double,
    val goal: Double,
    val general: Double,
    val held: Double,
    val total: Double,
    val spendable: Double
)

// ─── Format ngày giao dịch ───────────────────────────────────────────────────

/** Format hiển thị thời gian giao dịch trong danh sách (HH:mm • dd/MM). */
val transactionDateFormat: SimpleDateFormat =
    SimpleDateFormat("HH:mm • dd/MM", Locale.getDefault())

// ─── Tra cứu icon danh mục ───────────────────────────────────────────────────

/**
 * Trả về icon phù hợp với tên danh mục giao dịch.
 * Tìm trong cả 3 danh sách: Chi tiêu, Thu nhập, Chuyển khoản.
 */
fun getCategoryIcon(category: String): ImageVector =
    (EXPENSE_CATEGORIES + INCOME_CATEGORIES + TRANSFER_CATEGORIES)
        .find { it.label == category }?.icon
        ?: Icons.Default.Receipt
