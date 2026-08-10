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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.core.device.DeviceSessionManager
import com.example.finfit.core.device.UserDeviceSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagementScreen(
    onBack: () -> Unit = {}
) {
    val activeDevices by DeviceSessionManager.observeActiveDevices().collectAsState(initial = emptyList())
    var toastMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            toastMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0D0D0E),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📱", fontSize = 20.sp)
                        Text("Thiết Bị Đang Đăng Nhập", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
            // Header Info Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141416),
                border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🛡️", fontSize = 20.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Bảo Mật Đa Thiết Bị", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            "Dữ liệu được đồng bộ an toàn thời gian thực. Bạn có thể đăng xuất khỏi các thiết bị cũ từ xa bất kỳ lúc nào.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Text("DANH SÁCH THIẾT BỊ (${activeDevices.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E8E93), letterSpacing = 0.5.sp)

            // Devices List
            activeDevices.forEach { device ->
                DeviceCardItem(
                    device = device,
                    onRevoke = {
                        val success = DeviceSessionManager.revokeDeviceSession(device.deviceId)
                        if (success) {
                            toastMessage = "Đã đăng xuất khỏi ${device.deviceName} thành công!"
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DeviceCardItem(
    device: UserDeviceSession,
    onRevoke: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN")).format(Date(device.lastActiveTimestamp))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF17171A),
        border = BorderStroke(1.dp, if (device.isCurrentDevice) Color(0xFF4CAF50).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222228)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (device.isCurrentDevice) "📲" else "💻", fontSize = 20.sp)
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(device.deviceName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            if (device.isCurrentDevice) {
                                Surface(
                                    color = Color(0xFF2E7D32).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Thiết bị này", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Text("${device.osVersion} • ${device.appVersion}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                }

                if (!device.isCurrentDevice) {
                    IconButton(onClick = onRevoke, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Đăng xuất từ xa", tint = Color(0xFFEF5350))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Vị trí: ${device.ipAddressOrCity}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                Text("Hoạt động: $dateStr", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
            }
        }
    }
}
