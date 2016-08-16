package org.projectforge.plugins.eed;

import de.micromata.genome.db.jpa.history.api.WithHistory;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Indexed
@Table(name = "T_PLUGIN_EMPLOYEE_GENERAL_VALUE",
    uniqueConstraints = { @UniqueConstraint(columnNames = {"key", "tenant_id" }) })
@WithHistory
public class EmployeeGeneralValueDO extends DefaultBaseDO
{

  private static final long serialVersionUID = -2146312748316341791L;

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
