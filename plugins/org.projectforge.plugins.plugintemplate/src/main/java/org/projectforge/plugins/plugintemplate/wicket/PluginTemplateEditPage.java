/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.plugins.plugintemplate.wicket;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.plugins.plugintemplate.model.PluginTemplateDO;
import org.projectforge.plugins.plugintemplate.repository.PluginTemplateDao;
import org.projectforge.plugins.plugintemplate.service.PluginTemplateService;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.EditPage;
import org.slf4j.Logger;

@EditPage(defaultReturnPage = PluginTemplateListPage.class)
public class PluginTemplateEditPage extends AbstractEditPage<PluginTemplateDO, PluginTemplateEditForm, PluginTemplateDao>
    implements ISelectCallerPage
{
  private static final long serialVersionUID = -3899191243765232906L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PluginTemplateEditPage.class);

  @SpringBean
  private PluginTemplateService pluginTemplateService;

  public PluginTemplateEditPage(final PageParameters parameters)
  {
    super(parameters, "plugins.plugintemplate");
    init();
  }

  @Override
  public void select(final String property, final Object selectedValue)
  {
    // Do nothing.
  }

  @Override
  public void unselect(final String property)
  {
    // Do nothing.
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#cancelSelection(java.lang.String)
   */
  @Override
  public void cancelSelection(final String property)
  {
    // Do nothing.
  }

  @Override
  protected PluginTemplateDao getBaseDao()
  {
    return pluginTemplateService.getPluginTemplateDao();
  }

  @Override
  protected PluginTemplateEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final PluginTemplateDO data)
  {
    return new PluginTemplateEditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
