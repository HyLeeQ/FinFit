package com.example.finfit.finance.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.finance.ui.utils.formatCurrency
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

// ─── Colors ──────────────────────────────────────────────────────────────────
val tealColor = Color(0xFF005F73)
val tealLightColor = Color(0xFF0A9396)

// ─── Placeholder chọn nguồn ảnh ──────────────────────────────────────────────
@Composable
fun BillPickerPlaceholder(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    teal: Color = tealColor,
    tealLight: Color = tealLightColor
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(teal.copy(alpha = 0.15f))
                .border(2.dp, teal.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                tint = tealLight,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Quét hoá đơn",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Chụp ảnh bill hoặc chọn từ thư viện\nFinFit sẽ tự động đọc số tiền cho bạn",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(36.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(teal.copy(alpha = 0.12f))
                    .border(1.5.dp, teal.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .clickable { onCamera() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(teal.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, null, tint = tealLight, modifier = Modifier.size(24.dp))
                    }
                    Text("Chụp ảnh", color = tealLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .clickable { onGallery() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Photo, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
                    }
                    Text("Thư viện", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun SmallActionBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ─── OCR Engine: ML Kit + Smart Extraction ───────────────────────────────────
fun runOcr(
    bitmap: Bitmap,
    context: android.content.Context? = null,
    uri: android.net.Uri? = null,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    val inputImage = if (uri != null && context != null) {
        try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            InputImage.fromBitmap(bitmap, 0)
        }
    } else {
        InputImage.fromBitmap(bitmap, 0)
    }

    recognizer.process(inputImage)
        .addOnSuccessListener { visionText ->
            val rawText = visionText.text
            if (rawText.isBlank()) {
                onError("Không đọc được chữ trong ảnh")
                return@addOnSuccessListener
            }
            val amount = extractSmartAmount(rawText)
            if (amount != null && amount > 0) {
                onSuccess(amount.toLong().toString())
            } else {
                onError("Không tìm thấy số tiền")
            }
        }
        .addOnFailureListener {
            onError("Lỗi đọc ảnh: ${it.message}")
        }
}

private fun extractSmartAmount(text: String): Double? {
    val lines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }

    val highPriorityKeywords = listOf(
        "phải trả", "tổng cộng", "tổng thanh toán", "total amount", "grand total",
        "amount due", "total due", "to pay", "thành tiền", "tổng tiền", "thanh toan", "tong cong"
    )
    val midPriorityKeywords = listOf(
        "tổng", "total", "cộng", "subtotal", "sum", "tong"
    )
    val ignoreKeywords = listOf(
        "khách đưa", "tiền mặt", "cash", "tiền thừa", "thối lại", "change", "trả lại", "thẻ", "card"
    )

    val numberPattern = Regex("""(?<!\d)(\d{1,3}(?:[., ]\d{3}){1,4}|\d{4,8})(?:\s*(?:đ|vnd|vnđ|₫))?(?!\d)""", RegexOption.IGNORE_CASE)

    fun parseNumber(match: MatchResult): Double? {
        val raw = match.groups[1]?.value ?: return null
        
        if (raw.startsWith("0")) return null
        
        if (!raw.contains(".") && !raw.contains(",") && !raw.contains(" ") && raw.length > 8) return null

        val cleaned = raw.trim()
        return when {
            cleaned.matches(Regex("""\d{1,3}(\.\d{3})+""")) -> cleaned.replace(".", "").toDoubleOrNull()
            cleaned.matches(Regex("""\d{1,3}(,\d{3})+""")) -> cleaned.replace(",", "").toDoubleOrNull()
            cleaned.matches(Regex("""\d{1,3}( \d{3})+""")) -> cleaned.replace(" ", "").toDoubleOrNull()
            cleaned.matches(Regex("""\d{1,3}(\.\d{3})+,\d{1,2}""")) -> cleaned.replace(".", "").replace(",", ".").toDoubleOrNull()
            else -> cleaned.filter { it.isDigit() }.toDoubleOrNull()
        }
    }

    fun getValidNumbers(line: String): List<Double> {
        if (ignoreKeywords.any { line.lowercase().contains(it) }) return emptyList()
        return numberPattern.findAll(line.lowercase())
            .mapNotNull { parseNumber(it) }
            .filter { it in 1_000.0..500_000_000.0 }
            .toList()
    }

    for (i in lines.indices) {
        val lower = lines[i].lowercase()
        if (highPriorityKeywords.any { lower.contains(it) }) {
            val sameLineNums = getValidNumbers(lines[i])
            if (sameLineNums.isNotEmpty()) return sameLineNums.maxOrNull()

            if (i + 1 < lines.size) {
                val nextLineNums = getValidNumbers(lines[i + 1])
                if (nextLineNums.isNotEmpty()) return nextLineNums.maxOrNull()
            }
        }
    }

    for (i in lines.indices) {
        val lower = lines[i].lowercase()
        if (midPriorityKeywords.any { lower.contains(it) }) {
            val sameLineNums = getValidNumbers(lines[i])
            if (sameLineNums.isNotEmpty()) return sameLineNums.maxOrNull()

            if (i + 1 < lines.size) {
                val nextLineNums = getValidNumbers(lines[i + 1])
                if (nextLineNums.isNotEmpty()) return nextLineNums.maxOrNull()
            }
        }
    }

    val validNumbers: List<Triple<Double, Boolean, Boolean>> = lines.flatMap { line -> 
        if (ignoreKeywords.any { line.lowercase().contains(it) }) emptyList()
        else {
            numberPattern.findAll(line.lowercase()).mapNotNull { match ->
                val num = parseNumber(match)
                if (num != null && num in 1_000.0..500_000_000.0) {
                    val fullMatch = match.value.lowercase()
                    val hasCurrency = fullMatch.contains("đ") || fullMatch.contains("vnd") || fullMatch.contains("₫")
                    val hasSeparator = match.groups[1]?.value?.let { it.contains(".") || it.contains(",") } == true
                    
                    Triple(num, hasCurrency, hasSeparator)
                } else null
            }.toList()
        }
    }

    if (validNumbers.isEmpty()) return null

    val p1 = validNumbers.filter { it.second && it.third }.maxByOrNull { it.first }?.first
    if (p1 != null) return p1

    val p2 = validNumbers.filter { it.third }.maxByOrNull { it.first }?.first
    if (p2 != null) return p2

    val p3 = validNumbers.filter { it.second }.maxByOrNull { it.first }?.first
    if (p3 != null) return p3

    return validNumbers.maxByOrNull { it.first }?.first
}
