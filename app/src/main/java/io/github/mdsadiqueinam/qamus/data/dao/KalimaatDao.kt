package io.github.mdsadiqueinam.qamus.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.mdsadiqueinam.qamus.data.model.Kalimaat
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
    fun getEntries(): PagingSource<Int, Kalimaat>

    /**
     * Get a specific dictionary entry by ID.
     */
    @Query("SELECT * FROM kalimaat WHERE id = :id")
    suspend fun getEntryById(id: Long): Kalimaat?

    /**
     * Search for dictionary entries by huroof (word) as a PagingSource for pagination.
     */
    @Query("SELECT * FROM kalimaat WHERE huroof LIKE '%' || :searchQuery || '%' ORDER BY huroof ASC")
    fun searchEntries(searchQuery: String): PagingSource<Int, Kalimaat>

    /**
     * Get all entries of a specific type as a PagingSource for pagination.
     */
    @Query("SELECT * FROM kalimaat WHERE type = :type ORDER BY huroof ASC")
    fun getEntriesByType(type: WordType): PagingSource<Int, Kalimaat>

    /**
     * Insert a new dictionary entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: Kalimaat): Long

    /**
     * Insert multiple dictionary entries.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<Kalimaat>): List<Long>

    /**
     * Update an existing dictionary entry.
     */
    @Update
    suspend fun updateEntry(entry: Kalimaat)

    /**
     * Delete a dictionary entry.
     */
    @Delete
    suspend fun deleteEntry(entry: Kalimaat)

    /**
     * Delete a dictionary entry by ID.
     */
    @Query("DELETE FROM kalimaat WHERE id = :id")
    suspend fun deleteEntryById(id: Long)
}
