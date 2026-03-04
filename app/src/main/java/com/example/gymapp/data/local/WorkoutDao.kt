package com.example.gymapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(userSession: UserSession)

    @Query("SELECT * FROM user_sessions ORDER BY id DESC")
    fun observeSessions(): Flow<List<UserSession>>
}
