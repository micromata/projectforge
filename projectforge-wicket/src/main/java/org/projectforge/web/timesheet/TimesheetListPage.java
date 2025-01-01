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

package org.projectforge.web.timesheet;

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
import org.hibernate.Hibernate;
import org.projectforge.business.system.SystemInfoCache;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.timesheet.TimesheetExport;
import org.projectforge.business.timesheet.TimesheetFilter;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.utils.HtmlDateTimeFormatter;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.common.DateFormatType;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.renderer.PdfRenderer;
import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.utils.FileHelper;
import org.projectforge.framework.utils.MyBeanComparator;
import org.projectforge.jira.JiraUtils;
import org.projectforge.registry.Registry;
import org.projectforge.renderer.custom.Formatter;
import org.projectforge.renderer.custom.FormatterFactory;
import org.projectforge.rest.TimesheetPagesRest;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.task.TaskPropertyColumn;
import org.projectforge.web.user.UserPrefListPage;
import org.projectforge.web.user.UserPropertyColumn;
import org.projectforge.web.wicket.*;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.*;

@ListPage(editPage = TimesheetEditPage.class)
public class TimesheetListPage extends AbstractListPage<TimesheetListForm, TimesheetDao, TimesheetDO> implements
        IListPageColumnsCreator<TimesheetDO> {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TimesheetListPage.class);

    private static final String[] MY_BOOKMARKABLE_INITIAL_PROPERTIES = mergeStringArrays(
            BOOKMARKABLE_INITIAL_PROPERTIES, new String[]{
                    "f.userId|user", "f.taskId|task", "f.startTime|t1", "f.stopTime|t2", "f.marked", "f.longFormat|long",
                    "f.recursive"});

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

    private TimesheetsICSExportDialog icsExportDialog;

    public TimesheetListPage(final PageParameters parameters) {
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
            final Long id = WicketUtils.getAsLong(parameters, PARAMETER_KEY_TASK_ID);
            form.getSearchFilter().setTaskId(id);
        }
        if (WicketUtils.contains(parameters, PARAMETER_KEY_SEARCHSTRING) == true) {
            final String searchString = WicketUtils.getAsString(parameters, PARAMETER_KEY_SEARCHSTRING);
            form.getSearchFilter().setSearchString(searchString);
        }
        if (WicketUtils.contains(parameters, PARAMETER_KEY_USER_ID) == true) {
            final Long id = WicketUtils.getAsLong(parameters, PARAMETER_KEY_USER_ID);
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
    protected void init() {
        dataTable = createDataTable(createColumns(this, true), "startTime", SortOrder.DESCENDING);
        form.add(dataTable);
        final BookmarkablePageLink<Void> addTemplatesLink = UserPrefListPage.createLink("link",
                UserPrefArea.TIMESHEET_TEMPLATE);
        final ContentMenuEntryPanel menuEntry = new ContentMenuEntryPanel(getNewContentMenuChildId(), addTemplatesLink,
                getString("timesheet.templates"));
        addContentMenuEntry(menuEntry);
        final ContentMenuEntryPanel exportMenu = new ContentMenuEntryPanel(getNewContentMenuChildId(), getString("export"));
        addContentMenuEntry(exportMenu);
        {
            final SubmitLink exportPDFButton = new SubmitLink(ContentMenuEntryPanel.LINK_ID, form) {
                @Override
                public void onSubmit() {
                    exportPDF();
                }
            };
            exportMenu.addSubMenuEntry(
                    new ContentMenuEntryPanel(exportMenu.newSubMenuChildId(), exportPDFButton, getString("exportAsPdf"))
                            .setTooltip(getString("tooltip.export.pdf")));
        }
        {
            final SubmitLink exportExcelButton = new SubmitLink(ContentMenuEntryPanel.LINK_ID, form) {
                @Override
                public void onSubmit() {
                    exportExcel();
                }
            };
            exportMenu.addSubMenuEntry(
                    new ContentMenuEntryPanel(exportMenu.newSubMenuChildId(), exportExcelButton, getString("exportAsXls"))
                            .setTooltip(getString("tooltip.export.excel")));
        }
        icsExportDialog = new TimesheetsICSExportDialog(newModalDialogId(),
                new ResourceModel("timesheet.iCalSubscription"));
        add(icsExportDialog);
        icsExportDialog.init(ThreadLocalUserContext.getLoggedInUserId());
        icsExportDialog.redraw();
        final AjaxLink<Void> icsExportDialogButton = new AjaxLink<Void>(ContentMenuEntryPanel.LINK_ID) {
            /**
             * @see org.apache.wicket.ajax.markup.html.AjaxLink#onClick(org.apache.wicket.ajax.AjaxRequestTarget)
             */
            @Override
            public void onClick(final AjaxRequestTarget target) {
                icsExportDialog.open(target);
            }

        };
        // final IconLinkPanel exportICalButtonPanel = new IconLinkPanel(buttonGroupPanel.newChildId(), IconType.DOWNLOAD,
        // getString("timesheet.iCalSubscription"), iCalExportLink);
        exportMenu.addSubMenuEntry(new ContentMenuEntryPanel(exportMenu.newSubMenuChildId(), icsExportDialogButton,
                getString("timesheet.icsExport")).setTooltip(getString("timesheet.iCalSubscription")));
        addNewMassSelect(TimesheetPagesRest.class);
    }

    @Override
    public List<IColumn<TimesheetDO, String>> createColumns(final WebPage returnToPage, final boolean sortable) {
        return createColumns(getUserGroupCache(), returnToPage, form.getSearchFilter());
    }

    /**
     * For re-usage in other pages.
     *
     * @param page
     * @param timesheetFilter If given, then the long format filter setting will be used for displaying the description,
     *                        otherwise the short description is used.
     */
    @SuppressWarnings("serial")
    protected static final List<IColumn<TimesheetDO, String>> createColumns(
            UserGroupCache userGroupCache, final WebPage page,
            final TimesheetFilter timesheetFilter) {
        final List<IColumn<TimesheetDO, String>> columns = new ArrayList<IColumn<TimesheetDO, String>>();
        final CellItemListener<TimesheetDO> cellItemListener = new CellItemListener<TimesheetDO>() {
            @Override
            public void populateItem(final Item<ICellPopulator<TimesheetDO>> item, final String componentId,
                                     final IModel<TimesheetDO> rowModel) {
                final TimesheetDO timesheet = rowModel.getObject();
                final Serializable highlightedRowId;
                if (page instanceof AbstractListPage<?, ?, ?>) {
                    highlightedRowId = ((AbstractListPage<?, ?, ?>) page).getHighlightedRowId();
                } else {
                    highlightedRowId = null;
                }
                appendCssClasses(item, timesheet.getId(), highlightedRowId, timesheet.getDeleted());
            }
        };
        columns.add(new UserPropertyColumn<TimesheetDO>(userGroupCache, page.getString("timesheet.user"),
                getSortable("user.fullname", true), "user",
                cellItemListener) {
            @Override
            public void populateItem(final Item<ICellPopulator<TimesheetDO>> item, final String componentId,
                                     final IModel<TimesheetDO> rowModel) {
                item.add(new ListSelectActionPanel(componentId, rowModel, TimesheetEditPage.class,
                        rowModel.getObject().getId(), page,
                        getLabelString(rowModel)));
                cellItemListener.populateItem(item, componentId, rowModel);
                addRowClick(item);
            }
        });
        final SystemInfoCache systemInfoCache = SystemInfoCache.instance();
        if (systemInfoCache.isCost2EntriesExists() == true) {
            columns.add(
                    new CellItemListenerPropertyColumn<TimesheetDO>(new Model<String>(page.getString("fibu.kunde")), getSortable(
                            "kost2.projekt.kunde.name", true), "kost2.projekt.kunde.name", cellItemListener));
            columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(new Model<String>(page.getString("fibu.projekt")),
                    getSortable(
                            "kost2.projekt.name", true),
                    "kost2.projekt.name", cellItemListener));
        }
        columns.add(new TaskPropertyColumn<TimesheetDO>(page.getString("task"),
                getSortable("task.title", true), "task",
                cellItemListener));
        if (systemInfoCache.isCost2EntriesExists() == true) {
            columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(page.getString("fibu.kost2"),
                    getSortable("kost2.displayName",
                            true),
                    "kost2.displayName", cellItemListener));
        }
        columns.add(
                new CellItemListenerPropertyColumn<TimesheetDO>(page.getString("calendar.weekOfYearShortLabel"), getSortable(
                        "formattedWeekOfYear", true), "formattedWeekOfYear", cellItemListener));
        columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(page.getString("calendar.dayOfWeekShortLabel"),
                getSortable("startTime",
                        true),
                "startTime", cellItemListener) {
            @Override
            public void populateItem(final Item<ICellPopulator<TimesheetDO>> item, final String componentId,
                                     final IModel<TimesheetDO> rowModel) {
                final TimesheetDO timesheet = rowModel.getObject();
                final Label label = new Label(componentId, WicketSupport.get(HtmlDateTimeFormatter.class).getFormattedDate(timesheet.getStartTime(),
                        DateFormats.getFormatString(DateFormatType.DAY_OF_WEEK_SHORT)));
                cellItemListener.populateItem(item, componentId, rowModel);
                item.add(label);
            }
        });
        columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(page.getString("timePeriod"),
                getSortable("startTime", true),
                "timePeriod", cellItemListener) {
            @Override
            public void populateItem(final Item<ICellPopulator<TimesheetDO>> item, final String componentId,
                                     final IModel<TimesheetDO> rowModel) {
                final TimesheetDO timesheet = rowModel.getObject();
                final Label label = new Label(componentId, WicketSupport.get(HtmlDateTimeFormatter.class).getFormattedTimePeriod(timesheet.getTimePeriod()));
                label.setEscapeModelStrings(false);
                cellItemListener.populateItem(item, componentId, rowModel);
                item.add(label);
            }
        });
        columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(page.getString("timesheet.duration"),
                getSortable("duration", true),
                "duration", cellItemListener) {
            @Override
            public void populateItem(final Item<ICellPopulator<TimesheetDO>> item, final String componentId,
                                     final IModel<TimesheetDO> rowModel) {
                final TimesheetDO timesheet = rowModel.getObject();
                final Label label = new Label(componentId, WicketSupport.get(HtmlDateTimeFormatter.class).getFormattedDuration(timesheet.getDuration()));
                label.setEscapeModelStrings(false);
                cellItemListener.populateItem(item, componentId, rowModel);
                item.add(label);
            }
        });
        columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(page.getString("timesheet.location"),
                getSortable("location", true),
                "location", cellItemListener));
        columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(page.getString("description"),
                getSortable("shortDescription", true),
                "shortDescription", cellItemListener) {
            @Override
            public void populateItem(final Item<ICellPopulator<TimesheetDO>> item, final String componentId,
                                     final IModel<TimesheetDO> rowModel) {
                final TimesheetDO timesheet = rowModel.getObject();
                final Label label = new Label(componentId, new Model<String>() {
                    @Override
                    public String getObject() {
                        String text;
                        if (timesheetFilter != null && timesheetFilter.isLongFormat() == true) {
                            text = HtmlHelper.escapeXml(timesheet.getDescription());
                        } else {
                            text = HtmlHelper.escapeXml(timesheet.getShortDescription());
                        }
                        return JiraUtils.linkJiraIssues(text);
                    }
                });
                label.setEscapeModelStrings(false);
                cellItemListener.populateItem(item, componentId, rowModel);
                item.add(label);
            }
        });
        columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(page.getString("timesheet.reference"),
                getSortable("reference", true),
                "reference", cellItemListener));
        if (!CollectionUtils.isEmpty(Registry.getInstance().getDao(TimesheetDao.class).getTags())) {
            columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(page.getString("timesheet.tag"),
                    getSortable("tag", true),
                    "tag", cellItemListener));
        }
        return columns;
    }

    @Override
    protected TimesheetListForm newListForm(final AbstractListPage<?, ?, ?> parentPage) {
        return new TimesheetListForm(this);
    }

    @Override
    public TimesheetDao getBaseDao() {
        return WicketSupport.get(TimesheetDao.class);
    }

    /**
     * @see org.projectforge.web.wicket.AbstractListPage#select(java.lang.String, java.lang.Object)
     */
    @Override
    public void select(final String property, final Object selectedValue) {
        if ("taskId".equals(property) == true) {
            form.getSearchFilter().setTaskId((Long) selectedValue);
            refresh();
        } else if ("userId".equals(property) == true) {
            form.getSearchFilter().setUserId((Long) selectedValue);
            refresh();
        } else {
            super.select(property, selectedValue);
        }
    }

    /**
     * @see org.projectforge.web.fibu.ISelectCallerPage#unselect(java.lang.String)
     */
    @Override
    public void unselect(final String property) {
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
    protected List<TimesheetDO> buildList() {
        final TimesheetFilter filter = form.getSearchFilter();
        if (filter.getStartTime() == null && filter.getStopTime() == null && filter.getTaskId() == null) {
            return new ArrayList<>(); // return null results in an addition error message! (search.error)
        }
        return super.buildList();
    }

    void exportPDF() {
        refresh();
        final List<TimesheetDO> timeSheets = getList();
        if (timeSheets == null || timeSheets.size() == 0) {
            // Nothing to export.
            form.addError("validation.error.nothingToExport");
            return;
        }
        final StringBuilder buf = new StringBuilder();
        buf.append("timesheets_");
        final TimesheetFilter filter = form.getSearchFilter();
        if (filter.getUserId() != null) {
            buf.append(FileHelper
                            .createSafeFilename(UserGroupCache.getInstance().getUser(filter.getUserId()).getLastname(), 20))
                    .append("_");
        }
        if (filter.getTaskId() != null) {
            final String taskTitle = TaskTree.getInstance().getTaskById(filter.getTaskId()).getTitle();
            buf.append(FileHelper.createSafeFilename(taskTitle, 8)).append("_");
        }
        buf.append(DateHelper.getDateAsFilenameSuffix(filter.getStartTime())).append("_")
                .append(DateHelper.getDateAsFilenameSuffix(filter.getStopTime())).append(".pdf");
        final String filename = buf.toString();

        // get the sheets from the given Format
        final String styleSheet = "fo-styles/" + form.getExportFormat() + "/timesheet-template-fo.xsl";
        final String xmlData = "fo-styles/" + form.getExportFormat() + "/timesheets2pdf.xml";

        // get the formatter for the different export formats
        final Formatter formatter = WicketSupport.get(FormatterFactory.class).getFormatter(form.getExportFormat());

        final Long taskId = filter.getTaskId();

        final Map<String, Object> data = formatter.getData(timeSheets, taskId, getRequest(), getResponse(), filter);

        // render the PDF with fop
        final byte[] content = WicketSupport.get(PdfRenderer.class).render(styleSheet, xmlData, data);

        DownloadUtils.setDownloadTarget(content, filename);
    }

    protected void exportExcel() {
        refresh();
        final List<TimesheetDO> timeSheets = getList();
        if (timeSheets == null || timeSheets.size() == 0) {
            // Nothing to export.
            form.addError("validation.error.nothingToExport");
            return;
        }
        final String filename = "ProjectForge-TimesheetExport_" + DateHelper.getDateAsFilenameSuffix(new Date()) + ".xlsx";
        final byte[] xls = WicketSupport.get(TimesheetExport.class).export(timeSheets);
        if (xls == null || xls.length == 0) {
            log.error("Oups, xls has zero size. Filename: " + filename);
            return;
        }
        DownloadUtils.setDownloadTarget(xls, filename);
    }

    /**
     * Avoid LazyInitializationException user.fullname.
     */
    @SuppressWarnings("serial")
    @Override
    protected ISortableDataProvider<TimesheetDO, String> createSortableDataProvider(final SortParam<String> sortParam) {
        this.listPageSortableDataProvider = new MyListPageSortableDataProvider<TimesheetDO>(sortParam, null, this) {
            @Override
            protected Comparator<TimesheetDO> getComparator(final SortParam<String> sortParam,
                                                            final SortParam<String> secondSortParam) {
                final String sortProperty = sortParam != null ? sortParam.getProperty() : null;
                final boolean ascending = sortParam != null ? sortParam.isAscending() : true;
                final String secondSortProperty = secondSortParam != null ? secondSortParam.getProperty() : null;
                final boolean secondAscending = secondSortParam != null ? secondSortParam.isAscending() : true;
                return new MyBeanComparator<TimesheetDO>(sortProperty, ascending, secondSortProperty, secondAscending) {
                    @Override
                    public int compare(final TimesheetDO t1, final TimesheetDO t2) {
                        if ("user.fullname".equals(sortProperty) == true) {
                            PFUserDO user = t1.getUser();
                            if (user != null && Hibernate.isInitialized(user) == false) {
                                t1.setUser(UserGroupCache.getInstance().getUser(user.getId()));
                            }
                            user = t2.getUser();
                            if (user != null && Hibernate.isInitialized(user) == false) {
                                t2.setUser(UserGroupCache.getInstance().getUser(user.getId()));
                            }
                        } else if ("task.title".equals(sortProperty) == true) {
                            TaskDO task = t1.getTask();
                            if (task != null && Hibernate.isInitialized(task) == false) {
                                t1.setTask(TaskTree.getInstance().getTaskById(task.getId()));
                            }
                            task = t2.getTask();
                            if (task != null && Hibernate.isInitialized(task) == false) {
                                t2.setTask(TaskTree.getInstance().getTaskById(task.getId()));
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
    protected String[] getBookmarkableInitialProperties() {
        return MY_BOOKMARKABLE_INITIAL_PROPERTIES;
    }

    private TaskTree getTaskTree() {
        return TaskTree.getInstance();
    }
}
