package com.example.deokmoa

import android.R
import android.net.Uri
import android.os.Bundle
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

class AddReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddReviewBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    private var selectedImageUri: Uri? = null

    // 갤러리에서 이미지 선택을 위한 ActivityResultLauncher
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // (참고) 영구적인 접근 권한 획득 (선택 사항)
            // contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            binding.ivSelectedImage.load(it) // Coil로 이미지 미리보기
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCategorySpinner()
        setupTagCheckBoxes()

        // 이미지 선택 버튼
        binding.btnSelectImage.setOnClickListener {
            pickImageLauncher.launch("image/*") // 갤러리 열기
        }

        // 저장 버튼
        binding.btnSaveReview.setOnClickListener {
            saveReview()
        }
    }

    // 카테고리 스피너 설정
    private fun setupCategorySpinner() {
        val categories = Category.values().map { it.displayName }
        val adapter = ArrayAdapter(this, R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }

    // 태그 체크박스 동적 생성
    private fun setupTagCheckBoxes() {
        Tag.values().forEach {tag ->
            val checkBox = CheckBox(this).apply {
                text = tag.displayName
                this.tag = tag.name // 체크박스의 tag 속성에 enum의 name (e.g., "ACTION") 저장
            }
            binding.layoutTags.addView(checkBox) // LinearLayout에 추가
        }
    }

    // 선택된 태그 목록 가져오기
    private fun getSelectedTags(): String {
        val selectedTags = mutableListOf<String>()
        for (i in 0 until binding.layoutTags.childCount) {
            val view = binding.layoutTags.getChildAt(i)
            if (view is CheckBox && view.isChecked) {
                selectedTags.add(view.tag.toString()) // CheckBox의 tag 속성 (enum name)을 리스트에 추가
            }
        }
        return selectedTags.joinToString(",") // 쉼표로 구분된 문자열로 반환
    }

    // 리뷰 저장
    private fun saveReview() {
        val categoryName = Category.values()[binding.spinnerCategory.selectedItemPosition].name
        val title = binding.etTitle.text.toString()
        val rating = binding.rbRating.rating
        val reviewText = binding.etReviewText.text.toString()
        val tags = getSelectedTags()
        val imageUriString = selectedImageUri?.toString()

        if (title.isBlank() || reviewText.isBlank()) {
            Toast.makeText(this, "제목과 리뷰 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val review = Review(
            category = categoryName,
            title = title,
            rating = rating,
            reviewText = reviewText,
            tags = tags,
            imageUri = imageUriString
        )

        // 코루틴을 사용해 백그라운드 스레드에서 DB에 저장
        lifecycleScope.launch {
            database.reviewDao().insert(review)
            runOnUiThread {
                Toast.makeText(this@AddReviewActivity, "리뷰가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                finish() // 액티비티 종료
            }
        }
    }
}