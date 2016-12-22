package org.projectforge.plugins.eed.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.projectforge.framework.persistence.api.AUserRightId;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.attr.entities.DefaultBaseWithAttrDO;
import org.projectforge.framework.persistence.jpa.impl.BaseDaoJpaAdapter;

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
