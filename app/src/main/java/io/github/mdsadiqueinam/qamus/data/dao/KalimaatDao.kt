package io.github.mdsadiqueinam.qamus.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.mdsadiqueinam.qamus.data.model.Kalima
import io.github.mdsadiqueinam.qamus.data.model.WordType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the kalimaat table.
 */
@Dao
interface KalimaatDao {
    /**
     * Get all dictionary entries as a PagingSource for pagination.
     */
    @Query("SELECT * FROM kalimaat ORDER BY huroof ASC")
    fun getEntries(): PagingSource<Int, Kalima>

    /**
     * Get a specific dictionary kalima by ID.
     */
    @Query("SELECT * FROM kalimaat WHERE id = :id")
    suspend fun getEntryById(id: Long): Kalima?

    /**
     * Get multiple kalima by ids.
     */
    @Query("SELECT * FROM kalimaat WHERE id IN (:ids)")
    suspend fun getEntriesByIds(ids: List<Long>): List<Kalima>

    /**
     * Search for dictionary entries by huroof (word) as a PagingSource for pagination.
     */
    @Query("SELECT * FROM kalimaat WHERE (huroof LIKE '%' || :searchQuery || '%') AND (:type IS NULL OR type = :type) ORDER BY huroof ASC")
    fun searchEntries(searchQuery: String, type: WordType?): PagingSource<Int, Kalima>

    /**
     * Get all entries of a specific type as a PagingSource for pagination.
     */
    @Query("SELECT * FROM kalimaat WHERE type = :type ORDER BY huroof ASC")
    fun getEntriesByType(type: WordType): PagingSource<Int, Kalima>

    /**
     * Insert a new dictionary entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: Kalima): Long

    /**
     * Insert multiple dictionary entries.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<Kalima>): List<Long>

    /**
     * Update an existing dictionary entry.
     */
    @Update
    suspend fun updateEntry(entry: Kalima)

    /**
     * Delete a dictionary entry.
     */
    @Delete
    suspend fun deleteEntry(entry: Kalima)

    /**
     * Delete a dictionary entry by ID.
     */
    @Query("DELETE FROM kalimaat WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    /**
     * Get all dictionary entries as a Flow of List.
     */
    @Query("SELECT * FROM kalimaat ORDER BY huroof ASC")
    fun getAllEntriesAsList(): Flow<List<Kalima>>

    /**
     * Get entries by rootId.
     */
    @Query("SELECT * FROM kalimaat WHERE rootId = :rootId ORDER BY huroof ASC")
    suspend fun getEntriesByRootId(rootId: Long): List<Kalima>
}
