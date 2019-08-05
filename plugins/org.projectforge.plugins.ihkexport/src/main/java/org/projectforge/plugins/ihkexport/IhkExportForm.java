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

package org.projectforge.plugins.ihkexport;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.projectforge.framework.time.TimePeriod;
import org.projectforge.web.CSSColor;
import org.projectforge.web.calendar.QuickSelectWeekPanel;
import org.projectforge.web.wicket.AbstractStandardForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.IconLinkPanel;
import org.projectforge.web.wicket.flowlayout.IconType;

import java.util.Date;

public class IhkExportForm extends AbstractStandardForm<Object, IhkExportPage>
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IhkExportForm.class);

  private TimePeriod timePeriod = new TimePeriod();

  protected DatePanel startDate;

  protected DatePanel stopDate;

  public IhkExportForm(IhkExportPage parentPage)
  {
    super(parentPage);
  }

  @Override
  protected void init()
  {
    super.init();

    gridBuilder.newSplitPanel(GridSize.COL66);
    final FieldsetPanel fs = gridBuilder.newFieldset(getString("timePeriod"));

    startDate = new DatePanel(fs.newChildId(), new PropertyModel<>(timePeriod, "fromDate"),
        DatePanelSettings.get().withSelectPeriodMode(true));
    fs.add(startDate);
    fs.setLabelFor(startDate);
    fs.add(new DivTextPanel(fs.newChildId(), " - "));
    stopDate = new DatePanel(fs.newChildId(), new PropertyModel<>(timePeriod, "toDate"),
        DatePanelSettings.get().withSelectPeriodMode(true));
    fs.add(stopDate);

    {
      final SubmitLink unselectPeriodLink = new SubmitLink(IconLinkPanel.LINK_ID)
      {
        @Override
        public void onSubmit()
        {
          timePeriod.setFromDate(null);
          timePeriod.setToDate(null);
          clearInput();
        }
      };
      unselectPeriodLink.setDefaultFormProcessing(false);
      fs.add(new IconLinkPanel(fs.newChildId(), IconType.REMOVE_SIGN,
          new ResourceModel("calendar.tooltip.unselectPeriod"),
          unselectPeriodLink).setColor(CSSColor.RED));
    }

    final QuickSelectWeekPanel quickSelectWeekPanel = new QuickSelectWeekPanel(fs.newChildId(), new Model<Date>()
    {
      @Override
      public Date getObject()
      {
        startDate.getDateField().validate();
        return startDate.getDateField().getConvertedInput();
      }
    }, parentPage, "quickSelect" + ".week");
    fs.add(quickSelectWeekPanel);
    quickSelectWeekPanel.init();

    fs.add(new DivTextPanel(fs.newChildId(), new Model<String>()
    {
      @Override
      public String getObject()
      {
        return WicketUtils.getCalendarWeeks(IhkExportForm.this, timePeriod.getFromDate(), timePeriod.getToDate());
      }
    }));

    Button downloadButton = new Button(SingleButtonPanel.WICKET_ID, new Model("download"))
    {
      @Override
      public void onSubmit()
      {
        parentPage.export();
      }
    };
    fs.add(new SingleButtonPanel(fs.newChildId(), downloadButton,
        getString("plugins.ihkexport.download"), SingleButtonPanel.DEFAULT_SUBMIT));
  }

  public TimePeriod getTimePeriod()
  {
    return timePeriod;
  }

  public DatePanel getStartDate()
  {
    return startDate;
  }

}
