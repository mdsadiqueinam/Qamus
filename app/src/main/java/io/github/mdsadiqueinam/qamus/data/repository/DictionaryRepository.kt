package io.github.mdsadiqueinam.qamus.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import io.github.mdsadiqueinam.qamus.data.dao.DictionaryDao
import io.github.mdsadiqueinam.qamus.data.model.DictionaryEntry
import io.github.mdsadiqueinam.qamus.data.model.WordType
import kotlinx.coroutines.flow.Flow

/**
 * Repository for accessing dictionary data.
 */
class DictionaryRepository(private val dictionaryDao: DictionaryDao) {

    // Default page size for pagination
    private val defaultPageSize = 20

    /**
     * Get all dictionary entries.
     */
    fun getAllEntries(): Flow<List<DictionaryEntry>> {
        return dictionaryDao.getAllEntries()
    }

    /**
     * Get all dictionary entries with pagination.
     */
    fun getEntriesPaged(): Flow<PagingData<DictionaryEntry>> {
        return Pager(
            config = PagingConfig(
                pageSize = defaultPageSize,
                enablePlaceholders = true
            ),
            pagingSourceFactory = { dictionaryDao.getEntriesPaged() }
        ).flow
    }

    /**
     * Get a specific dictionary entry by ID.
     */
    suspend fun getEntryById(id: Long): DictionaryEntry? {
        return dictionaryDao.getEntryById(id)
    }

    /**
     * Search for dictionary entries by kalima (word).
     */
    fun searchEntries(searchQuery: String): Flow<List<DictionaryEntry>> {
        return dictionaryDao.searchEntries(searchQuery)
    }

    /**
     * Search for dictionary entries by kalima (word) with pagination.
     */
    fun searchEntriesPaged(searchQuery: String): Flow<PagingData<DictionaryEntry>> {
        return Pager(
            config = PagingConfig(
                pageSize = defaultPageSize,
                enablePlaceholders = true
            ),
            pagingSourceFactory = { dictionaryDao.searchEntriesPaged(searchQuery) }
        ).flow
    }

    /**
     * Get all entries of a specific type.
     */
    fun getEntriesByType(type: WordType): Flow<List<DictionaryEntry>> {
        return dictionaryDao.getEntriesByType(type)
    }

    /**
     * Get all entries of a specific type with pagination.
     */
    fun getEntriesByTypePaged(type: WordType): Flow<PagingData<DictionaryEntry>> {
        return Pager(
            config = PagingConfig(
                pageSize = defaultPageSize,
                enablePlaceholders = true
            ),
            pagingSourceFactory = { dictionaryDao.getEntriesByTypePaged(type) }
        ).flow
    }

    /**
     * Get all entries derived from a specific root word.
     */
    fun getEntriesByRootId(rootId: Long): Flow<List<DictionaryEntry>> {
        return dictionaryDao.getEntriesByRootId(rootId)
    }

    /**
     * Insert a new dictionary entry.
     */
    suspend fun insertEntry(entry: DictionaryEntry): Long {
        return dictionaryDao.insertEntry(entry)
    }

    /**
     * Insert multiple dictionary entries.
     */
    suspend fun insertEntries(entries: List<DictionaryEntry>): List<Long> {
        return dictionaryDao.insertEntries(entries)
    }

    /**
     * Update an existing dictionary entry.
     */
    suspend fun updateEntry(entry: DictionaryEntry) {
        dictionaryDao.updateEntry(entry)
    }

    /**
     * Delete a dictionary entry.
     */
    suspend fun deleteEntry(entry: DictionaryEntry) {
        dictionaryDao.deleteEntry(entry)
    }

    /**
     * Delete a dictionary entry by ID.
     */
    suspend fun deleteEntryById(id: Long) {
        dictionaryDao.deleteEntryById(id)
    }
}
