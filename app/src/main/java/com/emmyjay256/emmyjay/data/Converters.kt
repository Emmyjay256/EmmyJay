package com.emmyjay256.emmyjay.data

import androidx.room.TypeConverter
import java.time.LocalTime

class Converters {

    @TypeConverter
    fun localTimeToString(t: LocalTime?): String? = t?.toString()

    @TypeConverter
    fun stringToLocalTime(s: String?): LocalTime? =
        s?.let { LocalTime.parse(it) }

    @TypeConverter
    fun categoryToString(c: TaskCategory?): String? = c?.name

    @TypeConverter
    fun stringToCategory(s: String?): TaskCategory? =
        s?.let { TaskCategory.valueOf(it) }
}