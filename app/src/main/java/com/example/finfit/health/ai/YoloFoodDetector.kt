package com.example.finfit.health.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val TAG = "YoloDetector"

data class DetectionResult(
    val label: String,
    val confidence: Float,
    val boxAreaRatio: Float,
    val boundingBox: RectF
)

class YoloFoodDetector(context: Context) {
    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()

    private val inputSize = 640
    private val confidenceThreshold = 0.25f
    private val iouThreshold = 0.45f

    // Model info (populated during init)
    var modelInputShape: IntArray = intArrayOf()
        private set
    var modelOutputShapes: List<IntArray> = emptyList()
        private set
    var outputCount: Int = 0
        private set

    init {
        try {
            val modelBuffer = FileUtil.loadMappedFile(context, "best_float32.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(modelBuffer, options)
            labels = FileUtil.loadLabels(context, "labels.txt")

            // Log input tensor info
            val inputTensor = interpreter!!.getInputTensor(0)
            modelInputShape = inputTensor.shape()
            Log.i(TAG, "=== MODEL INFO ===")
            Log.i(TAG, "Input shape: ${modelInputShape.contentToString()}")
            Log.i(TAG, "Input type: ${inputTensor.dataType()}")

            // Log ALL output tensors
            outputCount = interpreter!!.outputTensorCount
            Log.i(TAG, "Output tensor count: $outputCount")
            val shapes = mutableListOf<IntArray>()
            for (i in 0 until outputCount) {
                val outTensor = interpreter!!.getOutputTensor(i)
                val shape = outTensor.shape()
                shapes.add(shape)
                Log.i(TAG, "Output[$i] shape: ${shape.contentToString()}, type: ${outTensor.dataType()}")
            }
            modelOutputShapes = shapes
            Log.i(TAG, "Labels count: ${labels.size}")
            Log.i(TAG, "Labels: ${labels.joinToString(", ")}")
            Log.i(TAG, "=== END MODEL INFO ===")
        } catch (e: Exception) {
            Log.e(TAG, "Init FAILED", e)
        }
    }

    /**
     * Preprocess bitmap to float32 [1, 640, 640, 3] RGB.
     * Implements YOLO's "letterbox" resize: maintains aspect ratio and pads with 114.
     * normScale controls normalization (use 255.0f for standard YOLO norm).
     * swapToBgr = true will output BGR instead of RGB.
     */
    private fun preprocessBitmap(bitmap: Bitmap, normScale: Float = 255.0f, swapToBgr: Boolean = false): ByteBuffer {
        val scale = minOf(inputSize.toFloat() / bitmap.width, inputSize.toFloat() / bitmap.height)
        val scaledWidth = Math.round(bitmap.width * scale)
        val scaledHeight = Math.round(bitmap.height * scale)

        // Calculate padding to center the image
        val padX = (inputSize - scaledWidth) / 2
        val padY = (inputSize - scaledHeight) / 2

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        
        // Create a padded bitmap filled with gray (114)
        val paddedBitmap = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(paddedBitmap)
        canvas.drawColor(android.graphics.Color.rgb(114, 114, 114)) // YOLO gray padding
        canvas.drawBitmap(scaledBitmap, padX.toFloat(), padY.toFloat(), null)

        val buffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        paddedBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            // Android Bitmap is ARGB
            val r = ((pixel shr 16) and 0xFF) / normScale
            val g = ((pixel shr 8) and 0xFF) / normScale
            val b = (pixel and 0xFF) / normScale
            
            if (swapToBgr) {
                buffer.putFloat(b)
                buffer.putFloat(g)
                buffer.putFloat(r)
            } else {
                buffer.putFloat(r)
                buffer.putFloat(g)
                buffer.putFloat(b)
            }
        }
        buffer.rewind()

        if (scaledBitmap != bitmap) scaledBitmap.recycle()
        paddedBitmap.recycle()
        return buffer
    }

    /**
     * Run full inference and return detection results.
     * Handles YOLO segmentation model with multiple output tensors.
     */
    fun detect(bitmap: Bitmap): List<DetectionResult> {
        val interp = interpreter ?: return emptyList()
        if (modelOutputShapes.isEmpty()) return emptyList()

        val startTime = System.currentTimeMillis()

        // 1. Preprocess
        val inputBuffer = preprocessBitmap(bitmap)
        Log.d(TAG, "Preprocess done: ${System.currentTimeMillis() - startTime}ms")

        // 2. Allocate output buffers for ALL output tensors
        val outputBuffers = mutableMapOf<Int, Any>()
        for (i in 0 until outputCount) {
            val shape = modelOutputShapes[i]
            val totalElements = shape.fold(1) { acc, v -> acc * v }
            val buf = ByteBuffer.allocateDirect(totalElements * 4).order(ByteOrder.nativeOrder())
            outputBuffers[i] = buf
        }

        // 3. Run inference
        // We dynamically allocate all required outputs to prevent crashes with placeholder models,
        // but this architecture exclusively processes output[0] (Detection Tensor).
        val inputs = arrayOf<Any>(inputBuffer)
        interp.runForMultipleInputsOutputs(inputs, outputBuffers)

        val inferenceTime = System.currentTimeMillis() - startTime
        Log.d(TAG, "Inference latency: ${inferenceTime}ms")

        // 4. Parse detection output (output[0] is always the detection tensor)
        val detOutput = outputBuffers[0] as ByteBuffer
        detOutput.rewind()
        val detShape = modelOutputShapes[0]

        // Convert to float array
        val totalDetElements = detShape.fold(1) { acc, v -> acc * v }
        val detFloats = FloatArray(totalDetElements)
        detOutput.asFloatBuffer().get(detFloats)

        // 5. Parse boxes
        val results = parseDetectionOutput(detFloats, detShape)

        return results
    }

    /**
     * Parse the raw detection output tensor.
     * YOLO-seg detection output is [1, 4+numClasses+32, numAnchors] (transposed)
     * or [1, numAnchors, 4+numClasses+32]
     * The 32 extra values are mask coefficients (ignored for detection-only).
     */
    private fun parseDetectionOutput(data: FloatArray, shape: IntArray): List<DetectionResult> {
        if (shape.size != 3) {
            Log.e(TAG, "Unexpected detection shape: ${shape.contentToString()}")
            return emptyList()
        }

        val dim1 = shape[1]
        val dim2 = shape[2]

        // Determine orientation: smaller dim = features (4+classes+32), larger = anchors
        val isTransposed: Boolean  // [1, features, anchors] format
        val numAnchors: Int
        val numFeatures: Int

        if (dim1 < dim2) {
            // [1, features, anchors] - typical YOLO export
            isTransposed = true
            numFeatures = dim1
            numAnchors = dim2
        } else {
            // [1, anchors, features]
            isTransposed = false
            numAnchors = dim1
            numFeatures = dim2
        }

        // Architecture Refactor: Detection-Only Pipeline
        // The detector now assumes a strict detection layout: 4 bbox + N classes.
        // If a segmentation model is temporarily used, mask coefficients are natively ignored.
        val numClasses = minOf(labels.size, numFeatures - 4)

        if (numClasses <= 0) {
            Log.e(TAG, "Invalid numClasses=$numClasses")
            return emptyList()
        }

        val detections = mutableListOf<BoundingBox>()

        for (i in 0 until numAnchors) {
            // Find best class
            var maxConf = -1f
            var maxClassId = -1

            for (c in 0 until numClasses) {
                val conf = if (isTransposed) {
                    data[(4 + c) * numAnchors + i]
                } else {
                    data[i * numFeatures + (4 + c)]
                }
                if (conf > maxConf) {
                    maxConf = conf
                    maxClassId = c
                }
            }

            if (maxConf > confidenceThreshold && maxClassId in labels.indices) {
                var cx: Float
                var cy: Float
                var w: Float
                var h: Float

                if (isTransposed) {
                    cx = data[0 * numAnchors + i]
                    cy = data[1 * numAnchors + i]
                    w  = data[2 * numAnchors + i]
                    h  = data[3 * numAnchors + i]
                } else {
                    cx = data[i * numFeatures + 0]
                    cy = data[i * numFeatures + 1]
                    w  = data[i * numFeatures + 2]
                    h  = data[i * numFeatures + 3]
                }

                // If coordinates are normalized (0.0 to 1.0), scale them back to model input size (e.g., 640x640)
                if (w <= 1.0f && h <= 1.0f) {
                    cx *= inputSize
                    cy *= inputSize
                    w  *= inputSize
                    h  *= inputSize
                }

                val x1 = cx - w / 2f
                val y1 = cy - h / 2f
                val x2 = cx + w / 2f
                val y2 = cy + h / 2f

                detections.add(BoundingBox(x1, y1, x2, y2, maxConf, maxClassId))
            }
        }

        // Apply NMS
        val nmsResults = nonMaxSuppression(detections, iouThreshold)

        return nmsResults.map { box ->
            val bw = box.x2 - box.x1
            val bh = box.y2 - box.y1
            val boxArea = bw * bh
            val totalArea = (inputSize * inputSize).toFloat()

            DetectionResult(
                label = labels[box.classId],
                confidence = box.score,
                boxAreaRatio = boxArea / totalArea,
                boundingBox = RectF(box.x1, box.y1, box.x2, box.y2)
            )
        }
    }

    private fun nonMaxSuppression(boxes: List<BoundingBox>, iouThresh: Float): List<BoundingBox> {
        if (boxes.isEmpty()) return emptyList()
        val sorted = boxes.sortedByDescending { it.score }.toMutableList()
        val selected = mutableListOf<BoundingBox>()
        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            selected.add(best)
            sorted.removeAll { best.classId == it.classId && calculateIoU(best, it) >= iouThresh }
        }
        return selected
    }

    private fun calculateIoU(a: BoundingBox, b: BoundingBox): Float {
        val xA = maxOf(a.x1, b.x1); val yA = maxOf(a.y1, b.y1)
        val xB = minOf(a.x2, b.x2); val yB = minOf(a.y2, b.y2)
        val inter = maxOf(0f, xB - xA) * maxOf(0f, yB - yA)
        val aArea = (a.x2 - a.x1) * (a.y2 - a.y1)
        val bArea = (b.x2 - b.x1) * (b.y2 - b.y1)
        return inter / (aArea + bArea - inter)
    }

    fun close() {
        interpreter?.close()
    }
}

data class BoundingBox(
    val x1: Float, val y1: Float,
    val x2: Float, val y2: Float,
    val score: Float, val classId: Int
)
