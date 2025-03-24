package io.github.mdsadiqueinam.qamus

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import io.github.mdsadiqueinam.qamus.service.KalimaReminderScheduler
import io.github.mdsadiqueinam.qamus.service.KalimaWorkerFactory
import javax.inject.Inject

/**
 * Application class for Qamus.
 * 
 * The @HiltAndroidApp annotation triggers Hilt's code generation and
 * serves as the application-level dependency container.
 */
@HiltAndroidApp
class QamusApplication : Application() {

    @Inject
    lateinit var workerFactory: KalimaWorkerFactory

    @Inject
    lateinit var kalimaReminderScheduler: KalimaReminderScheduler

    override fun onCreate() {
        super.onCreate()

        // Initialize WorkManager with our custom factory
        val config = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
        WorkManager.initialize(this, config)

        // Schedule the Kalima reminder
        kalimaReminderScheduler.startScheduling()
    }
}
