package org.projectforge.caldav.cache;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.projectforge.caldav.model.AddressBook;
import org.projectforge.caldav.model.CalendarsHome;
import org.projectforge.caldav.model.ContactsHome;
import org.projectforge.caldav.model.User;
import org.springframework.stereotype.Component;

/**
 * Created by blumenstein on 06.11.16.
 */
@Component
public class UserCache
{
  private static long FIVE_MINUTES = 300000;

  private Map<Long, CalendarsHome> userCalendarHomeMap = new HashMap<>();

  private Map<Long, ContactsHome> userContactsHomeMap = new HashMap<>();

  private Map<Long, AddressBook> userAddressBookMap = new HashMap<>();

  private Map<User, Date> authorizedUserMap = new HashMap<>();

  public Map<User, Date> getAuthorizedUserMap()
  {
    return authorizedUserMap;
  }

  public boolean isUserAuthenticationValid(User user)
  {
    boolean userIsInList = false;
    Iterator<Map.Entry<User, Date>> it = authorizedUserMap.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<User, Date> item = it.next();
      if (item.getKey().getPk().equals(user.getPk())) {
        Date now = new Date();
        if (now.getTime() - item.getValue().getTime() < FIVE_MINUTES) {
          userIsInList = true;
        } else {
          it.remove();
        }
      }
    }
    return userIsInList;
  }

  public Map<Long, CalendarsHome> getUserCalendarHomeMap()
  {
    return userCalendarHomeMap;
  }

  public Map<Long, ContactsHome> getUserContactsHomeMap()
  {
    return userContactsHomeMap;
  }

  public Map<Long, AddressBook> getUserAddressBookMap()
  {
    return userAddressBookMap;
  }
}
