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

package org.projectforge.plugins.eed.model;

import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO;

import javax.persistence.*;

@Entity
@Table(name = "T_PLUGIN_EMPLOYEE_CONFIGURATION_TIMEDATTRDATA")
public class EmployeeConfigurationTimedAttrDataDO extends JpaTabAttrDataBaseDO<EmployeeConfigurationTimedAttrDO, Integer>
{
  // this constructor is necessary for JPA
  public EmployeeConfigurationTimedAttrDataDO()
  {
    super();
  }

  public EmployeeConfigurationTimedAttrDataDO(EmployeeConfigurationTimedAttrDO parent)
  {
    super(parent);
  }

  public EmployeeConfigurationTimedAttrDataDO(final EmployeeConfigurationTimedAttrDO parent, final String data)
  {
    super(parent, data);
  }

  @Override
  @Id
  @GeneratedValue
  @Column(name = "pk")
  public Integer getPk()
  {
    return pk;
  }

  @Override
  @ManyToOne(optional = false)
  @JoinColumn(name = "parent_id", referencedColumnName = "pk")
  public EmployeeConfigurationTimedAttrDO getParent()
  {
    return super.getParent();
  }

}
