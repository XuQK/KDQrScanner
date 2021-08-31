package github.xuqk.kdqrscanner

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.graphics.Point
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat
import kotlin.math.sqrt

/**
 * Created By：XuQK
 * Created Date：2021/8/20 13:32
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

abstract class ScanResultView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    protected open val touchCircleRegion: Boolean = true
    protected open val touchCircleRadius = 10.dp

    protected val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gestureDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            codeList.forEach {
                if (estimateClickSuccess(e, it)) {
                    onClickBarcode?.invoke(it)
                    return true
                }
            }
            return false
        }
    })

    internal var onClickBarcode: ((barcode: KDQrScanner.BarcodeEntity) -> Unit)? = null

    var codeList: List<KDQrScanner.BarcodeEntity> = emptyList()
        private set

    open fun showResult(barcodes: List<KDQrScanner.BarcodeEntity>) {
        codeList = barcodes
    }

    open fun reset() {
        codeList = emptyList()
    }

    open fun estimateClickSuccess(e: MotionEvent, barcode: KDQrScanner.BarcodeEntity): Boolean {
        return if (touchCircleRegion) {
            (sqrt(((e.x - barcode.centerPoint.x) * (e.x - barcode.centerPoint.x) + (e.y - barcode.centerPoint.y) * (e.y - barcode.centerPoint.y)).toDouble()) < touchCircleRadius)
        } else {
            isPointInRect(e.x.toInt(), e.y.toInt(), barcode.cornerPoints)
        }
    }

    private fun isPointInRect(x: Int, y: Int, ps: List<Point>): Boolean {
        val pa: Point = ps[0]
        val pb: Point = ps[1]
        val pc: Point = ps[2]
        val pd: Point = ps[3]
        val a: Int = (pb.x - pa.x) * (y - pa.y) - (pb.y - pa.y) * (x - pa.x)
        val b: Int = (pc.x - pb.x) * (y - pb.y) - (pc.y - pb.y) * (x - pb.x)
        val c: Int = (pd.x - pc.x) * (y - pc.y) - (pd.y - pc.y) * (x - pc.x)
        val d: Int = (pa.x - pd.x) * (y - pd.y) - (pa.y - pd.y) * (x - pd.x)
        return a >= 0 && b >= 0 && c >= 0 && d >= 0 || a <= 0 && b <= 0 && c <= 0 && d <= 0
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true
    }
}