/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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
   */
  I18nKeyAndParams getPasswordQualityI18nKeyAndParams();

  /**
   * Check password quality and compare old and new password.
   */
  List<I18nKeyAndParams> checkPasswordQuality(String oldPassword, String newPassword);

  /**
   * Checks the password quality of a new password.
   */
  List<I18nKeyAndParams> checkPasswordQuality(String password);
}
