package com.firstlogistics.flproof.data.dao

import androidx.room.*
import com.firstlogistics.flproof.data.entity.SeabankPackageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeabankPackageDao {

    @Query("SELECT * FROM seabank_packages ORDER BY createdTimestamp DESC")
    fun getAllPackages(): Flow<List<SeabankPackageEntity>>

    @Query("SELECT * FROM seabank_packages ORDER BY createdTimestamp DESC")
    suspend fun getAllList(): List<SeabankPackageEntity>

    @Query("SELECT * FROM seabank_packages WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SeabankPackageEntity?

    @Query("SELECT * FROM seabank_packages WHERE LOWER(awb) = LOWER(:awb) LIMIT 1")
    suspend fun getByAwb(awb: String): SeabankPackageEntity?

    @Query("SELECT COUNT(*) FROM seabank_packages")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM seabank_packages WHERE createdTimestamp >= :startOfDay AND createdTimestamp <= :endOfDay")
    fun getTodayCount(startOfDay: Long, endOfDay: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: SeabankPackageEntity): Long

    @Delete
    suspend fun delete(entity: SeabankPackageEntity): Int

    @Query("DELETE FROM seabank_packages WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>): Int
}

