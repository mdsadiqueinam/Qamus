package io.github.mdsadiqueinam.qamus.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.mdsadiqueinam.qamus.data.dao.DictionaryDao
import io.github.mdsadiqueinam.qamus.data.model.DictionaryEntry

/**
 * Room database for the Qamus application.
 */
@Database(
    entities = [DictionaryEntry::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class QamusDatabase : RoomDatabase() {

    /**
     * Get the DictionaryDao.
     */
    abstract fun dictionaryDao(): DictionaryDao

    companion object {
        @Volatile
        private var INSTANCE: QamusDatabase? = null

        /**
         * Get the singleton instance of the database.
         */
        fun getDatabase(context: Context): QamusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QamusDatabase::class.java,
                    "qamus_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}