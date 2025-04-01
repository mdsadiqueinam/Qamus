package io.github.mdsadiqueinam.qamus.di

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.mdsadiqueinam.qamus.service.QamusWorkerFactory
import javax.inject.Singleton

/**
 * Hilt module for providing WorkManager related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {

    /**
     * Provides the WorkManager configuration with our custom worker factory.
     */
    @Provides
    @Singleton
    fun provideWorkManagerConfiguration(
        workerFactory: QamusWorkerFactory
    ): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }

    /**
     * Provides the WorkManager instance.
     */
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
}
