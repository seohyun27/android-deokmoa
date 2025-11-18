package com.example.deokmoa.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.example.deokmoa.AddReviewActivity
import com.example.deokmoa.R
import com.example.deokmoa.databinding.FragmentRankingBinding
import com.example.deokmoa.ui.filterpage.CategoryFragment
import com.example.deokmoa.ui.filterpage.RankingContentFragment
import com.example.deokmoa.ui.filterpage.TagFragment

class RankingFragment : Fragment() {

    private var _binding: FragmentRankingBinding? = null
    private val binding get() = _binding!!
    private enum class TabType { CATEGORY, RATING , TAG  }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRankingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()
        selectTab(TabType.CATEGORY)

        binding.tabCategory.setOnClickListener {
            selectTab(TabType.CATEGORY)
        }
        binding.tabRating.setOnClickListener {
            selectTab(TabType.RATING)
        }
        binding.tabTag.setOnClickListener {
            selectTab(TabType.TAG)
        }

    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.main_toolbar, menu)
                // 랭킹 화면에서는 글쓰기만 보이게, 검색은 숨김
                menu.findItem(R.id.action_add)?.isVisible = true
                menu.findItem(R.id.action_search)?.isVisible = false
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_add -> {
                        val intent = Intent(requireContext(), AddReviewActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
    // 버튼 클릭 설정
    private fun selectTab(type: TabType) {
        updateTabUi(type)

        val fragment: Fragment = when (type) {
            TabType.CATEGORY -> CategoryFragment()
            TabType.RATING   -> RankingContentFragment()
            TabType.TAG      -> TagFragment() }

        childFragmentManager.beginTransaction()
            .replace(R.id.rankingContent, fragment)
            .commit()
    }
    //선택된 탭만 배경 흰색 처리 및 글자 볼드
    private fun updateTabUi(type: TabType) {
        fun TextView.setSelectedStyle(selected: Boolean) {
            if (selected) {
                setBackgroundColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD)
            } else {
                setBackgroundColor(Color.TRANSPARENT)
                setTypeface(typeface, Typeface.NORMAL)
            }
        }

        binding.tabCategory.setSelectedStyle(type == TabType.CATEGORY)
        binding.tabRating.setSelectedStyle(type == TabType.RATING)
        binding.tabTag.setSelectedStyle(type == TabType.TAG)

    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
