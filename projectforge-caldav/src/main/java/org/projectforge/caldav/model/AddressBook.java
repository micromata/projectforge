package org.projectforge.caldav.model;

/**
 * Created by blumenstein on 21.11.16.
 */
public class AddressBook
{
  private final User user;

  public AddressBook(User user)
  {
    this.user = user;
  }

  public String getName()
  {
    return "default";
  }

  public User getUser()
  {
    return this.user;
  }
}
