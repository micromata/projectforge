package org.projectforge.business.password;

import java.util.Set;

import org.projectforge.framework.i18n.I18nKeyAndParams;

/**
 * @author Matthias Altmann (m.altmann@micromata.de)
 */
public interface PasswordQualityService
{
  Set<I18nKeyAndParams> getPasswordQualityI18nKeyAndParams();

  Set<I18nKeyAndParams> checkPasswordQuality(String newPassword);
}
