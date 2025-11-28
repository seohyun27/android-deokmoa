package com.example.deokmoa.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ReviewDao {
    @Insert
    suspend fun insert(review: Review)

    @Update
    suspend fun update(review: Review)

    @Delete
    suspend fun delete(review: Review)

    @Query("SELECT * FROM reviews ORDER BY id DESC")
    fun getAll(): LiveData<List<Review>>

    @Query("SELECT * FROM reviews WHERE id = :reviewId")
    fun getReviewById(reviewId: Int): LiveData<Review?>

    @Query("SELECT * FROM reviews WHERE id = :reviewId")
    suspend fun getReviewByIdSuspend(reviewId: Int): Review?

    // 카테고리 랭킹 쿼리에 사용할 변수
    data class CategoryStats(
        val category: String,
        val count: Int
    )

    // 카테고리 랭킹 쿼리
    @Query("SELECT category, COUNT(category) as count FROM reviews GROUP BY category ORDER BY count DESC LIMIT 4")
    suspend fun getTopCategories(): List<CategoryStats>

    // 태그 랭킹 쿼리
    @Query("SELECT tags FROM reviews")
    suspend fun getAllTags(): List<String>

    // 평균 별점 쿼리
    @Query("SELECT AVG(rating) FROM reviews")
    suspend fun getAverageRating(): Float?

    // 특정 카테고리를 사용하는 리뷰 개수 조회 (카테고리 삭제 전 체크용)
    @Query("SELECT COUNT(*) FROM reviews WHERE category = :categoryName")
    suspend fun countReviewsByCategory(categoryName: String): Int
}