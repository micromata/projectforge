package org.projectforge.business.password;

import org.apache.commons.lang.StringUtils;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.framework.i18n.I18nKeyAndParams;
import org.projectforge.framework.i18n.I18nKeysAndParamsSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Class responsible to check quality of a password referencing stored configuration.
 *
 * @author Matthias Altmann (m.altmann@micromata.de)
 */
@Service
public class PasswordQualityServiceImpl implements PasswordQualityService
{

  /**
   * Constant MESSAGE_KEY_PASSWORD_QUALITY_ERROR.
   */
  private static final String MESSAGE_KEY_PASSWORD_QUALITY_ERROR = "user.changePassword.error.passwordQualityCheck";

  /**
   * Constant MESSAGE_KEY_PASSWORD_MIN_LENGTH_ERROR.
   */
  private static final String MESSAGE_KEY_PASSWORD_MIN_LENGTH_ERROR = "user.changePassword.error.notMinLength";

  /**
   * Constant MESSAGE_KEY_PASSWORD_CHARACTER_ERROR.
   */
  private static final String MESSAGE_KEY_PASSWORD_CHARACTER_ERROR = "user.changePassword.error.noCharacter";

  /**
   * Constant MESSAGE_KEY_PASSWORD_NONCHAR_ERROR.
   */
  private static final String MESSAGE_KEY_PASSWORD_NONCHAR_ERROR = "user.changePassword.error.noNonCharacter";

  /**
   * Constant MESSAGE_KEY_PASSWORD_OLD_EQ_NEW_ERROR.
   */
  private static final String MESSAGE_KEY_PASSWORD_OLD_EQ_NEW_ERROR = "user.changePassword.error.oldPasswdEqualsNew";

  /**
   * Configuration service projectforge.
   */
  @Autowired
  private ConfigurationService configurationService;

  /**
   * Set holding i18n keys for constraints not fulfilled by password to check inside this class.
   */
  private I18nKeysAndParamsSet i18nKeyAndParamsSet = new I18nKeysAndParamsSet();

  @Override
  public I18nKeysAndParamsSet getPasswordQualityI18nKeyAndParams()
  {
    i18nKeyAndParamsSet.add(new I18nKeyAndParams(MESSAGE_KEY_PASSWORD_QUALITY_ERROR, configurationService.getMinPasswordLength()));
    return i18nKeyAndParamsSet;

  }

  /**
   * Add an I18n Error Key.
   *
   * @param i18nKeyAndParams the 18 n key and params
   */
  private void addErrorI18nKey(final I18nKeyAndParams i18nKeyAndParams)
  {
    i18nKeyAndParamsSet.add(i18nKeyAndParams);
  }

  /**
   * Checks the password quality of a new password change is required. Password must have at least n characters and at minimum one letter
   * and one non-letter character.
   *
   * @param newPassword
   * @return null if password quality is OK, otherwise the i18n message key of the password check failure.
   */
  @Override
  public I18nKeysAndParamsSet checkPasswordQualityOnChange(final String oldPassword, final String newPassword)

  {
    i18nKeyAndParamsSet = checkPasswordQuality(newPassword);
    if (i18nKeyAndParamsSet == null) {
      i18nKeyAndParamsSet = new I18nKeysAndParamsSet();
    }
    if (StringUtils.equals(oldPassword, newPassword) == true) {
      addErrorI18nKey(new I18nKeyAndParams(MESSAGE_KEY_PASSWORD_OLD_EQ_NEW_ERROR));
    }
    if (i18nKeyAndParamsSet.isEmpty() == true)
      return null;
    else
      return i18nKeyAndParamsSet;
  }

  /**
   * Checks the password quality of a new password. Password must have at least n characters and at minimum one letter
   * and one non-letter character.
   *
   * @return null if password quality is OK, otherwise the i18n message key of the password check failure.
   */
  @Override
  public I18nKeysAndParamsSet checkPasswordQuality(final String password)
  {
    i18nKeyAndParamsSet = new I18nKeysAndParamsSet();
    final int minPasswordLength = configurationService.getMinPasswordLength();
    if (password == null || password.length() < minPasswordLength) {
      addErrorI18nKey(new I18nKeyAndParams(MESSAGE_KEY_PASSWORD_MIN_LENGTH_ERROR, configurationService.getMinPasswordLength()));
      if (password == null) {
        return i18nKeyAndParamsSet;
      }
    }

    boolean letter = false;
    boolean nonLetter = false;
    for (int i = 0; i < password.length(); i++) {
      final char ch = password.charAt(i);
      if (letter == false && Character.isLetter(ch) == true) {
        letter = true;
      } else if (nonLetter == false && Character.isLetter(ch) == false) {
        nonLetter = true;
      }
    }
    if (letter == false) {
      addErrorI18nKey(new I18nKeyAndParams(MESSAGE_KEY_PASSWORD_CHARACTER_ERROR));
    }
    if (nonLetter == false) {
      addErrorI18nKey(new I18nKeyAndParams(MESSAGE_KEY_PASSWORD_NONCHAR_ERROR));
    }

    if (i18nKeyAndParamsSet.isEmpty() == true)
      return null;
    else
      return i18nKeyAndParamsSet;
  }

}
