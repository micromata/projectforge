package org.projectforge.plugins.poll;

import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.core.ProjectforgePluginService;

/**
 * The Class PollProjectforgePluginService.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
public class PollProjectforgePluginService implements ProjectforgePluginService
{

  @Override
  public String getPluginId()
  {

    return "poll";
  }

  @Override
  public String getPluginName()
  {
    return getPluginId();
  }

  @Override
  public String getPluginDescription()
  {
    return "Fuer die Durchfuehrung von Anfragen";
  }

  @Override
  public AbstractPlugin createPluginInstance()
  {
    return new PollPlugin();
  }

}
