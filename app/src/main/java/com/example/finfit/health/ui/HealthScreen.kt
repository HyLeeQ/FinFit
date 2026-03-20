package com.example.finfit.health.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.*

import com.example.finfit.ui.theme.PrimaryBlue

// Màn hình HeaderSection chung cho Sức Khỏe
@Composable
fun HealthHeaderSection(
    title: String,
    userEmail: String,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.width(if (showBackButton) 8.dp else 12.dp))
            Column {
                Text(
                    title,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    "Chào buổi sáng, ${userEmail.split("@")[0]}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
            }
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray))
        }
    }
}

// Common Placeholder Screen cho các module tính năng
@Composable
fun HealthPlaceholderScreen(userEmail: String, title: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        HealthHeaderSection(
            title = title,
            userEmail = userEmail,
            showBackButton = true,
            onBackClick = onBack
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Tính năng đang phát triển",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

// 5 Màn hình con
@Composable fun StepTrackerScreen(userEmail: String, onBack: () -> Unit) = HealthPlaceholderScreen(userEmail, "Theo dõi bước chân", onBack)
@Composable fun FoodScannerScreen(userEmail: String, onBack: () -> Unit) = HealthPlaceholderScreen(userEmail, "AI quét món ăn", onBack)
@Composable fun HealthStatsScreen(userEmail: String, onBack: () -> Unit) = HealthPlaceholderScreen(userEmail, "Thống kê sức khỏe", onBack)
@Composable fun HealthPredictionScreen(userEmail: String, onBack: () -> Unit) = HealthPlaceholderScreen(userEmail, "Dự báo sức khỏe", onBack)
@Composable fun HealthLogScreen(userEmail: String, onBack: () -> Unit) = HealthPlaceholderScreen(userEmail, "Nhật ký sức khỏe", onBack)

// Dữ liệu cho Card
data class HealthCardItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun HealthDashboardScreen(userEmail: String, onNavigate: (String) -> Unit) {
    val userName = userEmail.split("@")[0]
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // --- Greeting Section ---
        item {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = "Chào buổi sáng.",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "Bắt đầu ngày mới năng động và khỏe mạnh.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- Stats Grid Section (2x2) ---
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Theo dõi nước uống
                HealthStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Theo dõi nước uống",
                    value = "0.8L",
                    progress = 0.4f,
                    icon = Icons.Rounded.WaterDrop,
                    accentColor = Color(0xFF0EA5E9),
                    onClick = { /* Navigate */ }
                )
                Spacer(modifier = Modifier.width(16.dp))
                // AI Food Scan
                HealthActionCard(
                    modifier = Modifier.weight(1f),
                    title = "AI Food Scan",
                    buttonText = "Quét ngay",
                    icon = Icons.Rounded.CameraAlt,
                    accentColor = Color(0xFF64748B),
                    onClick = { onNavigate("food_scanner") }
                )
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Vận động (Steps)
                HealthChartCard(
                    modifier = Modifier.weight(1f),
                    title = "Vận động",
                    icon = Icons.Rounded.Timeline,
                    accentColor = Color(0xFF22C55E),
                    onClick = { onNavigate("stepCounter") }
                )
                Spacer(modifier = Modifier.width(16.dp))
                // Chế độ sinh hoạt (Sleep)
                HealthStatusCard(
                    modifier = Modifier.weight(1f),
                    title = "Chế độ sinh hoạt",
                    status = "Đã ngủ 6h 12p",
                    goal = "Mục tiêu 8h",
                    icon = Icons.Rounded.Nightlight,
                    accentColor = Color(0xFFA855F7),
                    onClick = { onNavigate("health_log") }
                )
            }
        }

        // --- Insights Section ---
        item {
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kiến thức hôm nay",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = { /* See all */ }) {
                    Text("Xem tất cả", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // Mock Insight Items
        item {
            InsightItem(
                title = "Mẹo tập luyện hiệu quả",
                source = "Bí quyết tập luyện",
                icon = Icons.Rounded.FitnessCenter,
                accentColor = Color(0xFFFEF3C7)
            )
        }
        item {
            InsightItem(
                title = "Chế độ ăn sạch (Clean Eating)",
                source = "Dinh dưỡng & Sức khỏe",
                icon = Icons.Rounded.Restaurant,
                accentColor = Color(0xFFD1FAE5)
            )
        }
    }
}

@Composable
fun HealthStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    progress: Float,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(70.dp),
                    color = accentColor,
                    strokeWidth = 10.dp,
                    trackColor = accentColor.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
                Text(value, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun HealthActionCard(
    modifier: Modifier = Modifier,
    title: String,
    buttonText: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Nhận dạng calo thông minh qua Camera", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), lineHeight = 14.sp)
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(buttonText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.surface)
            }
        }
    }
}

@Composable
fun HealthChartCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.weight(1f))
            // Mock Bar Chart
            Row(
                modifier = Modifier.fillMaxWidth().height(60.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val heights = listOf(0.4f, 0.7f, 0.5f, 0.9f, 0.6f)
                heights.forEach { h ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(h)
                            .background(accentColor.copy(alpha = 0.6f), shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun HealthStatusCard(
    modifier: Modifier = Modifier,
    title: String,
    status: String,
    goal: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(status, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color(0xFFFFB200))
                Spacer(Modifier.width(4.dp))
                Text(goal, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun InsightItem(title: String, source: String, icon: ImageVector, accentColor: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = accentColor)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(text = source, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
