package io.github.mdsadiqueinam.qamus.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.mdsadiqueinam.qamus.data.repository.BackupRestoreRepository
import javax.inject.Singleton

/**
 * Hilt module for providing repository-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
}
