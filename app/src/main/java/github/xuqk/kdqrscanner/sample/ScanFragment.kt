package github.xuqk.kdqrscanner.sample

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import github.xuqk.kdqrscanner.KDQrScanner
import github.xuqk.kdqrscanner.sample.databinding.FragmentScanBinding

/**
 * Created By：XuQK
 * Created Date：2021/8/13 10:37
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

class ScanFragment : BaseFragment() {

    companion object {
        const val REQUEST_IMAGE_OPEN = 1

        const val TAG_TYPE = "tag_type"

        const val TYPE_FULL_SCREEN_NORMAL = 100
        const val TYPE_FULL_SCREEN_WX = 101
        const val TYPE_RECT = 102
    }

    private var _binding: FragmentScanBinding? = null
    private val binding: FragmentScanBinding get() = _binding!!

    private var pageType: Int = TYPE_FULL_SCREEN_NORMAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pageType = arguments?.getInt(TAG_TYPE, TYPE_FULL_SCREEN_NORMAL) ?: TYPE_FULL_SCREEN_NORMAL
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.scanner.run {
            when (pageType) {
                TYPE_FULL_SCREEN_NORMAL -> {
                    scanResultView = SimpleScanResultView(view.context)
                }
                TYPE_FULL_SCREEN_WX -> {
                    scanResultView = WechatScanResultView(view.context)
                }
                TYPE_RECT -> {
                    val w = resources.displayMetrics.widthPixels
                    setScanRect(
                        Rect(w / 4, w / 4, w / 4 * 3, w / 4 * 3),
                        0.5f,
                        Color.BLACK,
                        0f,
                        Color.TRANSPARENT,
                        0.8f,
                        Color.BLACK,
                        1f
                    )
                }
            }

            scanResultListener = object : KDQrScanner.ScanResultListener() {
                override fun onScanSuccess(fromCamera: Boolean, resultList: List<KDQrScanner.BarcodeEntity>) {
                    if (!fromCamera) {
                        if (resultList.isNotEmpty()) {
                            findNavController().navigate(R.id.ResultShowFragment, bundleOf("list" to resultList.map { it.value }))
                        } else {
                            binding.scanner.startScan()
                            Toast.makeText(context, "未识别到内容", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        if (pageType == TYPE_RECT) {
                            findNavController().navigate(R.id.ResultShowFragment, bundleOf("list" to resultList.map { it.value }))
                        }
                    }
                }

                override fun onClickBarcode(barcode: KDQrScanner.BarcodeEntity) {
                    findNavController().navigate(R.id.ResultShowFragment, bundleOf("list" to listOf(barcode.value)))
                }

                override fun onError(e: Throwable) {
                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                }
            }
            // 做完以上设置后，必须调用此方法进行初始化
            bind(viewLifecycleOwner)

            // 开始识别，也可通过按钮控制
            binding.scanner.startScan()
        }

        binding.btnStartScan.setOnClickListener {
            binding.scanner.startScan()
        }

        binding.btnStopScan.setOnClickListener {
            binding.scanner.stopScan()
        }

        binding.btnOpenGallery.setOnClickListener {
            selectImage()
        }

        binding.btnTorchOn.setOnClickListener {
            binding.scanner.torchOn()
        }

        binding.btnTorchOff.setOnClickListener {
            binding.scanner.torchOff()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, REQUEST_IMAGE_OPEN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_OPEN && resultCode == Activity.RESULT_OK) {
            val fullPhotoUri: Uri? = data?.data
            if (fullPhotoUri == null) {
                Toast.makeText(context, "未选中图片", Toast.LENGTH_SHORT).show()
            } else {
                binding.scanner.decode(fullPhotoUri)
            }
        }
    }
}