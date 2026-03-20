package com.example.finfit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.ui.theme.*
import com.example.finfit.data.local.ThemeMode

@Composable
fun ProfileScreen(
    email: String, 
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onLogout: () -> Unit
) {
    var showThemeDialog by remember { mutableStateOf(false) }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Chọn giao diện") },
            text = {
                Column {
                    ThemeOptionRow("Theo hệ thống", themeMode == com.example.finfit.data.local.ThemeMode.SYSTEM) {
                        onThemeChange(com.example.finfit.data.local.ThemeMode.SYSTEM)
                        showThemeDialog = false
                    }
                    ThemeOptionRow("Sáng", themeMode == com.example.finfit.data.local.ThemeMode.LIGHT) {
                        onThemeChange(com.example.finfit.data.local.ThemeMode.LIGHT)
                        showThemeDialog = false
                    }
                    ThemeOptionRow("Tối", themeMode == com.example.finfit.data.local.ThemeMode.DARK) {
                        onThemeChange(com.example.finfit.data.local.ThemeMode.DARK)
                        showThemeDialog = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Đóng") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tiêu đề với hình bánh răng (Settings icon)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Cài đặt cá nhân",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Avatar giả lập
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(email, color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Thành viên Premium", color = PrimaryBlue, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(48.dp))

        // Danh sách cài đặt
        val currentThemeLabel = when(themeMode) {
            com.example.finfit.data.local.ThemeMode.SYSTEM -> "Theo hệ thống"
            com.example.finfit.data.local.ThemeMode.LIGHT -> "Sáng"
            com.example.finfit.data.local.ThemeMode.DARK -> "Tối"
        }
        ProfileSettingsItemRow(Icons.Default.Palette, "Giao diện", currentThemeLabel, onClick = { showThemeDialog = true })
        
        val context = androidx.compose.ui.platform.LocalContext.current
        val isNotiEnabled = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            ?.contains(context.packageName) ?: false
            
        ProfileSettingsItemRow(
            Icons.Default.Notifications, 
            "Đồng bộ ngân hàng", 
            if (isNotiEnabled) "Tự động (+/-) số dư" else "Chưa cấp quyền",
            onClick = {
                context.startActivity(android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply { 
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        )

        ProfileSettingsItemRow(Icons.Default.Security, "Bảo mật", "Vân tay / PIN")

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Đăng xuất", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfileSettingsItemRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ThemeOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface)
        RadioButton(selected = selected, onClick = onClick)
    }
}
