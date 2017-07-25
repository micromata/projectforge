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

package org.projectforge.web.teamcal.event.importics;

import java.util.List;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.teamcal.admin.TeamCalDao;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.web.core.importstorage.AbstractImportForm;
import org.projectforge.web.core.importstorage.ImportFilter;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.FileUploadPanel;

public class TeamCalImportForm extends AbstractImportForm<ImportFilter, TeamCalImportPage, TeamCalImportStoragePanel>
{
  private static final long serialVersionUID = -4812284533159635654L;

  protected FileUploadField fileUploadField;

  protected TeamCalDO calendar;

  //  private ModalDialog modalDialog;

  @SpringBean
  private TeamCalDao teamCalDao;

  public TeamCalImportForm(final TeamCalImportPage parentPage)
  {
    super(parentPage);
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();

    //    createModalDialog(parentPage);

    gridBuilder.newGridPanel();
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.teamcal.event.teamCal"));
      final List<TeamCalDO> list = teamCalDao.getAllCalendarsWithFullAccess();
      final LabelValueChoiceRenderer<TeamCalDO> calChoiceRenderer = new LabelValueChoiceRenderer<TeamCalDO>();
      for (final TeamCalDO cal : list) {
        calChoiceRenderer.addValue(cal, cal.getTitle());
      }
      final DropDownChoice<TeamCalDO> calDropDownChoice = new DropDownChoice<TeamCalDO>(fs.getDropDownChoiceId(),
          new PropertyModel<TeamCalDO>(this, "calendar"), calChoiceRenderer.getValues(), calChoiceRenderer);
      calDropDownChoice.add(new FormComponentUpdatingBehavior()
      {
        @Override
        public void onUpdate()
        {
          parentPage.reconcile();
        }
      });
      calDropDownChoice.setNullValid(false);
      calDropDownChoice.setRequired(true);
      fs.add(calDropDownChoice);
    }
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("file"), "*.ics");
      fileUploadField = new FileUploadField(FileUploadPanel.WICKET_ID);
      fs.add(new FileUploadPanel(fs.newChildId(), fileUploadField));
      fs.add(new SingleButtonPanel(fs.newChildId(),
          new Button(SingleButtonPanel.WICKET_ID, new Model<String>("importEvents"))
          {
            @Override
            public final void onSubmit()
            {
              parentPage.importEvents();
            }
          }, getString("upload"), SingleButtonPanel.NORMAL).setTooltip(getString("common.import.upload.tooltip")));
      addClearButton(fs);
    }
    {
      addImportFilterRadio(gridBuilder);
    }
    gridBuilder.newGridPanel();
    final DivPanel panel = gridBuilder.getPanel();
    storagePanel = new TeamCalImportStoragePanel(panel.newChildId(), parentPage, importFilter);
    panel.add(storagePanel);
  }

  protected Integer getCalendarId()
  {
    return calendar != null ? calendar.getId() : null;
  }
}
