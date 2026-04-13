package com.example.finfit.finance.ui.utils

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

/** Hỗ trợ định dạng tiền tệ VNĐ */
fun formatCurrency(amount: Double): String {
    val symbols = java.text.DecimalFormatSymbols(java.util.Locale.US).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    val fmt = java.text.DecimalFormat("#,###", symbols)
    return "${fmt.format(amount)} đ"
}

/**
 * Format số tiền theo chuẩn VN khi nhập: 1.000.000
 * Dùng trong TextField khi người dùng đang gõ.
 */
fun formatAmountInput(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    if (digits.isBlank()) return ""
    val number = digits.toLongOrNull() ?: return digits
    val symbols = java.text.DecimalFormatSymbols(java.util.Locale.US).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    return java.text.DecimalFormat("#,###", symbols).format(number)
}

/**
 * Chuyển chuỗi đã format (1.000.000) về Double
 */
fun parseAmountInput(formatted: String): Double {
    return formatted.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
}

/**
 * TextField dùng chung cho nhập số tiền VNĐ.
 * Tự động hiển thị dấu chấm mỗi 3 chữ số (1.000.000).
 * State bên ngoài nên là raw digits (chỉ số, không dấu).
 *
 * @param rawValue  Chuỗi raw digits từ state ngoài (vd: "50000")
 * @param onValueChange  Trả về raw digits mới khi người dùng thay đổi
 */
@Composable
fun VnAmountTextField(
    rawValue: String,
    onValueChange: (String) -> Unit,
    label: String = "Số tiền (đ)",
    modifier: Modifier = Modifier,
    suffix: String = "đ",
    singleLine: Boolean = true
) {
    val displayValue = formatAmountInput(rawValue)
    OutlinedTextField(
        value = displayValue,
        onValueChange = { input ->
            // Chỉ giữ lại chữ số
            onValueChange(input.filter { it.isDigit() })
        },
        label = { Text(label) },
        suffix = if (suffix.isNotEmpty()) {{ Text(suffix) }} else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        singleLine = singleLine
    )
}


/** 
 * Một component hỗ trợ hiển thị số tiền có hiệu ứng nhảy số mượt mà 
 */
@Composable
fun AnimatedAmountText(
    amount: Double,
    isHidden: Boolean,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    isMaskedAll: Boolean = false 
) {
    if (isHidden) {
        Text(
            text = if (isMaskedAll) "****" else "********",
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight
        )
    } else {
        val animatedAmount by animateFloatAsState(
            targetValue = amount.toFloat(),
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            label = "AmountAnimation"
        )
        Text(
            text = formatCurrency(animatedAmount.toDouble()),
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight
        )
    }
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
