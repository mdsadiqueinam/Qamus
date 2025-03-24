package io.github.mdsadiqueinam.qamus.service

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Custom WorkerFactory for creating KalimaReminderWorker instances with dependencies.
 */
@Singleton
class KalimaWorkerFactory @Inject constructor(
    private val kalimaReminderWorkerFactory: KalimaReminderWorker.Factory
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            KalimaReminderWorker::class.java.name -> 
                kalimaReminderWorkerFactory.create(appContext, workerParameters)
            else -> null
        }
    }
}