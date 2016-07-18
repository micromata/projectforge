package org.projectforge.plugins.memo;

import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.core.ProjectforgePluginService;

/**
 * The Class MemoProjectforgePluginService.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
public class MemoProjectforgePluginService implements ProjectforgePluginService
{

  /**
   * {@inheritDoc}
   *
   */

  @Override
  public String getPluginId()
  {
    return "memo";
  }

  /**
   * {@inheritDoc}
   *
   */

  @Override
  public String getPluginName()
  {
    return "Memo";
  }

  /**
   * {@inheritDoc}
   *
   */

  @Override
  public String getPluginDescription()
  {
    return "Memo description";
  }

  /**
   * {@inheritDoc}
   *
   */

  @Override
  public AbstractPlugin createPluginInstance()
  {
    return new MemoPlugin();
  }

}
