package com.example.finfit.finance.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.finfit.finance.ui.utils.formatCurrency

@Composable
fun BillScannerScreen(
    onBack: () -> Unit,
    onConfirm: (amount: Double, note: String, imageUri: Uri?) -> Unit
) {
    val context = LocalContext.current

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var detectedAmount by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var scanError by remember { mutableStateOf<String?>(null) }
    var showResult by remember { mutableStateOf(false) }
    var isFromGallery by remember { mutableStateOf(false) }

    var cameraFileUri by remember { mutableStateOf<Uri?>(null) }

    fun createCameraFileUri(): Uri {
        val dir = java.io.File(context.cacheDir, "bill_images").also { it.mkdirs() }
        val file = java.io.File(dir, "bill_${System.currentTimeMillis()}.jpg")
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun processUriForOcr(uri: Uri, fromGallery: Boolean = false) {
        isScanning = true
        showResult = false
        scanError = null
        detectedAmount = ""
        try {
            val bmp = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            bitmap = bmp
            runOcr(
                bitmap = bmp,
                context = context,
                uri = if (fromGallery) uri else null,
                onSuccess = { amount ->
                    detectedAmount = amount
                    amountInput = amount
                    isScanning = false
                    showResult = true
                },
                onError = { err ->
                    scanError = err
                    isScanning = false
                    showResult = true
                }
            )
        } catch (e: Exception) {
            scanError = "Lỗi đọc ảnh: ${e.message}"
            isScanning = false
            showResult = true
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraFileUri != null) {
            imageUri = cameraFileUri
            isFromGallery = false
            processUriForOcr(cameraFileUri!!, fromGallery = false)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageUri = uri
            isFromGallery = true
            processUriForOcr(uri, fromGallery = true)
        }
    }

    val teal = Color(0xFF005F73)
    val tealLight = Color(0xFF0A9396)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Brush.verticalGradient(listOf(teal, Color(0xFF0D1117))))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Quét Bill",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Chụp hoá đơn để tự động nhận số tiền",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .weight(1f)
            ) {
                if (imageUri == null) {
                    BillPickerPlaceholder(
                        onCamera = {
                            cameraFileUri = createCameraFileUri()
                            cameraLauncher.launch(cameraFileUri!!)
                        },
                        onGallery = { galleryLauncher.launch("image/*") },
                        teal = teal, tealLight = tealLight
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(20.dp))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imageUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isScanning) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = tealLight, strokeWidth = 3.dp)
                                        Spacer(Modifier.height(12.dp))
                                        Text("Đang nhận diện số tiền...", color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }
                            if (!isScanning) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    SmallActionBtn(icon = Icons.Default.CameraAlt, label = "Chụp lại") {
                                        cameraFileUri = createCameraFileUri()
                                        cameraLauncher.launch(cameraFileUri!!)
                                    }
                                    SmallActionBtn(icon = Icons.Default.Photo, label = "Thư viện") {
                                        galleryLauncher.launch("image/*")
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = showResult,
                            enter = fadeIn() + slideInVertically { 30 }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                if (detectedAmount.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(teal.copy(alpha = 0.15f))
                                            .border(1.dp, teal.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, null, tint = tealLight, modifier = Modifier.size(20.dp))
                                        Column {
                                            Text("Số tiền nhận diện được", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                                            Text(
                                                formatCurrency(detectedAmount.toDoubleOrNull() ?: 0.0),
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black,
                                                color = tealLight
                                            )
                                        }
                                    }
                                } else if (scanError != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFFD62828).copy(alpha = 0.12f))
                                            .border(1.dp, Color(0xFFD62828).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.Warning, null, tint = Color(0xFFF4A261), modifier = Modifier.size(20.dp))
                                        Column {
                                            Text("Không nhận diện được", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                                            Text("Nhập thủ công bên dưới", fontSize = 13.sp, color = Color(0xFFF4A261))
                                        }
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Số tiền (đ)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.7f))
                                    OutlinedTextField(
                                        value = amountInput,
                                        onValueChange = { v -> amountInput = v.filter { it.isDigit() } },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("0", color = Color.White.copy(alpha = 0.3f)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = tealLight,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            cursorColor = tealLight,
                                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        leadingIcon = {
                                            Text("đ", color = tealLight, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                                                modifier = Modifier.padding(start = 4.dp))
                                        },
                                        singleLine = true
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Ghi chú", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.7f))
                                    OutlinedTextField(
                                        value = noteInput,
                                        onValueChange = { noteInput = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("Tên cửa hàng, mô tả...", color = Color.White.copy(alpha = 0.3f)) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = tealLight,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            cursorColor = tealLight,
                                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        singleLine = true
                                    )
                                }

                                // ── Dual-Link Cross-Module: Ghi nhận Dinh Dưỡng ──
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF2E7D32).copy(alpha = 0.15f))
                                        .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Restaurant, null, tint = Color(0xFF81C784), modifier = Modifier.size(22.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Liên kết Nhật ký Dinh Dưỡng", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF81C784))
                                        Text("Tự động đồng bộ bữa ăn & tính calo nạp vào", fontSize = 10.sp, color = Color(0xFFB0B0B0))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showResult && amountInput.isNotEmpty(),
                enter = fadeIn() + slideInVertically { it }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.linearGradient(listOf(teal, tealLight)))
                            .clickable {
                                val amt = amountInput.toDoubleOrNull() ?: 0.0
                                if (amt > 0) onConfirm(amt, noteInput, imageUri)
                             },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCart, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Text("Thêm vào giao dịch", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
