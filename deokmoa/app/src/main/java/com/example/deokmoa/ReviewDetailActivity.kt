package com.example.deokmoa

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.deokmoa.data.AppDatabase
import com.example.deokmoa.data.Category
import com.example.deokmoa.data.Review
import com.example.deokmoa.data.Tag
import com.example.deokmoa.databinding.ActivityReviewDetailBinding
import kotlinx.coroutines.launch

class ReviewDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewDetailBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    private var currentReview: Review? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val reviewId = intent.getIntExtra("REVIEW_ID", -1)
        if (reviewId == -1) {
            Toast.makeText(this, "리뷰를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // LiveData를 관찰하여 리뷰 정보 표시
        database.reviewDao().getReviewById(reviewId).observe(this) { review ->
            review?.let {
                currentReview = it
                populateReviewDetails(it)
            }
        }

        // (참고) 수정 기능은 AddReviewActivity를 재사용하는 방식으로 구현할 수 있습니다.
        // binding.btnEdit.setOnClickListener { ... }

        // 삭제 버튼
        binding.btnDelete.setOnClickListener {
            currentReview?.let { reviewToDelete ->
                deleteReview(reviewToDelete)
            }
        }
    }

    // 리뷰 상세 정보 UI에 표시
    private fun populateReviewDetails(review: Review) {
        binding.tvDetailTitle.text = review.title
        binding.rbDetailRating.rating = review.rating
        binding.tvDetailReviewText.text = review.reviewText

        // 카테고리 (Enum의 displayName으로 변환)
        val categoryDisplayName = Category.values().find { it.name == review.category }?.displayName ?: review.category
        binding.tvDetailCategory.text = "카테고리: $categoryDisplayName"

        // 태그 (쉼표로 구분된 name을 displayName으로 변환)
        val tagDisplayNames = review.tags.split(",")
            .mapNotNull { tagName -> Tag.values().find { it.name == tagName }?.displayName }
            .joinToString(", ")
        binding.tvDetailTags.text = "태그: $tagDisplayNames"

        // 이미지
        if (!review.imageUri.isNullOrEmpty()) {
            binding.ivDetailImage.load(Uri.parse(review.imageUri)) {
                crossfade(true)
                placeholder(R.drawable.ic_launcher_background)
                error(R.drawable.ic_launcher_background)
            }
        } else {
            binding.ivDetailImage.setImageResource(R.drawable.ic_launcher_background)
        }
    }

    // 리뷰 삭제
    private fun deleteReview(review: Review) {
        lifecycleScope.launch {
            database.reviewDao().delete(review)
            runOnUiThread {
                Toast.makeText(this@ReviewDetailActivity, "리뷰가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                finish() // 액티비티 종료
            }
        }
    }
}