package com.example.gymapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [WorkoutSession::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(ExerciseListConverter::class)
abstract class GymDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
}
