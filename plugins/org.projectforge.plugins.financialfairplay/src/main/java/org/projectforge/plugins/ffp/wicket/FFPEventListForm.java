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

package org.projectforge.plugins.ffp.wicket;

import org.apache.wicket.model.PropertyModel;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.slf4j.Logger;

public class FFPEventListForm extends AbstractListForm<FFPEventFilter, FFPEventListPage>
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FFPEventListForm.class);

  private static final long serialVersionUID = -5969136444233092172L;

  public FFPEventListForm(final FFPEventListPage parentPage)
  {
    super(parentPage);
  }

  @Override
  protected void onOptionsPanelCreate(FieldsetPanel optionsFieldsetPanel, DivPanel optionsCheckBoxesPanel) {
     FFPEventFilter filter = getSearchFilter();
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(),
        new PropertyModel<>(filter, "showOnlyActiveEntries"), getString("plugins.ffp.event.options.showOnlyActiveEntries")));
	}
  
  @Override
  protected Logger getLogger()
  {
    return log;
  }

  @Override
  protected FFPEventFilter newSearchFilterInstance()
  {
    return new FFPEventFilter();
  }
}
