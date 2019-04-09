package org.projectforge.rest.calendar

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import java.time.*
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

/**
 * Holder of [LocalDateTime] for transforming to [java.util.Date] (once) if used several times.
 */
class ZonedDateTimeHolder(val dateTime: ZonedDateTime) {

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

    fun isBefore(other: ZonedDateTimeHolder): Boolean {
        return dateTime.isBefore(other.dateTime)
    }

    fun isAfter(other: ZonedDateTimeHolder): Boolean {
        return dateTime.isAfter(other.dateTime)
    }

    fun month(): Month {
        return dateTime.month
    }

    fun daysBetween(other: ZonedDateTimeHolder): Long {
        return ChronoUnit.DAYS.between(dateTime, other.dateTime)
    }

    fun plusDays(days: Long): ZonedDateTimeHolder {
        return ZonedDateTimeHolder(dateTime.plusDays(days))
    }

    fun getBeginOfMonth(): ZonedDateTimeHolder {
        return ZonedDateTimeHolder(CalDateUtils.getBeginOfDay(dateTime.withDayOfMonth(1)))
    }

    fun getEndOfMonth(): ZonedDateTimeHolder {
        val nextMonth = dateTime.plusMonths(1).withDayOfMonth(1)
        return ZonedDateTimeHolder(CalDateUtils.getBeginOfDay(nextMonth.withDayOfMonth(1)))
    }

    fun getBeginOfWeek(): ZonedDateTimeHolder {
        val startOfWeek = CalDateUtils.getBeginOfWeek(this.dateTime)
        return ZonedDateTimeHolder(startOfWeek)
    }

    fun getEndOfWeek(): ZonedDateTimeHolder {
        val startOfWeek = CalDateUtils.getBeginOfWeek(this.dateTime).plusDays(7)
        return ZonedDateTimeHolder(startOfWeek)
    }

    fun getBeginOfDay(): ZonedDateTimeHolder {
        val startOfDay = CalDateUtils.getBeginOfDay(dateTime)
        return ZonedDateTimeHolder(startOfDay)
    }

    fun getEndOfDay(): ZonedDateTimeHolder {
        val endOfDay = CalDateUtils.getEndOfDay(dateTime)
        return ZonedDateTimeHolder(endOfDay)
    }

    companion object {
        fun from(zonedDateTime: ZonedDateTime?): ZonedDateTimeHolder {
            if (zonedDateTime == null)
                return now()
            return ZonedDateTimeHolder(zonedDateTime)
        }

        fun from(localDateTime: LocalDateTime?): ZonedDateTimeHolder {
            if (localDateTime == null)
                return now()
            return ZonedDateTimeHolder(ZonedDateTime.of(localDateTime, CalDateUtils.getUsersZoneId()))
        }

        /**
         * Creates mindnight [LocalDateTime] from given [LocalDate].
         */
        fun from(localDate: LocalDate?): ZonedDateTimeHolder {
            if (localDate == null)
                return now()
            val localDateTime = LocalDateTime.of(localDate, LocalTime.MIDNIGHT)
            return ZonedDateTimeHolder(ZonedDateTime.of(localDateTime, CalDateUtils.getUsersZoneId()))
        }

        /**
         * Creates mindnight [LocalDateTime] from given [LocalDate].
         */
        fun from(date: java.util.Date?): ZonedDateTimeHolder {
            if (date == null)
                return now()
            return ZonedDateTimeHolder(CalDateUtils.convertToZonedDateTime(date)!!)
        }

        fun now(): ZonedDateTimeHolder {
            return ZonedDateTimeHolder(ZonedDateTime.now(CalDateUtils.getUsersZoneId()))
        }
    }
}