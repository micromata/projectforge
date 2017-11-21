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

package org.projectforge.web.core;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.framework.persistence.jpa.impl.HibernateSearchFilterUtils;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DatePrecision;
import org.projectforge.registry.Registry;
import org.projectforge.registry.RegistryEntry;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.AbstractStandardForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.CheckBoxButton;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.HtmlCommentPanel;

public class SearchForm extends AbstractStandardForm<SearchPageFilter, SearchPage>
{
  private static final String USER_PREF_KEY_FILTER = "Search:Filter";

  private static final int MAGIC_NUMBER_LAST_DAYS_FOR_WITHOUT_TIME_SETTINGS = 42;

  private static final long serialVersionUID = 2638309407446431727L;

  SearchPageFilter filter;

  private Label modifiedSearchExpressionLabel;

  final static int MIN_PAGE_SIZE = 3;

  final static int MAX_PAGE_SIZE = 50;

  public SearchForm(final SearchPage parentPage)
  {
    this(parentPage, null);
  }

  public SearchForm(final SearchPage parentPage, final String searchString)
  {
    super(parentPage);
    filter = (SearchPageFilter) getParentPage().getUserPrefEntry(SearchPageFilter.class, USER_PREF_KEY_FILTER);
    if (filter == null) {
      filter = new SearchPageFilter();
      getParentPage().putUserPrefEntry(USER_PREF_KEY_FILTER, filter, true);
    }
    if (filter.getArea() == null) {
      filter.setArea(SearchPageFilter.ALL);
    }
    if (StringUtils.isNotBlank(searchString) == true) {
      filter.setSearchString(HibernateSearchFilterUtils.modifySearchString(searchString, true));
      filter.setModifiedByUser(null);
      filter.setStartTimeOfModification(null);
      filter.setStopTimeOfModification(null);
      filter.setSearchHistory(false);
    }
  }

  @Override
  @SuppressWarnings("serial")
  protected void init()
  {
    super.init();
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("searchFilter"));
      final TextField<String> searchField = new TextField<String>(fs.getTextFieldId(),
          new PropertyModel<String>(filter, "searchString"));
      WicketUtils.setFocus(searchField);
      fs.add(searchField);
      final Model<String> modifiedSearchExpressionModel = new Model<String>()
      {
        @Override
        public String getObject()
        {
          return AbstractListForm.getModifiedSearchExpressionLabel(SearchForm.this, filter.getSearchString());
        }
      };
      final DivPanel div = new DivPanel(fs.newChildId());
      div.add(AttributeModifier.append("class", "modifiedSearchExpressionLabel"));
      fs.add(div);
      modifiedSearchExpressionLabel = new Label(div.newChildId(), modifiedSearchExpressionModel)
      {
        @Override
        public boolean isVisible()
        {
          return StringUtils.isNotBlank(filter.getSearchString()) == true;
        }
      };
      modifiedSearchExpressionLabel.setEscapeModelStrings(false);
      div.add(modifiedSearchExpressionLabel);
    }
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("label.options")).suppressLabelForWarning();
      final DivPanel checkBoxButton = fs.addNewCheckBoxButtonDiv();
      checkBoxButton
          .add(new CheckBoxButton(checkBoxButton.newChildId(), new PropertyModel<Boolean>(filter, "searchHistory"),
              getString("search.searchHistory")).setTooltip(getString("search.searchHistory.additional.tooltip")));
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("search.area"));
      // DropDownChoice: area
      final LabelValueChoiceRenderer<String> areaChoiceRenderer = new LabelValueChoiceRenderer<String>();
      for (final RegistryEntry entry : Registry.getInstance().getOrderedList()) {
        if (isSearchable(entry) == true) {
          areaChoiceRenderer.addValue(entry.getId(), getString(entry.getI18nTitleHeading()));
        }
      }
      areaChoiceRenderer.sortLabels();
      areaChoiceRenderer.addValue(0, SearchPageFilter.ALL, getString("filter.all"));
      final DropDownChoice<String> areaChoice = new DropDownChoice<String>(fs.getDropDownChoiceId(),
          new PropertyModel<String>(filter,
              "area"),
          areaChoiceRenderer.getValues(), areaChoiceRenderer);
      areaChoice.setRequired(false);
      fs.add(areaChoice);
    }
    {
      // DropDownChoice pageSize
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("label.pageSize"));
      final DropDownChoice<Integer> pageSizeChoice = AbstractListForm.getPageSizeDropDownChoice(
          fs.getDropDownChoiceId(), getLocale(),
          new PropertyModel<Integer>(filter, "maxRows"), MIN_PAGE_SIZE, MAX_PAGE_SIZE);
      fs.add(pageSizeChoice);
    }

    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("search.periodOfModification"));
      final DatePanel modifiedStartDatePanel = new DatePanel(fs.newChildId(),
          new PropertyModel<Date>(filter, "startTimeOfModification"),
          DatePanelSettings.get().withSelectPeriodMode(true));
      fs.add(modifiedStartDatePanel);
      fs.add(new DivTextPanel(fs.newChildId(), " - "));
      final DatePanel modifiedStopDatePanel = new DatePanel(fs.newChildId(),
          new PropertyModel<Date>(filter, "stopTimeOfModification"),
          DatePanelSettings.get().withSelectPeriodMode(true));
      fs.add(modifiedStopDatePanel);
      fs.add(new HtmlCommentPanel(fs.newChildId(), new Model<String>()
      {
        @Override
        public String getObject()
        {
          return WicketUtils.getUTCDates(filter.getStartTimeOfModification(), filter.getStopTimeOfModification());
        }
      }));
      // DropDownChoice: time period
      final LabelValueChoiceRenderer<Integer> lastDaysChoiceRenderer = new LabelValueChoiceRenderer<Integer>();
      lastDaysChoiceRenderer.addValue(MAGIC_NUMBER_LAST_DAYS_FOR_WITHOUT_TIME_SETTINGS,
          getString("search.withoutTimePeriod"));
      lastDaysChoiceRenderer.addValue(0, getString("search.today"));
      lastDaysChoiceRenderer.addValue(1, getString("search.lastDay"));
      for (final int days : new int[] { 3, 7, 14, 30, 60, 90 }) {
        lastDaysChoiceRenderer.addValue(days, getLocalizedMessage("search.lastDays", days));
      }
      final DropDownChoice<Integer> lastDaysChoice = new DropDownChoice<Integer>(fs.getDropDownChoiceId(),
          new PropertyModel<Integer>(
              filter, "lastDays"),
          lastDaysChoiceRenderer.getValues(), lastDaysChoiceRenderer)
      {
        @Override
        protected void onSelectionChanged(final Integer newSelection)
        {
          if (newSelection == null) {
            return;
          }
          if (newSelection == MAGIC_NUMBER_LAST_DAYS_FOR_WITHOUT_TIME_SETTINGS) { // Magic number for
            filter.setStartTimeOfModification(null);
            filter.setStopTimeOfModification(null);
            modifiedStartDatePanel.markModelAsChanged();
            modifiedStopDatePanel.markModelAsChanged();
            filter.setLastDays(null);
            return;
          }
          final DateHolder dh = new DateHolder(new Date(), DatePrecision.MILLISECOND);
          dh.setEndOfDay();
          filter.setStopTimeOfModification(dh.getDate());
          dh.setBeginOfDay();
          dh.add(Calendar.DAY_OF_YEAR, -newSelection);
          filter.setStartTimeOfModification(dh.getDate());
          filter.setLastDays(null);
          modifiedStartDatePanel.markModelAsChanged();
          modifiedStopDatePanel.markModelAsChanged();
        }

        @Override
        protected boolean wantOnSelectionChangedNotifications()
        {
          return true;
        }
      };
      lastDaysChoice.setNullValid(true);
      lastDaysChoice.setRequired(false);
      fs.add(lastDaysChoice);
    }
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("modifiedBy"));
      final UserSelectPanel userSelectPanel = new UserSelectPanel(fs.newChildId(),
          new PropertyModel<PFUserDO>(filter, "modifiedByUser"),
          parentPage, "userId");
      fs.add(userSelectPanel);
      userSelectPanel.init().withAutoSubmit(true);
    }
    {
      final Button resetButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("reset"))
      {
        @Override
        public final void onSubmit()
        {
          filter.reset();
        }
      };
      resetButton.setDefaultFormProcessing(false);
      final SingleButtonPanel resetButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), resetButton,
          getString("reset"),
          SingleButtonPanel.RESET);
      actionButtons.add(resetButtonPanel);

      final Button searchButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("search"));
      final SingleButtonPanel sendButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), searchButton,
          getString("search"),
          SingleButtonPanel.DEFAULT_SUBMIT);
      actionButtons.add(sendButtonPanel);
      setDefaultButton(searchButton);
    }
  }

  public static boolean isSearchable(final RegistryEntry entry)
  {
    return entry.getDao().isHistorizable() == true
        && entry.getDao().hasLoggedInUserHistoryAccess(false) == true
        && entry.isSearchable() == true;
  }
}
