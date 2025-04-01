package io.github.mdsadiqueinam.qamus.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.mdsadiqueinam.qamus.data.dao.KalimaatDao
import io.github.mdsadiqueinam.qamus.data.model.Kalima

/**
 * Room database for the Qamus application.
 */
@Database(
    entities = [Kalima::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class QamusDatabase : RoomDatabase() {

    /**
     * Get the DictionaryDao.
     */
    abstract fun dictionaryDao(): KalimaatDao
}