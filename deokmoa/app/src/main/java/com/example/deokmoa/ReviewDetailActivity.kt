package com.example.deokmoa

// import 1개 추가
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import java.io.File
import java.lang.Exception

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

        database.reviewDao().getReviewById(reviewId).observe(this) { review ->
            review?.let {
                currentReview = it
                populateReviewDetails(it)
            }
        }

        // ⭐️ "수정" 버튼 리스너 (XML에 ID: btnEdit 버튼 필요)
        binding.btnEdit.setOnClickListener {
            currentReview?.let { review ->
                // AddReviewActivity를 "수정 모드"로 연다
                val intent = Intent(this, AddReviewActivity::class.java).apply {
                    // "EDIT_REVIEW_ID" 라는 Key로 현재 리뷰 ID를 넘겨준다
                    putExtra("EDIT_REVIEW_ID", review.id)
                }
                startActivity(intent)
            }
        }

        // 삭제 버튼
        binding.btnDelete.setOnClickListener {
            currentReview?.let { reviewToDelete ->
                deleteReview(reviewToDelete)
            }
        }
    }

    // (populateReviewDetails 함수는 동일 - 수정 없음)
    private fun populateReviewDetails(review: Review) {
        binding.tvDetailTitle.text = review.title
        binding.rbDetailRating.rating = review.rating
        binding.tvDetailReviewText.text = review.reviewText

        val categoryDisplayName = Category.values().find { it.name == review.category }?.displayName ?: review.category
        binding.tvDetailCategory.text = "카테고리: $categoryDisplayName"

        val tagDisplayNames = review.tags.split(",")
            .mapNotNull { tagName -> Tag.values().find { it.name == tagName }?.displayName }
            .joinToString(", ")
        binding.tvDetailTags.text = "태그: $tagDisplayNames"

        if (!review.imageUri.isNullOrEmpty()) {
            val file = File(filesDir, review.imageUri!!)
            binding.ivDetailImage.load(file) {
                crossfade(true)
                placeholder(R.drawable.ic_launcher_background)
                error(R.drawable.ic_launcher_background)
                listener(onError = { _, result ->
                    Log.e("ReviewDetailActivity", "Coil (File) load failed: ${result.throwable.message}")
                })
            }
        } else {
            binding.ivDetailImage.setImageResource(R.drawable.ic_launcher_background)
        }
    }

    // (deleteReview 함수는 동일 - 수정 없음)
    private fun deleteReview(review: Review) {
        lifecycleScope.launch {
            if (!review.imageUri.isNullOrEmpty()) {
                try {
                    val file = File(filesDir, review.imageUri!!)
                    if (file.exists()) {
                        file.delete()
                        Log.d("ReviewDetailActivity", "Internal image file deleted: ${review.imageUri}")
                    }
                } catch (e: Exception) {
                    Log.e("ReviewDetailActivity", "Failed to delete internal file", e)
                }
            }

            database.reviewDao().delete(review)

            runOnUiThread {
                Toast.makeText(this@ReviewDetailActivity, "리뷰가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}