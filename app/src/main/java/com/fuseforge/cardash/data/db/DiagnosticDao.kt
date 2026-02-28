package com.fuseforge.cardash.data.db

import androidx.room.*

@Dao
interface DiagnosticDao {
    @Query("SELECT * FROM diagnostic_codes WHERE code = :code")
    suspend fun getDiagnosticByCode(code: String): DiagnosticCode?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiagnosticCodes(codes: List<DiagnosticCode>)

    @Query("SELECT COUNT(*) FROM diagnostic_codes")
    suspend fun getCount(): Int
}
