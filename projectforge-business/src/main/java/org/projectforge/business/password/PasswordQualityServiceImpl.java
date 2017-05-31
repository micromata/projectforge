package org.projectforge.business.password;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.framework.i18n.I18nKeyAndParams;
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

  @Override
  public I18nKeyAndParams getPasswordQualityI18nKeyAndParams()
  {
    return new I18nKeyAndParams(MESSAGE_KEY_PASSWORD_QUALITY_ERROR, configurationService.getMinPasswordLength());
  }

  /**
   * Checks the password quality of a new password. Password must have at least n characters and at minimum one letter
   * and one non-letter character.
   *
   * @return null if password quality is OK, otherwise the i18n message key of the password check failure.
   */
  @Override
  public List<I18nKeyAndParams> checkPasswordQuality(final String password)
  {
    return this.validate(password, null, false);
  }

  /**
   * Checks the password quality of a new password change is required. Password must have at least n characters and at minimum one letter
   * and one non-letter character.
   *
   * @param newPassword
   * @return null if password quality is OK, otherwise the i18n message key of the password check failure.
   */
  @Override
  public List<I18nKeyAndParams> checkPasswordQuality(final String oldPassword, final String newPassword)
  {
    return validate(newPassword, oldPassword, true);
  }

  private List<I18nKeyAndParams> validate(final String newPassword, final String oldPassword, final boolean checkOldPassword)
  {
    final List<I18nKeyAndParams> result = new ArrayList<>();

    // check min length
    final int minPasswordLength = configurationService.getMinPasswordLength();
    if (newPassword == null || newPassword.length() < minPasswordLength) {
      result.add(new I18nKeyAndParams(MESSAGE_KEY_PASSWORD_MIN_LENGTH_ERROR, configurationService.getMinPasswordLength()));

      if (newPassword == null) {
        return result;
      }
    }

    // check for character and none character
    checkForCharsInPassword(newPassword, result);

    // stop here if only the new password is validated
    if (checkOldPassword == false) {
      return result;
    }

    // compare old and new password
    if (configurationService.getFlagCheckPasswordChange() && StringUtils.equals(oldPassword, newPassword)) {
      result.add(new I18nKeyAndParams(MESSAGE_KEY_PASSWORD_OLD_EQ_NEW_ERROR));
    }

    return result;
  }

  private void checkForCharsInPassword(final String password, final List<I18nKeyAndParams> result)
  {
    boolean letter = false;
    boolean nonLetter = false;
    for (int i = 0; i < password.length(); i++) {
      final char ch = password.charAt(i);
      if (letter == false && Character.isLetter(ch)) {
        letter = true;
      } else if (nonLetter == false && Character.isLetter(ch) == false) {
        nonLetter = true;
      }
    }
    if (letter == false) {
      result.add(new I18nKeyAndParams(MESSAGE_KEY_PASSWORD_CHARACTER_ERROR));
    }
    if (nonLetter == false) {
      result.add(new I18nKeyAndParams(MESSAGE_KEY_PASSWORD_NONCHAR_ERROR));
    }
  }

}
