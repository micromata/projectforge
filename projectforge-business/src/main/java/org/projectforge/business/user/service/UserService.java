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

package org.projectforge.business.user.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.login.Login;
import org.projectforge.business.login.PasswordCheckResult;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.multitenancy.TenantService;
import org.projectforge.business.password.PasswordQualityService;
import org.projectforge.business.user.*;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.configuration.SecurityConfig;
import org.projectforge.framework.i18n.I18nKeyAndParams;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.history.HistoryBaseDaoAdapter;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.framework.utils.Crypt;
import org.projectforge.framework.utils.NumberHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserService.class);

  private static final String MESSAGE_KEY_OLD_PASSWORD_WRONG = "user.changePassword.error.oldPasswordWrong";

  private static final String MESSAGE_KEY_LOGIN_PASSWORD_WRONG = "user.changeWlanPassword.error.loginPasswordWrong";
  private final UsersComparator usersComparator = new UsersComparator();
  private UserGroupCache userGroupCache;
  private UserTokenCache userTokenCache;
  private ConfigurationService configurationService;
  private UserDao userDao;
  private UserAuthenticationsDao userAuthenticationsDao;
  private AccessChecker accessChecker;
  private TenantService tenantService;
  private PasswordQualityService passwordQualityService;

  /**
   * Needed by Wicket for proxying.
   */
  public UserService() {
  }

  @Autowired
  public UserService(AccessChecker accessChecker,
                     ConfigurationService configurationService,
                     PasswordQualityService passwordQualityService,
                     TenantService tenantService,
                     UserDao userDao,
                     UserAuthenticationsDao userAuthenticationsDao,
                     UserTokenCache userTokenCache) {
    this.accessChecker = accessChecker;
    this.configurationService = configurationService;
    this.passwordQualityService = passwordQualityService;
    this.tenantService = tenantService;
    this.userDao = userDao;
    this.userAuthenticationsDao = userAuthenticationsDao;
    this.userTokenCache = userTokenCache;
  }

  /**
   * @param userIds
   * @return
   */
  public List<String> getUserNames(final String userIds) {
    if (StringUtils.isEmpty(userIds)) {
      return null;
    }
    final int[] ids = StringHelper.splitToInts(userIds, ",", false);
    final List<String> list = new ArrayList<>();
    for (final int id : ids) {
      final PFUserDO user = getUserGroupCache().getUser(id);
      if (user != null) {
        list.add(user.getFullname());
      } else {
        log.warn("User with id '" + id + "' not found in UserGroupCache. userIds string was: " + userIds);
      }
    }
    return list;
  }

  public Collection<PFUserDO> getSortedUsers() {
    TreeSet<PFUserDO> sortedUsers = new TreeSet<>(usersComparator);
    final Collection<PFUserDO> allusers = getUserGroupCache().getAllUsers();
    final PFUserDO loggedInUser = ThreadLocalUserContext.getUser();
    for (final PFUserDO user : allusers) {
      if (!user.isDeleted() && !user.getDeactivated()
              && userDao.hasUserSelectAccess(loggedInUser, user, false)) {
        sortedUsers.add(user);
      }
    }
    return sortedUsers;
  }

  /**
   * @param userIds
   * @return
   */
  public Collection<PFUserDO> getSortedUsers(final String userIds) {
    if (StringUtils.isEmpty(userIds)) {
      return null;
    }
    TreeSet<PFUserDO> sortedUsers = new TreeSet<>(usersComparator);
    final int[] ids = StringHelper.splitToInts(userIds, ",", false);
    for (final int id : ids) {
      final PFUserDO user = getUserGroupCache().getUser(id);
      if (user != null) {
        sortedUsers.add(user);
      } else {
        log.warn("Group with id '" + id + "' not found in UserGroupCache. groupIds string was: " + userIds);
      }
    }
    return sortedUsers;
  }

  public String getUserIds(final Collection<PFUserDO> users) {
    final StringBuffer buf = new StringBuffer();
    boolean first = true;
    for (final PFUserDO user : users) {
      if (user.getId() != null) {
        first = StringHelper.append(buf, first, String.valueOf(user.getId()), ",");
      }
    }
    return buf.toString();
  }

  /**
   * @return the useruserCache
   */
  private UserGroupCache getUserGroupCache() {
    if (userGroupCache == null) {
      userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
    }
    return userGroupCache;
  }

  public List<PFUserDO> getAllUsers() {
    try {
      TenantDO tenant = ThreadLocalUserContext.getUser().getTenant() != null ? ThreadLocalUserContext.getUser().getTenant() : tenantService.getDefaultTenant();
      return userDao.internalLoadAll(tenant);
    } catch (final Exception ex) {
      log.error(
              "******* Exception while getting users from data-base (OK only in case of migration from older versions): "
                      + ex.getMessage(),
              ex);
      return new ArrayList<>();
    }
  }

  public List<PFUserDO> getAllActiveUsers() {
    return getAllUsers().stream().filter(u -> !u.getDeactivated() && !u.isDeleted()).collect(Collectors.toList());
  }

  /**
   * @param userId
   * @param type
   * @param authenticationToken
   * @param userIdAttribute     The required http attribute (only for logging purposes)
   * @param tokenAttribute      The required http attribute (only for logging purposes)
   * @return
   */
  public boolean checkAuthenticationToken(final Integer userId, UserTokenType type, String authenticationToken, String userIdAttribute, String tokenAttribute) {
    String storedAuthenticationToken = getAuthenticationToken(userId, type);
    if (storedAuthenticationToken == null) {
      log.error(userIdAttribute + " '" + userId + "' does not exist. Authentication failed.");
    } else if (authenticationToken == null) {
      log.error(tokenAttribute + " not given for userId '" + userId + "'. Authentication failed.");
    } else if (authenticationToken.equals(storedAuthenticationToken)) {
      return true;
    } else {
      log.error(tokenAttribute + " doesn't match for " + userIdAttribute + " '" + userId + "'. Authentication failed.");
    }
    return false;
  }

  /**
   * Decrypts a given string encrypted with selected token (selected by UserTokenType).
   *
   * @param userId
   * @param encryptedString
   * @return The decrypted string.
   * @see Crypt#decrypt(String, String)
   */
  public String decrypt(final Integer userId, UserTokenType type, final String encryptedString) {
    String storedAuthenticationToken = getAuthenticationToken(userId, type);
    if (storedAuthenticationToken == null) {
      log.warn("Can't get authentication token for user " + userId + ". So can't decrypt encrypted string.");
      return "";
    }
    final String authenticationToken = StringUtils.rightPad(storedAuthenticationToken, 32, "x");
    return Crypt.decrypt(authenticationToken, encryptedString);
  }

  /**
   * Encrypts the given str with AES. The key is the selected authenticationToken of the given user (by id) (first 16
   * bytes of it).
   *
   * @param userId
   * @param data
   * @return The base64 encoded result (url safe).
   * @see Crypt#encrypt(String, String)
   */
  public String encrypt(final Integer userId, final UserTokenType type, final String data) {
    String storedAuthenticationToken = getAuthenticationToken(userId, type);
    if (storedAuthenticationToken == null) {
      log.warn("Can't get authentication token for user " + userId + ". So can't encrypt string.");
      return "";
    }
    final String authenticationToken = StringUtils.rightPad(storedAuthenticationToken, 32, "x");
    return Crypt.encrypt(authenticationToken, data);
  }

  /**
   * Uses the context user.
   *
   * @param type The token to use for encryption.
   * @param data
   * @return
   */
  public String encrypt(final UserTokenType type, final String data) {
    return encrypt(ThreadLocalUserContext.getUserId(), type, data);
  }

  /**
   * @param userId
   * @return The user from UserGroupCache.
   */
  public PFUserDO getUser(Integer userId) {
    return getUserGroupCache().getUser(userId);
  }

  public Collection<Integer> getAssignedTenants(final PFUserDO user) {
    final PFUserDO u = getUserGroupCache().getUser(user.getId());
    return userDao.getAssignedTenants(u);
  }

  /**
   * Encrypts the password with a new generated salt string and the pepper string if configured any.
   *
   * @param user     The user to user.
   * @param password as clear text.
   * @see Crypt#digest(String)
   */
  public void createEncryptedPassword(final PFUserDO user, final String password) {
    final String saltString = createSaltString();
    user.setPasswordSalt(saltString);
    final String encryptedPassword = Crypt.digest(getPepperString() + saltString + password);
    user.setPassword(encryptedPassword);
  }

  private String createSaltString() {
    return NumberHelper.getSecureRandomBase64String(10);
  }

  /**
   * Changes the user's password. Checks the password quality and the correct authentication for the old password
   * before. Also the stay-logged-in-key will be renewed, so any existing stay-logged-in cookie will be invalid.
   *
   * @param user
   * @param oldPassword
   * @param newPassword
   * @return Error message key if any check failed or null, if successfully changed.
   */
  public List<I18nKeyAndParams> changePassword(PFUserDO user, final String oldPassword, final String newPassword) {
    Validate.notNull(user);
    Validate.notNull(oldPassword);
    Validate.notNull(newPassword);

    final List<I18nKeyAndParams> errorMsgKeys = passwordQualityService.checkPasswordQuality(oldPassword, newPassword);
    if (!errorMsgKeys.isEmpty()) {
      return errorMsgKeys;
    }

    accessChecker.checkRestrictedOrDemoUser();
    user = getUser(user.getUsername(), oldPassword, false);
    if (user == null) {
      return Collections.singletonList(new I18nKeyAndParams(MESSAGE_KEY_OLD_PASSWORD_WRONG));
    }

    createEncryptedPassword(user, newPassword);
    onPasswordChange(user, true);
    Login.getInstance().passwordChanged(user, newPassword);
    log.info("Password changed for user: " + user.getId() + " - " + user.getUsername());
    return Collections.emptyList();
  }

  /**
   * Changes the user's WLAN password. Checks the password quality and the correct authentication for the login password before.
   *
   * @param user
   * @param loginPassword
   * @param newWlanPassword
   * @return Error message key if any check failed or null, if successfully changed.
   */
  public List<I18nKeyAndParams> changeWlanPassword(PFUserDO user, final String loginPassword, final String newWlanPassword) {
    Validate.notNull(user);
    Validate.notNull(loginPassword);
    Validate.notNull(newWlanPassword);

    final List<I18nKeyAndParams> errorMsgKeys = passwordQualityService.checkPasswordQuality(newWlanPassword);
    if (!errorMsgKeys.isEmpty()) {
      return errorMsgKeys;
    }

    accessChecker.checkRestrictedOrDemoUser();
    user = getUser(user.getUsername(), loginPassword, false); // get user from DB to persist the change of the wlan password time
    if (user == null) {
      return Collections.singletonList(new I18nKeyAndParams(MESSAGE_KEY_LOGIN_PASSWORD_WRONG));
    }

    onWlanPasswordChange(user, true); // set last change time and creaty history entry
    Login.getInstance().wlanPasswordChanged(user, newWlanPassword); // change the wlan password
    log.info("WLAN Password changed for user: " + user.getId() + " - " + user.getUsername());
    return Collections.emptyList();
  }

  public void onPasswordChange(final PFUserDO user, final boolean createHistoryEntry) {
    user.checkAndFixPassword();
    if (user.getPassword() != null) {
      if (createHistoryEntry) {
        HistoryBaseDaoAdapter.wrapHistoryUpdate(user, () -> {
          user.setLastPasswordChange(new Date());
          return ModificationStatus.MAJOR;
        });
      } else {
        user.setLastPasswordChange(new Date());
      }
      renewAuthenticationToken(user.getId(), UserTokenType.STAY_LOGGED_IN_KEY);
      renewAuthenticationToken(user.getId(), UserTokenType.REST_CLIENT);
    } else {
      throw new IllegalArgumentException(
              "Given password seems to be not encrypted! Aborting due to security reasons (for avoiding storage of clear password in the database).");
    }
  }

  public void onWlanPasswordChange(final PFUserDO user, final boolean createHistoryEntry) {
    if (createHistoryEntry) {
      HistoryBaseDaoAdapter.wrapHistoryUpdate(user, () -> {
        user.setLastWlanPasswordChange(new Date());
        return ModificationStatus.MAJOR;
      });
    } else {
      user.setLastWlanPasswordChange(new Date());
    }
  }

  @SuppressWarnings("unchecked")
  protected PFUserDO getUser(final String username, final String password, final boolean updateSaltAndPepperIfNeeded) {
    final List<PFUserDO> list = userDao.findByUsername(username);
    if (list == null || list.isEmpty() || list.get(0) == null) {
      return null;
    }
    final PFUserDO user = list.get(0);
    final PasswordCheckResult passwordCheckResult = checkPassword(user, password);
    if (!passwordCheckResult.isOK()) {
      return null;
    }
    if (updateSaltAndPepperIfNeeded && passwordCheckResult.isPasswordUpdateNeeded()) {
      log.info("Giving salt and/or pepper to the password of the user " + user.getId() + ".");
      createEncryptedPassword(user, password);
      userDao.internalUpdate(user);
    }
    return user;
  }

  /**
   * Checks the given password by comparing it with the stored user password. For backward compatibility the password is
   * encrypted with and without pepper (if configured). The salt string of the given user is used.
   *
   * @param user
   * @param password as clear text.
   * @return true if the password matches the user's password.
   */
  public PasswordCheckResult checkPassword(final PFUserDO user, final String password) {
    if (user == null) {
      log.warn("User not given in checkPassword(PFUserDO, String) method.");
      return PasswordCheckResult.FAILED;
    }
    final PFUserDO internalUser = userDao.internalGetById(user.getId());
    if (internalUser == null) {
      log.warn("Can't load user; " + user.getId());
      return PasswordCheckResult.FAILED;
    }
    final String userPassword = internalUser.getPassword();
    if (StringUtils.isBlank(userPassword)) {
      log.warn("User's password is blank, can't checkPassword(PFUserDO, String) for user with id " + user.getId());
      return PasswordCheckResult.FAILED;
    }
    String saltString = internalUser.getPasswordSalt();
    if (saltString == null) {
      saltString = "";
    }
    final String pepperString = getPepperString();
    String encryptedPassword = Crypt.digest(pepperString + saltString + password);
    if (userPassword.equals(encryptedPassword)) {
      // Passwords match!
      if (StringUtils.isEmpty(saltString)) {
        log.info("Password of user " + user.getId() + " with username '" + user.getUsername() + "' is not yet salted!");
        return PasswordCheckResult.OK_WITHOUT_SALT;
      }
      return PasswordCheckResult.OK;
    }
    if (StringUtils.isNotBlank(pepperString)) {
      // Check password without pepper:
      encryptedPassword = Crypt.digest(saltString + password);
      if (userPassword.equals(encryptedPassword)) {
        // Passwords match!
        if (StringUtils.isEmpty(saltString)) {
          log.info("Password of user " + user.getId() + " with username '" + user.getUsername()
                  + "' is not yet salted and has no pepper!");
          return PasswordCheckResult.OK_WITHOUT_SALT_AND_PEPPER;
        }
        log.info("Password of user " + user.getId() + " with username '" + user.getUsername() + "' has no pepper!");
        return PasswordCheckResult.OK_WITHOUT_PEPPER;
      }
    }
    return PasswordCheckResult.FAILED;
  }

  /**
   * Ohne Zugangsbegrenzung. Wird bei Anmeldung benötigt.
   */
  public PFUserDO authenticateUser(final String username, final String password) {
    Validate.notNull(username);
    Validate.notNull(password);

    PFUserDO user = getUser(username, password, true);
    if (user != null) {

      final int loginFailures = user.getLoginFailures();
      final Date lastLogin = user.getLastLogin();
      userDao.updateUserAfterLoginSuccess(user);
      if (!user.hasSystemAccess()) {
        log.warn("Deleted/deactivated user tried to login: " + user);
        return null;
      }
      final PFUserDO contextUser = PFUserDO.createCopyWithoutSecretFields(user);
      contextUser.setLoginFailures(loginFailures); // Restore loginFailures for current user session.
      contextUser.setLastLogin(lastLogin); // Restore lastLogin for current user session.
      return contextUser;
    }
    userDao.updateIncrementLoginFailure(username);
    return null;
  }

  private String getPepperString() {
    final SecurityConfig securityConfig = configurationService.getSecurityConfig();
    if (securityConfig != null) {
      return securityConfig.getPasswordPepper();
    }
    return "";
  }

  public PFUserDO getInternalByUsername(String username) {
    return userDao.getInternalByName(username);
  }

  /**
   * @param id
   * @return the user from db (UserDao).
   */
  public PFUserDO getById(Serializable id) {
    return userDao.getById(id);
  }

  public PFUserDO internalGetById(Serializable id) {
    return userDao.internalGetById(id);
  }

  public Integer save(PFUserDO user) {
    return userDao.internalSave(user);
  }

  public void markAsDeleted(PFUserDO user) {
    userDao.internalMarkAsDeleted(user);
  }

  public boolean doesUsernameAlreadyExist(PFUserDO user) {
    return userDao.doesUsernameAlreadyExist(user);
  }

  public ModificationStatus update(PFUserDO user) {
    return userDao.update(user);
  }

  /**
   * Without access checking!!! Secret fields are cleared.
   *
   * @see UserDao#internalLoadAll()
   */
  public List<PFUserDO> internalLoadAll() {
    return userDao.internalLoadAll();
  }

  public String getNormalizedPersonalPhoneIdentifiers(final PFUserDO user) {
    if (StringUtils.isNotBlank(user.getPersonalPhoneIdentifiers())) {
      final String[] ids = getPersonalPhoneIdentifiers(user);
      if (ids != null) {
        final StringBuilder buf = new StringBuilder();
        boolean first = true;
        for (final String id : ids) {
          if (first) {
            first = false;
          } else {
            buf.append(",");
          }
          buf.append(id);
        }
        return buf.toString();
      }
    }
    return null;
  }

  public String[] getPersonalPhoneIdentifiers(final PFUserDO user) {
    final String[] tokens = StringUtils.split(user.getPersonalPhoneIdentifiers(), ", ;|");
    if (tokens == null) {
      return null;
    }
    int n = 0;
    for (final String token : tokens) {
      if (StringUtils.isNotBlank(token)) {
        n++;
      }
    }
    if (n == 0) {
      return null;
    }
    final String[] result = new String[n];
    n = 0;
    for (final String token : tokens) {
      if (StringUtils.isNotBlank(token)) {
        result[n] = token.trim();
        n++;
      }
    }
    return result;
  }

  public UserDao getUserDao() {
    return userDao;
  }

  public void updateMyAccount(PFUserDO data) {
    userDao.updateMyAccount(data);
  }

  public void undelete(PFUserDO dbUser) {
    userDao.internalUndelete(dbUser);
  }

  public PFUserDO getUserByAuthenticationToken(Integer userId, UserTokenType type, String authKey) {
    return userAuthenticationsDao.getUserByToken(userId, type, authKey);
  }

  public List<PFUserDO> findUserByMail(String email) {
    List<PFUserDO> userList = new ArrayList<>();
    for (PFUserDO user : getUserGroupCache().getAllUsers()) {
      if (user.getEmail() != null && user.getEmail().toLowerCase().equals(email.toLowerCase())) {
        userList.add(user);
      }
    }
    return userList;
  }

  /**
   * Uses an internal cache for faster access. The cache is automatically renewed if the authentication token was changed.
   *
   * @param userId
   * @param type
   * @return The user's authentication token (will be created if not given yet).
   * @see UserTokenCache#getAuthenticationToken
   */
  public String getAuthenticationToken(Integer userId, UserTokenType type) {
    return this.userTokenCache.getAuthenticationToken(userId, type);
  }

  /**
   * Renews the user's authentication token.
   *
   * @param userId
   * @param type
   * @see UserAuthenticationsDao#renewToken(int, UserTokenType)
   */
  public void renewAuthenticationToken(Integer userId, UserTokenType type) {
    userAuthenticationsDao.renewToken(userId, type);
  }
}
