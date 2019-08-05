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

import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrBaseDO;
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO;
import de.micromata.genome.db.jpa.tabattr.entities.TimeableBaseDO;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Map;

@Entity
@Table(name = "T_PLUGIN_EMPLOYEE_CONFIGURATION_TIMED",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "employee_configuration_id", "group_name", "start_time" })
    },
    indexes = {
        @Index(name = "idx_plugin_employee_configuration_timed_start_time", columnList = "start_time")
    })
public class EmployeeConfigurationTimedDO extends TimeableBaseDO<EmployeeConfigurationTimedDO, Integer>
{
  private EmployeeConfigurationDO employeeConfiguration;

  @Id
  @GeneratedValue
  @Column(name = "pk")
  @Override
  public Integer getPk()
  {
    return pk;
  }

  /**
   * @return The associated EmployeeConfigurationDO.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employee_configuration_id", nullable = false)
  public EmployeeConfigurationDO getEmployeeConfiguration()
  {
    return employeeConfiguration;
  }

  public void setEmployeeConfiguration(final EmployeeConfigurationDO employeeConfiguration)
  {
    this.employeeConfiguration = employeeConfiguration;
  }

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", targetEntity = EmployeeConfigurationTimedAttrDO.class,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @MapKey(name = "propertyName")
  @Override
  public Map<String, JpaTabAttrBaseDO<EmployeeConfigurationTimedDO, Integer>> getAttributes()
  {
    return super.getAttributes();
  }

  @Override
  @Transient
  public Class<? extends JpaTabAttrBaseDO<EmployeeConfigurationTimedDO, ? extends Serializable>> getAttrEntityClass()
  {
    return EmployeeConfigurationTimedAttrDO.class;
  }

  @Override
  @Transient
  public Class<? extends JpaTabAttrBaseDO<EmployeeConfigurationTimedDO, ? extends Serializable>> getAttrEntityWithDataClass()
  {
    return EmployeeConfigurationTimedAttrWithDataDO.class;
  }

  @Override
  @Transient
  public Class<? extends JpaTabAttrDataBaseDO<? extends JpaTabAttrBaseDO<EmployeeConfigurationTimedDO, Integer>, Integer>> getAttrDataEntityClass()
  {
    return EmployeeConfigurationTimedAttrDataDO.class;
  }

  @Override
  public JpaTabAttrBaseDO<EmployeeConfigurationTimedDO, Integer> createAttrEntity(final String key, final char type, final String value)
  {
    return new EmployeeConfigurationTimedAttrDO(this, key, type, value);
  }

  @Override
  public JpaTabAttrBaseDO<EmployeeConfigurationTimedDO, Integer> createAttrEntityWithData(final String key, final char type, final String value)
  {
    return new EmployeeConfigurationTimedAttrWithDataDO(this, key, type, value);
  }
}
