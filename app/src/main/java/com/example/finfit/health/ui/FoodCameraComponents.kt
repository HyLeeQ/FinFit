package com.example.finfit.health.ui

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect

// ──────────────────────────────────────────────────────────────
//  CameraViewfinder Component
// ──────────────────────────────────────────────────────────────
@Composable
fun CameraViewfinder() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val rectSize = minOf(width, height) * 0.7f
        val left = (width - rectSize) / 2
        val top = (height - rectSize) / 2
        val rect = androidx.compose.ui.geometry.Rect(left, top, left + rectSize, top + rectSize)
        val strokeWidth = 3.dp.toPx()

        // 1. Dark overlay with a hole in the middle
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = rect,
                    cornerRadius = CornerRadius(24.dp.toPx())
                )
            )
        }

        clipPath(path, clipOp = ClipOp.Difference) {
            drawRect(color = Color.Black.copy(alpha = 0.5f))
        }

        // 2. Draw 4 corners (Brackets)
        val cornerLength = 40.dp.toPx()
        val cornerColor = Color.White.copy(alpha = 0.8f)

        // Top Left
        drawLine(
            color = cornerColor,
            start = Offset(left, top + cornerLength),
            end = Offset(left, top),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = cornerColor,
            start = Offset(left, top),
            end = Offset(left + cornerLength, top),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Top Right
        drawLine(
            color = cornerColor,
            start = Offset(left + rectSize - cornerLength, top),
            end = Offset(left + rectSize, top),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = cornerColor,
            start = Offset(left + rectSize, top),
            end = Offset(left + rectSize, top + cornerLength),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Bottom Left
        drawLine(
            color = cornerColor,
            start = Offset(left, top + rectSize - cornerLength),
            end = Offset(left, top + rectSize),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = cornerColor,
            start = Offset(left, top + rectSize),
            end = Offset(left + cornerLength, top + rectSize),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Bottom Right
        drawLine(
            color = cornerColor,
            start = Offset(left + rectSize - cornerLength, top + rectSize),
            end = Offset(left + rectSize, top + rectSize),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = cornerColor,
            start = Offset(left + rectSize, top + rectSize),
            end = Offset(left + rectSize, top + rectSize - cornerLength),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

// ──────────────────────────────────────────────────────────────
//  FoodItemCard Component
// ──────────────────────────────────────────────────────────────
@Composable
fun FoodItemCard(
    item: VisionFoodItem,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(96.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Image Section - EXACTLY 50%
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
            ) {
                Image(
                    bitmap = item.bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Top gradient for X button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent)
                            )
                        )
                )
            }
            
            // Text Content - EXACTLY 50%
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.result.dishName,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Text(
                    text = "${item.result.estimatedCalories.toInt()} Cal",
                    color = Color(0xFF64B5F6),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Delete button
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(20.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
//  MealAnalysisCard Component
// ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealAnalysisCard(
    result: com.example.finfit.health.model.vision.DishNutritionResult,
    bitmap: Bitmap,
    saveStatus: String = "",
    onDismiss: () -> Unit,
    onAdd: (com.example.finfit.health.model.vision.DishNutritionResult) -> Unit,
    modifier: Modifier = Modifier
) {
    var portionScale by remember { mutableStateOf(1.0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.88f)
            .padding(bottom = 16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = result.dishName,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            
                            val confPercent = (result.dishConfidence * 100).toInt()
                            val confColor = if (confPercent > 80) Color(0xFF4CAF50) else if (confPercent > 50) Color(0xFFFFC107) else Color(0xFFF44336)
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(confColor))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Độ tin cậy: $confPercent%",
                                    color = confColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        
                        IconButton(onClick = onDismiss, modifier = Modifier.background(Color(0xFF2A2A2A), CircleShape).size(32.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Image and Calories (Dynamically Scaled)
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        
                        Spacer(modifier = Modifier.width(20.dp))
                        
                        Column {
                            val scaledCalories = (result.estimatedCalories * portionScale).toInt()
                            Text(
                                text = "$scaledCalories",
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Calories ước tính",
                                color = Color(0xFF8E8E8E),
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Macros Row (Dynamically Scaled)
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFF2A2A2A), RoundedCornerShape(16.dp)).padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val scaledProt = (result.macros.proteinG * portionScale).toInt()
                        val scaledCarb = (result.macros.carbsG * portionScale).toInt()
                        val scaledFat = (result.macros.fatG * portionScale).toInt()
                        MacroItem("Protein", "${scaledProt}g", Color(0xFFF06292))
                        MacroItem("Carbs", "${scaledCarb}g", Color(0xFF4FC3F7))
                        MacroItem("Fat", "${scaledFat}g", Color(0xFFFFB74D))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Ingredients
                    Text("Thành phần nhận diện:", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        result.ingredients.take(6).forEach { ingredient ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF333333))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(ingredient.name, color = Color(0xFFE0E0E0), fontSize = 12.sp)
                            }
                        }
                    }

                    // Possible Alternatives (if confidence is moderate)
                    if (result.possibleDishes.size > 1 && result.dishConfidence < 0.9f) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Món ăn khả thi khác:", color = Color(0xFF8E8E8E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = result.possibleDishes.drop(1).joinToString(", ") { it.name },
                            color = Color(0xFF666666),
                            fontSize = 12.sp
                        )
                    }

                    // --- PORTION SIZE SEGMENTED SELECTOR ---
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Khẩu phần ăn (Portion Size):",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(0.5f to "0.5x (Ít)", 1.0f to "1.0x (Chuẩn)", 1.5f to "1.5x (Nhiều)", 2.0f to "2.0x (Gấp đôi)").forEach { (scale, label) ->
                            val isSelected = portionScale == scale
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF64B5F6) else Color.Transparent)
                                    .clickable { portionScale = scale }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Analysis Notes
                    if (result.analysisNotes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF2A2A2A).copy(alpha = 0.5f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                            Column {
                                result.analysisNotes.forEach { note ->
                                    Text("• $note", color = Color(0xFFadaaaa), fontSize = 11.sp, fontStyle = FontStyle.Italic)
                                }
                            }
                        }
                    }

                } // end scrollable column

                // --- PINNED BOTTOM: CONFIRM BUTTON (fixed, never moves) ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E))
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = {
                            val scaledResult = result.copy(
                                estimatedCalories = result.estimatedCalories * portionScale,
                                macros = result.macros.copy(
                                    proteinG = result.macros.proteinG * portionScale,
                                    carbsG = result.macros.carbsG * portionScale,
                                    fatG = result.macros.fatG * portionScale
                                )
                            )
                            onAdd(scaledResult)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            "XÁC NHẬN & THÊM",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }

                    if (saveStatus.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (saveStatus.contains("Đang lưu")) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color(0xFF64B5F6),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            } else if (saveStatus.contains("thành công")) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = saveStatus,
                                color = if (saveStatus.contains("Lỗi")) Color(0xFFEF5350) else Color(0xFFB0B0B0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MacroItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(text = label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun VisionFoodCard(item: VisionFoodItem) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a1a))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                bitmap = item.bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(item.result.dishName, color = Color.White, fontWeight = FontWeight.Bold)
                Text("${item.result.estimatedCalories.toInt()} Cal", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
//  loadBitmapFromUri Helper
// ──────────────────────────────────────────────────────────────
fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
            android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        Log.e("FoodCameraScreen", "Gallery load error", e)
        null
    }
}

// ──────────────────────────────────────────────────────────────
//  ScanningOverlay Component
// ──────────────────────────────────────────────────────────────
@Composable
fun ScanningOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    
    val lineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lineOffset"
    )

    val iconBounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -30f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconBounce"
    )

    val bgAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgAlpha"
    )

    val statusMessages = listOf(
        "AI REASONING...",
        "SCANNING INGREDIENTS...",
        "CALCULATING MACROS...",
        "VALIDATING DISH...",
        "OPTIMIZING FOR VIETNAMESE CUISINE..."
    )
    
    var currentMessageIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(1200)
            currentMessageIndex = (currentMessageIndex + 1) % statusMessages.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = bgAlpha))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val currentY = lineOffset * height

            val brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF4CAF50).copy(alpha = 0.1f),
                    Color(0xFF4CAF50).copy(alpha = 0.8f),
                    Color(0xFF4CAF50).copy(alpha = 0.1f),
                    Color.Transparent
                ),
                startY = currentY - 50.dp.toPx(),
                endY = currentY + 50.dp.toPx()
            )

            drawRect(
                brush = brush,
                topLeft = Offset(0f, currentY - 50.dp.toPx()),
                size = Size(width, 100.dp.toPx())
            )

            drawLine(
                color = Color(0xFF4CAF50),
                start = Offset(0f, currentY),
                end = Offset(width, currentY),
                strokeWidth = 2.dp.toPx()
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.Fastfood,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier
                    .size(64.dp)
                    .offset(y = iconBounce.dp)
                    .graphicsLayer {
                        rotationZ = iconBounce * 0.5f
                    }
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = statusMessages[currentMessageIndex],
                color = Color(0xFF4CAF50),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "TRUY XUẤT DỮ LIỆU DINH DƯỠNG",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    val dotAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = index * 200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dotAlpha"
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50).copy(alpha = dotAlpha))
                    )
                }
            }
        }
    }
}
