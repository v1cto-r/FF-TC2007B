package mx.tec.sonrisas.data.local

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class Converters {

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? = value?.toString()

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? = value?.let(LocalDateTime::parse)

    // Activity images are local paths/URIs; a plain separator is enough for
    // a list of text, no need for JSON.
    @TypeConverter
    fun fromStringList(value: List<String>?): String? = value?.joinToString(separator = "|")

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        if (value.isNullOrEmpty()) emptyList() else value.split("|")
}
