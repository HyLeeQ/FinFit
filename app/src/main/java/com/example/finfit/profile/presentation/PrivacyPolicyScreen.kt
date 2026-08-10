package com.example.finfit.profile.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit = {}
) {
    Scaffold(
        containerColor = Color(0xFF0D0D0E),
        topBar = {
            TopAppBar(
                title = {
                    Text("Chính Sách Quyền Riêng Tư", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141416),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("1. Cam Kết Bảo Vệ Dữ Liệu", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64B5F6))
                    Text(
                        "FinFit cam kết bảo vệ thông tin cá nhân và dữ liệu tài chính của người dùng theo Nghị định bảo vệ dữ liệu cá nhân (Việt Nam). Mọi dữ liệu thu thập chỉ phục vụ mục đích quản lý tài chính và sức khỏe cá nhân của chính bạn.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        lineHeight = 17.sp
                    )

                    Text("2. Quyền Truy Cập & Thông Báo", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64B5F6))
                    Text(
                        "Dịch vụ đọc thông báo ngân hàng (BankNotificationListener) chỉ xử lý các gói tin biến động số dư từ danh sách ngân hàng được hỗ trợ. Chúng tôi tuyệt đối không đọc, ghi nhận hoặc chia sẻ tin nhắn cá nhân, mã OTP hay thông báo của bất kỳ ứng dụng nào khác.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        lineHeight = 17.sp
                    )

                    Text("3. Xử Lý Trí Tuệ Nhân Tạo (AI)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64B5F6))
                    Text(
                        "Các mô hình AI nhận diện (OCR hóa đơn, Nhận diện món ăn) được xử lý trực tiếp trên thiết bị (On-Device). Khi bạn trò chuyện với Trợ lý Fitie, toàn bộ số tài khoản và danh tính cá nhân đều được ẩn danh hóa trước khi truyền qua kênh mã hóa an toàn.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        lineHeight = 17.sp
                    )

                    Text("4. Quyền Kiểm Soát & Xóa Dữ Liệu", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64B5F6))
                    Text(
                        "Bạn có toàn quyền xuất bản sao dữ liệu cá nhân (Export JSON/CSV) hoặc yêu cầu xóa vĩnh viễn toàn bộ tài khoản và lịch sử giao dịch bất kỳ lúc nào trong mục Cài Đặt Bảo Mật.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}
