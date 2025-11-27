package com.example.deokmoa

// import 1개 추가
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.deokmoa.data.AppDatabase
import com.example.deokmoa.data.Category
import com.example.deokmoa.data.Review
import com.example.deokmoa.data.Tag
import com.example.deokmoa.databinding.ActivityReviewDetailBinding
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.io.File
import java.lang.Exception
import android.view.Menu

class ReviewDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewDetailBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    private var currentReview: Review? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()

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

        // 삭제 버튼
        binding.btnDelete.setOnClickListener {
            currentReview?.let { reviewToDelete ->
                //삭제 확인 알람창 띄우기
                AlertDialog.Builder(this).setTitle("안내문")
                    .setMessage("리뷰를 삭제하시겠습니까?")
                    .setPositiveButton("삭제") { dialog, _ ->
                        deleteReview(reviewToDelete)
                        dialog.dismiss()
                    }
                    .setNegativeButton("취소") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.review_detail_toolbar, menu)
        return true
    }

        private fun setupToolbar() {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            return when (item.itemId) {
                // 홈화면 뒤로가기
                android.R.id.home -> {
                    finish()   // 이전 화면(홈)으로
                    true
                }

                // 수정메뉴
                R.id.action_edit -> {
                    currentReview?.let { review ->
                        val intent = Intent(this, AddReviewActivity::class.java).apply {
                            putExtra("EDIT_REVIEW_ID", review.id)
                        }
                        startActivity(intent)
                    }
                    true
                }

                else -> super.onOptionsItemSelected(item)
            }
        }


        // (populateReviewDetails 함수는 동일 - 수정 없음)
        private fun populateReviewDetails(review: Review) {
            supportActionBar?.title = review.title //toolbar에 제목명
            binding.tvDetailTitle.text = review.title
            binding.rbDetailRating.rating = review.rating
            binding.tvDetailReviewText.text = review.reviewText

            val categoryDisplayName =
                Category.values().find { it.name == review.category }?.displayName
                    ?: review.category
            binding.tvDetailCategory.text = "카테고리: $categoryDisplayName"

            val chipGroup = binding.chipGroupDetailTags
            chipGroup.removeAllViews()

            val tagNames = review.tags.split(",")
            tagNames.forEach { tag ->
                val chip = Chip(this).apply {
                    text = Tag.valueOf(tag).displayName
                    isCheckable = false
                    isClickable = false
                    setChipBackgroundColorResource(R.color.hashtag_background_selector)
                    setTextColor(resources.getColor(R.color.black, null))
                }
                chipGroup.addView(chip)
            }

            if (!review.imageUri.isNullOrEmpty()) {
                val file = File(filesDir, review.imageUri!!)
                binding.ivDetailImage.load(file) {
                    crossfade(true)
                    placeholder(R.drawable.ic_launcher_background)
                    error(R.drawable.ic_launcher_background)
                    listener(onError = { _, result ->
                        Log.e(
                            "ReviewDetailActivity",
                            "Coil (File) load failed: ${result.throwable.message}"
                        )
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
                            Log.d(
                                "ReviewDetailActivity",
                                "Internal image file deleted: ${review.imageUri}"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("ReviewDetailActivity", "Failed to delete internal file", e)
                    }
                }

                database.reviewDao().delete(review)

                runOnUiThread {
                    Toast.makeText(this@ReviewDetailActivity, "리뷰가 삭제되었습니다.", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }
            }
        }
    }