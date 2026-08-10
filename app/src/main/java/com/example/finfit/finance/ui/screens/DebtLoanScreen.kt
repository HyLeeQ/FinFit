package com.example.finfit.finance.ui.screens

import com.example.finfit.finance.ui.utils.*
import com.example.finfit.finance.ui.wrappers.*
import com.example.finfit.core.ui.FinFitTopAppBar
import com.example.finfit.finance.util.VietQrGenerator

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.finfit.finance.model.*
import com.example.finfit.ui.theme.AccentGreen
import com.example.finfit.ui.theme.PrimaryBlue
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

enum class DebtLoanMainTab(val label: String) {
    BY_PERSON("Theo người"),
    DEBT_LIST("Tôi đang nợ"),
    LOAN_LIST("Cho vay & Chia bill")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtLoanScreen(
    items: List<DebtLoan>,
    groupPrepaidItems: List<GroupPrepaidItem> = emptyList(),
    wallet: AppUserWallet? = null,
    onSave: (DebtLoan) -> Unit,
    onTogglePaid: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onCollectGroupPrepaid: (GroupPrepaidItem, Double) -> Unit = { _, _ -> },
    onBack: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    var selectedTab by remember { mutableStateOf(DebtLoanMainTab.BY_PERSON) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedPersonForDetail by remember { mutableStateOf<PersonDebtSummary?>(null) }
    var prefillPersonName by remember { mutableStateOf("") }
    var showPartialPayDialog by remember { mutableStateOf<DebtLoan?>(null) }

    var showCollectDialog by remember { mutableStateOf(false) }
    var collectingItem by remember { mutableStateOf<GroupPrepaidItem?>(null) }

    // Gom nhóm nợ theo từng người
    val personSummaries = remember(items) {
        val grouped = items.groupBy { it.personName.trim() }
        grouped.map { (person, personItems) ->
            val unpaid = personItems.filter { !it.isPaid }
            val debtTotal = unpaid.filter { it.type == DebtLoanType.DEBT }.sumOf { it.remainingAmount }
            val loanTotal = unpaid.filter { it.type == DebtLoanType.LOAN }.sumOf { it.remainingAmount }
            val net = loanTotal - debtTotal // > 0: họ nợ mình, < 0: mình nợ họ
            PersonDebtSummary(person, debtTotal, loanTotal, net, personItems)
        }.sortedByDescending { abs(it.netAmount) }
    }

    val debtItems = remember(items) { items.filter { it.type == DebtLoanType.DEBT }.sortedBy { it.isPaid } }
    val loanItems = remember(items) { items.filter { it.type == DebtLoanType.LOAN }.sortedBy { it.isPaid } }

    Scaffold(
        topBar = {
            FinFitTopAppBar(
                title = "Quản lý Nợ & Cho vay",
                onBack = onBack,
                containerColor = MaterialTheme.colorScheme.background
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { 
                    prefillPersonName = ""
                    showAddDialog = true 
                },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Khoản nợ mới") },
                shape = RoundedCornerShape(20.dp)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Modern Tab Selector
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedTab.ordinal < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]).padding(horizontal = 24.dp).clip(CircleShape),
                            height = 3.dp,
                            color = PrimaryBlue
                        )
                    }
                }
            ) {
                DebtLoanMainTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                tab.label,
                                fontWeight = if (selectedTab == tab) FontWeight.Black else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600)) + slideInVertically(initialOffsetY = { 30 })
            ) {
                when (selectedTab) {
                    DebtLoanMainTab.BY_PERSON -> {
                        // ─── View Gom Nhóm Theo Người ───
                        if (personSummaries.isEmpty()) {
                            EmptyDebtLoanState(isDebt = false, customText = "Chưa có danh sách nợ với ai.")
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 100.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                item {
                                    Text(
                                        "TỔNG HỢP THEO TỪNG NGƯỜI (${personSummaries.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                    )
                                }

                                items(personSummaries, key = { it.personName }) { summary ->
                                    PersonDebtSummaryCard(
                                        summary = summary,
                                        onClick = { selectedPersonForDetail = summary }
                                    )
                                }
                            }
                        }
                    }

                    DebtLoanMainTab.DEBT_LIST -> {
                        // ─── Danh Sách Tôi Đang Nợ ───
                        if (debtItems.isEmpty()) {
                            EmptyDebtLoanState(isDebt = true)
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 100.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                itemsIndexed(debtItems, key = { _, it -> it.id }) { _, item ->
                                    DebtLoanItemCard(
                                        item = item,
                                        onTogglePaid = { onTogglePaid(item.id, !item.isPaid) },
                                        onDelete = { onDelete(item.id) },
                                        onPartialPay = { showPartialPayDialog = item }
                                    )
                                }
                            }
                        }
                    }

                    DebtLoanMainTab.LOAN_LIST -> {
                        // ─── Danh Sách Cho Vay & Chia Bill Nhóm ───
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 100.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (groupPrepaidItems.isNotEmpty()) {
                                val activeGroupItems = groupPrepaidItems.filter { !it.isFullyCollected }
                                if (activeGroupItems.isNotEmpty()) {
                                    item {
                                        Text(
                                            "Tiền ứng trước chia bill nhóm",
                                            fontSize = 12.sp, fontWeight = FontWeight.Black,
                                            color = PrimaryBlue,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
                                    items(activeGroupItems, key = { "gp_${it.id}" }) { item ->
                                        GroupPrepaidItemCard(
                                            item = item,
                                            onCollectPartial = {
                                                collectingItem = item
                                                showCollectDialog = true
                                            },
                                            onCollectFull = {
                                                onCollectGroupPrepaid(item, item.groupOwedAmount - item.collectedAmount)
                                            }
                                        )
                                    }
                                }
                            }

                            if (loanItems.isNotEmpty()) {
                                item {
                                    Text(
                                        "Tiền cho cá nhân vay",
                                        fontSize = 12.sp, fontWeight = FontWeight.Black,
                                        color = PrimaryBlue,
                                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                    )
                                }
                                itemsIndexed(loanItems, key = { _, it -> it.id }) { _, item ->
                                    DebtLoanItemCard(
                                        item = item,
                                        onTogglePaid = { onTogglePaid(item.id, !item.isPaid) },
                                        onDelete = { onDelete(item.id) },
                                        onPartialPay = { showPartialPayDialog = item }
                                    )
                                }
                            }

                            if (groupPrepaidItems.isEmpty() && loanItems.isEmpty()) {
                                item {
                                    EmptyDebtLoanState(isDebt = false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal chi tiết theo người (Person Detail Sheet)
    if (selectedPersonForDetail != null) {
        val currentSummary = personSummaries.find { it.personName == selectedPersonForDetail!!.personName }
        if (currentSummary != null) {
            PersonDebtDetailSheet(
                summary = currentSummary,
                wallet = wallet,
                onDismiss = { selectedPersonForDetail = null },
                onAddNewDebt = {
                    prefillPersonName = currentSummary.personName
                    showAddDialog = true
                },
                onTogglePaid = onTogglePaid,
                onDelete = onDelete,
                onPartialPay = { showPartialPayDialog = it }
            )
        } else {
            selectedPersonForDetail = null
        }
    }

    // Dialog thêm khoản nợ mới
    if (showAddDialog) {
        AddDebtLoanDialog(
            defaultType = if (selectedTab == DebtLoanMainTab.DEBT_LIST) DebtLoanType.DEBT else DebtLoanType.LOAN,
            initialPersonName = prefillPersonName,
            onDismiss = { showAddDialog = false },
            onConfirm = { onSave(it); showAddDialog = false }
        )
    }

    // Dialog trả nợ một phần
    if (showPartialPayDialog != null) {
        PartialPaymentDialog(
            item = showPartialPayDialog!!,
            onDismiss = { showPartialPayDialog = null },
            onConfirm = { paidDelta ->
                val it = showPartialPayDialog!!
                val newPaidAmount = it.paidAmount + paidDelta
                val isFullyPaid = newPaidAmount >= it.amount
                val updated = it.copy(
                    paidAmount = newPaidAmount,
                    isPaid = isFullyPaid,
                    paidInstallments = if (it.isInstallment) it.paidInstallments + 1 else it.paidInstallments
                )
                onSave(updated)
                showPartialPayDialog = null
            }
        )
    }

    if (showCollectDialog && collectingItem != null) {
        CollectGroupPrepaidDialog(
            item = collectingItem!!,
            onDismiss = { showCollectDialog = false; collectingItem = null },
            onConfirm = { collectAmount ->
                onCollectGroupPrepaid(collectingItem!!, collectAmount)
                showCollectDialog = false
                collectingItem = null
            }
        )
    }
}

// ─── Person Debt Summary Card ────────────────────────────────────────────────

@Composable
fun PersonDebtSummaryCard(
    summary: PersonDebtSummary,
    onClick: () -> Unit
) {
    val isLoanNet = summary.netAmount > 0 // Họ đang nợ mình
    val isDebtNet = summary.netAmount < 0 // Mình đang nợ họ
    val netColor = if (isLoanNet) AccentGreen else if (isDebtNet) Color(0xFFEF4444) else Color.Gray

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, netColor.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(netColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    summary.personName.take(1).uppercase(),
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = netColor
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(summary.personName, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${summary.items.size} khoản nợ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (summary.totalDebtAmount > 0) {
                        Text("• Nợ họ: ${formatCurrency(summary.totalDebtAmount)}", fontSize = 11.sp, color = Color(0xFFEF4444))
                    }
                    if (summary.totalLoanAmount > 0) {
                        Text("• Họ nợ: ${formatCurrency(summary.totalLoanAmount)}", fontSize = 11.sp, color = AccentGreen)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (isLoanNet) "+${formatCurrency(summary.netAmount)}"
                    else if (isDebtNet) "-${formatCurrency(abs(summary.netAmount))}"
                    else "Đã thanh toán hết",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = netColor
                )
                Text(
                    if (isLoanNet) "Họ nợ bạn" else if (isDebtNet) "Bạn nợ họ" else "0 đ",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
        }
    }
}

// ─── Person Debt Detail Modal Sheet ───────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDebtDetailSheet(
    summary: PersonDebtSummary,
    wallet: AppUserWallet?,
    onDismiss: () -> Unit,
    onAddNewDebt: () -> Unit,
    onTogglePaid: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onPartialPay: (DebtLoan) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showQrDialog by remember { mutableStateOf<DebtLoan?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(PrimaryBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(summary.personName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(summary.personName, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text("Chi tiết các khoản nợ & cho vay", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
            }

            Spacer(Modifier.height(16.dp))

            // Action Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onAddNewDebt,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Thêm khoản nợ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (summary.totalLoanAmount > 0) {
                    OutlinedButton(
                        onClick = {
                            val account = wallet?.accounts?.firstOrNull { it.accountNumber.isNotBlank() } ?: wallet?.accounts?.firstOrNull()
                            val msg = VietQrGenerator.generateTransferMessage(
                                bankDisplayName = account?.displayName ?: "Ngân hàng",
                                accountNumber = account?.accountNumber ?: "Chưa có STK",
                                accountName = "FinFit",
                                amount = summary.totalLoanAmount,
                                description = "Tra no ${summary.personName}"
                            )
                            clipboardManager.setText(AnnotatedString(msg))
                            Toast.makeText(context, "Đã sao chép tin nhắn nhắc nợ kèm thông tin chuyển khoản!", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp), tint = PrimaryBlue)
                        Spacer(Modifier.width(4.dp))
                        Text("Nhắc nợ nhanh", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Danh sách các khoản (${summary.items.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))

            summary.items.forEach { item ->
                DebtLoanItemCard(
                    item = item,
                    onTogglePaid = { onTogglePaid(item.id, !item.isPaid) },
                    onDelete = { onDelete(item.id) },
                    onPartialPay = { onPartialPay(item) },
                    onShowQr = { showQrDialog = item }
                )
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showQrDialog != null) {
        QuickVietQrDialog(
            item = showQrDialog!!,
            wallet = wallet,
            onDismiss = { showQrDialog = null }
        )
    }
}

// ─── Debt Loan Item Card ─────────────────────────────────────────────────────

@Composable
fun DebtLoanItemCard(
    item: DebtLoan,
    onTogglePaid: () -> Unit,
    onDelete: () -> Unit,
    onPartialPay: () -> Unit = {},
    onShowQr: () -> Unit = {}
) {
    val statusColor = if (item.isPaid) AccentGreen else if (item.type == DebtLoanType.DEBT) Color(0xFFEF4444) else PrimaryBlue
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val isOverdue = item.dueDate?.let { it.toDate().before(Date()) && !item.isPaid } ?: false
    val daysLeft = item.dueDate?.let {
        val diff = it.toDate().time - System.currentTimeMillis()
        (diff / (1000 * 60 * 60 * 24)).toInt()
    } ?: 999

    val accruedInterest = item.calculateAccruedInterest()

    Card(
        modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = if (item.isPaid) 0.65f else 1f },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(statusColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (item.isPaid) Icons.Default.CheckCircle else if (item.type == DebtLoanType.DEBT) Icons.Default.CallReceived else Icons.Default.CallMade,
                        null, tint = statusColor, modifier = Modifier.size(22.dp)
                    )
                }
                
                Spacer(Modifier.width(14.dp))
                
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.personName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (item.isPaid) Color.Gray else MaterialTheme.colorScheme.onSurface)
                        if (item.isInstallment) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF8B5CF6).copy(alpha = 0.12f)).padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("Trả góp ${item.paidInstallments}/${item.totalInstallments}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                            }
                        }
                    }
                    if (item.note.isNotEmpty()) {
                        Text(item.note, fontSize = 12.sp, color = Color.Gray)
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatCurrency(item.remainingAmount),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = if (item.isPaid) Color.Gray else statusColor
                    )
                    if (item.paidAmount > 0 && !item.isPaid) {
                        Text("Đã trả: ${formatCurrency(item.paidAmount)}", fontSize = 10.sp, color = AccentGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Badges row: Due date countdown & Accrued Interest
            if (!item.isPaid && (item.dueDate != null || item.interestRate > 0)) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (item.dueDate != null) {
                        val dueBadgeColor = when {
                            isOverdue -> Color(0xFFEF4444)
                            daysLeft <= 1 -> Color(0xFFEF4444)
                            daysLeft <= 3 -> Color(0xFFF59E0B)
                            else          -> Color.Gray
                        }
                        val dueText = when {
                            isOverdue -> "Quá hạn ${abs(daysLeft)} ngày"
                            daysLeft == 0 -> "Hạn chót hôm nay"
                            daysLeft <= 3 -> "Đến hạn trong $daysLeft ngày"
                            else          -> "Hạn: ${sdf.format(item.dueDate.toDate())}"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(dueBadgeColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(dueText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = dueBadgeColor)
                        }
                    }

                    if (accruedInterest > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Lãi: +${formatCurrency(accruedInterest)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!item.isPaid) {
                    TextButton(onClick = onPartialPay) {
                        Text("Trả một phần", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    }
                    if (item.type == DebtLoanType.LOAN) {
                        IconButton(onClick = onShowQr, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.QrCode, "VietQR", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                IconButton(
                    onClick = onTogglePaid,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = statusColor.copy(alpha = 0.08f))
                ) {
                    Icon(if (item.isPaid) Icons.Default.History else Icons.Default.Check, null, tint = statusColor, modifier = Modifier.size(16.dp))
                }

                Spacer(Modifier.width(6.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Red.copy(alpha = 0.08f))
                ) {
                    Icon(Icons.Default.DeleteOutline, null, tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ─── Dialog Trả Một Phần ─────────────────────────────────────────────────────

@Composable
fun PartialPaymentDialog(
    item: DebtLoan,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    val remaining = item.remainingAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ghi nhận thanh toán", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Người liên quan: ${item.personName}\nCòn lại phải trả: ${formatCurrency(remaining)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Nút nhanh: Trả hết
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimaryBlue.copy(alpha = 0.1f))
                        .clickable { amountText = remaining.toLong().toString() }
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Thanh toán toàn bộ", fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                    Text(formatCurrency(remaining), fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.Black)
                }

                VnAmountTextField(
                    rawValue = amountText,
                    onValueChange = { amountText = it },
                    label = "Số tiền trả lần này (đ)",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) onConfirm(amt)
                },
                enabled = amountText.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Xác nhận") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
        shape = RoundedCornerShape(24.dp)
    )
}

// ─── Dialog Thêm Khoản Nợ / Cho Vay Mới ───────────────────────────────────────

@Composable
fun AddDebtLoanDialog(
    defaultType: DebtLoanType,
    initialPersonName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (DebtLoan) -> Unit
) {
    var person by remember { mutableStateOf(initialPersonName) }
    var phone by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(defaultType) }
    var note by remember { mutableStateOf("") }
    var interestRateText by remember { mutableStateOf("") }
    var isInstallment by remember { mutableStateOf(false) }
    var totalInstallmentsText by remember { mutableStateOf("6") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (type == DebtLoanType.DEBT) "Thêm khoản tôi đi vay" else "Thêm khoản tôi cho vay", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(4.dp)
                ) {
                    Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(8.dp)).background(if (type == DebtLoanType.DEBT) Color(0xFFEF4444) else Color.Transparent).clickable { type = DebtLoanType.DEBT }, contentAlignment = Alignment.Center) {
                        Text("Tôi đi vay", color = if (type == DebtLoanType.DEBT) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(8.dp)).background(if (type == DebtLoanType.LOAN) PrimaryBlue else Color.Transparent).clickable { type = DebtLoanType.LOAN }, contentAlignment = Alignment.Center) {
                        Text("Tôi cho vay", color = if (type == DebtLoanType.LOAN) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                OutlinedTextField(
                    value = person,
                    onValueChange = { person = it },
                    label = { Text("Tên người tham gia") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại (tùy chọn)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                VnAmountTextField(
                    rawValue = amount,
                    onValueChange = { amount = it },
                    label = "Số tiền (đ)",
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú/Mục đích") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                // Lãi suất
                OutlinedTextField(
                    value = interestRateText,
                    onValueChange = { interestRateText = it },
                    label = { Text("Lãi suất %/năm (nếu có)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                // Trả góp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Trả góp nhiều kỳ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Switch(checked = isInstallment, onCheckedChange = { isInstallment = it })
                }
                if (isInstallment) {
                    OutlinedTextField(
                        value = totalInstallmentsText,
                        onValueChange = { totalInstallmentsText = it },
                        label = { Text("Số kỳ trả góp (tháng)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    val totalInst = totalInstallmentsText.toIntOrNull() ?: 1
                    if (person.isNotEmpty() && amt > 0) {
                        onConfirm(
                            DebtLoan(
                                id = UUID.randomUUID().toString(),
                                personName = person,
                                personPhone = phone,
                                amount = amt,
                                type = type,
                                note = note,
                                interestRate = interestRateText.toDoubleOrNull() ?: 0.0,
                                isInstallment = isInstallment,
                                totalInstallments = if (isInstallment) totalInst else 1,
                                installmentAmount = if (isInstallment && totalInst > 0) amt / totalInst else 0.0
                            )
                        )
                    }
                },
                enabled = person.isNotBlank() && amount.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
        shape = RoundedCornerShape(24.dp)
    )
}

// ─── Dialog VietQR Thanh Toán Nhanh ──────────────────────────────────────────

@Composable
fun QuickVietQrDialog(
    item: DebtLoan,
    wallet: AppUserWallet?,
    onDismiss: () -> Unit
) {
    val account = wallet?.accounts?.firstOrNull { it.accountNumber.isNotBlank() } ?: wallet?.accounts?.firstOrNull()
    val qrUrl = if (account != null && account.accountNumber.isNotBlank()) {
        VietQrGenerator.generateQrUrl(
            bankCode = account.bankCode,
            accountNumber = account.accountNumber,
            amount = item.remainingAmount,
            description = "Tra no ${item.personName}",
            accountName = account.name
        )
    } else ""

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mã VietQR thanh toán", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Người nợ có thể quét mã này qua bất kỳ ứng dụng ngân hàng nào để chuyển tiền đúng số tài khoản và nội dung.",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (qrUrl.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        coil.compose.AsyncImage(
                            model = qrUrl,
                            contentDescription = "VietQR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Vui lòng vào Quản lý ví để cập nhật Số tài khoản ngân hàng trước khi tạo mã VietQR.",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp),
                            color = Color.Gray
                        )
                    }
                }

                Text(
                    "Số tiền: ${formatCurrency(item.remainingAmount)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = PrimaryBlue
                )
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Đóng") } },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun EmptyDebtLoanState(isDebt: Boolean, customText: String? = null) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(120.dp).clip(CircleShape).background(PrimaryBlue.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
            Icon(if (isDebt) Icons.Default.BackHand else Icons.Default.VolunteerActivism, null, tint = PrimaryBlue.copy(alpha = 0.3f), modifier = Modifier.size(60.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(customText ?: if (isDebt) "Bạn không có nợ ai cả" else "Không ai nợ bạn", fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(
            "Tài chính đang được quản lý minh bạch và gọn gàng! 🎉",
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

// ─── Card hiển thị 1 khoản trả trước nhóm ────────────────────────────────────
@Composable
fun GroupPrepaidItemCard(
    item: com.example.finfit.finance.model.GroupPrepaidItem,
    onCollectPartial: () -> Unit,
    onCollectFull: () -> Unit
) {
    val remaining = (item.groupOwedAmount - item.collectedAmount).coerceAtLeast(0.0)
    val progress = if (item.groupOwedAmount > 0) (item.collectedAmount / item.groupOwedAmount).coerceIn(0.0, 1.0).toFloat() else 0f
    val accentOrange = Color(0xFFE67E22)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🧾", fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.description.ifBlank { "Chia tiền nhóm" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        "${item.participantCount} người tham gia • Còn lại: ${formatCurrency(remaining)}",
                        fontSize = 12.sp,
                        color = accentOrange,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    formatCurrency(item.groupOwedAmount),
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = accentOrange
                )
            }

            Spacer(Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = accentOrange,
                trackColor = accentOrange.copy(alpha = 0.15f)
            )

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                    onClick = onCollectPartial,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Thu 1 phần", fontSize = 12.sp)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onCollectFull,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentOrange)
                ) {
                    Text("Thu hết", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CollectGroupPrepaidDialog(
    item: com.example.finfit.finance.model.GroupPrepaidItem,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    val remaining = (item.groupOwedAmount - item.collectedAmount).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thu tiền chia bill nhóm", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Mục: ${item.description}\nCòn phải thu: ${formatCurrency(remaining)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                VnAmountTextField(
                    rawValue = amountText,
                    onValueChange = { amountText = it },
                    label = "Số tiền thu được (đ)",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) onConfirm(amt)
                },
                enabled = amountText.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Xác nhận") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
        shape = RoundedCornerShape(24.dp)
    )
}
