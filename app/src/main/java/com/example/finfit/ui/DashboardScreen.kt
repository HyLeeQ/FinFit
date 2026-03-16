package com.example.finfit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.data.model.UserWallet
import com.example.finfit.ui.theme.*

@Composable
fun DashboardScreen(userEmail: String, wallet: UserWallet?, onLogout: () -> Unit) {
    HomeContent(userEmail, wallet)
}

@Composable
fun HomeContent(userEmail: String, wallet: UserWallet?) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item { HeaderSection(userEmail) }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { WalletOverviewSection(wallet) }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { QuickActionsSection() }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { SpendingBreakdownSection() }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { RecentTransactionsSection() }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}



@Composable
fun HeaderSection(email: String) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                    modifier =
                            Modifier.size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                        "Tài chính FinFit",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                )
                Text("Chào buổi sáng, ${email.split("@")[0]}", color = TextGray, fontSize = 12.sp)
            }
        }
        Row {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = TextWhite)
            }
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray))
        }
    }
}

@Composable
fun WalletOverviewSection(wallet: UserWallet?) {
    Column {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tổng quan ví", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Xem", color = PrimaryBlue, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                BalanceCard(
                        title = "TỔNG SỐ DƯ",
                        amount = "${wallet?.totalBalance ?: 0.0} đ",
                        colors = listOf(Color(0xFF2D82FE), Color(0xFF1E40AF))
                )
            }
            item {
                BalanceCard(
                        title = "CÓ THỂ DÙNG",
                        amount = "${wallet?.disposableAmount ?: 0.0} đ",
                        colors = listOf(Color(0xFF10C67F), Color(0xFF065F46))
                )
            }
        }
    }
}

@Composable
fun BalanceCard(title: String, amount: String, colors: List<Color>) {
    Box(
            modifier =
                    Modifier.width(280.dp)
                            .height(160.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Brush.linearGradient(colors))
                            .padding(20.dp)
    ) {
        Column {
            Text(
                    title,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
            )
            Text(amount, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("ALEX J. STERLING", color = Color.White, fontSize = 14.sp)
                Text("**** 4582", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun QuickActionsSection() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ActionItem(Icons.Default.SwapHoriz, "Chuyển tiền")
        ActionItem(Icons.Default.BarChart, "Phân tích")
        ActionItem(Icons.Default.QrCodeScanner, "Quét hóa đơn")
    }
}

@Composable
fun ActionItem(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
                modifier =
                        Modifier.size(80.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(CardBackground),
                contentAlignment = Alignment.Center
        ) {
            Icon(
                    icon,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = TextGray, fontSize = 12.sp)
    }
}

@Composable
fun SpendingBreakdownSection() {
    Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Phân tích chi tiêu", color = TextWhite, fontWeight = FontWeight.Bold)
                Text("Tuần này", color = PrimaryBlue, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Đồ thị tròn giả lập
                Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                            progress = 0.7f,
                            modifier = Modifier.fillMaxSize(),
                            color = PrimaryBlue,
                            strokeWidth = 10.dp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TỔNG", color = TextGray, fontSize = 10.sp)
                        Text("$1.2k", color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(24.dp))
                Column {
                    LegendItem(AccentGreen, "Nhà ở", "55%")
                    LegendItem(PrimaryBlue, "Ăn uống", "28%")
                    LegendItem(Color.Magenta, "Khác", "17%")
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, percent: String) {
    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = TextGray, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(percent, color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RecentTransactionsSection() {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                    "Giao dịch gần đây",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
            )
            Text("Xem tất cả", color = PrimaryBlue, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("HÔM NAY", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        TransactionListItem(
                Icons.Default.ShoppingBag,
                "Apple Store",
                "10:30 AM • Entertainment",
                "-$199.00",
                AccentRed
        )
        TransactionListItem(
                Icons.Default.AccountBalance,
                "Salary Credit",
                "08:15 AM • Income",
                "+$4,500.00",
                AccentGreen
        )
    }
}

@Composable
fun TransactionListItem(
        icon: ImageVector,
        title: String,
        sub: String,
        amount: String,
        amountColor: Color
) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(CardBackground),
                contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = Color(0xFFF59E0B)) }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextWhite, fontWeight = FontWeight.Bold)
            Text(sub, color = TextGray, fontSize = 12.sp)
        }
        Text(amount, color = amountColor, fontWeight = FontWeight.Bold)
    }
}
