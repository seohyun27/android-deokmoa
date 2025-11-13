package com.example.deokmoa.data

import androidx.lifecycle.LiveData
import androidx.room.*

import java.util.List;

// 데이터베이스 접근 객체 (Data Access Object)
@Dao
interface ReviewDao {
    @Insert
    suspend fun insert(review: Review)

    @Update
    suspend fun update(review: Review)

    @Delete
    suspend fun delete(review: Review)

    // 모든 리뷰를 ID 내림차순 (최신순)으로 가져오기 (LiveData 사용)
    @Query("SELECT * FROM reviews ORDER BY id DESC")
    fun getAllReviews(): LiveData<List<Review>>

    // ID로 특정 리뷰 가져오기 (LiveData 사용)
    @Query("SELECT * FROM reviews WHERE id = :id")
    fun getReviewById(id: Int): LiveData<Review>
}