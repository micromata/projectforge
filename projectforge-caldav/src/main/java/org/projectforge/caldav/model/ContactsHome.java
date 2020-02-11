package org.projectforge.caldav.model;

/**
 * Created by blumenstein on 21.11.16.
 */
public class ContactsHome
{
  private final User user;

  public ContactsHome(User user)
  {
    this.user = user;
  }

  public String getName()
  {
    return "addressBooks";
  }

  public User getUser()
  {
    return this.user;
  }
}
