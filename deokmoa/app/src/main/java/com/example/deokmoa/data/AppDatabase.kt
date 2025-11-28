package com.example.deokmoa.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import kotlin.jvm.Volatile;

// Room 데이터베이스 메인 클래스 (싱글톤)
@Database(entities = [Review::class, CategoryEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reviewDao(): ReviewDao
    abstract fun categoryDao(): CategoryDao

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
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // 데이터베이스가 처음 생성될 때 기본 카테고리 추가
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    prepopulateCategories(database.categoryDao())
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // 기본 카테고리 미리 채우기
        private suspend fun prepopulateCategories(categoryDao: CategoryDao) {
            val defaultCategories = listOf(
                CategoryEntity(name = "애니메이션", isDefault = true),
                CategoryEntity(name = "소설", isDefault = true),
                CategoryEntity(name = "드라마", isDefault = true),
                CategoryEntity(name = "영화", isDefault = true),
                CategoryEntity(name = "방탈출", isDefault = false),
                CategoryEntity(name = "뮤지컬", isDefault = false),
                CategoryEntity(name = "콘서트", isDefault = false)
            )
            defaultCategories.forEach { categoryDao.insert(it) }
        }
    }
}