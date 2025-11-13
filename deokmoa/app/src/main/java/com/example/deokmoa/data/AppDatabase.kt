package com.example.deokmoa.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import kotlin.jvm.Volatile;

// Room 데이터베이스 메인 클래스 (싱글톤)
@Database(entities = [Review::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reviewDao(): ReviewDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "deokmoa_database"
                )
                    .fallbackToDestructiveMigration() // (참고) 스키마 변경 시 기존 데이터 삭제 (개발용)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}