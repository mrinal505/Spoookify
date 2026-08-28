package com.spoookify.data.local.database

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromString(value: String): List<Float> {
        if (value.isEmpty()) return emptyList()
        return value.split(",").map { it.toFloat() }
    }

    @TypeConverter
    fun fromList(list: List<Float>): String {
        return list.joinToString(",")
    }
}
