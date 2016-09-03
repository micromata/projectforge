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

package org.projectforge.business.teamcal.event.model;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.projectforge.business.teamcal.event.TeamRecurrenceEvent;
import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.utils.NumberHelper;

/**
 * Used by {@link TeamCalEventProvider} for handling event id's. The id of {@link TeamEventDO} objects is the data-base
 * id (pk). The id of recurrence events is the data-base id of the master {@link TeamEventDO} followed by the date, e.
 * g. "42-20121222".
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class TeamCalEventId
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamCalEventId.class);

  private final Integer databaseId;

  private Date date;

  private static final DateFormat FORMAT_COMPACT_DATE = new SimpleDateFormat(DateFormats.COMPACT_DATE);

  private DateFormat dateFormat;

  private final TimeZone timeZone;

  public TeamCalEventId(final Integer databaseId, final Date date, final TimeZone timeZone)
  {
    this.databaseId = databaseId;
    this.date = date;
    this.timeZone = timeZone;
  }

  public TeamCalEventId(final String idString, final TimeZone timeZone)
  {
    this.timeZone = timeZone;
    final int pos = idString.indexOf('-');
    if (pos < 0) {
      this.date = null;
      databaseId = NumberHelper.parseInteger(idString);
      return;
    }
    final String idStr = idString.substring(0, pos);
    databaseId = NumberHelper.parseInteger(idStr);
    final String dateString = idString.substring(pos + 1);
    try {
      date = getDateFormat().parse(dateString);
    } catch (final ParseException ex) {
      log.error("Can't parse date '" + dateString + "' of '" + idString + "': " + ex.getMessage(), ex);
    }
  }

  public TeamCalEventId(final TeamEvent event, final TimeZone timeZone)
  {
    this.timeZone = timeZone;
    if (event instanceof TeamEventDO) {
      this.databaseId = ((TeamEventDO) event).getId();
    } else {
      this.databaseId = ((TeamRecurrenceEvent) event).getMaster().getId();
      this.date = ((TeamRecurrenceEvent) event).getStartDate();
    }
  }

  /**
   * @return the id of the event or the id of the master event for recurrence events.
   */
  public Integer getDataBaseId()
  {
    return databaseId;
  }

  /**
   * @return the date of the recurrence event (other than the master event) or null if this is no recurrence event.
   */
  public Date getDate()
  {
    return date;
  }

  private DateFormat getDateFormat()
  {
    if (dateFormat == null) {
      dateFormat = (DateFormat) FORMAT_COMPACT_DATE.clone();
      dateFormat.setTimeZone(timeZone);
    }
    return dateFormat;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    if (date == null) {
      return String.valueOf(databaseId);
    } else {
      return String.valueOf(databaseId) + "-" + getDateFormat().format(date);
    }
  }
}
