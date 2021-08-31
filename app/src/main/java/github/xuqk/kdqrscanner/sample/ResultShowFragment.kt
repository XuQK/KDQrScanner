package github.xuqk.kdqrscanner.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import github.xuqk.kdqrscanner.sample.databinding.FragmentResultShowBinding

/**
 * Created By：XuQK
 * Created Date：2021/8/16 11:03
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

class ResultShowFragment : BaseFragment() {

    private var _binding: FragmentResultShowBinding? = null
    private val binding: FragmentResultShowBinding get() = _binding!!

    private var list = listOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        list = arguments?.get("list") as List<String>
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultShowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tv.text = list.joinToString("\n")
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}