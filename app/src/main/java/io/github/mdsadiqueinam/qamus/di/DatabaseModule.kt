package io.github.mdsadiqueinam.qamus.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.mdsadiqueinam.qamus.data.dao.KalimaatDao
import io.github.mdsadiqueinam.qamus.data.database.QamusDatabase
import javax.inject.Singleton

/**
 * Hilt module for providing database-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the QamusDatabase instance.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): QamusDatabase {
        return Room.databaseBuilder(
            context,
            QamusDatabase::class.java,
            "qamus_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    /**
     * Provides the KalimaatDao instance.
     */
    @Provides
    fun provideKalimaatDao(database: QamusDatabase): KalimaatDao {
        return database.dictionaryDao()
    }
}