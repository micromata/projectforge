/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.TeamEventDao;
import org.projectforge.business.teamcal.event.TeamEventFilter;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.teamcal.admin.TeamCalsProvider;
import org.projectforge.web.wicket.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@ListPage(editPage = TeamEventEditPage.class)
public class TeamEventListPage extends AbstractListPage<TeamEventListForm, TeamEventDao, TeamEventDO> implements
        IListPageColumnsCreator<TeamEventDO> {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TeamEventListPage.class);

  public static final String PARAM_CALENDARS = "cals";

  private static final long serialVersionUID = 1749480610890950450L;

  /**
   *
   */
  public TeamEventListPage(final PageParameters parameters) {
    super(parameters, "plugins.teamcal.event");
  }

  protected void onFormInit() {
    final String str = WicketUtils.getAsString(getPageParameters(), PARAM_CALENDARS);
    if (StringUtils.isNotBlank(str) == true) {
      final Collection<TeamCalDO> teamCals = new TeamCalsProvider(WicketSupport.get(TeamCalCache.class)).getSortedCalendars(str);
      getFilter().setTeamCals(getCalIdList(teamCals));
    }
  }

  private List<Long> getCalIdList(final Collection<TeamCalDO> teamCals) {
    final List<Long> list = new ArrayList<Long>();
    if (teamCals != null) {
      for (final TeamCalDO cal : teamCals) {
        list.add(cal.getId());
      }
    }
    return list;
  }

  /**
   * @see org.projectforge.web.wicket.IListPageColumnsCreator#createColumns(org.apache.wicket.markup.html.WebPage,
   * boolean)
   */
  @SuppressWarnings("serial")
  @Override
  public List<IColumn<TeamEventDO, String>> createColumns(final WebPage returnToPage, final boolean sortable) {
    final List<IColumn<TeamEventDO, String>> columns = new ArrayList<IColumn<TeamEventDO, String>>();

    final CellItemListener<TeamEventDO> cellItemListener = new CellItemListener<TeamEventDO>() {
      @Override
      public void populateItem(final Item<ICellPopulator<TeamEventDO>> item, final String componentId,
                               final IModel<TeamEventDO> rowModel) {
        final TeamEventDO teamEvent = rowModel.getObject();
        appendCssClasses(item, teamEvent.getId(), teamEvent.getDeleted());
      }
    };

    columns.add(new CellItemListenerPropertyColumn<TeamEventDO>(getString("plugins.teamcal.calendar"), null, "calendar",
            cellItemListener) {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<TeamEventDO>> item, final String componentId,
                               final IModel<TeamEventDO> rowModel) {
        final TeamEventDO teamEvent = rowModel.getObject();
        final TeamCalDO calendar = teamEvent.getCalendar();
        item.add(
                new ListSelectActionPanel(componentId, rowModel, TeamEventEditPage.class, teamEvent.getId(), returnToPage,
                        calendar != null ? calendar.getTitle() : ""));
        cellItemListener.populateItem(item, componentId, rowModel);
        addRowClick(item);
      }
    });

    columns.add(new CellItemListenerPropertyColumn<TeamEventDO>(getString("plugins.teamcal.event.subject"),
            getSortable("subject", sortable),
            "subject", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<TeamEventDO>(getString("plugins.teamcal.event.beginDate"),
            getSortable("startDate", sortable), "startDate", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<TeamEventDO>(getString("plugins.teamcal.event.endDate"),
            getSortable("endDate", sortable),
            "endDate", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<TeamEventDO>(getString("plugins.teamcal.event.allDay"),
            getSortable("allDay", sortable),
            "allDay", cellItemListener) {
      @Override
      public void populateItem(final Item<ICellPopulator<TeamEventDO>> item, final String componentId,
                               final IModel<TeamEventDO> rowModel) {
        final TeamEventDO event = rowModel.getObject();
        item.add(WicketUtils.createBooleanLabel(getRequestCycle(), componentId, event.getAllDay() == true));
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    return columns;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#onSearchSubmit()
   */
  @Override
  protected boolean onSearchSubmit() {
    getFilter().setTeamCals(getCalIdList(form.calendarsListHelper.getAssignedItems()));
    return super.onSearchSubmit();
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#select(java.lang.String, java.lang.Object)
   */
  @Override
  public void select(final String property, final Object selectedValue) {
    if (property.startsWith("quickSelect.") == true) { // month".equals(property) == true) {
      PFDateTime date = PFDateTime.fromOrNullAny(selectedValue);
      if (date != null) {
        form.getSearchFilter().setStartDate(date.getUtilDate());
        if (property.endsWith(".month") == true) {
          date = date.getEndOfMonth();
        } else if (property.endsWith(".week") == true) {
          date = date.getEndOfWeek();
        } else {
          log.error("Property '" + property + "' not supported for selection.");
        }
        form.getSearchFilter().setEndDate(date.getUtilDate());
        form.startDate.markModelAsChanged();
        form.endDate.markModelAsChanged();
        refresh();
      }
    } else {
      super.select(property, selectedValue);
    }
  }

  protected TeamEventFilter getFilter() {
    return form.getFilter();
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#getBaseDao()
   */
  @Override
  public TeamEventDao getBaseDao() {
    return WicketSupport.get(TeamEventDao.class);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#newListForm(org.projectforge.web.wicket.AbstractListPage)
   */
  @Override
  protected TeamEventListForm newListForm(final AbstractListPage<?, ?, ?> parentPage) {
    return new TeamEventListForm(this);
  }

  @Override
  protected void init() {
    dataTable = createDataTable(createColumns(this, true), "lastUpdate", SortOrder.DESCENDING);
    form.add(dataTable);
  }
}
