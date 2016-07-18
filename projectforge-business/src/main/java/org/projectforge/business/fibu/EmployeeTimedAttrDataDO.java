/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.business.fibu;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO;

/**
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 *
 */
@Entity
@Table(name = "t_fibu_employee_timedattrdata")
public class EmployeeTimedAttrDataDO extends JpaTabAttrDataBaseDO<EmployeeTimedAttrDO, Integer>
{

  public EmployeeTimedAttrDataDO()
  {
    super();
  }

  public EmployeeTimedAttrDataDO(final EmployeeTimedAttrDO parent, final String value)
  {
    super(parent, value);
  }

  public EmployeeTimedAttrDataDO(final EmployeeTimedAttrDO parent)
  {
    super(parent);
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
  public EmployeeTimedAttrDO getParent()
  {
    return super.getParent();
  }

}
