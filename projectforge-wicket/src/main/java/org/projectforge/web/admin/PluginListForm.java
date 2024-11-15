/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.model.Model;
import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.core.PluginAdminService;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.wicket.AbstractStandardForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;

import java.util.List;

/**
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
public class PluginListForm extends AbstractStandardForm<PluginListForm, PluginListPage> {
  private static final long serialVersionUID = 5241668711103233356L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PluginListForm.class);

  public PluginListForm(PluginListPage parentPage) {
    super(parentPage);
  }

  @Override
  protected void init() {
    super.init();
    List<AbstractPlugin> availables = WicketSupport.get(PluginAdminService.class).getAvailablePlugins();
    List<String> activatedPlugins = WicketSupport.get(PluginAdminService.class).getActivatedPluginsFromConfiguration();

    gridBuilder.newFormHeading("Please note: (de)activation of plugins will take effect only after restart!");

    for (AbstractPlugin plugin : availables) {
      gridBuilder.newGridPanel();
      gridBuilder.newSplitPanel(GridSize.SPAN2);
      DivPanel section = gridBuilder.getPanel();
      DivTextPanel pluginId = new DivTextPanel(section.newChildId(), new Model<String>() {
        @Override
        public String getObject() {
          return plugin.getInfo().getId();
        }
      });
      section.add(pluginId);
      gridBuilder.newSplitPanel(GridSize.SPAN8);
      section = gridBuilder.getPanel();
      pluginId = new DivTextPanel(section.newChildId(), new Model<String>() {
        @Override
        public String getObject() {
          return plugin.getInfo().getDescription();
        }
      });
      section.add(pluginId);
      gridBuilder.newSplitPanel(GridSize.SPAN2);
      section = gridBuilder.getPanel();
      final Button button = new Button(SingleButtonPanel.WICKET_ID, new Model<String>()) {
        @Override
        public final void onSubmit() {
          WicketSupport.get(PluginAdminService.class).storePluginToBeActivated(plugin.getInfo().getId(), !isActivated(activatedPlugins, plugin));
          setResponsePage(new PluginListPage(getPage().getPageParameters()));
        }
      };
      final SingleButtonPanel buttonPanel = new SingleButtonPanel(section.newChildId(), button,
          isActivated(activatedPlugins, plugin) ? getString("system.pluginAdmin.button.deactivate")
              : getString("system.pluginAdmin.button.activate"),
          SingleButtonPanel.DANGER);
      if (isActivated(activatedPlugins, plugin)) {
        buttonPanel.setClassnames(SingleButtonPanel.SUCCESS);
      }
      section.add(buttonPanel);
    }
  }

  private boolean isActivated(List<String> activatedPlugins, AbstractPlugin plugin) {
    return activatedPlugins.contains(plugin.getInfo().getId());
  }
}
