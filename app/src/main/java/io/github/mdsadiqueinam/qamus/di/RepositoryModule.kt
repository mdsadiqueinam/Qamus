package io.github.mdsadiqueinam.qamus.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.mdsadiqueinam.qamus.data.dao.KalimaatDao
import io.github.mdsadiqueinam.qamus.data.repository.KalimaatRepository
import io.github.mdsadiqueinam.qamus.data.repository.SettingsRepository
import io.github.mdsadiqueinam.qamus.service.KalimaReminderScheduler
import javax.inject.Singleton

/**
 * Hilt module for providing repository-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /**
     * Provides the KalimaatRepository instance.
     */
    @Provides
    @Singleton
    fun provideKalimaatRepository(kalimaatDao: KalimaatDao): KalimaatRepository {
        return KalimaatRepository(kalimaatDao)
    }

    /**
     * Provides the SettingsRepository instance.
     */
    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context, scheduler: KalimaReminderScheduler): SettingsRepository {
        return SettingsRepository(context, scheduler)
    }
}
