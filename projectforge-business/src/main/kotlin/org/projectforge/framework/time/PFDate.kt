package org.projectforge.framework.time

import java.time.*
import java.time.temporal.ChronoUnit

/**
 * Immutable holder of [LocalDate] for transforming to [java.sql.Date] (once) if used several times.
 * If you don't need to use [java.sql.Date] you may use [LocalDate] directly.
 */
class PFDate(val date: LocalDate) {

    constructor(instant: Instant) : this(LocalDate.from(instant))

    private var sqlDate: java.sql.Date? = null

    fun asSqlDate(): java.sql.Date {
        if (sqlDate == null) {
            sqlDate = java.sql.Date.valueOf(date)
        }
        return sqlDate!!
    }

    fun isBefore(other: PFDate): Boolean {
        return date.isBefore(other.date)
    }

    fun isAfter(other: PFDate): Boolean {
        return date.isAfter(other.date)
    }

    fun month(): Month {
        return date.month
    }

    fun daysBetween(other: PFDate): Long {
        return ChronoUnit.DAYS.between(date, other.date)
    }

    fun plusDays(days: Long): PFDate {
        return PFDate(date.plusDays(days))
    }

    companion object {
        /**
         * Creates mindnight [ZonedDateTime] from given [LocalDate].
         */
        @JvmStatic
        fun from(localDate: LocalDate?): PFDate {
            if (localDate == null)
                return now()
            return PFDate(localDate)
        }

        /**
         * Creates mindnight [ZonedDateTime] from given [LocalDate].
         */
        @JvmStatic
        fun from(date: java.sql.Date?): PFDate {
            if (date == null)
                return now()
            return PFDate(date.toLocalDate())
        }

        /**
         * Creates mindnight [ZonedDateTime] from given [LocalDate].
         */
        @JvmStatic
        fun from(date: java.util.Date?): PFDate {
            if (date == null)
                return now()
            return PFDate(date.toInstant()
                    .atZone(ZoneId.of("UTC"))
                    .toLocalDate())
        }


        @JvmStatic
        fun now(): PFDate {
            return PFDate(LocalDate.now())
        }
    }
}