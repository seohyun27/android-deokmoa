package com.example.deokmoa

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.deokmoa.data.AppDatabase
import com.example.deokmoa.data.CategoryEntity
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deokmoa.data.api.ApiConnect
import com.example.deokmoa.data.api.MovieResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/* TMDB API 연결 방법(api 사용한 작품 검색 사용 방법)
* TMDB에서 회원가입 후 각자 api key 받기
* api key 보안을 위해 각자의 local.properties에 설정
* TMDB_API_KEY=apikey값 (""는 넣지 않음)*/
class AddReviewActivity : AppCompatActivity() {
    //api 관련 변수
    private lateinit var searchAdapter: SearchAdapter
    private val apiService: ApiConnect by lazy { ApiConnect.create()}
    private val apiKey = BuildConfig.TMDB_API_KEY
    private lateinit var binding: ActivityAddReviewBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    private var selectedImageUri: Uri? = null
    private var selectedImageUrl: String? = null //api 이미지 url을 저장할 변수

    // "수정 모드" 변수
    private var editingReviewId: Int = -1
    private var originalImageFileName: String? = null // 원래 이미지 파일 이름 (삭제 처리용)
    
    // 카테고리 목록
    private var categoryList: List<CategoryEntity> = emptyList()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            selectedImageUrl = null
            binding.ivSelectedImage.load(it)
            binding.btnDeleteImg.visibility = View.VISIBLE
            binding.btnSelectImage.visibility = View.GONE //이미지 선택 버튼 숨기기
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupSearchRecyclerView()
        setupSearchEditText()
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
        //이미지 삭제 버튼
        binding.btnSelectImage.setOnClickListener {
            selectedImageUrl = null
            pickImageLauncher.launch("image/*")
        }
        binding.btnDeleteImg.setOnClickListener {
            selectedImageUri = null
            selectedImageUrl = null
            originalImageFileName = null

            binding.ivSelectedImage.setImageDrawable(null)
            binding.btnDeleteImg.visibility = View.GONE
            binding.btnSelectImage.visibility = View.VISIBLE // 이미지 삭제 시 이미지 선택 버튼 보이기
        }

        // 리뷰 입력창 터치 시 스크롤 간섭 해결
        binding.etReviewText.setOnTouchListener { v, event ->
            if (v.id == R.id.et_review_text) {
                v.parent.requestDisallowInterceptTouchEvent(true)
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_UP -> v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
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
            val categoryIndex = categoryList.indexOfFirst { it.name == review.category }
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
                binding.btnSelectImage.visibility = View.GONE
                binding.btnDeleteImg.visibility = View.VISIBLE
            }
        }
    }

    private fun setupSearchRecyclerView() {
        searchAdapter = SearchAdapter { movie ->
            binding.etTitle.setText(movie.title) //제목 자동 채우기
            binding.rvSearchResults.visibility = View.GONE // 검색 결과 숨기기
            binding.etApi.text?.clear() // 검색어 초기화

            if (!movie.posterPath.isNullOrEmpty()) {
                val fullPosterUrl = "https://image.tmdb.org/t/p/w500${movie.posterPath}"
                selectedImageUrl = fullPosterUrl
                selectedImageUri = null

                binding.ivSelectedImage.load(fullPosterUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_launcher_background)
                }
                binding.btnDeleteImg.visibility = View.VISIBLE // 확인해보기
                binding.btnSelectImage.visibility = View.GONE
            }
        }
        binding.rvSearchResults.apply {
            adapter = searchAdapter
            layoutManager = LinearLayoutManager(this@AddReviewActivity)
        }
    }

    private fun setupSearchEditText() {
        binding.etApi.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                if (query.isNotBlank()) {
                    searchMoviesApi(query)
                } else {
                    binding.rvSearchResults.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun searchMoviesApi(query: String) {
        if (apiKey.isBlank() || apiKey == "null") {
            Log.e("AddReviewActivity", "TMDB_API_KEY가 설정되지 않았습니다.")
            binding.rvSearchResults.visibility = View.GONE
            return
        }

        apiService.searchMovie(apiKey, query).enqueue(object : Callback<MovieResult> {
            override fun onResponse(call: Call<MovieResult>, response: Response<MovieResult>) {
                if (response.isSuccessful) {
                    val movies = response.body()?.results ?: emptyList()
                    if (movies.isNotEmpty()) {
                        searchAdapter.setItems(movies)
                        binding.rvSearchResults.visibility = View.VISIBLE
                    } else {
                        binding.rvSearchResults.visibility = View.GONE
                    }
                }
            }
            // api 연결 실패시 로그메세지
            override fun onFailure(call: Call<MovieResult>, t: Throwable) {
                Log.e("AddReviewActivity", "API Call Failed ${t.message}")
                binding.rvSearchResults.visibility = View.GONE
            }
        })
    }

    // 카테고리 스피너
    private fun setupCategorySpinner() {
        // DB에서 카테고리 로드
        lifecycleScope.launch {
            categoryList = database.categoryDao().getAllSync()
            val categoryNames = categoryList.map { it.name }
            val adapter = ArrayAdapter(this@AddReviewActivity, AndroidR.layout.simple_spinner_item, categoryNames)
            adapter.setDropDownViewResource(AndroidR.layout.simple_spinner_dropdown_item)
            binding.spinnerCategory.adapter = adapter

            // 카테고리 선택에 따른 검색창 표시,숨김 로직 추가
            binding.spinnerCategory.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedCategoryName = categoryList[position].name

                    // 검색창을 보여줄 기본 카테고리 (애니메이션, 소설, 드라마, 영화)
                    val searchableCategories = listOf("애니메이션", "소설", "드라마", "영화")
                    val isSearchable = selectedCategoryName in searchableCategories

                    // 해당 줄 아래에서 일어나는 모든 레이아웃 변경(visibility 등)이 부드럽게 애니메이션 되도록 설정
                    val transition = AutoTransition()
                    transition.duration = 200  // 애니메이션의 속도를 200ms로 설정
                    transition.interpolator = FastOutSlowInInterpolator()
                    TransitionManager.beginDelayedTransition(binding.writeMain, transition)

                    if (isSearchable) {
                        binding.tilSearch.visibility = View.VISIBLE
                    } else {
                        binding.tilSearch.visibility = View.GONE
                        binding.rvSearchResults.visibility = View.GONE // 숨겨질 때 열려있던 검색 결과 목록도 같이 닫기
                    }
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        }
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

    // URL 이미지를 다운로드 후 로컬에 저장하고 반환
    private suspend fun downloadImageToInternalStorage(imageUrl: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                //  Coil 이미지 로더 생성
                val loader = ImageLoader(this@AddReviewActivity)
                val request = ImageRequest.Builder(this@AddReviewActivity)
                    .data(imageUrl)
                    .allowHardware(false) // 비트맵으로 받기 위해
                    .build()

                val result = loader.execute(request)

                if (result is SuccessResult) {
                    // drawable을 꺼내서 BitmapDrawable 로 캐스팅해야 함
                    val drawable = result.drawable
                    val bitmap = (drawable as? BitmapDrawable)?.bitmap

                    if (bitmap != null) {
                        // 파일명 생성 및 저장
                        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
                        val file = File(filesDir, fileName)
                        val outputStream = FileOutputStream(file)

                        // 비트맵을 파일로 압축 저장
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                        outputStream.flush()
                        outputStream.close()

                        Log.d("AddReviewActivity", "API image saved: $fileName")
                        return@withContext fileName
                    } else {
                        Log.e("AddReviewActivity", "Bitmap is null (drawable cast 실패)")
                    }
                } else {
                    Log.e("AddReviewActivity", "Coil result is not SuccessResult: $result")
                }
            } catch (e: Exception) {
                Log.e("AddReviewActivity", "API Image Download Failed", e)
            }
            return@withContext null
        }
    }

    // 저장(추가/수정 공통)
    private fun saveReview() {
        val categoryName = categoryList[binding.spinnerCategory.selectedItemPosition].name
        val title = binding.etTitle.text.toString()
        val rating = binding.rbRating.rating
        val reviewText = binding.etReviewText.text.toString()
        val tags = getSelectedTags()

        // 기본 유효성 검사
        if (title.isBlank() || reviewText.isBlank()) {
            Toast.makeText(this, "제목과 리뷰 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // 이미지 다운로드/복사 및 DB 저장을 위해
        lifecycleScope.launch {
            var finalImageUri: String? = null
            // === 이미지 처리 로직 ===

            //  갤러리에서 이미지를 선택한 경우
            if (selectedImageUri != null) {
                finalImageUri = withContext(Dispatchers.IO) {
                    saveImageToInternalStorage(selectedImageUri!!)
                }
                if (finalImageUri == null) {
                    Toast.makeText(this@AddReviewActivity, "갤러리 이미지 저장 실패", Toast.LENGTH_SHORT).show()
                    return@launch
                }
            }
            // TMDB API로 선택한 포스터(URL)가 있는 경우
            else if (!selectedImageUrl.isNullOrEmpty()) {
                // 다운로드하여 파일로 저장
                finalImageUri = downloadImageToInternalStorage(selectedImageUrl!!)

                if (finalImageUri == null) {
                    Toast.makeText(this@AddReviewActivity, "API 이미지 다운로드 실패", Toast.LENGTH_SHORT).show()
                    return@launch
                }
            }
            // 수정 모드이고 이미지를 변경하지 않은 경우 (기존 파일 유지)
            else if (editingReviewId != -1) {
                finalImageUri = originalImageFileName
            }
            // 수정 모드로 새로운 이미지 선택 시 기존 파일 삭제
            if (editingReviewId != -1 && (selectedImageUri != null || !selectedImageUrl.isNullOrEmpty())) {
                if (!originalImageFileName.isNullOrEmpty() && !originalImageFileName!!.startsWith("http")) {
                    withContext(Dispatchers.IO) {
                        try {
                            File(filesDir, originalImageFileName!!).delete()
                            Log.d("AddReviewActivity", "Old image file deleted: $originalImageFileName")
                        } catch (e: Exception) {
                            Log.e("AddReviewActivity", "Failed to delete old image file", e)
                        }
                    }
                }
            }

            // DB 저장 로직
            if (editingReviewId != -1) {
                //수정
                val updatedReview = Review(
                    id = editingReviewId,
                    category = categoryName,
                    title = title,
                    rating = rating,
                    reviewText = reviewText,
                    tags = tags,
                    imageUri = finalImageUri // 파일명 저장
                )
                database.reviewDao().update(updatedReview)
                Toast.makeText(this@AddReviewActivity, "리뷰가 수정되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                // 추가
                val review = Review(
                    category = categoryName,
                    title = title,
                    rating = rating,
                    reviewText = reviewText,
                    tags = tags,
                    imageUri = finalImageUri // 파일명 저장
                )
                database.reviewDao().insert(review)
                Toast.makeText(this@AddReviewActivity, "리뷰가 저장되었습니다.", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }
}


