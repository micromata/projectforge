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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.humanresources.HRViewDao;
import org.projectforge.business.humanresources.HRFilter;
import org.projectforge.business.humanresources.HRPlanningDO;
import org.projectforge.business.humanresources.HRPlanningEntryDO;
import org.projectforge.business.humanresources.HRViewData;
import org.projectforge.business.humanresources.HRViewUserData;
import org.projectforge.business.humanresources.HRViewUserEntryData;
import org.projectforge.business.user.UserFormatter;
import org.projectforge.framework.persistence.api.ReindexSettings;
import org.projectforge.framework.persistence.database.DatabaseDao;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.utils.NumberFormatter;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.timesheet.TimesheetListPage;
import org.projectforge.web.user.UserPropertyColumn;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;
import org.projectforge.web.wicket.WebConstants;
import org.projectforge.web.wicket.flowlayout.DivPanel;

/**
 *
 * @author Mario Groß (m.gross@micromata.de)
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
@ListPage(editPage = HRPlanningEditPage.class)
public class HRListPage extends AbstractListPage<HRListForm, HRViewDao, HRViewUserData> implements ISelectCallerPage
{
  private static final long serialVersionUID = -718881597957595460L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HRListPage.class);

  @SpringBean
  private HRViewDao hrViewDao;

  @SpringBean
  private DatabaseDao databaseDao;

  @SpringBean
  private UserFormatter userFormatter;

  private HRViewData hrViewData;

  private HRListResourceLinkPanel resourceLinkPanel;

  private Long weekMillis;

  public HRListPage(final PageParameters parameters)
  {
    super(parameters, "hr.planning");
  }

  @Override
  protected void init()
  {
    recreateDataTable();
    recreateBottomPanel();
  }

  @SuppressWarnings("serial")
  private void recreateDataTable()
  {
    final Date date = form.getSearchFilter().getStartTime();
    weekMillis = date != null ? date.getTime() : null;
    if (dataTable != null) {
      form.remove(dataTable);
    }
    final List<IColumn<HRViewUserData, String>> columns = new ArrayList<IColumn<HRViewUserData, String>>();
    final CellItemListener<HRViewUserData> cellItemListener = new CellItemListener<HRViewUserData>()
    {
      @Override
      public void populateItem(final Item<ICellPopulator<HRViewUserData>> item, final String componentId,
          final IModel<HRViewUserData> rowModel)
      {
        final HRViewUserData entry = rowModel.getObject();
        appendCssClasses(item, entry.getPlanningId(), entry.isDeleted());
      }
    };
    columns.add(new UserPropertyColumn<HRViewUserData>(getUserGroupCache(), getString("timesheet.user"),
        "user.fullname", "user",
        cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<HRViewUserData>> item, final String componentId,
          final IModel<HRViewUserData> rowModel)
      {
        final Integer planningId = rowModel.getObject().getPlanningId();
        final String[] params;
        if (planningId == null) {
          // Preset fields for adding new entry:
          final Integer userId = rowModel.getObject().getUserId();
          params = new String[] { WebConstants.PARAMETER_USER_ID, userId != null ? String.valueOf(userId) : null,
              WebConstants.PARAMETER_DATE, weekMillis != null ? String.valueOf(weekMillis) : null };
        } else {
          params = null;
        }
        item.add(new ListSelectActionPanel(componentId, rowModel, HRPlanningEditPage.class, planningId, HRListPage.this,
            getLabelString(rowModel), params));
        cellItemListener.populateItem(item, componentId, rowModel);
        addRowClick(item);
      }
    }.withUserFormatter(userFormatter));
    columns.add(new CellItemListenerPropertyColumn<HRViewUserData>(getString("sum"), "plannedDaysSum", "plannedDaysSum",
        cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<HRViewUserData>> item, final String componentId,
          final IModel<HRViewUserData> rowModel)
      {
        final HRViewUserData userData = rowModel.getObject();
        final HRFilter filter = form.getSearchFilter();
        addListEntry(item, componentId, userData.getPlannedDaysSum(), userData.getActualDaysSum(),
            new Link<Object>("actualDaysLink")
            {
              @Override
              public void onClick()
              {
                // Redirect to time sheet list page and show the corresponding time sheets.
                final PageParameters parameters = new PageParameters();
                parameters.add(TimesheetListPage.PARAMETER_KEY_STORE_FILTER, false);
                parameters.add(TimesheetListPage.PARAMETER_KEY_START_TIME, filter.getStartTime().getTime());
                parameters.add(TimesheetListPage.PARAMETER_KEY_STOP_TIME, filter.getStopTime().getTime());
                parameters.add(TimesheetListPage.PARAMETER_KEY_USER_ID, userData.getUserId());
                final TimesheetListPage timesheetListPage = new TimesheetListPage(parameters);
                setResponsePage(timesheetListPage);
              }
            });
        item.add(AttributeModifier.append("style", new Model<String>("text-align: right;")));
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<HRViewUserData>(getString("rest"), "plannedDaysRestSum",
        "plannedDaysRestSum",
        cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<HRViewUserData>> item, final String componentId,
          final IModel<HRViewUserData> rowModel)
      {
        final HRViewUserData userData = rowModel.getObject();
        addLabel(item, componentId, userData.getPlannedDaysRestSum(), userData.getActualDaysRestSum());
        item.add(AttributeModifier.append("style", new Model<String>("text-align: right;")));
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    for (final ProjektDO project : getHRViewData().getProjects()) {
      columns
          .add(new CellItemListenerPropertyColumn<HRViewUserData>(project.getProjektIdentifierDisplayName(), null, null,
              cellItemListener)
          {
            @Override
            public void populateItem(final Item<ICellPopulator<HRViewUserData>> item, final String componentId,
                final IModel<HRViewUserData> rowModel)
            {
              cellItemListener.populateItem(item, componentId, rowModel);
              final HRViewUserData userData = rowModel.getObject();
              final HRViewUserEntryData entry = userData.getEntry(project);
              if (entry == null) {
                item.add(createInvisibleDummyComponent(componentId));
                return;
              }
              final HRFilter filter = form.getSearchFilter();
              addListEntry(item, componentId, entry.getPlannedDays(), entry.getActualDays(),
                  new Link<Object>("actualDaysLink")
                  {
                    @Override
                    public void onClick()
                    {
                      // Redirect to time sheet list page and show the corresponding time sheets.
                      final PageParameters parameters = new PageParameters();
                      parameters.add(TimesheetListPage.PARAMETER_KEY_STORE_FILTER, false);
                      parameters.add(TimesheetListPage.PARAMETER_KEY_TASK_ID, project.getTaskId());
                      parameters.add(TimesheetListPage.PARAMETER_KEY_START_TIME, filter.getStartTime().getTime());
                      parameters.add(TimesheetListPage.PARAMETER_KEY_STOP_TIME, filter.getStopTime().getTime());
                      parameters.add(TimesheetListPage.PARAMETER_KEY_USER_ID, userData.getUserId());
                      final TimesheetListPage timesheetListPage = new TimesheetListPage(parameters);
                      setResponsePage(timesheetListPage);
                    }
                  });
              item.add(AttributeModifier.append("style", new Model<String>("text-align: right;")));
            }
          });
    }
    for (final KundeDO customer : getHRViewData().getCustomers()) {
      columns
          .add(new CellItemListenerPropertyColumn<HRViewUserData>(customer.getKundeIdentifierDisplayName(), null, null,
              cellItemListener)
          {
            @Override
            public void populateItem(final Item<ICellPopulator<HRViewUserData>> item, final String componentId,
                final IModel<HRViewUserData> rowModel)
            {
              cellItemListener.populateItem(item, componentId, rowModel);
              final HRViewUserEntryData entry = rowModel.getObject().getEntry(customer);
              if (entry == null) {
                item.add(createInvisibleDummyComponent(componentId));
                return;
              }
              addLabel(item, componentId, entry.getPlannedDays(), entry.getActualDays());
              item.add(AttributeModifier.append("style", new Model<String>("text-align: right;")));
            }
          });
    }
    dataTable = createDataTable(columns, "user.fullname", SortOrder.ASCENDING);
    form.add(dataTable);
  }

  private void addListEntry(final Item<ICellPopulator<HRViewUserData>> item, final String componentId,
      final BigDecimal plannedDays,
      final BigDecimal actualDays, final Link<?> link)
  {
    final HRListEntryPanel entryPanel = new HRListEntryPanel(componentId, form.getSearchFilter(), plannedDays,
        actualDays, link);
    item.add(entryPanel);
  }

  private void addLabel(final Item<ICellPopulator<HRViewUserData>> item, final String componentId,
      final BigDecimal plannedDays,
      final BigDecimal actualDays)
  {
    final HRFilter filter = form.getSearchFilter();
    final BigDecimal planned = filter.isShowPlanning() == true ? plannedDays : null;
    final BigDecimal actual = filter.isShowBookedTimesheets() == true ? actualDays : null;
    final StringBuffer buf = new StringBuffer();
    if (NumberHelper.isNotZero(plannedDays) == true) {
      buf.append(NumberFormatter.format(planned, 2));
    }
    if (NumberHelper.isNotZero(actualDays) == true) {
      buf.append(" (").append(NumberFormatter.format(actual, 2)).append(")");
    }
    item.add(new Label(componentId, buf.toString()));
  }

  /**
   * Get the current date (start date) and preset this date for the edit page.
   *
   * @see org.projectforge.web.wicket.AbstractListPage#onNewEntryClick(org.apache.wicket.PageParameters)
   */
  @Override
  protected AbstractEditPage<?, ?, ?> redirectToEditPage(PageParameters params)
  {
    if (params == null) {
      params = new PageParameters();
    }
    if (weekMillis != null) {
      params.add(WebConstants.PARAMETER_DATE, String.valueOf(weekMillis));
    }
    final AbstractEditPage<?, ?, ?> editPage = super.redirectToEditPage(params);
    return editPage;
  }

  @SuppressWarnings("serial")
  @Override
  protected void addBottomPanel(final String id)
  {
    final DivPanel panel = new DivPanel(id);// DivType.GRID12, DivType.BOX, DivType.ROUND_ALL);
    form.add(panel);
    resourceLinkPanel = new HRListResourceLinkPanel(panel.newChildId(), this, hrViewDao, userFormatter)
    {
      @Override
      public boolean isVisible()
      {
        return form.getSearchFilter().isOnlyMyProjects() == false;
      }
    };
    panel.add(resourceLinkPanel);
    recreateBottomPanel();
  }

  private void recreateBottomPanel()
  {
    resourceLinkPanel.refresh(getHRViewData(), form.getSearchFilter().getStartTime());
  }

  @Override
  protected HRListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new HRListForm(this);
  }

  private HRViewData getHRViewData()
  {
    if (hrViewData == null) {
      hrViewData = hrViewDao.getResources(form.getSearchFilter());
    }
    return hrViewData;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#buildList()
   */
  @Override
  protected List<HRViewUserData> buildList()
  {
    if (hrViewData == null) {
      return null;
    }
    final List<HRViewUserData> list = getHRViewData().getUserDatas();
    return list;
  }

  @Override
  public HRViewDao getBaseDao()
  {
    return hrViewDao;
  }

  @Override
  public void cancelSelection(final String property)
  {
    // Do nothing.
  }

  @Override
  public void select(final String property, final Object selectedValue)
  {
    if (property.equals("week") == true) {
      final Date date = (Date) selectedValue;
      final DateHolder dateHolder = new DateHolder(date);
      form.getSearchFilter().setStartTime(dateHolder.getDate());
      dateHolder.setEndOfWeek();
      form.getSearchFilter().setStopTime(dateHolder.getDate());
      form.startDate.markModelAsChanged();
      form.stopDate.markModelAsChanged();
      refresh();
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  @Override
  public void unselect(final String property)
  {
    log.error("Property '" + property + "' not supported for selection.");
  }

  @Override
  public void refresh()
  {
    super.refresh();
    this.hrViewData = null;
    recreateDataTable();
    recreateBottomPanel();
  }

  @Override
  protected boolean providesOwnRebuildDatabaseIndex()
  {
    return true;
  }

  @Override
  protected void ownRebuildDatabaseIndex(final boolean onlyNewest)
  {
    final ReindexSettings settings = DatabaseDao.createReindexSettings(onlyNewest);
    databaseDao.rebuildDatabaseSearchIndices(HRPlanningDO.class, settings);
    databaseDao.rebuildDatabaseSearchIndices(HRPlanningEntryDO.class, settings);
  }
}
