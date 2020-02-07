/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.business.humanresources.HRFilter;
import org.projectforge.framework.time.PFDay;
import org.projectforge.web.calendar.QuickSelectWeekPanel;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.LocalDateModel;
import org.projectforge.web.wicket.components.LocalDatePanel;
import org.projectforge.web.wicket.flowlayout.*;
import org.slf4j.Logger;

import java.time.LocalDate;
import java.util.Date;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class HRListForm extends AbstractListForm<HRFilter, HRListPage>
{
  private static final long serialVersionUID = -5511800187080680095L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HRListForm.class);

  protected LocalDatePanel startDate;

  protected LocalDatePanel stopDate;

  @Override
  protected void init()
  {
    super.init();
    final HRFilter filter = getSearchFilter();
    gridBuilder.newGridPanel();
    final FieldsetPanel fs = gridBuilder.newFieldset(super.getOptionsLabel()).suppressLabelForWarning();
    final DivPanel optionsCheckBoxesPanel = fs.addNewCheckBoxButtonDiv();
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(), new PropertyModel<>(filter, "showPlanning"),
        getString("hr.planning.filter.showPlanning")));
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(), new PropertyModel<>(filter, "showBookedTimesheets"),
        getString("hr.planning.filter.showBookedTimesheets")));
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(), new PropertyModel<>(filter, "onlyMyProjects"),
        getString("hr.planning.filter.onlyMyProjects")));
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(), new PropertyModel<>(filter, "allProjectsGroupedByCustomer"),
        getString("hr.planning.filter.allProjectsGroupedByCustomer")));
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(), new PropertyModel<>(filter, "otherProjectsGroupedByCustomer"),
        getString("hr.planning.filter.otherProjectsGroupedByCustomer")));
    pageSizeFieldsetPanel.setVisible(false);
    filter.setMaxRows(-1);
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
    FieldProperties<LocalDate> props = getStartDateProperties(filter);
    startDate = new LocalDatePanel(optionsFieldsetPanel.newChildId(), new LocalDateModel(props.getModel()), DatePanelSettings.get().withSelectPeriodMode(true), true);
    startDate.setRequired(true);
    optionsFieldsetPanel.add(startDate);
    optionsFieldsetPanel.add(new DivTextPanel(optionsFieldsetPanel.newChildId(), " - "));
    props = getStopDateProperties(filter);
    stopDate = new LocalDatePanel(optionsFieldsetPanel.newChildId(), new LocalDateModel(props.getModel()), DatePanelSettings.get().withSelectPeriodMode(true), true);
    stopDate.setRequired(true);
    optionsFieldsetPanel.add(stopDate);
    final QuickSelectWeekPanel quickSelectPanel = new QuickSelectWeekPanel(optionsFieldsetPanel.newChildId(), new Model<LocalDate>() {
      @Override
      public LocalDate getObject() {
        startDate.validate(); // Update model from form field.
        final Date date = startDate.getConvertedInput();
        return PFDay.from(date).getLocalDate();
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
      public String getObject() {
        return WicketUtils.getUTCDates(filter.getStartTime(), filter.getStopTime());
      }
    }));
  }

  private FieldProperties<LocalDate> getStopDateProperties(HRFilter filter) {
    return new FieldProperties<>("", new PropertyModel<>(filter, "stopTime"));
  }

  private FieldProperties<LocalDate> getStartDateProperties(HRFilter filter) {
    return new FieldProperties<>("", new PropertyModel<>(filter, "startTime"));
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
