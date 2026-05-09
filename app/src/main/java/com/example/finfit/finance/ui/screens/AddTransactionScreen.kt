package com.example.finfit.finance.ui.screens

import com.example.finfit.finance.ui.utils.*
import com.example.finfit.finance.ui.wrappers.*

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    wallet: AppUserWallet?,
    budgets: List<FinanceBudget> = emptyList(),
    transactions: List<FinanceTransaction> = emptyList(),
    initialType: TransactionType = TransactionType.EXPENSE,
    autoOpenCamera: Boolean = false,
    onSave: (FinanceTransaction, AppUserWallet, android.net.Uri?) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit = onBack
) {
    if (wallet == null) { onBack(); return }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    var txType   by remember { mutableStateOf(initialType) }
    var amount   by remember { mutableStateOf("") }
    var note     by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var isGroupPrepayment by remember { mutableStateOf(false) }
    var participantCount by remember { mutableIntStateOf(2) }
    var personalAmountText by remember { mutableStateOf("") }
    // === Ảnh chi tiêu ===
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val file = java.io.File(context.cacheDir, "temp_diary_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }
            selectedImageUri = android.net.Uri.fromFile(file)
        }
    }

    // Tự động mở camera nếu được yêu cầu từ Navbar
    LaunchedEffect(autoOpenCamera) {
        if (autoOpenCamera && selectedImageUri == null) {
            cameraLauncher.launch(null)
        }
    }

    var fromAccount by remember { mutableStateOf(wallet.accounts.firstOrNull()) }
    var toAccount by remember { mutableStateOf(wallet.accounts.getOrNull(1)) }
    var showAccountPicker by remember { mutableStateOf<String?>(null) } // "from" | "to"

    LaunchedEffect(txType) { category = "" }

    val accentColor by animateColorAsState(
        targetValue = when (txType) {
            TransactionType.EXPENSE  -> Color(0xFFEF4444)
            TransactionType.INCOME   -> Color(0xFF10B981)
            TransactionType.TRANSFER -> Color(0xFF6366F1)
            TransactionType.GROUP_PREPAYMENT -> Color(0xFFEF4444) // Giống màu chi tiêu
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
                        Spacer(Modifier.width(48.dp)) // Maintain balance for center alignment
                    }
                }

                item {
                    // Type Selector with premium pill look
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
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
                                // Hiển thị số đã format (1.000.000) nhưng lưu raw digits
                                val displayAmount = formatAmountInput(amount)
                                BasicTextField(
                                    value = displayAmount,
                                    onValueChange = { input ->
                                        // Chỉ giữ lại chữ số gốc
                                        amount = input.filter { it.isDigit() }
                                    },
                                    textStyle = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Black, color = accentColor, textAlign = TextAlign.Start),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    cursorBrush = SolidColor(accentColor)
                                )
                            }
                            // Hiển thị preview có đơn vị đồng
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

                item { Spacer(Modifier.height(24.dp)) }

                item {
                    // Account Selector
                    if (txType == TransactionType.TRANSFER) {
                        TransferAccountSection(fromAccount, toAccount, { showAccountPicker = "from" }, { showAccountPicker = "to" })
                    } else {
                        SingleAccountSelector(fromAccount, { showAccountPicker = "from" })
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }

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
                                    Text("Trả trước cho nhóm", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Tính vào Tiền trả trước", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = isGroupPrepayment,
                                onCheckedChange = { isGroupPrepayment = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryBlue)
                            )
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }

                if (isGroupPrepayment) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("CHI TIẾT CHIA TIỀN", fontSize = 11.sp, fontWeight = FontWeight.Black, color = PrimaryBlue, letterSpacing = 1.sp)
                                Spacer(Modifier.height(16.dp))
                                
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Số người chia (bao gồm bạn)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
                                
                                Spacer(Modifier.height(20.dp))
                                
                                Column {
                                    Text("Số tiền bạn tự chịu (đ)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = personalAmountText,
                                        onValueChange = { if (it.all { c -> c.isDigit() }) personalAmountText = it },
                                        placeholder = { Text("Mặc định là chia đều") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        trailingIcon = {
                                            TextButton(onClick = { 
                                                val tot = amount.toDoubleOrNull() ?: 0.0
                                                personalAmountText = (tot / participantCount).toInt().toString()
                                            }) {
                                                Text("Chia đều", fontSize = 12.sp)
                                            }
                                        }
                                    )
                                }
                                
                                Spacer(Modifier.height(16.dp))
                                
                                val tot = amount.toDoubleOrNull() ?: 0.0
                                val pers = personalAmountText.toDoubleOrNull() ?: (tot / participantCount)
                                val others = (tot - pers).coerceAtLeast(0.0)
                                
                                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Bạn trả", fontSize = 10.sp, color = Color.Gray)
                                        Text(formatCurrency(pers), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Box(Modifier.width(1.dp).height(24.dp).background(Color.LightGray.copy(alpha = 0.3f)))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Nhóm trả", fontSize = 10.sp, color = Color.Gray)
                                        Text(formatCurrency(others), fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }

                if (txType != TransactionType.TRANSFER) {
                    item {
                        Text("DANH MỤC", fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp))
                        Spacer(Modifier.height(16.dp))
                        CategoryGrid(
                            categories = if (txType == TransactionType.EXPENSE) EXPENSE_CATEGORIES else INCOME_CATEGORIES,
                            selected = category,
                            onSelected = { category = it }
                        )
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }

                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Ghi chú & Mô tả") },
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
                            "KỸ NIỆM ẢNH",
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
                                // Overlay xóa ảnh
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
                                // Nút đổi ảnh
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
                            // Button chọn ảnh / Chụp ảnh
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Camera
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(accentColor.copy(alpha = 0.06f))
                                        .border(width = 1.5.dp, color = accentColor.copy(alpha = 0.25f), shape = RoundedCornerShape(20.dp))
                                        .clickable { cameraLauncher.launch(null) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier.size(52.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.CameraAlt, null, tint = accentColor, modifier = Modifier.size(24.dp))
                                        }
                                        Spacer(Modifier.height(10.dp))
                                        Text("Chụp ảnh mới", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = accentColor)
                                    }
                                }
                                
                                // Gallery
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(accentColor.copy(alpha = 0.06f))
                                        .border(width = 1.5.dp, color = accentColor.copy(alpha = 0.25f), shape = RoundedCornerShape(20.dp))
                                        .clickable { imagePickerLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier.size(52.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.PhotoLibrary, null, tint = accentColor, modifier = Modifier.size(24.dp))
                                        }
                                        Spacer(Modifier.height(10.dp))
                                        Text("Chọn từ thư viện", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = accentColor)
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }

                item {
                    val isValid = amount.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0 && (
                        (txType == TransactionType.TRANSFER && fromAccount != null && toAccount != null && fromAccount?.id != toAccount?.id) ||
                        (txType != TransactionType.TRANSFER && category.isNotBlank() && fromAccount != null)
                    )

                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            if (isValid) {
                                val txId = UUID.randomUUID().toString()
                                val newTx = FinanceTransaction(
                                    id = txId,
                                    amount = amt,
                                    type = if (isGroupPrepayment) TransactionType.GROUP_PREPAYMENT else txType,
                                    category = if (txType == TransactionType.TRANSFER) "Chuyển tiền" else category,
                                    note = note,
                                    timestamp = Timestamp.now(),
                                    accountId = fromAccount?.id,
                                    toAccountId = if (txType == TransactionType.TRANSFER) toAccount?.id else null,
                                    paymentMethod = if (fromAccount?.bankCode == "CASH") PaymentMethod.CASH else PaymentMethod.BANKING,
                                    isGroupPrepayment = isGroupPrepayment,
                                    personalAmount = if (isGroupPrepayment) (personalAmountText.toDoubleOrNull() ?: (amt / participantCount)) else amt,
                                    groupAmount = if (isGroupPrepayment) (amt - (personalAmountText.toDoubleOrNull() ?: (amt / participantCount))).coerceAtLeast(0.0) else 0.0,
                                    participantCount = if (isGroupPrepayment) participantCount else 1
                                )

                                // === LOGIC 1: Cập nhật số dư trong các tài khoản ===
                                var updatedAccounts = wallet.accounts.map { acc ->
                                    when (txType) {
                                        TransactionType.INCOME -> {
                                            if (acc.id == fromAccount?.id) acc.copy(amount = acc.amount + amt) else acc
                                        }
                                        TransactionType.EXPENSE, TransactionType.GROUP_PREPAYMENT -> {
                                            if (acc.id == fromAccount?.id) acc.copy(amount = acc.amount - amt) else acc
                                        }
                                        TransactionType.TRANSFER -> {
                                            when (acc.id) {
                                                fromAccount?.id -> acc.copy(amount = acc.amount - amt)
                                                toAccount?.id -> acc.copy(amount = acc.amount + amt)
                                                else -> acc
                                            }
                                        }
                                    }
                                }
                                
                                var finalGeneralSavings = wallet.generalSavings
                                var finalGroupPrepaid = wallet.groupPrepaidAmount
                                
                                // === LOGIC 2: Trả trước cho nhóm ===
                                if (isGroupPrepayment) {
                                    val groupPart = amt - (personalAmountText.toDoubleOrNull() ?: (amt / participantCount))
                                    finalGroupPrepaid += groupPart.coerceAtLeast(0.0)
                                }
                                
                                // === LOGIC 3: Khấu trừ vào Tiết kiệm chung nếu vượt ngân sách ===
                                if (txType == TransactionType.EXPENSE || isGroupPrepayment) {
                                    val personalExpense = if (isGroupPrepayment) (personalAmountText.toDoubleOrNull() ?: (amt / participantCount)) else amt
                                    
                                    val relevantBudgets = budgets.filter { 
                                        it.category == "Tất cả" || it.category == category 
                                    }
                                    
                                    for (budget in relevantBudgets) {
                                        // Tính tổng đã chi trong kỳ của budget này
                                        val cal = Calendar.getInstance()
                                        cal.time = budget.startDate.toDate()
                                        
                                        val totalSpentBefore = transactions.filter { tx ->
                                            (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.GROUP_PREPAYMENT) && 
                                            (budget.category == "Tất cả" || tx.category == budget.category) &&
                                            tx.timestamp.toDate().after(budget.startDate.toDate())
                                        }.sumOf { if (it.isGroupPrepayment) it.personalAmount else it.amount }
                                        
                                        val budgetLimit = budget.amount
                                        
                                        if (totalSpentBefore + personalExpense > budgetLimit) {
                                            // Có phần vượt quá
                                            val excess = if (totalSpentBefore >= budgetLimit) {
                                                personalExpense // Vượt toàn bộ phần chi cá nhân này
                                            } else {
                                                (totalSpentBefore + personalExpense) - budgetLimit // Vượt một phần
                                            }
                                            
                                            if (excess > 0) {
                                                finalGeneralSavings -= excess
                                            }
                                        }
                                    }
                                }

                                val updatedWallet = wallet.copy(
                                    accounts = updatedAccounts,
                                    generalSavings = finalGeneralSavings,
                                    groupPrepaidAmount = finalGroupPrepaid
                                )
                                
                                onSave(newTx, updatedWallet, selectedImageUri)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
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

@Composable
fun SingleAccountSelector(account: AppBankAccount?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Từ tài khoản", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(account?.name ?: "Chưa chọn tài khoản", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.LightGray)
        }
    }
}

@Composable
fun TransferAccountSection(from: AppBankAccount?, to: AppBankAccount?, onFrom: () -> Unit, onTo: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SingleAccountSelector(from, onFrom)
        Box(Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 40.dp).background(Color.LightGray.copy(alpha = 0.2f)))
        SingleAccountSelector(to, onTo)
    }
}

@Composable
fun CategoryGrid(categories: List<TxCategory>, selected: String, onSelected: (String) -> Unit) {
    Column {
        categories.chunked(3).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { cat ->
                    val isSelected = selected == cat.label
                    Card(
                        modifier = Modifier.weight(1f).clickable { onSelected(cat.label) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) cat.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        border = if (isSelected) BorderStroke(1.5.dp, cat.color) else null
                    ) {
                        Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(cat.icon, null, tint = if (isSelected) cat.color else Color.Gray.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(cat.label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun AccountPickerDialog(accounts: List<AppBankAccount>, onSelected: (AppBankAccount) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn tài khoản", fontWeight = FontWeight.Black) },
        text = {
            LazyColumn {
                items(accounts) { acc ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelected(acc) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val bank = SUPPORTED_BANKS.find { it.code == acc.bankCode } ?: SUPPORTED_BANKS.last()
                        Text(bank.emoji, fontSize = 24.sp)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(acc.name, fontWeight = FontWeight.Bold)
                            Text(formatCurrency(acc.amount), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Đóng") } },
        shape = RoundedCornerShape(28.dp)
    )
}
