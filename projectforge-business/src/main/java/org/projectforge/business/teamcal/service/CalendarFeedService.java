/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.teamcal.service;

import org.projectforge.business.configuration.DomainService;
import org.projectforge.business.teamcal.model.CalendarFeedConst;
import org.projectforge.business.user.UserAuthenticationsService;
import org.projectforge.business.user.UserTokenType;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CalendarFeedService {
  private static final String PARAM_EXPORT_REMINDER = "exportReminders";
  private static final String PARAM_CALENDAR = "teamCals";
  private static final String BASE_URI = "/export/ProjectForge.ics"; // See also Rest.java

  @Autowired
  private DomainService domainService;

  @Autowired
  private UserAuthenticationsService userAuthenticationsService;

  public String getUrl() {
    return getUrl(null);
  }

  /**
   * @return The url for downloading timesheets (including context), e. g.
   * /ProjectForge/export/ProjectForge.ics?user=....
   */
  public String getUrl4Timesheets(final Long timesheetUserId) {
    return getUrl("&" + CalendarFeedConst.PARAM_NAME_TIMESHEET_USER + "=" + timesheetUserId);
  }

  /**
   * @return The url for downloading timesheets (including context), e. g.
   * /ProjectForge/export/ProjectForge.ics?user=....
   */
  public String getFullUrl4Timesheets(final Long timesheetUserId) {
    return getFullUrl("&" + CalendarFeedConst.PARAM_NAME_TIMESHEET_USER + "=" + timesheetUserId);
  }

  /**
   * @return The url for downloading timesheets (including context), e. g.
   * /ProjectForge/export/ProjectForge.ics?user=....
   */
  public String getUrl4Holidays() {
    return getUrl("&" + CalendarFeedConst.PARAM_NAME_HOLIDAYS + "=true");
  }

  /**
   * @return The url for downloading timesheets (including context), e. g.
   * /ProjectForge/export/ProjectForge.ics?user=....
   */
  public String getFullUrl4Holidays() {
    return getFullUrl("&" + CalendarFeedConst.PARAM_NAME_HOLIDAYS + "=true");
  }

  /**
   * @return The url for downloading timesheets (including context), e. g.
   * /ProjectForge/export/ProjectForge.ics?user=....
   */
  public String getUrl4WeekOfYears() {
    return getUrl("&" + CalendarFeedConst.PARAM_NAME_WEEK_OF_YEARS + "=true");
  }

  /**
   * @return The url for downloading timesheets (including context), e. g.
   * /ProjectForge/export/ProjectForge.ics?user=....
   */
  public String getFullUrl4WeekOfYears() {
    return getFullUrl("&" + CalendarFeedConst.PARAM_NAME_WEEK_OF_YEARS + "=true");
  }

  /**
   * @param additionalParams Request parameters such as "&calId=42", may be null.
   * @return The url for downloading calendars (without context), e. g. /export/ProjectForge.ics?user=...
   */
  public String getUrl(final String additionalParams) {
    final PFUserDO user = ThreadLocalUserContext.getLoggedInUser();
    final String authenticationKey = userAuthenticationsService.getToken(user.getId(), UserTokenType.CALENDAR_REST);
    final StringBuilder buf = new StringBuilder();
    buf.append("token=").append(authenticationKey);
    if (additionalParams != null) {
      buf.append(additionalParams);
    }
    final String encryptedParams = userAuthenticationsService.encrypt(UserTokenType.CALENDAR_REST, buf.toString());
    final String result = BASE_URI + "?user=" + user.getId() + "&q=" + encryptedParams;
    return result;
  }

  public String getFullUrl(final String additionalParams) {
    final String pfBaseUrl = domainService.getDomainWithContextPath();
    final String url = getUrl(additionalParams);
    return pfBaseUrl + url;
  }

  public String getFullUrl(Long teamCalId, boolean exportReminders) {
    return getFullUrl("&" + PARAM_CALENDAR + "=" + teamCalId + "&" + PARAM_EXPORT_REMINDER + "=" + exportReminders);
  }
}
