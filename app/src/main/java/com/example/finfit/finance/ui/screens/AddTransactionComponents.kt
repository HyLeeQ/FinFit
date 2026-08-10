package com.example.finfit.finance.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.finance.model.*
import com.example.finfit.finance.ui.utils.formatCurrency

@Composable
fun SingleAccountSelector(account: AppBankAccount?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Từ tài khoản", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(account?.name ?: "Chưa chọn tài khoản", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.LightGray)
        }
    }
}

@Composable
fun TransferAccountSection(from: AppBankAccount?, to: AppBankAccount?, onFrom: () -> Unit, onTo: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SingleAccountSelector(from, onFrom)
        Box(Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 40.dp).background(Color.LightGray.copy(alpha = 0.2f)))
        SingleAccountSelector(to, onTo)
    }
}

@Composable
fun CategoryGrid(categories: List<TxCategory>, selected: String, onSelected: (String) -> Unit) {
    Column {
        categories.chunked(3).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { cat ->
                    val isSelected = selected == cat.label
                    Card(
                        modifier = Modifier.weight(1f).clickable { onSelected(cat.label) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) cat.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        border = if (isSelected) BorderStroke(1.5.dp, cat.color) else null
                    ) {
                        Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(cat.icon, null, tint = if (isSelected) cat.color else Color.Gray.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(cat.label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun AccountPickerDialog(accounts: List<AppBankAccount>, onSelected: (AppBankAccount) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn tài khoản", fontWeight = FontWeight.Black) },
        text = {
            LazyColumn {
                items(accounts) { acc ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelected(acc) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val bank = SUPPORTED_BANKS.find { it.code == acc.bankCode } ?: SUPPORTED_BANKS.last()
                        Text(bank.emoji, fontSize = 24.sp)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(acc.name, fontWeight = FontWeight.Bold)
                            Text(formatCurrency(acc.amount), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Đóng") } },
        shape = RoundedCornerShape(28.dp)
    )
}
