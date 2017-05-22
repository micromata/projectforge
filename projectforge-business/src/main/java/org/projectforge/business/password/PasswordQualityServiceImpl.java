package org.projectforge.business.password;

import java.util.HashSet;
import java.util.Set;

import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.framework.i18n.I18nKeyAndParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Matthias Altmann (m.altmann@micromata.de)
 */
@Service
public class PasswordQualityServiceImpl implements PasswordQualityService
{

  private static final String MESSAGE_KEY_PASSWORD_QUALITY_CHECK = "user.changePassword.error.passwordQualityCheck";

  @Autowired
  private ConfigurationService configurationService;

  @Override
  public Set<I18nKeyAndParams> getPasswordQualityI18nKeyAndParams()
  {
    Set<I18nKeyAndParams> i18nKeyAndParams = new HashSet<>();
    i18nKeyAndParams.add(new I18nKeyAndParams(MESSAGE_KEY_PASSWORD_QUALITY_CHECK, configurationService.getMinPasswordLength()));
    return i18nKeyAndParams;

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
    boolean letter = false;
    boolean nonLetter = false;
    final int minPasswordLength = configurationService.getMinPasswordLength();
    if (newPassword == null || newPassword.length() < minPasswordLength) {
      return getPasswordQualityI18nKeyAndParams();
    }
    for (int i = 0; i < newPassword.length(); i++) {
      final char ch = newPassword.charAt(i);
      if (letter == false && Character.isLetter(ch) == true) {
        letter = true;
      } else if (nonLetter == false && Character.isLetter(ch) == false) {
        nonLetter = true;
      }
    }
    if (letter == true && nonLetter == true) {
      return null;
    }
    return getPasswordQualityI18nKeyAndParams();
  }
}
