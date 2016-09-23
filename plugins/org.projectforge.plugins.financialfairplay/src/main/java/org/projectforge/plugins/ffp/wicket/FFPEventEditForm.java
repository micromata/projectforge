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

package org.projectforge.plugins.ffp.wicket;

import org.apache.log4j.Logger;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.plugins.ffp.model.FFPEventDO;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;

public class FFPEventEditForm extends AbstractEditForm<FFPEventDO, FFPEventEditPage>
{
  private static final long serialVersionUID = 8746545908106124484L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(FFPEventEditForm.class);

  public FFPEventEditForm(final FFPEventEditPage parentPage, final FFPEventDO data)
  {
    super(parentPage, data);
  }

  @Override
  protected void init()
  {
    super.init();

    gridBuilder.newSplitPanel(GridSize.COL25, true).newSubSplitPanel(GridSize.COL100);
    {
      // Event date
      final FieldsetPanel fs = gridBuilder.newFieldset(FFPEventDO.class, "eventDate");
      fs.add(new DatePanel(fs.newChildId(), new PropertyModel<>(data, "eventDate"), new DatePanelSettings()));
    }
    {
      // Division
      final FieldsetPanel fs = gridBuilder.newFieldset(FFPEventDO.class, "title");
      MaxLengthTextField titel = new MaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<>(data, "title"));
      titel.setMarkupId("titel").setOutputMarkupId(true);
      fs.add(titel);
    }
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
