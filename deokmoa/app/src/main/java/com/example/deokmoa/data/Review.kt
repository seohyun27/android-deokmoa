package com.example.deokmoa.data;

import androidx.room.Entity
import androidx.room.PrimaryKey

// 리뷰 엔티티 (DB 테이블)
@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val category: String,        // Category.name 을 저장
    val title: String,
    val rating: Float,
    val reviewText: String,
    val tags: String,            // Tag.name 을 쉼표(,)로 구분하여 저장
    val imageUri: String?        // 이미지 Uri의 String 경로 (선택 사항)
)