package org.projectforge.plugins.plugintemplate;

import org.projectforge.business.user.UserRightAccessCheck;
import org.projectforge.business.user.UserRightCategory;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.plugins.plugintemplate.model.PluginTemplateDO;

public class PluginTemplateRight extends UserRightAccessCheck<PluginTemplateDO>
{
  public PluginTemplateRight(AccessChecker accessChecker)
  {
    super(accessChecker, PluginTemplatePluginUserRightId.PLUGIN_PLUGINTEMPLATE, UserRightCategory.PLUGINS,
        UserRightValue.TRUE);
  }

  /**
   * @return true if the owner is equals to the logged-in user, otherwise false.
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final PluginTemplateDO obj, final PluginTemplateDO oldObj,
      final OperationType operationType)
  {
    return true;
  }
}
