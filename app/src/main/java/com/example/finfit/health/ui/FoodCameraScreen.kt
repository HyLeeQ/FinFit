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
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState


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
    var showManualSearchSheet by remember { mutableStateOf(false) }
    
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
                                val snapshot = queuedItems.toList() // freeze a copy before navigation
                                viewModel.saveCompleteMealSession(snapshot) {
                                    onLogMeal(snapshot)
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

                // --- MARQUEE OFFLINE RUNNING NOTIFICATION BANNER ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(Color(0xFF2C2514), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(vertical = 6.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "Tips",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "💡 Mẹo tiết kiệm thời gian: Nhấn biểu tượng kính lúp 🔍 ở góc phải bên dưới để chọn ngay món ăn có sẵn với độ chính xác tuyệt đối 100% mà không cần đợi quét AI!",
                        color = Color(0xFFFFCC80),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
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
                    // Use DisposableEffect to manage camera lifecycle cleanly
                    val analysisExecutorRef = remember { Executors.newSingleThreadExecutor() }
                    var isScreenActive by remember { mutableStateOf(true) }

                    DisposableEffect(lifecycleOwner) {
                        onDispose {
                            isScreenActive = false
                            // Shutdown executor first so no new frames are submitted
                            analysisExecutorRef.shutdown()
                            // Then unbind camera safely
                            try {
                                ProcessCameraProvider.getInstance(context).get()?.unbindAll()
                            } catch (e: Exception) {
                                Log.e("Camera", "Cleanup unbind error", e)
                            }
                        }
                    }

                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val imageAnalyzer = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .setTargetResolution(android.util.Size(640, 480))
                                    .build()
                                    .also {
                                        it.setAnalyzer(analysisExecutorRef) { imageProxy ->
                                            // Guard: stop processing if screen is gone
                                            if (!isScreenActive) {
                                                imageProxy.close()
                                                return@setAnalyzer
                                            }
                                            try {
                                                val bitmap = imageProxy.toBitmap()
                                                val matrix = Matrix().apply {
                                                    postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                                                }
                                                val rotatedBitmap = Bitmap.createBitmap(
                                                    bitmap, 0, 0,
                                                    bitmap.width, bitmap.height,
                                                    matrix, true
                                                )
                                                if (!bitmap.isRecycled) bitmap.recycle()

                                                if (!isScreenActive) {
                                                    if (!rotatedBitmap.isRecycled) rotatedBitmap.recycle()
                                                    return@setAnalyzer
                                                }

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
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
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

                    // Manual Search & Log Button (Premium Offline alternative)
                    IconButton(
                        onClick = { showManualSearchSheet = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = "Chọn thủ công",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
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
            // Guard: don't render if bitmap was already recycled (e.g. during rapid navigation)
            if (!successState.bitmap.isRecycled) {
                MealAnalysisCard(
                    result = successState.result,
                    bitmap = successState.bitmap,
                    saveStatus = saveStatus,
                    onDismiss = { viewModel.dismissCard() },
                    onAdd = { scaledResult ->
                        val newItem = VisionFoodItem(
                            id = System.currentTimeMillis().toString(),
                            result = scaledResult,
                            bitmap = successState.bitmap,
                            imageUrl = successState.imageUrl
                        )
                        queuedItems.add(newItem)
                        viewModel.dismissCard()
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
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
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { viewModel.dismissError() }) {
                        Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // --- MANUAL SEARCH & SELECT OVERLAY (Hybrid 0ms Latency Custom Creator) ---
        if (showManualSearchSheet) {
            var searchQuery by remember { mutableStateOf("") }
            var isCreatingCustomFood by remember { mutableStateOf(false) }

            val allFoods = remember {
                com.example.finfit.health.ai.LocalNutritionDb.db.entries.toList()
            }
            val filteredFoods = allFoods.filter {
                it.value.name.contains(searchQuery, ignoreCase = true) ||
                it.key.contains(searchQuery, ignoreCase = true)
            }

            // Custom Gradient Placeholder Generator
            val placeholderBitmap = remember {
                val b = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(b)
                val paint = android.graphics.Paint().apply {
                    shader = android.graphics.LinearGradient(
                        0f, 0f, 300f, 300f,
                        android.graphics.Color.parseColor("#1E3C72"), // Deep Royal Blue
                        android.graphics.Color.parseColor("#2A5298"), // Light Blue
                        android.graphics.Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, 300f, 300f, paint)
                b
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable { 
                        showManualSearchSheet = false 
                        isCreatingCustomFood = false
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    color = Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .clickable(enabled = false) {} // Prevent click-through
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        if (isCreatingCustomFood) {
                            // --- PART 1: CUSTOM FOOD ENTRY FORM ---
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { isCreatingCustomFood = false },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color.White.copy(alpha = 0.08f), CircleShape)
                                    ) {
                                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Quay lại", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Tự nhập món ăn mới",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(
                                    onClick = { 
                                        showManualSearchSheet = false 
                                        isCreatingCustomFood = false
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            var customName by remember { mutableStateOf(searchQuery) }
                            var customCalories by remember { mutableStateOf("350") }
                            var customProtein by remember { mutableStateOf("15") }
                            var customCarbs by remember { mutableStateOf("45") }
                            var customFat by remember { mutableStateOf("10") }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Dish Name
                                OutlinedTextField(
                                    value = customName,
                                    onValueChange = { customName = it },
                                    label = { Text("Tên món ăn (bắt buộc)", color = Color.Gray) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF64B5F6),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                        focusedContainerColor = Color.White.copy(alpha = 0.03f),
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.01f)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Calories
                                OutlinedTextField(
                                    value = customCalories,
                                    onValueChange = { customCalories = it },
                                    label = { Text("Calo ước tính (kcal)", color = Color.Gray) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF64B5F6),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                        focusedContainerColor = Color.White.copy(alpha = 0.03f),
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.01f)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Macros Row (Protein, Carbs, Fat)
                                Text(
                                    text = "THÀNH PHẦN DINH DƯỠNG (MACROS)",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customProtein,
                                        onValueChange = { customProtein = it },
                                        label = { Text("Đạm (g)", color = Color.Gray) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFF81C784),
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = customCarbs,
                                        onValueChange = { customCarbs = it },
                                        label = { Text("Carb (g)", color = Color.Gray) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFFFFD54F),
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = customFat,
                                        onValueChange = { customFat = it },
                                        label = { Text("Béo (g)", color = Color.Gray) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFFE57373),
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Submit Button
                            Button(
                                onClick = {
                                    if (customName.trim().isNotEmpty()) {
                                        val customResult = com.example.finfit.health.model.vision.DishNutritionResult(
                                            dishName = customName.trim(),
                                            dishConfidence = 1.0f,
                                            possibleDishes = listOf(com.example.finfit.health.model.vision.DishInfo(customName.trim(), 1.0f)),
                                            ingredients = listOf(com.example.finfit.health.model.vision.IngredientInfo("Tự nhập thủ công", 1.0f)),
                                            estimatedCalories = (customCalories.toFloatOrNull() ?: 350f),
                                            macros = com.example.finfit.health.model.vision.Macros(
                                                proteinG = (customProtein.toFloatOrNull() ?: 15f),
                                                carbsG = (customCarbs.toFloatOrNull() ?: 45f),
                                                fatG = (customFat.toFloatOrNull() ?: 10f)
                                            ),
                                            healthScore = 8.0f,
                                            analysisNotes = listOf("Món ăn tùy chỉnh được thiết lập nhanh ngoại tuyến.")
                                        )
                                        viewModel.selectFoodManually(customResult, placeholderBitmap)
                                        isCreatingCustomFood = false
                                        showManualSearchSheet = false
                                    }
                                },
                                enabled = customName.trim().isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF64B5F6),
                                    disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "Xác nhận & Thêm món",
                                    color = if (customName.trim().isNotEmpty()) Color.Black else Color.White.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        } else {
                            // --- PART 2: SEARCH & LIST AVAILABLE FOODS ---
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Chọn nhanh món ăn",
                                    color = Color.White,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { showManualSearchSheet = false },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Search Bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Tìm kiếm món ăn...", color = Color.Gray) },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF64B5F6),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // "Tự nhập món ăn mới" quick action row (Aesthetic glow card)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF64B5F6).copy(alpha = 0.08f))
                                    .border(1.dp, Color(0xFF64B5F6).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        isCreatingCustomFood = true
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF64B5F6).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Add,
                                        contentDescription = null,
                                        tint = Color(0xFF90CAF9),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (searchQuery.isEmpty()) "Tự nhập món ăn tùy chỉnh..." else "Tự nhập món ăn: \"$searchQuery\"",
                                    color = Color(0xFF90CAF9),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Food List Header
                            Text(
                                text = "DANH SÁCH MÓN ĂN AN TOÀN (${filteredFoods.size})",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredFoods) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.03f))
                                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                            .clickable {
                                                val nutritionResult = com.example.finfit.health.ai.LocalNutritionDb.getNutritionForLabel(item.key, 1.0f)
                                                if (nutritionResult != null) {
                                                    viewModel.selectFoodManually(nutritionResult, placeholderBitmap)
                                                    showManualSearchSheet = false
                                                }
                                            }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Food Icon or Circle
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF2E7D32).copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Rounded.Fastfood,
                                                contentDescription = null,
                                                tint = Color(0xFF81C784),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        // Name and Details
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.value.name,
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${item.value.ingredients.take(3).joinToString(", ")}...",
                                                color = Color.Gray,
                                                fontSize = 12.sp
                                            )
                                        }

                                        // Calories Badge
                                        Surface(
                                            color = Color(0xFF64B5F6).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "${item.value.calories.toInt()} kcal",
                                                color = Color(0xFF90CAF9),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


