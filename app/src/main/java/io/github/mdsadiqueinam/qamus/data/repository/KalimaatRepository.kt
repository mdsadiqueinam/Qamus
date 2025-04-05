package io.github.mdsadiqueinam.qamus.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import io.github.mdsadiqueinam.qamus.data.dao.KalimaatDao
import io.github.mdsadiqueinam.qamus.data.model.Kalima
import io.github.mdsadiqueinam.qamus.data.model.WordType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for accessing dictionary data.
 * Follows Single Responsibility Principle by focusing only on dictionary data operations.
 */
@Singleton
class KalimaatRepository @Inject constructor(private val kalimaatDao: KalimaatDao) {

    companion object {
        // Default page size for pagination
        private const val DEFAULT_PAGE_SIZE = 20
    }

    /**
     * Get all dictionary entries with pagination.
     * 
     * @return A Flow of PagingData containing dictionary entries
     */
    fun getEntries(): Flow<PagingData<Kalima>> {
        return Pager(
            config = PagingConfig(
                pageSize = DEFAULT_PAGE_SIZE,
                enablePlaceholders = true
            ),
            pagingSourceFactory = { kalimaatDao.getEntries() }
        ).flow
    }

    /**
     * Get a specific dictionary entry by ID.
     * 
     * @param id The ID of the entry to retrieve
     * @return The entry with the specified ID, or null if not found
     */
    suspend fun getEntryById(id: Long): Kalima? {
        return kalimaatDao.getEntryById(id)
    }

    /**
     * Get multiple dictionary entries by IDs.
     * 
     * @param ids List of entry IDs to retrieve
     * @return List of entries with the specified IDs
     */
    suspend fun getEntriesByIds(ids: List<Long>): List<Kalima> {
        return kalimaatDao.getEntriesByIds(ids)
    }

    /**
     * Search for dictionary entries by huroof or type with pagination.
     * 
     * @param searchQuery The search query to filter entries
     * @param type Optional word type to filter entries
     * @return A Flow of PagingData containing filtered dictionary entries
     */
    fun searchEntries(searchQuery: String, type: WordType?): Flow<PagingData<Kalima>> {
        return Pager(
            config = PagingConfig(
                pageSize = DEFAULT_PAGE_SIZE,
                enablePlaceholders = true
            ),
            pagingSourceFactory = { kalimaatDao.searchEntries(searchQuery, type) }
        ).flow
    }

    /**
     * Insert a new dictionary entry.
     * 
     * @param entry The entry to insert
     * @return The ID of the newly inserted entry
     */
    suspend fun insertEntry(entry: Kalima): Long {
        return kalimaatDao.insertEntry(entry)
    }

    /**
     * Insert multiple dictionary entries.
     * 
     * @param entries List of entries to insert
     * @return List of IDs for the newly inserted entries
     */
    suspend fun insertEntries(entries: List<Kalima>): List<Long> {
        return kalimaatDao.insertEntries(entries)
    }

    /**
     * Update an existing dictionary entry.
     * 
     * @param entry The entry to update
     */
    suspend fun updateEntry(entry: Kalima) {
        kalimaatDao.updateEntry(entry)
    }

    /**
     * Delete a dictionary entry.
     * 
     * @param entry The entry to delete
     */
    suspend fun deleteEntry(entry: Kalima) {
        kalimaatDao.deleteEntry(entry)
    }

    /**
     * Delete a dictionary entry by ID.
     * 
     * @param id The ID of the entry to delete
     */
    suspend fun deleteEntryById(id: Long) {
        kalimaatDao.deleteEntryById(id)
    }

    /**
     * Get all dictionary entries as a Flow of List.
     * 
     * @return A Flow of List containing all dictionary entries
     */
    fun getAllEntriesAsList(): Flow<List<Kalima>> {
        return kalimaatDao.getAllEntriesAsList()
    }

    /**
     * Get entries by rootId.
     * 
     * @param rootId The root ID to filter entries
     * @return List of entries with the specified root ID
     */
    suspend fun getEntriesByRootId(rootId: Long): List<Kalima> {
        return kalimaatDao.getEntriesByRootId(rootId)
    }

    /**
     * Get a random entry from the kalimaat table.
     * 
     * @return A random entry, or null if the table is empty
     */
    suspend fun getRandomEntry(): Kalima? {
        return kalimaatDao.getRandomEntry()
    }
}
