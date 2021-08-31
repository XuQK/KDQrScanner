package github.xuqk.kdqrscanner.sample

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.util.AttributeSet
import androidx.core.animation.doOnEnd
import github.xuqk.kdqrscanner.KDQrScanner
import github.xuqk.kdqrscanner.ScanResultView

/**
 * Created By：XuQK
 * Created Date：2021/8/19 17:50
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

class SimpleScanResultView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ScanResultView(context, attrs, defStyleAttr) {

    override val touchCircleRegion: Boolean = false

    private var pathList: List<Path> = emptyList()
    private val pathMeasure = PathMeasure()
    private var animatorFraction = 0f
    private var color = Color.RED

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 600
        addUpdateListener {
            animatorFraction = it.animatedFraction
            postInvalidateOnAnimation()
        }
        doOnEnd {
            animatorFraction = 1f
            postInvalidateOnAnimation()
        }
    }

    override fun showResult(barcodes: List<KDQrScanner.BarcodeEntity>) {
        super.showResult(barcodes)
        pathList = List(codeList.size) { Path() }
        animator.cancel()
        animator.start()
    }

    override fun reset() {
        super.reset()
        pathList = emptyList()
        animator.cancel()
        animatorFraction = 0f
        postInvalidateOnAnimation()
    }

    private val pathTemp = Path()
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (codeList.isEmpty()) return

        canvas.drawARGB((animatorFraction * 0.4 * 255).toInt(), 0, 0, 0)

        codeList.forEachIndexed { i, barcodeEntity ->
            val path = pathList[i]
            barcodeEntity.cornerPoints.forEachIndexed { index, point ->
                if (index == 0) {
                    path.moveTo(point.x.toFloat(), point.y.toFloat())
                } else {
                    path.lineTo(point.x.toFloat(), point.y.toFloat())
                }
            }
            path.close()
        }

        paint.color = color
        paint.strokeWidth = 10f
        paint.style = Paint.Style.STROKE
        pathList.forEach { path ->
            pathTemp.reset()
            pathMeasure.setPath(path, false)
            val length = pathMeasure.length * animatorFraction
            pathMeasure.getSegment(0f, length, pathTemp, true)

            canvas.drawPath(pathTemp, paint)
        }
    }
}