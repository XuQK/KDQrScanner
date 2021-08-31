package github.xuqk.kdqrscanner

import android.content.res.Resources
import android.util.TypedValue

/**
 * Created By：XuQK
 * Created Date：2021/8/20 11:33
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

var Int.dp: Int
    inline get() = toFloat().dp
    private set(_){}

var Float.dp: Int
    inline get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics).toInt()
    private set(_){}
