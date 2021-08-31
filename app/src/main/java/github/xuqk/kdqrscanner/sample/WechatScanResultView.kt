package github.xuqk.kdqrscanner.sample

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.core.animation.doOnEnd
import github.xuqk.kdqrscanner.KDQrScanner
import github.xuqk.kdqrscanner.ScanResultView
import github.xuqk.kdqrscanner.dp

/**
 * Created By：XuQK
 * Created Date：2021/8/19 17:50
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

class WechatScanResultView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ScanResultView(context, attrs, defStyleAttr) {

    private var animatorFraction = 0f
    private var startRadius = 5.dp
    private var increaseRadius = 8.dp

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 300
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
        animator.cancel()
        animator.start()
    }

    override fun reset() {
        super.reset()
        animatorFraction = 0f
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (codeList.isEmpty() || animatorFraction == 0f) return

        canvas.drawARGB((animatorFraction * 0.4 * 255).toInt(), 0, 0, 0)

        codeList.forEach {
            paint.color = Color.WHITE
            paint.strokeWidth = 3.dp.toFloat()
            paint.style = Paint.Style.STROKE
            canvas.drawCircle(
                it.centerPoint.x.toFloat(),
                it.centerPoint.y.toFloat(),
                startRadius + 3.dp / 2f + increaseRadius * animatorFraction,
                paint
            )

            paint.color = Color.GREEN
            paint.alpha = 150
            paint.strokeWidth = 0f
            paint.style = Paint.Style.FILL
            canvas.drawCircle(
                it.centerPoint.x.toFloat(),
                it.centerPoint.y.toFloat(),
                startRadius + increaseRadius * animatorFraction,
                paint
            )
        }
    }


}