package com.example.finfit.finance.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.finance.model.*
import com.example.finfit.finance.ui.utils.formatCurrency
import com.example.finfit.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyScheduleScreen(
    items: List<SpendingScheduleItem>,
    onSave: (SpendingScheduleItem) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit
) {
    var selectedDay by remember { mutableStateOf(1) } // 1: T2, ..., 7: CN
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    
    val days = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
    val fullDays = listOf("Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật")
    
    val dayItems = items.filter { it.dayOfWeek == selectedDay }
    val totalAmount = dayItems.sumOf { it.amount }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Lịch trình chi tiêu", fontWeight = FontWeight.Black, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PrimaryBlue)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBottomSheet = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp).size(60.dp)
            ) {
                Icon(Icons.Default.Add, "Thêm lịch trình", modifier = Modifier.size(32.dp))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Day Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEachIndexed { index, name ->
                    val isSelected = selectedDay == index + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) PrimaryBlue else Color.Transparent)
                            .clickable { selectedDay = index + 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            name, 
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Day Summary Card
            SummaryHeaderCard(dayName = fullDays[selectedDay - 1], total = totalAmount)

            if (dayItems.isEmpty()) {
                EmptyScheduleView(onAdd = { showBottomSheet = true })
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(dayItems, key = { it.id }) { item ->
                        ScheduleCardWithAnim(item, onDelete = { onDelete(item.id) })
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            AddScheduleSheetContent(
                dayName = fullDays[selectedDay - 1],
                dayOfWeek = selectedDay,
                onDismiss = { scope.launch { sheetState.hide() }.invokeOnCompletion { showBottomSheet = false } },
                onConfirm = { newItem ->
                    onSave(newItem)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showBottomSheet = false }
                }
            )
        }
    }
}

@Composable
fun SummaryHeaderCard(dayName: String, total: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(dayName, fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text("Dự kiến chi", fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Text(
                formatCurrency(total),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = if (total > 0) Color(0xFFEF4444) else AccentGreen
            )
        }
    }
}

@Composable
fun ScheduleCardWithAnim(item: SpendingScheduleItem, onDelete: () -> Unit) {
    val categoryInfo = EXPENSE_CATEGORIES.find { it.label == item.category } ?: EXPENSE_CATEGORIES.last()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(categoryInfo.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(categoryInfo.icon, null, tint = categoryInfo.color, modifier = Modifier.size(24.dp))
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(item.category, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                if (item.note.isNotBlank()) {
                    Text(item.note, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatCurrency(item.amount), 
                    fontWeight = FontWeight.Black, 
                    fontSize = 16.sp,
                    color = categoryInfo.color
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp).padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyScheduleView(onAdd: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.EventNote, 
                    null, 
                    modifier = Modifier.size(60.dp), 
                    tint = PrimaryBlue.copy(alpha = 0.3f)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Chưa có kế hoạch", 
                fontSize = 20.sp, 
                fontWeight = FontWeight.Black, 
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Lập lịch chi tiêu giúp bạn kiểm soát dòng tiền tốt hơn trong tuần.",
                textAlign = TextAlign.Center,
                color = Color.Gray,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(48.dp).padding(horizontal = 24.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Lập lịch ngay")
            }
        }
    }
}

@Composable
fun AddScheduleSheetContent(
    dayName: String,
    dayOfWeek: Int,
    onDismiss: () -> Unit,
    onConfirm: (SpendingScheduleItem) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(EXPENSE_CATEGORIES[0].label) }
    var note by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text("Dự kiến chi $dayName", fontWeight = FontWeight.Black, fontSize = 24.sp)
        Spacer(Modifier.height(24.dp))
        
        // Amount Entry — hiển thị format VN dấu chấm
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            val displayAmount = com.example.finfit.finance.ui.utils.formatAmountInput(amount)
            TextField(
                value = displayAmount,
                onValueChange = { newVal ->
                    amount = newVal.filter { it.isDigit() }.take(12)
                },
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                placeholder = { Text("0", fontSize = 32.sp, fontWeight = FontWeight.Light, color = Color.LightGray) }
            )
        }
        
        Spacer(Modifier.height(24.dp))
        Text("Hạng mục", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))
        
        // Categories Grid
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(EXPENSE_CATEGORIES) { cat ->
                val isSelected = selectedCategory == cat.label
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat.label },
                    label = { Text(cat.label) },
                    leadingIcon = { Icon(cat.icon, null, modifier = Modifier.size(16.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = cat.color,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    )
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            placeholder = { Text("Thêm lời nhắn (Không bắt buộc)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                if (amt > 0) {
                    onConfirm(SpendingScheduleItem(dayOfWeek = dayOfWeek, amount = amt, category = selectedCategory, note = note))
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp),
            enabled = amount.isNotBlank()
        ) {
            Text("Lưu vào $dayName", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
