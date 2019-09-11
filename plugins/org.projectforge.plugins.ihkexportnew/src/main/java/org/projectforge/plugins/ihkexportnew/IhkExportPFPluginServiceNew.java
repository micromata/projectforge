package org.projectforge.plugins.ihkexportnew;

import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.core.PFPluginService;
/**
 * The Class MemoProjectforgePluginService.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
public class IhkExportPFPluginServiceNew implements PFPluginService
{

  /**
   * {@inheritDoc}
   *
   */

  @Override
  public String getPluginId()
  {
    return "ihkexportNew";
  }

  /**
   * {@inheritDoc}
   *
   */

  @Override
  public String getPluginName()
  {
    return "IHK-Export New";
  }

  /**
   * {@inheritDoc}
   *
   */

  @Override
  public String getPluginDescription()
  {
    return "Ausbildungsnachweise drucken";
  }

  /**
   * {@inheritDoc}
   *
   */

  @Override
  public AbstractPlugin createPluginInstance()
  {
    return new IhkExportPluginNew();
  }

}
