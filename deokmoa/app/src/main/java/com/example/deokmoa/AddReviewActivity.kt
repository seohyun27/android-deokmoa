package com.example.deokmoa

import android.R
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.deokmoa.data.AppDatabase
import com.example.deokmoa.data.Review
import com.example.deokmoa.data.Tag
import com.example.deokmoa.data.Category
import com.example.deokmoa.databinding.ActivityAddReviewBinding
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.Exception // ⭐️ import 추가

class AddReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddReviewBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    private var selectedImageUri: Uri? = null

    // ⭐️ "수정 모드"를 위한 변수
    private var editingReviewId: Int = -1
    private var originalImageFileName: String? = null // 원래 이미지 파일 이름 (삭제 처리용)

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.ivSelectedImage.load(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCategorySpinner()
        setupTagCheckBoxes()

        // ⭐️ 1. "수정 모드"인지 확인
        editingReviewId = intent.getIntExtra("EDIT_REVIEW_ID", -1)
        if (editingReviewId != -1) {
            // "수정 모드"일 경우
            title = "리뷰 수정" // (선택) 액티비티 제목 변경
            binding.btnSaveReview.text = "수정하기" // 버튼 텍스트 변경
            loadReviewData(editingReviewId) // 기존 데이터 불러오기
        }

        binding.btnSelectImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSaveReview.setOnClickListener {
            saveReview()
        }
    }

    // ⭐️ 2. (신규) "수정 모드"일 때 DB에서 데이터를 가져와 UI에 채우는 함수
    private fun loadReviewData(reviewId: Int) {
        lifecycleScope.launch {
            // ReviewDao에 추가한 getReviewByIdSuspend 함수 사용
            val review = database.reviewDao().getReviewByIdSuspend(reviewId)
            if (review == null) {
                Toast.makeText(this@AddReviewActivity, "리뷰 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            // UI에 데이터 채우기
            binding.etTitle.setText(review.title)
            binding.rbRating.rating = review.rating
            binding.etReviewText.setText(review.reviewText)

            // 원본 이미지 파일 이름 저장 (나중에 새 이미지로 교체 시 원본 삭제하기 위함)
            originalImageFileName = review.imageUri

            // 카테고리 스피너 설정
            val categoryIndex = Category.values().indexOfFirst { it.name == review.category }
            if (categoryIndex != -1) {
                binding.spinnerCategory.setSelection(categoryIndex)
            }

            // 태그 체크박스 설정
            val selectedTags = review.tags.split(",").toSet()
            for (i in 0 until binding.layoutTags.childCount) {
                (binding.layoutTags.getChildAt(i) as? CheckBox)?.let { checkBox ->
                    if (selectedTags.contains(checkBox.tag.toString())) {
                        checkBox.isChecked = true
                    }
                }
            }

            // 이미지 미리보기 설정
            if (!review.imageUri.isNullOrEmpty()) {
                val file = File(filesDir, review.imageUri!!)
                binding.ivSelectedImage.load(file) {
                    error(R.drawable.ic_dialog_alert) // (임시) 에러 이미지
                }
            }
        }
    }

    // (setupCategorySpinner, setupTagCheckBoxes 함수는 동일 - 수정 없음)
    private fun setupCategorySpinner() {
        val categories = Category.values().map { it.displayName }
        val adapter = ArrayAdapter(this, R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }

    private fun setupTagCheckBoxes() {
        Tag.values().forEach {tag ->
            val checkBox = CheckBox(this).apply {
                text = tag.displayName
                this.tag = tag.name
            }
            binding.layoutTags.addView(checkBox)
        }
    }

    // (getSelectedTags 함수는 동일 - 수정 없음)
    private fun getSelectedTags(): String {
        val selectedTags = mutableListOf<String>()
        for (i in 0 until binding.layoutTags.childCount) {
            val view = binding.layoutTags.getChildAt(i)
            if (view is CheckBox && view.isChecked) {
                selectedTags.add(view.tag.toString())
            }
        }
        return selectedTags.joinToString(",")
    }

    // (saveImageToInternalStorage 함수는 동일 - 수정 없음)
    private fun saveImageToInternalStorage(uri: Uri): String? {
        val inputStream: InputStream?
        try {
            inputStream = contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e("AddReviewActivity", "Failed to open input stream for URI.")
                return null
            }
            val fileName = "review_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            Log.d("AddReviewActivity", "Image saved to internal storage: $fileName")
            return fileName
        } catch (e: Exception) {
            Log.e("AddReviewActivity", "Failed to save image to internal storage", e)
            return null
        }
    }

    // ⭐️ 3. "저장" 함수 수정 (추가/수정 분기 처리)
    private fun saveReview() {
        val categoryName = Category.values()[binding.spinnerCategory.selectedItemPosition].name
        val title = binding.etTitle.text.toString()
        val rating = binding.rbRating.rating
        val reviewText = binding.etReviewText.text.toString()
        val tags = getSelectedTags()

        if (title.isBlank() || reviewText.isBlank()) {
            Toast.makeText(this, "제목과 리뷰 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // ⭐️ 이미지 처리 로직 수정
        var finalImageFileName: String? = null
        if (selectedImageUri != null) {
            // 1. (수정/추가 공통) 새 이미지를 선택한 경우
            finalImageFileName = saveImageToInternalStorage(selectedImageUri!!)
            if (finalImageFileName == null) {
                Toast.makeText(this, "이미지 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                return // 저장 실패 시 중단
            }

            // 1-1. (수정 모드 전용) 새 이미지 저장 성공 시, *기존 이미지 파일*이 있었다면 삭제
            if (editingReviewId != -1 && !originalImageFileName.isNullOrEmpty()) {
                try {
                    File(filesDir, originalImageFileName!!).delete()
                    Log.d("AddReviewActivity", "Old image file deleted: $originalImageFileName")
                } catch (e: Exception) {
                    Log.e("AddReviewActivity", "Failed to delete old image file", e)
                }
            }
        } else {
            // 2. 새 이미지를 선택하지 않은 경우
            if (editingReviewId != -1) {
                // 2-1. (수정 모드) 기존 이미지 파일 이름을 그대로 사용
                finalImageFileName = originalImageFileName
            }
            // 2-2. (추가 모드) null (이미지 없음)
        }


        // ⭐️ "수정 모드"와 "추가 모드" 분기
        if (editingReviewId != -1) {
            // === "수정 모드"일 경우 ===
            val updatedReview = Review(
                id = editingReviewId, //️ 기존 ID를 반드시 포함!!
                category = categoryName,
                title = title,
                rating = rating,
                reviewText = reviewText,
                tags = tags,
                imageUri = finalImageFileName // 새 이미지 또는 기존 이미지
            )

            lifecycleScope.launch {
                database.reviewDao().update(updatedReview) //️ update 호출
                runOnUiThread {
                    Toast.makeText(this@AddReviewActivity, "리뷰가 수정되었습니다.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            // === "추가 모드"일 경우 (기존 로직) ===
            val review = Review(
                category = categoryName,
                title = title,
                rating = rating,
                reviewText = reviewText,
                tags = tags,
                imageUri = finalImageFileName // 새 이미지 또는 null
            )

            lifecycleScope.launch {
                database.reviewDao().insert(review) // ⭐️ insert 호출
                runOnUiThread {
                    Toast.makeText(this@AddReviewActivity, "리뷰가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}