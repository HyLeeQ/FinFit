package com.example.finfit.health.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScheduleScreen(
    onBack: () -> Unit
) {
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {},
                containerColor = Color(0xFF64b5f6),
                shape = CircleShape
            ) {
                Icon(Icons.Rounded.Add, "Add", tint = Color(0xFF0e0e0e))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 1: Ideal Sleep
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
                        Text("Giờ ngủ lý tưởng", fontSize = 12.sp, color = Color(0xFFadaaaa))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "8h 30m",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = (-1).sp
                        )
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

            // Card 2: Tonight's Prediction
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a1a))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Bạn sẽ ngủ 8h 10m đêm nay", fontSize = 14.sp, color = Color(0xFFadaaaa))
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("96% mục tiêu hàng ngày", fontSize = 12.sp, color = Color(0xFF64b5f6), fontWeight = FontWeight.Bold)
                        Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFF64b5f6), modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { 0.96f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF64b5f6),
                        trackColor = Color(0xFF262626),
                    )
                }
            }

            // Horizontal Weekly Calendar
            val days = listOf("T2" to "10", "T3" to "11", "T4" to "12", "T5" to "13", "T6" to "14")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(days.size) { index ->
                    val (dayOfWeek, dayOfMonth) = days[index]
                    val isSelected = index == 2 // T4 12
                    
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
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(dayOfWeek, fontSize = 12.sp, color = if (isSelected) Color.White else Color(0xFFadaaaa))
                            Spacer(Modifier.height(4.dp))
                            Text(dayOfMonth, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Card 3: Bedtime
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a1a))
            ) {
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
                        Text("09:00 PM", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Text("trong 6h\n22m", fontSize = 10.sp, color = Color(0xFFadaaaa), modifier = Modifier.padding(end = 8.dp))
                    Switch(
                        checked = true,
                        onCheckedChange = {},
                        colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF64b5f6))
                    )
                }
            }

            // Card 4: Wake-up time
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a1a))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).background(Color(0xFF262626), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Alarm, contentDescription = null, tint = Color(0xFF64b5f6))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Giờ thức dậy", fontSize = 12.sp, color = Color(0xFFadaaaa))
                        Text("05:10 AM", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Switch(
                        checked = true,
                        onCheckedChange = {},
                        colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF64b5f6))
                    )
                }
            }
        }
    }
}
