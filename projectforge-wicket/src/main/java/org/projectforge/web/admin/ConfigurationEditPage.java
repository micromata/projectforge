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

package org.projectforge.web.admin;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.framework.configuration.ConfigurationDao;
import org.projectforge.framework.configuration.entities.ConfigurationDO;
import org.projectforge.web.MenuItemRegistry;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.projectforge.web.wicket.EditPage;
import org.slf4j.Logger;

@EditPage(defaultReturnPage = ConfigurationListPage.class)
public class ConfigurationEditPage extends AbstractEditPage<ConfigurationDO, ConfigurationEditForm, ConfigurationDao>
    implements
    ISelectCallerPage
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConfigurationEditPage.class);

  private static final long serialVersionUID = -8192471994161712577L;

  @SpringBean
  private ConfigurationDao configurationDao;

  @SpringBean
  private MenuItemRegistry menuItemRegistry;

  public ConfigurationEditPage(final PageParameters parameters)
  {
    super(parameters, "administration.configuration");
    init();
  }

  /**
   * Calls {@link MenuItemRegistry#refresh()} for the case that the visibility of some menu entries might have change.
   * 
   * @see org.projectforge.web.wicket.AbstractEditPage#afterSaveOrUpdate()
   */

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#afterSaveOrUpdate()
   */
  @Override
  public AbstractSecuredBasePage afterSaveOrUpdate()
  {
    menuItemRegistry.refresh();
    return null;
  }

  @Override
  protected ConfigurationDao getBaseDao()
  {
    return configurationDao;
  }

  @Override
  protected ConfigurationEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final ConfigurationDO data)
  {
    return new ConfigurationEditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  @Override
  public void cancelSelection(final String property)
  {
  }

  @Override
  public void select(final String property, final Object selectedValue)
  {
    if (property == null) {
      log.error("Oups, null property not supported for selection.");
      return;
    }
    if ("taskId".equals(property) == true) {
      form.setTask((Integer) selectedValue);
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  @Override
  public void unselect(final String property)
  {
    if (property == null) {
      log.error("Oups, null property not supported for selection.");
      return;
    }
    if ("taskId".equals(property) == true) {
      form.setTask((Integer) null);
    } else {
      log.error("Property '" + property + "' not supported for unselection.");
    }
  }
}
