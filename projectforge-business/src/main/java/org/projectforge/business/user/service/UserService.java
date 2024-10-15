/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.login.Login;
import org.projectforge.business.login.LoginHandler;
import org.projectforge.business.login.PasswordCheckResult;
import org.projectforge.business.password.PasswordQualityService;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UserPasswordDao;
import org.projectforge.business.user.UsersComparator;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.configuration.SecurityConfig;
import org.projectforge.framework.i18n.I18nKeyAndParams;
import org.projectforge.framework.persistence.api.EntityCopyStatus;
import org.projectforge.framework.persistence.jpa.PfPersistenceService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
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
    private ConfigurationService configurationService;
    private UserDao userDao;
    private UserPasswordDao userPasswordDao;
    private AccessChecker accessChecker;
    private PasswordQualityService passwordQualityService;
    private PfPersistenceService persistenceService;

    /**
     * Needed by Wicket for proxying.
     */
    public UserService() {
    }

    @Autowired
    public UserService(AccessChecker accessChecker, ConfigurationService configurationService, PasswordQualityService passwordQualityService, UserDao userDao, UserPasswordDao userPasswordDao, UserGroupCache userGroupCache, PfPersistenceService persistenceService) {
        this.accessChecker = accessChecker;
        this.configurationService = configurationService;
        this.passwordQualityService = passwordQualityService;
        this.userDao = userDao;
        this.userPasswordDao = userPasswordDao;
        this.userGroupCache = userGroupCache;
        this.persistenceService = persistenceService;
    }

    /**
     * @param userIds
     */
    public List<String> getUserNames(final String userIds) {
        if (StringUtils.isEmpty(userIds)) {
            return null;
        }
        final long[] ids = StringHelper.splitToLongs(userIds, ",", false);
        final List<String> list = new ArrayList<>();
        for (final long id : ids) {
            final PFUserDO user = userGroupCache.getUser(id);
            if (user != null) {
                list.add(user.getFullname());
            } else {
                log.warn("User with id '" + id + "' not found in UserGroupCache. userIds string was: " + userIds);
            }
        }
        return list;
    }

    public String getUserMails(final Collection<PFUserDO> users) {
        if (CollectionUtils.isEmpty(users)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (PFUserDO user : users) {
            String mail = user.getEmail();
            if (StringUtils.isNotBlank(mail)) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(mail);
            }
        }
        return sb.toString();
    }

    /**
     * @return Sorted list of not deleted and not deactivated users.
     */
    public Collection<PFUserDO> getSortedUsers() {
        TreeSet<PFUserDO> sortedUsers = new TreeSet<>(usersComparator);
        final Collection<PFUserDO> allusers = userGroupCache.getAllUsers();
        final PFUserDO loggedInUser = ThreadLocalUserContext.getRequiredLoggedInUser();
        for (final PFUserDO user : allusers) {
            if (!user.getDeleted() && !user.getDeactivated() && userDao.hasUserSelectAccess(loggedInUser, user, false)) {
                sortedUsers.add(user);
            }
        }
        return sortedUsers;
    }

    public Collection<PFUserDO> getSortedUsers(final String userIds) {
        if (StringUtils.isEmpty(userIds)) {
            return null;
        }
        TreeSet<PFUserDO> sortedUsers = new TreeSet<>(usersComparator);
        final long[] ids = StringHelper.splitToLongs(userIds, ",", false);
        for (final long id : ids) {
            final PFUserDO user = userGroupCache.getUser(id);
            if (user != null) {
                sortedUsers.add(user);
            } else {
                log.warn("Group with id '" + id + "' not found in UserGroupCache. groupIds string was: " + userIds);
            }
        }
        return sortedUsers;
    }

    public String getUserIds(final Collection<PFUserDO> users) {
        final StringBuilder buf = new StringBuilder();
        boolean first = true;
        for (final PFUserDO user : users) {
            if (user.getId() != null) {
                first = StringHelper.append(buf, first, String.valueOf(user.getId()), ",");
            }
        }
        return buf.toString();
    }

    public List<PFUserDO> getAllUsers() {
        try {
            return userDao.loadAll(false);
        } catch (final Exception ex) {
            log.error("******* Exception while getting users from data-base (OK only in case of migration from older versions): " + ex.getMessage(), ex);
            return new ArrayList<>();
        }
    }

    public List<PFUserDO> getAllActiveUsers() {
        return getAllUsers().stream().filter(u -> !u.getDeactivated() && !u.getDeleted()).collect(Collectors.toList());
    }

    /**
     * @return The user from UserGroupCache.
     */
    public PFUserDO getUser(Long userId) {
        return userGroupCache.getUser(userId);
    }

    /**
     * Encrypts the password with a new generated salt string and the pepper string if configured any.
     *
     * @param user              The user to user.
     * @param clearTextPassword as clear text.
     * @see UserPasswordDao#encryptAndSavePassword(long, char[])
     */
    public void encryptAndSavePassword(final PFUserDO user, final char[] clearTextPassword) {
        encryptAndSavePassword(user, clearTextPassword, true);
    }

    /**
     * Encrypts the password with a new generated salt string and the pepper string if configured any.
     *
     * @param user              The user to user.
     * @param clearTextPassword as clear text.
     */
    public void encryptAndSavePassword(final PFUserDO user, final char[] clearTextPassword, final boolean checkAccess) {
        userPasswordDao.encryptAndSavePassword(user.getId(), clearTextPassword, checkAccess);
    }

    /**
     * Changes the user's password. Checks the password quality and the correct authentication for the old password
     * before. Also the stay-logged-in-key will be renewed, so any existing stay-logged-in cookie will be invalid.
     *
     * @param userId
     * @param oldPassword Will be cleared at the end of this method due to security reasons.
     * @param newPassword Will be cleared at the end of this method due to security reasons.
     * @return Error message key if any check failed or null, if successfully changed.
     */
    public List<I18nKeyAndParams> changePassword(final Long userId, final char[] oldPassword, final char[] newPassword) {
        try {
            return persistenceService.runInTransaction(context -> {
                Validate.notNull(userId);
                Validate.isTrue(oldPassword.length > 0);
                Validate.isTrue(Objects.equals(userId, ThreadLocalUserContext.getLoggedInUserId()), "User is only allowed to change his own password-");
                final PFUserDO user = userDao.getById(userId, false);
                final PFUserDO userCheck = getUser(user.getUsername(), oldPassword, false);
                if (userCheck == null) {
                    return Collections.singletonList(new I18nKeyAndParams(MESSAGE_KEY_OLD_PASSWORD_WRONG));
                }
                return doPasswordChange(user, oldPassword, newPassword);
            });
        } finally {
            LoginHandler.clearPassword(newPassword);
            LoginHandler.clearPassword(oldPassword);
        }
    }

    /**
     * Changes the user's password. Checks the password quality.
     * Also the stay-logged-in-key will be renewed, so any existing stay-logged-in cookie will be invalid.
     *
     * @param userId
     * @param newPassword Will be cleared at the end of this method due to security reasons.
     * @return Error message key if any check failed or null, if successfully changed.
     */
    public List<I18nKeyAndParams> changePasswordByAdmin(final Long userId, final char[] newPassword) {
        try {
            return persistenceService.runInTransaction(context -> {
                Validate.notNull(userId);
                Validate.isTrue(!Objects.equals(userId, ThreadLocalUserContext.getLoggedInUserId()), "Admin user is not allowed to change his own password without entering his login password-");
                accessChecker.checkIsLoggedInUserMemberOfAdminGroup();
                final PFUserDO user = userDao.getById(userId, false);
                return doPasswordChange(user, null, newPassword);
            });
        } finally {
            LoginHandler.clearPassword(newPassword);
        }
    }

    private List<I18nKeyAndParams> doPasswordChange(final PFUserDO user, final char[] oldPassword, final char[] newPassword) {
        Validate.notNull(user);
        Validate.isTrue(newPassword.length > 0);
        final List<I18nKeyAndParams> errorMsgKeys = passwordQualityService.checkPasswordQuality(oldPassword, newPassword);
        if (!errorMsgKeys.isEmpty()) {
            return errorMsgKeys;
        }
        encryptAndSavePassword(user, newPassword);
        onPasswordChange(user);
        userDao.update(user, false);
        Login.getInstance().passwordChanged(user, newPassword);
        log.info("Password changed for user: " + user.getId() + " - " + user.getUsername());
        return Collections.emptyList();
    }

    /**
     * Should be called only by password reset (authentication via 2FA.
     * Changes the user's password. Checks the password quality before.
     * Also the stay-logged-in-key will be renewed, so any existing stay-logged-in cookie will be invalid.
     *
     * @param userId
     * @param newPassword Will be cleared at the end of this method due to security reasons.
     * @return Error message key if any check failed or null, if successfully changed.
     */
    public List<I18nKeyAndParams> internalChangePasswordAfterPasswordReset(final Long userId, final char[] newPassword) {
        try {
            Validate.notNull(userId);
            Validate.isTrue(newPassword.length > 0);
            Validate.isTrue(ThreadLocalUserContext.getLoggedInUser() == null, "ThreadLocalUser mustn't be given on password reset.");

            final List<I18nKeyAndParams> errorMsgKeys = passwordQualityService.checkPasswordQuality(newPassword);
            if (!errorMsgKeys.isEmpty()) {
                return errorMsgKeys;
            }
            persistenceService.runInTransaction(context -> {
                final PFUserDO user = userDao.getById(userId, false);
                ThreadLocalUserContext.setUser(user);
                encryptAndSavePassword(user, newPassword);
                onPasswordChange(user);
                userDao.update(user, false);
                Login.getInstance().passwordChanged(user, newPassword);
                log.info("Password changed for user: " + user.getId() + " - " + user.getUsername());
                return null;
            });
            return Collections.emptyList();
        } finally {
            ThreadLocalUserContext.setUserContext(null);
            LoginHandler.clearPassword(newPassword);
        }
    }

    /**
     * Changes the user's WLAN password. Checks the password quality and the correct authentication for the login password before.
     *
     * @param userId
     * @param loginPassword   Will be cleared at the end of this method due to security reasons
     * @param newWlanPassword Will be cleared at the end of this method due to security reasons
     * @return Error message key if any check failed or null, if successfully changed.
     */
    public List<I18nKeyAndParams> changeWlanPassword(final Long userId, final char[] loginPassword, final char[] newWlanPassword) {
        try {
            Validate.notNull(userId);
            Validate.isTrue(loginPassword.length > 0);
            Validate.isTrue(Objects.equals(userId, ThreadLocalUserContext.getLoggedInUserId()), "User is only allowed to change his own Wlan/Samba password-");
            final PFUserDO user = userDao.getById(userId, false);
            Validate.notNull(user);
            final PFUserDO userCheck = getUser(user.getUsername(), loginPassword, false); // get user from DB to persist the change of the wlan password time
            if (userCheck == null) {
                return Collections.singletonList(new I18nKeyAndParams(MESSAGE_KEY_LOGIN_PASSWORD_WRONG));
            }
            return doWlanPasswordChange(user, newWlanPassword);
        } finally {
            LoginHandler.clearPassword(loginPassword);
            LoginHandler.clearPassword(newWlanPassword);
        }
    }

    /**
     * Changes the user's password. Checks the password quality.
     * Also the stay-logged-in-key will be renewed, so any existing stay-logged-in cookie will be invalid.
     *
     * @param userId
     * @param newWlanPassword Will be cleared at the end of this method due to security reasons.
     * @return Error message key if any check failed or null, if successfully changed.
     */
    public List<I18nKeyAndParams> changeWlanPasswordByAdmin(final Long userId, final char[] newWlanPassword) {
        try {
            Validate.notNull(userId);
            Validate.isTrue(!Objects.equals(userId, ThreadLocalUserContext.getLoggedInUserId()), "Admin user is not allowed to change his own password without entering his login password-");
            accessChecker.checkIsLoggedInUserMemberOfAdminGroup();
            final PFUserDO user = userDao.getById(userId, false);
            return doWlanPasswordChange(user, newWlanPassword);
        } finally {
            LoginHandler.clearPassword(newWlanPassword);
        }
    }

    private List<I18nKeyAndParams> doWlanPasswordChange(final PFUserDO user, final char[] newWlanPassword) {
        Login.getInstance().wlanPasswordChanged(user, newWlanPassword); // change the wlan password
        user.setLastWlanPasswordChange(new Date());
        userDao.update(user, false);
        log.info("WLAN Password changed for user: " + user.getId() + " - " + user.getUsername());
        return Collections.emptyList();
    }


    public void onPasswordChange(final PFUserDO user) {
        userPasswordDao.onPasswordChange(user);
    }

    /**
     * @param username
     * @param password
     * @param updateSaltAndPepperIfNeeded
     * @return
     */
    @SuppressWarnings("unchecked")
    protected PFUserDO getUser(final String username, final char[] password, final boolean updateSaltAndPepperIfNeeded) {
        final List<PFUserDO> list = userDao.findByUsername(username);
        if (list == null || list.isEmpty() || list.get(0) == null) {
            return null;
        }
        final PFUserDO user = list.get(0);
        final PasswordCheckResult passwordCheckResult = userPasswordDao.checkPassword(user, password);
        if (!passwordCheckResult.isOK()) {
            return null;
        }
        if (updateSaltAndPepperIfNeeded && passwordCheckResult.isPasswordUpdateNeeded()) {
            log.info("Giving salt and/or pepper to the password of the user " + user.getId() + ".");
            encryptAndSavePassword(user, password, false);
            userDao.update(user, false);
        }
        return user;
    }

    /**
     * Ohne Zugangsbegrenzung. Wird bei Anmeldung benötigt.
     *
     * @param username
     * @param password
     */
    public PFUserDO authenticateUser(final String username, final char[] password) {
        Validate.notNull(username);
        Validate.isTrue(password.length > 0);

        PFUserDO user = getUser(username, password, true);
        if (user != null) {

            final int loginFailures = user.getLoginFailures();
            final Date lastLogin = user.getLastLogin();
            userDao.updateUserAfterLoginSuccess(user);
            if (!user.hasSystemAccess()) {
                log.warn("Deleted/deactivated user tried to login: " + user);
                return null;
            }
            user.setLoginFailures(loginFailures); // Restore loginFailures for current user session.
            user.setLastLogin(lastLogin); // Restore lastLogin for current user session.
            return user;
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
        return userDao.getById(id, false);
    }

    public Long save(PFUserDO user) {
        return userDao.save(user, false);
    }

    public void markAsDeleted(PFUserDO user, boolean checkAccess) {
        userDao.markAsDeleted(user, checkAccess);
    }

    public boolean doesUsernameAlreadyExist(PFUserDO user) {
        return userDao.doesUsernameAlreadyExist(user);
    }

    public EntityCopyStatus update(PFUserDO user) {
        return userDao.update(user);
    }

    /**
     * Without access checking!!! Secret fields are cleared.
     *
     * @see UserDao#loadAll()
     */
    public List<PFUserDO> loadAll(boolean checkAccess) {
        return userDao.loadAll(checkAccess);
    }

    public UserDao getUserDao() {
        return userDao;
    }

    public void updateMyAccount(PFUserDO data) {
        userDao.updateMyAccount(data);
    }

    public void undelete(PFUserDO dbUser) {
        userDao.undelete(dbUser);
    }

    public List<PFUserDO> findUserByMail(String email) {
        List<PFUserDO> userList = new ArrayList<>();
        for (PFUserDO user : userGroupCache.getAllUsers()) {
            if (user.getEmail() != null && user.getEmail().toLowerCase().equals(email.toLowerCase())) {
                userList.add(user);
            }
        }
        return userList;
    }

    /**
     * Encrypts the given data with the user's password hash. If the user changes his password, decryption isn't possible
     * anymore.
     *
     * @param data The data to encrypt.
     * @return The encrypted data.
     * @see UserDao#encrypt(String)
     */
    public String encrypt(String data) {
        return userDao.encrypt(data);
    }

    /**
     * Decrypts the given data with the user's password hash. If the user changes his password, decryption isn't possible
     * anymore.
     *
     * @param encrypted The data to encrypt.
     * @return The decrypted data.
     * @see UserDao#decrypt(String)
     */
    public String decrypt(String encrypted) {
        return userDao.decrypt(encrypted);
    }

    /**
     * Decrypts the given data with the user's password hash. If the user changes his password, decryption isn't possible
     * anymore.
     *
     * @param encrypted The data to encrypt.
     * @param userId    Use the password of the given user (used by CookieService, because user isn't yet logged-in).
     * @return The decrypted data.
     * @see UserDao#decrypt(String)
     */
    public String decrypt(String encrypted, Long userId) {
        return userDao.decrypt(encrypted, userId);
    }
}
