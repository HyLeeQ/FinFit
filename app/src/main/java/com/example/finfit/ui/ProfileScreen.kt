package com.example.finfit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.data.local.ThemeMode
import com.example.finfit.finance.model.AppUserWallet
import com.example.finfit.finance.model.SavingsGoal
import com.example.finfit.ui.theme.*

@Composable
fun ProfileScreen(
    email: String,
    wallet: AppUserWallet?,
    goals: List<SavingsGoal>,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var transactionNotiEnabled by remember { mutableStateOf(true) }
    var budgetWarningEnabled by remember { mutableStateOf(true) }
    var periodicReportEnabled by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tài khoản", color = MaterialTheme.colorScheme.onBackground, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("v1.2.0", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // User Card Premium
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .shadow(12.dp, RoundedCornerShape(32.dp))
                    .clickable(onClick = onEditProfile),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFEC4899))
                            )
                        )
                ) {
                    // Decorative patterns
                    Box(modifier = Modifier.size(200.dp).offset(x = (-50).dp, y = (-80).dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)))
                    Box(modifier = Modifier.size(120.dp).align(Alignment.BottomEnd).offset(x = 30.dp, y = 30.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)))

                    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Avatar with Glass effect
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(email.take(1).uppercase(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                            }
                            
                            Spacer(Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Chào bạn trở lại 👋", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                Text(email.substringBefore("@"), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            }

                            // Edit icon
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Edit, "Chỉnh sửa hồ sơ", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        
                        Spacer(Modifier.weight(1f))
                        
                        // Real-time Stats
                        Row(modifier = Modifier.fillMaxWidth()) {
                            StatItem(label = "Tài sản ví", value = com.example.finfit.finance.ui.utils.formatCurrency(wallet?.totalBalance ?: 0.0), modifier = Modifier.weight(1f))
                            Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha = 0.2f)).align(Alignment.CenterVertically))
                            StatItem(label = "Đã tích luỹ", value = (goals.size).toString() + " mục tiêu", modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Tài khoản & Ví
            SectionHeader(icon = Icons.Default.AccountBalance, title = "Tài khoản & Ví")
            Spacer(modifier = Modifier.height(16.dp))
            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.Wallet,
                    iconBgColor = PrimaryBlue,
                    title = "Quản lý ví",
                    subtitle = "${wallet?.accounts?.size ?: 0} ví đang hoạt động"
                )
                DividerProfile()
                SettingsItem(
                    icon = Icons.Default.GridView,
                    iconBgColor = Color(0xFF8B5CF6),
                    title = "Danh mục chi tiêu",
                    subtitle = "Tuỳ chỉnh danh mục AI"
                )
                DividerProfile()
                SettingsItem(
                    icon = Icons.Default.TaskAlt,
                    iconBgColor = Color(0xFF10B981),
                    title = "Ngân sách",
                    subtitle = "Thiết lập hạn mức chi tiêu"
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Thông báo
            SectionHeader(icon = Icons.Default.Notifications, title = "Thông báo")
            Spacer(modifier = Modifier.height(12.dp))
            SettingsCard {
                SettingsToggleItem(
                    icon = Icons.Default.Receipt,
                    iconBgColor = Color(0xFF3B82F6),
                    title = "Giao dịch mới",
                    subtitle = "Thông báo khi có giao dịch",
                    checked = transactionNotiEnabled,
                    onCheckedChange = { transactionNotiEnabled = it }
                )
                DividerProfile()
                SettingsToggleItem(
                    icon = Icons.Default.PieChart,
                    iconBgColor = Color(0xFFEF4444),
                    title = "Cảnh báo ngân sách",
                    subtitle = "Khi chi tiêu vượt hạn mức",
                    checked = budgetWarningEnabled,
                    onCheckedChange = { budgetWarningEnabled = it }
                )
                DividerProfile()
                SettingsToggleItem(
                    icon = Icons.Default.InsertChartOutlined,
                    iconBgColor = Color(0xFFF59E0B),
                    title = "Báo cáo định kỳ",
                    subtitle = "Tổng kết hàng tuần / tháng",
                    checked = periodicReportEnabled,
                    onCheckedChange = { periodicReportEnabled = it }
                )
                DividerProfile()
                val context = LocalContext.current
                val isNotiEnabled = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")?.contains(context.packageName) ?: false
                SettingsItem(
                    icon = Icons.Default.AccountBalance,
                    iconBgColor = Color(0xFF6366F1),
                    title = "Đồng bộ ngân hàng",
                    subtitle = if (isNotiEnabled) "Đã cấp quyền đọc SMS/App" else "Nhấn để cấp quyền",
                    onClick = {
                        context.startActivity(android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply { 
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // System
            var showThemeDialog by remember { mutableStateOf(false) }
            if (showThemeDialog) {
                AlertDialog(
                    onDismissRequest = { showThemeDialog = false },
                    title = { Text("Đổi giao diện", color = Color.White) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            ThemeOptionRow("Theo hệ thống", themeMode == ThemeMode.SYSTEM) { onThemeChange(ThemeMode.SYSTEM); showThemeDialog = false }
                            ThemeOptionRow("Sáng", themeMode == ThemeMode.LIGHT) { onThemeChange(ThemeMode.LIGHT); showThemeDialog = false }
                            ThemeOptionRow("Tối", themeMode == ThemeMode.DARK) { onThemeChange(ThemeMode.DARK); showThemeDialog = false }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text("Đóng", color = Color(0xFFEAB308)) } },
                    containerColor = Color(0xFF14151B)
                )
            }
            
            SectionHeader(icon = Icons.Default.SettingsSystemDaydream, title = "Hệ thống")
            Spacer(modifier = Modifier.height(12.dp))
            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.Palette,
                    iconBgColor = Color(0xFF8B5CF6),
                    title = "Giao diện",
                    subtitle = when(themeMode) {
                        ThemeMode.SYSTEM -> "Theo hệ thống"
                        ThemeMode.LIGHT -> "Sáng"
                        ThemeMode.DARK -> "Tối"
                    },
                    onClick = { showThemeDialog = true }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
            ) {
                Icon(Icons.Default.Logout, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text("Đăng xuất", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(120.dp)) // Avoid overlap with bottom nav
        }
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun ThemeOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White)
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFEAB308), unselectedColor = Color.Gray)
        )
    }
}

@Composable
fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
fun DividerProfile() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.05f))
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    iconBgColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(iconBgColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconBgColor, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    iconBgColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBgColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconBgColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryBlue,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
