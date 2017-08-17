package org.projectforge.plugins.plugintemplate.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

import de.micromata.genome.db.jpa.history.api.WithHistory;

@Entity
@Indexed
@Table(name = "T_PLUGIN_PLUGINTEMPLATE")
@WithHistory
public class PluginTemplateDO extends DefaultBaseDO
{

  private static final long serialVersionUID = 661129912349832435L;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  @PropertyInfo(i18nKey = "plugins.plugintemplate.key")
  private String key;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  @PropertyInfo(i18nKey = "plugins.plugintemplate.value")
  private String value;

  @Column(nullable = false)
  public String getKey()
  {
    return key;
  }

  public void setKey(String key)
  {
    this.key = key;
  }

  @Column
  public String getValue()
  {
    return value;
  }

  public void setValue(String value)
  {
    this.value = value;
  }

}
