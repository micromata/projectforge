package org.projectforge.business.teamcal.service;

import org.projectforge.business.teamcal.model.CalendarFeedConst;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.service.UserService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CalendarFeedService
{
  @Autowired
  private UserDao userDao;

  @Autowired
  private UserService userService;

  public String getUrl()
  {
    return getUrl(null);
  }

  /**
   * @return The url for downloading timesheets (including context), e. g.
   *         /ProjectForge/export/ProjectForge.ics?user=....
   */
  public String getUrl4Timesheets(final Integer timesheetUserId)
  {
    return getUrl("&" + CalendarFeedConst.PARAM_NAME_TIMESHEET_USER + "=" + timesheetUserId);
  }

  /**
   * @return The url for downloading timesheets (including context), e. g.
   *         /ProjectForge/export/ProjectForge.ics?user=....
   */
  public String getUrl4Holidays()
  {
    return getUrl("&" + CalendarFeedConst.PARAM_NAME_HOLIDAYS + "=true");
  }

  /**
   * @return The url for downloading timesheets (including context), e. g.
   *         /ProjectForge/export/ProjectForge.ics?user=....
   */
  public String getUrl4WeekOfYears()
  {
    return getUrl("&" + CalendarFeedConst.PARAM_NAME_WEEK_OF_YEARS + "=true");
  }

  /**
   * @param additionalParams Request parameters such as "&calId=42", may be null.
   * @return The url for downloading calendars (without context), e. g. /export/ProjectForge.ics?user=...
   */
  public String getUrl(final String additionalParams)
  {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    final String authenticationKey = userDao.getAuthenticationToken(user.getId());
    final StringBuilder buf = new StringBuilder();
    buf.append("token=").append(authenticationKey);
    if (additionalParams != null) {
      buf.append(additionalParams);
    }
    final String encryptedParams = userService.encrypt(buf.toString());
    final String result = "/export/ProjectForge.ics?user=" + user.getId() + "&q=" + encryptedParams;
    return result;
  }
}
