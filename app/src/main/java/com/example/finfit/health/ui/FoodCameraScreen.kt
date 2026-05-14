package com.example.finfit.health.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finfit.health.repository.CameraUIState
import com.example.finfit.health.repository.FoodCameraViewModel
import com.example.finfit.health.utils.toBitmap
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import java.util.concurrent.Executors
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.alpha


data class VisionFoodItem(
    val id: String,
    val result: com.example.finfit.health.model.vision.DishNutritionResult,
    val bitmap: Bitmap,
    val imageUrl: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodCameraScreen(
    mealTitle: String,
    onBackClick: () -> Unit,
    onLogMeal: (List<VisionFoodItem>) -> Unit,
    viewModel: FoodCameraViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by viewModel.uiState.collectAsState()
    val aiStatus by viewModel.aiStatusText.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState()
    val cooldownSeconds by viewModel.cooldownSeconds.collectAsState()
    val isCooldownLocked = cooldownSeconds > 0
    
    val queuedItems = remember { mutableStateListOf<VisionFoodItem>() }
    
    val totalCalories = queuedItems.sumOf { it.result.estimatedCalories.toInt() }
    val maxCalories = 2500 // Increased for realistic daily tracking
    val bgColor = Color(0xFF141414)

    // Permissions
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Photo Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                try {
                    val bitmap = loadBitmapFromUri(context, it)
                    if (bitmap != null) {
                        viewModel.processGalleryBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    Log.e("FoodCamera", "Gallery load failed", e)
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        viewModel.setMealName(mealTitle)
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        } else {
            viewModel.initializeDetector(context)
        }
    }
    
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            viewModel.initializeDetector(context)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- 1. HEADER SECTION ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141414))
                    .padding(top = 44.dp, bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Close Button
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = Color.White, modifier = Modifier.size(18.dp))
                    }

                    // Title & Calorie Progress
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Scan AI Food",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$totalCalories / $maxCalories Cal",
                            color = Color(0xFFadaaaa),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Done Button
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (queuedItems.isNotEmpty()) Color(0xFF2E7D32) else Color(0xFF262626))
                            .clickable(enabled = queuedItems.isNotEmpty() && saveStatus.isEmpty()) {
                                viewModel.saveCompleteMealSession(queuedItems) {
                                    onLogMeal(queuedItems)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (saveStatus.isNotEmpty()) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = "Hoàn\ntất",
                                color = if (queuedItems.isNotEmpty()) Color.White else Color(0xFF666666),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- 2. HORIZONTAL FOOD LIST ---
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(queuedItems) { item ->
                        FoodItemCard(
                            item = item,
                            onDelete = { queuedItems.remove(item) }
                        )
                    }
                    
                    // Placeholder card if empty
                    if (queuedItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .width(56.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.03f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Fastfood, contentDescription = null, tint = Color.White.copy(alpha = 0.12f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // --- 3. CAMERA VIEWPORT ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color.Black)
            ) {
                if (hasCameraPermission) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            val executor = ContextCompat.getMainExecutor(ctx)
                            val analysisExecutor = Executors.newSingleThreadExecutor()

                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val imageAnalyzer = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .setTargetResolution(android.util.Size(1280, 720))
                                    .build()
                                    .also {
                                        it.setAnalyzer(analysisExecutor) { imageProxy ->
                                            try {
                                                val bitmap = imageProxy.toBitmap()
                                                val matrix = Matrix().apply {
                                                    postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                                                }
                                                val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                                                
                                                if (viewModel.captureNextFrame) {
                                                    viewModel.processCapturedBitmap(rotatedBitmap)
                                                } else {
                                                    viewModel.evaluateEnvironment(rotatedBitmap)
                                                }
                                            } catch (e: Exception) {
                                                Log.e("Camera", "Extraction failed", e)
                                            } finally {
                                                imageProxy.close()
                                            }
                                        }
                                    }

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageAnalyzer
                                    )
                                } catch (exc: Exception) {
                                    Log.e("Camera", "Use case binding failed", exc)
                                }
                            }, executor)
                            previewView
                        },
                        modifier = Modifier.fillMaxSize(),
                        onRelease = {
                            // This runs when AndroidView is removed from the composition
                            try {
                                val cameraProvider = ProcessCameraProvider.getInstance(it.context).get()
                                cameraProvider.unbindAll()
                            } catch (e: Exception) {
                                Log.e("Camera", "Failed to unbind on release", e)
                            }
                        }
                    )
                }

                // Overlay for Detections and Scanning Animation
                Box(modifier = Modifier.fillMaxSize()) {
                    // 1. Static Viewfinder Frame
                    CameraViewfinder()

                    // 2. Scanning Animation (Instant UI response during upload/detection/analysis)
                    if (uiState is CameraUIState.Analyzing || uiState is CameraUIState.UploadingImage || uiState is CameraUIState.Detecting) {
                        ScanningOverlay()
                    }

                    // Instruction Text or Cooldown Warning Overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 140.dp)
                    ) {
                        if (isCooldownLocked) {
                            Surface(
                                color = Color(0xFFb71c1c),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "Vượt hạn mức AI. Phục hồi sau: ${cooldownSeconds}s",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else if (aiStatus.isNotEmpty() && uiState is CameraUIState.Idle) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.5f),
                                shape = CircleShape,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = aiStatus,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- 4. BOTTOM CONTROLS (Compact) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141414))
                    .padding(bottom = 12.dp, top = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gallery Button (smaller)
                    IconButton(
                        onClick = { 
                            if (!isCooldownLocked) {
                                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        },
                        enabled = !isCooldownLocked,
                        modifier = Modifier.size(40.dp).background(if (isCooldownLocked) Color.Gray.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = "Thư viện", tint = if (isCooldownLocked) Color.White.copy(alpha = 0.3f) else Color.White, modifier = Modifier.size(20.dp))
                    }

                    // Shutter Button (smaller)
                    val isShutterEnabled = !isCooldownLocked && uiState !is CameraUIState.Analyzing && uiState !is CameraUIState.Success
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .border(3.dp, if (isCooldownLocked) Color.Gray.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.2f), CircleShape)
                            .padding(4.dp)
                            .background(Color.Transparent, CircleShape)
                            .clickable(
                                enabled = isShutterEnabled,
                                onClick = { viewModel.requestCapture() }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(when {
                                    isCooldownLocked -> Color.DarkGray
                                    uiState is CameraUIState.Analyzing -> Color.Gray
                                    else -> Color.White
                                })
                        )
                    }

                    // Mode Switcher Spacer
                    Box(modifier = Modifier.size(40.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tab Switcher (compact & fixed alignment)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Photo", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.size(16.dp, 2.dp).background(Color(0xFF64B5F6)))
                    }
                    Spacer(modifier = Modifier.width(48.dp))
                    Text("Barcode", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // --- SUCCESS DIALOG / CARD ---
        if (uiState is CameraUIState.Success) {
            val successState = uiState as CameraUIState.Success
            MealAnalysisCard(
                result = successState.result,
                bitmap = successState.bitmap,
                saveStatus = saveStatus,
                onDismiss = { viewModel.dismissCard() },
                onAdd = {
                    val newItem = VisionFoodItem(
                        id = System.currentTimeMillis().toString(),
                        result = successState.result,
                        bitmap = successState.bitmap,
                        imageUrl = successState.imageUrl
                    )
                    queuedItems.add(newItem)
                    viewModel.dismissCard()
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        
        // Error Snackbar
        if (uiState is CameraUIState.Error) {
            Surface(
                color = Color(0xFFD32F2F),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 8.dp,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = (uiState as CameraUIState.Error).message,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    IconButton(onClick = { viewModel.dismissError() }) {
                        Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealAnalysisCard(
    result: com.example.finfit.health.model.vision.DishNutritionResult,
    bitmap: Bitmap,
    saveStatus: String = "",
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
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

            // Image and Calories
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
                    Text(
                        text = "${result.estimatedCalories.toInt()}",
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

            // Macros Row
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF2A2A2A), RoundedCornerShape(16.dp)).padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroItem("Protein", "${result.macros.proteinG.toInt()}g", Color(0xFFF06292))
                MacroItem("Carbs", "${result.macros.carbsG.toInt()}g", Color(0xFF4FC3F7))
                MacroItem("Fat", "${result.macros.fatG.toInt()}g", Color(0xFFFFB74D))
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

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Text("XÁC NHẬN & THÊM", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }

            // Persistence Status Feedback
            if (saveStatus.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
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

@Composable
fun MacroItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(text = label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun VisionFoodCard(item: VisionFoodItem) {
    // Re-using simplified view for the list of already added items
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

/**
 * Helper to load bitmap from Uri safely
 */
private fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? {
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

@Composable
fun ScanningOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    
    // Vertical scan line position
    val lineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lineOffset"
    )

    // Bouncing effect for the icon
    val iconBounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -30f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconBounce"
    )

    // Pulsing effect for the background
    val bgAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgAlpha"
    )

    // Dynamic status text
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

            // Draw glowing scan line
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

            // Draw sharp laser line
            drawLine(
                color = Color(0xFF4CAF50),
                start = Offset(0f, currentY),
                end = Offset(width, currentY),
                strokeWidth = 2.dp.toPx()
            )
        }

        // HUD Text & Animated Icon
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // BOUNCING FOOD ICON
            Icon(
                imageVector = Icons.Rounded.Fastfood,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier
                    .size(64.dp)
                    .offset(y = iconBounce.dp)
                    .graphicsLayer {
                        rotationZ = iconBounce * 0.5f // Slight rotation as it bounces
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
            
            // Dynamic Progress Dots
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
