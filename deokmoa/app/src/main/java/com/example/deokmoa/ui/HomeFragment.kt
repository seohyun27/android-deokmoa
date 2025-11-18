package com.example.deokmoa.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deokmoa.R
import com.example.deokmoa.AddReviewActivity
import com.example.deokmoa.ReviewAdapter
import com.example.deokmoa.ReviewDetailActivity
import com.example.deokmoa.data.AppDatabase
import com.example.deokmoa.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var _bind: FragmentHomeBinding? = null
    private val binding get() = _bind!!
    private lateinit var reviewAdapter: ReviewAdapter
    private val database by lazy { AppDatabase.getDatabase(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // fragment ViewBinding 설정
        _bind = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
        setupRecyclerView()

        // 데이터베이스의 모든 리뷰를 관찰 (Observe)
        database.reviewDao().getAll().observe(viewLifecycleOwner) { reviews ->
            // .toList()를 호출하여 Java List를 Kotlin List로 변환
            reviewAdapter.submitList(reviews?.toList())
        }
    }

    private fun setupRecyclerView() {
        // ReviewAdapter 초기화 (아이템 클릭 시 상세 화면으로 이동)
        reviewAdapter = ReviewAdapter { review ->
            val intent = Intent(requireContext(), ReviewDetailActivity::class.java)
            intent.putExtra("REVIEW_ID", review.id)
            startActivity(intent)
        }
        // RecyclerView에 Adapter와 LayoutManager 설정
        binding.rvHomeReviews.apply {
            adapter = reviewAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_toolbar, menu)
                // 홈에서는 글쓰기, 검색 둘 다 보이게
                menu.findItem(R.id.action_add)?.isVisible = true
                menu.findItem(R.id.action_search)?.isVisible = true

                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem?.actionView as androidx.appcompat.widget.SearchView

                searchView.queryHint = "제목을 검색해보세요"

                // 검색 입력 이벤트 감지
                searchView.setOnQueryTextListener(object :
                    androidx.appcompat.widget.SearchView.OnQueryTextListener {

                    override fun onQueryTextSubmit(query: String?): Boolean {
                        reviewAdapter.filter(query ?: "")
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        reviewAdapter.filter(newText ?: "")
                        return true
                    }
                })
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_add -> {
                        // 글쓰기 버튼 클릭
                        val intent = Intent(requireContext(), AddReviewActivity::class.java)
                        startActivity(intent)
                        true
                    }else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    // 메모리 누수 방지 위해 바인딩 참조 해제
    override fun onDestroyView() {
        super.onDestroyView()
        _bind = null
    }
}
