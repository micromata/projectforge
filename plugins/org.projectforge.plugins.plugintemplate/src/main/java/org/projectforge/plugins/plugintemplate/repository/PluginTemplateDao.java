package org.projectforge.plugins.plugintemplate.repository;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.plugins.plugintemplate.PluginTemplatePluginUserRightId;
import org.projectforge.plugins.plugintemplate.model.PluginTemplateDO;
import org.projectforge.plugins.plugintemplate.wicket.PluginTemplateFilter;
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
  @Override
  public List<PluginTemplateDO> getList(BaseSearchFilter filter)
  {
    final PluginTemplateFilter myFilter;
    if (filter instanceof PluginTemplateFilter) {
      myFilter = (PluginTemplateFilter) filter;
    } else {
      myFilter = new PluginTemplateFilter(filter);
    }
    final QueryFilter queryFilter = createQueryFilter(filter);
    if (myFilter.isShowOnlyEntriesWithValue()) {
      queryFilter.add(Restrictions.isNotNull("value"));
    }
    return getList(queryFilter);

    //Without access check and hibernate fulltext search.
    //return getJPAList(myFilter);
  }

  //JPA example without access check and hibernate full text search.
  public List<PluginTemplateDO> getJPAList(PluginTemplateFilter filter)
  {
    List<PluginTemplateDO> result = new ArrayList<>();
    result = emgrFactory.runRoTrans(emgr -> {
      String sql = "SELECT pt FROM PluginTemplateDO pt";
      if (filter.isShowOnlyEntriesWithValue()) {
        sql += " WHERE pt.value is not null";
      }
      return emgr.select(PluginTemplateDO.class, sql);
    });
    return result;
  }
}
