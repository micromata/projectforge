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

import com.fasterxml.jackson.annotation.JsonManagedReference;
import de.micromata.genome.db.jpa.tabattr.api.TimeableAttrRow;
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrBaseDO;
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO;
import de.micromata.genome.db.jpa.tabattr.entities.TimeableBaseDO;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.framework.persistence.api.IdObject;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Map;

/**
 *
 * Time pending attributes of an employee.
 *
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 *
 */
@Entity
@Indexed
@Table(name = "t_fibu_employee_timed",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "employee_id", "group_name", "start_time" })
    },
    indexes = {
        @Index(name = "idx_fibu_employee_timed_start_time", columnList = "start_time")
    })
public class EmployeeTimedDO extends TimeableBaseDO<EmployeeTimedDO, Integer>
    implements TimeableAttrRow<Integer>, IdObject<Integer>
{

  @JsonManagedReference
  @PropertyInfo(i18nKey = "fibu.employee")
  @IndexedEmbedded(depth = 2)
  private EmployeeDO employee;

  @Id
  @GeneratedValue
  @Column(name = "pk")
  @Override
  public Integer getPk()
  {
    return pk;
  }

  @Override
  @Transient
  public Integer getId()
  {
    return pk;
  }

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", targetEntity = EmployeeTimedAttrDO.class,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @MapKey(name = "propertyName")
  @Override
  public Map<String, JpaTabAttrBaseDO<EmployeeTimedDO, Integer>> getAttributes()
  {
    return super.getAttributes();
  }

  /**
   * @return Zugehöriger Mitarbeiter.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employee_id", nullable = false)
  public EmployeeDO getEmployee()
  {
    return employee;
  }

  public void setEmployee(final EmployeeDO employee)
  {
    this.employee = employee;
  }

  @Override
  @Transient
  public Class<? extends JpaTabAttrBaseDO<EmployeeTimedDO, ? extends Serializable>> getAttrEntityClass()
  {
    return EmployeeTimedAttrDO.class;
  }

  @Override
  @Transient
  public Class<? extends JpaTabAttrBaseDO<EmployeeTimedDO, ? extends Serializable>> getAttrEntityWithDataClass()
  {
    return EmployeeTimedAttrWithDataDO.class;
  }

  @Override
  @Transient
  public Class<? extends JpaTabAttrDataBaseDO<? extends JpaTabAttrBaseDO<EmployeeTimedDO, Integer>, Integer>> getAttrDataEntityClass()
  {
    return EmployeeTimedAttrDataDO.class;
  }

  @Override
  public JpaTabAttrBaseDO<EmployeeTimedDO, Integer> createAttrEntity(String key, char type, String value)
  {
    return new EmployeeTimedAttrDO(this, key, type, value);
  }

  @Override
  public JpaTabAttrBaseDO<EmployeeTimedDO, Integer> createAttrEntityWithData(String key, char type, String value)
  {
    return new EmployeeTimedAttrWithDataDO(this, key, type, value);
  }

}
