package com.example.finfit.finance.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.finfit.finance.ui.utils.formatCurrency
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

// ─── Bill Scanner Screen ──────────────────────────────────────────────────────
@Composable
fun BillScannerScreen(
    onBack: () -> Unit,
    onConfirm: (amount: Double, note: String, imageUri: Uri?) -> Unit
) {
    val context = LocalContext.current

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var detectedAmount by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var scanError by remember { mutableStateOf<String?>(null) }
    var showResult by remember { mutableStateOf(false) }

    // Tạo file URI cho ảnh full resolution (TakePicture cần URI đích)
    var cameraFileUri by remember { mutableStateOf<Uri?>(null) }

    fun createCameraFileUri(): Uri {
        val dir = java.io.File(context.cacheDir, "bill_images").also { it.mkdirs() }
        val file = java.io.File(dir, "bill_${System.currentTimeMillis()}.jpg")
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun processUriForOcr(uri: Uri) {
        isScanning = true
        showResult = false
        scanError = null
        detectedAmount = ""
        try {
            val bmp = android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            bitmap = bmp
            runOcr(bmp,
                onSuccess = { amount ->
                    detectedAmount = amount
                    amountInput = amount
                    isScanning = false
                    showResult = true
                },
                onError = { err ->
                    scanError = err
                    isScanning = false
                    showResult = true
                }
            )
        } catch (e: Exception) {
            scanError = "Lỗi đọc ảnh: ${e.message}"
            isScanning = false
            showResult = true
        }
    }

    // Camera launcher — chụp ảnh FULL RESOLUTION (không dùng TakePicturePreview nữa)
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraFileUri != null) {
            imageUri = cameraFileUri
            processUriForOcr(cameraFileUri!!)
        }
    }

    // Gallery launcher — chọn từ thư viện
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageUri = uri
            processUriForOcr(uri)
        }
    }


    val teal = Color(0xFF005F73)
    val tealLight = Color(0xFF0A9396)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // Header gradient banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Brush.verticalGradient(listOf(teal, Color(0xFF0D1117))))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ─── Top bar ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Quét Bill",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Chụp hoá đơn để tự động nhận số tiền",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── Khu vực ảnh + OCR ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .weight(1f)
            ) {
                if (imageUri == null) {
                    // Placeholder — chọn nguồn ảnh
                    BillPickerPlaceholder(
                        onCamera = {
                            cameraFileUri = createCameraFileUri()
                            cameraLauncher.launch(cameraFileUri!!)
                        },
                        onGallery = { galleryLauncher.launch("image/*") },
                        teal = teal, tealLight = tealLight
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Preview ảnh
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(20.dp))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imageUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Scanning overlay
                            if (isScanning) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = tealLight, strokeWidth = 3.dp)
                                        Spacer(Modifier.height(12.dp))
                                        Text("Đang nhận diện số tiền...", color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }
                            // Nút chụp lại
                            if (!isScanning) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    SmallActionBtn(icon = Icons.Default.CameraAlt, label = "Chụp lại") {
                                        cameraFileUri = createCameraFileUri()
                                        cameraLauncher.launch(cameraFileUri!!)
                                    }
                                    SmallActionBtn(icon = Icons.Default.Photo, label = "Thư viện") {
                                        galleryLauncher.launch("image/*")
                                    }
                                }
                            }
                        }

                        // ─── Kết quả OCR ─────────────────────────────────────
                        AnimatedVisibility(
                            visible = showResult,
                            enter = fadeIn() + slideInVertically { 30 }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                // Chip kết quả
                                if (detectedAmount.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(teal.copy(alpha = 0.15f))
                                            .border(1.dp, teal.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, null, tint = tealLight, modifier = Modifier.size(20.dp))
                                        Column {
                                            Text("Số tiền nhận diện được", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                                            Text(
                                                "${formatCurrency(detectedAmount.toDoubleOrNull() ?: 0.0)} đ",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black,
                                                color = tealLight
                                            )
                                        }
                                    }
                                } else if (scanError != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFFD62828).copy(alpha = 0.12f))
                                            .border(1.dp, Color(0xFFD62828).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.Warning, null, tint = Color(0xFFF4A261), modifier = Modifier.size(20.dp))
                                        Column {
                                            Text("Không nhận diện được", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                                            Text("Nhập thủ công bên dưới", fontSize = 13.sp, color = Color(0xFFF4A261))
                                        }
                                    }
                                }

                                // Input số tiền (có thể sửa)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Số tiền (đ)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.7f))
                                    OutlinedTextField(
                                        value = amountInput,
                                        onValueChange = { v -> amountInput = v.filter { it.isDigit() } },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("0", color = Color.White.copy(alpha = 0.3f)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = tealLight,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            cursorColor = tealLight,
                                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        leadingIcon = {
                                            Text("đ", color = tealLight, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                                                modifier = Modifier.padding(start = 4.dp))
                                        },
                                        singleLine = true
                                    )
                                }

                                // Ghi chú
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Ghi chú", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.7f))
                                    OutlinedTextField(
                                        value = noteInput,
                                        onValueChange = { noteInput = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("Tên cửa hàng, mô tả...", color = Color.White.copy(alpha = 0.3f)) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = tealLight,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            cursorColor = tealLight,
                                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Nút xác nhận ở bottom
            AnimatedVisibility(
                visible = showResult && amountInput.isNotEmpty(),
                enter = fadeIn() + slideInVertically { it }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.linearGradient(listOf(teal, tealLight)))
                            .clickable {
                                val amt = amountInput.toDoubleOrNull() ?: 0.0
                                if (amt > 0) onConfirm(amt, noteInput, imageUri)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCart, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Text("Thêm vào giao dịch", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─── Placeholder chọn nguồn ảnh ──────────────────────────────────────────────
@Composable
private fun BillPickerPlaceholder(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    teal: Color,
    tealLight: Color
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
private fun SmallActionBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
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
private fun runOcr(
    bitmap: Bitmap,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val inputImage = InputImage.fromBitmap(bitmap, 0)

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

/**
 * Thuật toán trích xuất số tiền thông minh từ text bill OCR.
 *
 * Chiến lược theo thứ tự ưu tiên:
 * 1. Tìm số trên CÙNG DÒNG hoặc DÒNG KẾ TIẾP với từ khoá tổng tiền (phải trả, tổng cộng, total...)
 * 2. Ưu tiên số có dấu phân cách nghìn (1.500.000) và ký hiệu tiền tệ (đ, vnd)
 * 3. Bỏ qua số điện thoại (bắt đầu bằng 0), mã hoá đơn (dài > 8 không phân cách)
 * 4. Bỏ qua các dòng "tiền khách đưa", "tiền thừa"
 */
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

    // Regex tìm số: có thể có dấu phân cách nghìn, theo sau là tuỳ chọn (đ, vnd, vnđ, ₫)
    val numberPattern = Regex("""(?<!\d)(\d{1,3}(?:[.,]\d{3}){1,4}|\d{4,8})(?:\s*(?:đ|vnd|vnđ|₫))?(?!\d)""", RegexOption.IGNORE_CASE)

    fun parseNumber(match: MatchResult): Double? {
        val raw = match.groups[1]?.value ?: return null
        
        // Bỏ qua các số có số 0 ở đầu (số điện thoại)
        if (raw.startsWith("0")) return null
        
        // Bỏ qua số thuần dài hơn 8 chữ số (mã hoá đơn, mã vạch)
        if (!raw.contains(".") && !raw.contains(",") && raw.length > 8) return null

        val cleaned = raw.trim()
        return when {
            cleaned.matches(Regex("""\d{1,3}(\.\d{3})+""")) -> cleaned.replace(".", "").toDoubleOrNull()
            cleaned.matches(Regex("""\d{1,3}(,\d{3})+""")) -> cleaned.replace(",", "").toDoubleOrNull()
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

    // 1. Tìm trên dòng chứa keyword ưu tiên cao (tìm cả dòng đó và dòng kế)
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

    // 2. Tìm trên dòng chứa keyword ưu tiên trung bình
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

    // 3. Fallback: Thu thập tất cả các số hợp lệ
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

    // Ưu tiên 1: Lớn nhất trong các số có cả separator và currency
    val p1 = validNumbers.filter { it.second && it.third }.maxByOrNull { it.first }?.first
    if (p1 != null) return p1

    // Ưu tiên 2: Lớn nhất trong các số có separator
    val p2 = validNumbers.filter { it.third }.maxByOrNull { it.first }?.first
    if (p2 != null) return p2

    // Ưu tiên 3: Lớn nhất trong các số có currency
    val p3 = validNumbers.filter { it.second }.maxByOrNull { it.first }?.first
    if (p3 != null) return p3

    // Ưu tiên 4: Số lớn nhất nói chung
    return validNumbers.maxByOrNull { it.first }?.first
}

