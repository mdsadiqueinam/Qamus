package io.github.mdsadiqueinam.qamus.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.mdsadiqueinam.qamus.data.model.DictionaryEntry
import io.github.mdsadiqueinam.qamus.data.model.WordType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the dictionary_entries table.
 */
@Dao
interface DictionaryDao {
    /**
     * Get all dictionary entries as a Flow.
     */
    @Query("SELECT * FROM dictionary_entries ORDER BY kalima ASC")
    fun getAllEntries(): Flow<List<DictionaryEntry>>

    /**
     * Get a specific dictionary entry by ID.
     */
    @Query("SELECT * FROM dictionary_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): DictionaryEntry?

    /**
     * Search for dictionary entries by kalima (word).
     */
    @Query("SELECT * FROM dictionary_entries WHERE kalima LIKE '%' || :searchQuery || '%' ORDER BY kalima ASC")
    fun searchEntries(searchQuery: String): Flow<List<DictionaryEntry>>

    /**
     * Get all entries of a specific type.
     */
    @Query("SELECT * FROM dictionary_entries WHERE type = :type ORDER BY kalima ASC")
    fun getEntriesByType(type: WordType): Flow<List<DictionaryEntry>>

    /**
     * Get all entries derived from a specific root word.
     */
    @Query("SELECT * FROM dictionary_entries WHERE rootId = :rootId ORDER BY kalima ASC")
    fun getEntriesByRootId(rootId: Long): Flow<List<DictionaryEntry>>

    /**
     * Insert a new dictionary entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: DictionaryEntry): Long

    /**
     * Insert multiple dictionary entries.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<DictionaryEntry>): List<Long>

    /**
     * Update an existing dictionary entry.
     */
    @Update
    suspend fun updateEntry(entry: DictionaryEntry)

    /**
     * Delete a dictionary entry.
     */
    @Delete
    suspend fun deleteEntry(entry: DictionaryEntry)

    /**
     * Delete a dictionary entry by ID.
     */
    @Query("DELETE FROM dictionary_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)
}