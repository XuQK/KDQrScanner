package github.xuqk.kdqrscanner.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import github.xuqk.kdqrscanner.sample.databinding.FragmentMainBinding

/**
 * Created By：XuQK
 * Created Date：2021/8/13 10:37
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

class MainFragment : BaseFragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding: FragmentMainBinding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnOpenFullScanner.setOnClickListener {
            findNavController().navigate(R.id.FullScanFragment, bundleOf(ScanFragment.TAG_TYPE to ScanFragment.TYPE_FULL_SCREEN_NORMAL))
        }

        binding.btnOpenFullScannerWx.setOnClickListener {
            findNavController().navigate(R.id.FullScanFragment, bundleOf(ScanFragment.TAG_TYPE to ScanFragment.TYPE_FULL_SCREEN_WX))
        }

        binding.btnOpenRectScanner.setOnClickListener {
            findNavController().navigate(R.id.FullScanFragment, bundleOf(ScanFragment.TAG_TYPE to ScanFragment.TYPE_RECT))
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}