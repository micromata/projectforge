package org.projectforge.business.password;

import org.projectforge.framework.i18n.I18nKeysAndParamsSet;

/**
 * @author Matthias Altmann (m.altmann@micromata.de)
 */
public interface PasswordQualityService
{
  I18nKeysAndParamsSet getPasswordQualityI18nKeyAndParams();

  I18nKeysAndParamsSet checkPasswordQualityOnChange(String oldPassword, String newPassword);

  I18nKeysAndParamsSet checkPasswordQuality(String password);
}
