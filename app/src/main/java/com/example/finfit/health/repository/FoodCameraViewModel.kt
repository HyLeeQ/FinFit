package com.example.finfit.health.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finfit.health.ai.YoloFoodDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.finfit.BuildConfig
import com.example.finfit.health.api.vision.GeminiVisionProvider
import com.example.finfit.health.model.vision.VisionAiResult
import com.example.finfit.health.utils.BitmapUtils
import java.io.ByteArrayOutputStream
import android.graphics.BitmapFactory

sealed class CameraUIState {
    object Idle : CameraUIState()
    object Detecting : CameraUIState()
    object UploadingImage : CameraUIState()
    object Analyzing : CameraUIState()
    data class Success(
        val result: com.example.finfit.health.model.vision.DishNutritionResult, 
        val bitmap: Bitmap,
        val imageUrl: String
    ) : CameraUIState()
    data class Error(val message: String) : CameraUIState()
}

class FoodCameraViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<CameraUIState>(CameraUIState.Idle)
    val uiState: StateFlow<CameraUIState> = _uiState.asStateFlow()

    // Informational AI status text (NOT used for locking)
    private val _aiStatusText = MutableStateFlow("")
    val aiStatusText: StateFlow<String> = _aiStatusText.asStateFlow()

    private val _saveStatus = MutableStateFlow<String>("")
    val saveStatus: StateFlow<String> = _saveStatus.asStateFlow()

    private var processingJob: kotlinx.coroutines.Job? = null
    private var mealName: String = "Bữa ăn"
    private var yoloDetector: YoloFoodDetector? = null

    // Repositories
    private val visionAiProvider = GeminiVisionProvider(BuildConfig.VISION_API_KEY)
    private val visionAiRepository = VisionAiRepository(visionAiProvider)
    private val mealRepository = MealRepository()
    private var cloudinaryRepository: com.example.finfit.health.data.remote.cloudinary.CloudinaryRepository? = null

    // Throttle for background preview detection
    private var lastEvalTime: Long = 0
    private var isEvaluating = java.util.concurrent.atomic.AtomicBoolean(false)

    // Flag to capture the next frame
    @Volatile
    var captureNextFrame = false
        private set

    private val saveMutex = kotlinx.coroutines.sync.Mutex()
    
    fun initializeDetector(context: Context) {
        if (yoloDetector == null) {
            try {
                yoloDetector = YoloFoodDetector(context)
                _aiStatusText.value = "Hướng camera vào món ăn"
            } catch (e: Exception) {
                Log.e("FoodCameraVM", "Failed to init AI", e)
                _aiStatusText.value = "Lỗi khởi tạo AI"
            }
        }
        if (cloudinaryRepository == null) {
            val service = com.example.finfit.health.data.remote.cloudinary.CloudinaryService(context)
            cloudinaryRepository = com.example.finfit.health.data.remote.cloudinary.CloudinaryRepository(service)
        }
    }

    fun setMealName(name: String) {
        this.mealName = name
    }

    // --- RATE LIMIT QUOTA TRACKER (Sliding 60-second Window) ---
    private val scanTimestamps = java.util.concurrent.CopyOnWriteArrayList<Long>()
    private val _cooldownSeconds = MutableStateFlow(0)
    val cooldownSeconds: StateFlow<Int> = _cooldownSeconds.asStateFlow()
    private var cooldownJob: kotlinx.coroutines.Job? = null

    @Synchronized
    fun canInitiateScan(): Boolean {
        val now = System.currentTimeMillis()
        scanTimestamps.removeIf { now - it > 60_000 }
        
        if (scanTimestamps.size >= 5) {
            startCooldownTimer()
            return false
        }
        return true
    }

    @Synchronized
    fun recordScanAttempt() {
        scanTimestamps.add(System.currentTimeMillis())
        val now = System.currentTimeMillis()
        scanTimestamps.removeIf { now - it > 60_000 }
        if (scanTimestamps.size >= 5) {
            startCooldownTimer()
        }
    }

    private fun startCooldownTimer() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                scanTimestamps.removeIf { now - it > 60_000 }
                if (scanTimestamps.size < 5) {
                    _cooldownSeconds.value = 0
                    break
                }
                val oldest = scanTimestamps.firstOrNull() ?: break
                val remainingMs = 60_000 - (now - oldest)
                if (remainingMs <= 0) {
                    _cooldownSeconds.value = 0
                    break
                }
                _cooldownSeconds.value = (remainingMs / 1000).toInt() + 1
                kotlinx.coroutines.delay(500)
            }
        }
    }

    fun triggerImmediateCooldown() {
        scanTimestamps.clear()
        val now = System.currentTimeMillis()
        repeat(5) { scanTimestamps.add(now) }
        startCooldownTimer()
    }

    /** Initiates capture request protected by the sliding Quota tracker */
    fun requestCapture() {
        if (!canInitiateScan()) {
            _uiState.value = CameraUIState.Error("Hệ thống đang phục hồi hạn mức AI. Vui lòng đợi ${_cooldownSeconds.value} giây.")
            return
        }
        if (_uiState.value !is CameraUIState.Analyzing) {
            recordScanAttempt()
            _uiState.value = CameraUIState.Analyzing
            captureNextFrame = true
            _saveStatus.value = "" // Reset save status
        }
    }


    /** Background evaluation - informational only, does NOT block capture */
    fun evaluateEnvironment(bitmap: Bitmap) {
        if (_uiState.value !is CameraUIState.Idle) return

        val currentTime = System.currentTimeMillis()
        
        // We run detection roughly every 200ms (5 FPS max for inference) to keep UI smooth
        if (currentTime - lastEvalTime < 200) return 

        if (!isEvaluating.compareAndSet(false, true)) {
            return // Skip frame, inference is still running
        }
        
        lastEvalTime = currentTime

        viewModelScope.launch(Dispatchers.Default) {
            try {
                // Scale down for faster detection
                val smallBitmap = if (bitmap.width > 640) {
                    Bitmap.createScaledBitmap(bitmap, 640, (640f * bitmap.height / bitmap.width).toInt(), true)
                } else bitmap

                // STEP 1: YOLO Detect Food Region
                val results = yoloDetector?.detect(smallBitmap) ?: emptyList()
                val bestBox = results.maxByOrNull { it.confidence }

                if (smallBitmap != bitmap) smallBitmap.recycle()
                
                withContext(Dispatchers.Main) {

                    if (bestBox != null && bestBox.confidence > 0.25f) {
                        _aiStatusText.value = "Phát hiện món ăn. Giữ yên camera..."
                    } else {
                        _aiStatusText.value = "Hướng camera vào món ăn"
                    }
                }
            } catch (e: Exception) {
                Log.e("FoodCameraVM", "Eval error: ${e.message}")
            } finally {
                isEvaluating.set(false)
            }
        }
    }

    /** Process the captured frame from CAMERA for final detection (YOLO → Crop → Upload → Vision AI) */
    fun processCapturedBitmap(bitmap: Bitmap) {
        captureNextFrame = false
        
        // Cancel any pending job to avoid overlapping
        processingJob?.cancel()

        processingJob = viewModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) {
                _uiState.value = CameraUIState.Detecting
                _saveStatus.value = ""
                _aiStatusText.value = "Đang xử lý ảnh..."
            }
            try {
                // IMPORTANT: Scale down IMMEDIATELY to speed up all subsequent steps (YOLO, Crop, Upload)
                val sourceBitmap = if (bitmap.width > 1080) {
                    Bitmap.createScaledBitmap(bitmap, 1080, (1080f * bitmap.height / bitmap.width).toInt(), true)
                } else bitmap

                // 1. YOLO Detect Food Region (Now much faster on scaled bitmap)
                val results = yoloDetector?.detect(sourceBitmap) ?: emptyList()
                val bestBox = results.maxByOrNull { it.confidence }

                if (bestBox == null) {
                    if (sourceBitmap != bitmap) sourceBitmap.recycle()
                    withContext(Dispatchers.Main) {
                        _uiState.value = CameraUIState.Error("Không nhận diện được món ăn. Hãy thử lại.")
                    }
                    return@launch
                }

                // 2. Crop
                withContext(Dispatchers.Main) {
                    _aiStatusText.value = "Đang trích xuất món ăn..."
                }

                val croppedBitmap = BitmapUtils.cropFromBoundingBox(sourceBitmap, bestBox.boundingBox) ?: run {
                    if (sourceBitmap != bitmap) sourceBitmap.recycle()
                    withContext(Dispatchers.Main) {
                        _uiState.value = CameraUIState.Error("Lỗi xử lý ảnh vùng cắt.")
                    }
                    return@launch
                }

                // 3. Upload to Cloudinary
                withContext(Dispatchers.Main) {
                    _uiState.value = CameraUIState.UploadingImage
                    _aiStatusText.value = "Đang tải ảnh lên máy chủ..."
                }
                val imageUrl = cloudinaryRepository?.uploadImage(croppedBitmap)
                if (imageUrl == null) {
                    croppedBitmap.recycle()
                    if (sourceBitmap != bitmap) sourceBitmap.recycle()
                    withContext(Dispatchers.Main) {
                        _uiState.value = CameraUIState.Error("Lỗi tải ảnh lên Cloudinary.")
                    }
                    return@launch
                }

                // 4. Gemini Analysis
                withContext(Dispatchers.Main) {
                    _uiState.value = CameraUIState.Analyzing
                    _aiStatusText.value = "Đang phân tích dinh dưỡng..."
                }
                val compressedForAi = compressAndScaleBitmap(croppedBitmap)
                val result = visionAiRepository.analyzeFood(compressedForAi)
                
                // Cleanup temp bitmaps
                if (sourceBitmap != bitmap) sourceBitmap.recycle()
                
                // Create highly optimized UI bitmap (300px)
                val uiBitmap = Bitmap.createScaledBitmap(croppedBitmap, 300, (300f * croppedBitmap.height / croppedBitmap.width).toInt(), true)
                croppedBitmap.recycle()
                compressedForAi.recycle()

                withContext(Dispatchers.Main) {
                    when (result) {
                        is VisionAiResult.Success -> {
                            _uiState.value = CameraUIState.Success(result.data, uiBitmap, imageUrl)
                        }
                        is VisionAiResult.Error -> {
                            uiBitmap.recycle()
                            if (result.message.contains("429") || result.message.contains("vượt quá giới hạn") || result.message.contains("Quota")) {
                                triggerImmediateCooldown()
                                _uiState.value = CameraUIState.Error("Vượt hạn mức AI. Các nút gửi/chụp sẽ bị khóa trong 60 giây để tuân thủ thời gian API.")
                            } else {
                                _uiState.value = CameraUIState.Error(result.message)
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("FoodCameraVM", "Capture processing error", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = CameraUIState.Error("Lỗi: ${e.message?.take(50)}")
                }
            }
        }
    }

    /** Process image from Gallery - Upload then Analysis */
    fun processGalleryBitmap(bitmap: Bitmap) {
        if (!canInitiateScan()) {
            _uiState.value = CameraUIState.Error("Hệ thống đang phục hồi hạn mức AI. Vui lòng đợi ${_cooldownSeconds.value} giây.")
            return
        }
        recordScanAttempt()
        viewModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) {
                _uiState.value = CameraUIState.UploadingImage
                _saveStatus.value = ""
                _aiStatusText.value = "Đang chuẩn bị ảnh..."
            }
            try {
                // Scale down for speed and memory safety
                val sourceBitmap = if (bitmap.width > 1280) {
                    Bitmap.createScaledBitmap(bitmap, 1280, (1280f * bitmap.height / bitmap.width).toInt(), true)
                } else bitmap

                // 1. Upload to Cloudinary
                val imageUrl = cloudinaryRepository?.uploadImage(sourceBitmap)
                if (imageUrl == null) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = CameraUIState.Error("Lỗi tải ảnh lên Cloudinary.")
                    }
                    return@launch
                }

                // 2. Gemini Analysis
                withContext(Dispatchers.Main) {
                    _uiState.value = CameraUIState.Analyzing
                    _aiStatusText.value = "Đang phân tích dinh dưỡng..."
                }
                val compressedForAi = compressAndScaleBitmap(bitmap)
                val result = visionAiRepository.analyzeFood(compressedForAi)

                val uiBitmap = Bitmap.createScaledBitmap(sourceBitmap, 300, (300f * sourceBitmap.height / sourceBitmap.width).toInt(), true)
                if (sourceBitmap != bitmap) sourceBitmap.recycle()
                compressedForAi.recycle()

                withContext(Dispatchers.Main) {
                    when (result) {
                        is VisionAiResult.Success -> {
                            _uiState.value = CameraUIState.Success(result.data, uiBitmap, imageUrl)
                        }
                        is VisionAiResult.Error -> {
                            uiBitmap.recycle()
                            if (result.message.contains("429") || result.message.contains("vượt quá giới hạn") || result.message.contains("Quota")) {
                                triggerImmediateCooldown()
                                _uiState.value = CameraUIState.Error("Vượt hạn mức AI. Các nút gửi/chụp sẽ bị khóa trong 60 giây để tuân thủ thời gian API.")
                            } else {
                                _uiState.value = CameraUIState.Error(result.message)
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("FoodCameraVM", "Gallery error", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = CameraUIState.Error("Lỗi xử lý ảnh: ${e.message?.take(60)}")
                }
            }
        }
    }

    /**
     * Resize and compress bitmap to meet API constraints safely.
     * Target: Max dimension ~1024px, JPEG quality 85.
     */
    private suspend fun compressAndScaleBitmap(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val maxDim = 1024f
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()

        var scale = 1f
        if (width > maxDim || height > maxDim) {
            scale = if (width > height) maxDim / width else maxDim / height
        }

        val scaledWidth = Math.round(width * scale)
        val scaledHeight = Math.round(height * scale)

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        
        if (scaledBitmap != bitmap) scaledBitmap.recycle()

        BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    /**
     * Finalizes the meal session by saving all queued items to Firestore.
     */
    fun saveCompleteMealSession(items: List<com.example.finfit.health.ui.VisionFoodItem>, onComplete: () -> Unit) {
        if (items.isEmpty()) return

        viewModelScope.launch {
            if (!saveMutex.tryLock()) return@launch // Prevent multiple saves
            try {
                _saveStatus.value = "Đang lưu bữa ăn..."
                
                val totalCal = items.sumOf { it.result.estimatedCalories.toInt() }
                val totalProt = items.sumOf { it.result.macros.proteinG.toInt() }
                val totalCarb = items.sumOf { it.result.macros.carbsG.toInt() }
                val totalFat = items.sumOf { it.result.macros.fatG.toInt() }

                val mealHeader = com.example.finfit.health.model.FoodMealEntity(
                    mealName = this@FoodCameraViewModel.mealName,
                    totalCalories = totalCal,
                    totalProtein = totalProt,
                    totalCarbs = totalCarb,
                    totalFat = totalFat,
                    createdAt = System.currentTimeMillis(),
                    source = "gemini"
                )
                
                val mealItems = items.map { item ->
                    com.example.finfit.health.model.MealItemEntity(
                        itemName = item.result.dishName,
                        calories = item.result.estimatedCalories.toInt(),
                        protein = item.result.macros.proteinG.toInt(),
                        carbs = item.result.macros.carbsG.toInt(),
                        fat = item.result.macros.fatG.toInt(),
                        confidence = item.result.dishConfidence,
                        ingredients = item.result.ingredients.map { it.name },
                        imageUrl = item.imageUrl,
                        source = "gemini",
                        createdAt = System.currentTimeMillis()
                    )
                }
                
                val saveResult = mealRepository.saveMultiItemMealSession(mealHeader, mealItems)
                
                if (saveResult is VisionAiResult.Success) {
                    _saveStatus.value = "Đã lưu bữa ăn vào lịch sử"
                    onComplete()
                } else {
                    _saveStatus.value = "Lỗi lưu: ${(saveResult as VisionAiResult.Error).message}"
                }
            } catch (e: Exception) {
                Log.e("FoodCameraVM", "Error in saveCompleteMealSession", e)
                _saveStatus.value = "Lỗi hệ thống: ${e.message?.take(30)}"
            } finally {
                saveMutex.unlock()
            }
        }
    }

    fun dismissCard() {
        _uiState.value = CameraUIState.Idle
        _saveStatus.value = ""
    }

    fun dismissError() {
        _uiState.value = CameraUIState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        processingJob?.cancel()
        yoloDetector?.close()
        yoloDetector = null
        
        // Final attempt to clean up any UI bitmaps if the VM is cleared
        val state = _uiState.value
        if (state is CameraUIState.Success) {
            state.bitmap.recycle()
        }
    }
}
