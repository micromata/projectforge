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

package org.projectforge.plugins.eed.service;

import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.plugins.eed.model.EmployeeConfigurationDO;
import org.springframework.stereotype.Repository;

@Repository
public class EmployeeConfigurationDao extends BaseDao<EmployeeConfigurationDO> {
  protected EmployeeConfigurationDao() {
    super(EmployeeConfigurationDO.class);
    userRightId = UserRightId.HR_EMPLOYEE_SALARY; // this is used for right check from BaseDao::hasAccess which is used e.g. in BaseDao::checkLoggedInUserSelectAccess
  }

  @Override
  public EmployeeConfigurationDO newInstance() {
    return new EmployeeConfigurationDO();
  }
}
