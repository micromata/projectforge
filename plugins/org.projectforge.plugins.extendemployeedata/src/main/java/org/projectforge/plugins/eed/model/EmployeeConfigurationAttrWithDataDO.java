package org.projectforge.plugins.eed.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO;

@Entity
@DiscriminatorValue("1")
public class EmployeeConfigurationAttrWithDataDO extends EmployeeConfigurationAttrDO
{
  public EmployeeConfigurationAttrWithDataDO()
  {
    super();
  }

  public EmployeeConfigurationAttrWithDataDO(final EmployeeConfigurationDO parent, final String propertyName, final char type, final String value)
  {
    super(parent, propertyName, type, value);
  }

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", targetEntity = EmployeeConfigurationAttrDataDO.class,
      orphanRemoval = true, fetch = FetchType.EAGER)
  @OrderColumn(name = "datarow")
  @Override
  public List<JpaTabAttrDataBaseDO<?, Integer>> getData()
  {
    return super.getData();
  }
}
