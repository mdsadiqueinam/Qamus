package io.github.mdsadiqueinam.qamus.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for providing repository-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule
