package org.projectforge.plugins.licensemanagement;

import java.util.Arrays;
import java.util.Collection;

import org.projectforge.framework.persistence.api.IUserRightId;
import org.projectforge.framework.persistence.api.RightRightIdProviderService;

public enum LicensemanagementPluginUserRightsId implements IUserRightId
{
  PLUGIN_LICENSE_MANAGEMENT("PLUGIN_LICENSE_MANAGEMENT", "plugin100",
      "plugins.licensemanagement.license");
  public static class ProviderService implements RightRightIdProviderService
  {
    @Override
    public Collection<IUserRightId> getUserRightIds()
    {
      return Arrays.asList(LicensemanagementPluginUserRightsId.values());
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
  private LicensemanagementPluginUserRightsId(final String id, final String orderString, final String i18nKey)
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
