package com.example.deokmoa.data

// Enum 클래스로 카테고리 정의
enum class Category(val displayName: String) {
    ANIMATION("애니메이션"),
    NOVEL("소설"),
    DRAMA("드라마"),
    MOVIE("영화"),
    OTHER("기타")
}