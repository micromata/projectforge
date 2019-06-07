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

package org.projectforge.business.fibu;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrBaseDO;

/**
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 */
@Entity
@Table(name = "t_fibu_employee_timedattr")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "withdata", discriminatorType = DiscriminatorType.CHAR)
@DiscriminatorValue("0")
public class EmployeeTimedAttrDO extends JpaTabAttrBaseDO<EmployeeTimedDO, Integer>
{

  public EmployeeTimedAttrDO()
  {
    super();
  }

  public EmployeeTimedAttrDO(final EmployeeTimedDO parent, final String propertyName, final char type,
      final String value)
  {
    super(parent, propertyName, type, value);
  }

  public EmployeeTimedAttrDO(final EmployeeTimedDO parent)
  {
    super(parent);
  }

  /**
   * @see org.projectforge.framework.persistence.attr.entities.DeprAttrBaseDO#createData(java.lang.String)
   */
  @Override
  public EmployeeTimedAttrDataDO createData(final String data)
  {
    return new EmployeeTimedAttrDataDO(this, data);

  }

  @Override
  @ManyToOne(optional = false)
  @JoinColumn(name = "parent", referencedColumnName = "pk")
  public EmployeeTimedDO getParent()
  {
    return super.getParent();

  }

  @Override
  @Id
  @GeneratedValue
  @Column(name = "pk")
  public Integer getPk()
  {
    return pk;
  }

}
