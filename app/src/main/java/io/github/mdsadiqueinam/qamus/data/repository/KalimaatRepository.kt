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
     * Get all dictionary entries.
     */
    fun getAllEntries(): Flow<List<Kalimaat>> {
        return kalimaatDao.getAllEntries()
    }

    /**
     * Get all dictionary entries with pagination.
     */
    fun getEntriesPaged(): Flow<PagingData<Kalimaat>> {
        return Pager(
            config = PagingConfig(
                pageSize = defaultPageSize,
                enablePlaceholders = true
            ),
            pagingSourceFactory = { kalimaatDao.getEntriesPaged() }
        ).flow
    }

    /**
     * Get a specific dictionary entry by ID.
     */
    suspend fun getEntryById(id: Long): Kalimaat? {
        return kalimaatDao.getEntryById(id)
    }

    /**
     * Search for dictionary entries by kalima (word).
     */
    fun searchEntries(searchQuery: String): Flow<List<Kalimaat>> {
        return kalimaatDao.searchEntries(searchQuery)
    }

    /**
     * Search for dictionary entries by kalima (word) with pagination.
     */
    fun searchEntriesPaged(searchQuery: String): Flow<PagingData<Kalimaat>> {
        return Pager(
            config = PagingConfig(
                pageSize = defaultPageSize,
                enablePlaceholders = true
            ),
            pagingSourceFactory = { kalimaatDao.searchEntriesPaged(searchQuery) }
        ).flow
    }

    /**
     * Get all entries of a specific type.
     */
    fun getEntriesByType(type: WordType): Flow<List<Kalimaat>> {
        return kalimaatDao.getEntriesByType(type)
    }

    /**
     * Get all entries of a specific type with pagination.
     */
    fun getEntriesByTypePaged(type: WordType): Flow<PagingData<Kalimaat>> {
        return Pager(
            config = PagingConfig(
                pageSize = defaultPageSize,
                enablePlaceholders = true
            ),
            pagingSourceFactory = { kalimaatDao.getEntriesByTypePaged(type) }
        ).flow
    }

    /**
     * Get all entries derived from a specific root word.
     */
    fun getEntriesByRootId(rootId: Long): Flow<List<Kalimaat>> {
        return kalimaatDao.getEntriesByRootId(rootId)
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
}
