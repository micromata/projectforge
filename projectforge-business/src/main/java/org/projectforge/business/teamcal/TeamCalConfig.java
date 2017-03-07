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

package org.projectforge.business.teamcal;

import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.configuration.ConfigurationData;
import java.util.UUID;

public class TeamCalConfig implements ConfigurationData
{

  // Don't change this, otherwise the synchronization with older entries may fail.
  public static final String TIMESHEET_UID_PREFIX = "pf-ts";

  /**
   * setup event is needed for empty calendars
   */
  public static final String SETUP_EVENT = "SETUP EVENT";

  private static TeamCalConfig config;

  public static TeamCalConfig get()
  {
    if (config == null) {
      config = (TeamCalConfig) ConfigXml.getInstance().getPluginConfig(TeamCalConfig.class);
    }
    return config;
  }

  public String getDomain()
  {
    return Configuration.getInstance().getCalendarDomain();
  }

  /**
   * @param prefix
   * @param id
   * @see #createUid(String, String)
   */
  public String createUid(final String prefix, final Integer id)
  {
    return createUid(prefix, id != null ? id.toString() : "");
  }

  /**
   * Creates a world wide unique event id for ical events for better synchronization.
   * 
   * @param prefix
   * @param id
   * @return uid of the format: "${prefix}-${id}@${domain}", e. g. "pf-event-1234@projectforge.org".
   */
  public String createUid(final String prefix, final String id)
  {
    return prefix + "-" + id + "@" + getDomain();
  }

  /**
   * @return
   */
  public String createEventUid()
  {
    return UUID.randomUUID().toString();
  }

  /**
   * @param id
   * @return
   */
  public String createTimesheetUid(final Integer id)
  {
    return createUid(TIMESHEET_UID_PREFIX, id);
  }

  /**
   * Only for internal test purposes.
   * 
   * @param config
   */
  public static void __internalSetConfig(final TeamCalConfig newConfig)
  {
    config = newConfig;
  }
}
