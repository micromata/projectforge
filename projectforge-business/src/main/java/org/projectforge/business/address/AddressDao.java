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

package org.projectforge.business.address;

import java.io.PrintWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.utils.NumberHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
public class AddressDao extends BaseDao<AddressDO>
{
  private static final DateFormat V_CARD_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AddressDao.class);

  @Autowired
  private AddressbookDao addressbookDao;

  @Autowired
  private UserRightService userRights;

  private transient AddressbookRight addressbookRight;

  @Autowired
  private PersonalAddressDao personalAddressDao;

  public AddressDao()
  {
    super(AddressDO.class);
  }

  /**
   * Addresses will be assigned to a default task.
   */
  public Integer getDefaultTaskId()
  {
    return Configuration.getInstance().getTaskIdValue(ConfigurationParam.DEFAULT_TASK_ID_4_ADDRESSES);
  }

  public List<Locale> getUsedCommunicationLanguages()
  {
    @SuppressWarnings("unchecked")
    final List<Locale> list = (List<Locale>) getHibernateTemplate()
        .find(
            "select distinct a.communicationLanguage from AddressDO a where deleted=false and a.communicationLanguage is not null order by a.communicationLanguage");
    return list;
  }

  /**
   * Get the newest address entries (by time of creation).
   *
   * @return
   * @see #getNewestMax()
   */
  public List<AddressDO> getNewest(final BaseSearchFilter filter)
  {
    final QueryFilter queryFilter = new QueryFilter();
    queryFilter.addOrder(Order.desc("created"));
    addAddressbookRestriction(queryFilter, null);
    if (filter.getMaxRows() > 0) {
      queryFilter.setMaxResults(filter.getMaxRows());
    }
    return getList(queryFilter);
  }

  @Override
  public List<AddressDO> getList(final BaseSearchFilter filter)
  {
    final AddressFilter myFilter;
    if (filter instanceof AddressFilter) {
      myFilter = (AddressFilter) filter;
    } else {
      myFilter = new AddressFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    if (StringUtils.isBlank(myFilter.getSearchString()) == true) {
      if (myFilter.isDeleted() == false) {
        if (myFilter.isNewest() == true) {
          return getNewest(myFilter);
        }
        if (myFilter.isMyFavorites() == true) {
          // Show only favorites.
          return personalAddressDao.getMyAddresses();
        }
      }
    } else {
      if (StringUtils.isNumeric(filter.getSearchString()) == true) {
        myFilter.setSearchString("*" + myFilter.getSearchString() + "*");
      }
    }
    if (myFilter.isFilter() == true) {
      // Proceed contact status:
      // Use filter only for non deleted entries:
      if (myFilter.isActive() == true
          || myFilter.isNonActive() == true
          || myFilter.isUninteresting() == true
          || myFilter.isDeparted() == true
          || myFilter.isPersonaIngrata() == true) {
        final Collection<ContactStatus> col = new ArrayList<ContactStatus>();
        if (myFilter.isActive() == true) {
          col.add(ContactStatus.ACTIVE);
        }
        if (myFilter.isNonActive() == true) {
          col.add(ContactStatus.NON_ACTIVE);
        }
        if (myFilter.isUninteresting() == true) {
          col.add(ContactStatus.UNINTERESTING);
        }
        if (myFilter.isDeparted() == true) {
          col.add(ContactStatus.DEPARTED);
        }
        if (myFilter.isPersonaIngrata() == true) {
          col.add(ContactStatus.PERSONA_INGRATA);
        }
        queryFilter.add(Restrictions.in("contactStatus", col));
      }

      // Proceed address status:
      // Use filter only for non deleted books:
      if (myFilter.isUptodate() == true || myFilter.isOutdated() == true || myFilter.isLeaved() == true) {
        final Collection<AddressStatus> col = new ArrayList<AddressStatus>();
        if (myFilter.isUptodate() == true) {
          col.add(AddressStatus.UPTODATE);
        }
        if (myFilter.isOutdated() == true) {
          col.add(AddressStatus.OUTDATED);
        }
        if (myFilter.isLeaved() == true) {
          col.add(AddressStatus.LEAVED);
        }
        queryFilter.add(Restrictions.in("addressStatus", col));
      }

      //Add addressbook restriction
      addAddressbookRestriction(queryFilter, myFilter);

    }
    queryFilter.addOrder(Order.asc("name"));
    final List<AddressDO> result = getList(queryFilter);
    if (myFilter.isDoublets() == true) {
      final HashSet<String> fullnames = new HashSet<String>();
      final HashSet<String> doubletFullnames = new HashSet<String>();
      for (final AddressDO address : result) {
        final String fullname = getNormalizedFullname(address);
        if (fullnames.contains(fullname) == true) {
          doubletFullnames.add(fullname);
        }
        fullnames.add(fullname);
      }
      final List<AddressDO> doublets = new LinkedList<AddressDO>();
      for (final AddressDO address : result) {
        if (doubletFullnames.contains(getNormalizedFullname(address)) == true) {
          doublets.add(address);
        }
      }
      return doublets;
    }
    return result;
  }

  private void addAddressbookRestriction(final QueryFilter queryFilter, final AddressFilter addressFilter)
  {
    //Addressbook rights check
    Set<Integer> abIdList = new HashSet();
    //First check wicket ui addressbook filter
    if (addressFilter != null && addressFilter.getAddressbooks() != null && addressFilter.getAddressbooks().size() > 0) {
      abIdList.addAll(addressFilter.getAddressbooks().stream().mapToInt(ab -> ab.getId()).boxed().collect(Collectors.toList()));
    } else {
      //Global addressbook is selectable for every one
      abIdList.add(AddressbookDao.GLOBAL_ADDRESSBOOK_ID);
      //Get all addressbooks for user
      if (addressbookRight == null) {
        addressbookRight = (AddressbookRight) userRights.getRight(UserRightId.MISC_ADDRESSBOOK);
      }
      for (AddressbookDO ab : addressbookDao.internalLoadAll()) {
        if (ab.isDeleted() == false && addressbookRight.hasSelectAccess(ThreadLocalUserContext.getUser(), ab)) {
          abIdList.add(ab.getId());
        }
      }
    }
    //Has to be on id value, full entity doesn't work!!!
    queryFilter.createAlias("addressbookList", "abl");
    queryFilter.add(Restrictions.in("abl.id", abIdList));
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#hasAccess(Object, OperationType)
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final AddressDO obj, final AddressDO oldObj,
      final OperationType operationType,
      final boolean throwException)
  {
    if (addressbookRight == null) {
      addressbookRight = (AddressbookRight) userRights.getRight(UserRightId.MISC_ADDRESSBOOK);
    }
    if (obj == null || obj.getAddressbookList() == null) {
      //Nothing to check, should not happen, but does
      return true;
    }
    switch (operationType) {
      case SELECT:
        for (AddressbookDO ab : obj.getAddressbookList()) {
          if (addressbookRight.hasSelectAccess(user, ab)) {
            return true;
          }
          if (throwException) {
            throw new AccessException(user, "access.exception.userHasNotRight", addressbookRight, operationType);
          }
          return false;
        }
      case INSERT:
        for (AddressbookDO ab : obj.getAddressbookList()) {
          if (addressbookRight.hasInsertAccess(user, ab)) {
            return true;
          }
          if (throwException) {
            throw new AccessException(user, "access.exception.userHasNotRight", addressbookRight, operationType);
          }
          return false;
        }
      case UPDATE:
        for (AddressbookDO ab : obj.getAddressbookList()) {
          if (addressbookRight.hasUpdateAccess(user, ab, ab)) {
            return true;
          }
          if (throwException) {
            throw new AccessException(user, "access.exception.userHasNotRight", addressbookRight, operationType);
          }
          return false;
        }
      case DELETE:
        for (AddressbookDO ab : obj.getAddressbookList()) {
          if (addressbookRight.hasDeleteAccess(user, ab, ab)) {
            return true;
          }
          if (throwException) {
            throw new AccessException(user, "access.exception.userHasNotRight", addressbookRight, operationType);
          }
          return false;
        }
      default:
        if (throwException) {
          throw new AccessException(user, "access.exception.userHasNotRight", addressbookRight, operationType);
        }
        return false;
    }
  }

  private String getNormalizedFullname(final AddressDO address)
  {
    final StringBuilder builder = new StringBuilder();
    if (address.getFirstName() != null) {
      builder.append(address.getFirstName().toLowerCase().trim());
    }
    if (address.getName() != null) {
      builder.append(address.getName().toLowerCase().trim());
    }
    return builder.toString();
  }

  /**
   * Get the birthdays of address entries.
   *
   * @param fromDate Search for birthdays from given date (ignoring the year).
   * @param toDate   Search for birthdays until given date (ignoring the year).
   * @param max      Maximum number of result entries.
   * @param all      If false, only the birthdays of favorites will be returned.
   * @return The entries are ordered by date of year and name.
   */
  public Set<BirthdayAddress> getBirthdays(final Date fromDate, final Date toDate, final int max, final boolean all)
  {
    final QueryFilter filter = new QueryFilter();
    filter.add(Restrictions.isNotNull("birthday"));
    final List<AddressDO> list = getList(filter);
    // Uses not Collections.sort because every comparison needs Calendar.getDayOfYear().
    final Set<BirthdayAddress> set = new TreeSet<BirthdayAddress>();
    final Set<Integer> favorites = getFavorites();
    final DateHolder from = new DateHolder(fromDate);
    final DateHolder to = new DateHolder(toDate);
    DateHolder dh;
    final int fromMonth = from.getMonth();
    final int fromDayOfMonth = from.getDayOfMonth();
    final int toMonth = to.getMonth();
    final int toDayOfMonth = to.getDayOfMonth();
    for (final AddressDO address : list) {
      if (all == false && favorites.contains(address.getId()) == false) {
        // Address is not a favorite address, so ignore it.
        continue;
      }
      dh = new DateHolder(address.getBirthday());
      final int month = dh.getMonth();
      final int dayOfMonth = dh.getDayOfMonth();
      if (DateHelper.dateOfYearBetween(month, dayOfMonth, fromMonth, fromDayOfMonth, toMonth, toDayOfMonth) == false) {
        continue;
      }
      final BirthdayAddress ba = new BirthdayAddress(address);
      if (favorites.contains(address.getId()) == true) {
        ba.setFavorite(true);
      }
      set.add(ba);
    }
    return set;
  }

  public List<PersonalAddressDO> getFavoriteVCards()
  {
    final List<PersonalAddressDO> list = personalAddressDao.getList();
    final List<PersonalAddressDO> result = new ArrayList<PersonalAddressDO>();
    if (CollectionUtils.isNotEmpty(list) == true) {
      for (final PersonalAddressDO entry : list) {
        if (entry.isFavoriteCard() == true) {
          result.add(entry);
        }
      }
    }
    return result;
  }

  public Set<Integer> getFavorites()
  {
    final List<PersonalAddressDO> list = personalAddressDao.getList();
    final Set<Integer> result = new HashSet<Integer>();
    if (CollectionUtils.isNotEmpty(list) == true) {
      for (final PersonalAddressDO entry : list) {
        if (entry.isFavoriteCard() == true) {
          result.add(entry.getAddressId());
        }
      }
    }
    return result;
  }

  public void exportFavoriteVCards(final Writer out, final List<PersonalAddressDO> favorites)
  {
    log.info("Exporting personal AddressBook.");
    final PrintWriter pw = new PrintWriter(out);
    for (final PersonalAddressDO entry : favorites) {
      if (entry.isFavoriteCard() == false) {
        // Entry is not marks as vCard-Entry.
        continue;
      }
      final AddressDO addressDO = entry.getAddress();
      exportVCard(pw, addressDO);
    }
    pw.flush();
  }

  /**
   * Exports a single vcard for the given addressDO
   *
   * @param pw
   * @param addressDO
   * @return
   */
  public void exportVCard(final PrintWriter pw, final AddressDO addressDO)
  {
    if (log.isDebugEnabled() == true) {
      log.debug("Exporting vCard for addressDo : " + addressDO != null ? addressDO.getId() : null);
    }
    pw.println("BEGIN:VCARD");
    pw.println("VERSION:3.0");
    pw.print("N:");
    out(pw, addressDO.getName());
    pw.print(';');
    out(pw, addressDO.getFirstName());
    pw.print(";;");
    out(pw, addressDO.getTitle());
    pw.println(";");
    print(pw, "FN:", getFullName(addressDO));
    if (isGiven(addressDO.getOrganization()) == true || isGiven(addressDO.getDivision()) == true) {
      pw.print("ORG:");
      out(pw, addressDO.getOrganization());
      pw.print(';');
      if (isGiven(addressDO.getDivision()) == true) {
        out(pw, addressDO.getDivision());
      }
      pw.println();
    }
    print(pw, "TITLE:", addressDO.getPositionText());
    print(pw, "EMAIL;type=INTERNET;type=WORK;type=pref:", addressDO.getEmail());
    print(pw, "EMAIL;type=INTERNET;type=HOME;type=pref:", addressDO.getPrivateEmail());
    print(pw, "TEL;type=WORK;type=pref:", addressDO.getBusinessPhone());
    print(pw, "TEL;TYPE=CELL:", addressDO.getMobilePhone());
    print(pw, "TEL;type=WORK;type=FAX:", addressDO.getFax());
    print(pw, "TEL;TYPE=HOME:", addressDO.getPrivatePhone());
    print(pw, "TEL;TYPE=HOME;type=CELL:", addressDO.getPrivateMobilePhone());

    if (isGiven(addressDO.getAddressText()) == true || isGiven(addressDO.getCity()) == true
        || isGiven(addressDO.getZipCode()) == true) {
      pw.print("ADR;TYPE=WORK:;;");
      out(pw, addressDO.getAddressText());
      pw.print(';');
      out(pw, addressDO.getCity());
      pw.print(";;");
      out(pw, addressDO.getZipCode());
      pw.print(';');
      out(pw, addressDO.getCountry());
      pw.println();
    }
    if (isGiven(addressDO.getPrivateAddressText()) == true
        || isGiven(addressDO.getPrivateCity()) == true
        || isGiven(addressDO.getPrivateZipCode()) == true) {
      pw.print("ADR;TYPE=HOME:;;");
      out(pw, addressDO.getPrivateAddressText());
      pw.print(';');
      out(pw, addressDO.getPrivateCity());
      pw.print(";;");
      out(pw, addressDO.getPrivateZipCode());
      pw.print(";");
      pw.println();
    }
    print(pw, "URL;type=pref:", addressDO.getWebsite());
    if (addressDO.getBirthday() != null) {
      print(pw, "BDAY;value=date:", V_CARD_DATE_FORMAT.format(addressDO.getBirthday()));
    }
    if (isGiven(addressDO.getComment()) == true) {
      print(pw, "NOTE:", addressDO.getComment() + "\\nCLASS: WORK");
    } else {
      print(pw, "NOTE:", "CLASS: WORK");
    }
    // pw.println("TZ:+00:00");
    pw.println("CATEGORIES:ProjectForge");
    pw.print("UID:U");
    pw.println(addressDO.getId());
    pw.println("END:VCARD");
    pw.println();
    // Unused: addressDO.getState();
  }

  /**
   * Used by vCard export for field 'FN' (full name). Concatenates first name, last name and title.
   *
   * @return
   */
  public String getFullName(final AddressDO a)
  {
    final StringBuffer buf = new StringBuffer();
    boolean space = false;
    if (isGiven(a.getName()) == true) {
      buf.append(a.getName());
      space = true;
    }
    if (isGiven(a.getFirstName()) == true) {
      if (space == true) {
        buf.append(' ');
      } else {
        space = true;
      }
      buf.append(a.getFirstName());
    }
    if (isGiven(a.getTitle()) == true) {
      if (space == true) {
        buf.append(' ');
      } else {
        space = true;
      }
      buf.append(a.getTitle());
    }
    return buf.toString();
  }

  public List<PersonalAddressDO> getFavoritePhoneEntries()
  {
    final List<PersonalAddressDO> list = personalAddressDao.getList();
    final List<PersonalAddressDO> result = new ArrayList<PersonalAddressDO>();
    if (CollectionUtils.isNotEmpty(list) == true) {
      for (final PersonalAddressDO entry : list) {
        if (entry.isFavoriteBusinessPhone() == true
            || entry.isFavoriteFax() == true
            || entry.isFavoriteMobilePhone() == true
            || entry.isFavoritePrivatePhone() == true) {
          result.add(entry);
        }
      }
    }
    return result;
  }

  /**
   * Throws UserException, if for example the phone list is empty.
   */
  public void exportFavoritePhoneList(final Writer out, final List<PersonalAddressDO> favorites)
  {
    log.info("Exporting phone list");
    final PrintWriter pw = new PrintWriter(out);
    pw.println("\"Name\",\"Phone number\"");
    for (final PersonalAddressDO entry : favorites) {
      final AddressDO address = entry.getAddress();
      String number = address.getBusinessPhone();
      if (entry.isFavoriteBusinessPhone() == true && StringUtils.isNotBlank(number)) {
        appendPhoneEntry(pw, address, "", number);
      }
      number = address.getFax();
      if (entry.isFavoriteFax() == true && StringUtils.isNotBlank(number)) {
        appendPhoneEntry(pw, address, "fax", number);
      }
      number = address.getMobilePhone();
      if (entry.isFavoriteMobilePhone() == true && StringUtils.isNotBlank(number)) {
        appendPhoneEntry(pw, address, "mobil", number);
      }
      number = address.getPrivateMobilePhone();
      if (entry.isFavoritePrivateMobilePhone() == true && StringUtils.isNotBlank(number)) {
        final String str = StringUtils.isNotBlank(address.getMobilePhone()) == true ? "mobil privat" : "mobil";
        appendPhoneEntry(pw, address, str, number);
      }
      number = address.getPrivatePhone();
      if (entry.isFavoritePrivatePhone() == true && StringUtils.isNotBlank(number)) {
        appendPhoneEntry(pw, address, "privat", number);
      }
    }
    pw.flush();
  }

  private void print(final PrintWriter pw, final String key, final String value)
  {
    if (isGiven(value) == false) {
      return;
    }
    pw.print(key);
    out(pw, value);
    pw.println();
  }

  /**
   * Simply calls StringUtils.defaultString(String) and replaces: "\r" -> "", "\n" -> "\\n", "," -> "\\,", ":" -> "\\:"
   * and print the resulted string into given PrintWriter (without newline).
   *
   * @param str
   * @see StringUtils#defaultString(String)
   */
  private void out(final PrintWriter pw, final String str)
  {
    final String s = StringUtils.defaultString(str);
    boolean cr = false;
    for (int i = 0; i < s.length(); i++) {
      final char ch = s.charAt(i);
      if (ch == ':') {
        pw.print("\\:");
      } else if (ch == ',') {
        pw.print("\\,");
      } else if (ch == ';') {
        pw.print("\\;");
      } else if (ch == '\r') {
        pw.print("\\n");
        cr = true;
        continue;
      } else if (ch == '\n') {
        if (cr == false) {
          // Print only \n if not already done by previous \r.
          pw.print("\\n");
        }
      } else {
        pw.print(ch);
      }
      cr = false;
    }
  }

  /**
   * Simply call StringUtils.isNotBlank(String)
   *
   * @param str
   * @return
   * @see StringUtils#isNotBlank(String)
   */
  private boolean isGiven(final String str)
  {
    return StringUtils.isNotBlank(str);
  }

  private void appendPhoneEntry(final PrintWriter pw, final AddressDO address, final String suffix, final String number)
  {
    if (isGiven(number) == false) {
      // Do nothing, number is empty.
      return;
    }
    final String no = NumberHelper
        .extractPhonenumber(number,
            Configuration.getInstance().getStringValue(ConfigurationParam.DEFAULT_COUNTRY_PHONE_PREFIX));
    final String name = address.getName();
    pw.print("\"");
    if (StringUtils.isNotEmpty(name)) {
      pw.print(name);
    }
    final String firstName = address.getFirstName();
    if (StringUtils.isNotBlank(firstName)) {
      if (StringUtils.isNotBlank(name)) {
        pw.print(", ");
      }
      pw.print(firstName);
    }
    if (StringUtils.isNotEmpty(suffix)) {
      pw.print(' ');
      pw.print(suffix);
    }
    pw.print("\",\"");
    pw.println(no + "\"");
  }

  @Override
  public AddressDO newInstance()
  {
    return new AddressDO();
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#useOwnCriteriaCacheRegion()
   */
  @Override
  protected boolean useOwnCriteriaCacheRegion()
  {
    return true;
  }

  public List<AddressDO> findAll()
  {
    return internalLoadAll();
  }
}
