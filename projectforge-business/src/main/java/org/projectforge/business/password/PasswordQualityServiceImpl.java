/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.password;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.framework.i18n.I18nKeyAndParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
  public List<I18nKeyAndParams> checkPasswordQuality(final char[] password)
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
  public List<I18nKeyAndParams> checkPasswordQuality(final char[] oldPassword, final char[] newPassword)
  {
    return validate(newPassword, oldPassword, true);
  }

  private List<I18nKeyAndParams> validate(final char[] newPassword, final char[] oldPassword, final boolean checkOldPassword)
  {
    final List<I18nKeyAndParams> result = new ArrayList<>();

    // check min length
    final int minPasswordLength = configurationService.getMinPasswordLength();
    if (newPassword == null || newPassword.length< minPasswordLength) {
      result.add(new I18nKeyAndParams(MESSAGE_KEY_PASSWORD_MIN_LENGTH_ERROR, configurationService.getMinPasswordLength()));

      if (newPassword == null) {
        return result;
      }
    }

    // check for character and none character
    checkForCharsInPassword(newPassword, result);

    // stop here if only the new password is validated
    if (!checkOldPassword) {
      return result;
    }

    // compare old and new password
    if (configurationService.getFlagCheckPasswordChange() && Arrays.equals(oldPassword, newPassword)) {
      result.add(new I18nKeyAndParams(MESSAGE_KEY_PASSWORD_OLD_EQ_NEW_ERROR));
    }

    return result;
  }

  private void checkForCharsInPassword(final char[] password, final List<I18nKeyAndParams> result)
  {
    boolean letter = false;
    boolean nonLetter = false;
    for (int i = 0; i < password.length; i++) {
      final char ch = password[i];
      if (!letter && Character.isLetter(ch)) {
        letter = true;
      } else if (!nonLetter && !Character.isLetter(ch)) {
        nonLetter = true;
      }
    }
    if (!letter) {
      result.add(new I18nKeyAndParams(MESSAGE_KEY_PASSWORD_CHARACTER_ERROR));
    }
    if (!nonLetter) {
      result.add(new I18nKeyAndParams(MESSAGE_KEY_PASSWORD_NONCHAR_ERROR));
    }
  }

}
