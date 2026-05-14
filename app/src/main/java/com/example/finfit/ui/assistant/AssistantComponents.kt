package com.example.finfit.ui.assistant

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.finfit.finance.model.*
import com.example.finfit.finance.ui.screens.EXPENSE_CATEGORIES
import com.example.finfit.finance.ui.screens.INCOME_CATEGORIES
import com.example.finfit.finance.util.ParsedTransaction
import com.example.finfit.finance.ui.utils.formatCurrency
import com.example.finfit.ui.theme.AccentGreen
import com.example.finfit.ui.theme.PrimaryBlue

@Composable
fun BubbleText(text: String, isUser: Boolean) {
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val containerColor = if (isUser) Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))) else null
    val bgColor = if (isUser) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant
    val shape = if (isUser) RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp) else RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Box(
            modifier = Modifier.widthIn(max = 280.dp).clip(shape)
                .then(if (containerColor != null) Modifier.background(containerColor) else Modifier.background(bgColor))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(text = text, color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, lineHeight = 22.sp)
        }
    }
}

@Composable
fun GenericConfirmCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isConfirmed: Boolean, infoLines: List<String>, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AnimatedVisibility(visible = true, enter = slideInVertically { it / 2 } + fadeIn()) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(PrimaryBlue.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = PrimaryBlue)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(title, fontWeight=FontWeight.Bold, fontSize=16.sp)
                }
                Spacer(Modifier.height(12.dp))
                infoLines.forEach { line ->
                    Text("• $line", fontSize=14.sp, modifier = Modifier.padding(bottom=4.dp))
                }
                if (!isConfirmed) {
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Huỷ") }
                        Button(onClick = onConfirm, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) { Text("Xác nhận") }
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Đã xử lý xong", fontSize = 13.sp, color = AccentGreen)
                    }
                }
            }
        }
    }
}

@Composable
fun DebtConfirmCard(debt: DebtLoan, isConfirmed: Boolean, onConfirm: (DebtLoan) -> Unit, onDismiss: () -> Unit) {
    var personName by remember(debt) { mutableStateOf(if (debt.personName == "Không tên") "" else debt.personName) }
    var amount by remember(debt) { mutableStateOf(if (debt.amount == 0.0) "" else debt.amount.toLong().toString()) }
    var note by remember(debt) { mutableStateOf(debt.note) }

    AnimatedVisibility(visible = true, enter = slideInVertically { it / 2 } + fadeIn()) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(PrimaryBlue.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AccountBalanceWallet, null, tint = PrimaryBlue)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(if(debt.type == DebtLoanType.DEBT) "Theo dõi Khoản nợ" else "Theo dõi Khoản vay", fontWeight=FontWeight.Bold, fontSize=16.sp)
                }
                Spacer(Modifier.height(16.dp))

                if (!isConfirmed) {
                    OutlinedTextField(
                        value = personName,
                        onValueChange = { personName = it },
                        label = { Text("Tên người liên hệ") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Số tiền (VND)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Ghi chú (Không bắt buộc)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Huỷ") }
                        Button(
                            onClick = {
                                val finalAmount = amount.toDoubleOrNull() ?: 0.0
                                onConfirm(debt.copy(personName = personName.ifBlank { "Không tên" }, amount = finalAmount, note = note))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            enabled = personName.isNotBlank() && amount.isNotBlank()
                        ) { Text("Xác nhận") }
                    }
                } else {
                    Text("• Tên liên hệ: $personName", fontSize=14.sp)
                    Text("• Số tiền: ${formatCurrency(amount.toDoubleOrNull() ?: debt.amount)}", fontSize=14.sp)
                    if (note.isNotBlank()) Text("• Ghi chú: $note", fontSize=14.sp)
                    
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Đã lưu sổ nợ", fontSize = 13.sp, color = AccentGreen)
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleConfirmCard(
    item: SpendingScheduleItem,
    isConfirmed: Boolean,
    onConfirm: (SpendingScheduleItem) -> Unit,
    onDismiss: () -> Unit
) {
    var editAmount by remember { mutableStateOf(if(item.amount == 0.0) "" else item.amount.toLong().toString()) }
    var editDay by remember { mutableStateOf(item.dayOfWeek.toString()) }
    var editNote by remember { mutableStateOf(item.note) }

    AnimatedVisibility(visible = true, enter = slideInVertically { it / 2 } + fadeIn()) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(PrimaryBlue.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Schedule, null, tint = PrimaryBlue)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Lập Lịch Trình Tự Động", fontWeight=FontWeight.Bold, fontSize=16.sp)
                }
                Spacer(Modifier.height(16.dp))

                if (!isConfirmed) {
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it },
                        label = { Text("Số tiền tự trừ mỗi tuần (VND)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editDay,
                        onValueChange = { editDay = it },
                        label = { Text("Ngày trừ tiền (1: T2, ..., 7: CN)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editNote,
                        onValueChange = { editNote = it },
                        label = { Text("Ghi chú") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Huỷ") }
                        Button(
                            onClick = {
                                val finalAmount = editAmount.toDoubleOrNull() ?: 0.0
                                val finalDay = editDay.toIntOrNull()?.coerceIn(1..7) ?: 1
                                onConfirm(item.copy(amount = finalAmount, dayOfWeek = finalDay, note = editNote))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            enabled = editAmount.isNotBlank() && editDay.isNotBlank()
                        ) { Text("Chốt Lịch") }
                    }
                } else {
                    val dayStr = if (item.dayOfWeek in 1..6) "Thứ ${item.dayOfWeek + 1}" else "Chủ Nhật"
                    Text("• Tự động trừ: ${formatCurrency(item.amount)}", fontSize=14.sp)
                    Text("• Định kỳ vào: Mỗi $dayStr", fontSize=14.sp)
                    
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Tuyệt vời! Đã setup xong lịch", fontSize = 13.sp, color = AccentGreen)
                    }
                }
            }
        }
    }
}

@Composable
fun HeldFundConfirmCard(
    fundName: String,
    amount: Double,
    isConfirmed: Boolean,
    onConfirm: (String, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var editName by remember { mutableStateOf(fundName) }
    var editAmount by remember { mutableStateOf(if(amount == 0.0) "" else amount.toLong().toString()) }

    AnimatedVisibility(visible = true, enter = slideInVertically { it / 2 } + fadeIn()) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFEAB308).copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.VolunteerActivism, null, tint = Color(0xFFEAB308))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Tạo Quỹ Giữ Hộ", fontWeight=FontWeight.Bold, fontSize=16.sp)
                }
                Spacer(Modifier.height(16.dp))

                if (!isConfirmed) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Tên Quỹ (ví dụ: Quỹ Lớp)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it },
                        label = { Text("Số tiền tiếp nhận (VND)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )

                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Huỷ") }
                        Button(
                            onClick = {
                                val finalAmount = editAmount.toDoubleOrNull() ?: 0.0
                                onConfirm(editName.ifBlank { "Quỹ Bất Ngờ" }, finalAmount)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308)),
                            enabled = editAmount.isNotBlank()
                        ) { Text("Chốt Quỹ", color = Color.White) }
                    }
                } else {
                    Text("• Tên Quỹ: $fundName", fontSize=14.sp)
                    Text("• Số tiền: ${formatCurrency(amount)}", fontSize=14.sp)
                    
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Đã mở quỹ giữ hộ mới!", fontSize = 13.sp, color = AccentGreen)
                    }
                }
            }
        }
    }
}
@Composable
fun SplitBillConfirmCard(
    totalAmount: Double,
    participantCount: Int,
    category: String,
    note: String,
    isConfirmed: Boolean,
    initialParticipants: List<TransactionParticipant> = emptyList(),
    onConfirm: (total: Double, participants: List<TransactionParticipant>, cat: String, n: String) -> Unit,
    onDismiss: () -> Unit
) {
    var editTotal by remember { mutableStateOf(if (totalAmount == 0.0) "" else totalAmount.toLong().toString()) }
    var editNote by remember { mutableStateOf(note) }
    
    // Quản lý danh sách người tham gia (trừ bản thân)
    var participants by remember { 
        mutableStateOf(
            if (initialParticipants.isNotEmpty()) initialParticipants 
            else List(participantCount.coerceAtLeast(2) - 1) { 
                TransactionParticipant(name = "Người ${it + 1}") 
            }
        )
    }

    val currentTotal = editTotal.toDoubleOrNull() ?: 0.0
    val totalPeople = participants.size + 1
    val shareAmount = if (totalPeople > 0) currentTotal / totalPeople else 0.0

    // Cập nhật shareAmount cho mọi người khi tổng bill hoặc số người thay đổi
    LaunchedEffect(currentTotal, participants.size) {
        participants = participants.map { it.copy(shareAmount = shareAmount) }
    }

    AnimatedVisibility(visible = true, enter = slideInVertically { it / 2 } + fadeIn()) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(PrimaryBlue.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.People, null, tint = PrimaryBlue)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Hoá đơn Trả Nhóm", fontWeight=FontWeight.Bold, fontSize=16.sp)
                }
                Spacer(Modifier.height(16.dp))

                if (!isConfirmed) {
                    OutlinedTextField(
                        value = editTotal,
                        onValueChange = { editTotal = it },
                        label = { Text("Tổng bill (VND)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editNote,
                        onValueChange = { editNote = it },
                        label = { Text("Ghi chú bill") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    Text("Danh sách người tham gia", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    
                    participants.forEachIndexed { index, participant ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = participant.name,
                                onValueChange = { newName ->
                                    participants = participants.toMutableList().apply {
                                        this[index] = this[index].copy(name = newName)
                                    }
                                },
                                placeholder = { Text("Tên") },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )
                            
                            OutlinedTextField(
                                value = if (participant.paidAmount == 0.0) "" else participant.paidAmount.toLong().toString(),
                                onValueChange = { newVal ->
                                    val amt = newVal.toDoubleOrNull() ?: 0.0
                                    participants = participants.toMutableList().apply {
                                        this[index] = this[index].copy(paidAmount = amt, isPaid = amt >= this[index].shareAmount)
                                    }
                                },
                                placeholder = { Text("Đã trả") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )

                            IconButton(
                                onClick = {
                                    participants = participants.toMutableList().apply {
                                        val current = this[index]
                                        this[index] = current.copy(
                                            isPaid = !current.isPaid,
                                            paidAmount = if (!current.isPaid) current.shareAmount else 0.0
                                        )
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    if (participant.isPaid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    null,
                                    tint = if (participant.isPaid) AccentGreen else Color.Gray
                                )
                            }
                            
                            IconButton(
                                onClick = {
                                    participants = participants.toMutableList().apply { removeAt(index) }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.RemoveCircleOutline, null, tint = Color.Red.copy(alpha=0.6f))
                            }
                        }
                    }
                    
                    TextButton(
                        onClick = {
                            participants = participants + TransactionParticipant(name = "Người ${participants.size + 2}", shareAmount = shareAmount)
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Thêm người")
                    }

                    Spacer(Modifier.height(12.dp))
                    Surface(color = PrimaryBlue.copy(alpha=0.05f), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                            val groupOwes = participants.filter { !it.isPaid }.sumOf { it.shareAmount - it.paidAmount }
                            Text("Tổng chia: ${formatCurrency(currentTotal)}", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("Phần của bạn: ${formatCurrency(shareAmount)}", fontSize=14.sp)
                            Text("Nhóm còn nợ bạn: ${formatCurrency(groupOwes)}", fontSize=14.sp, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Huỷ") }
                        Button(
                            onClick = {
                                onConfirm(currentTotal, participants, category, editNote)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            enabled = currentTotal > 0 && totalPeople > 0
                        ) { Text("Chia Bill") }
                    }
                } else {
                    val groupOwes = participants.filter { !it.isPaid }.sumOf { it.shareAmount - it.paidAmount }
                    Text("• Bạn chịu: ${formatCurrency(shareAmount)}", fontSize=14.sp)
                    Text("• Nhóm nợ: ${formatCurrency(groupOwes)}", fontSize=14.sp)
                    participants.forEach { p ->
                        val status = if (p.isPaid) "Đã trả đủ" else "Còn nợ ${formatCurrency(p.shareAmount - p.paidAmount)}"
                        Text("  - ${p.name}: $status", fontSize = 13.sp, color = Color.Gray)
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Đã xử lý & chia quỹ", fontSize = 13.sp, color = AccentGreen)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionConfirmCard(parsed: ParsedTransaction, isConfirmed: Boolean, wallet: AppUserWallet?, onConfirm: (FinanceTransaction, AppUserWallet) -> Unit, onDismiss: () -> Unit) {
    val isIncome = parsed.type == "INCOME"
    val accentColor = if (isIncome) Color(0xFF10B981) else Color(0xFF6366F1)
    val cat = (EXPENSE_CATEGORIES + INCOME_CATEGORIES).find { it.label == parsed.category }
    var selectedCategory by remember { mutableStateOf(parsed.category) }
    var editNote by remember { mutableStateOf(parsed.note) }
    
    val accounts = wallet?.accounts ?: emptyList()
    var selectedAccount by remember { mutableStateOf(accounts.find { it.id == parsed.accountId } ?: accounts.firstOrNull()) }
    var accountExpanded by remember { mutableStateOf(false) }

    AnimatedVisibility(visible = true, enter = slideInVertically { it / 2 } + fadeIn()) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(32.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                            Icon(cat?.icon ?: Icons.Default.Receipt, null, tint = accentColor, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(if (isIncome) "Thu nhập khả dụng" else "Bill thanh toán", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                            Text(selectedCategory, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Text(formatCurrency(parsed.amount), fontWeight = FontWeight.Black, fontSize = 20.sp, color = accentColor)
                }

                if (!isConfirmed) {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(16.dp))
                    
                    // Nguồn tiền dropdown
                    Text("Nguồn tiền", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Box {
                        OutlinedButton(onClick = { accountExpanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Text(selectedAccount?.name ?: "Chưa chọn ví", color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                        DropdownMenu(expanded = accountExpanded, onDismissRequest = { accountExpanded = false }) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (${formatCurrency(acc.amount)})") },
                                    onClick = { selectedAccount = acc; accountExpanded = false }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    
                    OutlinedTextField(value = editNote, onValueChange = { editNote = it }, label = { Text("Mô tả ghi chú") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Huỷ") }
                        Button(
                            onClick = {
                                val tx = FinanceTransaction(id = java.util.UUID.randomUUID().toString(), amount = parsed.amount, type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE, category = selectedCategory, note = editNote, accountId = selectedAccount?.id, paymentMethod = PaymentMethod.CASH)
                                val updatedWallet = if (wallet != null && selectedAccount != null) {
                                    val delta = if (isIncome) parsed.amount else -parsed.amount
                                    wallet.copy(accounts = wallet.accounts.map { if (it.id == selectedAccount!!.id) it.copy(amount = it.amount + delta) else it })
                                } else wallet ?: AppUserWallet()
                                onConfirm(tx, updatedWallet)
                            },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) { Text("Xác nhận", color = Color.White) }
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Đã lưu lịch sử", fontSize = 13.sp, color = AccentGreen)
                    }
                }
            }
        }
    }
}

@Composable
fun BillConfirmCard(imageUri: Uri, extractedAmount: Double?, isConfirmed: Boolean, wallet: AppUserWallet?, onConfirm: (FinanceTransaction, AppUserWallet) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var note by remember { mutableStateOf("Hoá đơn chụp") }
    var category by remember { mutableStateOf("Ăn uống") }
    
    val accounts = wallet?.accounts ?: emptyList()
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()) }
    var accountExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(4.dp)) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                AsyncImage(model = ImageRequest.Builder(context).data(imageUri).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f)))))
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text("HÓA ĐƠN", color = Color.White.copy(0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    if (extractedAmount != null) {
                        Text(formatCurrency(extractedAmount), color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp)
                    } else {
                        Text("Không tìm được số tiền", color = Color.White.copy(0.7f), fontSize = 14.sp)
                    }
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                if (extractedAmount != null) {
                    // Nguồn tiền dropdown
                    Text("Nguồn thanh toán", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Box {
                        OutlinedButton(onClick = { accountExpanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Text(selectedAccount?.name ?: "Chưa chọn ví", color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                        DropdownMenu(expanded = accountExpanded, onDismissRequest = { accountExpanded = false }) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (${formatCurrency(acc.amount)})") },
                                    onClick = { selectedAccount = acc; accountExpanded = false }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Nội dung mua hàng") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Huỷ") }
                        Button(
                            onClick = {
                                val tx = FinanceTransaction(id = java.util.UUID.randomUUID().toString(), amount = extractedAmount, type = TransactionType.EXPENSE, category = category, note = note, accountId = selectedAccount?.id, paymentMethod = PaymentMethod.CASH)
                                val updatedWallet = if (wallet != null && selectedAccount != null) {
                                    wallet.copy(accounts = wallet.accounts.map { if (it.id == selectedAccount!!.id) it.copy(amount = it.amount - extractedAmount) else it })
                                } else wallet ?: AppUserWallet()
                                onConfirm(tx, updatedWallet)
                            },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) { Text("Chốt số Invoice", color = Color.White) }
                    }
                } else {
                    Text("Hệ thống nhận diện chưa chính xác lượng tiền.\nBạn vui lòng nhập bằng chữ thay thế nhé.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Xác nhận") }
                }
            }
        }
    }
}

@Composable
fun SmartChatInput(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit, onCamera: () -> Unit, isLoading: Boolean) {
    Surface(tonalElevation = 8.dp, modifier = Modifier.navigationBarsPadding()) {
        Column {
            val suggestions = listOf("Ghi chi tiêu", "Ghi thu nhập", "Ghi khoản vay", "Ghi nợ", "Lên ngân sách", "Mục tiêu tiết kiệm")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 12.dp, top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestions.forEach { suggestion ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.clickable { onTextChange(suggestion) }
                    ) {
                        Text(
                            text = suggestion,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onCamera, enabled = !isLoading) { Icon(Icons.Default.CameraAlt, "Chụp bill", tint = PrimaryBlue) }
                TextField(value = text, onValueChange = onTextChange, modifier = Modifier.weight(1f), placeholder = { Text("Ăn tối 50k, vay Nam 2tr...", fontSize = 14.sp) }, colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), maxLines = 3)
                IconButton(onClick = onSend, enabled = text.isNotBlank() && !isLoading) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) else Icon(Icons.AutoMirrored.Filled.Send, null, tint = PrimaryBlue)
                }
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
        repeat(3) { i ->
            val alpha by infiniteTransition.animateFloat(initialValue = 0.3f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(500, delayMillis = i * 150), repeatMode = RepeatMode.Reverse), label = "dot$i")
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color.Gray.copy(alpha = alpha)))
            Spacer(Modifier.width(4.dp))
        }
    }
}
@Composable
fun HabitUpdateCard(
    habit: UserHabit,
    isConfirmed: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(visible = true, enter = slideInVertically { it / 2 } + fadeIn()) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(PrimaryBlue.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Psychology, null, tint = PrimaryBlue)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Cập nhật thói quen", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(12.dp))
                
                if (habit.minMealCost > 0 || habit.maxMealCost > 0) {
                    Text("• Giá bữa ăn: ${formatCurrency(habit.minMealCost)} - ${formatCurrency(habit.maxMealCost)}", fontSize = 14.sp)
                }
                if (habit.generalNotes.isNotBlank()) {
                    Text("• Thông tin thêm:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(habit.generalNotes, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                }

                if (!isConfirmed) {
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Huỷ") }
                        Button(onClick = onConfirm, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) { Text("Ghi nhớ") }
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Đã ghi nhớ vào bộ não AI", fontSize = 13.sp, color = AccentGreen)
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyPlanCard(
    description: String,
    items: List<SpendingScheduleItem>,
    isConfirmed: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(visible = true, enter = slideInVertically { it / 2 } + fadeIn()) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(AccentGreen.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.EventAvailable, null, tint = AccentGreen)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Kế hoạch tuần đề xuất", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(12.dp))
                
                Text(description, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlue)
                Spacer(Modifier.height(8.dp))
                
                // Hiển thị danh sách tóm tắt
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val daysGrouped = items.groupBy { it.dayOfWeek }
                    daysGrouped.keys.sorted().forEach { day ->
                        val dayName = when(day) {
                            1 -> "Thứ 2"
                            2 -> "Thứ 3"
                            3 -> "Thứ 4"
                            4 -> "Thứ 5"
                            5 -> "Thứ 6"
                            6 -> "Thứ 7"
                            else -> "Chủ Nhật"
                        }
                        val dayTotal = daysGrouped[day]?.sumOf { it.amount } ?: 0.0
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(dayName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(formatCurrency(dayTotal), fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                }

                if (!isConfirmed) {
                    Spacer(Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Bỏ qua") }
                        Button(onClick = onConfirm, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)) { Text("Áp dụng lịch") }
                    }
                } else {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TaskAlt, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Đã áp dụng toàn bộ vào lịch trình", fontSize = 13.sp, color = AccentGreen)
                    }
                }
            }
        }
    }
}

@Composable
fun DepositSavingsConfirmCard(
    goalName: String,
    amount: Double,
    walletSource: String?,
    isConfirmed: Boolean,
    wallet: AppUserWallet?,
    onConfirm: (goalName: String, amount: Double, sourceAccountId: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var editAmount by remember { mutableStateOf(if (amount == 0.0) "" else amount.toLong().toString()) }
    val accounts = wallet?.accounts ?: emptyList()
    var selectedAccount by remember {
        mutableStateOf(accounts.find { it.name.contains(walletSource ?: "", ignoreCase = true) } ?: accounts.firstOrNull())
    }
    var accountExpanded by remember { mutableStateOf(false) }

    AnimatedVisibility(visible = true, enter = slideInVertically { it / 2 } + fadeIn()) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF3B82F6).copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Savings, null, tint = Color(0xFF3B82F6))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Nạp vào Tiết kiệm", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(16.dp))

                if (!isConfirmed) {
                    Text("Mục tiêu: $goalName", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it },
                        label = { Text("Số tiền nạp (VND)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    Spacer(Modifier.height(10.dp))

                    Text("Nguồn tiền múc", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Box {
                        OutlinedButton(onClick = { accountExpanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Text(selectedAccount?.name ?: "Chưa chọn ví", color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                        DropdownMenu(expanded = accountExpanded, onDismissRequest = { accountExpanded = false }) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (${formatCurrency(acc.amount)})") },
                                    onClick = { selectedAccount = acc; accountExpanded = false }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Huỷ") }
                        Button(
                            onClick = { onConfirm(goalName, editAmount.toDoubleOrNull() ?: 0.0, selectedAccount?.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            enabled = editAmount.isNotBlank()
                        ) { Text("Chuyển tiền") }
                    }
                } else {
                    Text("• Đã nạp: ${formatCurrency(amount)}", fontSize=14.sp)
                    Text("• Vào quỹ: $goalName", fontSize=14.sp)
                    Text("• Từ nguồn: ${selectedAccount?.name ?: "Không xác định"}", fontSize=14.sp)

                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Tiền đã vào két sắt", fontSize = 13.sp, color = AccentGreen)
                    }
                }
            }
        }
    }
}

@Composable
fun WithdrawSavingsConfirmCard(
    goalName: String,
    amount: Double,
    destinationWallet: String?,
    transferToSavingsGoal: String?,
    isConfirmed: Boolean,
    wallet: AppUserWallet?,
    savingsGoals: List<SavingsGoal>,
    onConfirm: (goalName: String, amount: Double, destAccountId: String?, destGoalId: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var editAmount by remember { mutableStateOf(if (amount == 0.0) "" else amount.toLong().toString()) }
    var isTransferToGoal by remember { mutableStateOf(transferToSavingsGoal != null) }

    val accounts = wallet?.accounts ?: emptyList()
    var selectedAccount by remember {
        mutableStateOf(accounts.find { it.name.contains(destinationWallet ?: "", ignoreCase = true) } ?: accounts.firstOrNull())
    }
    var accountExpanded by remember { mutableStateOf(false) }

    var selectedGoal by remember {
        mutableStateOf(savingsGoals.find { it.goalName.contains(transferToSavingsGoal ?: "", ignoreCase = true) } ?: savingsGoals.firstOrNull { it.goalName != goalName })
    }
    var goalExpanded by remember { mutableStateOf(false) }

    AnimatedVisibility(visible = true, enter = slideInVertically { it / 2 } + fadeIn()) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFEF4444).copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AccountBalanceWallet, null, tint = Color(0xFFEF4444))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Rút tiền / Chuyển quỹ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(16.dp))

                if (!isConfirmed) {
                    Text("Rút từ quỹ: $goalName", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it },
                        label = { Text("Số tiền rút (VND)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !isTransferToGoal,
                            onClick = { isTransferToGoal = false },
                            label = { Text("Rút về ví") }
                        )
                        FilterChip(
                            selected = isTransferToGoal,
                            onClick = { isTransferToGoal = true },
                            label = { Text("Chuyển quỹ khác") }
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    if (!isTransferToGoal) {
                        Text("Ví/Tài khoản nhận tiền", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Box {
                            OutlinedButton(onClick = { accountExpanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                Text(selectedAccount?.name ?: "Chưa chọn ví đích", color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurface)
                            }
                            DropdownMenu(expanded = accountExpanded, onDismissRequest = { accountExpanded = false }) {
                                accounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = { Text("${acc.name} (${formatCurrency(acc.amount)})") },
                                        onClick = { selectedAccount = acc; accountExpanded = false }
                                    )
                                }
                            }
                        }
                    } else {
                        Text("Quỹ tiết kiệm nhận", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Box {
                            OutlinedButton(onClick = { goalExpanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                Text(selectedGoal?.goalName ?: "Chưa chọn quỹ đích", color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurface)
                            }
                            DropdownMenu(expanded = goalExpanded, onDismissRequest = { goalExpanded = false }) {
                                savingsGoals.filter { it.goalName != goalName }.forEach { g ->
                                    DropdownMenuItem(
                                        text = { Text("${g.goalName} (${formatCurrency(g.currentAmount)})") },
                                        onClick = { selectedGoal = g; goalExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Huỷ") }
                        Button(
                            onClick = {
                                val amt = editAmount.toDoubleOrNull() ?: 0.0
                                if (isTransferToGoal) {
                                    onConfirm(goalName, amt, null, selectedGoal?.id)
                                } else {
                                    onConfirm(goalName, amt, selectedAccount?.id, null)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            enabled = editAmount.isNotBlank() && (!isTransferToGoal || selectedGoal != null)
                        ) { Text("Xác nhận") }
                    }
                } else {
                    Text("• Đã xử lý: ${formatCurrency(amount)}", fontSize=14.sp)
                    Text("• Rút từ: $goalName", fontSize=14.sp)
                    if (transferToSavingsGoal != null) {
                        Text("• Chuyển sang quỹ: $transferToSavingsGoal", fontSize=14.sp)
                    } else {
                        Text("• Rút về ví: ${selectedAccount?.name ?: "Không xác định"}", fontSize=14.sp)
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Khớp lệnh thành công", fontSize = 13.sp, color = AccentGreen)
                    }
                }
            }
        }
    }
}
