package io.github.mdsadiqueinam.qamus

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import io.github.mdsadiqueinam.qamus.service.QamusWorkerFactory
import javax.inject.Inject

/**
 * Application class for Qamus.
 *
 * The @HiltAndroidApp annotation triggers Hilt's code generation and
 * serves as the application-level dependency container.
 */
@HiltAndroidApp
class QamusApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: QamusWorkerFactory

    override fun onCreate() {
        super.onCreate()
        setupExceptionHandler()
    }

    private fun setupExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("QamusApplication", "Uncaught exception in thread ${thread.name}", throwable)
            // Here you can add crash reporting logic
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
            .build()
}
