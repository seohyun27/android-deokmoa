package com.example.deokmoa

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.deokmoa.data.AppDatabase
import com.example.deokmoa.data.Category
import com.example.deokmoa.data.Review
import com.example.deokmoa.data.Tag
import com.example.deokmoa.databinding.ActivityAddReviewBinding
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.Exception
import android.R as AndroidR
import androidx.appcompat.app.AlertDialog

class AddReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddReviewBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    private var selectedImageUri: Uri? = null

    // "수정 모드" 변수
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
     private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            pickImageLauncher.launch("image/*")
        } else{
            Toast.makeText(this, "갤러리 접근 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
     }
    private fun showPermissionAlertDialog(permission: String) {
        AlertDialog.Builder(this)
            .setTitle("권한 승인이 필요합니다.")
            .setMessage("사진을 선택 하려면 권한이 필요합니다.")
            .setPositiveButton("허용하기") { _, _ ->
                pickImageLauncher.launch("image/*")
            }
            .setNegativeButton("허용 안함") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()

        setupCategorySpinner()

        // "수정 모드"인지 확인
        editingReviewId = intent.getIntExtra("EDIT_REVIEW_ID", -1)
        if (editingReviewId != -1) {
            // === 수정 모드 ===
            title = "리뷰 수정"
            binding.btnSaveReview.text = "수정하기"
            loadReviewData(editingReviewId)
        } else {
            // === 추가 모드 ===
            title = "리뷰 작성"
            // 비어있는 태그 목록으로 Chip들을 화면에 먼저 생성합니다.
            setupTagChips(emptySet())
        }

        binding.btnSelectImage.setOnClickListener {
            val permission = android.Manifest.permission.READ_EXTERNAL_STORAGE
            when {
                // 권한이 있으면 갤러리 열기
                ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_GRANTED -> {
                    pickImageLauncher.launch("image/*")
                }
                else -> {
                    // 다이얼로그 먼저(항상)
                    showPermissionAlertDialog(permission)
                }
            }
        }

        binding.btnSaveReview.setOnClickListener {
            saveReview()
        }
    }
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true) // 타이틀 보이게 설정
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // 뒤로가기 버튼(home)을 눌렀을 때의 동작
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // DB에서 기존 리뷰 불러오기
    private fun loadReviewData(reviewId: Int) {
        lifecycleScope.launch {
            val review = database.reviewDao().getReviewByIdSuspend(reviewId)
            if (review == null) {
                Toast.makeText(this@AddReviewActivity, "리뷰 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            // 기본 필드 채우기
            binding.etTitle.setText(review.title)
            binding.rbRating.rating = review.rating
            binding.etReviewText.setText(review.reviewText)

            // 기존 이미지 파일 이름 저장
            originalImageFileName = review.imageUri

            // 카테고리 스피너 설정
            val categoryIndex = Category.values().indexOfFirst { it.name == review.category }
            if (categoryIndex != -1) {
                binding.spinnerCategory.setSelection(categoryIndex)
            }

            // DB에서 불러온 태그 정보로 Chip을 생성하고 선택 상태 설정.
            val selectedTags = if (review.tags.isNotEmpty()) review.tags.split(",").toSet() else emptySet()
            setupTagChips(selectedTags)

            // 이미지 미리보기
            if (!review.imageUri.isNullOrEmpty()) {
                val file = File(filesDir, review.imageUri!!)
                binding.ivSelectedImage.load(file) {
                    error(AndroidR.drawable.ic_dialog_alert) // 기본 에러 아이콘
                }
            }
        }
    }
    // 카테고리 스피너
    private fun setupCategorySpinner() {
        val categories = Category.values().map { it.displayName }
        val adapter = ArrayAdapter(this, AndroidR.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(AndroidR.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }

    // Tag enum 기반 해시태그 동적 생성 (선택 상태 인자 추가)
    private fun setupTagChips(selectedTags: Set<String>) {
        val chipGroup = binding.layoutTags
        chipGroup.removeAllViews() // 중복 생성을 방지하기 위해 기존 뷰를 모두 제거

        Tag.values().forEach { tag ->
            val chip = Chip(this).apply {
                text = tag.displayName       // 화면에 보이는 이름
                this.tag = tag.name         // 내부적으로 저장할 enum 이름
                isCheckable = true
                isClickable = true
                isCheckedIconVisible = false

                // Chip 생성 시점에 인자로 받은 Set을 이용해 선택 상태를 바로 결정
                isChecked = selectedTags.contains(tag.name)

                // 스타일 적용
                setChipBackgroundColorResource(R.color.hashtag_background_selector)
                setTextColor(
                    ContextCompat.getColor(
                        this@AddReviewActivity,
                        R.color.black
                    )
                )
            }
            chipGroup.addView(chip)
        }
    }

    // 선택된 해시태그를 enum 형식으로 변환
    private fun getSelectedTags(): String {
        val selectedTags = mutableListOf<String>()
        val chipGroup = binding.layoutTags

        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip ?: continue
            if (chip.isChecked) {
                selectedTags.add(chip.tag.toString())
            }
        }
        return selectedTags.joinToString(",")
    }

    // 이미지 내부 저장
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

    // 저장(추가/수정 공통)
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

        // 이미지 처리
        var finalImageFileName: String? = null
        if (selectedImageUri != null) {
            // 새 이미지 선택
            finalImageFileName = saveImageToInternalStorage(selectedImageUri!!)
            if (finalImageFileName == null) {
                Toast.makeText(this, "이미지 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                return
            }

            // 수정 모드이고 기존 이미지가 있었다면 삭제
            if (editingReviewId != -1 && !originalImageFileName.isNullOrEmpty()) {
                try {
                    File(filesDir, originalImageFileName!!).delete()
                    Log.d("AddReviewActivity", "Old image file deleted: $originalImageFileName")
                } catch (e: Exception) {
                    Log.e("AddReviewActivity", "Failed to delete old image file", e)
                }
            }
        } else {
            // 새 이미지를 선택하지 않은 경우 (수정 모드에서만 기존 이미지 유지)
            if (editingReviewId != -1) {
                finalImageFileName = originalImageFileName
            }
        }

        lifecycleScope.launch {
            if (editingReviewId != -1) {
                // === 수정 모드 ===
                val updatedReview = Review(
                    id = editingReviewId,
                    category = categoryName,
                    title = title,
                    rating = rating,
                    reviewText = reviewText,
                    tags = tags,
                    imageUri = finalImageFileName
                )
                database.reviewDao().update(updatedReview)
                runOnUiThread {
                    Toast.makeText(this@AddReviewActivity, "리뷰가 수정되었습니다.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } else {
                // === 추가 모드 ===
                val review = Review(
                    category = categoryName,
                    title = title,
                    rating = rating,
                    reviewText = reviewText,
                    tags = tags,
                    imageUri = finalImageFileName
                )
                database.reviewDao().insert(review)
                runOnUiThread {
                    Toast.makeText(this@AddReviewActivity, "리뷰가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}

