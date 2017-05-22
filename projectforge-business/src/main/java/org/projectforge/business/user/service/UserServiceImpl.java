package org.projectforge.business.user.service;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.login.Login;
import org.projectforge.business.login.PasswordCheckResult;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.password.PasswordQualityService;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UsersComparator;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.configuration.SecurityConfig;
import org.projectforge.framework.i18n.I18nKeyAndParams;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.history.HistoryBaseDaoAdapter;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.Crypt;
import org.projectforge.framework.utils.NumberHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UserService.class);

  private static final short STAY_LOGGED_IN_KEY_LENGTH = 20;

  private static final String MESSAGE_KEY_OLD_PASSWORD_WRONG = "user.changePassword.error.oldPasswordWrong";

  private static final String MESSAGE_KEY_LOGIN_PASSWORD_WRONG = "user.changeWlanPassword.error.loginPasswordWrong";

  private UserGroupCache userGroupCache;

  @Autowired
  private ConfigurationService configurationService;

  @Autowired
  private UserDao userDao;

  @Autowired
  private AccessChecker accessChecker;

  @Autowired
  private PasswordQualityService passwordQualityService;

  private final UsersComparator usersComparator = new UsersComparator();

  /**
   * @param userIds
   * @return
   */
  @Override
  public List<String> getUserNames(final String userIds)
  {
    if (StringUtils.isEmpty(userIds) == true) {
      return null;
    }
    final int[] ids = StringHelper.splitToInts(userIds, ",", false);
    final List<String> list = new ArrayList<String>();
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

  @Override
  public Collection<PFUserDO> getSortedUsers()
  {
    TreeSet<PFUserDO> sortedUsers = new TreeSet<PFUserDO>(usersComparator);
    final Collection<PFUserDO> allusers = getUserGroupCache().getAllUsers();
    final PFUserDO loggedInUser = ThreadLocalUserContext.getUser();
    for (final PFUserDO user : allusers) {
      if (user.isDeleted() == false && user.isDeactivated() == false
          && userDao.hasSelectAccess(loggedInUser, user, false) == true) {
        sortedUsers.add(user);
      }
    }
    return sortedUsers;
  }

  /**
   * @param userIds
   * @return
   */
  @Override
  public Collection<PFUserDO> getSortedUsers(final String userIds)
  {
    if (StringUtils.isEmpty(userIds) == true) {
      return null;
    }
    TreeSet<PFUserDO> sortedUsers = new TreeSet<PFUserDO>(usersComparator);
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

  @Override
  public String getUserIds(final Collection<PFUserDO> users)
  {
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
  private UserGroupCache getUserGroupCache()
  {
    if (userGroupCache == null) {
      userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
    }
    return userGroupCache;
  }

  @Override
  public List<PFUserDO> getAllUsers()
  {
    try {
      return userDao.internalLoadAll();
    } catch (final Exception ex) {
      log.fatal(
          "******* Exception while getting users from data-base (OK only in case of migration from older versions): "
              + ex.getMessage(),
          ex);
      return new ArrayList<PFUserDO>();
    }
  }

  /**
   * for faster access (due to permanent usage e. g. by subscription of calendars
   *
   * @param userId
   * @return
   */
  @Override
  public String getCachedAuthenticationToken(final Integer userId)
  {
    final PFUserDO user = getUserGroupCache().getUser(userId);
    final String authenticationToken = user.getAuthenticationToken();
    if (StringUtils.isBlank(authenticationToken) == false && authenticationToken.trim().length() >= 10) {
      return authenticationToken;
    }
    return userDao.getAuthenticationToken(userId);
  }

  /**
   * @param userId
   * @param encryptedString
   * @return The decrypted string.
   * @see Crypt#decrypt(String, String)
   */
  @Override
  public String decrypt(final Integer userId, final String encryptedString)
  {
    // final PFUserDO user = userCache.getUser(userId); // for faster access (due to permanent usage e. g. by subscription of calendars
    // (ics).
    final String authenticationToken = StringUtils.rightPad(getCachedAuthenticationToken(userId), 32, "x");
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
  public String encrypt(final Integer userId, final String data)
  {
    final String authenticationToken = StringUtils.rightPad(getCachedAuthenticationToken(userId), 32, "x");
    return Crypt.encrypt(authenticationToken, data);
  }

  /**
   * Uses the context user.
   *
   * @param data
   * @return
   * @see #encrypt(Integer, String)
   */
  @Override
  public String encrypt(final String data)
  {
    return encrypt(ThreadLocalUserContext.getUserId(), data);
  }

  @Override
  public PFUserDO getUser(Integer userId)
  {
    return getUserGroupCache().getUser(userId);
  }

  @Override
  public Collection<Integer> getAssignedTenants(final PFUserDO user)
  {
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
  @Override
  public void createEncryptedPassword(final PFUserDO user, final String password)
  {
    final String saltString = createSaltString();
    user.setPasswordSalt(saltString);
    final String encryptedPassword = Crypt.digest(getPepperString() + saltString + password);
    user.setPassword(encryptedPassword);
  }

  private String createSaltString()
  {
    return NumberHelper.getSecureRandomBase64String(10);
  }

  @Override
  public Set<I18nKeyAndParams> getPasswordQualityI18nKeyAndParams()
  {
    return passwordQualityService.getPasswordQualityI18nKeyAndParams();
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
  @Override
  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  public Set<I18nKeyAndParams> changePassword(PFUserDO user, final String oldPassword, final String newPassword)
  {
    Validate.notNull(user);
    Validate.notNull(oldPassword);
    Validate.notNull(newPassword);
    final Set<I18nKeyAndParams> errorMsgKeys = checkPasswordQuality(newPassword);
    if (errorMsgKeys != null) {
      return errorMsgKeys;
    }

    accessChecker.checkRestrictedOrDemoUser();
    user = getUser(user.getUsername(), oldPassword, false);
    if (user == null) {
      I18nKeyAndParams compareErrorMsgKey = new I18nKeyAndParams(MESSAGE_KEY_OLD_PASSWORD_WRONG);
      Set<I18nKeyAndParams> compareErrorMsgKeys = new HashSet<>();
      compareErrorMsgKeys.add(compareErrorMsgKey);
      return compareErrorMsgKeys;

    }
    createEncryptedPassword(user, newPassword);
    onPasswordChange(user, true);
    Login.getInstance().passwordChanged(user, newPassword);
    log.info("Password changed and stay-logged-key renewed for user: " + user.getId() + " - " + user.getUsername());
    return null;
  }

  /**
   * Changes the user's WLAN password. Checks the password quality and the correct authentication for the login password before.
   *
   * @param user
   * @param loginPassword
   * @param newWlanPassword
   * @return Error message key if any check failed or null, if successfully changed.
   */
  @Override
  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  public Set<I18nKeyAndParams> changeWlanPassword(PFUserDO user, final String loginPassword, final String newWlanPassword)
  {
    Validate.notNull(user);
    Validate.notNull(loginPassword);
    Validate.notNull(newWlanPassword);

    final Set<I18nKeyAndParams> errorMsgKeys = checkPasswordQuality(newWlanPassword);
    if (errorMsgKeys != null) {
      return errorMsgKeys;
    }

    accessChecker.checkRestrictedOrDemoUser();
    user = getUser(user.getUsername(), loginPassword, false); // get user from DB to persist the change of the wlan password time
    if (user == null) {
      Set<I18nKeyAndParams> compareErrorMsgKeys = new HashSet<>();
      compareErrorMsgKeys.add(new I18nKeyAndParams(MESSAGE_KEY_LOGIN_PASSWORD_WRONG));
      return compareErrorMsgKeys;
    }

    onWlanPasswordChange(user, true); // set last change time and creaty history entry
    Login.getInstance().wlanPasswordChanged(user, newWlanPassword); // change the wlan password
    log.info("WLAN Password changed for user: " + user.getId() + " - " + user.getUsername());
    return null;
  }

  /**
   * Checks the password quality of a new password. Password must have at least n characters and at minimum one letter
   * and one non-letter character.
   *
   * @param newPassword
   * @return null if password quality is OK, otherwise the i18n message key of the password check failure.
   */
  @Override
  public Set<I18nKeyAndParams> checkPasswordQuality(final String newPassword)
  {
    return passwordQualityService.getPasswordQualityI18nKeyAndParams();
  }

  @Override
  public void onPasswordChange(final PFUserDO user, final boolean createHistoryEntry)
  {
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

  @Override
  public void onWlanPasswordChange(final PFUserDO user, final boolean createHistoryEntry)
  {
    if (createHistoryEntry) {
      HistoryBaseDaoAdapter.wrappHistoryUpdate(user, () -> {
        user.setLastWlanPasswordChange(new Date());
        return ModificationStatus.MAJOR;
      });
    } else {
      user.setLastWlanPasswordChange(new Date());
    }
  }

  private String createStayLoggedInKey()
  {
    return NumberHelper.getSecureRandomUrlSaveString(STAY_LOGGED_IN_KEY_LENGTH);
  }

  @SuppressWarnings("unchecked")
  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  protected PFUserDO getUser(final String username, final String password, final boolean updateSaltAndPepperIfNeeded)
  {
    final List<PFUserDO> list = userDao.findByUsername(username);
    if (list == null || list.isEmpty() == true || list.get(0) == null) {
      return null;
    }
    final PFUserDO user = list.get(0);
    final PasswordCheckResult passwordCheckResult = checkPassword(user, password);
    if (passwordCheckResult.isOK() == false) {
      return null;
    }
    if (updateSaltAndPepperIfNeeded == true && passwordCheckResult.isPasswordUpdateNeeded() == true) {
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
  @Override
  public PasswordCheckResult checkPassword(final PFUserDO user, final String password)
  {
    if (user == null) {
      log.warn("User not given in checkPassword(PFUserDO, String) method.");
      return PasswordCheckResult.FAILED;
    }
    final String userPassword = user.getPassword();
    if (StringUtils.isBlank(userPassword) == true) {
      log.warn("User's password is blank, can't checkPassword(PFUserDO, String) for user with id " + user.getId());
      return PasswordCheckResult.FAILED;
    }
    String saltString = user.getPasswordSalt();
    if (saltString == null) {
      saltString = "";
    }
    final String pepperString = getPepperString();
    String encryptedPassword = Crypt.digest(pepperString + saltString + password);
    if (userPassword.equals(encryptedPassword) == true) {
      // Passwords match!
      if (StringUtils.isEmpty(saltString) == true) {
        log.info("Password of user " + user.getId() + " with username '" + user.getUsername() + "' is not yet salted!");
        return PasswordCheckResult.OK_WITHOUT_SALT;
      }
      return PasswordCheckResult.OK;
    }
    if (StringUtils.isNotBlank(pepperString) == true) {
      // Check password without pepper:
      encryptedPassword = Crypt.digest(saltString + password);
      if (userPassword.equals(encryptedPassword) == true) {
        // Passwords match!
        if (StringUtils.isEmpty(saltString) == true) {
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
  @Override
  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  public String getStayLoggedInKey(final Integer userId)
  {
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
  @Override
  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  public void renewStayLoggedInKey(final Integer userId)
  {
    if (ThreadLocalUserContext.getUserId().equals(userId) == false) {
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
  @Override
  public PFUserDO authenticateUser(final String username, final String password)
  {
    Validate.notNull(username);
    Validate.notNull(password);

    PFUserDO user = getUser(username, password, true);
    if (user != null) {

      final int loginFailures = user.getLoginFailures();
      final Timestamp lastLogin = user.getLastLogin();
      userDao.updateUserAfterLoginSuccess(user);
      if (user.hasSystemAccess() == false) {
        log.warn("Deleted/deactivated user tried to login: " + user);
        return null;
      }
      final PFUserDO contextUser = new PFUserDO();
      contextUser.copyValuesFrom(user);
      contextUser.setLoginFailures(loginFailures); // Restore loginFailures for current user session.
      contextUser.setLastLogin(lastLogin); // Restore lastLogin for current user session.
      contextUser.setPassword(null);
      return contextUser;
    }
    userDao.updateIncrementLoginFailure(username);
    return null;
  }

  private String getPepperString()
  {
    final SecurityConfig securityConfig = configurationService.getSecurityConfig();
    if (securityConfig != null) {
      return securityConfig.getPasswordPepper();
    }
    return "";
  }

  @Override
  public PFUserDO getByUsername(String username)
  {
    return userDao.getInternalByName(username);
  }

  @Override
  public PFUserDO getById(Serializable id)
  {
    return userDao.internalGetById(id);
  }

  @Override
  public Integer save(PFUserDO user)
  {
    return userDao.internalSave(user);
  }

  @Override
  public void markAsDeleted(PFUserDO user)
  {
    userDao.internalMarkAsDeleted(user);
  }

  @Override
  public boolean doesUsernameAlreadyExist(PFUserDO user)
  {
    return userDao.doesUsernameAlreadyExist(user);
  }

  @Override
  public String getAuthenticationToken(int userId)
  {
    return userDao.getAuthenticationToken(userId);
  }

  @Override
  public ModificationStatus update(PFUserDO user)
  {
    return userDao.update(user);
  }

  @Override
  public List<PFUserDO> loadAll()
  {
    return userDao.internalLoadAll();
  }

  @Override
  public String getNormalizedPersonalPhoneIdentifiers(final PFUserDO user)
  {
    if (StringUtils.isNotBlank(user.getPersonalPhoneIdentifiers()) == true) {
      final String[] ids = getPersonalPhoneIdentifiers(user);
      if (ids != null) {
        final StringBuffer buf = new StringBuffer();
        boolean first = true;
        for (final String id : ids) {
          if (first == true) {
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

  @Override
  public String[] getPersonalPhoneIdentifiers(final PFUserDO user)
  {
    final String[] tokens = StringUtils.split(user.getPersonalPhoneIdentifiers(), ", ;|");
    if (tokens == null) {
      return null;
    }
    int n = 0;
    for (final String token : tokens) {
      if (StringUtils.isNotBlank(token) == true) {
        n++;
      }
    }
    if (n == 0) {
      return null;
    }
    final String[] result = new String[n];
    n = 0;
    for (final String token : tokens) {
      if (StringUtils.isNotBlank(token) == true) {
        result[n] = token.trim();
        n++;
      }
    }
    return result;
  }

  @Override
  public UserDao getUserDao()
  {
    return userDao;
  }

  @Override
  public void updateMyAccount(PFUserDO data)
  {
    userDao.updateMyAccount(data);
  }

  @Override
  public void undelete(PFUserDO dbUser)
  {
    userDao.internalUndelete(dbUser);
  }

  @Override
  public PFUserDO getUserByAuthenticationToken(Integer userId, String authKey)
  {
    return userDao.getUserByAuthenticationToken(userId, authKey);
  }

  @Override
  public List<PFUserDO> findUserByMail(String email)
  {
    List<PFUserDO> userList = new ArrayList<>();
    for (PFUserDO user : getUserGroupCache().getAllUsers()) {
      if (user.getEmail() != null && user.getEmail().toLowerCase().equals(email.toLowerCase())) {
        userList.add(user);
      }
    }
    return userList;
  }

}
