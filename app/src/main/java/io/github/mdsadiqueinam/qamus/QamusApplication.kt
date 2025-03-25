package io.github.mdsadiqueinam.qamus

import android.app.Application
import androidx.work.Configuration
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
class QamusApplication() : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: KalimaWorkerFactory

    @Inject
    lateinit var kalimaReminderScheduler: KalimaReminderScheduler

    override val workManagerConfiguration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Schedule the Kalima reminder
        kalimaReminderScheduler.startScheduling()
    }
}
