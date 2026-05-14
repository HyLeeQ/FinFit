package com.example.finfit.health.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finfit.health.model.SleepUiState
import com.example.finfit.health.repository.HealthViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScheduleScreen(
    onBack: () -> Unit,
    healthViewModel: HealthViewModel = viewModel()
) {
    val sleepUiState by healthViewModel.sleepUiState.collectAsStateWithLifecycle()
    var showLogDialog by remember { mutableStateOf(false) }
    
    var fabOffsetX by remember { mutableStateOf(0f) }
    var fabOffsetY by remember { mutableStateOf(0f) }
    var editingSession by remember { mutableStateOf<com.example.finfit.health.model.SleepLogUiItem?>(null) }
    var sessionToDelete by remember { mutableStateOf<com.example.finfit.health.model.SleepLogUiItem?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color(0xFF0e0e0e),
            topBar = {
                TopAppBar(
                    title = { Text("Sleep Schedule", fontWeight = FontWeight.Bold, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0e0e0e))
                )
            }
        ) { paddingValues ->
            val data = (sleepUiState as? SleepUiState.Ready)?.data
            val totalHours = data?.totalSleepHours ?: 0f
            val goalHours = data?.sleepGoalHours ?: 8f
            val progress = data?.progress ?: 0f
            val todaySessions = data?.todaySessions ?: emptyList()
            val firstSession = todaySessions.firstOrNull()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: Total Sleep
                Card(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF00314f), Color(0xFF1a1a1a))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Thời gian ngủ", fontSize = 12.sp, color = Color(0xFFadaaaa))
                            Spacer(Modifier.height(4.dp))
                            if (firstSession == null) {
                                Text(
                                    "--h --m",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = (-1).sp
                                )
                            } else {
                                val h = totalHours.toInt()
                                val m = ((totalHours - h) * 60).toInt()
                                Text(
                                    "${h}h ${m}m",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = (-1).sp
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {},
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64b5f6)),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Tìm hiểu thêm", color = Color(0xFF0e0e0e), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Icon(
                            Icons.Rounded.NightlightRound,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp).align(Alignment.CenterEnd).offset(x = 20.dp),
                            tint = Color(0xFF64b5f6).copy(alpha = 0.6f)
                        )
                    }
                }

                // Card 2: Goal Progress
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a1a))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Mục tiêu: ${goalHours.toInt()}h", fontSize = 14.sp, color = Color(0xFFadaaaa))
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${(progress * 100).toInt()}% mục tiêu hàng ngày", fontSize = 12.sp, color = Color(0xFF64b5f6), fontWeight = FontWeight.Bold)
                            Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFF64b5f6), modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF64b5f6),
                            trackColor = Color(0xFF262626),
                        )
                    }
                }

                // Horizontal Weekly Calendar
                val formatFull = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }
                val formatDayOfWeek = remember { java.text.SimpleDateFormat("E", java.util.Locale("vi", "VN")) }
                val formatDayOfMonth = remember { java.text.SimpleDateFormat("dd", java.util.Locale.getDefault()) }
                val last7Days = remember {
                    (6 downTo 0).map { i ->
                        val cal = java.util.Calendar.getInstance()
                        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
                        Triple(formatFull.format(cal.time), formatDayOfWeek.format(cal.time), formatDayOfMonth.format(cal.time))
                    }
                }
                val selectedDate = data?.selectedDate ?: formatFull.format(java.util.Calendar.getInstance().time)

                val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                LaunchedEffect(Unit) {
                    listState.scrollToItem(last7Days.size - 1)
                }

                LazyRow(
                    state = listState,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(last7Days.size) { index ->
                        val (dateStr, dayOfWeek, dayOfMonth) = last7Days[index]
                        val isSelected = dateStr == selectedDate
                        
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(80.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Color(0xFF00314f) else Color(0xFF1a1a1a))
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) Color(0xFF64b5f6) else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { healthViewModel.changeSelectedSleepDate(dateStr) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(dayOfWeek.replace("Th ", "T").uppercase(), fontSize = 12.sp, color = if (isSelected) Color.White else Color(0xFFadaaaa))
                                Spacer(Modifier.height(4.dp))
                                Text(dayOfMonth, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                // Dynamic Sleep Sessions
                if (todaySessions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Chưa có giấc ngủ nào được ghi nhận", color = Color(0xFFadaaaa), fontSize = 14.sp)
                    }
                } else {
                    todaySessions.forEachIndexed { index, session ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a1a))
                        ) {
                            Column {
                                // Header: Phiên ngủ X & Nút Xóa
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Phiên ngủ ${index + 1}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Row {
                                        IconButton(
                                            onClick = { 
                                                editingSession = session
                                                showLogDialog = true
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Rounded.Edit, contentDescription = "Sửa", tint = Color(0xFF64b5f6))
                                        }
                                        Spacer(Modifier.width(16.dp))
                                        IconButton(
                                            onClick = { sessionToDelete = session },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Xóa", tint = Color(0xFFff5252))
                                        }
                                    }
                                }
                                
                                Divider(color = Color(0xFF262626))

                                // Bedtime Row
                                Row(
                                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(48.dp).background(Color(0xFF262626), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.Bed, contentDescription = null, tint = Color(0xFF64b5f6))
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Giờ đi ngủ", fontSize = 12.sp, color = Color(0xFFadaaaa))
                                        val bedCal = java.util.Calendar.getInstance().apply { timeInMillis = session.bedTimeTimestamp }
                                        Text(String.format("%02d:%02d", bedCal.get(java.util.Calendar.HOUR_OF_DAY), bedCal.get(java.util.Calendar.MINUTE)), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                // Wake-up Row
                                Row(
                                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(48.dp).background(Color(0xFF262626), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.Alarm, contentDescription = null, tint = Color(0xFFffcc80))
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Giờ thức dậy", fontSize = 12.sp, color = Color(0xFFadaaaa))
                                        if (session.wakeTimeTimestamp == 0L) {
                                            Text("--:--", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        } else {
                                            val wakeCal = java.util.Calendar.getInstance().apply { timeInMillis = session.wakeTimeTimestamp }
                                            Text(String.format("%02d:%02d", wakeCal.get(java.util.Calendar.HOUR_OF_DAY), wakeCal.get(java.util.Calendar.MINUTE)), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(100.dp)) // Extra padding so FAB doesn't permanently block the bottom
            }
        }

        // Draggable FAB overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 32.dp, end = 16.dp)
                .offset { IntOffset(fabOffsetX.roundToInt(), fabOffsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        fabOffsetX += dragAmount.x
                        fabOffsetY += dragAmount.y
                    }
                }
        ) {
            FloatingActionButton(
                onClick = { showLogDialog = true },
                containerColor = Color(0xFF64b5f6),
                shape = CircleShape
            ) {
                Icon(Icons.Rounded.Add, "Add", tint = Color(0xFF0e0e0e))
            }
        }

        if (showLogDialog) {
            LogSleepDialog(
                initialSession = editingSession,
                onDismiss = { showLogDialog = false },
                onSave = { bedTime, wakeTime ->
                    if (editingSession != null) {
                        healthViewModel.deleteSleepSession(editingSession!!.id)
                    }
                    healthViewModel.logSleepSession(bedTime, wakeTime, 3)
                    showLogDialog = false
                }
            )
        }

        if (sessionToDelete != null) {
            AlertDialog(
                onDismissRequest = { sessionToDelete = null },
                containerColor = Color(0xFF1a1a1a),
                title = { Text("Xác nhận", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Bạn có chắc chắn muốn xóa phiên ngủ này không?", color = Color(0xFFadaaaa)) },
                confirmButton = {
                    TextButton(onClick = {
                        healthViewModel.deleteSleepSession(sessionToDelete!!.id)
                        sessionToDelete = null
                    }) {
                        Text("Xóa", color = Color(0xFFff5252))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { sessionToDelete = null }) {
                        Text("Hủy", color = Color(0xFFadaaaa))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSleepDialog(
    initialSession: com.example.finfit.health.model.SleepLogUiItem?,
    onDismiss: () -> Unit,
    onSave: (Long, Long) -> Unit
) {
    val initialBedMin = remember(initialSession) {
        if (initialSession != null) {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = initialSession.bedTimeTimestamp }
            cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        } else {
            22 * 60
        }
    }
    
    val initialWakeMin = remember(initialSession) {
        if (initialSession != null && initialSession.wakeTimeTimestamp != 0L) {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = initialSession.wakeTimeTimestamp }
            cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        } else {
            6 * 60
        }
    }

    var bedTimeMin by remember { mutableStateOf(initialBedMin) }
    var wakeTimeMin by remember { mutableStateOf(initialWakeMin) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1a1a1a),
        title = {
            Text(
                text = if (initialSession == null) "Tạo phiên ngủ" else "Sửa phiên ngủ",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header displaying times
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Bed, contentDescription = null, tint = Color(0xFF64b5f6), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ĐI NGỦ", fontSize = 12.sp, color = Color(0xFFadaaaa))
                        }
                        Text(String.format("%02d:%02d", bedTimeMin / 60, bedTimeMin % 60), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Alarm, contentDescription = null, tint = Color(0xFFffcc80), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("THỨC DẬY", fontSize = 12.sp, color = Color(0xFFadaaaa))
                        }
                        Text(String.format("%02d:%02d", wakeTimeMin / 60, wakeTimeMin % 60), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                CircularSleepPicker(
                    bedTimeMin = bedTimeMin,
                    wakeTimeMin = wakeTimeMin,
                    onBedTimeChange = { bedTimeMin = it },
                    onWakeTimeChange = { wakeTimeMin = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val bedCal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, bedTimeMin / 60)
                    set(java.util.Calendar.MINUTE, bedTimeMin % 60)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val wakeCal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, wakeTimeMin / 60)
                    set(java.util.Calendar.MINUTE, wakeTimeMin % 60)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                
                if (wakeTimeMin <= bedTimeMin) {
                    wakeCal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                }

                onSave(bedCal.timeInMillis, wakeCal.timeInMillis)
            }) {
                Text("Lưu", color = Color(0xFF64b5f6))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = Color(0xFFadaaaa))
            }
        }
    )
}
