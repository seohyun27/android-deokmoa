package com.example.deokmoa

// import 2개 추가 (android.net.Uri는 이제 사용하지 않으므로 제거해도 됩니다)
import java.io.File // ⭐️ File 객체 사용을 위해 import 추가
import android.util.Log // ⭐️ (선택) 로그 확인용

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load // Coil 라이브러리 사용
import com.example.deokmoa.data.Review
import com.example.deokmoa.databinding.ItemReviewBinding

// RecyclerView 어댑터 (ListAdapter 사용)
class ReviewAdapter(private val onItemClicked: (Review) -> Unit) :
    ListAdapter<Review, ReviewAdapter.ReviewViewHolder>(DiffCallback) {
    private var original: List<Review> = emptyList()
    private var isFiltering = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
        holder.itemView.setOnClickListener {
            onItemClicked(current)
        }
    }
    override fun submitList(list: List<Review>?) {
        super.submitList(list)
        if(list != null && !isFiltering) {
            original = list //전체 리스트 저장
        }
    }
    fun filter(query: String) {
        isFiltering = true

        if(query.isBlank()){
            super.submitList(original)
            isFiltering = false
            return
        }
        val filteredList = original.filter { review ->
            review.title.contains(query, ignoreCase = true)
        }
        super.submitList(filteredList)
        isFiltering = false
    }


    class ReviewViewHolder(private val binding: ItemReviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(review: Review) {
            binding.tvReviewTitle.text = review.title
            binding.rbReviewRating.rating = review.rating

            // 이미지 파일 이름이 있으면 File 객체로 로드
            if (!review.imageUri.isNullOrEmpty()) {
                // 1. ViewHolder의 아이템 뷰(root)에서 context를 가져온다
                val context = binding.root.context
                // 2. context.filesDir와 파일 이름으로 File 객체 생성
                val file = File(context.filesDir, review.imageUri!!)

                // 3. Uri.parse() 대신 File 객체로 로드
                binding.ivReviewImage.load(file) {
                    crossfade(true)
                    placeholder(R.drawable.ic_launcher_background) // 로딩 중 이미지 (임시)
                    error(R.drawable.ic_launcher_background) // 에러 시 이미지 (임시)
                    listener(onError = { _, result ->
                        Log.e("ReviewAdapter", "Coil (File) load failed: ${result.throwable.message}")
                    })
                }
            } else {
                // 이미지가 없을 경우 기본 이미지 설정 (임시)
                binding.ivReviewImage.setImageResource(R.drawable.ic_launcher_background)
            }
        }
    }

    // DiffUtil: RecyclerView의 성능 향상을 위해 아이템 변경 사항을 계산
    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Review>() {
            override fun areItemsTheSame(oldItem: Review, newItem: Review): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Review, newItem: Review): Boolean {
                return oldItem == newItem
            }
        }
    }
}