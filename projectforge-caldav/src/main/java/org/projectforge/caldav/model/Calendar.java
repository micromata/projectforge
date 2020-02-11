package org.projectforge.caldav.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.annotations.UniqueId;

public class Calendar
{
  private static Logger log = LoggerFactory.getLogger(Calendar.class);

  private final User user;

  private final String name;

  private final Integer id;

  public Calendar(User user, Integer id, String name)
  {
    this.user = user;
    this.id = id;
    this.name = name;
  }

  @UniqueId
  public Integer getUniqueId(Calendar c)
  {
    return c.getId();
  }

  public String getName()
  {
    return name;
  }

  public User getUser()
  {
    return user;
  }

  public Integer getId()
  {
    return id;
  }
}
