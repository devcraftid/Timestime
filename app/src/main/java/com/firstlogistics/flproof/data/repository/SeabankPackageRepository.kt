package com.firstlogistics.flproof.data.repository

import com.firstlogistics.flproof.data.dao.SeabankPackageDao
import com.firstlogistics.flproof.data.entity.SeabankPackageEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class SeabankPackageRepository(private val dao: SeabankPackageDao) {

    val allPackages: Flow<List<SeabankPackageEntity>> = dao.getAllPackages()
    val totalCount: Flow<Int> = dao.getTotalCount()

    fun getTodayCount(): Flow<Int> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = cal.timeInMillis - 1
        return dao.getTodayCount(startOfDay, endOfDay)
    }

    suspend fun getAllList(): List<SeabankPackageEntity> = dao.getAllList()

    suspend fun getById(id: String): SeabankPackageEntity? = dao.getById(id)

    suspend fun getByAwb(awb: String): SeabankPackageEntity? = dao.getByAwb(awb)

    suspend fun savePackage(pkg: SeabankPackageEntity) = dao.insertOrUpdate(pkg)

    suspend fun deletePackage(pkg: SeabankPackageEntity) = dao.delete(pkg)

    suspend fun deleteByIds(ids: List<String>) = dao.deleteByIds(ids)
}

