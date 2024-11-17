/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.framework.calendar;

import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.transform.recurrence.Frequency;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.teamcal.event.RecurrenceFrequencyModeOne;
import org.projectforge.business.teamcal.event.RecurrenceFrequencyModeTwo;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.RecurrenceFrequency;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.List;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class ICal4JUtils {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ICal4JUtils.class);

    private static TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();

    /**
     * @return The timeZone (ical4j) built of the default java timeZone of the user.
     * @see ThreadLocalUserContext#getTimeZone()
     */
    public static TimeZone getUserTimeZone() {
        return registry.getTimeZone(ThreadLocalUserContext.getTimeZone().getID());
    }

    /**
     * @return The timeZone (ical4j) built of the default java timeZone of the user.
     * @see ThreadLocalUserContext#getTimeZone()
     */
    public static TimeZone getTimeZone(final java.util.TimeZone timeZone) {

        try {
            return registry.getTimeZone(timeZone.getID());
        } catch (Exception ex) {
            log.error("Error getting timezone from ical4j.");
            return ICal4JUtils.getUserTimeZone();
        }
    }

    public static WeekDayList getDayListForRecurrenceFrequencyModeTwo(RecurrenceFrequencyModeTwo mode) {
        WeekDayList weekDays = new WeekDayList();
        if (mode == RecurrenceFrequencyModeTwo.MONDAY)
            weekDays.add(WeekDay.MO);
        else if (mode == RecurrenceFrequencyModeTwo.TUESDAY)
            weekDays.add(WeekDay.TU);
        else if (mode == RecurrenceFrequencyModeTwo.WEDNESDAY)
            weekDays.add(WeekDay.WE);
        else if (mode == RecurrenceFrequencyModeTwo.THURSDAY)
            weekDays.add(WeekDay.TH);
        else if (mode == RecurrenceFrequencyModeTwo.FRIDAY)
            weekDays.add(WeekDay.FR);
        else if (mode == RecurrenceFrequencyModeTwo.SATURDAY)
            weekDays.add(WeekDay.SA);
        else if (mode == RecurrenceFrequencyModeTwo.SUNDAY)
            weekDays.add(WeekDay.SU);
        else if (mode == RecurrenceFrequencyModeTwo.WEEKDAY) {
            weekDays.add(WeekDay.MO);
            weekDays.add(WeekDay.TU);
            weekDays.add(WeekDay.WE);
            weekDays.add(WeekDay.TH);
            weekDays.add(WeekDay.FR);
        } else if (mode == RecurrenceFrequencyModeTwo.WEEKEND) {
            weekDays.add(WeekDay.SA);
            weekDays.add(WeekDay.SU);
        } else if (mode == RecurrenceFrequencyModeTwo.DAY) {
            weekDays.add(WeekDay.SA);
            weekDays.add(WeekDay.SU);
            weekDays.add(WeekDay.MO);
            weekDays.add(WeekDay.TU);
            weekDays.add(WeekDay.WE);
            weekDays.add(WeekDay.TH);
            weekDays.add(WeekDay.FR);
        }
        return weekDays;
    }

    public static RecurrenceFrequencyModeTwo getRecurrenceFrequencyModeTwoForDay(List<WeekDay> dayList) {
        if (dayList.size() == 1) {
            for (WeekDay wd : dayList) {
                if (wd.getDay() == WeekDay.MO.getDay()) {
                    return RecurrenceFrequencyModeTwo.MONDAY;
                } else if (wd.getDay() == WeekDay.TU.getDay()) {
                    return RecurrenceFrequencyModeTwo.TUESDAY;
                } else if (wd.getDay() == WeekDay.WE.getDay()) {
                    return RecurrenceFrequencyModeTwo.WEDNESDAY;
                } else if (wd.getDay() == WeekDay.TH.getDay()) {
                    return RecurrenceFrequencyModeTwo.THURSDAY;
                } else if (wd.getDay() == WeekDay.FR.getDay()) {
                    return RecurrenceFrequencyModeTwo.FRIDAY;
                } else if (wd.getDay() == WeekDay.SA.getDay()) {
                    return RecurrenceFrequencyModeTwo.SATURDAY;
                } else if (wd.getDay() == WeekDay.SU.getDay()) {
                    return RecurrenceFrequencyModeTwo.SUNDAY;
                }
            }
        } else if (dayList.size() == 2) {
            return RecurrenceFrequencyModeTwo.WEEKEND;
        } else if (dayList.size() == 5) {
            return RecurrenceFrequencyModeTwo.WEEKDAY;
        } else if (dayList.size() == 7) {
            return RecurrenceFrequencyModeTwo.DAY;
        }
        return null;
    }

    public static RecurrenceFrequencyModeOne getRecurrenceFrequencyModeOneByOffset(int offset) {
        if (offset == 1 || offset == 0) {
            return RecurrenceFrequencyModeOne.FIRST;
        } else if (offset == 2) {
            return RecurrenceFrequencyModeOne.SECOND;
        } else if (offset == 3) {
            return RecurrenceFrequencyModeOne.THIRD;
        } else if (offset == 4) {
            return RecurrenceFrequencyModeOne.FOURTH;
        } else if (offset == 5) {
            return RecurrenceFrequencyModeOne.FIFTH;
        } else if (offset == -1) {
            return RecurrenceFrequencyModeOne.LAST;
        }
        return null;
    }

    public static int getOffsetForRecurrenceFrequencyModeOne(RecurrenceFrequencyModeOne mode) {
        if (mode == RecurrenceFrequencyModeOne.FIRST)
            return 1;
        else if (mode == RecurrenceFrequencyModeOne.SECOND)
            return 2;
        else if (mode == RecurrenceFrequencyModeOne.THIRD)
            return 3;
        else if (mode == RecurrenceFrequencyModeOne.FOURTH)
            return 3;
        else if (mode == RecurrenceFrequencyModeOne.FIFTH)
            return 5;
        else
            return -1;
    }

    /**
     * @param rruleString
     * @return null if rruleString is empty, otherwise new RRule object.
     */
    public static RRule<Temporal> calculateRRule(final String rruleString) {
        if (StringUtils.isBlank(rruleString)) {
            return null;
        }
        try {
            return new RRule(rruleString);
        } catch (final Exception ex) {
            log.error("Exception encountered while parsing rrule '" + rruleString + "': " + ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * @param interval
     * @return
     */
    public static Frequency getCal4JFrequency(final RecurrenceFrequency interval) {
        if (interval == RecurrenceFrequency.DAILY) {
            return Frequency.DAILY;
        } else if (interval == RecurrenceFrequency.WEEKLY) {
            return Frequency.WEEKLY;
        } else if (interval == RecurrenceFrequency.MONTHLY) {
            return Frequency.MONTHLY;
        } else if (interval == RecurrenceFrequency.YEARLY) {
            return Frequency.YEARLY;
        }
        return null;
    }

    /**
     * @param recur
     * @return
     */
    public static RecurrenceFrequency getFrequency(final Recur recur) {
        if (recur == null) {
            return null;
        }
        final Frequency freq = recur.getFrequency();
        if (Frequency.WEEKLY.equals(freq)) {
            return RecurrenceFrequency.WEEKLY;
        } else if (Frequency.MONTHLY.equals(freq)) {
            return RecurrenceFrequency.MONTHLY;
        } else if (Frequency.DAILY.equals(freq)) {
            return RecurrenceFrequency.DAILY;
        } else if (Frequency.YEARLY.equals(freq)) {
            return RecurrenceFrequency.YEARLY;
        }
        return null;
    }

    public static String asICalDateString(final Date date, final java.util.TimeZone timeZone, final boolean withoutTime) {
        if (date == null) {
            return null;
        }
        DateFormat df;
        if (withoutTime) {
            df = new SimpleDateFormat(DateFormats.COMPACT_DATE);
        } else {
            df = new SimpleDateFormat(DateFormats.ICAL_DATETIME_FORMAT);
        }
        if (timeZone != null) {
            df.setTimeZone(timeZone);
        } else {
            df.setTimeZone(DateHelper.UTC);
        }
        return df.format(date);
    }
}
