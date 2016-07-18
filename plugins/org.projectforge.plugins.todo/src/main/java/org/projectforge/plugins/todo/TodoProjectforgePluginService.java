package org.projectforge.plugins.todo;

import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.core.ProjectforgePluginService;

/**
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class TodoProjectforgePluginService implements ProjectforgePluginService
{

  @Override
  public String getPluginId()
  {
    return "todo";
  }

  @Override
  public String getPluginName()
  {
    return "Todo";
  }

  @Override
  public String getPluginDescription()
  {
    return "Manage Todos";
  }

  @Override
  public AbstractPlugin createPluginInstance()
  {
    return new ToDoPlugin();
  }

}
