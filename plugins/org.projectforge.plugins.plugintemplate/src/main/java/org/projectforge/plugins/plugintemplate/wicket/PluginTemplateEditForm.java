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

package org.projectforge.plugins.plugintemplate.wicket;

import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.plugins.plugintemplate.model.PluginTemplateDO;
import org.projectforge.plugins.plugintemplate.service.PluginTemplateService;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.slf4j.Logger;

public class PluginTemplateEditForm extends AbstractEditForm<PluginTemplateDO, PluginTemplateEditPage>
{
  private static final long serialVersionUID = 8746545908106124484L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PluginTemplateEditForm.class);

  @SpringBean
  private PluginTemplateService pluginTemplateService;

  public PluginTemplateEditForm(final PluginTemplateEditPage parentPage, final PluginTemplateDO data)
  {
    super(parentPage, data);
  }

  @Override
  protected void init()
  {
    super.init();

    gridBuilder.newSplitPanel(GridSize.COL100);
    {
      // Key
      final FieldsetPanel fs = gridBuilder.newFieldset(PluginTemplateDO.class, "key");
      MaxLengthTextField titel = new MaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<>(data, "key"));
      titel.setRequired(true);
      fs.add(titel);
    }
    {
      // Value
      final FieldsetPanel fs = gridBuilder.newFieldset(PluginTemplateDO.class, "value");
      MaxLengthTextField titel = new MaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<>(data, "value"));
      fs.add(titel);
    }
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
