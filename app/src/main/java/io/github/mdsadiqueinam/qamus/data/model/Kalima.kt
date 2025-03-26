package io.github.mdsadiqueinam.qamus.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity class representing a kalimaat (كَلِمَات) entry in the Arabic dictionary.
 *
 * @property id The unique identifier for the entry
 * @property huroof The Arabic word
 * @property meaning The meaning/translation of the word
 * @property desc Additional description or information about the word
 * @property type The type of the word (noun, verb, particle)
 * @property rootId Optional reference to the root word's id (self-referential)
 */
@Entity(
    tableName = "kalimaat",
    foreignKeys = [
        ForeignKey(
            entity = Kalima::class,
            parentColumns = ["id"],
            childColumns = ["rootId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["rootId"]),
        Index(value = ["huroof"], unique = true)
    ]
)
data class Kalima(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val huroof: String,
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