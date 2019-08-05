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

import de.micromata.genome.db.jpa.history.api.HistoryProperty;
import de.micromata.genome.db.jpa.history.api.WithHistory;
import de.micromata.genome.db.jpa.history.impl.TabAttrHistoryPropertyConverter;
import de.micromata.genome.db.jpa.history.impl.TimependingHistoryPropertyConverter;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithConfigurableAttr;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithTimeableAttr;
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrBaseDO;
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO;
import de.micromata.genome.jpa.ComplexEntity;
import de.micromata.genome.jpa.ComplexEntityVisitor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.projectforge.framework.persistence.api.AUserRightId;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.attr.entities.DefaultBaseWithAttrDO;
import org.projectforge.framework.persistence.jpa.impl.BaseDaoJpaAdapter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "T_PLUGIN_EMPLOYEE_CONFIGURATION",
    uniqueConstraints = { @UniqueConstraint(columnNames = { "tenant_id" } /* only one entity per tenant allowed */) })
@WithHistory
@AUserRightId("HR_EMPLOYEE_SALARY") // this is used for right check from CorePersistenceServiceImpl::update
public class EmployeeConfigurationDO extends DefaultBaseWithAttrDO<EmployeeConfigurationDO>
    implements EntityWithTimeableAttr<Integer, EmployeeConfigurationTimedDO>, EntityWithConfigurableAttr,
    ComplexEntity
{
  private static final long serialVersionUID = -996267280801986212L;

  private List<EmployeeConfigurationTimedDO> timeableAttributes = new ArrayList<>();

  @Override
  @Transient
  public String getAttrSchemaName()
  {
    return "employeeConfiguration";
  }

  @Override
  @Transient
  public Class<? extends JpaTabAttrDataBaseDO<? extends JpaTabAttrBaseDO<EmployeeConfigurationDO, Integer>, Integer>> getAttrDataEntityClass()
  {
    return EmployeeConfigurationAttrDataDO.class;
  }

  @Override
  @Transient
  public Class<? extends JpaTabAttrBaseDO<EmployeeConfigurationDO, Integer>> getAttrEntityClass()
  {
    return EmployeeConfigurationAttrDO.class;
  }

  @Override
  @Transient
  public Class<? extends JpaTabAttrBaseDO<EmployeeConfigurationDO, Integer>> getAttrEntityWithDataClass()
  {
    return EmployeeConfigurationAttrWithDataDO.class;
  }

  @Override
  public JpaTabAttrBaseDO<EmployeeConfigurationDO, Integer> createAttrEntityWithData(String key, char type, String value)
  {
    return new EmployeeConfigurationAttrWithDataDO(this, key, type, value);
  }

  @Override
  public JpaTabAttrBaseDO<EmployeeConfigurationDO, Integer> createAttrEntity(String key, char type, String value)
  {
    return new EmployeeConfigurationAttrDO(this, key, type, value);
  }

  @Override
  public void addTimeableAttribute(final EmployeeConfigurationTimedDO row)
  {
    row.setEmployeeConfiguration(this);
    timeableAttributes.add(row);
  }

  @Override
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true,
      mappedBy = "employeeConfiguration")
  // fetch mode, only returns one
  @Fetch(FetchMode.SELECT)
  // unfortunatelly this does work. Date is not valid for order (only integral types)
  //  @OrderColumn(name = "startTime")
  @HistoryProperty(converter = TimependingHistoryPropertyConverter.class)
  public List<EmployeeConfigurationTimedDO> getTimeableAttributes()
  {
    return timeableAttributes;
  }

  public void setTimeableAttributes(final List<EmployeeConfigurationTimedDO> timeableAttributes)
  {
    this.timeableAttributes = timeableAttributes;
  }

  // this is necessary to set createdAt etc. in EmployeeConfigurationTimedDO
  @Override
  public void visit(final ComplexEntityVisitor visitor)
  {
    super.visit(visitor);
    timeableAttributes.forEach(row -> row.visit(visitor));
  }

  @Override
  public ModificationStatus copyValuesFrom(final BaseDO<? extends Serializable> source, final String... ignoreFields)
  {
    ModificationStatus modificationStatus = super.copyValuesFrom(source, "timeableAttributes");
    final EmployeeConfigurationDO src = (EmployeeConfigurationDO) source;
    modificationStatus = modificationStatus
        .combine(BaseDaoJpaAdapter.copyTimeableAttribute(this, src));
    return modificationStatus;
  }

  @Override
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", targetEntity = EmployeeConfigurationAttrDO.class, orphanRemoval = true,
      fetch = FetchType.EAGER)
  @MapKey(name = "propertyName")
  @HistoryProperty(converter = TabAttrHistoryPropertyConverter.class)
  public Map<String, JpaTabAttrBaseDO<EmployeeConfigurationDO, Integer>> getAttrs()
  {
    return super.getAttrs();
  }

}
