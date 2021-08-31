package github.xuqk.kdqrscanner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.util.AttributeSet
import android.util.Size
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.ColorLong
import androidx.annotation.Dimension
import androidx.annotation.FloatRange
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max

/**
 * Created By：XuQK
 * Created Date：2021/8/11 11:07
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

class KDQrScanner @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs), LifecycleEventObserver {

    // 设备是否相对静止状态，此状态下才开始记录识别结果
    var deviceStatic: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (field) {
                    reset()
                }
            }
        }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private var lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    private val accelerationSensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            deviceStatic = !event.values.any { abs(it) > 0.5 }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

        }
    }

    private val lightSensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            scanResultListener?.lightChanged(event.values[0].toInt())
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

        }
    }

    private val viewFinder: PreviewView = PreviewView(context)
    private val imageView: ImageView = ImageView(context)

    private var lifecycleOwner: LifecycleOwner? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private val cameraExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private var barDecoder: BarcodeScanner? = null
    private var initing: Boolean = false

    private val preview: Preview by lazy {
        Preview.Builder()
            .setTargetResolution(Size(width, height))
            .build()
    }

    private val imageAnalysis: ImageAnalysis by lazy {
        ImageAnalysis.Builder()
            .setTargetResolution(Size(width, height))
            .build()
    }
    private val qrCodeAnalyzer = QrCodeAnalyzer()

    private var barcodeBoxingOffsetX: Float = 0f
    private var barcodeBoxingOffsetY: Float = 0f
    private var barcodeBoxingScale: Float = 0f

    var barcodeFormats: IntArray = intArrayOf()
        set(value) {
            field = value

            val optionsBuilder = BarcodeScannerOptions.Builder()
            if (field.isNotEmpty()) {
                val mainFormat = field[0]
                val subFormats = field.drop(1)
                optionsBuilder.setBarcodeFormats(mainFormat, *subFormats.toIntArray())
            }

            barDecoder = BarcodeScanning.getClient(optionsBuilder.build())
        }
    private val barcodeMapTemp: MutableMap<String, BarcodeEntity> = mutableMapOf()
    private val barcodeMap: MutableMap<String, BarcodeEntity> = mutableMapOf()
    private var currentAnalyseTimes: Int = 0
    private var analyseStartTime: Long = 0


    /**
     * 是否在设备移动的时候进行识别，默认为 否
     */
    var scanWhenDeviceMove: Boolean = false

    var scanResultListener: ScanResultListener? = null

    var scanResultView: ScanResultView? = null

    /**
     * 开始识别后，产生最终结果前需要达到的连续识别持续时间，本参数只有在[totalAnalyseTimes]为0的时候才生效。
     */
    var analyseDuration: Int = 0

    /**
     * 开始识别后，产生最终结果前需要达到的连续识别次数，默认4次。
     */
    var totalAnalyseTimes: Int = 4

    /**
     * 识别范围矩形，超出范围的不计入识别结果。
     */
    private var scanRect: Rect? = null

    /**
     * 识别范围以外的颜色
     */
    private var scanRectOuterColor: Int = Color.TRANSPARENT

    /**
     * 识别范围内的颜色
     */
    private var scanRectCenterColor: Int = Color.TRANSPARENT

    /**
     * 识别范围分界线颜色
     */
    private var scanRectDividerColor: Int = Color.TRANSPARENT

    /**
     * 识别范围分界线宽度
     */
    private var scanRectDividerWidth: Int = 0

    init {
        setWillNotDraw(false)
        addView(viewFinder, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        addView(imageView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    fun setScanRect(
        rect: Rect,
        @FloatRange(from = 0.0, to = 1.0) outerAlpha: Float,
        @ColorLong outerColor: Int,
        @FloatRange(from = 0.0, to = 1.0) centerAlpha: Float,
        @ColorLong centerColor: Int,
        @FloatRange(from = 0.0, to = 1.0) dividerAlpha: Float,
        @ColorLong dividerColor: Int,
        @Dimension(unit = Dimension.DP) dividerWidth: Float,
    ) {
        scanRect = rect
        if (outerAlpha > 0f && outerColor != 0) {
            scanRectOuterColor = Color.argb(
                (255 * outerAlpha).toInt(),
                Color.red(outerColor),
                Color.green(outerColor),
                Color.blue(outerColor)
            )
        }
        if (centerAlpha > 0f && centerColor != 0) {
            scanRectCenterColor = Color.argb(
                (255 * centerAlpha).toInt(),
                Color.red(centerColor),
                Color.green(centerColor),
                Color.blue(centerColor)
            )
        }
        if (dividerAlpha > 0f && dividerColor != 0) {
            scanRectDividerColor = Color.argb(
                (255 * dividerAlpha).toInt(),
                Color.red(dividerColor),
                Color.green(dividerColor),
                Color.blue(dividerColor)
            )
            scanRectDividerWidth = dividerWidth.dp
        }

        postInvalidateOnAnimation()
    }

    /**
     * 设置完后，必须调用此方法进行初始化
     * @param lifecycleOwner
     */
    fun bind(lifecycleOwner: LifecycleOwner) {
        this.lifecycleOwner = lifecycleOwner
        lifecycleOwner.lifecycle.removeObserver(this)
        lifecycleOwner.lifecycle.addObserver(this)

        scanResultView?.let { scanResultView ->
            addView(scanResultView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            scanResultView.onClickBarcode = { barcode -> scanResultListener?.onClickBarcode(barcode) }
        }
    }

    private fun reset() {
        barcodeMap.clear()
        currentAnalyseTimes = 0
    }

    /**
     * 开始分析图像
     * @param formatList 支持的格式，不指定即为全部支持 [Barcode.BarcodeFormat]
     */
    fun startScan(@Barcode.BarcodeFormat vararg formatList: Int) {
        if (initing) return
        imageView.setImageBitmap(null)
        scanResultView?.reset()

        barcodeFormats = formatList

        if (cameraProvider == null) {
            initing = true
            viewFinder.post {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

                cameraProviderFuture.addListener({
                    try {
                        cameraProvider = cameraProviderFuture.get()
                        initing = false

                        lensFacing = when {
                            hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                            hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                            else -> throw IllegalStateException("Back and front camera are unavailable")
                        }

                        bindCameraPreview()
                        bindCameraAnalyzer()

                    } catch (e: Exception) {
                        e.printStackTrace()
                        scanResultListener?.onError(e)
                        initing = false
                    }
                }, mainExecutor)
            }
        } else {
            bindCameraAnalyzer()
        }

        registerSensorListener()
    }

    fun stopScan() {
        if (cameraProvider?.isBound(imageAnalysis) != true) return
        unregisterSensorListener()
        cameraProvider?.unbindAll()

        imageView.setImageBitmap(viewFinder.bitmap)
        imageView.post {
            bindCameraPreview()
        }
    }

    private fun scanSuccess() {
        val barcodes = barcodeMap.values.toList()
        scanResultView?.showResult(barcodes)
        stopScan()
        scanResultListener?.onScanSuccess(true, barcodes)
    }

    private fun registerSensorListener() {
        unregisterSensorListener()

        accelerationSensor?.let {
            sensorManager.registerListener(accelerationSensorListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        lightSensor?.let {
            sensorManager.registerListener(lightSensorListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun unregisterSensorListener() {
        sensorManager.unregisterListener(accelerationSensorListener)
        sensorManager.unregisterListener(lightSensorListener)
    }

    fun torchOn() {
        camera?.cameraControl?.enableTorch(true)
    }

    fun torchOff() {
        camera?.cameraControl?.enableTorch(false)
    }

    private fun bindCameraPreview() {
        viewFinder.post {
            if (lifecycleOwner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != true) return@post
            try {
                val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera provider is not initialize")
                cameraProvider.unbindAll()
                val lifecycleOwner = lifecycleOwner ?: throw IllegalStateException("can not find lifecycleOwner")
                val rotation = viewFinder.display.rotation

                val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

                @androidx.camera.core.ExperimentalUseCaseGroup
                preview.targetRotation = rotation
                preview.setSurfaceProvider(viewFinder.surfaceProvider)

                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview)
            } catch (e: Exception) {
                e.printStackTrace()
                scanResultListener?.onError(e)
            }
        }
    }

    private fun bindCameraAnalyzer() {
        viewFinder.post {
            if (lifecycleOwner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != true) return@post
            try {
                val cameraProvider = cameraProvider
                if (cameraProvider == null) {
                    bindCameraAnalyzer()
                    return@post
                }
                cameraProvider.unbind(imageAnalysis)

                val lifecycleOwner = lifecycleOwner ?: throw IllegalStateException("can not find lifecycleOwner")
                val rotation = viewFinder.display.rotation

                val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

                imageAnalysis.targetRotation = rotation
                imageAnalysis.setAnalyzer(cameraExecutor, qrCodeAnalyzer)

                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, imageAnalysis)
            } catch (e: Exception) {
                e.printStackTrace()
                scanResultListener?.onError(e)
            }
        }
    }

    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    override fun onDrawForeground(canvas: Canvas) {
        super.onDrawForeground(canvas)
        val scanRect = scanRect ?: return

        // 绘制 outer
        if (scanRectOuterColor != Color.TRANSPARENT) {
            canvas.drawColor(scanRectOuterColor)
        }

        // 镂空 center
        paint.style = Paint.Style.FILL
        paint.xfermode = xfermode
        canvas.drawRect(scanRect, paint)
        paint.xfermode = null

        // 绘制 divider
        if (scanRectDividerColor != Color.TRANSPARENT && scanRectDividerWidth > 0) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = scanRectDividerWidth.toFloat()
            paint.color = scanRectDividerColor
            canvas.drawRect(scanRect, paint)
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                if (cameraProvider?.isBound(imageAnalysis) == true) {
                    registerSensorListener()
                }
            }
            Lifecycle.Event.ON_PAUSE -> unregisterSensorListener()
            Lifecycle.Event.ON_DESTROY -> cameraExecutor.shutdown()
            else -> {}
        }
    }

    private fun putNewBarcode(list: List<Barcode>) {
        barcodeMapTemp.clear()
        val scanRect = scanRect
        list.forEach { barcode ->
            val value = barcode.rawValue ?: return
            val cornerPoints = barcode.cornerPoints ?: return
            val boundingBox = barcode.boundingBox ?: return
            val format = barcode.format
            if (cornerPoints.isEmpty()) return

            val newCornerPoints = List(cornerPoints.size) { index ->
                Point(
                    (cornerPoints[index].x * barcodeBoxingScale + barcodeBoxingOffsetX).toInt(),
                    (cornerPoints[index].y * barcodeBoxingScale + barcodeBoxingOffsetY).toInt(),
                )
            }

            if (scanRect == null || newCornerPoints.all { scanRect.contains(it.x, it.y) }) {
                barcodeMapTemp[value + format] = BarcodeEntity(
                    format,
                    value,
                    boundingBox,
                    newCornerPoints,
                    Point(
                        newCornerPoints.sumOf { p -> p.x } / newCornerPoints.size,
                        newCornerPoints.sumOf { p -> p.y } / newCornerPoints.size
                    )
                )
            }
        }

        if (barcodeMapTemp.isEmpty()) {
            return
        } else {
            barcodeMap.putAll(barcodeMapTemp)
        }

        if (currentAnalyseTimes == 0) {
            analyseStartTime = System.currentTimeMillis()
        }
        currentAnalyseTimes++
        if (totalAnalyseTimes == 0 && analyseDuration > 0) {
            if (System.currentTimeMillis() - analyseStartTime >= analyseDuration) {
                if (barcodeMap.isNotEmpty()) {
                    scanSuccess()
                    postInvalidate()
                }
            }
        } else {
            if (currentAnalyseTimes >= totalAnalyseTimes) {
                scanSuccess()
                postInvalidate()
            }
        }
    }

    fun decode(uri: Uri, @Barcode.BarcodeFormat vararg formatList: Int) {
        barcodeFormats = formatList
        barDecoder?.let { decoder ->
            stopScan()

            val inputImage = InputImage.fromFilePath(context, uri)
            decoder.process(inputImage)
                .addOnSuccessListener { barcodeList ->
                    scanResultListener?.onScanSuccess(false, barcodeList.map {
                        BarcodeEntity(
                            it.format,
                            it.rawValue.orEmpty(),
                            it.boundingBox ?: Rect(),
                            it.cornerPoints.orEmpty().toList(),
                            Point()
                        )
                    })
                }.addOnFailureListener {
                    it.printStackTrace()
                    scanResultListener?.onError(it)
                }.addOnCompleteListener {
                }
        }
    }

    private inner class QrCodeAnalyzer : ImageAnalysis.Analyzer {
        private val sourceRect = RectF()
        private val destinationRect = RectF()

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val decoder = barDecoder
            if (!deviceStatic || decoder == null) {
                imageProxy.close()
                return
            }
            val image = imageProxy.image ?: return
            val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)

            decoder.process(inputImage)
                .addOnSuccessListener { barcodeList ->
                    if (barcodeList.size > 0) {
                        setBoxingBoundModifierParam(imageProxy, viewFinder)
                        putNewBarcode(barcodeList)
                    }
                }.addOnFailureListener {
                    it.printStackTrace()
                }.addOnCompleteListener {
                    imageProxy.close()
                }
        }

        /**
         * 坐标转换
         */
        fun setBoxingBoundModifierParam(imageProxy: ImageProxy, previewView: PreviewView) {
            val cropRect = imageProxy.cropRect
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            destinationRect.set(0f, 0f, previewView.width.toFloat(), previewView.height.toFloat())
            if (rotationDegrees % 180 == 0) {
                barcodeBoxingScale = max(previewView.height.toFloat() / cropRect.height().toFloat(), previewView.width.toFloat() / cropRect.width().toFloat())
                sourceRect.set(0f, 0f, cropRect.width().toFloat() * barcodeBoxingScale, cropRect.height().toFloat() * barcodeBoxingScale)

            } else {
                barcodeBoxingScale = max(previewView.height.toFloat() / cropRect.width().toFloat(), previewView.width.toFloat() / cropRect.height().toFloat())
                sourceRect.set(0f, 0f, cropRect.height().toFloat() * barcodeBoxingScale, cropRect.width().toFloat() * barcodeBoxingScale)
            }

            barcodeBoxingOffsetX = (destinationRect.centerX() - sourceRect.centerX())
            barcodeBoxingOffsetY = (destinationRect.centerY() - sourceRect.centerY())
        }
    }

    data class BarcodeEntity(
        /**
         * [Barcode.BarcodeFormat]
         */
        val format: Int = 0,
        val value: String = "",
        val boundingBox: Rect = Rect(),
        val cornerPoints: List<Point> = listOf(),
        val centerPoint: Point = Point(),
    )

    open class ScanResultListener {

        /**
         * @param fromCamera true 表示是从摄像头识别，false 从给到的图片识别
         * @param resultList 识别结果
         */
        open fun onScanSuccess(fromCamera: Boolean, resultList: List<BarcodeEntity>) {}

        /**
         * @param barcode 点击选中的码
         */
        open fun onClickBarcode(barcode: BarcodeEntity) {}

        open fun onError(e: Throwable) {}

        /**
         * 环境亮度
         */
        open fun lightChanged(lux: Int) {}
    }
}
