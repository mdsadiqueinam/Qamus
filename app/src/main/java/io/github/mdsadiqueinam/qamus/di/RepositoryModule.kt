package io.github.mdsadiqueinam.qamus.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.mdsadiqueinam.qamus.data.repository.BackupRestoreRepository
import io.github.mdsadiqueinam.qamus.data.repository.GoogleDriveBackupRestoreRepository
import javax.inject.Singleton

/**
 * Hilt module for providing repository-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds the GoogleDriveBackupRestoreRepository implementation to the BackupRestoreRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindBackupRestoreRepository(
        repository: GoogleDriveBackupRestoreRepository
    ): BackupRestoreRepository
}
