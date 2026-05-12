package com.example.finfit.finance.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.finfit.finance.model.*
import com.example.finfit.finance.ui.utils.formatCurrency
import com.example.finfit.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.util.*

// ─── Màn hình chính: Nhật ký ảnh ────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDiaryScreen(
    transactions: List<FinanceTransaction>,
    onBack: () -> Unit,
    onAddClick: () -> Unit = {}
) {
    // Chỉ lấy giao dịch có ảnh
    val photoTransactions = remember(transactions) {
        transactions
            .filter { !it.imageUrl.isNullOrBlank() }
            .sortedByDescending { it.timestamp.seconds }
    }

    var selectedTx by remember { mutableStateOf<FinanceTransaction?>(null) }
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    // Detail view
    if (selectedTx != null) {
        PhotoDetailView(
            transaction = selectedTx!!,
            onBack = { selectedTx = null }
        )
        return
    }

    Scaffold(containerColor = Color(0xFF0A0A0A)) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // ─── Header ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0A0A0A), Color(0xFF0A0A0A).copy(alpha = 0f)),
                            endY = 160f
                        )
                    )
                    .padding(top = 16.dp, start = 8.dp, end = 16.dp, bottom = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Nhật ký chi tiêu",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                        Text(
                            "${photoTransactions.size} khoảnh khắc",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (photoTransactions.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "Chưa có ảnh nào",
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Thêm ảnh khi tạo giao dịch\nđể lưu lại khoảnh khắc!",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Photo grid
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(400))
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(
                            items = photoTransactions,
                            key = { _, tx -> tx.id }
                        ) { index, tx ->
                            PhotoGridCell(
                                transaction = tx,
                                index = index,
                                onClick = { selectedTx = tx }
                            )
                        }
                        // Bottom spacer
                        item(span = { GridItemSpan(3) }) {
                            Spacer(Modifier.height(100.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─── Một ô trong lưới ảnh ───────────────────────────────────────────────────
@Composable
fun PhotoGridCell(
    transaction: FinanceTransaction,
    index: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isIncome = transaction.type == TransactionType.INCOME

    val amountColor = if (isIncome) Color(0xFF4ADE80) else Color.White
    val sign = if (isIncome) "+" else "-"

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index % 9) * 40L)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.85f)
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RectangleShape)
                .clickable { onClick() }
        ) {
            // Ảnh nền
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(transaction.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient overlay phía dưới
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Số tiền
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            ) {
                Text(
                    text = "$sign${formatCurrency(transaction.amount)}",
                    color = amountColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Badge số ảnh nếu category có nhiều tx cùng ngày (tuỳ chọn cosmetic)
            if (transaction.note.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue)
                )
            }
        }
    }
}

// ─── Màn hình chi tiết ảnh (giống CapMoney slide view) ──────────────────────
@Composable
fun PhotoDetailView(
    transaction: FinanceTransaction,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isIncome = transaction.type == TransactionType.INCOME
    val isTransfer = transaction.type == TransactionType.TRANSFER

    val sign = when (transaction.type) {
        TransactionType.INCOME -> "+"
        TransactionType.EXPENSE, TransactionType.GROUP_PREPAYMENT -> "-"
        TransactionType.TRANSFER -> "↔"
    }

    val amountColor = when (transaction.type) {
        TransactionType.INCOME -> Color(0xFF4ADE80)
        TransactionType.EXPENSE, TransactionType.GROUP_PREPAYMENT -> Color.White
        TransactionType.TRANSFER -> Color(0xFFA78BFA)
    }

    val dateStr = remember(transaction.timestamp) {
        SimpleDateFormat("dd MMMM yyyy 'at' HH:mm", Locale.ENGLISH)
            .format(transaction.timestamp.toDate())
    }

    var animIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animIn = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        // Full-screen photo
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(transaction.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .align(Alignment.TopCenter)
        )

        // Top gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                    )
                )
        )

        // Bottom gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF0A0A0A))
                    )
                )
        )

        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 20.dp, start = 8.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = Color.White
            )
        }

        // Date chip top center
        AnimatedVisibility(
            visible = animIn,
            enter = fadeIn(tween(400)) + slideInVertically { -20 },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        dateStr,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Bottom content panel
        AnimatedVisibility(
            visible = animIn,
            enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 3 },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Category tags
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (transaction.category.isNotBlank()) {
                        val cat = (EXPENSE_CATEGORIES + INCOME_CATEGORIES + TRANSFER_CATEGORIES)
                            .find { it.label == transaction.category }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (cat != null) cat.color.copy(alpha = 0.85f)
                                    else Color(0xFF10B981).copy(alpha = 0.85f)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (cat != null) {
                                    Icon(
                                        cat.icon,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(
                                    transaction.category,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Payment method badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1E3A5F).copy(alpha = 0.85f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (transaction.paymentMethod == PaymentMethod.BANKING)
                                    Icons.Default.AccountBalance
                                else
                                    Icons.Default.Wallet,
                                contentDescription = null,
                                tint = Color(0xFF93C5FD),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (transaction.paymentMethod == PaymentMethod.BANKING) "Banking" else "Tiền mặt",
                                color = Color(0xFF93C5FD),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Big amount
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 28.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$sign ${formatCurrency(transaction.amount)}",
                            color = amountColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 36.sp,
                            letterSpacing = (-1).sp
                        )
                        if (transaction.note.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                transaction.note,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
