package com.snehil.cvoptima.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.snehil.cvoptima.data.local.entity.TokenEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TokenDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveToken(tokenEntity: TokenEntity): Long

    @Query("SELECT token FROM token_table WHERE id = 0")
    fun getTokenFlow(): Flow<String?>

    @Query("SELECT token FROM token_table WHERE id = 0")
    fun getTokenSync(): String?

    @Query("DELETE FROM token_table WHERE id = 0")
    suspend fun clearToken(): Int
}
