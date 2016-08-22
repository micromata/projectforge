package org.projectforge.plugins.eed;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

import de.micromata.genome.db.jpa.history.api.WithHistory;

@Entity
@Indexed
@Table(name = "T_PLUGIN_EMPLOYEE_CONFIGURATION",
    uniqueConstraints = { @UniqueConstraint(columnNames = {"key", "tenant_id" }) })
@WithHistory
public class EmployeeConfigurationDO extends DefaultBaseDO
{

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String key;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String value;

  @Column(name = "key", length = 255, nullable = false)
  public String getKey()
  {
    return key;
  }

  public void setKey(final String key)
  {
    this.key = key;
  }

  public void setValue(final String value)
  {
    this.value = value;
  }

  @Column(name = "value", length = 255, nullable = false)
  public String getValue()
  {
    return value;
  }


}
