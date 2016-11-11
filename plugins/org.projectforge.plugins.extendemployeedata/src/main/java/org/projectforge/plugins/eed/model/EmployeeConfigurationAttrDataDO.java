package org.projectforge.plugins.eed.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO;

@Entity
@Table(name = "T_PLUGIN_EMPLOYEE_CONFIGURATION_attrdata")
public class EmployeeConfigurationAttrDataDO extends JpaTabAttrDataBaseDO<EmployeeConfigurationAttrDO, Integer>
{
  public EmployeeConfigurationAttrDataDO()
  {
    super();
  }

  public EmployeeConfigurationAttrDataDO(final EmployeeConfigurationAttrDO parent, final String value)
  {
    super(parent, value);
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
  public EmployeeConfigurationAttrDO getParent()
  {
    return super.getParent();
  }
}
