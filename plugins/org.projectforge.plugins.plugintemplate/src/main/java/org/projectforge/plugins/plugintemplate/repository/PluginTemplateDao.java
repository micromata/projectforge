package org.projectforge.plugins.plugintemplate.repository;

import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.plugins.plugintemplate.PluginTemplatePluginUserRightId;
import org.projectforge.plugins.plugintemplate.model.PluginTemplateDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Access to plugin template table.
 *
 * @author Florian Blumenstein
 */
@Repository
public class PluginTemplateDao extends BaseDao<PluginTemplateDO>
{
  @Autowired
  private PfEmgrFactory emgrFactory;

  public PluginTemplateDao()
  {
    super(PluginTemplateDO.class);
    userRightId = PluginTemplatePluginUserRightId.PLUGIN_PLUGINTEMPLATE;
  }

  @Override
  public PluginTemplateDO newInstance()
  {
    return new PluginTemplateDO();
  }

  //To customize get list method for some filter settings
  //  @Override
  //  public List<PluginTemplateDO> getList(BaseSearchFilter filter)
  //  {
  //    final PluginTemplateFilter myFilter;
  //    if (filter instanceof PluginTemplateFilter) {
  //      myFilter = (PluginTemplateFilter) filter;
  //    } else {
  //      myFilter = new PluginTemplateFilter(filter);
  //    }
  //    final QueryFilter queryFilter = createQueryFilter(filter);
  //    if (myFilter.isShowOnlyEntriesWithValue()) {
  //      queryFilter.add(Restrictions.isNotNull("value"));
  //    }
  //    return getList(queryFilter);
  //  }
}
