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

package org.projectforge.business.teamcal.event.ical.converter;

import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.Trigger;
import org.projectforge.Constants;
import org.projectforge.business.teamcal.event.ical.VEventComponentConverter;
import org.projectforge.business.teamcal.event.model.ReminderActionType;
import org.projectforge.business.teamcal.event.model.ReminderDurationUnit;
import org.projectforge.business.teamcal.event.model.TeamEventDO;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.List;

public class AlarmConverter implements VEventComponentConverter {
  private static final int DURATION_OF_WEEK = 7;

  @Override
  public boolean toVEvent(final TeamEventDO event, final VEvent vEvent) {
    if (event.getReminderDuration() == null || event.getReminderActionType() == null) {
      return false;
    }

    final VAlarm alarm = new VAlarm();
    Duration dur = null;
    // (-1) * needed to set alert before
    if (ReminderDurationUnit.MINUTES.equals(event.getReminderDurationUnit())) {
      dur = Duration.ofMinutes((-1) * event.getReminderDuration());
    } else if (ReminderDurationUnit.HOURS.equals(event.getReminderDurationUnit())) {
      dur = Duration.ofHours((-1) * event.getReminderDuration());
    } else if (ReminderDurationUnit.DAYS.equals(event.getReminderDurationUnit())) {
      dur = Duration.ofDays((-1) * event.getReminderDuration());
    }

    if (dur == null) {
      return false;
    }

    alarm.getProperties().add(new Trigger(dur));
    alarm.getProperties().add(new Action(event.getReminderActionType().getType()));
    vEvent.getAlarms().add(alarm);

    return true;
  }

  @Override
  public boolean fromVEvent(final TeamEventDO event, final VEvent vEvent) {

    final List<VAlarm> alarms = vEvent.getAlarms();
    if (alarms == null || alarms.isEmpty()) {
      return false;
    }

    final VAlarm alarm = alarms.get(0);
    final TemporalAmount dur = alarm.getTrigger().getDuration();
    if (alarm.getAction() == null || dur == null) {
      return false;
    }
    long seconds = dur.get(ChronoUnit.SECONDS);

    if (Action.AUDIO.equals(alarm.getAction())) {
      event.setReminderActionType(ReminderActionType.MESSAGE_SOUND);
    } else {
      event.setReminderActionType(ReminderActionType.MESSAGE);
    }

    long absSeconds = Math.abs(seconds);
    if (!getUnitCount(absSeconds, Constants.SECONDS_PER_DAY, event, ReminderDurationUnit.DAYS) &&
        !getUnitCount(absSeconds, Constants.SECONDS_PER_HOUR, event, ReminderDurationUnit.HOURS) &&
        !getUnitCount(absSeconds, Constants.SECONDS_PER_MINUTE, event, ReminderDurationUnit.MINUTES)) {
      event.setReminderDuration(15L);
      event.setReminderDurationUnit(ReminderDurationUnit.MINUTES);
    }
    return true;
  }

  // Try to get unit and duration.
  private boolean getUnitCount(long absSeconds, long millisPerUnit, TeamEventDO event, ReminderDurationUnit unit) {
    if (absSeconds < millisPerUnit) {
      return false;
    }
    if (absSeconds % millisPerUnit != 0) {
      return false;
    }
    event.setReminderDuration(absSeconds / millisPerUnit);
    event.setReminderDurationUnit(unit);
    return true;
  }
}
