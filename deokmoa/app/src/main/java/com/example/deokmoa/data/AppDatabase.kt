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

                            // DB 생성 시점에 SQL 쿼리로 직접 데이터 삽입
                            db.beginTransaction()
                            try {
                                // 1. 기본 카테고리 (isDefault = true -> 1)
                                db.execSQL("INSERT INTO categories (name, isDefault) VALUES ('애니메이션', 1)")
                                db.execSQL("INSERT INTO categories (name, isDefault) VALUES ('소설', 1)")
                                db.execSQL("INSERT INTO categories (name, isDefault) VALUES ('드라마', 1)")
                                db.execSQL("INSERT INTO categories (name, isDefault) VALUES ('영화', 1)")

                                // 2. 추가 카테고리 (isDefault = false -> 0)
                                /**
                                db.execSQL("INSERT INTO categories (name, isDefault) VALUES ('방탈출', 0)")
                                db.execSQL("INSERT INTO categories (name, isDefault) VALUES ('뮤지컬', 0)")
                                db.execSQL("INSERT INTO categories (name, isDefault) VALUES ('콘서트', 0)")
                                */

                                db.setTransactionSuccessful()
                            } finally {
                                db.endTransaction()
                            }
                        }
                    })                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}