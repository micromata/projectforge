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

package org.projectforge.web.teamcal.event;

import java.util.Collection;
import java.util.Date;

import org.slf4j.Logger;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.admin.TeamCalsComparator;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.TeamEventFilter;
import org.projectforge.web.CSSColor;
import org.projectforge.web.calendar.QuickSelectPanel;
import org.projectforge.web.common.MultiChoiceListHelper;
import org.projectforge.web.teamcal.admin.TeamCalsProvider;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.HtmlCommentPanel;
import org.projectforge.web.wicket.flowlayout.IconLinkPanel;
import org.projectforge.web.wicket.flowlayout.IconType;
import org.wicketstuff.select2.Select2MultiChoice;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class TeamEventListForm extends AbstractListForm<TeamEventFilter, TeamEventListPage>
{
  private static final long serialVersionUID = 3659495003810851072L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TeamEventListForm.class);

  @SpringBean
  TeamCalCache teamCalCache;

  MultiChoiceListHelper<TeamCalDO> calendarsListHelper;

  protected DatePanel startDate;

  protected DatePanel endDate;

  private final FormComponent<?>[] dependentFormComponents = new FormComponent<?>[2];

  public TeamEventListForm(final TeamEventListPage parentPage)
  {
    super(parentPage);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#newSearchFilterInstance()
   */
  @Override
  protected TeamEventFilter newSearchFilterInstance()
  {
    return new TeamEventFilter();
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#init()
   */
  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    getParentPage().onFormInit();
    add(new IFormValidator()
    {
      @Override
      public FormComponent<?>[] getDependentFormComponents()
      {
        return dependentFormComponents;
      }

      @Override
      public void validate(final Form<?> form)
      {
        if (parentPage.isMassUpdateMode() == false) {
          final Date from = startDate.getConvertedInput();
          final Date to = endDate.getConvertedInput();
          if (from != null && to != null && from.after(to) == true) {
            error(getString("timePeriodPanel.startTimeAfterStopTime"));
          }
        }
      }
    });
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("templates")).suppressLabelForWarning();
      fs.add(new SingleButtonPanel(fs.newChildId(), new Button(SingleButtonPanel.WICKET_ID, new Model<String>("all"))
      {
        @Override
        public final void onSubmit()
        {
          final Collection<TeamCalDO> assignedItems = teamCalCache.getAllAccessibleCalendars();
          calendarsListHelper.setAssignedItems(assignedItems);
        }
      }, getString("selectAll"), SingleButtonPanel.NORMAL));
      fs.add(new SingleButtonPanel(fs.newChildId(), new Button(SingleButtonPanel.WICKET_ID, new Model<String>("own"))
      {
        @Override
        public final void onSubmit()
        {
          final Collection<TeamCalDO> assignedItems = teamCalCache.getAllOwnCalendars();
          calendarsListHelper.setAssignedItems(assignedItems);
        }
      }, getString("plugins.teamcal.own"), SingleButtonPanel.NORMAL));
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#onOptionsPanelCreate(org.projectforge.web.wicket.flowlayout.FieldsetPanel,
   * org.projectforge.web.wicket.flowlayout.DivPanel)
   */
  @SuppressWarnings("serial")
  @Override
  protected void onOptionsPanelCreate(final FieldsetPanel optionsFieldsetPanel, final DivPanel optionsCheckBoxesPanel)
  {
    {
      optionsFieldsetPanel.setOutputMarkupId(true);
      startDate = new DatePanel(optionsFieldsetPanel.newChildId(),
          new PropertyModel<Date>(getSearchFilter(), "startDate"),
          DatePanelSettings.get().withSelectPeriodMode(true));
      optionsFieldsetPanel.add(dependentFormComponents[0] = startDate);
      optionsFieldsetPanel.setLabelFor(startDate);
      optionsFieldsetPanel.add(new DivTextPanel(optionsFieldsetPanel.newChildId(), " - "));
      endDate = new DatePanel(optionsFieldsetPanel.newChildId(), new PropertyModel<Date>(getSearchFilter(), "endDate"),
          DatePanelSettings
              .get().withSelectPeriodMode(true));
      optionsFieldsetPanel.add(dependentFormComponents[1] = endDate);
      {
        final SubmitLink unselectPeriod = new SubmitLink(IconLinkPanel.LINK_ID)
        {
          @Override
          public void onSubmit()
          {
            getSearchFilter().setStartDate(null);
            getSearchFilter().setEndDate(null);
            clearInput();
            parentPage.refresh();
          }

        };
        unselectPeriod.setDefaultFormProcessing(false);
        optionsFieldsetPanel
            .add(new IconLinkPanel(optionsFieldsetPanel.newChildId(), IconType.REMOVE_SIGN, new ResourceModel(
                "calendar.tooltip.unselectPeriod"), unselectPeriod).setColor(CSSColor.RED));
      }
      final QuickSelectPanel quickSelectPanel = new QuickSelectPanel(optionsFieldsetPanel.newChildId(), parentPage,
          "quickSelect",
          startDate);
      optionsFieldsetPanel.add(quickSelectPanel);
      quickSelectPanel.init();
      optionsFieldsetPanel.add(new HtmlCommentPanel(optionsFieldsetPanel.newChildId(), new Model<String>()
      {
        @Override
        public String getObject()
        {
          return WicketUtils.getUTCDates(getSearchFilter().getStartDate(), getSearchFilter().getEndDate());
        }
      }));
    }
    {
      // Team calendar
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.teamcal.calendar"));// .setLabelSide(false);
      final TeamCalsProvider calendarProvider = new TeamCalsProvider(teamCalCache);
      calendarsListHelper = new MultiChoiceListHelper<TeamCalDO>().setComparator(new TeamCalsComparator()).setFullList(
          calendarProvider.getSortedCalenders());
      final Collection<Integer> list = getFilter().getTeamCals();
      if (list != null) {
        for (final Integer calId : list) {
          final TeamCalDO cal = teamCalCache.getCalendar(calId);
          calendarsListHelper.addOriginalAssignedItem(cal).assignItem(cal);
        }
      }
      final Select2MultiChoice<TeamCalDO> calendars = new Select2MultiChoice<TeamCalDO>(fs.getSelect2MultiChoiceId(),
          new PropertyModel<Collection<TeamCalDO>>(this.calendarsListHelper, "assignedItems"), calendarProvider);
      fs.add(calendars);
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#getLogger()
   */
  @Override
  protected Logger getLogger()
  {
    return log;
  }

  /**
   * @return the filter
   */
  public TeamEventFilter getFilter()
  {
    return getSearchFilter();
  }
}
