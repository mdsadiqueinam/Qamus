package io.github.mdsadiqueinam.qamus.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import io.github.mdsadiqueinam.qamus.data.dao.KalimaatDao
import io.github.mdsadiqueinam.qamus.data.model.Kalimaat
import io.github.mdsadiqueinam.qamus.data.model.WordType
import kotlinx.coroutines.flow.Flow

/**
 * Repository for accessing dictionary data.
 */
class KalimaatRepository(private val kalimaatDao: KalimaatDao) {

    // Default page size for pagination
    private val defaultPageSize = 20

    /**
     * Get all dictionary entries with pagination.
     */
    fun getEntries(): Flow<PagingData<Kalimaat>> {
        return Pager(
            config = PagingConfig(
                pageSize = defaultPageSize,
                enablePlaceholders = true
            ),
            pagingSourceFactory = { kalimaatDao.getEntries() }
        ).flow
    }

    /**
     * Get a specific dictionary entry by ID.
     */
    suspend fun getEntryById(id: Long): Kalimaat? {
        return kalimaatDao.getEntryById(id)
    }

    /**
     * Get multiple dictionary entries by IDs.
     */
    suspend fun getEntriesByIds(ids: List<Long>): List<Kalimaat> {
        return kalimaatDao.getEntriesByIds(ids)
    }

    /**
     * Search for dictionary entries by huroof or type
     */
    fun searchEntries(searchQuery: String, type: WordType?): Flow<PagingData<Kalimaat>> {
        return Pager(
            config = PagingConfig(
                pageSize = defaultPageSize,
                enablePlaceholders = true
            ),
            pagingSourceFactory = { kalimaatDao.searchEntries(searchQuery, type) }
        ).flow
    }

    /**
     * Insert a new dictionary entry.
     */
    suspend fun insertEntry(entry: Kalimaat): Long {
        return kalimaatDao.insertEntry(entry)
    }

    /**
     * Insert multiple dictionary entries.
     */
    suspend fun insertEntries(entries: List<Kalimaat>): List<Long> {
        return kalimaatDao.insertEntries(entries)
    }

    /**
     * Update an existing dictionary entry.
     */
    suspend fun updateEntry(entry: Kalimaat) {
        kalimaatDao.updateEntry(entry)
    }

    /**
     * Delete a dictionary entry.
     */
    suspend fun deleteEntry(entry: Kalimaat) {
        kalimaatDao.deleteEntry(entry)
    }

    /**
     * Delete a dictionary entry by ID.
     */
    suspend fun deleteEntryById(id: Long) {
        kalimaatDao.deleteEntryById(id)
    }

    /**
     * Get all dictionary entries as a Flow of List.
     */
    fun getAllEntriesAsList(): Flow<List<Kalimaat>> {
        return kalimaatDao.getAllEntriesAsList()
    }

    /**
     * Get entries by rootId.
     */
    suspend fun getEntriesByRootId(rootId: Long): List<Kalimaat> {
        return kalimaatDao.getEntriesByRootId(rootId)
    }
}
