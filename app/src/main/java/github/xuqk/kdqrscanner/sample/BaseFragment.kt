package github.xuqk.kdqrscanner.sample

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

/**
 * Created By：XuQK
 * Created Date：2021/8/16 11:04
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

abstract class BaseFragment : Fragment() {
    protected val logTag: String
        get() = javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(logTag, "life:onCreate")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(logTag, "life:onAttach")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(logTag, "life:onCreateView")
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(logTag, "life:onViewCreated")
    }

    override fun onStart() {
        super.onStart()
        Log.d(logTag, "life:onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(logTag, "life:onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(logTag, "life:onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(logTag, "life:onStop")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(logTag, "life:onDestroyView")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(logTag, "life:onDestroy")
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(logTag, "life:onDetach")
    }
}