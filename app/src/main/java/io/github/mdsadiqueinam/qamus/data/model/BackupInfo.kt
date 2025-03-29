package io.github.mdsadiqueinam.qamus.data.model

import kotlinx.datetime.Instant

data class BackupInfo(
    val id: String,
    val backupAt: Instant,
)
