package org.projectforge.plugins.eed;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

import de.micromata.genome.db.jpa.history.api.HistoryProperty;
import de.micromata.genome.db.jpa.history.api.WithHistory;
import de.micromata.genome.db.jpa.history.impl.TimependingHistoryPropertyConverter;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithConfigurableAttr;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithTimeableAttr;
import de.micromata.genome.jpa.ComplexEntity;
import de.micromata.genome.jpa.ComplexEntityVisitor;

@Entity
@Table(name = "T_PLUGIN_EMPLOYEE_CONFIGURATION",
    uniqueConstraints = { @UniqueConstraint(columnNames = { "tenant_id" } /* only one entity per tenant allowed */) }
)
@WithHistory
public class EmployeeConfigurationDO extends DefaultBaseDO implements EntityWithTimeableAttr<Integer, EmployeeConfigurationTimedDO>, EntityWithConfigurableAttr,
    ComplexEntity
{
  private List<EmployeeConfigurationTimedDO> timeableAttributes = new ArrayList<>();

  @Override
  @Transient
  public String getAttrSchemaName()
  {
    return "employeeConfiguration";
  }

  @Override
  public void addTimeableAttribute(final EmployeeConfigurationTimedDO row)
  {
    row.setEmployeeConfiguration(this);
    timeableAttributes.add(row);
  }

  @Override
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "employeeConfiguration")
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
    timeableAttributes.forEach(row -> row.visit(visitor));
  }
}
