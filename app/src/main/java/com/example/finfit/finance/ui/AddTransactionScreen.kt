package com.example.finfit.finance.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.finance.model.BankAccount
import com.example.finfit.finance.model.SUPPORTED_BANKS
import com.example.finfit.finance.model.Transaction
import com.example.finfit.finance.model.TransactionType
import com.example.finfit.finance.model.UserWallet
import com.example.finfit.ui.theme.*
import com.google.firebase.Timestamp

// ── Danh mục chi tiêu / thu nhập ─────────────────────────────
data class TxCategory(val label: String, val icon: ImageVector, val color: Color)

val EXPENSE_CATEGORIES = listOf(
    TxCategory("Ăn uống",    Icons.Default.Restaurant,    Color(0xFFEF4444)),
    TxCategory("Di chuyển",  Icons.Default.DirectionsBus, Color(0xFF8B5CF6)),
    TxCategory("Mua sắm",   Icons.Default.ShoppingBag,   Color(0xFFF59E0B)),
    TxCategory("Giải trí",  Icons.Default.SportsEsports, Color(0xFF0EA5E9)),
    TxCategory("Y tế",       Icons.Default.LocalHospital, Color(0xFF10B981)),
    TxCategory("Giáo dục",  Icons.Default.School,         Color(0xFF6366F1)),
    TxCategory("Hóa đơn",   Icons.Default.Receipt,        Color(0xFFEC4899)),
    TxCategory("Nhà ở",     Icons.Default.Home,           Color(0xFF14B8A6)),
    TxCategory("Du lịch",   Icons.Default.Flight,         Color(0xFFF97316)),
    TxCategory("Khác",       Icons.Default.MoreHoriz,      Color(0xFF6B7280)),
)

val INCOME_CATEGORIES = listOf(
    TxCategory("Lương",     Icons.Default.AccountBalance,  Color(0xFF10B981)),
    TxCategory("Thưởng",    Icons.Default.CardGiftcard,    Color(0xFFF59E0B)),
    TxCategory("Đầu tư",   Icons.Default.TrendingUp,       Color(0xFF6366F1)),
    TxCategory("Bán hàng", Icons.Default.Storefront,       Color(0xFF0EA5E9)),
    TxCategory("Cho vay",  Icons.Default.PeopleAlt,        Color(0xFFEC4899)),
    TxCategory("Khác",      Icons.Default.MoreHoriz,        Color(0xFF6B7280)),
)

// ── Màn hình thêm giao dịch ───────────────────────────────────
@Composable
fun AddTransactionScreen(
    wallet: UserWallet?,
    initialType: TransactionType = TransactionType.EXPENSE,
    onSave: (Transaction, UserWallet) -> Unit,
    onBack: () -> Unit
) {
    if (wallet == null) { onBack(); return }

    var txType   by remember { mutableStateOf(initialType) }
    var amount   by remember { mutableStateOf("") }
    var note     by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }

    var fromAccount by remember {
        mutableStateOf(wallet.accounts.firstOrNull())
    }
    var toAccount by remember {
        mutableStateOf(wallet.accounts.getOrNull(1))
    }

    var showAccountPicker by remember { mutableStateOf<String?>(null) } // "from" | "to"

    // Khi chuyển type, reset category
    LaunchedEffect(txType) { category = "" }

    // Account picker bottom sheet
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

    val accentColor = when (txType) {
        TransactionType.EXPENSE  -> Color(0xFFEF4444)
        TransactionType.INCOME   -> Color(0xFF10B981)
        TransactionType.TRANSFER -> Color(0xFF6366F1)
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // ── Header ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextWhite)
                }
                Text("Thêm giao dịch", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }

        // ── Tabs loại giao dịch ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardBackground)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TxTypeTab("Chi tiêu",    txType == TransactionType.EXPENSE,  Color(0xFFEF4444)) { txType = TransactionType.EXPENSE  }
                TxTypeTab("Thu nhập",    txType == TransactionType.INCOME,   Color(0xFF10B981)) { txType = TransactionType.INCOME   }
                TxTypeTab("Chuyển tiền", txType == TransactionType.TRANSFER, Color(0xFF6366F1)) { txType = TransactionType.TRANSFER }
            }
        }

        // ── Nhập số tiền ──
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Số tiền", color = TextGray, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                    Text(
                        text = formatAmountDisplay(amount),
                        color = accentColor,
                        fontSize = if (amount.length > 7) 34.sp else 42.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(" đ", color = accentColor.copy(alpha = 0.6f), fontSize = 20.sp, modifier = Modifier.padding(bottom = 6.dp))
                }
            }
        }

        // ── Bàn phím số ──
        item { NumericKeypad(onDigit = { d -> if (amount.length < 12) amount += d }, onDelete = { if (amount.isNotEmpty()) amount = amount.dropLast(1) }) }

        item { Spacer(Modifier.height(16.dp)) }

        // ── Chọn tài khoản ──
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (txType != TransactionType.TRANSFER) {
                    AccountSelectorRow(
                        label   = if (txType == TransactionType.EXPENSE) "Từ tài khoản" else "Vào tài khoản",
                        account = fromAccount,
                        onClick = { showAccountPicker = "from" }
                    )
                } else {
                    AccountSelectorRow("Từ tài khoản", fromAccount) { showAccountPicker = "from" }
                    Spacer(Modifier.height(8.dp))
                    AccountSelectorRow("Đến tài khoản", toAccount)  { showAccountPicker = "to"   }
                }
            }
        }

        // ── Danh mục (chỉ khi expense / income) ──
        if (txType != TransactionType.TRANSFER) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("Danh mục", color = TextGray, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    CategoryGrid(
                        categories = if (txType == TransactionType.EXPENSE) EXPENSE_CATEGORIES else INCOME_CATEGORIES,
                        selected   = category,
                        onSelect   = { category = it }
                    )
                }
            }
        }

        // ── Ghi chú ──
        item {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label  = { Text("Ghi chú (tùy chọn)") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Default.Edit, null, tint = TextGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor   = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = accentColor
                ),
                shape = RoundedCornerShape(14.dp)
            )
        }

        item { Spacer(Modifier.height(16.dp)) }

        // ── Nút lưu ──
        item {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt <= 0) return@Button

                    val transaction = Transaction(
                        amount   = amt,
                        type     = txType,
                        category = category.ifBlank { if (txType == TransactionType.EXPENSE) "Khác" else "Khác" },
                        note     = note,
                        timestamp = Timestamp.now()
                    )

                    // Cập nhật số dư tài khoản
                    val updatedAccounts = when (txType) {
                        TransactionType.EXPENSE -> wallet.accounts.map {
                            if (it.id == fromAccount?.id) it.copy(amount = it.amount - amt) else it
                        }
                        TransactionType.INCOME -> wallet.accounts.map {
                            if (it.id == fromAccount?.id) it.copy(amount = it.amount + amt) else it
                        }
                        TransactionType.TRANSFER -> wallet.accounts.map {
                            when (it.id) {
                                fromAccount?.id -> it.copy(amount = it.amount - amt)
                                toAccount?.id   -> it.copy(amount = it.amount + amt)
                                else -> it
                            }
                        }
                    }
                    onSave(transaction, wallet.copy(accounts = updatedAccounts))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(54.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape    = RoundedCornerShape(16.dp)
            ) {
                Text("Lưu giao dịch", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ── Tab loại giao dịch ────────────────────────────────────────
@Composable
fun RowScope.TxTypeTab(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) color else Color.Transparent, label = "")
    val tc by animateColorAsState(if (selected) Color.White else TextGray, label = "")
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = tc, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

// ── Bàn phím số ──────────────────────────────────────────────
@Composable
fun NumericKeypad(onDigit: (String) -> Unit, onDelete: () -> Unit) {
    val keys = listOf("1","2","3","4","5","6","7","8","9","000","0","⌫")
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(240.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false
    ) {
        items(keys) { key ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBackground)
                    .clickable { if (key == "⌫") onDelete() else onDigit(key) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (key == "⌫") {
                    Icon(Icons.Default.Backspace, null, tint = TextGray, modifier = Modifier.size(20.dp))
                } else {
                    Text(key, color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Hiển thị số tiền đẹp ─────────────────────────────────────
fun formatAmountDisplay(raw: String): String {
    val number = raw.toLongOrNull() ?: return "0"
    return String.format("%,d", number).replace(',', '.')
}

// ── Chọn tài khoản ───────────────────────────────────────────
@Composable
fun AccountSelectorRow(label: String, account: BankAccount?, onClick: () -> Unit) {
    val bankInfo = SUPPORTED_BANKS.find { it.code == account?.bankCode } ?: SUPPORTED_BANKS.last()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(bankInfo.emoji, fontSize = 22.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = TextGray, fontSize = 11.sp)
            Text(
                account?.displayName ?: "Chọn tài khoản",
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
        }
        if (account != null) {
            Text(formatCurrency(account.amount), color = TextGray, fontSize = 12.sp)
            Spacer(Modifier.width(6.dp))
        }
        Icon(Icons.Default.KeyboardArrowDown, null, tint = TextGray, modifier = Modifier.size(18.dp))
    }
}

// ── Grid danh mục ─────────────────────────────────────────────
@Composable
fun CategoryGrid(categories: List<TxCategory>, selected: String, onSelect: (String) -> Unit) {
    val rows = categories.chunked(5)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { cat ->
                    val isSelected = cat.label == selected
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) cat.color.copy(alpha = 0.2f) else CardBackground)
                            .border(
                                if (isSelected) 1.5.dp else 0.dp,
                                if (isSelected) cat.color else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelect(cat.label) }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(cat.color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(cat.icon, null, tint = cat.color, modifier = Modifier.size(18.dp)) }
                        Spacer(Modifier.height(4.dp))
                        Text(cat.label, color = if (isSelected) cat.color else TextGray, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }
                // Filler nếu row không đủ 5
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ── Bottom sheet chọn tài khoản ──────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPickerDialog(accounts: List<BankAccount>, onSelected: (BankAccount) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CardBackground) {
        Text(
            "Chọn tài khoản",
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        accounts.forEach { acc ->
            val bankInfo = SUPPORTED_BANKS.find { it.code == acc.bankCode } ?: SUPPORTED_BANKS.last()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelected(acc) }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(bankInfo.emoji, fontSize = 22.sp)
                val formatCurrency = { amount: Double ->
                    val fmt = java.text.NumberFormat.getInstance(java.util.Locale("vi", "VN"))
                    fmt.maximumFractionDigits = 0
                    "${fmt.format(amount)} đ"
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(acc.displayName, color = TextWhite, fontWeight = FontWeight.Bold)
                    Text(bankInfo.displayName, color = TextGray, fontSize = 12.sp)
                }
                Text(formatCurrency(acc.amount), color = TextGray)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
