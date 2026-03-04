package com.example.gymapp.data.local

import androidx.room.TypeConverter
import com.example.gymapp.domain.model.Exercise
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ExerciseListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromExerciseList(value: List<Exercise>): String = gson.toJson(value)

    @TypeConverter
    fun toExerciseList(value: String): List<Exercise> {
        val listType = object : TypeToken<List<Exercise>>() {}.type
        return gson.fromJson(value, listType)
    }
}
