package org.projectforge.business.user.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.projectforge.business.login.PasswordCheckResult;
import org.projectforge.business.user.UserDao;
import org.projectforge.framework.i18n.I18nKeyAndParams;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

public interface UserService
{

  String getUserIds(Collection<PFUserDO> users);

  Collection<PFUserDO> getSortedUsers(String userIds);

  Collection<PFUserDO> getSortedUsers();

  List<String> getUserNames(String userIds);

  List<PFUserDO> getAllUsers();

  String getCachedAuthenticationToken(Integer userId);

  PFUserDO getUser(Integer userId);

  PFUserDO authenticateUser(String userString, String password);

  String decrypt(Integer userId, String encryptedParams);

  String encrypt(String string);

  Collection<Integer> getAssignedTenants(PFUserDO user);

  void renewStayLoggedInKey(Integer id);

  Set<I18nKeyAndParams> getPasswordQualityI18nKeyAndParams();

  Set<I18nKeyAndParams> checkPasswordQuality(String passwordInput);

  void createEncryptedPassword(PFUserDO passwordUser, String passwordInput);

  PFUserDO getByUsername(String username);

  PasswordCheckResult checkPassword(PFUserDO user, String password);

  Set<I18nKeyAndParams> changePassword(PFUserDO user, String oldPassword, String newPassword);

  Set<I18nKeyAndParams> changeWlanPassword(PFUserDO user, String loginPassword, String newWlanPassword);

  Integer save(PFUserDO user);

  void markAsDeleted(PFUserDO user);

  boolean doesUsernameAlreadyExist(PFUserDO user);

  String getAuthenticationToken(int userId);

  PFUserDO getById(Serializable id);

  ModificationStatus update(PFUserDO user);

  String getStayLoggedInKey(Integer id);

  List<PFUserDO> loadAll();

  void onPasswordChange(final PFUserDO user, final boolean createHistoryEntry);

  void onWlanPasswordChange(final PFUserDO user, final boolean createHistoryEntry);

  String getNormalizedPersonalPhoneIdentifiers(PFUserDO data);

  UserDao getUserDao();

  void updateMyAccount(PFUserDO data);

  String[] getPersonalPhoneIdentifiers(PFUserDO user);

  void undelete(PFUserDO dbUser);

  PFUserDO getUserByAuthenticationToken(Integer userId, String authKey);

  List<PFUserDO> findUserByMail(String email);

}
