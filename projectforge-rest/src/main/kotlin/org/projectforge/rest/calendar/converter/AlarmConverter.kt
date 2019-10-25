/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.calendar.converter

import net.fortuna.ical4j.model.Dur
import net.fortuna.ical4j.model.component.VAlarm
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.Action
import net.fortuna.ical4j.model.property.Trigger
import org.projectforge.business.teamcal.event.model.ReminderActionType
import org.projectforge.business.teamcal.event.model.ReminderDurationUnit
import org.projectforge.rest.dto.CalEvent

class AlarmConverter : VEventComponentConverter {

    override fun toVEvent(event: CalEvent, vEvent: VEvent): Boolean {
        if (event.reminderDuration == null || event.reminderActionType == null) {
            return false
        }

        val alarm = VAlarm()
        var dur: Dur? = null
        // (-1) * needed to set alert before
        if (ReminderDurationUnit.MINUTES == event.reminderDurationUnit) {
            dur = Dur(0, 0, -1 * event.reminderDuration!!, 0)
        } else if (ReminderDurationUnit.HOURS == event.reminderDurationUnit) {
            dur = Dur(0, -1 * event.reminderDuration!!, 0, 0)
        } else if (ReminderDurationUnit.DAYS == event.reminderDurationUnit) {
            dur = Dur(-1 * event.reminderDuration!!, 0, 0, 0)
        }

        if (dur == null) {
            return false
        }

        alarm.properties.add(Trigger(dur))
        alarm.properties.add(Action(event.reminderActionType!!.type))
        vEvent.alarms.add(alarm)

        return true
    }

    override fun fromVEvent(event: CalEvent, vEvent: VEvent): Boolean {

        val alarms = vEvent.alarms
        if (alarms == null || alarms.isEmpty()) {
            return false
        }

        val alarm = alarms[0]
        val dur = alarm.trigger.duration
        if (alarm.action == null || dur == null) {
            return false
        }

        if (Action.AUDIO == alarm.action) {
            event.reminderActionType = ReminderActionType.MESSAGE_SOUND
        } else {
            event.reminderActionType = ReminderActionType.MESSAGE
        }

        // consider weeks
        var weeksToDays = 0
        if (dur.weeks != 0) {
            weeksToDays = dur.weeks * DURATION_OF_WEEK
        }

        if (dur.days != 0) {
            event.reminderDuration = dur.days + weeksToDays
            event.reminderDurationUnit = ReminderDurationUnit.DAYS
        } else if (dur.hours != 0) {
            event.reminderDuration = dur.hours
            event.reminderDurationUnit = ReminderDurationUnit.HOURS
        } else if (dur.minutes != 0) {
            event.reminderDuration = dur.minutes
            event.reminderDurationUnit = ReminderDurationUnit.MINUTES
        } else {
            event.reminderDuration = 15
            event.reminderDurationUnit = ReminderDurationUnit.MINUTES
        }

        return true
    }

    companion object {
        private const val DURATION_OF_WEEK = 7
    }
}
