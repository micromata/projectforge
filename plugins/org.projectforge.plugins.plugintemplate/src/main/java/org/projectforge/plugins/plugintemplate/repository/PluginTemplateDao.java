/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.plugins.plugintemplate.repository;

import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.plugins.plugintemplate.PluginTemplatePluginUserRightId;
import org.projectforge.plugins.plugintemplate.model.PluginTemplateDO;
import org.projectforge.plugins.plugintemplate.wicket.PluginTemplateFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Access to plugin template table.
 *
 * @author Florian Blumenstein
 */
@Repository
public class PluginTemplateDao extends BaseDao<PluginTemplateDO> {
  @Autowired
  private PfEmgrFactory emgrFactory;

  public PluginTemplateDao() {
    super(PluginTemplateDO.class);
    userRightId = PluginTemplatePluginUserRightId.PLUGIN_PLUGINTEMPLATE;
  }

  @Override
  public PluginTemplateDO newInstance() {
    return new PluginTemplateDO();
  }

  //To customize get list method for some filter settings
  @Override
  public List<PluginTemplateDO> getList(BaseSearchFilter filter) {
    final PluginTemplateFilter myFilter;
    if (filter instanceof PluginTemplateFilter) {
      myFilter = (PluginTemplateFilter) filter;
    } else {
      myFilter = new PluginTemplateFilter(filter);
    }
    final QueryFilter queryFilter = createQueryFilter(filter);
    if (myFilter.isShowOnlyEntriesWithValue()) {
      queryFilter.add(QueryFilter.isNotNull("value"));
    }
    return getList(queryFilter);

    //Without access check and hibernate fulltext search.
    //return getJPAList(myFilter);
  }

  //JPA example without access check and hibernate full text search.
  public List<PluginTemplateDO> getJPAList(PluginTemplateFilter filter) {
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
