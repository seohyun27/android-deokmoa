package com.example.deokmoa

import android.net.Uri
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

    class ReviewViewHolder(private val binding: ItemReviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(review: Review) {
            binding.tvReviewTitle.text = review.title
            binding.rbReviewRating.rating = review.rating

            // 이미지 URI가 있으면 Coil을 사용해 로드
            if (!review.imageUri.isNullOrEmpty()) {
                binding.ivReviewImage.load(Uri.parse(review.imageUri)) {
                    crossfade(true)
                    placeholder(R.drawable.ic_launcher_background) // 로딩 중 이미지 (임시)
                    error(R.drawable.ic_launcher_background) // 에러 시 이미지 (임시)
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