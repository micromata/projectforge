package org.projectforge.business.password;

import org.projectforge.framework.i18n.I18nKeysAndParamsSet;

/**
 * Interface holding password quality check functions in Projectforge.
 *
 * @author Matthias Altmann (m.altmann@micromata.de)
 */
public interface PasswordQualityService
{
  /**
   * Gets password quality i18nkeyandparams.
   *
   * @return the password quality i18nkeyandparams
   */
  I18nKeysAndParamsSet getPasswordQualityI18nKeyAndParams();

  /**
   * Check password quality on change 18nkeysandparams set.
   *
   * @param oldPassword the old password
   * @param newPassword the new password
   * @return 18nkeysandparams set of constraints not fulfilled.
   */
  I18nKeysAndParamsSet checkPasswordQualityOnChange(String oldPassword, String newPassword);

  /**
   * Check password quality 18nkeysandparams set.
   *
   * @param password entered password
   * @return 18nkeysandparams set of constraints not fulfilled.
   */
  I18nKeysAndParamsSet checkPasswordQuality(String password);
}
