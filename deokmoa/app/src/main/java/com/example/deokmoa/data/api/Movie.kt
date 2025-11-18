package com.example.deokmoa.data.api

import com.google.gson.annotations.SerializedName

data class Movie (
    val id: Int,
    val title: String,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("release_date") val releaseDate: String?
)