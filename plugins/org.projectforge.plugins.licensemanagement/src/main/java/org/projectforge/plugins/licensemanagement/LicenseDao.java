/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.licensemanagement;

import org.projectforge.framework.persistence.api.BaseDao;
import org.springframework.stereotype.Service;

/**
 *
 * @author Kai Reinhard
 *
 */
@Service
public class LicenseDao extends BaseDao<LicenseDO>
{
  public LicenseDao()
  {
    super(LicenseDO.class);
    userRightId = LicensemanagementPluginUserRightsId.PLUGIN_LICENSE_MANAGEMENT;
  }

  /**
   * Organization and product complete from the values already entered. Every user with select access sees all
   * licenses, so no check of single entities is needed (the key is no autocompletion property).
   */
  @Override
  public boolean isAutocompletionPropertyEnabled(final String property)
  {
    return "organization".equals(property) || "product".equals(property);
  }

  @Override
  public LicenseDO newInstance()
  {
    return new LicenseDO();
  }
}
