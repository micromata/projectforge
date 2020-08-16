/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.ldap;

import arlut.csd.crypto.SmbEncrypt;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.framework.utils.NumberHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.*;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class LdapUserDao extends LdapDao<String, LdapUser> {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LdapUserDao.class);

  public static final String DEACTIVATED_SUB_CONTEXT = "deactivated";

  private static final String DEACTIVATED_SUB_CONTEXT2 = "ou=" + DEACTIVATED_SUB_CONTEXT;

  private static final String DEACTIVATED_SUB_CONTEXT3 = DEACTIVATED_SUB_CONTEXT2 + ",";

  static final String DEACTIVATED_MAIL = "deactivated@localhost";

  public static final String RESTRICTED_USER_SUB_CONTEXT = "restricted";

  private static final String RESTRICTED_USER_SUB_CONTEXT2 = "ou=" + RESTRICTED_USER_SUB_CONTEXT;

  private static final String RESTRICTED_USER_SUB_CONTEXT3 = RESTRICTED_USER_SUB_CONTEXT2 + ",";

  private boolean useUidInDn = false;

  @Autowired
  private LdapPersonDao ldapPersonDao;

  static String[] ALL_OBJECT_CLASSES;

  static String[] ALL_OBJECT_CLASSES_WITH_POSIX_ACCOUNT;

  static String[] ALL_OBJECT_CLASSES_WITH_SAMBA_ACCOUNT;

  static String[] ALL_OBJECT_CLASSES_WITH_SAMBA_AND_POSIX_ACCOUNT;

  private static String POSIX_OBJECT_CLASS = "posixAccount";

  private static String SAMBA_OBJECT_CLASS = "sambaSamAccount";

  @Autowired
  LdapService ldapService;

  @PostConstruct
  public void init() {
    useUidInDn = true;
  }

  public boolean isDeactivated(final LdapUser user) {
    return user.isDeactivated()
            || user.getOrganizationalUnit() != null
            && LdapUtils.getOu(user.getOrganizationalUnit()).contains(DEACTIVATED_SUB_CONTEXT);
  }

  public boolean isRestrictedUser(final LdapUser user) {
    return user.isRestrictedUser()
            || user.getOrganizationalUnit() != null
            && LdapUtils.getOu(user.getOrganizationalUnit()).contains(RESTRICTED_USER_SUB_CONTEXT);
  }

  public boolean isPosixAccountsConfigured() {
    final LdapConfig ldapConfig = ldapService.getLdapConfig();
    if (ldapConfig == null) {
      return false;
    }
    final LdapPosixAccountsConfig posixAccountsConfig = ldapConfig.getPosixAccountsConfig();
    return posixAccountsConfig != null;
  }

  public boolean isSambaAccountsConfigured() {
    final LdapConfig ldapConfig = ldapService.getLdapConfig();
    if (ldapConfig == null) {
      return false;
    }
    final LdapSambaAccountsConfig sambaAccountsConfig = ldapConfig.getSambaAccountsConfig();
    return sambaAccountsConfig != null && StringUtils.isNotBlank(sambaAccountsConfig.getSambaSIDPrefix());
  }

  /**
   * @see org.projectforge.business.ldap.LdapDao#getObjectClass()
   */
  @Override
  protected String getObjectClass() {
    return ldapPersonDao.getObjectClass();
  }

  /**
   * @see org.projectforge.business.ldap.LdapDao#getAdditionalObjectClasses()
   */
  @Override
  protected String[] getAdditionalObjectClasses() {
    throw new UnsupportedOperationException("Call getAdditionalObjectClasses(LdapUser) instead.");
  }

  void initializeObjectClasses() {
    if (ALL_OBJECT_CLASSES != null) {
      // Already initialized.
      return;
    }
    final List<String> additionalObjectClassesList = new LinkedList<>();
    Collections.addAll(additionalObjectClassesList, ldapPersonDao.getAdditionalObjectClasses());
    ALL_OBJECT_CLASSES = additionalObjectClassesList.toArray(new String[0]);
    additionalObjectClassesList.add(POSIX_OBJECT_CLASS);
    ALL_OBJECT_CLASSES_WITH_POSIX_ACCOUNT = additionalObjectClassesList.toArray(new String[0]);
    additionalObjectClassesList.add(SAMBA_OBJECT_CLASS);
    ALL_OBJECT_CLASSES_WITH_SAMBA_AND_POSIX_ACCOUNT = additionalObjectClassesList.toArray(new String[0]);
    additionalObjectClassesList.clear();
    additionalObjectClassesList.addAll(Arrays.asList(ldapPersonDao.getAdditionalObjectClasses()));
    additionalObjectClassesList.add(SAMBA_OBJECT_CLASS);
    ALL_OBJECT_CLASSES_WITH_SAMBA_ACCOUNT = additionalObjectClassesList.toArray(new String[0]);
  }

  /**
   * @see org.projectforge.business.ldap.LdapDao#getAdditionalObjectClasses(org.projectforge.business.ldap.LdapObject)
   */
  @Override
  protected String[] getAdditionalObjectClasses(final LdapUser obj) {
    final boolean posixAccount = isPosixAccountsConfigured()
            && !PFUserDOConverter.isPosixAccountValuesEmpty(obj);
    final boolean sambaAccount = isSambaAccountsConfigured()
            && !PFUserDOConverter.isSambaAccountValuesEmpty(obj);
    if (ALL_OBJECT_CLASSES == null) {
      initializeObjectClasses();
    }
    if (posixAccount) {
      if (sambaAccount) {
        return ALL_OBJECT_CLASSES_WITH_SAMBA_AND_POSIX_ACCOUNT;
      }
      return ALL_OBJECT_CLASSES_WITH_POSIX_ACCOUNT;
    }
    if (sambaAccount) {
      return ALL_OBJECT_CLASSES_WITH_SAMBA_ACCOUNT;
    }
    return ALL_OBJECT_CLASSES;
  }

  /**
   * @see org.projectforge.business.ldap.LdapDao#getIdAttrId()
   */
  @Override
  public String getIdAttrId() {
    return "employeeNumber";
  }

  /**
   * @see org.projectforge.business.ldap.LdapDao#getId(org.projectforge.business.ldap.LdapUser)
   */
  @Override
  public String getId(final LdapUser obj) {
    return obj.getEmployeeNumber();
  }

  /**
   * @see org.projectforge.business.ldap.LdapDao#mapToObject(java.lang.String, javax.naming.directory.Attributes)
   */
  @Override
  protected LdapUser mapToObject(final String dn, final Attributes attributes) throws NamingException {
    final LdapUser user = new LdapUser();
    ldapPersonDao.mapToObject(dn, user, attributes);
    ldapConfig = ldapService.getLdapConfig();
    final boolean posixAccountsConfigured = isPosixAccountsConfigured();
    final boolean sambaAccountsConfigured = isSambaAccountsConfigured();
    if (posixAccountsConfigured || sambaAccountsConfigured) {
      final String no = LdapUtils.getAttributeStringValue(attributes, "uidNumber");
      user.setUidNumber(NumberHelper.parseInteger(no));
    }
    if (posixAccountsConfigured) {
      final String no = LdapUtils.getAttributeStringValue(attributes, "gidNumber");
      user.setGidNumber(NumberHelper.parseInteger(no));
      user.setHomeDirectory(LdapUtils.getAttributeStringValue(attributes, "homeDirectory"));
      user.setLoginShell(LdapUtils.getAttributeStringValue(attributes, "loginShell"));
    }
    if (sambaAccountsConfigured) {
      final String sambaSID = LdapUtils.getAttributeStringValue(attributes, "sambaSID");
      final Integer sambaSIDNumber = ldapConfig.getSambaAccountsConfig().getSambaSIDNumber(sambaSID);
      user.setSambaSIDNumber(sambaSIDNumber);
      final String sambaPrimaryGroupSID = LdapUtils.getAttributeStringValue(attributes, "sambaPrimaryGroupSID");
      final Integer sambaPrimaryGroupSIDNumber = ldapConfig.getSambaAccountsConfig()
              .getSambaSIDNumber(sambaPrimaryGroupSID);
      user.setSambaPrimaryGroupSIDNumber(sambaPrimaryGroupSIDNumber);
      user.setSambaNTPassword(LdapUtils.getAttributeStringValue(attributes, "sambaNTPassword"));
      final String sambaPwdLastSet = LdapUtils.getAttributeStringValue(attributes, "sambaPwdLastSet");
      if (sambaPwdLastSet != null) {
        final Long value = NumberHelper.parseLong(sambaPwdLastSet);  // seconds since 1970
        if (value != null) {
          user.setSambaPwdLastSet(new Date(value * 1000));
        }
      }
    }
    if (dn != null) {
      if (dn.contains(DEACTIVATED_SUB_CONTEXT2)) {
        user.setDeactivated(true);
      }
      if (dn.contains(RESTRICTED_USER_SUB_CONTEXT2)) {
        user.setRestrictedUser(true);
      }

    }
    final Object userPassword = LdapUtils.getAttributeValue(attributes, "userPassword");
    if (userPassword != null) {
      user.setPasswordGiven(true);
    }
    return user;
  }

  public void deactivateUser(final LdapUser user) {
    new LdapTemplate(ldapConnector) {
      @Override
      protected Object call() throws NameNotFoundException, Exception {
        deactivateUser(ctx, user);
        return null;
      }
    }.excecute();
  }

  public void deactivateUser(final DirContext ctx, final LdapUser user) throws NamingException {
    log.info("Deactivate user: " + buildDn(null, user));
    final List<ModificationItem> modificationItems = new ArrayList<>();
    modificationItems.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("userPassword", null)));
    modificationItems
            .add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("mail", DEACTIVATED_MAIL)));
    buildDn(null, user);
    modify(ctx, user, modificationItems);
    final String ou = user.getOrganizationalUnit();
    if (!ou.startsWith(DEACTIVATED_SUB_CONTEXT2)) {
      // Move user to the sub-context "deactivated".
      final String newOu = LdapUtils.getOu(DEACTIVATED_SUB_CONTEXT, getOuBase());
      move(ctx, user, newOu);
      user.setOrganizationalUnit(newOu);
    }
  }

  @Override
  public String getOuBase() {
    return ldapConfig.getUserBase();
  }

  /**
   * Moves the user only from the "deactivated" sub-context to the parent context. If the user isn't in the context name
   * "deactivated" nothing will be done.
   *
   * @param user
   */
  public void reactivateUser(final LdapUser user) {
    new LdapTemplate(ldapConnector) {
      @Override
      protected Object call() throws NameNotFoundException, Exception {
        reactivateUser(ctx, user);
        return null;
      }
    }.excecute();
  }

  public void reactivateUser(final DirContext ctx, final LdapUser user) throws NamingException {
    log.info("Reactivate deactivated user: " + buildDn(null, user));
    final String ou = LdapUtils.getOu(user.getOrganizationalUnit());
    if (!ou.startsWith(DEACTIVATED_SUB_CONTEXT2)) {
      log.info("Object isn't in a deactivated sub-context, nothing will be done: " + buildDn(null, user));
      return;
    }
    String newPath;
    if (ou.startsWith(DEACTIVATED_SUB_CONTEXT3)) {
      newPath = ou.substring(DEACTIVATED_SUB_CONTEXT3.length());
    } else {
      newPath = ou.substring(DEACTIVATED_SUB_CONTEXT2.length());
    }
    move(ctx, user, newPath);
    user.setOrganizationalUnit(newPath);
  }

  void updateActivatedStatus(final DirContext ctx, final LdapUser user) throws NamingException {
    final String ou = LdapUtils.getOu(user.getOrganizationalUnit());
    if (user.isDeactivated()) {
      if (ou.startsWith(DEACTIVATED_SUB_CONTEXT2)) {
        // User is already stored in deactivated context. Nothing to be done.
        return;
      } else {
        deactivateUser(ctx, user);
      }
    } else {
      if (!ou.startsWith(DEACTIVATED_SUB_CONTEXT2)) {
        // User isn't stored in deactivated context. Nothing to be done.
        return;
      } else {
        reactivateUser(ctx, user);
      }
    }
  }

  void updateRestrictedUserStatus(final DirContext ctx, final LdapUser user) throws NamingException {
    final String ou = LdapUtils.getOu(user.getOrganizationalUnit());
    if (user.isDeactivated()) {
      // User is deactivated, thus the restricted-user-status is ignored.
      return;
    }
    if (user.isRestrictedUser()) {
      if (ou.startsWith(RESTRICTED_USER_SUB_CONTEXT2)) {
        // User is already stored in restricted context. Nothing to be done.
        return;
      } else {
        setUserAsRestrictedUser(ctx, user);
      }
    } else {
      if (!ou.startsWith(RESTRICTED_USER_SUB_CONTEXT2)) {
        // User isn't stored in restricted context. Nothing to be done.
        return;
      } else {
        log.info("Move user from restricted sub context: " + buildDn(null, user));
        String newPath;
        if (ou.startsWith(RESTRICTED_USER_SUB_CONTEXT3)) {
          newPath = ou.substring(RESTRICTED_USER_SUB_CONTEXT3.length());
        } else {
          newPath = ou.substring(RESTRICTED_USER_SUB_CONTEXT2.length());
        }
        move(ctx, user, newPath);
        user.setOrganizationalUnit(newPath);
      }
    }
  }

  private void setUserAsRestrictedUser(final DirContext ctx, final LdapUser user) throws NamingException {
    log.info("Move user to restricted sub context: " + buildDn(null, user));
    if (user.isDeactivated()) {
      log.info("User is deactivated, thus the restricted-user-status is ignored: " + buildDn(null, user));
      return;
    }
    final String ou = user.getOrganizationalUnit();
    if (!ou.startsWith(RESTRICTED_USER_SUB_CONTEXT2)) {
      // Move user to the sub-context "restricted".
      final String newOu = LdapUtils.getOu(RESTRICTED_USER_SUB_CONTEXT, user.getOrganizationalUnit());
      move(ctx, user, newOu);
      user.setOrganizationalUnit(newOu);
    }
  }

  /**
   * Calls super method and {@link #deactivateUser(DirContext, LdapUser)} if the given user is deactivated. If the given
   * user is deleted, nothing will be done.
   *
   * @see org.projectforge.business.ldap.LdapDao#create(javax.naming.directory.DirContext, org.projectforge.business.ldap.LdapObject,
   * java.lang.Object[])
   */
  @Override
  public void create(final DirContext ctx, final String ouBase, final LdapUser user, final Object... args)
          throws NamingException {
    if (user.isDeleted()) {
      log.info(
              "Given LDAP user is deleted, so the user will not be created in the LDAP system (nothing will be done).");
      return;
    }
    if (user.getUid() == null) {
      log.info(
              "Given LDAP user has UID null, so the user will be skipped: " + user);
      return;
    }
    super.create(ctx, ouBase, user, args);
    if (user.isDeactivated()) {
      deactivateUser(ctx, user);
    } else if (user.isRestrictedUser()) {
      // Deactivated users shouldn't be moved to restricted ou sub context.
      setUserAsRestrictedUser(ctx, user);
    }
  }

  /**
   * @see org.projectforge.business.ldap.LdapDao#update(javax.naming.directory.DirContext, org.projectforge.business.ldap.LdapObject,
   * java.lang.Object[])
   */
  @Override
  public void update(final DirContext ctx, final String ouBase, final LdapUser user, final Object... objs)
          throws NamingException {
    if (user.isDeleted()) {
      log.info("Given LDAP user is deleted, so the user will be removed from the LDAP system.");
      delete(ctx, user);
      return;
    }
    super.update(ctx, ouBase, user, objs);
    updateActivatedStatus(ctx, user);
    updateRestrictedUserStatus(ctx, user);
  }

  public void changePassword(final LdapUser user, final String oldPassword, final String newPassword) {
    final String userPasswordId = "userPassword";
    log.info("Change attribute " + userPasswordId + " for " + getObjectClass() + ": " + buildDn(null, user));
    final List<ModificationItem> modificationItems = new ArrayList<>();
    if (oldPassword != null) {
      modificationItems
              .add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(userPasswordId, oldPassword)));
      modificationItems
              .add(new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute(userPasswordId, newPassword)));
    } else {
      modificationItems
              .add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(userPasswordId, newPassword)));
    }
    // Perform the update
    modify(user, modificationItems);
  }

  public void changeWlanPassword(final LdapUser user, final String newPassword) {
    final String sambaPasswordAttributeId = "sambaNTPassword";

    if (!isSambaAccountsConfigured()) {
      log.error("Could not change attribute " + sambaPasswordAttributeId + " because the samba accounts are not configured.");
      return;
    }

    if (user.getSambaSIDNumber() == null) {
      log.error("Could not change attribute " + sambaPasswordAttributeId + " because the sambaSID is null.");
      return;
    }

    log.info("Change attribute " + sambaPasswordAttributeId + " for " + getObjectClass() + ": " + buildDn(null, user));
    final String sambaNTPassword = SmbEncrypt.NTUNICODEHash(newPassword);
    log.info("Checksum (for debugging): " + sambaNTPassword.substring(0, 4) + "...");
    final ModificationItem modItem = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(sambaPasswordAttributeId, sambaNTPassword));
    // Perform the update
    modify(user, Collections.singletonList(modItem));
  }

  public LdapUser findByUsername(final Object username, final String... organizationalUnits) {
    return (LdapUser) new LdapTemplate(ldapConnector) {
      @Override
      protected Object call() throws NameNotFoundException, Exception {
        NamingEnumeration<?> results = null;
        final SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        final String searchBase = getSearchBase(organizationalUnits);
        results = ctx.search(searchBase, "(&(objectClass=" + getObjectClass() + ")(uid=" + username + "))", controls);
        if (!results.hasMore()) {
          return null;
        }
        final SearchResult searchResult = (SearchResult) results.next();
        final String dn = searchResult.getName();
        final Attributes attributes = searchResult.getAttributes();
        if (results.hasMore()) {
          log.error("Oups, found entries with multiple id's: " + getObjectClass() + "." + username);
        }
        return mapToObject(dn, searchBase, attributes);
      }
    }.excecute();
  }

  public LdapUser authenticate(final String username, final String userPassword, final String... organizationalUnits) {
    String dn;
    LdapUser user = null;
    final String searchBase = getSearchBase(organizationalUnits);
    if (StringUtils.isNotBlank(ldapConfig.getManagerUser())
            && StringUtils.isNotBlank(ldapConfig.getManagerPassword())) {
      user = findByUsername(username, searchBase);
      if (user == null || !StringUtils.equals(username, user.getId())) {
        log.info("User with id '" + username + "' not found.");
        return null;
      }
      dn = user.getDn() + "," + ldapConnector.getBase();
    } else {
      dn = "uid=" + username + "," + searchBase + "," + ldapConnector.getBase();
    }
    try {
      ldapConnector.createContext(dn, userPassword);
      log.info("User '" + username + "' (" + dn + ") successfully authenticated.");
      return user;
    } catch (final Exception ex) {
      log.error("User '" + username + "' (" + dn + ") with invalid credentials.");
      return null;
    }
  }

  /**
   * @see org.projectforge.business.ldap.LdapDao#createAndAddModificationItems(java.util.List, java.lang.String,
   * java.lang.String[])
   */
  @Override
  protected void createAndAddModificationItems(final List<ModificationItem> list, final String attrId,
                                               final String... attrValues) {
    if ("uid".equals(attrId)) {
      // Don't change uid because it's part of the dn.
      return;
    }
    super.createAndAddModificationItems(list, attrId, attrValues);
  }

  /**
   * @see org.projectforge.ldap.LdapPDao#getModificationItems(java.util.List, org.projectforge.business.ldap.LdapUser)
   */
  @Override
  protected List<ModificationItem> getModificationItems(List<ModificationItem> list, final LdapUser user) {
    list = ldapPersonDao.getModificationItems(list, user);
    createAndAddModificationItems(list, "cn", user.getCommonName());
    final boolean modifyPosixAccount = isPosixAccountsConfigured()
            && !PFUserDOConverter.isPosixAccountValuesEmpty(user);
    final boolean modifySambaAccount = isSambaAccountsConfigured()
            && !PFUserDOConverter.isSambaAccountValuesEmpty(user);
    if (modifyPosixAccount || modifySambaAccount) {
      if (user.getObjectClasses() != null) {
        final List<String> missedObjectClasses = LdapUtils.getMissedObjectClasses(getAdditionalObjectClasses(user),
                getObjectClass(),
                user.getObjectClasses());
        if (CollectionUtils.isNotEmpty(missedObjectClasses)) {
          for (final String missedObjectClass : missedObjectClasses) {
            list.add(createModificationItem(DirContext.ADD_ATTRIBUTE, "objectClass", missedObjectClass));
          }
        }
      }
    }
    if (modifyPosixAccount) {
      createAndAddModificationItems(list, "uidNumber", String.valueOf(user.getUidNumber()));
      createAndAddModificationItems(list, "gidNumber", String.valueOf(user.getGidNumber()));
      createAndAddModificationItems(list, "homeDirectory", user.getHomeDirectory());
      createAndAddModificationItems(list, "loginShell", user.getLoginShell());
    }
    if (modifySambaAccount) {
      createAndAddModificationItems(list, "sambaSID",
              ldapConfig.getSambaAccountsConfig().getSambaSID(user.getSambaSIDNumber()));
      createAndAddModificationItems(list, "sambaPrimaryGroupSID",
              ldapConfig.getSambaAccountsConfig().getSambaPrimaryGroupSID(user.getSambaPrimaryGroupSIDNumber()));
      createAndAddModificationItems(list, "sambaAcctFlags", "U          ");
      createAndAddModificationItems(list, "sambaPasswordHistory",
              "0000000000000000000000000000000000000000000000000000000000000000");
      createAndAddModificationItems(list, "sambaPwdLastSet",
              String.valueOf(user.getSambaPwdLastSetAsUnixEpochSeconds()));
    }
    return list;
  }

  /**
   * @see org.projectforge.business.ldap.LdapDao#buildDnIdentifier(org.projectforge.business.ldap.LdapObject)
   */
  @Override
  protected String buildDnIdentifier(final LdapUser obj) {
    if (useUidInDn) {
      return "uid=" + obj.getUid();
    } else {
      return "cn=" + LdapUtils.escapeCommonName(obj.getCommonName());
    }
  }

  /**
   * @see org.projectforge.business.ldap.LdapDao#buildId(java.lang.Object)
   */
  @Override
  protected String buildId(final Object id) {
    if (id == null) {
      return null;
    }
    if (id instanceof String && ((String) id).startsWith(PFUserDOConverter.ID_PREFIX)) {
      return String.valueOf(id);
    }
    return PFUserDOConverter.ID_PREFIX + id;
  }

  /**
   * @param ldapPersonDao the ldapPersonDao to set
   */
  public void setLdapPersonDao(final LdapPersonDao ldapPersonDao) {
    this.ldapPersonDao = ldapPersonDao;
  }
}
