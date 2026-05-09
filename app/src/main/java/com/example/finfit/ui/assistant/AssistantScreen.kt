package com.example.finfit.ui.assistant

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finfit.finance.model.*
import com.example.finfit.finance.repository.FirestoreRepository
import com.example.finfit.finance.util.LocalAIEngine
import com.example.finfit.finance.util.SmartTransactionParser
import com.example.finfit.ui.theme.AccentGreen
import com.example.finfit.finance.ui.utils.formatCurrency
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    firestoreRepository: FirestoreRepository,
    userId: String,
    wallet: AppUserWallet?,
    transactions: List<FinanceTransaction>,
    schedule: List<SpendingScheduleItem>,
    debtLoans: List<DebtLoan>,
    savingsGoals: List<SavingsGoal>,
    budgets: List<FinanceBudget>,
    habit: UserHabit?,
    stepsToday: Int,
    onBack: () -> Unit
) {
    val viewModel: AssistantViewModel = viewModel(
        factory = AssistantViewModelFactory(firestoreRepository, userId, wallet, transactions, schedule, debtLoans, savingsGoals, budgets, stepsToday, habit)
    )
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Đồng bộ data thời gian thực vào ViewModel do ViewModel retaining state cũ
    LaunchedEffect(wallet, transactions, schedule, debtLoans, savingsGoals, budgets, stepsToday) {
        viewModel.wallet = wallet
        viewModel.transactions = transactions
        viewModel.schedule = schedule
        viewModel.debtLoans = debtLoans
        viewModel.savingsGoals = savingsGoals
        viewModel.budgets = budgets
        viewModel.stepsToday = stepsToday
    }
    
    // Kiểm tra Thứ Hai chủ động khi mở màn hình
    LaunchedEffect(Unit) {
        viewModel.checkMondayProactive()
        viewModel.checkBudgetAlerts()   // Cảnh báo ngân sách (local, no API)
    }

    // Smart suggestion chips (update khi data thay đổi)
    val suggestions = remember(transactions) { viewModel.getSmartSuggestions() }

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var isLocalProcessing by remember { mutableStateOf(false) }

    var pendingBillUri by remember { mutableStateOf<Uri?>(null) }
    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingBillUri = uri
        }
    }

    LaunchedEffect(pendingBillUri) {
        val uri = pendingBillUri ?: return@LaunchedEffect
        pendingBillUri = null
        isLocalProcessing = true

        viewModel.addLocalMessage(MessageContent.Text("📷 Đang quét hóa đơn..."), true)

        try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val ocrText = visionText.text
                    val amount = SmartTransactionParser.extractTotalFromBill(ocrText)
                    viewModel.removeLatestLocalMessage()
                    viewModel.addLocalMessage(MessageContent.BillCard(imageUri = uri, extractedAmount = amount), false)
                    isLocalProcessing = false
                }
                .addOnFailureListener {
                    viewModel.removeLatestLocalMessage()
                    viewModel.addLocalMessage(MessageContent.Text("❌ Không thể đọc ảnh hóa đơn bạn rảnh. Vui lòng nhập thủ công."), false)
                    isLocalProcessing = false
                }
        } catch (e: Exception) {
            viewModel.removeLatestLocalMessage()
            viewModel.addLocalMessage(MessageContent.Text("❌ Lỗi: ${e.message}"), false)
            isLocalProcessing = false
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(36.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Trợ lý AI Đầu tư", fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text("Chuyển text thành Data", fontSize = 10.sp, color = AccentGreen)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    // Quick insight button — no API needed
                    IconButton(onClick = {
                        viewModel.addLocalMessage(
                            MessageContent.Text(viewModel.getLocalInsight()),
                            isUser = false
                        )
                    }) {
                        Icon(Icons.Default.Insights, contentDescription = "Tổng quan nhanh",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // ── Smart suggestion chips ────────────────────────
                if (suggestions.isNotEmpty() && inputText.isBlank()) {
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(suggestions) { s ->
                            SuggestionChip(
                                onClick = {
                                    // Tap chip = gửi luôn (local parse sẽ bắt trước API)
                                    inputText = ""
                                    viewModel.sendMessage(s.text)
                                },
                                label = { Text("${s.emoji} ${s.label}", fontSize = 12.sp) },
                                shape = RoundedCornerShape(20.dp),
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                ),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    borderWidth = 1.dp
                                )
                            )
                        }
                    }
                }
                SmartChatInput(
                    text = inputText,
                    onTextChange = { inputText = it },
                    onSend = {
                        val text = inputText.trim()
                        if (text.isBlank()) return@SmartChatInput
                        inputText = ""

                        val parsed = SmartTransactionParser.parse(text)
                        if (parsed != null && parsed.amount > 0) {
                            viewModel.addLocalMessage(MessageContent.Text(text), true)
                            viewModel.addLocalMessage(MessageContent.TransactionCard(parsed), false)
                        } else {
                            viewModel.sendMessage(text)
                        }
                    },
                    onCamera = { imageLauncher.launch("image/*") },
                    isLoading = isLoading || isLocalProcessing
                )
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    when (val content = msg.content) {
                        is MessageContent.Text ->
                            BubbleText(content.text, msg.isUser)
                            
                        is MessageContent.TransactionCard ->
                            TransactionConfirmCard(
                                parsed = content.parsed,
                                isConfirmed = content.confirmed,
                                wallet = wallet,
                                onConfirm = { tx, updatedWallet ->
                                    viewModel.confirmTransaction(tx, updatedWallet, msg.id, content.parsed)
                                },
                                onDismiss = {
                                    viewModel.updateMessage(msg.id, MessageContent.TransactionCard(content.parsed, true))
                                    viewModel.addLocalMessage(MessageContent.Text("Đã huỷ."))
                                }
                            )

                        is MessageContent.BillCard ->
                            BillConfirmCard(
                                imageUri = content.imageUri, extractedAmount = content.extractedAmount, isConfirmed = content.confirmed, wallet = wallet,
                                onConfirm = { tx, updatedWallet -> viewModel.confirmBill(tx, updatedWallet, msg.id, content.imageUri, content.extractedAmount) },
                                onDismiss = { viewModel.updateMessage(msg.id, MessageContent.BillCard(content.imageUri, content.extractedAmount, true)) }
                            )
                            
                        is MessageContent.DebtCard ->
                            DebtConfirmCard(
                                debt = content.debt,
                                isConfirmed = content.confirmed,
                                onConfirm = { updatedDebt -> viewModel.confirmDebtLoan(updatedDebt, msg.id) },
                                onDismiss = { viewModel.updateMessage(msg.id, MessageContent.DebtCard(content.debt, true)) }
                            )
                            
                        is MessageContent.SavingsCard ->
                            GenericConfirmCard(
                                title = "Tạo Tiết Kiệm Mới",
                                icon = Icons.Default.Savings,
                                isConfirmed = content.confirmed,
                                infoLines = listOf("Mục tiêu: ${content.goal.goalName}", "Ngân sách cần: ${formatCurrency(content.goal.targetAmount)}"),
                                onConfirm = { viewModel.confirmSavingsGoal(content.goal, msg.id) },
                                onDismiss = { viewModel.updateMessage(msg.id, MessageContent.SavingsCard(content.goal, true)) }
                            )
                            
                        is MessageContent.BudgetCard ->
                            GenericConfirmCard(
                                title = "Thiết lập Hạn mức",
                                icon = Icons.Default.AttachMoney,
                                isConfirmed = content.confirmed,
                                infoLines = listOf("Mục chi tiêu: ${content.budget.category}", "Số tiền tối đa: ${formatCurrency(content.budget.amount)}", "Định kỳ: ${content.budget.period.name}"),
                                onConfirm = { viewModel.confirmBudget(content.budget, msg.id) },
                                onDismiss = { viewModel.updateMessage(msg.id, MessageContent.BudgetCard(content.budget, true)) }
                            )
                            
                        is MessageContent.ScheduleCard -> 
                            ScheduleConfirmCard(
                                item = content.item,
                                isConfirmed = content.confirmed,
                                onConfirm = { updatedItem -> viewModel.confirmSchedule(updatedItem, msg.id) },
                                onDismiss = { viewModel.updateMessage(msg.id, content.copy(confirmed = true)) }
                            )
                            
                        is MessageContent.SplitBillCard ->
                            SplitBillConfirmCard(
                                totalAmount = content.totalAmount,
                                participantCount = content.participantCount,
                                category = content.category,
                                note = content.note,
                                isConfirmed = content.confirmed,
                                initialParticipants = content.initialParticipants,
                                onConfirm = { t, pList, cat, n -> 
                                    viewModel.confirmSplitBill(t, pList, cat, n, msg.id)
                                },
                                onDismiss = { 
                                    viewModel.updateMessage(msg.id, content.copy(confirmed = true))
                                }
                            )
                            
                        is MessageContent.HeldFundCard ->
                            HeldFundConfirmCard(
                                fundName = content.fundName,
                                amount = content.amount,
                                isConfirmed = content.confirmed,
                                onConfirm = { n, a -> viewModel.confirmHeldFund(n, a, msg.id) },
                                onDismiss = { viewModel.updateMessage(msg.id, content.copy(confirmed = true)) }
                            )

                        is MessageContent.HabitUpdateCard ->
                            HabitUpdateCard(
                                habit = content.habit,
                                isConfirmed = content.confirmed,
                                onConfirm = { viewModel.confirmUserHabit(content.habit, msg.id) },
                                onDismiss = { viewModel.updateMessage(msg.id, content.copy(confirmed = true)) }
                            )

                        is MessageContent.WeeklyPlanCard ->
                            WeeklyPlanCard(
                                description = content.description,
                                items = content.items,
                                isConfirmed = content.confirmed,
                                onConfirm = { viewModel.confirmWeeklyPlan(content.items, msg.id, content.description) },
                                onDismiss = { viewModel.updateMessage(msg.id, content.copy(confirmed = true)) }
                            )

                        is MessageContent.DepositSavingsCard ->
                            DepositSavingsConfirmCard(
                                goalName = content.goalName,
                                amount = content.amount,
                                walletSource = content.walletSource,
                                isConfirmed = content.confirmed,
                                wallet = wallet,
                                onConfirm = { goal, amt, sourceId ->
                                    viewModel.confirmDepositSavings(goal, amt, sourceId, msg.id)
                                },
                                onDismiss = { viewModel.updateMessage(msg.id, content.copy(confirmed = true)) }
                            )

                        is MessageContent.WithdrawSavingsCard ->
                            WithdrawSavingsConfirmCard(
                                goalName = content.goalName,
                                amount = content.amount,
                                destinationWallet = content.destinationWallet,
                                transferToSavingsGoal = content.transferToSavingsGoal,
                                isConfirmed = content.confirmed,
                                wallet = wallet,
                                savingsGoals = savingsGoals,
                                onConfirm = { goal, amt, destId, destGoalId ->
                                    viewModel.confirmWithdrawSavings(goal, amt, destId, destGoalId, msg.id)
                                },
                                onDismiss = { viewModel.updateMessage(msg.id, content.copy(confirmed = true)) }
                            )
                    }
                }
            }
            if (isLoading || isLocalProcessing) item { TypingIndicator() }
        }
    }
}
