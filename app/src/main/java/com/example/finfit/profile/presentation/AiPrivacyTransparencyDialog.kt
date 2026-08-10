package com.example.finfit.profile.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun AiPrivacyTransparencyDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF141416),
            border = BorderStroke(1.dp, Color(0xFF64B5F6).copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("✨", fontSize = 20.sp)
                        Text("Minh Bạch Dữ Liệu AI", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.6f))
                    }
                }

                Text(
                    "FinFit cam kết bảo vệ dữ liệu cá nhân của bạn theo tiêu chuẩn cao nhất. Dưới đây là cách dữ liệu được phân luồng xử lý:",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    lineHeight = 16.sp
                )

                // Block 1: On-Device Processing
                PrivacyTransparencyBlock(
                    icon = "📱",
                    title = "Xử Lý Cục Bộ Trên Máy (On-Device 100%)",
                    description = "• Nhận diện hóa đơn OCR (Google ML Kit Offline)\n• Nhận diện món ăn (TensorFlow Lite YOLO)\n• Bộ phân tích câu lệnh nhanh (LocalAIEngine)\n• Cơ sở dữ liệu SQLite/Room mã hóa",
                    badgeColor = Color(0xFF81C784)
                )

                // Block 2: Cloud AI Processing
                PrivacyTransparencyBlock(
                    icon = "☁️",
                    title = "Xử Lý Qua Gemini Cloud AI",
                    description = "• Phân tích tư vấn tài chính chuyên sâu từ Trợ lý Fitie\n• Tự động che giấu số tài khoản ngân hàng\n• Tự động ẩn danh tên người trong sổ nợ\n• Tuyệt đối không dùng dữ liệu của bạn để huấn luyện mô hình",
                    badgeColor = Color(0xFF64B5F6)
                )

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text("Đã Hiểu & Đồng Ý", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun PrivacyTransparencyBlock(
    icon: String,
    title: String,
    description: String,
    badgeColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1B1B1F),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(icon, fontSize = 16.sp)
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = badgeColor)
            }
            Text(description, fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f), lineHeight = 16.sp)
        }
    }
}
