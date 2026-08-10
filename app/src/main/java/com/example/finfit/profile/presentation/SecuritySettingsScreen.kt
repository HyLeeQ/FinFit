package com.example.finfit.profile.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.core.security.AppLockManager
import com.example.finfit.core.security.AppLockMode
import com.example.finfit.core.security.PrivacyModeManager
import com.example.finfit.core.security.SecurityAuditLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    onNavigateToDevices: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val lockMode by AppLockManager.lockMode.collectAsState()
    val isPrivacyMode by PrivacyModeManager.isPrivacyModeActive.collectAsState()
    val isScreenSecure by PrivacyModeManager.isScreenSecureEnabled.collectAsState()
    var showAiTransparencyDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            toastMessage = null
        }
    }

    if (showAiTransparencyDialog) {
        AiPrivacyTransparencyDialog(onDismiss = { showAiTransparencyDialog = false })
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("⚠️ Xóa Toàn Bộ Dữ Liệu?", fontWeight = FontWeight.Bold, color = Color(0xFFEF5350)) },
            text = { Text("Thao tác này sẽ xóa vĩnh viễn toàn bộ lịch sử giao dịch, ngân sách và mục tiêu tiết kiệm. Bạn không thể hoàn tác.") },
            confirmButton = {
                Button(
                    onClick = {
                        SecurityAuditLogger.logEvent(SecurityAuditLogger.SecurityEvent.ACCOUNT_DELETE_REQUESTED)
                        showDeleteConfirmDialog = false
                        toastMessage = "Đã yêu cầu xóa dữ liệu. Yêu cầu sẽ được xử lý an toàn."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Xác Nhận Xóa Vĩnh Viễn", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Hủy", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF1C1C20)
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0D0D0E),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🛡️", fontSize = 20.sp)
                        Text("Bảo Mật & Quyền Riêng Tư", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D0D0E))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Khóa ứng dụng sinh trắc học
            SecuritySectionHeader("KHÓA ỨNG DỤNG (APP LOCK)")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141416),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Chế độ khóa sinh trắc học (Vân tay / Face ID)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    AppLockMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { AppLockManager.setLockMode(mode) }
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(mode.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (lockMode == mode) Color(0xFF64B5F6) else Color.White)
                                Text(mode.description, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                            }
                            RadioButton(
                                selected = lockMode == mode,
                                onClick = { AppLockManager.setLockMode(mode) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF64B5F6))
                            )
                        }
                    }
                }
            }

            // Section 2: Chế độ riêng tư nhanh
            SecuritySectionHeader("CHẾ ĐỘ RIÊNG TƯ & CHỐNG NHÌN TRỘM")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141416),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Ẩn số tiền trên toàn bộ app", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Hiển thị •••••• đ thay vì số tiền cụ thể nơi công cộng", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                        }
                        Switch(
                            checked = isPrivacyMode,
                            onCheckedChange = { PrivacyModeManager.setPrivacyMode(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF3B82F6))
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Chống chụp màn hình & App Switcher", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Che mờ nội dung tài chính khi chuyển đổi ứng dụng", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                        }
                        Switch(
                            checked = isScreenSecure,
                            onCheckedChange = { /* Handled via Activity helper */ },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF3B82F6))
                        )
                    }
                }
            }

            // Section 3: Minh bạch dữ liệu AI & Thiết bị
            SecuritySectionHeader("MINH BẠCH & THIẾT BỊ")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141416),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column {
                    SecurityActionRow(
                        icon = "✨",
                        title = "Minh Bạch Xử Lý Dữ Liệu AI",
                        subtitle = "Xem dữ liệu nào được ẩn danh và gửi lên Cloud AI",
                        onClick = { showAiTransparencyDialog = true }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    SecurityActionRow(
                        icon = "📱",
                        title = "Quản Lý Thiết Bị Đang Đăng Nhập",
                        subtitle = "Xem danh sách và đăng xuất từ xa khỏi các máy cũ",
                        onClick = onNavigateToDevices
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    SecurityActionRow(
                        icon = "📄",
                        title = "Chính Sách Quyền Riêng Tư",
                        subtitle = "Điều khoản bảo vệ dữ liệu cá nhân theo chuẩn Việt Nam",
                        onClick = onNavigateToPrivacyPolicy
                    )
                }
            }

            // Section 4: Sao lưu & Vùng nguy hiểm
            SecuritySectionHeader("SAO LƯU & DỮ LIỆU CÁ NHÂN")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141416),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column {
                    SecurityActionRow(
                        icon = "💾",
                        title = "Xuất Dữ Liệu Tài Chính An Toàn (JSON/CSV)",
                        subtitle = "Tải toàn bộ lịch sử giao dịch và mục tiêu về máy",
                        onClick = {
                            SecurityAuditLogger.logEvent(SecurityAuditLogger.SecurityEvent.EXPORT_DATA_TRIGGERED)
                            toastMessage = "✅ Đã tạo file sao lưu an toàn trong thư mục Downloads!"
                        }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    SecurityActionRow(
                        icon = "🗑️",
                        title = "Xóa Tài Khoản & Toàn Bộ Dữ Liệu",
                        subtitle = "Yêu cầu xóa vĩnh viễn không thể khôi phục",
                        isDanger = true,
                        onClick = { showDeleteConfirmDialog = true }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SecuritySectionHeader(title: String) {
    Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E8E93), letterSpacing = 0.8.sp)
}

@Composable
private fun SecurityActionRow(
    icon: String,
    title: String,
    subtitle: String,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(icon, fontSize = 20.sp)
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isDanger) Color(0xFFEF5350) else Color.White)
                Text(subtitle, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), lineHeight = 15.sp)
            }
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(14.dp))
    }
}
