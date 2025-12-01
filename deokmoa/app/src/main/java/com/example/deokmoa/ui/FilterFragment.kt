package com.example.deokmoa.ui

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deokmoa.AddReviewActivity
import com.example.deokmoa.R
import com.example.deokmoa.ReviewAdapter
import com.example.deokmoa.ReviewDetailActivity
import com.example.deokmoa.data.AppDatabase
import com.example.deokmoa.data.CategoryEntity  // [변경] Category -> CategoryEntity
import com.example.deokmoa.data.Review
import com.example.deokmoa.data.Tag
import com.example.deokmoa.databinding.FragmentFilterBinding
import com.google.android.material.chip.Chip

class FilterFragment : Fragment() {

    private var _binding: FragmentFilterBinding? = null
    private val binding get() = _binding!!

    private lateinit var reviewAdapter: ReviewAdapter
    private val database by lazy { AppDatabase.getDatabase(requireContext()) }

    private var allReviews: List<Review> = emptyList()
    private var categoryList: List<CategoryEntity> = emptyList()  // [추가] DB 카테고리 저장용

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()
        setupUI()
        setupRecyclerView()

        // [추가] DB에서 카테고리 로드
        database.categoryDao().getAll().observe(viewLifecycleOwner) { categories ->
            categoryList = categories ?: emptyList()
            setupCategorySpinner()
        }

        // DB에서 전체 데이터를 로드 (초기 상태)
        database.reviewDao().getAll().observe(viewLifecycleOwner) { reviews ->
            allReviews = reviews ?: emptyList()
            updateList(allReviews)
        }
    }

    // [추가] 카테고리 스피너 설정 - DB에서 로드
    private fun setupCategorySpinner() {
        val categoryDisplayList = mutableListOf("전체 카테고리")
        categoryDisplayList.addAll(categoryList.map { it.name })

        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryDisplayList)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFilterCategory.adapter = categoryAdapter
    }

    private fun setupUI() {
        // [삭제됨] 1. 카테고리 스피너 - setupCategorySpinner()로 이동

        // 2. 별점 RatingBar 설정
        binding.rbFilterRating.setOnRatingBarChangeListener { _, rating, _ ->
            binding.tvRatingValue.text = "$rating 점 이상"
        }

        // 3. 태그 Chip 생성
        val chipGroup = binding.chipGroupFilter
        Tag.values().forEach { tag ->
            val chip = Chip(requireContext()).apply {
                text = tag.displayName
                this.tag = tag.name
                isCheckable = true
                isClickable = true
                isCheckedIconVisible = false

                // 스타일 적용
                setChipBackgroundColorResource(R.color.hashtag_background_selector)
                setTextColor(ContextCompat.getColor(context, R.color.black))
            }
            chipGroup.addView(chip)
        }

        // 4. 버튼 이벤트 연결
        binding.btnApplyFilter.setOnClickListener {
            applyFilter()
        }

        binding.btnResetFilter.setOnClickListener {
            resetFilters()
        }
    }

    private fun setupRecyclerView() {
        reviewAdapter = ReviewAdapter { review ->
            val intent = Intent(requireContext(), ReviewDetailActivity::class.java)
            intent.putExtra("REVIEW_ID", review.id)
            startActivity(intent)
        }
        binding.rvFilterResults.apply {
            adapter = reviewAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    // 필터 적용 로직
    private fun applyFilter() {
        // 1. 카테고리
        val selectedCategoryPos = binding.spinnerFilterCategory.selectedItemPosition
        // [변경] enum 대신 DB 카테고리 사용
        val selectedCategoryName = if (selectedCategoryPos == 0) {
            null  // 전체 카테고리
        } else {
            categoryList.getOrNull(selectedCategoryPos - 1)?.name
        }

        // 2. 별점
        val minRating = binding.rbFilterRating.rating

        // 3. 태그
        val selectedTags = mutableListOf<String>()
        val chipGroup = binding.chipGroupFilter
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            if (chip.isChecked) {
                selectedTags.add(chip.tag.toString())
            }
        }

        // 4. 필터링 실행
        val filteredList = allReviews.filter { review ->
            // [변경] 카테고리 조건 - null이면 전체
            val isCategoryMatch = (selectedCategoryName == null) || (review.category == selectedCategoryName)

            // 별점 조건
            val isRatingMatch = review.rating >= minRating

            // 태그 조건
            val isTagMatch = if (selectedTags.isEmpty()) {
                true
            } else {
                val reviewTags = review.tags.split(",")
                selectedTags.any { it in reviewTags }
            }

            isCategoryMatch && isRatingMatch && isTagMatch
        }

        updateList(filteredList)
    }

    // 초기화 로직
    private fun resetFilters() {
        // UI 초기화
        binding.spinnerFilterCategory.setSelection(0)
        binding.rbFilterRating.rating = 0f
        binding.chipGroupFilter.clearCheck()

        // 리스트를 전체 목록으로 복구
        updateList(allReviews)

        Toast.makeText(requireContext(), "검색 조건이 초기화되었습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun updateList(list: List<Review>) {
        reviewAdapter.submitList(list)

        if (list.isEmpty()) {
            binding.tvNoFilterResult.visibility = View.VISIBLE
            binding.rvFilterResults.visibility = View.GONE
        } else {
            binding.tvNoFilterResult.visibility = View.GONE
            binding.rvFilterResults.visibility = View.VISIBLE
        }
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.main_toolbar, menu)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}