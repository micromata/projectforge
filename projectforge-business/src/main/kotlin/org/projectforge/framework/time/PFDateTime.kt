package org.projectforge.framework.time

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.*
import java.time.ZonedDateTime



/**
 * Immutable holder of [ZoneDateTime] for transforming to [java.util.Date] (once) if used several times.
 * Zone date times will be generated automatically with the context user's time zone.
 */
class PFDateTime {
    private constructor(dateTime: ZonedDateTime) {
        this.dateTime = dateTime
    }

    val dateTime: ZonedDateTime
    private var date: java.util.Date? = null
    private var sqlDate: java.sql.Date? = null
    private var sqlTimestamp: java.sql.Timestamp? = null
    private var localDate: LocalDate? = null

    fun asUtilDate(): java.util.Date {
        if (date == null)
            date = java.util.Date.from(dateTime.toInstant());
        return date!!
    }

    fun asSqlTimestamp(): java.sql.Timestamp {
        if (sqlTimestamp == null)
            sqlTimestamp = java.sql.Timestamp.from(dateTime.toInstant());
        return sqlTimestamp!!
    }

    fun asLocalDate(): LocalDate {
        if (localDate == null)
            localDate = dateTime.toLocalDate()
        return localDate!!
    }

    /**
     * Date part as ISO string: "yyyy-MM-dd".
     */
    fun dateAsIsoString() :String {
        return isoDateFormatter.format(dateTime)
    }

    /**
     * Date part as ISO string: "yyyy-MM-dd HH:mm".
     */
    fun dateTimeAsIsoString() :String {
        return isoDateTimeFormatterMinutes.format(dateTime)
    }

    fun getZone(): ZoneId {
        return dateTime.zone
    }

    fun getTimeZone(): TimeZone {
        return TimeZone.getTimeZone(dateTime.zone)
    }

    fun isBefore(other: PFDateTime): Boolean {
        return dateTime.isBefore(other.dateTime)
    }

    fun isAfter(other: PFDateTime): Boolean {
        return dateTime.isAfter(other.dateTime)
    }

    fun month(): Month {
        return dateTime.month
    }

    fun daysBetween(other: PFDateTime): Long {
        return ChronoUnit.DAYS.between(dateTime, other.dateTime)
    }

    fun plusDays(days: Long): PFDateTime {
        return PFDateTime(dateTime.plusDays(days))
    }

    fun getBeginOfMonth(): PFDateTime {
        return PFDateTime(PFDateTimeUtils.getBeginOfDay(dateTime.withDayOfMonth(1)))
    }

    fun getEndOfMonth(): PFDateTime {
        val nextMonth = dateTime.plusMonths(1).withDayOfMonth(1)
        return PFDateTime(PFDateTimeUtils.getBeginOfDay(nextMonth.withDayOfMonth(1)))
    }

    fun getBeginOfWeek(): PFDateTime {
        val startOfWeek = PFDateTimeUtils.getBeginOfWeek(this.dateTime)
        return PFDateTime(startOfWeek)
    }

    fun getEndOfWeek(): PFDateTime {
        val startOfWeek = PFDateTimeUtils.getBeginOfWeek(this.dateTime).plusDays(7)
        return PFDateTime(startOfWeek)
    }

    fun getBeginOfDay(): PFDateTime {
        val startOfDay = PFDateTimeUtils.getBeginOfDay(dateTime)
        return PFDateTime(startOfDay)
    }

    fun getEndOfDay(): PFDateTime {
        val endOfDay = PFDateTimeUtils.getEndOfDay(dateTime)
        return PFDateTime(endOfDay)
    }

    companion object {
        /**
         * Sets the user's time zone.
         */
        @JvmStatic
        fun from(epochMillis : Long?): PFDateTime {
            if (epochMillis == null)
                return now()
            val instant = Instant.ofEpochMilli(epochMillis)
            return PFDateTime(ZonedDateTime.ofInstant(instant, getUsersZoneId()))
        }

        /**
         * Sets the user's time zone.
         */
        @JvmStatic
        fun from(localDateTime: LocalDateTime?): PFDateTime {
            if (localDateTime == null)
                return now()
            return PFDateTime(ZonedDateTime.of(localDateTime, getUsersZoneId()))
        }

        /**
         * Creates mindnight [ZonedDateTime] from given [LocalDate].
         */
        @JvmStatic
        fun from(localDate: LocalDate?): PFDateTime {
            if (localDate == null)
                return now()
            val localDateTime = LocalDateTime.of(localDate, LocalTime.MIDNIGHT)
            return PFDateTime(ZonedDateTime.of(localDateTime, getUsersZoneId()))
        }

        /**
         * Creates mindnight [ZonedDateTime] from given [LocalDate].
         */
        @JvmStatic
        fun from(date: java.util.Date?): PFDateTime {
            if (date == null)
                return now()
            val dateTime = date.toInstant().atZone(getUsersZoneId());
            return PFDateTime(dateTime)
        }

        /**
         * Creates mindnight [ZonedDateTime] from given [LocalDate].
         */
        @JvmStatic
        fun from(date: java.sql.Date?): PFDateTime {
            if (date == null)
                return now()
            val dateTime = date.toInstant().atZone(getUsersZoneId());
            return PFDateTime(dateTime)
        }

        @JvmStatic
        fun now(): PFDateTime {
            return PFDateTime(ZonedDateTime.now(getUsersZoneId()))
        }

        @JvmStatic
        fun getUsersZoneId(): ZoneId {
            return ThreadLocalUserContext.getTimeZone().toZoneId()
        }

        /**
         * Parses the given date as UTC and converts it to the user's zoned date time.
         */
        @JvmStatic
        fun parseUTCDate(str: String?, dateTimeFormatter : DateTimeFormatter): PFDateTime? {
            if (str.isNullOrBlank())
                return null
            try {
                val local =  LocalDateTime.parse(str, dateTimeFormatter) // Parses UTC as local date.
                val utcZoned = ZonedDateTime.of(local, ZoneId.of("UTC"))
                val userZoned = utcZoned.withZoneSameInstant(getUsersZoneId())
                return PFDateTime(userZoned)
            } catch (ex: DateTimeParseException) {
                log.error("Error while parsing date '$str': ${ex.message}.")
                return null
            }

        }

        private val log = org.slf4j.LoggerFactory.getLogger(PFDateTime::class.java)

        private val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val isoDateTimeFormatterMinutes = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
       // private val jsonDateTimeFormatter = DateTimeFormatter.ofPattern(DateTimeFormat.JS_DATE_TIME_MILLIS.pattern)
    }
}