/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.multitenancy.TenantService;
import org.projectforge.business.user.UserRightId;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.persistence.api.*;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.framework.utils.NumberHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
public class AddressDao extends BaseDao<AddressDO> {
  private static final DateFormat V_CARD_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
  private static final String[] ENABLED_AUTOCOMPLETION_PROPERTIES = {"addressText", "postalAddressText", "privateAddressText", "organization"};

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AddressDao.class);

  @Override
  public boolean isAutocompletionPropertyEnabled(String property) {
    return ArrayUtils.contains(ENABLED_AUTOCOMPLETION_PROPERTIES, property);
  }

  @Autowired
  private AddressbookDao addressbookDao;

  @Autowired
  private UserRightService userRights;

  private transient AddressbookRight addressbookRight;

  @Autowired
  private PersonalAddressDao personalAddressDao;

  @Autowired
  private TenantService tenantService;

  public AddressDao() {
    super(AddressDO.class);
  }

  public List<Locale> getUsedCommunicationLanguages() {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Locale> cr = cb.createQuery(Locale.class);
    Root<AddressDO> root = cr.from(clazz);
    cr.select(root.get("communicationLanguage")).where(
            cb.equal(root.get("deleted"), false),
            cb.isNotNull(root.get("communicationLanguage")))
            .orderBy(cb.asc(root.get("communicationLanguage")))
            .distinct(true);
//    "select distinct a.communicationLanguage from AddressDO a where deleted=false and a.communicationLanguage is not null order by a.communicationLanguage");
    return em.createQuery(cr).getResultList();
  }

  /**
   * Get the newest address entries (by time of creation).
   */
  public List<AddressDO> getNewest(final BaseSearchFilter filter) {
    final QueryFilter queryFilter = new QueryFilter();
    queryFilter.addOrder(SortProperty.desc("created"));
    addAddressbookRestriction(queryFilter, null);
    if (filter.getMaxRows() > 0) {
      filter.setSortAndLimitMaxRowsWhileSelect(true);
    }
    return getList(queryFilter);
  }

  @Override
  public List<AddressDO> getList(final BaseSearchFilter filter) {
    final AddressFilter myFilter;
    if (filter instanceof AddressFilter) {
      myFilter = (AddressFilter) filter;
    } else {
      myFilter = new AddressFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    if (StringUtils.isBlank(myFilter.getSearchString())) {
      if (!myFilter.isDeleted()) {
        if (myFilter.isNewest()) {
          return getNewest(myFilter);
        }
        if (myFilter.isMyFavorites()) {
          // Show only favorites.
          return personalAddressDao.getMyAddresses();
        }
      }
    } else {
      if (StringUtils.isNumeric(filter.getSearchString())) {
        myFilter.setSearchString("*" + myFilter.getSearchString() + "*");
      }
    }
    if (myFilter.isFilter()) {
      // Proceed contact status:
      // Use filter only for non deleted entries:
      if (myFilter.isActive()
              || myFilter.isNonActive()
              || myFilter.isUninteresting()
              || myFilter.isDeparted()
              || myFilter.isPersonaIngrata()) {
        final Collection<ContactStatus> col = new ArrayList<>();
        if (myFilter.isActive()) {
          col.add(ContactStatus.ACTIVE);
        }
        if (myFilter.isNonActive()) {
          col.add(ContactStatus.NON_ACTIVE);
        }
        if (myFilter.isUninteresting()) {
          col.add(ContactStatus.UNINTERESTING);
        }
        if (myFilter.isDeparted()) {
          col.add(ContactStatus.DEPARTED);
        }
        if (myFilter.isPersonaIngrata()) {
          col.add(ContactStatus.PERSONA_INGRATA);
        }
        queryFilter.add(QueryFilter.isIn("contactStatus", col));
      }

      // Proceed address status:
      // Use filter only for non deleted books:
      if (myFilter.isUptodate() || myFilter.isOutdated() || myFilter.isLeaved()) {
        final Collection<AddressStatus> col = new ArrayList<>();
        if (myFilter.isUptodate()) {
          col.add(AddressStatus.UPTODATE);
        }
        if (myFilter.isOutdated()) {
          col.add(AddressStatus.OUTDATED);
        }
        if (myFilter.isLeaved()) {
          col.add(AddressStatus.LEAVED);
        }
        queryFilter.add(QueryFilter.isIn("addressStatus", col));
      }

      //Add addressbook restriction
      addAddressbookRestriction(queryFilter, myFilter);

    }
    queryFilter.addOrder(SortProperty.asc("name"));
    final List<AddressDO> result = getList(queryFilter);
    if (myFilter.isDoublets()) {
      final HashSet<String> fullnames = new HashSet<>();
      final HashSet<String> doubletFullnames = new HashSet<>();
      for (final AddressDO address : result) {
        final String fullname = getNormalizedFullname(address);
        if (fullnames.contains(fullname)) {
          doubletFullnames.add(fullname);
        }
        fullnames.add(fullname);
      }
      final List<AddressDO> doublets = new LinkedList<>();
      for (final AddressDO address : result) {
        if (doubletFullnames.contains(getNormalizedFullname(address))) {
          doublets.add(address);
        }
      }
      return doublets;
    }
    return result;
  }

  private void addAddressbookRestriction(final QueryFilter queryFilter, final AddressFilter addressFilter) {
    //Addressbook rights check
    Set<Integer> abIdList = new HashSet();
    //First check wicket ui addressbook filter
    if (addressFilter != null && addressFilter.getAddressbooks() != null && addressFilter.getAddressbooks().size() > 0) {
      abIdList.addAll(addressFilter.getAddressbookIds());
    } else {
      //Global addressbook is selectable for every one
      abIdList.add(AddressbookDao.GLOBAL_ADDRESSBOOK_ID);
      //Get all addressbooks for user
      if (addressbookRight == null) {
        addressbookRight = (AddressbookRight) userRights.getRight(UserRightId.MISC_ADDRESSBOOK);
      }
      for (AddressbookDO ab : addressbookDao.internalLoadAll()) {
        if (!ab.isDeleted() && addressbookRight.hasSelectAccess(ThreadLocalUserContext.getUser(), ab)) {
          abIdList.add(ab.getId());
        }
      }
    }
    //Has to be on id value, full entity doesn't work!!!
    queryFilter.createJoin("addressbookList");
    queryFilter.add(QueryFilter.isIn("addressbookList.id", abIdList));
  }

  @Override
  public boolean hasAccess(final PFUserDO user, final AddressDO obj, final AddressDO oldObj,
                           final OperationType operationType,
                           final boolean throwException) {
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
        }
        if (throwException) {
          throw new AccessException(user, "access.exception.userHasNotRight", addressbookRight, operationType);
        }
        return false;
      case INSERT:
        for (AddressbookDO ab : obj.getAddressbookList()) {
          if (addressbookRight.hasInsertAccess(user, ab)) {
            return true;
          }
        }
        if (throwException) {
          throw new AccessException(user, "access.exception.userHasNotRight", addressbookRight, operationType);
        }
        return false;
      case UPDATE:
        for (AddressbookDO ab : obj.getAddressbookList()) {
          if (addressbookRight.hasUpdateAccess(user, ab, ab)) {
            return true;
          }
        }
        if (throwException) {
          throw new AccessException(user, "access.exception.userHasNotRight", addressbookRight, operationType);
        }
        return false;
      case DELETE:
        for (AddressbookDO ab : obj.getAddressbookList()) {
          if (addressbookRight.hasDeleteAccess(user, ab, ab)) {
            return true;
          }
        }
        if (throwException) {
          throw new AccessException(user, "access.exception.userHasNotRight", addressbookRight, operationType);
        }
        return false;
      default:
        if (throwException) {
          throw new AccessException(user, "access.exception.userHasNotRight", addressbookRight, operationType);
        }
        return false;
    }
  }

  @Override
  protected void beforeSaveOrModify(final AddressDO obj) {
    if (obj.getId() == null) {
      if (obj.getAddressbookList() == null) {
        Set<AddressbookDO> addressbookSet = new HashSet<>();
        obj.setAddressbookList(addressbookSet);
      }
      if (obj.getAddressbookList().isEmpty()) {
        obj.getAddressbookList().add(addressbookDao.getGlobalAddressbook());
      }
    } else {
      //Check addressbook changes
      AddressDO dbAddress = internalGetById(obj.getId());
      AddressbookRight addressbookRight = (AddressbookRight) userRights.getRight(UserRightId.MISC_ADDRESSBOOK);
      for (AddressbookDO dbAddressbook : dbAddress.getAddressbookList()) {
        //If user has no right for assigned addressbook, it could not be removed
        if (!addressbookRight.hasSelectAccess(ThreadLocalUserContext.getUser(), dbAddressbook)
                && !obj.getAddressbookList().contains(dbAddressbook)) {
          obj.getAddressbookList().add(dbAddressbook);
        }
      }
    }
  }

  @Override
  protected void onSaveOrModify(final AddressDO obj) {
    beforeSaveOrModify(obj);
  }

  @Override
  protected void onSave(final AddressDO obj) {
    // create uid if empty
    if (StringUtils.isBlank(obj.getUid())) {
      obj.setUid(UUID.randomUUID().toString());
    }
  }

  /**
   * Sets birthday cache as expired.
   *
   * @param obj
   */
  @Override
  protected void afterSaveOrModify(AddressDO obj) {
    TenantRegistryMap.getCache(BirthdayCache.class).setExpired();
  }

  private String getNormalizedFullname(final AddressDO address) {
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
   * @param all      If false, only the birthdays of favorites will be returned.
   * @return The entries are ordered by date of year and name.
   */
  public Set<BirthdayAddress> getBirthdays(final Date fromDate, final Date toDate, final boolean all) {
    BirthdayCache cache = TenantRegistryMap.getCache(BirthdayCache.class);
    return cache.getBirthdays(fromDate, toDate, all, personalAddressDao.getIdList());
  }

  public List<PersonalAddressDO> getFavoriteVCards() {
    final List<PersonalAddressDO> list = personalAddressDao.getList();
    final List<PersonalAddressDO> result = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(list)) {
      for (final PersonalAddressDO entry : list) {
        if (entry.isFavoriteCard()) {
          result.add(entry);
        }
      }
    }
    return result;
  }

  public void exportFavoriteVCards(final Writer out, final List<PersonalAddressDO> favorites) {
    log.info("Exporting personal AddressBook.");
    final PrintWriter pw = new PrintWriter(out);
    for (final PersonalAddressDO entry : favorites) {
      if (!entry.isFavoriteCard()) {
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
  public void exportVCard(final PrintWriter pw, final AddressDO addressDO) {
    if (log.isDebugEnabled()) {
      log.debug("Exporting vCard for addressDo : " + (addressDO != null ? addressDO.getId() : null));
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
    if (isGiven(addressDO.getOrganization()) || isGiven(addressDO.getDivision())) {
      pw.print("ORG:");
      out(pw, addressDO.getOrganization());
      pw.print(';');
      if (isGiven(addressDO.getDivision())) {
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

    if (isGiven(addressDO.getAddressText()) || isGiven(addressDO.getCity())
            || isGiven(addressDO.getZipCode())) {
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
    if (isGiven(addressDO.getPrivateAddressText())
            || isGiven(addressDO.getPrivateCity())
            || isGiven(addressDO.getPrivateZipCode())) {
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
    if (isGiven(addressDO.getComment())) {
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
  public String getFullName(final AddressDO a) {
    final StringBuilder buf = new StringBuilder();
    boolean space = false;
    if (isGiven(a.getName())) {
      buf.append(a.getName());
      space = true;
    }
    if (isGiven(a.getFirstName())) {
      if (space) {
        buf.append(' ');
      } else {
        space = true;
      }
      buf.append(a.getFirstName());
    }
    if (isGiven(a.getTitle())) {
      if (space) {
        buf.append(' ');
      } else {
        space = true;
      }
      buf.append(a.getTitle());
    }
    return buf.toString();
  }

  public List<PersonalAddressDO> getFavoritePhoneEntries() {
    final List<PersonalAddressDO> list = personalAddressDao.getList();
    final List<PersonalAddressDO> result = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(list)) {
      for (final PersonalAddressDO entry : list) {
        if (entry.isFavoriteBusinessPhone()
                || entry.isFavoriteFax()
                || entry.isFavoriteMobilePhone()
                || entry.isFavoritePrivatePhone()) {
          result.add(entry);
        }
      }
    }
    return result;
  }

  /**
   * Throws UserException, if for example the phone list is empty.
   */
  public void exportFavoritePhoneList(final Writer out, final List<PersonalAddressDO> favorites) {
    log.info("Exporting phone list");
    final PrintWriter pw = new PrintWriter(out);
    pw.println("\"Name\",\"Phone number\"");
    for (final PersonalAddressDO entry : favorites) {
      final AddressDO address = entry.getAddress();
      String number = address.getBusinessPhone();
      if (entry.isFavoriteBusinessPhone() && StringUtils.isNotBlank(number)) {
        appendPhoneEntry(pw, address, "", number);
      }
      number = address.getFax();
      if (entry.isFavoriteFax() && StringUtils.isNotBlank(number)) {
        appendPhoneEntry(pw, address, "fax", number);
      }
      number = address.getMobilePhone();
      if (entry.isFavoriteMobilePhone() && StringUtils.isNotBlank(number)) {
        appendPhoneEntry(pw, address, "mobil", number);
      }
      number = address.getPrivateMobilePhone();
      if (entry.isFavoritePrivateMobilePhone() && StringUtils.isNotBlank(number)) {
        final String str = StringUtils.isNotBlank(address.getMobilePhone()) ? "mobil privat" : "mobil";
        appendPhoneEntry(pw, address, str, number);
      }
      number = address.getPrivatePhone();
      if (entry.isFavoritePrivatePhone() && StringUtils.isNotBlank(number)) {
        appendPhoneEntry(pw, address, "privat", number);
      }
    }
    pw.flush();
  }

  private void print(final PrintWriter pw, final String key, final String value) {
    if (!isGiven(value)) {
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
  private void out(final PrintWriter pw, final String str) {
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
        if (!cr) {
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
   */
  private boolean isGiven(final String str) {
    return StringUtils.isNotBlank(str);
  }

  private void appendPhoneEntry(final PrintWriter pw, final AddressDO address, final String suffix, final String number) {
    if (!isGiven(number)) {
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
  public AddressDO newInstance() {
    return new AddressDO();
  }

  public List<AddressDO> findAll() {
    return internalLoadAll();
  }

  public AddressDO findByUid(final String uid) {
    final TenantDO tenant =
            ThreadLocalUserContext.getUser().getTenant() != null ? ThreadLocalUserContext.getUser().getTenant() : tenantService.getDefaultTenant();
    return emgrFactory.runRoTrans(emgr -> emgr.selectSingleAttached(AddressDO.class,
            "SELECT a FROM AddressDO a WHERE a.uid = :uid AND tenant = :tenant", "uid", uid, "tenant", tenant));
  }

  public String internalPhoneLookUp(String phoneNumber) {
    final String searchNumber = NumberHelper.extractPhonenumber(phoneNumber);
    log.info("number=" + phoneNumber + ", searchNumber=" + searchNumber);
    final BaseSearchFilter filter = new BaseSearchFilter();
    filter.setSearchString("*" + searchNumber + "*");
    final QueryFilter queryFilter = new QueryFilter(filter);
    // Use internal get list method for avoiding access checking (no user is logged-in):
    List<AddressDO> resultList = internalGetList(queryFilter);
    final StringBuffer buf = new StringBuffer();
    if (resultList != null && resultList.size() >= 1) {
      AddressDO result = resultList.get(0);
      if (resultList.size() > 1) {
        // More than one result, therefore find the newest one:
        buf.append("+"); // Mark that more than one entry does exist.
        for (final AddressDO matchingUser : resultList) {
          if (matchingUser.getLastUpdate().after(result.getLastUpdate()) == true) {
            result = matchingUser;
          }
        }
      }
      final String fullname = result.getFullName();
      final String organization = result.getOrganization();
      StringHelper.listToString(buf, "; ", fullname, organization);
      return buf.toString();
    }
    return null;
  }
}
