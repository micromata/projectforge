package org.projectforge.plugins.ihkexportold;

import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.core.PFPluginService;
/**
 * The Class MemoProjectforgePluginService.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
public class IhkExportPFPluginServiceOld implements PFPluginService
{

  /**
   * {@inheritDoc}
   *
   */

  @Override
  public String getPluginId()
  {
    return "ihkexportOld";
  }

  /**
   * {@inheritDoc}
   *
   */

  @Override
  public String getPluginName()
  {
    return "IHK-Export Old";
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
    return new IhkExportPluginOld();
  }

}
