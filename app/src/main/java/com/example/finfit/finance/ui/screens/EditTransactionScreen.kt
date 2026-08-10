package com.example.finfit.finance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.finance.model.FinanceTransaction
import com.example.finfit.finance.ui.utils.formatAmountInput

@Composable
fun EditTransactionScreen(
    transaction: FinanceTransaction,
    onSave: (FinanceTransaction) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit
) {
    // Lưu raw digits, hiển thị formatted
    var amountRaw by remember { mutableStateOf(transaction.amount.toLong().toString()) }
    var noteText by remember { mutableStateOf(transaction.note) }
    var selectedCategory by remember { mutableStateOf(transaction.category) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa giao dịch?") },
            text = { Text("Bạn có chắc muốn xóa vĩnh viễn giao dịch này?") },
            confirmButton = { TextButton(onClick = { onDelete(transaction.id) }) { Text("Xóa", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Hủy") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Chi tiết giao dịch", modifier = Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.width(48.dp))
        }
        Spacer(Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Số tiền", fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("đ", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    val displayAmt = formatAmountInput(amountRaw)
                    BasicTextField(
                        value = displayAmt,
                        onValueChange = { amountRaw = it.filter { c -> c.isDigit() } },
                        textStyle = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Danh mục", fontWeight = FontWeight.Bold)
        val allCats = (EXPENSE_CATEGORIES + INCOME_CATEGORIES + TRANSFER_CATEGORIES).distinctBy { it.label }
        allCats.chunked(5).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { cat ->
                    val isSelected = selectedCategory == cat.label
                    Column(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) cat.color.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { selectedCategory = cat.label }.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(cat.color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            Icon(cat.icon, null, tint = cat.color, modifier = Modifier.size(20.dp))
                        }
                        Text(cat.label, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(value = noteText, onValueChange = { noteText = it }, label = { Text("Ghi chú") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(32.dp))
        Button(onClick = { onSave(transaction.copy(amount = amountRaw.toDoubleOrNull() ?: 0.0, note = noteText, category = selectedCategory)) },
            modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { Text("Lưu thay đổi") }
        TextButton(onClick = { showDeleteDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Xóa giao dịch", color = Color.Red) }
    }
}
