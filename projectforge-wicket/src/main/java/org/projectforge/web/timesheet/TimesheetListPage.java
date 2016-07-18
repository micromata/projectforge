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

package org.projectforge.web.timesheet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.hibernate.Hibernate;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.business.systeminfo.SystemInfoCache;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.timesheet.TimesheetExport;
import org.projectforge.business.timesheet.TimesheetFilter;
import org.projectforge.business.user.UserCache;
import org.projectforge.business.user.UserFormatter;
import org.projectforge.business.utils.HtmlDateTimeFormatter;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.renderer.PdfRenderer;
import org.projectforge.framework.time.DateFormatType;
import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.utils.FileHelper;
import org.projectforge.framework.utils.MyBeanComparator;
import org.projectforge.jira.JiraUtils;
import org.projectforge.renderer.custom.Formatter;
import org.projectforge.renderer.custom.FormatterFactory;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.web.calendar.CalendarFeedService;
import org.projectforge.web.task.TaskPropertyColumn;
import org.projectforge.web.user.UserPrefListPage;
import org.projectforge.web.user.UserPropertyColumn;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.DownloadUtils;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;
import org.projectforge.web.wicket.MyListPageSortableDataProvider;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;
import org.projectforge.web.wicket.flowlayout.CheckBoxPanel;
import org.springframework.util.CollectionUtils;

@ListPage(editPage = TimesheetEditPage.class)
public class TimesheetListPage extends AbstractListPage<TimesheetListForm, TimesheetDao, TimesheetDO> implements
    IListPageColumnsCreator<TimesheetDO>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TimesheetListPage.class);

  protected static final String[] MY_BOOKMARKABLE_INITIAL_PROPERTIES = mergeStringArrays(
      BOOKMARKABLE_INITIAL_PROPERTIES, new String[] {
          "f.userId|user", "f.taskId|task", "f.startTime|t1", "f.stopTime|t2", "f.marked", "f.longFormat|long",
          "f.recursive" });

  /**
   * Key for pre-setting the task id.
   */
  public static final String PARAMETER_KEY_TASK_ID = "taskId";

  public static final String PARAMETER_KEY_SEARCHSTRING = "searchString";

  public static final String PARAMETER_KEY_USER_ID = "userId";

  public static final String PARAMETER_KEY_START_TIME = "startTime";

  public static final String PARAMETER_KEY_STOP_TIME = "stopTime";

  public static final String PARAMETER_KEY_CLEAR_ALL = "clear";

  private static final long serialVersionUID = 8582874051700734977L;

  @SpringBean
  private HtmlDateTimeFormatter dateTimeFormatter;

  @SpringBean
  private FormatterFactory formatterFactory;

  @SpringBean
  private PdfRenderer pdfRenderer;

  @SpringBean
  private TimesheetDao timesheetDao;

  @SpringBean
  private TimesheetExport timesheetExport;

  private transient TaskTree taskTree;

  @SpringBean
  private UserFormatter userFormatter;

  @SpringBean
  CalendarFeedService calendarFeedService;

  @SpringBean
  UserCache userCache;

  private TimesheetsICSExportDialog icsExportDialog;

  public TimesheetListPage(final PageParameters parameters)
  {
    super(parameters, "timesheet");
    if (WicketUtils.contains(parameters, PARAMETER_KEY_CLEAR_ALL) == true) {
      final boolean clear = WicketUtils.getAsBoolean(parameters, PARAMETER_KEY_CLEAR_ALL);
      if (clear == true) {
        form.getSearchFilter().setTaskId(null);
        form.getSearchFilter().setSearchString(null);
        form.getSearchFilter().setUserId(null);
        form.getSearchFilter().setStartTime(null);
        form.getSearchFilter().setStopTime(null);
        form.getSearchFilter().setRecursive(true);
        form.getSearchFilter().setMarked(false);
        form.getSearchFilter().setDeleted(false);
      }
    }
    if (WicketUtils.contains(parameters, PARAMETER_KEY_TASK_ID) == true) {
      final Integer id = WicketUtils.getAsInteger(parameters, PARAMETER_KEY_TASK_ID);
      form.getSearchFilter().setTaskId(id);
    }
    if (WicketUtils.contains(parameters, PARAMETER_KEY_SEARCHSTRING) == true) {
      final String searchString = WicketUtils.getAsString(parameters, PARAMETER_KEY_SEARCHSTRING);
      form.getSearchFilter().setSearchString(searchString);
    }
    if (WicketUtils.contains(parameters, PARAMETER_KEY_USER_ID) == true) {
      final Integer id = WicketUtils.getAsInteger(parameters, PARAMETER_KEY_USER_ID);
      form.getSearchFilter().setUserId(id);
    }
    if (WicketUtils.contains(parameters, PARAMETER_KEY_START_TIME) == true) {
      final Long time = WicketUtils.getAsLong(parameters, PARAMETER_KEY_START_TIME);
      if (time != null) {
        form.getSearchFilter().setStartTime(new Date(time));
      } else {
        form.getSearchFilter().setStartTime(null);
      }
    }
    if (WicketUtils.contains(parameters, PARAMETER_KEY_STOP_TIME) == true) {
      final Long time = WicketUtils.getAsLong(parameters, PARAMETER_KEY_STOP_TIME);
      if (time != null) {
        form.getSearchFilter().setStopTime(new Date(time));
      } else {
        form.getSearchFilter().setStopTime(null);
      }
    }
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    final BookmarkablePageLink<Void> addTemplatesLink = UserPrefListPage.createLink("link",
        UserPrefArea.TIMESHEET_TEMPLATE);
    final ContentMenuEntryPanel menuEntry = new ContentMenuEntryPanel(getNewContentMenuChildId(), addTemplatesLink,
        getString("templates"));
    addContentMenuEntry(menuEntry);
    final ContentMenuEntryPanel exportMenu = new ContentMenuEntryPanel(getNewContentMenuChildId(), getString("export"));
    addContentMenuEntry(exportMenu);
    {
      final SubmitLink exportPDFButton = new SubmitLink(ContentMenuEntryPanel.LINK_ID, form)
      {
        @Override
        public void onSubmit()
        {
          exportPDF();
        };
      };
      exportMenu.addSubMenuEntry(
          new ContentMenuEntryPanel(exportMenu.newSubMenuChildId(), exportPDFButton, getString("exportAsPdf"))
              .setTooltip(getString("tooltip.export.pdf")));
    }
    {
      final SubmitLink exportExcelButton = new SubmitLink(ContentMenuEntryPanel.LINK_ID, form)
      {
        @Override
        public void onSubmit()
        {
          exportExcel();
        };
      };
      exportMenu.addSubMenuEntry(
          new ContentMenuEntryPanel(exportMenu.newSubMenuChildId(), exportExcelButton, getString("exportAsXls"))
              .setTooltip(getString("tooltip.export.excel")));
    }
    icsExportDialog = new TimesheetsICSExportDialog(calendarFeedService, newModalDialogId(),
        new ResourceModel("timesheet.iCalSubscription"));
    add(icsExportDialog);
    icsExportDialog.init(ThreadLocalUserContext.getUserId());
    icsExportDialog.redraw();
    final AjaxLink<Void> icsExportDialogButton = new AjaxLink<Void>(ContentMenuEntryPanel.LINK_ID)
    {
      /**
       * @see org.apache.wicket.ajax.markup.html.AjaxLink#onClick(org.apache.wicket.ajax.AjaxRequestTarget)
       */
      @Override
      public void onClick(final AjaxRequestTarget target)
      {
        icsExportDialog.open(target);
      };

    };
    // final IconLinkPanel exportICalButtonPanel = new IconLinkPanel(buttonGroupPanel.newChildId(), IconType.DOWNLOAD,
    // getString("timesheet.iCalSubscription"), iCalExportLink);
    exportMenu.addSubMenuEntry(new ContentMenuEntryPanel(exportMenu.newSubMenuChildId(), icsExportDialogButton,
        getString("timesheet.icsExport")).setTooltip(getString("timesheet.iCalSubscription")));
  }

  @Override
  protected void onNextSubmit()
  {
    if (CollectionUtils.isEmpty(this.selectedItems) == true) {
      return;
    }
    final List<TimesheetDO> list = timesheetDao.internalLoad(this.selectedItems);
    setResponsePage(new TimesheetMassUpdatePage(this, list));
  }

  @Override
  public boolean isSupportsMassUpdate()
  {
    return true;
  }

  @Override
  protected void createDataTable()
  {
    final List<IColumn<TimesheetDO, String>> columns = createColumns(userCache, this,
        !isMassUpdateMode(),
        isMassUpdateMode(),
        form.getSearchFilter(),
        getTaskTree(), userFormatter, dateTimeFormatter);
    dataTable = createDataTable(columns, "startTime", SortOrder.DESCENDING);
    form.add(dataTable);
  }

  @Override
  public List<IColumn<TimesheetDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    return createColumns(userCache, returnToPage, sortable, false, form.getSearchFilter(), getTaskTree(), userFormatter,
        dateTimeFormatter);
  }

  /**
   * For re-usage in other pages.
   * 
   * @param page
   * @param isMassUpdateMode
   * @param timesheetFilter If given, then the long format filter setting will be used for displaying the description,
   *          otherwise the short description is used.
   */
  @SuppressWarnings("serial")
  protected static final List<IColumn<TimesheetDO, String>> createColumns(
      UserCache userCache, final WebPage page,
      final boolean sortable,
      final boolean isMassUpdateMode, final TimesheetFilter timesheetFilter, final TaskTree taskTree,
      final UserFormatter userFormatter,
      final HtmlDateTimeFormatter dateTimeFormatter)
  {
    final List<IColumn<TimesheetDO, String>> columns = new ArrayList<IColumn<TimesheetDO, String>>();
    final CellItemListener<TimesheetDO> cellItemListener = new CellItemListener<TimesheetDO>()
    {
      @Override
      public void populateItem(final Item<ICellPopulator<TimesheetDO>> item, final String componentId,
          final IModel<TimesheetDO> rowModel)
      {
        final TimesheetDO timesheet = rowModel.getObject();
        final Serializable highlightedRowId;
        if (page instanceof AbstractListPage<?, ?, ?>) {
          highlightedRowId = ((AbstractListPage<?, ?, ?>) page).getHighlightedRowId();
        } else {
          highlightedRowId = null;
        }
        appendCssClasses(item, timesheet.getId(), highlightedRowId, timesheet.isDeleted());
      }
    };
    if (page instanceof TimesheetMassUpdatePage) {
      columns.add(new UserPropertyColumn<TimesheetDO>(userCache, page.getString("timesheet.user"),
          getSortable("user.fullname", sortable), "user",
          cellItemListener).withUserFormatter(userFormatter));
    } else {
      // Show first column not for TimesheetMassUpdatePage!
      if (isMassUpdateMode == true && page instanceof TimesheetListPage) {
        final TimesheetListPage timesheetListPage = (TimesheetListPage) page;
        columns.add(new CellItemListenerPropertyColumn<TimesheetDO>("", null, "selected", cellItemListener)
        {
          @Override
          public void populateItem(final Item<ICellPopulator<TimesheetDO>> item, final String componentId,
              final IModel<TimesheetDO> rowModel)
          {
            final TimesheetDO timesheet = rowModel.getObject();
            final CheckBoxPanel checkBoxPanel = new CheckBoxPanel(componentId,
                timesheetListPage.new SelectItemModel(timesheet.getId()),
                null);
            item.add(checkBoxPanel);
            cellItemListener.populateItem(item, componentId, rowModel);
            addRowClick(item, isMassUpdateMode);
          }
        });
        columns.add(new UserPropertyColumn<TimesheetDO>(userCache, page.getString("timesheet.user"),
            getSortable("user.fullname", sortable), "user",
            cellItemListener).withUserFormatter(userFormatter));
      } else {
        columns.add(new UserPropertyColumn<TimesheetDO>(userCache, page.getString("timesheet.user"),
            getSortable("user.fullname", sortable), "user",
            cellItemListener)
        {
          @Override
          public void populateItem(final Item<ICellPopulator<TimesheetDO>> item, final String componentId,
              final IModel<TimesheetDO> rowModel)
          {
            item.add(new ListSelectActionPanel(componentId, rowModel, TimesheetEditPage.class,
                rowModel.getObject().getId(), page,
                getLabelString(rowModel)));
            cellItemListener.populateItem(item, componentId, rowModel);
            addRowClick(item);
          }
        }.withUserFormatter(userFormatter));
      }
    }
    final SystemInfoCache systemInfoCache = SystemInfoCache.instance();
    if (systemInfoCache.isCost2EntriesExists() == true) {
      columns.add(
          new CellItemListenerPropertyColumn<TimesheetDO>(new Model<String>(page.getString("fibu.kunde")), getSortable(
              "kost2.projekt.kunde.name", sortable), "kost2.projekt.kunde.name", cellItemListener));
      columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(new Model<String>(page.getString("fibu.projekt")),
          getSortable(
              "kost2.projekt.name", sortable),
          "kost2.projekt.name", cellItemListener));
    }
    columns.add(new TaskPropertyColumn<TimesheetDO>(page.getString("task"),
        getSortable("task.title", sortable), "task",
        cellItemListener)
            .withTaskTree(taskTree));
    if (systemInfoCache.isCost2EntriesExists() == true) {
      columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(page.getString("fibu.kost2"),
          getSortable("kost2.shortDisplayName",
              sortable),
          "kost2.shortDisplayName", cellItemListener));
    }
    columns.add(
        new CellItemListenerPropertyColumn<TimesheetDO>(page.getString("calendar.weekOfYearShortLabel"), getSortable(
            "formattedWeekOfYear", sortable), "formattedWeekOfYear", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(page.getString("calendar.dayOfWeekShortLabel"),
        getSortable("startTime",
            sortable),
        "startTime", cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<TimesheetDO>> item, final String componentId,
          final IModel<TimesheetDO> rowModel)
      {
        final TimesheetDO timesheet = rowModel.getObject();
        final Label label = new Label(componentId, dateTimeFormatter.getFormattedDate(timesheet.getStartTime(),
            DateFormats.getFormatString(DateFormatType.DAY_OF_WEEK_SHORT)));
        cellItemListener.populateItem(item, componentId, rowModel);
        item.add(label);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(page.getString("timePeriod"),
        getSortable("startTime", sortable),
        "timePeriod", cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<TimesheetDO>> item, final String componentId,
          final IModel<TimesheetDO> rowModel)
      {
        final TimesheetDO timesheet = rowModel.getObject();
        final Label label = new Label(componentId, dateTimeFormatter.getFormattedTimePeriod(timesheet.getTimePeriod()));
        label.setEscapeModelStrings(false);
        cellItemListener.populateItem(item, componentId, rowModel);
        item.add(label);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(page.getString("timesheet.duration"),
        getSortable("duration", sortable),
        "duration", cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<TimesheetDO>> item, final String componentId,
          final IModel<TimesheetDO> rowModel)
      {
        final TimesheetDO timesheet = rowModel.getObject();
        final Label label = new Label(componentId, dateTimeFormatter.getFormattedDuration(timesheet.getDuration()));
        label.setEscapeModelStrings(false);
        cellItemListener.populateItem(item, componentId, rowModel);
        item.add(label);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(page.getString("timesheet.location"),
        getSortable("location", sortable),
        "location", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(page.getString("description"),
        getSortable("shortDescription", sortable),
        "shortDescription", cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<TimesheetDO>> item, final String componentId,
          final IModel<TimesheetDO> rowModel)
      {
        final TimesheetDO timesheet = rowModel.getObject();
        final Label label = new Label(componentId, new Model<String>()
        {
          @Override
          public String getObject()
          {
            String text;
            if (timesheetFilter != null && timesheetFilter.isLongFormat() == true) {
              text = HtmlHelper.escapeXml(timesheet.getDescription());
            } else {
              text = HtmlHelper.escapeXml(timesheet.getShortDescription());
            }
            if (isMassUpdateMode == true) {
              return text;
            } else {
              return JiraUtils.linkJiraIssues(text); // Not in mass update mode: link on table row results otherwises in JIRA-Link.
            }
          }
        });
        label.setEscapeModelStrings(false);
        cellItemListener.populateItem(item, componentId, rowModel);
        item.add(label);
      }
    });
    return columns;
  }

  @Override
  protected TimesheetListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new TimesheetListForm(this);
  }

  @Override
  protected TimesheetDao getBaseDao()
  {
    return timesheetDao;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#select(java.lang.String, java.lang.Object)
   */
  @Override
  public void select(final String property, final Object selectedValue)
  {
    if ("taskId".equals(property) == true) {
      form.getSearchFilter().setTaskId((Integer) selectedValue);
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
      form.startDate.markModelAsChanged();
      form.stopDate.markModelAsChanged();
      refresh();
    } else {
      super.select(property, selectedValue);
    }
  }

  /**
   * 
   * @see org.projectforge.web.fibu.ISelectCallerPage#unselect(java.lang.String)
   */
  @Override
  public void unselect(final String property)
  {
    if ("taskId".equals(property) == true) {
      form.getSearchFilter().setTaskId(null);
      refresh();
    } else if ("userId".equals(property) == true) {
      form.getSearchFilter().setUserId(null);
      refresh();
    } else {
      super.unselect(property);
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#buildList()
   */
  @Override
  protected List<TimesheetDO> buildList()
  {
    final TimesheetFilter filter = form.getSearchFilter();
    if (filter.getStartTime() == null && filter.getStopTime() == null && filter.getTaskId() == null) {
      return null;
    }
    return super.buildList();
  }

  void exportPDF()
  {
    refresh();
    final List<TimesheetDO> timeSheets = getList();
    if (timeSheets == null || timeSheets.size() == 0) {
      // Nothing to export.
      form.addError("validation.error.nothingToExport");
      return;
    }
    final StringBuffer buf = new StringBuffer();
    buf.append("timesheets_");
    final TimesheetFilter filter = form.getSearchFilter();
    if (filter.getUserId() != null) {
      buf.append(FileHelper
          .createSafeFilename(getTenantRegistry().getUserGroupCache().getUser(filter.getUserId()).getLastname(), 20))
          .append("_");
    }
    if (filter.getTaskId() != null) {
      final String taskTitle = taskTree.getTaskById(filter.getTaskId()).getTitle();
      buf.append(FileHelper.createSafeFilename(taskTitle, 8)).append("_");
    }
    buf.append(DateHelper.getDateAsFilenameSuffix(filter.getStartTime())).append("_")
        .append(DateHelper.getDateAsFilenameSuffix(filter.getStopTime())).append(".pdf");
    final String filename = buf.toString();

    // get the sheets from the given Format
    final String styleSheet = "fo-styles/" + form.getExportFormat() + "/timesheet-template-fo.xsl";
    final String xmlData = "fo-styles/" + form.getExportFormat() + "/timesheets2pdf.xml";

    // get the formatter for the different export formats
    final Formatter formatter = formatterFactory.getFormatter(form.getExportFormat());

    final Integer taskId = filter.getTaskId();

    final Map<String, Object> data = formatter.getData(timeSheets, taskId, getRequest(), getResponse(), filter);

    // render the PDF with fop
    final byte[] content = pdfRenderer.render(styleSheet, xmlData, data);

    DownloadUtils.setDownloadTarget(content, filename);
  }

  protected void exportExcel()
  {
    refresh();
    final List<TimesheetDO> timeSheets = getList();
    if (timeSheets == null || timeSheets.size() == 0) {
      // Nothing to export.
      form.addError("validation.error.nothingToExport");
      return;
    }
    final String filename = "ProjectForge-TimesheetExport_" + DateHelper.getDateAsFilenameSuffix(new Date()) + ".xls";
    final byte[] xls = timesheetExport.export(timeSheets);
    if (xls == null || xls.length == 0) {
      log.error("Oups, xls has zero size. Filename: " + filename);
      return;
    }
    DownloadUtils.setDownloadTarget(xls, filename);
  }

  /**
   * Avoid LazyInitializationException user.fullname.
   * 
   * @see org.projectforge.web.wicket.AbstractListPage#createSortableDataProvider(java.lang.String, boolean)
   */
  @SuppressWarnings("serial")
  @Override
  protected ISortableDataProvider<TimesheetDO, String> createSortableDataProvider(final SortParam<String> sortParam)
  {
    this.listPageSortableDataProvider = new MyListPageSortableDataProvider<TimesheetDO>(sortParam, null, this)
    {
      @Override
      protected Comparator<TimesheetDO> getComparator(final SortParam<String> sortParam,
          final SortParam<String> secondSortParam)
      {
        final String sortProperty = sortParam != null ? sortParam.getProperty() : null;
        final boolean ascending = sortParam != null ? sortParam.isAscending() : true;
        final String secondSortProperty = secondSortParam != null ? secondSortParam.getProperty() : null;
        final boolean secondAscending = secondSortParam != null ? secondSortParam.isAscending() : true;
        return new MyBeanComparator<TimesheetDO>(sortProperty, ascending, secondSortProperty, secondAscending)
        {
          @Override
          public int compare(final TimesheetDO t1, final TimesheetDO t2)
          {
            if ("user.fullname".equals(sortProperty) == true) {
              PFUserDO user = t1.getUser();
              if (user != null && Hibernate.isInitialized(user) == false) {
                t1.setUser(getTenantRegistry().getUserGroupCache().getUser(user.getId()));
              }
              user = t2.getUser();
              if (user != null && Hibernate.isInitialized(user) == false) {
                t2.setUser(getTenantRegistry().getUserGroupCache().getUser(user.getId()));
              }
            } else if ("task.title".equals(sortProperty) == true) {
              TaskDO task = t1.getTask();
              if (task != null && Hibernate.isInitialized(task) == false) {
                t1.setTask(taskTree.getTaskById(task.getId()));
              }
              task = t2.getTask();
              if (task != null && Hibernate.isInitialized(task) == false) {
                t2.setTask(taskTree.getTaskById(task.getId()));
              }
            }
            return super.compare(t1, t2);
          }
        };
      }
    };
    return this.listPageSortableDataProvider;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#getBookmarkableInitialProperties()
   */
  @Override
  protected String[] getBookmarkableInitialProperties()
  {
    return MY_BOOKMARKABLE_INITIAL_PROPERTIES;
  }

  private TaskTree getTaskTree()
  {
    if (taskTree == null) {
      taskTree = TaskTreeHelper.getTaskTree();
    }
    return taskTree;
  }
}
