package org.projectforge.plugins.eed.model;

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
import javax.persistence.UniqueConstraint;

import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrBaseDO;

@Entity
@Table(name = "T_PLUGIN_EMPLOYEE_CONFIGURATION_TIMEDATTR", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "parent", "propertyName" })
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "withdata", discriminatorType = DiscriminatorType.CHAR)
@DiscriminatorValue("0")
public class EmployeeConfigurationTimedAttrDO extends JpaTabAttrBaseDO<EmployeeConfigurationTimedDO, Integer>
{
  // this constructor is necessary for JPA
  public EmployeeConfigurationTimedAttrDO()
  {
    super();
  }

  public EmployeeConfigurationTimedAttrDO(EmployeeConfigurationTimedDO parent)
  {
    super(parent);
  }

  public EmployeeConfigurationTimedAttrDO(final EmployeeConfigurationTimedDO parent, final String key, final char type,
      final String value)
  {
    super(parent, key, type, value);
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
  public EmployeeConfigurationTimedAttrDataDO createData(final String data)
  {
    return new EmployeeConfigurationTimedAttrDataDO(this, data);
  }

  @Override
  @ManyToOne(optional = false)
  @JoinColumn(name = "parent", referencedColumnName = "pk")
  public EmployeeConfigurationTimedDO getParent()
  {
    return super.getParent();
  }

}
