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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.humanresources.HRPlanningDao;
import org.projectforge.business.humanresources.HRPlanningEntryDO;
import org.projectforge.business.humanresources.HRPlanningEntryDao;
import org.projectforge.business.user.UserFormatter;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.jira.JiraUtils;
import org.projectforge.web.core.PriorityFormatter;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.user.UserPropertyColumn;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;
import org.projectforge.web.wicket.NumberPropertyColumn;
import org.projectforge.web.wicket.WebConstants;

/**
 * 
 * @author Mario Groß (m.gross@micromata.de)
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@ListPage(editPage = HRPlanningEditPage.class)
public class HRPlanningListPage extends AbstractListPage<HRPlanningListForm, HRPlanningEntryDao, HRPlanningEntryDO>
    implements
    ISelectCallerPage
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HRPlanningListPage.class);

  private static final long serialVersionUID = 8582874051700734977L;

  @SpringBean
  private HRPlanningDao hrPlanningDao;

  @SpringBean
  private HRPlanningEntryDao hrPlanningEntryDao;

  @SpringBean
  private PriorityFormatter priorityFormatter;

  @SpringBean
  private UserFormatter userFormatter;

  private Boolean fullAccess;

  public HRPlanningListPage(final PageParameters parameters)
  {
    super(parameters, "hr.planning");
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    final List<IColumn<HRPlanningEntryDO, String>> columns = new ArrayList<IColumn<HRPlanningEntryDO, String>>();
    final CellItemListener<HRPlanningEntryDO> cellItemListener = new CellItemListener<HRPlanningEntryDO>()
    {
      @Override
      public void populateItem(final Item<ICellPopulator<HRPlanningEntryDO>> item, final String componentId,
          final IModel<HRPlanningEntryDO> rowModel)
      {
        final HRPlanningEntryDO entry = rowModel.getObject();
        appendCssClasses(item, entry.getPlanningId(), entry.isDeleted());
      }
    };
    columns
        .add(new UserPropertyColumn<HRPlanningEntryDO>(getUserGroupCache(), getString("timesheet.user"),
            "planning.user.fullname",
            "planning.user",
            cellItemListener)
        {
          @Override
          public void populateItem(final Item<ICellPopulator<HRPlanningEntryDO>> item, final String componentId,
              final IModel<HRPlanningEntryDO> rowModel)
          {
            if (hasFullAccess() == true) {
              item.add(new ListSelectActionPanel(componentId, rowModel, HRPlanningEditPage.class,
                  rowModel.getObject().getPlanning().getId(),
                  HRPlanningListPage.this, getLabelString(rowModel)));
              addRowClick(item);
            } else {
              item.add(new Label(componentId, getLabelString(rowModel)));
            }
            cellItemListener.populateItem(item, componentId, rowModel);
          }
        }.withUserFormatter(userFormatter));
    columns.add(new CellItemListenerPropertyColumn<HRPlanningEntryDO>(getString("calendar.year"), "planning.week",
        "planning.week",
        cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<HRPlanningEntryDO>> item, final String componentId,
          final IModel<HRPlanningEntryDO> rowModel)
      {
        final HRPlanningEntryDO entry = rowModel.getObject();
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy");
        final String year = simpleDateFormat.format(entry.getPlanning().getWeek());
        final Label label = new Label(componentId, year);
        item.add(label);
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<HRPlanningEntryDO>(getString("calendar.weekOfYearShortLabel"),
        "planning.formattedWeekOfYear", "planning.formattedWeekOfYear", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<HRPlanningEntryDO>(getString("fibu.kunde"), "projekt.kunde.name",
        "projekt.kunde.name",
        cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<HRPlanningEntryDO>(new Model<String>(getString("fibu.projekt")),
        "projektNameOrStatus",
        "projektNameOrStatus", cellItemListener));
    columns.add(
        new CellItemListenerPropertyColumn<HRPlanningEntryDO>(getString("hr.planning.priority"), "priority", "priority",
            cellItemListener)
        {
          @Override
          public void populateItem(final Item<ICellPopulator<HRPlanningEntryDO>> item, final String componentId,
              final IModel<HRPlanningEntryDO> rowModel)
          {
            final String formattedPriority = priorityFormatter.getFormattedPriority(rowModel.getObject().getPriority());
            final Label label = new Label(componentId, new Model<String>(formattedPriority));
            label.setEscapeModelStrings(false);
            item.add(label);
            cellItemListener.populateItem(item, componentId, rowModel);
            cellItemListener.populateItem(item, componentId, rowModel);
          }
        });
    columns
        .add(newNumberPropertyColumn("hr.planning.probability.short", "probability", cellItemListener).withSuffix("%"));
    columns.add(newNumberPropertyColumn("hr.planning.total", "planning.totalHours", cellItemListener));
    columns.add(newNumberPropertyColumn("hr.planning.sum", "totalHours", cellItemListener));
    columns.add(newNumberPropertyColumn("hr.planning.unassignedHours", "unassignedHours", cellItemListener));
    columns.add(newNumberPropertyColumn("calendar.shortday.monday", "mondayHours", cellItemListener));
    columns.add(newNumberPropertyColumn("calendar.shortday.tuesday", "tuesdayHours", cellItemListener));
    columns.add(newNumberPropertyColumn("calendar.shortday.wednesday", "wednesdayHours", cellItemListener));
    columns.add(newNumberPropertyColumn("calendar.shortday.thursday", "thursdayHours", cellItemListener));
    columns.add(newNumberPropertyColumn("calendar.shortday.friday", "fridayHours", cellItemListener));
    columns.add(newNumberPropertyColumn("hr.planning.weekend", "weekendHours", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<HRPlanningEntryDO>(getString("hr.planning.description"),
        "description", "description",
        cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<HRPlanningEntryDO>> item, final String componentId,
          final IModel<HRPlanningEntryDO> rowModel)
      {
        final HRPlanningEntryDO entry = rowModel.getObject();
        final Label label = new Label(componentId, new Model<String>()
        {
          @Override
          public String getObject()
          {
            String text;
            if (form.getSearchFilter().isLongFormat() == true) {
              text = HtmlHelper.escapeXml(entry.getDescription());
            } else {
              text = HtmlHelper.escapeXml(entry.getShortDescription());
            }
            return JiraUtils.linkJiraIssues(text); // Not in mass update mode: link on table row results otherwise in JIRA-Link.
          }
        });
        label.setEscapeModelStrings(false);
        item.add(label);
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });

    dataTable = createDataTable(columns, "planning.week", SortOrder.DESCENDING);
    form.add(dataTable);
    // final AbstractICSExportDialog icsExportDialog = new AbstractICSExportDialog(newModalDialogId(), new
    // ResourceModel("timesheet.iCalSubscription")) {
    // /**
    // * @see org.projectforge.web.calendar.AbstractICSExportDialog#getUrl()
    // */
    // @Override
    // protected String getUrl()
    // {
    // }
    // };
    // add(icsExportDialog);
    // icsExportDialog.init(ThreadLocalUserContext.getUserId());
    // icsExportDialog.redraw();
    // final AjaxLink<Void> icsExportDialogButton = new AjaxLink<Void>(ContentMenuEntryPanel.LINK_ID) {
    // /**
    // * @see org.apache.wicket.ajax.markup.html.AjaxLink#onClick(org.apache.wicket.ajax.AjaxRequestTarget)
    // */
    // @Override
    // public void onClick(final AjaxRequestTarget target)
    // {
    // icsExportDialog.open(target);
    // };
    //
    // };
    // addContentMenuEntry(new ContentMenuEntryPanel(getNewContentMenuChildId(), icsExportDialogButton, getString("timesheet.icsExport"))
    // .setTooltip(getString("timesheet.iCalSubscription")));
  }

  private NumberPropertyColumn<HRPlanningEntryDO> newNumberPropertyColumn(final String i18nKey, final String property,
      final CellItemListener<HRPlanningEntryDO> cellItemListener)
  {
    return new NumberPropertyColumn<HRPlanningEntryDO>(getString(i18nKey), property, property, cellItemListener)
        .withTextAlign("center")
        .withDisplayZeroValues(false);
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
    final Date date = form.getSearchFilter().getStartTime();
    if (date != null) {
      params.add(WebConstants.PARAMETER_DATE, String.valueOf(date.getTime()));
    }
    final AbstractEditPage<?, ?, ?> editPage = super.redirectToEditPage(params);
    return editPage;
  }

  @Override
  protected HRPlanningListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new HRPlanningListForm(this);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#buildList()
   */
  @Override
  protected List<HRPlanningEntryDO> buildList()
  {
    final List<HRPlanningEntryDO> list = hrPlanningEntryDao.getList(form.getSearchFilter());
    return list;
  }

  @Override
  public HRPlanningEntryDao getBaseDao()
  {
    return hrPlanningEntryDao;
  }

  @Override
  public void cancelSelection(final String property)
  {
    // Do nothing.
  }

  @Override
  public void select(final String property, final Object selectedValue)
  {
    if ("projektId".equals(property) == true) {
      form.getSearchFilter().setProjektId((Integer) selectedValue);
      form.projektSelectPanel.getTextField().modelChanged();
      refresh();
    } else if ("userId".equals(property) == true) {
      form.getSearchFilter().setUserId((Integer) selectedValue);
      refresh();
    } else if (property.startsWith("quickSelect.") == true) { // month".equals(property) == true) {
      final Date date = (Date) selectedValue;
      form.getSearchFilter().setStartTime(date);
      final DateHolder dateHolder = new DateHolder(date);
      if (property.endsWith(".month") == true) {
        dateHolder.setEndOfMonth();
      } else if (property.endsWith(".week") == true) {
        dateHolder.setEndOfWeek();
      } else {
        log.error("Property '" + property + "' not supported for selection.");
      }
      form.getSearchFilter().setStopTime(dateHolder.getDate());
      refresh();
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  @Override
  public void unselect(final String property)
  {
    if ("projektId".equals(property) == true) {
      form.getSearchFilter().setProjektId(null);
      form.projektSelectPanel.getTextField().modelChanged();
      refresh();
    } else if ("userId".equals(property) == true) {
      form.getSearchFilter().setUserId(null);
      refresh();
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  @Override
  public void refresh()
  {
    // form.getSearchFilter().setStartTime(new DateHolder(form.getSearchFilter().getStartTime()).setBeginOfWeek().getDate());
    // form.getSearchFilter().setStopDate(new DateHolder(form.getSearchFilter().getStopTime()).setEndOfWeek().getDate());
    form.startDate.markModelAsChanged();
    form.stopDate.markModelAsChanged();
    super.refresh();
  }

  protected boolean hasFullAccess()
  {
    if (fullAccess == null) {
      fullAccess = hrPlanningDao.hasLoggedInUserInsertAccess(null, false);
    }
    return fullAccess;
  }
}
