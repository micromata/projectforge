package org.projectforge.business.password;

import java.util.List;

import org.projectforge.framework.i18n.I18nKeyAndParams;

/**
 * Interface holding password quality check functions in Projectforge.
 *
 * @author Matthias Altmann (m.altmann@micromata.de)
 */
public interface PasswordQualityService
{
  /**
   * Gets password quality.
   *
   * @return the password quality i18nkeyandparams
   */
  List<I18nKeyAndParams> getPasswordQualityI18nKeyAndParams();

  /**
   * Check password quality and compare old and new password.
   *
   * @param oldPassword the old password
   * @param newPassword the new password
   * @return 18nkeysandparams set of constraints not fulfilled.
   */
  List<I18nKeyAndParams> checkPasswordQuality(String oldPassword, String newPassword);

  /**
   * Checks the password quality of a new password.
   *
   * @param password entered password
   * @return 18nkeysandparams set of constraints not fulfilled.
   */
  List<I18nKeyAndParams> checkPasswordQuality(String password);
}
