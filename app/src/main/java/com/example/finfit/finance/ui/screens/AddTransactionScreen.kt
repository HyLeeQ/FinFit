package com.example.finfit.finance.ui.screens

import com.example.finfit.finance.ui.utils.*
import com.example.finfit.finance.ui.wrappers.*
import com.example.finfit.finance.ui.logic.*
import com.example.finfit.finance.util.CategoryLearningManager
import com.example.finfit.finance.util.DuplicateTransactionDetector
import com.example.finfit.finance.util.VietQrGenerator

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.finfit.finance.model.*
import com.example.finfit.finance.ui.utils.formatAmountInput
import com.example.finfit.finance.ui.utils.parseAmountInput
import com.example.finfit.ui.theme.*
import com.google.firebase.Timestamp
import java.util.UUID
import java.util.Calendar
import java.util.Date

enum class SplitBillMode(val label: String) {
    EQUAL("Chia đều"),
    CUSTOM("Tùy chỉnh số tiền"),
    PERCENT("Theo %")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    wallet: AppUserWallet?,
    budgets: List<FinanceBudget> = emptyList(),
    transactions: List<FinanceTransaction> = emptyList(),
    initialType: TransactionType = TransactionType.EXPENSE,
    autoOpenCamera: Boolean = false,
    initialAmount: Double = 0.0,
    initialNote: String = "",
    onSave: (FinanceTransaction, AppUserWallet, android.net.Uri?) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit = onBack
) {
    if (wallet == null) { onBack(); return }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    var txType   by remember { mutableStateOf(initialType) }
    var amount   by remember { mutableStateOf(if (initialAmount > 0) initialAmount.toLong().toString() else "") }
    var note     by remember { mutableStateOf(initialNote) }
    var category by remember { mutableStateOf("") }
    var isGroupPrepayment by remember { mutableStateOf(false) }
    var splitMode by remember { mutableStateOf(SplitBillMode.EQUAL) }
    var participantCount by remember { mutableIntStateOf(2) }
    var personalAmountText by remember { mutableStateOf("") }
    var personalPercent by remember { mutableDoubleStateOf(50.0) }
    var ignoreDuplicateWarning by remember { mutableStateOf(false) }

    // === Ảnh chi tiêu ===
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }
    
    var cameraFileUri by remember { mutableStateOf<android.net.Uri?>(null) }
    fun createDiaryCameraUri(): android.net.Uri {
        val dir = java.io.File(context.cacheDir, "diary_images").also { it.mkdirs() }
        val file = java.io.File(dir, "diary_${System.currentTimeMillis()}.jpg")
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraFileUri != null) {
            selectedImageUri = cameraFileUri
        }
    }

    LaunchedEffect(autoOpenCamera) {
        if (autoOpenCamera && selectedImageUri == null) {
            cameraFileUri = createDiaryCameraUri()
            cameraLauncher.launch(cameraFileUri!!)
        }
    }

    var fromAccount by remember { mutableStateOf(wallet.accounts.firstOrNull()) }
    var toAccount by remember { mutableStateOf(wallet.accounts.getOrNull(1)) }
    var showAccountPicker by remember { mutableStateOf<String?>(null) }

    // Auto-predict category when note changes
    LaunchedEffect(note, txType) {
        if (note.isNotBlank() && (category.isBlank() || category == "Khác")) {
            val predicted = CategoryLearningManager.predictCategory(note)
            if (predicted != null && predicted.isNotBlank()) {
                category = predicted
            }
        }
    }

    // Duplicate detection
    val duplicateResult = remember(amount, category, txType, transactions) {
        val amt = amount.toDoubleOrNull() ?: 0.0
        if (amt > 0) {
            DuplicateTransactionDetector.checkForDuplicate(
                amount = amt,
                timestampMillis = System.currentTimeMillis(),
                transactions = transactions
            )
        } else {
            com.example.finfit.finance.util.DuplicateWarning(false)
        }
    }

    val accentColor by animateColorAsState(
        targetValue = when (txType) {
            TransactionType.EXPENSE  -> Color(0xFFEF4444)
            TransactionType.INCOME   -> Color(0xFF10B981)
            TransactionType.TRANSFER -> Color(0xFF6366F1)
            TransactionType.GROUP_PREPAYMENT -> Color(0xFFEF4444)
        },
        animationSpec = tween(500)
    )

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { 40 })
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                        Text(
                            text = if (txType == TransactionType.TRANSFER) "Chuyển tiền" else "Giao dịch mới",
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                        Spacer(Modifier.width(48.dp))
                    }
                }

                item {
                    // Type Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(4.dp)
                    ) {
                        listOf(
                            TransactionType.EXPENSE to "Chi tiêu",
                            TransactionType.INCOME to "Thu nhập",
                            TransactionType.TRANSFER to "Chuyển"
                        ).forEach { (type, label) ->
                            val isSelected = txType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) accentColor else Color.Transparent)
                                    .clickable { txType = type },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    // Big Amount Display
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("NHẬP SỐ TIỀN", fontSize = 11.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("đ", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                Spacer(Modifier.width(12.dp))
                                val displayAmount = formatAmountInput(amount)
                                BasicTextField(
                                    value = displayAmount,
                                    onValueChange = { input -> amount = input.filter { it.isDigit() } },
                                    textStyle = TextStyle(fontSize = 38.sp, fontWeight = FontWeight.Black, color = accentColor, textAlign = TextAlign.Start),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    cursorBrush = SolidColor(accentColor)
                                )
                            }
                            val rawVal = amount.toDoubleOrNull() ?: 0.0
                            if (rawVal > 0) {
                                Text(
                                    text = when {
                                        rawVal >= 1_000_000 -> "${(rawVal/1_000_000).toLong()} triệu đồng"
                                        rawVal >= 1_000 -> "${(rawVal/1_000).toLong()} nghìn đồng"
                                        else -> "${rawVal.toLong()} đồng"
                                    },
                                    fontSize = 12.sp,
                                    color = accentColor.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Cảnh báo trùng lặp giao dịch
                if (duplicateResult.isDuplicateCandidate && !ignoreDuplicateWarning) {
                    item {
                        Spacer(Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.12f)),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⚠️", fontSize = 20.sp)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Có thể bị trùng lặp giao dịch",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFFD97706)
                                    )
                                    Text(
                                        duplicateResult.warningMessage,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                TextButton(onClick = { ignoreDuplicateWarning = true }) {
                                    Text("Vẫn lưu", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(20.dp)) }

                item {
                    // Account Selector
                    if (txType == TransactionType.TRANSFER) {
                        TransferAccountSection(fromAccount, toAccount, { showAccountPicker = "from" }, { showAccountPicker = "to" })
                    } else {
                        SingleAccountSelector(fromAccount, { showAccountPicker = "from" })
                    }
                }

                item { Spacer(Modifier.height(20.dp)) }

                if (txType == TransactionType.EXPENSE) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(PrimaryBlue.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Group, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Trả trước cho nhóm (Chia bill)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Tự động theo dõi tiền nhóm còn nợ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = isGroupPrepayment,
                                onCheckedChange = { isGroupPrepayment = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryBlue)
                            )
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }

                if (isGroupPrepayment) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("CHI TIẾT CHIA TIỀN NHÓM", fontSize = 11.sp, fontWeight = FontWeight.Black, color = PrimaryBlue, letterSpacing = 1.sp)
                                Spacer(Modifier.height(14.dp))

                                // Split Mode Tabs
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(3.dp)
                                ) {
                                    SplitBillMode.values().forEach { mode ->
                                        val isSel = splitMode == mode
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSel) PrimaryBlue else Color.Transparent)
                                                .clickable { splitMode = mode },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                mode.label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))
                                
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Số người chia (bao gồm bạn)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { if (participantCount > 1) participantCount-- }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.RemoveCircleOutline, null, tint = PrimaryBlue)
                                        }
                                        Text(participantCount.toString(), modifier = Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.Bold)
                                        IconButton(onClick = { participantCount++ }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.AddCircleOutline, null, tint = PrimaryBlue)
                                        }
                                    }
                                }
                                
                                Spacer(Modifier.height(14.dp))

                                val tot = amount.toDoubleOrNull() ?: 0.0
                                val pers = when(splitMode) {
                                    SplitBillMode.EQUAL -> tot / participantCount
                                    SplitBillMode.CUSTOM -> personalAmountText.toDoubleOrNull() ?: (tot / participantCount)
                                    SplitBillMode.PERCENT -> tot * (personalPercent / 100.0)
                                }
                                val others = (tot - pers).coerceAtLeast(0.0)

                                if (splitMode == SplitBillMode.CUSTOM) {
                                    OutlinedTextField(
                                        value = personalAmountText,
                                        onValueChange = { if (it.all { c -> c.isDigit() }) personalAmountText = it },
                                        placeholder = { Text("Nhập số tiền bạn tự chịu (đ)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                } else if (splitMode == SplitBillMode.PERCENT) {
                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Tỷ lệ bạn tự trả:", fontSize = 12.sp)
                                            Text("${personalPercent.toInt()}%", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                        }
                                        Slider(
                                            value = personalPercent.toFloat(),
                                            onValueChange = { personalPercent = it.toDouble() },
                                            valueRange = 0f..100f,
                                            steps = 19
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                }
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Bạn tự trả", fontSize = 10.sp, color = Color.Gray)
                                        Text(formatCurrency(pers), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Box(Modifier.width(1.dp).height(24.dp).background(Color.LightGray.copy(alpha = 0.3f)))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Nhóm còn nợ bạn", fontSize = 10.sp, color = Color.Gray)
                                        Text(formatCurrency(others), fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                    }
                                }

                                // Quick VietQR copy button
                                if (others > 0 && fromAccount != null && fromAccount!!.accountNumber.isNotBlank()) {
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedButton(
                                        onClick = {
                                            val shareMsg = VietQrGenerator.generateTransferMessage(
                                                bankDisplayName = fromAccount?.name ?: "Ngân hàng",
                                                accountNumber = fromAccount?.accountNumber ?: "",
                                                accountName = "FinFit",
                                                amount = others / (participantCount - 1).coerceAtLeast(1),
                                                description = "Chia bill $note"
                                            )
                                            clipboardManager.setText(AnnotatedString(shareMsg))
                                            Toast.makeText(context, "Đã sao chép tin nhắn nhắc chuyển khoản!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.QrCode, null, modifier = Modifier.size(16.dp), tint = PrimaryBlue)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Sao chép nội dung & STK chia tiền", fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }

                if (txType != TransactionType.TRANSFER) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("DANH MỤC", fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp))
                            if (category.isNotBlank()) {
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(accentColor.copy(alpha = 0.1f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Đang chọn: $category", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        CategoryGrid(
                            categories = if (txType == TransactionType.EXPENSE) EXPENSE_CATEGORIES else INCOME_CATEGORIES,
                            selected = category,
                            onSelected = { category = it }
                        )
                    }
                }

                item { Spacer(Modifier.height(20.dp)) }

                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Ghi chú / Mô tả (VD: Highlands Coffee, Grab, Tiền điện...)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            focusedBorderColor = accentColor
                        )
                    )
                }

                item { Spacer(Modifier.height(16.dp)) }

                // === Thêm ảnh chi tiêu ===
                item {
                    Column {
                        Text(
                            "ẢNH HÓA ĐƠN / KỶ NIỆM",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        if (selectedImageUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(20.dp))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(selectedImageUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.4f)
                                                )
                                            )
                                        )
                                )
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalIconButton(
                                        onClick = { imagePickerLauncher.launch("image/*") },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                                    }
                                    FilledTonalIconButton(
                                        onClick = { selectedImageUri = null },
                                        modifier = Modifier.size(40.dp),
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor = Color.Red.copy(alpha = 0.2f)
                                        )
                                    ) {
                                        Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(accentColor.copy(alpha = 0.06f))
                                        .border(width = 1.5.dp, color = accentColor.copy(alpha = 0.25f), shape = RoundedCornerShape(20.dp))
                                        .clickable {
                                            cameraFileUri = createDiaryCameraUri()
                                            cameraLauncher.launch(cameraFileUri!!)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier.size(44.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.CameraAlt, null, tint = accentColor, modifier = Modifier.size(22.dp))
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text("Chụp ảnh mới", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = accentColor)
                                    }
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(accentColor.copy(alpha = 0.06f))
                                        .border(width = 1.5.dp, color = accentColor.copy(alpha = 0.25f), shape = RoundedCornerShape(20.dp))
                                        .clickable { imagePickerLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier.size(44.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.PhotoLibrary, null, tint = accentColor, modifier = Modifier.size(22.dp))
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text("Chọn từ thư viện", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = accentColor)
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(28.dp)) }

                item {
                    val isValid = amount.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0 && (
                        (txType == TransactionType.TRANSFER && fromAccount != null && toAccount != null && fromAccount?.id != toAccount?.id) ||
                        (txType != TransactionType.TRANSFER && category.isNotBlank() && fromAccount != null)
                    )

                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            if (isValid) {
                                // Ghi nhận học danh mục cho tương lai
                                if (note.isNotBlank() && category.isNotBlank()) {
                                    CategoryLearningManager.learnPattern(note, category)
                                }

                                val finalPersonalAmount = if (isGroupPrepayment) {
                                    when(splitMode) {
                                        SplitBillMode.EQUAL -> (amt / participantCount).toInt().toString()
                                        SplitBillMode.CUSTOM -> personalAmountText
                                        SplitBillMode.PERCENT -> (amt * (personalPercent / 100.0)).toInt().toString()
                                    }
                                } else personalAmountText

                                val result = buildTransactionResult(
                                    wallet = wallet,
                                    txType = txType,
                                    amount = amt,
                                    category = category,
                                    note = note,
                                    fromAccount = fromAccount,
                                    toAccount = toAccount,
                                    isGroupPrepayment = isGroupPrepayment,
                                    personalAmountText = finalPersonalAmount,
                                    participantCount = participantCount,
                                    budgets = budgets,
                                    transactions = transactions,
                                    imageUri = selectedImageUri
                                )
                                onSave(result.transaction, result.updatedWallet, result.imageUri)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .graphicsLayer { shadowElevation = 8.dp.toPx() },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        enabled = isValid
                    ) {
                        Text("Xác nhận ${if (txType == TransactionType.EXPENSE) "Chi" else if (txType == TransactionType.INCOME) "Thu" else "Chuyển"}", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
                
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }

    if (showAccountPicker != null) {
        AccountPickerDialog(
            accounts = wallet.accounts,
            onSelected = { acc ->
                if (showAccountPicker == "from") fromAccount = acc else toAccount = acc
                showAccountPicker = null
            },
            onDismiss = { showAccountPicker = null }
        )
    }
}
