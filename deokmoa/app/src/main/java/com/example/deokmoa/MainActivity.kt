package com.example.deokmoa

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deokmoa.data.AppDatabase
import com.example.deokmoa.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var reviewAdapter: ReviewAdapter
    private val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()

        // '리뷰 기록' (FAB) 버튼 클릭 리스너
        binding.fabAddReview.setOnClickListener {
            val intent = Intent(this, AddReviewActivity::class.java)
            startActivity(intent)
        }

        // 데이터베이스의 모든 리뷰를 관찰 (Observe)
        database.reviewDao().getAllReviews().observe(this) { reviews ->
            // .toList()를 호출하여 Java List를 Kotlin List로 변환 (오류 수정)
            reviewAdapter.submitList(reviews?.toList())
        }
    }

    private fun setupRecyclerView() {
        reviewAdapter = ReviewAdapter { review ->
            // 아이템 클릭 시 상세 보기 액티비티로 이동
            val intent = Intent(this, ReviewDetailActivity::class.java)
            intent.putExtra("REVIEW_ID", review.id)
            startActivity(intent)
        }
        binding.rvReviews.apply {
            adapter = reviewAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }
}
