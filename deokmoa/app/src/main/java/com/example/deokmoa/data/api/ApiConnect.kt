package com.example.deokmoa.data.api

import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.security.KeyRep

interface ApiConnect {
    @GET("search/movie")
    fun searchMovie(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String = "ko-KR"
    ): Call<MovieResult>

    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"

        fun create(): ApiConnect {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiConnect::class.java)
        }
    }
}