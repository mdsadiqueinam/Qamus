package io.github.mdsadiqueinam.qamus.data.database

import androidx.room.TypeConverter
import io.github.mdsadiqueinam.qamus.data.model.WordType

/**
 * Type converters for Room database.
 */
class Converters {
    /**
     * Convert WordType enum to String for storage in the database.
     */
    @TypeConverter
    fun fromWordType(wordType: WordType): String {
        return wordType.name
    }

    /**
     * Convert String from the database to WordType enum.
     */
    @TypeConverter
    fun toWordType(value: String): WordType {
        return WordType.valueOf(value)
    }
}