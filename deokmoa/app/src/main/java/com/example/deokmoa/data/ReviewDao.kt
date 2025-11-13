package com.example.deokmoa.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update // ⭐️ 1. Update import 추가

@Dao
interface ReviewDao {
    @Insert
    suspend fun insert(review: Review)

    // 2. 수정(Update) 함수 추가
    @Update
    suspend fun update(review: Review)

    @Delete
    suspend fun delete(review: Review)

    @Query("SELECT * FROM reviews ORDER BY id DESC")
    fun getAll(): LiveData<List<Review>>

    @Query("SELECT * FROM reviews WHERE id = :reviewId")
    fun getReviewById(reviewId: Int): LiveData<Review?> // (이건 원래 있으셨죠)

    // 3. "수정 모드"에서 데이터 채우기용 함수
    @Query("SELECT * FROM reviews WHERE id = :reviewId")
    suspend fun getReviewByIdSuspend(reviewId: Int): Review?
}