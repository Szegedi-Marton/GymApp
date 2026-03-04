package com.example.gymapp.di

import android.content.Context
import androidx.room.Room
import com.example.gymapp.data.local.GymDatabase
import com.example.gymapp.data.local.WorkoutDao
import com.example.gymapp.data.repository.WorkoutRepositoryImpl
import com.example.gymapp.domain.repository.WorkoutRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkoutDataModule {

    @Provides
    @Singleton
    fun provideGymDatabase(@ApplicationContext context: Context): GymDatabase =
        Room.databaseBuilder(
            context,
            GymDatabase::class.java,
            "gym_database",
        ).build()

    @Provides
    fun provideWorkoutDao(gymDatabase: GymDatabase): WorkoutDao = gymDatabase.workoutDao()

    @Provides
    @Singleton
    fun provideWorkoutRepository(workoutDao: WorkoutDao): WorkoutRepository =
        WorkoutRepositoryImpl(workoutDao)
}
