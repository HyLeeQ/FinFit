package com.example.finfit.health.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape

// Biểu đồ màu dùng chung mô phỏng file theme
import com.example.finfit.ui.theme.PrimaryBlue
import com.example.finfit.ui.theme.TextWhite
import com.example.finfit.ui.theme.TextGray

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
                        tint = TextWhite
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
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    "Chào buổi sáng, ${userEmail.split("@")[0]}",
                    color = TextGray,
                    fontSize = 12.sp
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = TextWhite)
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
    val cards = listOf(
        HealthCardItem(
            title = "Theo dõi bước chân",
            subtitle = "Theo dõi số bước và calo mỗi ngày",
            icon = Icons.Default.DirectionsWalk,
            route = "stepCounter"
        ),
        HealthCardItem(
            title = "AI quét món ăn",
            subtitle = "Nhận diện món ăn và tính calo",
            icon = Icons.Default.CameraAlt,
            route = "food_scanner"
        ),
        HealthCardItem(
            title = "Thống kê sức khỏe",
            subtitle = "Xem lại thành tích và thay đổi",
            icon = Icons.Default.BarChart,
            route = "health_stats"
        ),
        HealthCardItem(
            title = "Dự báo sức khỏe",
            subtitle = "Kết quả dự kiến từ thói quen",
            icon = Icons.Default.TrendingUp,
            route = "health_prediction"
        ),
        HealthCardItem(
            title = "Nhật ký sức khỏe",
            subtitle = "Ghi chép cảm xúc và giấc ngủ",
            icon = Icons.Default.Book,
            route = "health_log"
        )
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        HealthHeaderSection(
            title = "Sức khỏe FinFit",
            userEmail = userEmail,
            showBackButton = false
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp) // Offset the FAB / Bottom Bar
        ) {
            items(cards) { item ->
                HealthCard(
                    title = item.title,
                    subtitle = item.subtitle,
                    icon = item.icon,
                    onClick = { onNavigate(item.route) }
                )
            }
        }
    }
}

@Composable
fun HealthCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .clip(RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}
