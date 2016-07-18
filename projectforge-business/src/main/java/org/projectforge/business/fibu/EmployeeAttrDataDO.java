package org.projectforge.business.fibu;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO;

@Entity
@Table(name = "t_fibu_employee_attrdata")
public class EmployeeAttrDataDO extends JpaTabAttrDataBaseDO<EmployeeAttrDO, Integer>
{
  public EmployeeAttrDataDO()
  {
    super();
  }

  public EmployeeAttrDataDO(final EmployeeAttrDO parent, final String value)
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
  public EmployeeAttrDO getParent()
  {
    return super.getParent();
  }
}
