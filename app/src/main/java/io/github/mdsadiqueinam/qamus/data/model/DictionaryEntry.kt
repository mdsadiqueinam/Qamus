package io.github.mdsadiqueinam.qamus.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Entity class representing a dictionary entry in the Arabic dictionary.
 *
 * @property id The unique identifier for the entry
 * @property kalima The Arabic word
 * @property meaning The meaning/translation of the word
 * @property desc Additional description or information about the word
 * @property type The type of the word (noun, verb, particle)
 * @property rootId Optional reference to the root word's id (self-referential)
 */
@Entity(
    tableName = "dictionary_entries",
    foreignKeys = [
        ForeignKey(
            entity = DictionaryEntry::class,
            parentColumns = ["id"],
            childColumns = ["rootId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class DictionaryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val kalima: String,
    val meaning: String,
    val desc: String,
    val type: WordType,
    val rootId: Long? = null
)

/**
 * Enum representing the types of words in Arabic.
 */
enum class WordType {
    ISM, // اسم (Noun)
    FEEL, // فعل (Verb)
    HARF; // حرف (Particle)

    companion object {
        fun fromArabic(arabicType: String): WordType {
            return when (arabicType) {
                "اسم" -> ISM
                "فعل" -> FEEL
                "حرف" -> HARF
                else -> throw IllegalArgumentException("Unknown word type: $arabicType")
            }
        }

        fun toArabic(type: WordType): String {
            return when (type) {
                ISM -> "اسم"
                FEEL -> "فعل"
                HARF -> "حرف"
            }
        }
    }
}