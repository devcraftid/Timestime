package com.firstlogistics.flproof.data.dao

import androidx.room.*
import com.firstlogistics.flproof.data.entity.EvidenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EvidenceDao {

    @Query("SELECT * FROM evidence_records WHERE isSoftDeleted = 0 ORDER BY timestamp DESC")
    fun getAllEvidence(): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM evidence_records WHERE isSoftDeleted = 0 ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvidence(limit: Int): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM evidence_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): EvidenceEntity?

    @Query("SELECT * FROM evidence_records WHERE evidenceId = :evidenceId LIMIT 1")
    suspend fun getByEvidenceId(evidenceId: String): EvidenceEntity?

    @Query("""
        SELECT * FROM evidence_records 
        WHERE isSoftDeleted = 0 
        AND (sessionId = :sessionId OR barcodeValue = :sessionId OR evidenceId = :sessionId)
        ORDER BY sessionIndex ASC, timestamp ASC
    """)
    fun getSessionPhotos(sessionId: String): Flow<List<EvidenceEntity>>

    @Query("""
        SELECT * FROM evidence_records 
        WHERE isSoftDeleted = 0 
        AND (sessionId = :sessionId OR barcodeValue = :sessionId OR evidenceId = :sessionId)
        ORDER BY sessionIndex ASC, timestamp ASC
    """)
    suspend fun getSessionPhotosList(sessionId: String): List<EvidenceEntity>

    @Query("SELECT * FROM evidence_records WHERE isSoftDeleted = 0 AND (evidenceId = :code OR barcodeValue = :code) LIMIT 1")
    suspend fun findEvidenceByCode(code: String): EvidenceEntity?

    @Query("""
        SELECT * FROM evidence_records 
        WHERE isSoftDeleted = 0 
        AND (evidenceId LIKE '%' || :query || '%' 
             OR address LIKE '%' || :query || '%' 
             OR category LIKE '%' || :query || '%' 
             OR barcodeValue LIKE '%' || :query || '%'
             OR notes LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
    """)
    fun searchEvidence(query: String): Flow<List<EvidenceEntity>>

    @Query("""
        SELECT * FROM evidence_records 
        WHERE isSoftDeleted = 0 
        AND (:category = '' OR category = :category)
        AND (:status = '' OR status = :status)
        AND (:query = '' OR (evidenceId LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%' OR barcodeValue LIKE '%' || :query || '%'))
        AND (:startDate = 0 OR timestamp >= :startDate)
        AND (:endDate = 0 OR timestamp <= :endDate)
        ORDER BY timestamp DESC
    """)
    fun filterEvidence(
        query: String,
        category: String,
        status: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM evidence_records WHERE isSoftDeleted = 1 ORDER BY timestamp DESC")
    fun getTrashedEvidence(): Flow<List<EvidenceEntity>>

    @Query("SELECT COUNT(*) FROM evidence_records WHERE isSoftDeleted = 0")
    fun getTotalCount(): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM evidence_records 
        WHERE isSoftDeleted = 0 
        AND (category = 'BARANG MASUK' OR category = 'PENERIMAAN PAKET')
    """)
    fun getTotalIncoming(): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM evidence_records 
        WHERE isSoftDeleted = 0 
        AND (category = 'BARANG KELUAR' OR category = 'PENGIRIMAN')
    """)
    fun getTotalOutgoing(): Flow<Int>

    @Query("SELECT COUNT(*) FROM evidence_records WHERE isSoftDeleted = 0 AND timestamp >= :startOfDay AND timestamp <= :endOfDay")
    fun getTotalToday(startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM evidence_records 
        WHERE isSoftDeleted = 0 
        AND (category = 'BARANG RUSAK' OR category = 'BARANG HILANG' OR status = 'FLAGGED')
    """)
    fun getTotalProblematic(): Flow<Int>

    @Query("SELECT COUNT(*) FROM evidence_records")
    suspend fun getTotalHistoricalRecordsCount(): Long

    @Query("SELECT * FROM evidence_records WHERE isSoftDeleted = 0 AND latitude != 0.0 AND longitude != 0.0 ORDER BY timestamp DESC")
    fun getLocationsWithCoordinates(): Flow<List<EvidenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(evidence: EvidenceEntity): Long

    @Update
    suspend fun update(evidence: EvidenceEntity): Int

    @Query("UPDATE evidence_records SET isSoftDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long): Int

    @Query("UPDATE evidence_records SET isSoftDeleted = 0 WHERE id = :id")
    suspend fun restore(id: Long): Int

    @Delete
    suspend fun hardDelete(evidence: EvidenceEntity): Int

    @Query("DELETE FROM evidence_records WHERE id = :id")
    suspend fun hardDeleteById(id: Long): Int

    @Query("SELECT * FROM evidence_records WHERE isSoftDeleted = 0 ORDER BY timestamp DESC")
    suspend fun getAllList(): List<EvidenceEntity>
}

