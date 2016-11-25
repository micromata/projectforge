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

package org.projectforge.web.task;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.AuftragsPositionVO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.KostCache;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.task.TaskNode;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.task.formatter.WicketTaskFormatter;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.business.user.UserFormatter;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.core.PriorityFormatter;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.fibu.OrderPositionsPanel;
import org.projectforge.web.user.UserPrefListPage;
import org.projectforge.web.user.UserPropertyColumn;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.DatePropertyColumn;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.ConsumptionBarPanel;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;

/**
 * This page is shown when the user searches in the task tree. The task will be displayed as list.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@ListPage(editPage = TaskEditPage.class)
public class TaskListPage extends AbstractListPage<TaskListForm, TaskDao, TaskDO>
    implements IListPageColumnsCreator<TaskDO>
{
  private static final long serialVersionUID = -337660148607303435L;

  @SpringBean
  private TaskDao taskDao;

  @SpringBean
  private UserFormatter userFormatter;

  @SpringBean
  private DateTimeFormatter dateTimeFormatter;

  @SpringBean
  private PriorityFormatter priorityFormatter;

  @SpringBean
  private KostCache kostCache;

  /**
   * Sibling page (if the user switches between tree and list view.
   */
  private TaskTreePage taskTreePage;

  static void appendCssClasses(final Item<?> item, final TaskDO task, final Integer preselectedTaskNode)
  {
    appendCssClasses(item, task.getId(), preselectedTaskNode, task.isDeleted());
  }

  /**
   * @param parentComponent Needed for call parentComponent.getString(String) for i18n.
   * @param componentId
   * @param taskTree
   * @param selectMode
   * @param node
   * @return
   */
  public static ConsumptionBarPanel getConsumptionBarPanel(final Component parentComponent, final String componentId,
      final TaskTree taskTree, final boolean selectMode, final TaskNode node)
  {
    Integer maxHours = null;
    Integer taskId = null;
    boolean finished = false;
    if (node != null) {
      maxHours = node.getTask().getMaxHours();
      taskId = node.getTaskId();
      finished = node.isFinished();
    }
    final BigDecimal maxDays;
    if (maxHours != null && maxHours.intValue() == 0) {
      maxDays = null;
    } else {
      maxDays = NumberHelper.setDefaultScale(taskTree.getPersonDays(node));
    }
    BigDecimal usage = (node != null)
        ? new BigDecimal(node.getDuration(taskTree, true)).divide(DateHelper.SECONDS_PER_WORKING_DAY, 2,
        BigDecimal.ROUND_HALF_UP)
        : BigDecimal.ZERO;
    usage = NumberHelper.setDefaultScale(usage);
    final ConsumptionBarPanel panel = new ConsumptionBarPanel(componentId, usage, maxDays, taskId, finished,
        parentComponent.getString("projectmanagement.personDays.short"), selectMode == false);
    return panel;
  }

  static String[] getKost2s(final List<Kost2DO> list)
  {
    if (list == null || list.size() == 0) {
      return null;
    }
    final String[] kost2s = new String[list.size()];
    for (int i = 0; i < kost2s.length; i++) {
      final Kost2DO kost2 = list.get(i);
      if (kost2.getProjekt() != null) {
        kost2s[i] = kost2.getShortDisplayName() + " " + kost2.getKost2Art().getName();
      } else {
        kost2s[i] = kost2.getShortDisplayName() + " " + kost2.getDescription();
      }
    }
    return kost2s;
  }

  static Label getKostLabel(final String componentId, final TaskTree taskTree, final TaskDO task)
  {
    final List<Kost2DO> list = taskTree.getKost2List(task.getId(), false);
    final StringBuffer buf = new StringBuffer();
    String[] kost2s = null;
    if (list != null) {
      if (list.size() == 1) {
        buf.append(HtmlHelper.escapeXml(list.get(0).getShortDisplayName()));
      } else {
        kost2s = getKost2s(list);
        buf.append(HtmlHelper.escapeXml(StringHelper.getWildcardString(kost2s))).append("*");
      }
    }
    final Label label = new Label(componentId, buf.toString());
    if (kost2s != null) {
      WicketUtils.addTooltip(label, StringHelper.listToString("\n", kost2s));
    }
    // label.setEscapeModelStrings(false);
    return label;
  }

  static Label getPriorityLabel(final String componentId, final PriorityFormatter priorityFormatter, final TaskDO task)
  {
    final String formattedPriority = priorityFormatter.getFormattedPriority(task.getPriority());
    final Label label = new Label(componentId, formattedPriority);
    label.setEscapeModelStrings(false);
    return label;
  }

  static Label getStatusLabel(final String componentId, final TaskDO task)
  {
    final String formattedStatus = WicketTaskFormatter.getFormattedTaskStatus(task.getStatus());
    final Label label = new Label(componentId, formattedStatus);
    label.setEscapeModelStrings(false);
    return label;
  }

  public TaskListPage(final PageParameters parameters)
  {
    super(parameters, "task");
  }

  /**
   * Called if the user clicks on button "list view".
   *
   * @param taskTreePage
   * @param parameters
   */
  TaskListPage(final TaskTreePage taskTreePage, final PageParameters parameters)
  {
    super(parameters, taskTreePage.caller, taskTreePage.selectProperty, "task");
    this.taskTreePage = taskTreePage;
  }

  @SuppressWarnings("serial")
  public List<IColumn<TaskDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final TaskTree taskTree = TaskTreeHelper.getTaskTree();
    final CellItemListener<TaskDO> cellItemListener = new CellItemListener<TaskDO>()
    {
      public void populateItem(final Item<ICellPopulator<TaskDO>> item, final String componentId,
          final IModel<TaskDO> rowModel)
      {
        final TaskDO task = rowModel.getObject();
        appendCssClasses(item, task, (Integer) getHighlightedRowId());
      }
    };
    final List<IColumn<TaskDO, String>> columns = new ArrayList<IColumn<TaskDO, String>>();
    columns.add(new CellItemListenerPropertyColumn<TaskDO>(getString("task"), getSortable("title", sortable), "title",
        cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<TaskDO>> item, final String componentId,
          final IModel<TaskDO> rowModel)
      {
        final TaskDO task = rowModel.getObject();
        final StringBuffer buf = new StringBuffer();
        WicketTaskFormatter.appendFormattedTask(getRequestCycle(), buf, task, true, false);
        final Label formattedTaskLabel = new Label(ListSelectActionPanel.LABEL_ID, buf.toString());
        formattedTaskLabel.setEscapeModelStrings(false);
        if (isSelectMode() == false) {
          item.add(new ListSelectActionPanel(componentId, rowModel, TaskEditPage.class, task.getId(), returnToPage,
              formattedTaskLabel));
        } else {
          item.add(new ListSelectActionPanel(componentId, rowModel, caller, selectProperty, task.getId(),
              formattedTaskLabel));
        }
        cellItemListener.populateItem(item, componentId, rowModel);
        addRowClick(item);
      }
    });
    columns
        .add(new CellItemListenerPropertyColumn<TaskDO>(getString("task.consumption"), null, "task", cellItemListener)
        {
          @Override
          public void populateItem(final Item<ICellPopulator<TaskDO>> item, final String componentId,
              final IModel<TaskDO> rowModel)
          {
            final TaskNode node = taskTree.getTaskNodeById(rowModel.getObject().getId());
            item.add(getConsumptionBarPanel(TaskListPage.this, componentId, taskTree, isSelectMode(), node));
            cellItemListener.populateItem(item, componentId, rowModel);
          }
        });
    if (kostCache.isKost2EntriesExists() == true) {
      columns.add(
          new CellItemListenerPropertyColumn<TaskDO>(getString("fibu.kost2"), getSortable("kost2", sortable), "kost2",
              cellItemListener)
          {
            @Override
            public void populateItem(final Item<ICellPopulator<TaskDO>> item, final String componentId,
                final IModel<TaskDO> rowModel)
            {
              final Label label = getKostLabel(componentId, taskTree, rowModel.getObject());
              item.add(label);
              cellItemListener.populateItem(item, componentId, rowModel);
            }
          });
    }
    if (taskTree.hasOrderPositionsEntries() == true
        && accessChecker.isLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP,
        ProjectForgeGroup.CONTROLLING_GROUP,
        ProjectForgeGroup.PROJECT_ASSISTANT, ProjectForgeGroup.PROJECT_MANAGER) == true) {
      columns.add(
          new CellItemListenerPropertyColumn<TaskDO>(getString("fibu.auftrag.auftraege"), null, null, cellItemListener)
          {
            @Override
            public void populateItem(final Item<ICellPopulator<TaskDO>> item, final String componentId,
                final IModel<TaskDO> rowModel)
            {
              final TaskDO task = rowModel.getObject();
              final Set<AuftragsPositionVO> orderPositions = taskTree.getOrderPositionEntries(task.getId());
              if (CollectionUtils.isEmpty(orderPositions) == true) {
                final Label label = new Label(componentId, ""); // Empty label.
                item.add(label);
              } else {
                final OrderPositionsPanel orderPositionsPanel = new OrderPositionsPanel(componentId)
                {
                  @Override
                  protected void onBeforeRender()
                  {
                    super.onBeforeRender();
                    // Lazy initialization because getString(...) of OrderPositionsPanel fails if panel.init(orderPositions) is called directly
                    // after instantiation.
                    init(orderPositions);
                  }
                };
                item.add(orderPositionsPanel);
              }
              cellItemListener.populateItem(item, componentId, rowModel);
            }
          });

    }
    columns.add(new CellItemListenerPropertyColumn<TaskDO>(getString("shortDescription"),
        getSortable("shortDescription", sortable),
        "shortDescription", cellItemListener));
    if (accessChecker.isLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP) == true) {
      columns.add(
          new DatePropertyColumn<TaskDO>(dateTimeFormatter, getString("task.protectTimesheetsUntil.short"), getSortable(
              "protectTimesheetsUntil", sortable), "protectTimesheetsUntil", cellItemListener));
    }
    columns.add(new CellItemListenerPropertyColumn<TaskDO>(getString("task.reference"),
        getSortable("reference", sortable), "reference",
        cellItemListener));
    columns.add(
        new CellItemListenerPropertyColumn<TaskDO>(getString("priority"), getSortable("priority", sortable), "priority",
            cellItemListener)
        {
          @Override
          public void populateItem(final Item<ICellPopulator<TaskDO>> item, final String componentId,
              final IModel<TaskDO> rowModel)
          {
            final Label label = getPriorityLabel(componentId, priorityFormatter, rowModel.getObject());
            item.add(label);
            cellItemListener.populateItem(item, componentId, rowModel);
          }
        });
    columns
        .add(new CellItemListenerPropertyColumn<TaskDO>(getString("status"), getSortable("status", sortable), "status",
            cellItemListener)
        {
          @Override
          public void populateItem(final Item<ICellPopulator<TaskDO>> item, final String componentId,
              final IModel<TaskDO> rowModel)
          {
            final Label label = getStatusLabel(componentId, rowModel.getObject());
            item.add(label);
            cellItemListener.populateItem(item, componentId, rowModel);
          }
        });
    final UserPropertyColumn<TaskDO> userPropertyColumn = new UserPropertyColumn<TaskDO>(getUserGroupCache(),
        getString("task.assignedUser"),
        getSortable(
            "responsibleUserId", sortable),
        "responsibleUserId", cellItemListener).withUserFormatter(userFormatter);
    columns.add(userPropertyColumn);
    return columns;
  }

  @Override
  protected void init()
  {
    dataTable = createDataTable(createColumns(this, true), "title", SortOrder.DESCENDING);
    form.add(dataTable);
    final BookmarkablePageLink<Void> addTemplatesLink = UserPrefListPage.createLink("link", UserPrefArea.TASK_FAVORITE);
    final ContentMenuEntryPanel menuEntry = new ContentMenuEntryPanel(getNewContentMenuChildId(), addTemplatesLink,
        getString("favorites"));
    addContentMenuEntry(menuEntry);
  }

  void onTreeViewSubmit()
  {
    if (taskTreePage != null) {
      setResponsePage(taskTreePage);
    } else {
      setResponsePage(new TaskTreePage(this, getPageParameters()));
    }
  }

  ISelectCallerPage getCaller()
  {
    return this.caller;
  }

  String getSelectProperty()
  {
    return this.selectProperty;
  }

  @Override
  protected TaskListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new TaskListForm(this);
  }

  @Override
  public TaskDao getBaseDao()
  {
    return taskDao;
  }
}
