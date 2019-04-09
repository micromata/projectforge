package org.projectforge.rest.calendar

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.temporal.ChronoUnit

/**
 * Holder of [LocalDateTime] for transforming to [java.util.Date] (once) if used several times.
 */
class LocalDateTimeHolder(val dateTime: LocalDateTime) {

    private var date: java.util.Date? = null
    private var localDate: LocalDate? = null

    fun asUtilDate(): java.util.Date {
        if (date == null)
            date = CalDateUtils.getUtilDate(dateTime)
        return date!!
    }

    fun asLocalDate(): LocalDate {
        if (localDate == null)
            localDate = dateTime.toLocalDate()
        return localDate!!
    }

    fun isBefore(other: LocalDateTimeHolder): Boolean {
        return dateTime.isBefore(other.dateTime)
    }

    fun isAfter(other: LocalDateTimeHolder): Boolean {
        return dateTime.isAfter(other.dateTime)
    }

    fun month(): Month {
        return dateTime.month
    }

    fun daysBetween(other: LocalDateTimeHolder): Long {
        return ChronoUnit.DAYS.between(dateTime, other.dateTime)
    }

    fun plusDays(days: Long): LocalDateTimeHolder {
        return LocalDateTimeHolder(dateTime.plusDays(days))
    }

    fun getStartOfMonth(): LocalDateTimeHolder {
        return LocalDateTimeHolder(CalDateUtils.getBeginOfDay(dateTime.withDayOfMonth(1)))
    }

    fun getEndOfMonth(): LocalDateTimeHolder {
        val lastDayOfMonth = dateTime.toLocalDate().withDayOfMonth(1).plusMonths(1).minusDays(1)
        val endOfMonth = LocalDateTime.of(lastDayOfMonth, LocalTime.MAX)
        return LocalDateTimeHolder(endOfMonth)
    }

    fun getStartOfWeek(): LocalDateTimeHolder {
        val startOfWeek = CalDateUtils.getBeginOfWeek(this.dateTime)
        return LocalDateTimeHolder(startOfWeek)
    }

    fun getBeginOfDay(): LocalDateTimeHolder {
        val startOfDay = CalDateUtils.getBeginOfDay(dateTime)
        return LocalDateTimeHolder(startOfDay)
    }

    companion object {
        fun from(localDateTime: LocalDateTime?): LocalDateTimeHolder {
            if (localDateTime == null)
                return LocalDateTimeHolder(LocalDateTime.now())
            return LocalDateTimeHolder(localDateTime)
        }

        /**
         * Creates mindnight [LocalDateTime] from given [LocalDate].
         */
        fun from(localDate: LocalDate?): LocalDateTimeHolder {
            if (localDate == null)
                return LocalDateTimeHolder(LocalDateTime.now())
            return LocalDateTimeHolder(LocalDateTime.of(localDate, LocalTime.MIDNIGHT))
        }

        /**
         * Creates mindnight [LocalDateTime] from given [LocalDate].
         */
        fun from(date: java.util.Date?): LocalDateTimeHolder {
            if (date == null)
                return LocalDateTimeHolder(LocalDateTime.now())
            return LocalDateTimeHolder(CalDateUtils.convertToLocalDateTime(date)!!)
        }

        fun now(): LocalDateTimeHolder {
            return LocalDateTimeHolder(LocalDateTime.now())
        }
    }
}