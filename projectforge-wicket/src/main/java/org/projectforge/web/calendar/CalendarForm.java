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

package org.projectforge.web.calendar;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.joda.time.DateMidnight;
import org.projectforge.business.teamcal.filter.CalendarFilter;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.business.teamcal.filter.ICalendarFilter;
import org.projectforge.business.user.UserDao;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.wicket.AbstractStandardForm;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DateTimePanel;
import org.projectforge.web.wicket.components.JodaDatePanel;
import org.projectforge.web.wicket.flowlayout.ButtonGroupPanel;
import org.projectforge.web.wicket.flowlayout.CheckBoxButton;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.IconButtonPanel;
import org.projectforge.web.wicket.flowlayout.IconType;

public class CalendarForm extends AbstractStandardForm<CalendarFilter, CalendarPage>
{
  private static final long serialVersionUID = -145923669780937370L;

  @SpringBean
  protected transient UserDao userDao;

  @SpringBean
  transient AccessChecker accessChecker;

  protected ICalendarFilter filter;

  private JodaDatePanel currentDatePanel;

  private Label durationLabel;

  protected ButtonGroupPanel buttonGroupPanel;

  protected FieldsetPanel fieldset;

  protected CalendarPageSupport createCalendarPageSupport()
  {
    final CalendarPageSupport calendarPageSupport = new CalendarPageSupport(parentPage, accessChecker);
    return calendarPageSupport;
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    gridBuilder.newSplitPanel(GridSize.SPAN8);
    fieldset = gridBuilder.newFieldset(getString("label.options"));
    final CalendarPageSupport calendarPageSupport = createCalendarPageSupport();
    calendarPageSupport.addUserSelectPanel(fieldset, new PropertyModel<PFUserDO>(this, "timesheetsUser"), true);
    currentDatePanel = new JodaDatePanel(fieldset.newChildId(), new PropertyModel<DateMidnight>(filter, "startDate"))
        .setAutosubmit(true);
    currentDatePanel.getDateField().setOutputMarkupId(true);
    fieldset.add(currentDatePanel);

    final DropDownChoice<Integer> firstHourDropDownChoice = new DropDownChoice<Integer>(fieldset.getDropDownChoiceId(),
        new PropertyModel<Integer>(filter, "firstHour"), DateTimePanel.getHourOfDayRenderer().getValues(),
        DateTimePanel.getHourOfDayRenderer())
    {
      /**
       * @see org.apache.wicket.markup.html.form.DropDownChoice#wantOnSelectionChangedNotifications()
       */
      @Override
      protected boolean wantOnSelectionChangedNotifications()
      {
        return true;
      }
    };
    firstHourDropDownChoice.setNullValid(false);
    firstHourDropDownChoice.setRequired(true);
    WicketUtils.addTooltip(firstHourDropDownChoice, getString("calendar.option.firstHour.tooltip"));
    fieldset.add(firstHourDropDownChoice);

    final DivPanel checkBoxPanel = fieldset.addNewCheckBoxButtonDiv();

    calendarPageSupport.addOptions(checkBoxPanel, true, filter);
    checkBoxPanel.add(new CheckBoxButton(checkBoxPanel.newChildId(), new PropertyModel<Boolean>(filter, "slot30"),
        getString("calendar.option.slot30"), true).setTooltip(getString("calendar.option.slot30.tooltip")));

    buttonGroupPanel = new ButtonGroupPanel(fieldset.newChildId());
    fieldset.add(buttonGroupPanel);
    {
      final IconButtonPanel refreshButtonPanel = new IconButtonPanel(buttonGroupPanel.newChildId(), IconType.REFRESH,
          getRefreshIconTooltip())
      {
        /**
         * @see org.projectforge.web.wicket.flowlayout.IconButtonPanel#onSubmit()
         */
        @Override
        protected void onSubmit()
        {
          parentPage.refresh();
          setResponsePage(getPage().getClass(), getPage().getPageParameters());
        }
      };
      buttonGroupPanel.addButton(refreshButtonPanel);
      setDefaultButton(refreshButtonPanel.getButton());
    }
    gridBuilder.newSplitPanel(GridSize.SPAN4);
    final FieldsetPanel fs = gridBuilder.newFieldset(getString("timesheet.duration")).suppressLabelForWarning();
    final DivTextPanel durationPanel = new DivTextPanel(fs.newChildId(),
        new Label(DivTextPanel.WICKET_ID, new Model<String>()
        {
          @Override
          public String getObject()
          {
            return parentPage.calendarPanel.getTotalTimesheetDuration();
          }
        }));
    durationLabel = durationPanel.getLabel4Ajax();
    fs.add(durationPanel);
    onAfterInit(gridBuilder);
  }

  protected String getRefreshIconTooltip()
  {
    return getString("refresh");
  }

  /**
   * Hook method where child implementations could place their logic
   * 
   * @param gridBuilder
   */
  protected void onAfterInit(final GridBuilder gridBuilder)
  {
    // by default nothing happens here
  }

  public CalendarForm(final CalendarPage parentPage)
  {
    super(parentPage);
  }

  public ICalendarFilter getFilter()
  {
    return filter;
  }

  protected void setFilter(final ICalendarFilter filter)
  {
    this.filter = filter;
  }

  public PFUserDO getTimesheetsUser()
  {
    final Integer userId = getFilter().getTimesheetUserId();
    return userId != null ? getTenantRegistry().getUserGroupCache().getUser(userId) : null;
  }

  public void setTimesheetsUser(final PFUserDO user)
  {
    if (user == null) {
      getFilter().setTimesheetUserId(null);
    } else {
      getFilter().setTimesheetUserId(user.getId());
    }
  }

  /**
   * @return the currentDatePanel
   */
  public JodaDatePanel getCurrentDatePanel()
  {
    return currentDatePanel;
  }

  /**
   * @return the durationLabel
   */
  public Label getDurationLabel()
  {
    return durationLabel;
  }

}
