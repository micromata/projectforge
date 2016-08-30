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
public class EmployeeConfigurationTimedAttrWithDataDO extends EmployeeConfigurationTimedAttrDO
{
  // this constructor is necessary for JPA
  public EmployeeConfigurationTimedAttrWithDataDO()
  {
    super();
  }

  public EmployeeConfigurationTimedAttrWithDataDO(final EmployeeConfigurationTimedDO parent, final String key, final char type,
      final String value)
  {
    super(parent, key, type, value);
  }

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", targetEntity = EmployeeConfigurationTimedAttrDO.class,
      orphanRemoval = true, fetch = FetchType.EAGER)
  @Override
  @OrderColumn(name = "datarow")
  public List<JpaTabAttrDataBaseDO<?, Integer>> getData()
  {
    return super.getData();
  }

}
