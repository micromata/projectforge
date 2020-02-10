package org.projectforge.caldav.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CalendarsHome
{
  private static Logger log = LoggerFactory.getLogger(CalendarsHome.class);

  private final User user;

  public CalendarsHome(User user)
  {
    this.user = user;
  }

  public String getName()
  {
    return "cals";
  }

  public User getUser()
  {
    return user;
  }
}
