package io.github.mdsadiqueinam.qamus.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Custom WorkerFactory for creating worker instances with dependencies.
 */
@Singleton
class QamusWorkerFactory @Inject constructor(
    private val kalimaReminderWorkerFactory: KalimaReminderWorker.Factory,
    private val automaticBackupWorkerFactory: AutomaticBackupWorker.Factory
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            KalimaReminderWorker::class.java.name ->
                kalimaReminderWorkerFactory.create(appContext, workerParameters)

            AutomaticBackupWorker::class.java.name ->
                automaticBackupWorkerFactory.create(appContext, workerParameters)

            else -> null
        }
    }
}