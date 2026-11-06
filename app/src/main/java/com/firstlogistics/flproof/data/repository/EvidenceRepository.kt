package com.firstlogistics.flproof.data.repository

import com.firstlogistics.flproof.data.dao.EvidenceDao
import com.firstlogistics.flproof.data.entity.EvidenceEntity
import com.firstlogistics.flproof.utils.StorageManager
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.Calendar

class EvidenceRepository(
    private val evidenceDao: EvidenceDao,
    private val storageManager: StorageManager
) {

    fun getAllEvidence(): Flow<List<EvidenceEntity>> = evidenceDao.getAllEvidence()

    fun getRecentEvidence(limit: Int = 5): Flow<List<EvidenceEntity>> =
        evidenceDao.getRecentEvidence(limit)

    fun searchEvidence(query: String): Flow<List<EvidenceEntity>> =
        evidenceDao.searchEvidence(query)

    fun filterEvidence(
        query: String = "",
        category: String = "",
        status: String = "",
        startDate: Long = 0,
        endDate: Long = 0
    ): Flow<List<EvidenceEntity>> =
        evidenceDao.filterEvidence(query, category, status, startDate, endDate)

    fun getTrashedEvidence(): Flow<List<EvidenceEntity>> = evidenceDao.getTrashedEvidence()

    fun getTotalCount(): Flow<Int> = evidenceDao.getTotalCount()

    fun getTotalIncoming(): Flow<Int> = evidenceDao.getTotalIncoming()

    fun getTotalOutgoing(): Flow<Int> = evidenceDao.getTotalOutgoing()

    fun getTotalToday(): Flow<Int> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        return evidenceDao.getTotalToday(startOfDay, endOfDay)
    }

    fun getTotalProblematic(): Flow<Int> = evidenceDao.getTotalProblematic()

    fun getLocationsWithCoordinates(): Flow<List<EvidenceEntity>> =
        evidenceDao.getLocationsWithCoordinates()

    suspend fun getById(id: Long): EvidenceEntity? = evidenceDao.getById(id)

    suspend fun getByEvidenceId(evidenceId: String): EvidenceEntity? =
        evidenceDao.getByEvidenceId(evidenceId)

    fun getSessionPhotos(sessionId: String): Flow<List<EvidenceEntity>> =
        evidenceDao.getSessionPhotos(sessionId)

    suspend fun getSessionPhotosList(sessionId: String): List<EvidenceEntity> =
        evidenceDao.getSessionPhotosList(sessionId)

    suspend fun findEvidenceByCode(code: String): EvidenceEntity? =
        evidenceDao.findEvidenceByCode(code)

    suspend fun generateNextEvidenceId(): String {
        val currentCount = evidenceDao.getTotalHistoricalRecordsCount()
        return storageManager.generateEvidenceId(currentCount + 1)
    }

    suspend fun saveEvidence(evidence: EvidenceEntity): Long {
        return evidenceDao.insert(evidence)
    }

    suspend fun updateEvidence(evidence: EvidenceEntity) {
        evidenceDao.update(evidence)
    }

    suspend fun softDelete(id: Long) {
        evidenceDao.softDelete(id)
    }

    suspend fun restore(id: Long) {
        evidenceDao.restore(id)
    }

    suspend fun hardDelete(id: Long) {
        val entity = evidenceDao.getById(id)
        if (entity != null) {
            storageManager.deleteFileSafely(entity.originalPhotoPath)
            storageManager.deleteFileSafely(entity.watermarkedPhotoPath)
            evidenceDao.hardDeleteById(id)
        }
    }

    suspend fun getAllList(): List<EvidenceEntity> = evidenceDao.getAllList()
}

