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

package org.projectforge.business.user.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.login.Login;
import org.projectforge.business.login.PasswordCheckResult;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.multitenancy.TenantService;
import org.projectforge.business.password.PasswordQualityService;
import org.projectforge.business.user.UserChangedListener;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UsersComparator;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.OperationType;
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
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService implements UserChangedListener {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserService.class);

  private static final short STAY_LOGGED_IN_KEY_LENGTH = 20;

  private static final String MESSAGE_KEY_OLD_PASSWORD_WRONG = "user.changePassword.error.oldPasswordWrong";

  private static final String MESSAGE_KEY_LOGIN_PASSWORD_WRONG = "user.changeWlanPassword.error.loginPasswordWrong";
  private final UsersComparator usersComparator = new UsersComparator();
  private UserGroupCache userGroupCache;
  private Map<Integer, String> authenticationTokenCache = new HashMap<>();
  private ConfigurationService configurationService;
  private UserDao userDao;
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
                     UserDao userDao) {
    this.accessChecker = accessChecker;
    this.configurationService = configurationService;
    this.passwordQualityService = passwordQualityService;
    this.tenantService = tenantService;
    this.userDao = userDao;
    userDao.register(this);
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
   * @param authenticationToken
   * @param userIdAttribute     The required http attribute (only for logging purposes)
   * @param tokenAttribute      The required http attribute (only for logging purposes)
   * @return
   */
  public boolean checkAuthenticationToken(final Integer userId, String authenticationToken, String userIdAttribute, String tokenAttribute) {
    String storedAuthenticationToken = getAuthenticationToken(userId);
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
   * @param userId
   * @param encryptedString
   * @return The decrypted string.
   * @see Crypt#decrypt(String, String)
   */
  public String decrypt(final Integer userId, final String encryptedString) {
    String storedAuthenticationToken = getAuthenticationToken(userId);
    if (storedAuthenticationToken == null) {
      log.warn("Can't get authentication token for user " + userId + ". So can't decrypt encrypted string.");
      return "";
    }
    final String authenticationToken = StringUtils.rightPad(storedAuthenticationToken, 32, "x");
    return Crypt.decrypt(authenticationToken, encryptedString);
  }

  /**
   * Encrypts the given str with AES. The key is the current authenticationToken of the given user (by id) (first 16
   * bytes of it).
   *
   * @param userId
   * @param data
   * @return The base64 encoded result (url safe).
   * @see Crypt#encrypt(String, String)
   */
  public String encrypt(final Integer userId, final String data) {
    String storedAuthenticationToken = getAuthenticationToken(userId);
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
   * @param data
   * @return
   * @see #encrypt(Integer, String)
   */
  public String encrypt(final String data) {
    return encrypt(ThreadLocalUserContext.getUserId(), data);
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
    log.info("Password changed and stay-logged-key renewed for user: " + user.getId() + " - " + user.getUsername());
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
      user.setStayLoggedInKey(createStayLoggedInKey());
      if (createHistoryEntry) {
        HistoryBaseDaoAdapter.wrappHistoryUpdate(user, () -> {
          user.setLastPasswordChange(new Date());
          return ModificationStatus.MAJOR;
        });
      } else {
        user.setLastPasswordChange(new Date());
      }
    } else {
      throw new IllegalArgumentException(
              "Given password seems to be not encrypted! Aborting due to security reasons (for avoiding storage of clear password in the database).");
    }
  }

  public void onWlanPasswordChange(final PFUserDO user, final boolean createHistoryEntry) {
    if (createHistoryEntry) {
      HistoryBaseDaoAdapter.wrappHistoryUpdate(user, () -> {
        user.setLastWlanPasswordChange(new Date());
        return ModificationStatus.MAJOR;
      });
    } else {
      user.setLastWlanPasswordChange(new Date());
    }
  }

  private String createStayLoggedInKey() {
    return NumberHelper.getSecureRandomUrlSaveString(STAY_LOGGED_IN_KEY_LENGTH);
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
   * Returns the user's stay-logged-in key if exists (must be not blank with a size >= 10). If not, a new stay-logged-in
   * key will be generated.
   *
   * @param userId
   * @return
   */
  public String getStayLoggedInKey(final Integer userId) {
    final PFUserDO user = userDao.internalGetById(userId);
    if (StringUtils.isBlank(user.getStayLoggedInKey()) || user.getStayLoggedInKey().trim().length() < 10) {
      user.setStayLoggedInKey(createStayLoggedInKey());
      log.info("Stay-logged-key renewed for user: " + userId + " - " + user.getUsername());
    }
    return user.getStayLoggedInKey();
  }

  /**
   * Renews the user's stay-logged-in key (random string sequence).
   */
  public void renewStayLoggedInKey(final Integer userId) {
    if (!ThreadLocalUserContext.getUserId().equals(userId)) {
      // Only admin users are able to renew authentication token of other users:
      accessChecker.checkIsLoggedInUserMemberOfAdminGroup();
    }
    accessChecker.checkRestrictedOrDemoUser(); // Demo users are also not allowed to do this.
    final PFUserDO user = userDao.internalGetById(userId);
    user.setStayLoggedInKey(createStayLoggedInKey());
    log.info("Stay-logged-key renewed for user: " + userId + " - " + user.getUsername());
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
      final Timestamp lastLogin = user.getLastLogin();
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

  public PFUserDO getByUsername(String username) {
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

  public PFUserDO getUserByAuthenticationToken(Integer userId, String authKey) {
    return userDao.getUserByAuthenticationToken(userId, authKey);
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
   * @return The user's authentication token (will be created if not given yet).
   * @see UserDao#getAuthenticationToken(Integer)
   */
  public String getAuthenticationToken(Integer userId) {
    String authenticationToken = this.authenticationTokenCache.get(userId);
    if (authenticationToken == null) {
      authenticationToken = userDao.getAuthenticationToken(userId);
      this.authenticationTokenCache.put(userId, authenticationToken);
    }
    return authenticationToken;
  }

  /**
   * Clears authentication token.
   *
   * @param user
   * @param operationType
   */
  @Override
  public void afterUserChanged(PFUserDO user, OperationType operationType) {
    this.authenticationTokenCache.remove(user.getId());
  }
}
