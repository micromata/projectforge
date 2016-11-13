/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.web.teamcal.rest;

import java.util.Calendar;

import org.apache.commons.codec.binary.Base64;
import org.projectforge.business.teamcal.ICSGenerator;
import org.projectforge.business.teamcal.event.TeamRecurrenceEvent;
import org.projectforge.business.teamcal.event.model.ReminderDurationUnit;
import org.projectforge.business.teamcal.event.model.TeamEvent;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.model.rest.CalendarEventObject;
import org.projectforge.rest.converter.DOConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * For conversion of TeamEvent to CalendarEventObject.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class TeamEventDOConverter
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamEventDOConverter.class);

  @Autowired
  private ICSGenerator icsGenerator;

  public CalendarEventObject getEventObject(final TeamEvent src, boolean generateICS)
  {
    if (src == null) {
      return null;
    }
    final CalendarEventObject event = new CalendarEventObject();
    event.setUid(src.getUid());
    event.setStartDate(src.getStartDate());
    event.setEndDate(src.getEndDate());
    event.setLocation(src.getLocation());
    event.setNote(src.getNote());
    event.setSubject(src.getSubject());
    if (src instanceof TeamEventDO) {
      copyFields(event, (TeamEventDO) src);
    } else if (src instanceof TeamRecurrenceEvent) {
      final TeamEventDO master = ((TeamRecurrenceEvent) src).getMaster();
      if (master != null) {
        copyFields(event, master);
      }
    }
    if (generateICS) {
      event.setIcsData(Base64.encodeBase64String(icsGenerator.getIcsFileForTeamEvent(src).toByteArray()));
    }
    return event;
  }

  public CalendarEventObject getEventObject(final TeamEventDO src, boolean generateICS)
  {
    if (src == null) {
      return null;
    }
    final CalendarEventObject event = new CalendarEventObject();
    event.setUid(src.getUid());
    event.setStartDate(src.getStartDate());
    event.setEndDate(src.getEndDate());
    event.setLocation(src.getLocation());
    event.setNote(src.getNote());
    event.setSubject(src.getSubject());
    copyFields(event, src);
    if (generateICS) {
      event.setIcsData(Base64.encodeBase64String(icsGenerator.getIcsFile(src).toByteArray()));
    }
    return event;
  }

  private void copyFields(final CalendarEventObject event, final TeamEventDO src)
  {
    event.setCalendarId(src.getCalendarId());
    event.setRecurrenceRule(src.getRecurrenceRule());
    event.setRecurrenceExDate(src.getRecurrenceExDate());
    event.setRecurrenceUntil(src.getRecurrenceUntil());
    DOConverter.copyFields(event, src);
    if (src.getReminderActionType() != null && src.getReminderDuration() != null
        && src.getReminderDurationUnit() != null) {
      event.setReminderType(src.getReminderActionType().toString());
      event.setReminderDuration(src.getReminderDuration());
      final ReminderDurationUnit unit = src.getReminderDurationUnit();
      event.setReminderUnit(unit.toString());
      final DateHolder date = new DateHolder(src.getStartDate());
      if (unit == ReminderDurationUnit.MINUTES) {
        date.add(Calendar.MINUTE, -src.getReminderDuration());
        event.setReminder(date.getDate());
      } else if (unit == ReminderDurationUnit.HOURS) {
        date.add(Calendar.HOUR, -src.getReminderDuration());
        event.setReminder(date.getDate());
      } else if (unit == ReminderDurationUnit.DAYS) {
        date.add(Calendar.DAY_OF_YEAR, -src.getReminderDuration());
        event.setReminder(date.getDate());
      } else {
        log.warn("ReminderDurationUnit '" + src.getReminderDurationUnit() + "' not yet implemented.");
      }
    }
  }
}
