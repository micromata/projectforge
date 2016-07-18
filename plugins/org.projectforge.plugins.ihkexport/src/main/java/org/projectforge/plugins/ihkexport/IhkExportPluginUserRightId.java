package org.projectforge.plugins.ihkexport;

import java.util.Arrays;
import java.util.Collection;

import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.projectforge.business.user.HibernateSearchUserRightIdBridge;
import org.projectforge.framework.persistence.api.IUserRightId;
import org.projectforge.framework.persistence.api.RightRightIdProviderService;

@Indexed
@ClassBridge(index = Index.YES /* TOKENIZED */, store = Store.NO, impl = HibernateSearchUserRightIdBridge.class)
public enum IhkExportPluginUserRightId implements IUserRightId
{
  PLUGIN_IHKEXPORT("PLUGIN_IHKEXPORT", "plugin20", "plugins.ihkexport");
  public static class ProviderService implements RightRightIdProviderService
  {
    @Override
    public Collection<IUserRightId> getUserRightIds()
    {
      return Arrays.asList(IhkExportPluginUserRightId.values());
    }
  }

  private final String id;

  private final String orderString;

  private final String i18nKey;

  /**
   * @param id Must be unique (including all plugins).
   * @param orderString For displaying the rights in e. g. UserEditPage in the correct order.
   * @param i18nKey
   */
  private IhkExportPluginUserRightId(final String id, final String orderString, final String i18nKey)
  {
    this.id = id;
    this.orderString = orderString;
    this.i18nKey = i18nKey;
  }

  @Override
  public String getId()
  {
    return id;
  }

  @Override
  public String getI18nKey()
  {
    return i18nKey;
  }

  @Override
  public String getOrderString()
  {
    return orderString;
  }

  @Override
  public String toString()
  {
    return String.valueOf(id);
  }

  @Override
  public int compareTo(IUserRightId o)
  {
    return this.compareTo(o);
  }

}
