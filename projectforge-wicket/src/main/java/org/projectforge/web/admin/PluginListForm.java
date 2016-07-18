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

import java.util.List;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.model.Model;
import org.projectforge.plugins.core.AvailablePlugin;
import org.projectforge.plugins.core.PluginAdminService;
import org.projectforge.web.wicket.AbstractStandardForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;

/**
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class PluginListForm extends AbstractStandardForm<PluginListForm, PluginListPage>
{
  private static final long serialVersionUID = 5241668711103233356L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(PluginListForm.class);
  private PluginAdminService pluginAdminService;

  public PluginListForm(PluginListPage parentPage, PluginAdminService pluginAdminService)
  {
    super(parentPage);
    this.pluginAdminService = pluginAdminService;
  }

  @Override
  protected void init()
  {
    super.init();
    List<AvailablePlugin> availabl = pluginAdminService.getAvailablePlugins();

    for (AvailablePlugin pp : availabl) {
      gridBuilder.newGridPanel();
      gridBuilder.newSplitPanel(GridSize.SPAN2);
      DivPanel section = gridBuilder.getPanel();
      DivTextPanel pluginId = new DivTextPanel(section.newChildId(), new Model<String>()
      {
        @Override
        public String getObject()
        {
          return pp.getProjectForgePluginService().getPluginId();
        }
      });
      section.add(pluginId);
      gridBuilder.newSplitPanel(GridSize.SPAN8);
      section = gridBuilder.getPanel();
      pluginId = new DivTextPanel(section.newChildId(), new Model<String>()
      {
        @Override
        public String getObject()
        {
          return pp.getProjectForgePluginService().getPluginDescription();
        }
      });
      section.add(pluginId);
      gridBuilder.newSplitPanel(GridSize.SPAN2);
      section = gridBuilder.getPanel();
      final Button button = new Button(SingleButtonPanel.WICKET_ID, new Model<String>())
      {
        @Override
        public final void onSubmit()
        {
          pluginAdminService.storePluginToBeActivated(pp.getProjectForgePluginService().getPluginId(),
              pp.isActivated() == false);
          setResponsePage(new PluginListPage(getPage().getPageParameters()));
        }
      };
      final SingleButtonPanel buttonPanel = new SingleButtonPanel(section.newChildId(), button,
          pp.isActivated() ? getString("system.pluginAdmin.button.deactivate")
              : getString("system.pluginAdmin.button.activate"),
          SingleButtonPanel.DANGER);
      if (pp.isActivated() == true) {
        buttonPanel.setClassnames(SingleButtonPanel.SUCCESS);
      }
      if (pp.isBuildIn() == true) {
        buttonPanel.setEnabled(false);
        button.setEnabled(false);
      }
      section.add(buttonPanel);
    }
  }
}