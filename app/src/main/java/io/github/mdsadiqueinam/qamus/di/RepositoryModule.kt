package io.github.mdsadiqueinam.qamus.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.mdsadiqueinam.qamus.data.dao.KalimaatDao
import io.github.mdsadiqueinam.qamus.data.repository.KalimaatRepository
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
}