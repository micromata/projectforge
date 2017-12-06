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

package org.projectforge.web.humanresources;

import java.util.Date;

import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.business.humanresources.HRFilter;
import org.projectforge.web.calendar.QuickSelectWeekPanel;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.HtmlCommentPanel;
import org.slf4j.Logger;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class HRListForm extends AbstractListForm<HRFilter, HRListPage>
{
  private static final long serialVersionUID = -5511800187080680095L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HRListForm.class);

  protected DatePanel startDate;

  protected DatePanel stopDate;

  @Override
  protected void init()
  {
    super.init();
    final HRFilter filter = getSearchFilter();
    gridBuilder.newGridPanel();
    final FieldsetPanel fs = gridBuilder.newFieldset(super.getOptionsLabel()).suppressLabelForWarning();
    final DivPanel optionsCheckBoxesPanel = fs.addNewCheckBoxButtonDiv();
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(), new PropertyModel<Boolean>(filter, "showPlanning"),
        getString("hr.planning.filter.showPlanning")));
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(), new PropertyModel<Boolean>(filter, "showBookedTimesheets"),
        getString("hr.planning.filter.showBookedTimesheets")));
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(), new PropertyModel<Boolean>(filter, "onlyMyProjects"),
        getString("hr.planning.filter.onlyMyProjects")));
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(), new PropertyModel<Boolean>(filter, "allProjectsGroupedByCustomer"),
        getString("hr.planning.filter.allProjectsGroupedByCustomer")));
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(), new PropertyModel<Boolean>(filter, "otherProjectsGroupedByCustomer"),
        getString("hr.planning.filter.otherProjectsGroupedByCustomer")));
    pageSizeFieldsetPanel.setVisible(false);
    setPageSize(Integer.MAX_VALUE);
  }

  public HRListForm(final HRListPage parentPage)
  {
    super(parentPage);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#isFilterVisible()
   */
  @Override
  protected boolean isFilterVisible()
  {
    return false;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#getOptionsLabel()
   */
  @Override
  protected String getOptionsLabel()
  {
    return getString("timePeriod");
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#onOptionsPanelCreate(org.projectforge.web.wicket.flowlayout.FieldsetPanel,
   *      org.projectforge.web.wicket.flowlayout.DivPanel)
   */
  @SuppressWarnings("serial")
  @Override
  protected void onOptionsPanelCreate(final FieldsetPanel optionsFieldsetPanel, final DivPanel optionsCheckBoxesPanel)
  {
    final HRFilter filter = getSearchFilter();
    startDate = new DatePanel(optionsFieldsetPanel.newChildId(), new PropertyModel<Date>(filter, "startTime"), DatePanelSettings.get()
        .withSelectPeriodMode(true).withRequired(true));
    optionsFieldsetPanel.add(startDate);
    optionsFieldsetPanel.add(new DivTextPanel(optionsFieldsetPanel.newChildId(), " - "));
    stopDate = new DatePanel(optionsFieldsetPanel.newChildId(), new PropertyModel<Date>(filter, "stopTime"), DatePanelSettings.get()
        .withSelectPeriodMode(true).withRequired(true));
    optionsFieldsetPanel.add(stopDate);
    final QuickSelectWeekPanel quickSelectPanel = new QuickSelectWeekPanel(optionsFieldsetPanel.newChildId(), new Model<Date>() {
      @Override
      public Date getObject()
      {
        startDate.validate(); // Update model from form field.
        final Date date = startDate.getConvertedInput();
        return date;
      }
    }, parentPage, "week");
    optionsFieldsetPanel.add(quickSelectPanel);
    quickSelectPanel.init();
    optionsFieldsetPanel.add(new DivTextPanel(optionsFieldsetPanel.newChildId(), new Model<String>() {
      @Override
      public String getObject()
      {
        return WicketUtils.getCalendarWeeks(HRListForm.this, filter.getStartTime(), filter.getStopTime());
      }
    }));
    optionsFieldsetPanel.add(new HtmlCommentPanel(optionsFieldsetPanel.newChildId(), new Model<String>() {
      @Override
      public String getObject()
      {
        return WicketUtils.getUTCDates(filter.getStartTime(), filter.getStopTime());
      }
    }));
  }

  @Override
  protected boolean isSearchFilterVisible()
  {
    return false;
  }

  @Override
  protected HRFilter newSearchFilterInstance()
  {
    return new HRFilter();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#showOptionsPanel()
   */
  @Override
  protected boolean showOptionsPanel()
  {
    return true;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#showHistorySearchAndDeleteCheckbox()
   */
  @Override
  protected boolean showHistorySearchAndDeleteCheckbox()
  {
    return true;
  }
}
