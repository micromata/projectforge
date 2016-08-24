package org.projectforge.plugins.eed;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO;

@Entity
@Table(name = "T_PLUGIN_EMPLOYEE_CONFIGURATION_TIMEDATTRDATA")
public class EmployeeConfigurationTimedAttrDataDO extends JpaTabAttrDataBaseDO<EmployeeConfigurationTimedAttrDO, Integer>
{
  // this constructor is necessary for JPA
  public EmployeeConfigurationTimedAttrDataDO()
  {
    super();
  }

  public EmployeeConfigurationTimedAttrDataDO(EmployeeConfigurationTimedAttrDO parent)
  {
    super(parent);
  }

  public EmployeeConfigurationTimedAttrDataDO(final EmployeeConfigurationTimedAttrDO parent, final String data)
  {
    super(parent, data);
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
  public EmployeeConfigurationTimedAttrDO getParent()
  {
    return super.getParent();
  }

}
