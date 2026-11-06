package com.firstlogistics.flproof.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.firstlogistics.flproof.data.dao.EvidenceDao
import com.firstlogistics.flproof.data.dao.SeabankPackageDao
import com.firstlogistics.flproof.data.entity.EvidenceEntity
import com.firstlogistics.flproof.data.entity.SeabankPackageEntity

@Database(
    entities = [
        EvidenceEntity::class,
        SeabankPackageEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun evidenceDao(): EvidenceDao
    abstract fun seabankPackageDao(): SeabankPackageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fl_proof_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
