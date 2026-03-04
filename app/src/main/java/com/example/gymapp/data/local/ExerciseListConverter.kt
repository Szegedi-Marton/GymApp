package com.example.gymapp.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ExerciseListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromExerciseSetList(value: List<ExerciseSet>): String = gson.toJson(value)

    @TypeConverter
    fun toExerciseSetList(value: String): List<ExerciseSet> {
        val listType = object : TypeToken<List<ExerciseSet>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromLongList(value: List<Long>): String = gson.toJson(value)

    @TypeConverter
    fun toLongList(value: String): List<Long> {
        val listType = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(value, listType)
    }
}
