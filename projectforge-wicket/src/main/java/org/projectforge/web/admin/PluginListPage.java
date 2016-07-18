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
import org.projectforge.plugins.core.PluginAdminService;
import org.projectforge.web.wicket.AbstractStandardFormPage;

/**
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class PluginListPage extends AbstractStandardFormPage
{
  private static final long serialVersionUID = 5745110028112481137L;

  @SpringBean
  private PluginAdminService pluginAdminService;
  private PluginListForm form;

  public PluginListPage(final PageParameters parameters)
  {
    super(parameters);
  }

  @SuppressWarnings("serial")
  @Override
  protected void onInitialize()
  {
    checkAccess();
    super.onInitialize();

    PluginListForm form = new PluginListForm(this, pluginAdminService);
    body.add(form);
    form.init();
  }

  @Override
  protected String getTitle()
  {
    return getString("system.pluginAdmin.title");
  }

  private void checkAccess()
  {
    accessChecker.checkIsLoggedInUserMemberOfAdminGroup();
    accessChecker.checkRestrictedOrDemoUser();
  }
}
