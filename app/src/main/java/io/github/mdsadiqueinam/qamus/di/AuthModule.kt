package io.github.mdsadiqueinam.qamus.di

import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing authentication-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    /**
     * Provides the FirebaseAuth instance.
     */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
}