package com.example.deokmoa.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// 카테고리 엔티티 (DB 테이블)
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    val name: String,           // 카테고리 이름 (예: "애니메이션", "소설", "드라마" 등)
    val isDefault: Boolean = false  // 기본 카테고리 여부 (API 연동 카테고리)
)
