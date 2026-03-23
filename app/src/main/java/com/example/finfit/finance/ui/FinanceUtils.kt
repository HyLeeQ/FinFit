package com.example.finfit.finance.ui

import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import java.util.Locale

/** Hỗ trợ định dạng tiền tệ VNĐ */
fun formatCurrency(amount: Double): String {
    val fmt = NumberFormat.getInstance(Locale("vi", "VN"))
    fmt.maximumFractionDigits = 0
    return "${fmt.format(amount)} đ"
}

/** Trả về danh sách màu gradient tương ứng với chỉ số màu */
fun cardGradient(colorIndex: Int, bankColorHex: Long): List<Color> {
    val presets = listOf(
        listOf(Color(0xFF2D82FE), Color(0xFF1E40AF)),
        listOf(Color(0xFF10C67F), Color(0xFF065F46)),
        listOf(Color(0xFFF59E0B), Color(0xFFB45309)),
        listOf(Color(0xFF8B5CF6), Color(0xFF5B21B6)),
        listOf(Color(0xFFEF4444), Color(0xFF991B1B)),
        listOf(Color(0xFF0EA5E9), Color(0xFF0369A1)),
    )
    return presets.getOrElse(colorIndex) {
        val base = Color(bankColorHex)
        listOf(base, base.copy(red = (base.red * 0.7f).coerceIn(0f, 1f)))
    }
}

/** Chuyển màu hex ngân hàng → chỉ số thẻ màu gần nhất */
fun cardGradientIndex(bankColorHex: Long): Int {
    return when (bankColorHex) {
        0xFF059669L, 0xFF007A33L, 0xFF006838L, 0xFF00A651L, 0xFF009B4DL -> 1 // green
        0xFFF59E0B -> 2   // orange
        0xFFAE1F7EL, 0xFF6B21A8L -> 3  // purple
        0xFFE31837L, 0xFFDC2626L -> 4  // red
        0xFF0068FFL -> 5  // light blue
        else -> 0         // default blue
    }
}
