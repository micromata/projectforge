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

import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrBaseDO;
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO;

@Entity
@Table(name = "T_PLUGIN_EMPLOYEE_CONFIGURATION_attr")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "withdata", discriminatorType = DiscriminatorType.CHAR)
@DiscriminatorValue("0")
public class EmployeeConfigurationAttrDO extends JpaTabAttrBaseDO<EmployeeConfigurationDO, Integer>
{
  public EmployeeConfigurationAttrDO()
  {
    super();
  }

  public EmployeeConfigurationAttrDO(final EmployeeConfigurationDO parent, final String propertyName, final char type, final String value)
  {
    super(parent, propertyName, type, value);
  }

  @Override
  public JpaTabAttrDataBaseDO<?, Integer> createData(String data)
  {
    return new EmployeeConfigurationAttrDataDO(this, data);
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
  @JoinColumn(name = "parent", referencedColumnName = "pk")
  public EmployeeConfigurationDO getParent()
  {
    return super.getParent();
  }
}
