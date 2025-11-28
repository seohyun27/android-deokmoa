package com.example.deokmoa.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CategoryDao {
    // 모든 카테고리 가져오기 (LiveData)
    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
    fun getAll(): LiveData<List<CategoryEntity>>

    // 모든 카테고리 가져오기 (동기)
    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
    suspend fun getAllSync(): List<CategoryEntity>

    // 카테고리 추가
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity)

    // 카테고리 삭제
    @Delete
    suspend fun delete(category: CategoryEntity)

    // 이름으로 카테고리 찾기
    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): CategoryEntity?
}
