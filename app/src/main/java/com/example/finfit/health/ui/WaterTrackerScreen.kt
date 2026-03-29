package com.example.finfit.health.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.EmojiFoodBeverage
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finfit.health.repository.HealthViewModel
import kotlinx.coroutines.isActive
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// --- Bubble Logic ---
private class Bubble(
    var x: Float = 0f,
    var y: Float = 0f,
    var radius: Float = 0f,
    var speed: Float = 0f,
    var alpha: Float = 0f,
    var active: Boolean = false,
    var needsReset: Boolean = true
) {
    fun reset(canvasWidth: Float, canvasHeight: Float) {
        x = Random.nextFloat() * canvasWidth
        y = canvasHeight + Random.nextFloat() * 50f
        radius = Random.nextFloat() * 20f + 10f // 10 to 30 radius (will be multiplied by 2 in render)
        speed = Random.nextFloat() * 15f + 10f // 10.0 to 25.0 speed per frame
        alpha = Random.nextFloat() * 0.4f + 0.15f // 0.15 to 0.55 opacity
        active = true
        needsReset = false
    }
    
    fun update() {
        if (!active || needsReset) return
        y -= speed
        if (y < -100f) {
            active = false
        }
    }
}

// --- Main Screen ---
@Composable
fun WaterTrackerScreen(
    userEmail: String,
    onBack: () -> Unit,
    healthViewModel: HealthViewModel = viewModel()
) {
    val uiState by healthViewModel.healthUiState.collectAsStateWithLifecycle()
    
    val percentage = uiState.waterConsumedMl.toFloat() / maxOf(1, uiState.waterGoalMl).toFloat()
    val targetWaterColor = if (percentage > 1f) Color(0xFF50C878) else Color(0xFF0EA5E9)
    val animatedBubbleColor by animateColorAsState(
        targetValue = targetWaterColor, 
        animationSpec = tween(500), 
        label = "bubbleColorAnim"
    )

    // Bubble management
    val maxBubbles = 80
    val bubbles = remember { List(maxBubbles) { Bubble() } }
    
    // Bubble Trigger
    var previousConsumed by remember { mutableIntStateOf(uiState.waterConsumedMl) }
    LaunchedEffect(uiState.waterConsumedMl) {
        if (uiState.waterConsumedMl > previousConsumed) {
            val spawnCount = 40
            var spawned = 0
            for (bubble in bubbles) {
                if (!bubble.active) {
                    bubble.active = true // Mark active
                    bubble.needsReset = true // Guarantee it hits the Canvas reset condition
                    spawned++
                    if (spawned >= spawnCount) break
                }
            }
            // Nếu người dùng bấm quá nhanh (spam) và dùng hết quota 80 hạt, ta tái chế luôn các hạt đang bay
            if (spawned < spawnCount) {
                for (bubble in bubbles) {
                    bubble.active = true
                    bubble.needsReset = true // Ép Canvas reset lại hạt này xuống đáy
                    spawned++
                    if (spawned >= spawnCount) break
                }
            }
        }
        previousConsumed = uiState.waterConsumedMl
    }

    val animationFrame = remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { time ->
                animationFrame.value = time
                for (b in bubbles) {
                    if (b.active) b.update()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            HealthHeaderSection(
                title = "Theo dõi uống nước",
                userEmail = userEmail,
                showBackButton = true,
                onBackClick = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                WaterTrackerWidget(
                    consumedMl = uiState.waterConsumedMl,
                    goalMl = uiState.waterGoalMl,
                    onAddWater = { amount -> healthViewModel.addWater(amount) }
                )
            }
        }
        
        // Full screen bubbles overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            animationFrame.value.let {
                for (b in bubbles) {
                    if (b.active) {
                        if (b.needsReset) {
                            b.reset(size.width, size.height)
                        }
                        drawCircle(
                            color = animatedBubbleColor.copy(alpha = b.alpha),
                            radius = b.radius * 2f,
                            center = androidx.compose.ui.geometry.Offset(b.x, b.y)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaterTrackerWidget(
    consumedMl: Int,
    goalMl: Int,
    onAddWater: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeGoal = if (goalMl <= 0) 1 else goalMl
    val percentage = consumedMl.toFloat() / safeGoal.toFloat()
    
    // Animate target filling surface
    val animatedProgress by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "waterFillAnim"
    )
    
    // Colors
    val targetWaterColor = if (percentage > 1f) Color(0xFF50C878) else Color(0xFF2196F3)
    val animatedWaterColor by animateColorAsState(targetValue = targetWaterColor, animationSpec = tween(500), label = "colorAnim")

    // State for Dialog
    var showPresetDialog by remember { mutableStateOf(false) }
    var selectedPresetName by remember { mutableStateOf("") }
    var selectedPresetIcon by remember { mutableStateOf(Icons.Rounded.WaterDrop) }
    var customDrinkName by remember { mutableStateOf("") }
    var customVolumeInput by remember { mutableStateOf("") }

    // Human Path & Wave Path references
    val humanPath = remember { Path() }
    val wavePath = remember { Path() }
    
    // Infinity animation for wave
    val infiniteTransition = rememberInfiniteTransition(label = "waveInfinite")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // -- Tiêu đề Card --
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = animatedWaterColor, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Mục tiêu Nước", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Spacer(Modifier.height(32.dp))

            // -- Phần 1: Canvas Cột Nước Hình Người --
            Box(
                modifier = Modifier.size(140.dp, 280.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Grid reference: 64x128
                    val scaleX = size.width / 64f
                    val scaleY = size.height / 128f
                    
                    // Build path exactly once
                    if (humanPath.isEmpty) {
                        humanPath.apply {
                            // Head
                            moveTo(32f, 4f)
                            cubicTo(38.6f, 4f, 44f, 9.4f, 44f, 16f)
                            cubicTo(44f, 22.6f, 38.6f, 28f, 32f, 28f)
                            cubicTo(25.4f, 28f, 20f, 22.6f, 20f, 16f)
                            cubicTo(20f, 9.4f, 25.4f, 4f, 32f, 4f)
                            close()
                            
                            // Body 
                            moveTo(32f, 32f)
                            cubicTo(20f, 32f, 13f, 34f, 12f, 44f) // Left Shoulder
                            lineTo(8f, 76f) // Left Arm
                            cubicTo(8f, 80f, 12f, 80f, 14f, 78f)
                            lineTo(18f, 50f) 
                            
                            lineTo(20f, 80f) // Left Torso & Leg
                            lineTo(18f, 120f)
                            cubicTo(18f, 124f, 26f, 124f, 26f, 120f)
                            lineTo(30f, 84f) 
                            
                            cubicTo(31f, 80f, 33f, 80f, 34f, 84f) // Crotch inner 
                            
                            lineTo(38f, 120f) // Right Leg
                            cubicTo(38f, 124f, 46f, 124f, 46f, 120f)
                            lineTo(44f, 80f) 
                            
                            lineTo(46f, 50f) // Right Arm
                            lineTo(50f, 78f)
                            cubicTo(52f, 80f, 56f, 80f, 56f, 76f)
                            
                            lineTo(52f, 44f) // Right Shoulder
                            cubicTo(51f, 34f, 44f, 32f, 32f, 32f)
                            close()
                        }
                    }

                    withTransform({
                        scale(scaleX, scaleY, pivot = androidx.compose.ui.geometry.Offset.Zero)
                    }) {
                        // 1. Draw Gray Background Silhouette
                        drawPath(humanPath, color = Color.LightGray.copy(alpha = 0.2f))

                        // 2. Clip and Draw Water Level
                        clipPath(humanPath) {
                            val fillH = 128f * (1f - minOf(animatedProgress, 1f))
                            
                            // Build wavy surface
                            wavePath.reset()
                            wavePath.moveTo(0f, fillH)
                            
                            val amplitude = 2f
                            val waveLength = 32f
                            
                            if (animatedProgress > 0f) {
                                for (x in 0..64 step 2) {
                                    val y = fillH + sin((x / waveLength) * 2 * PI + wavePhase).toFloat() * amplitude
                                    wavePath.lineTo(x.toFloat(), y)
                                }
                            } else {
                                wavePath.lineTo(64f, fillH)
                            }
                            
                            wavePath.lineTo(64f, 128f)
                            wavePath.lineTo(0f, 128f)
                            wavePath.close()

                            drawPath(wavePath, color = animatedWaterColor)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            
            // Text Dung lượng
            val formatNumber = { value: Int -> NumberFormat.getInstance(Locale("vi", "VN")).format(value) }
            Text(
                text = "${formatNumber(consumedMl)} ml",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-1).sp
            )
            Text(
                text = "/ ${formatNumber(goalMl)} ml (${(percentage * 100).toInt()}%)",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            // -- Phần 3: LazyRow Presets --
            Text("Các loại thức uống", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            val presets = listOf(
                Pair("Nước lọc", Icons.Rounded.WaterDrop),
                Pair("Trà", Icons.Rounded.EmojiFoodBeverage),
                Pair("Cà phê", Icons.Rounded.Coffee),
                Pair("Sữa", Icons.Rounded.LocalDrink),
                Pair("Khác", Icons.Rounded.Add)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presets.size) { index ->
                    val preset = presets[index]
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledTonalIconButton(
                            onClick = { 
                                selectedPresetName = preset.first
                                selectedPresetIcon = preset.second
                                customDrinkName = ""
                                customVolumeInput = ""
                                showPresetDialog = true 
                            },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(preset.second, contentDescription = preset.first, Modifier.size(28.dp), tint = animatedWaterColor)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(preset.first, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // -- Redesigned Dialog --
    if (showPresetDialog) {
        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            icon = {
                Icon(selectedPresetIcon, contentDescription = null, tint = animatedWaterColor, modifier = Modifier.size(32.dp))
            },
            title = {
                Text(
                    text = if (selectedPresetName == "Khác") "Thêm đồ uống mới" else "Thêm ${selectedPresetName}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    if (selectedPresetName == "Khác") {
                        OutlinedTextField(
                            value = customDrinkName,
                            onValueChange = { customDrinkName = it },
                            label = { Text("Tên đồ uống (vd: Sinh tố)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    
                    Text("Phân lượng nhanh", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { onAddWater(100); showPresetDialog = false }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("100") }
                        OutlinedButton(onClick = { onAddWater(200); showPresetDialog = false }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("200") }
                        OutlinedButton(onClick = { onAddWater(300); showPresetDialog = false }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("300") }
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = customVolumeInput,
                        onValueChange = { customVolumeInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Hoặc tự nhập dung tích (ml)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val customVal = customVolumeInput.toIntOrNull() ?: 0
                        if (customVal > 0) {
                            onAddWater(customVal)
                        }
                        showPresetDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Xác nhận")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPresetDialog = false }) { Text("Hủy") }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
