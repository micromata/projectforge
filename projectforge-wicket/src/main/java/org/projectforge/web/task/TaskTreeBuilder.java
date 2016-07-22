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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NoRecordsToolbar;
import org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.TableTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.content.Folder;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.NodeBorder;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.NodeModel;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.TreeColumn;
import org.apache.wicket.extensions.markup.html.repeater.tree.theme.WindowsTheme;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.projectforge.business.fibu.AuftragsPositionVO;
import org.projectforge.business.multitenancy.TenantRegistry;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.task.TaskFilter;
import org.projectforge.business.task.TaskNode;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.business.user.UserFormatter;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.web.core.PriorityFormatter;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.fibu.OrderPositionsPanel;
import org.projectforge.web.user.UserPropertyColumn;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.AbstractSecuredPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.DatePropertyColumn;
import org.projectforge.web.wicket.ListSelectActionPanel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Service
@Scope("prototype")
public class TaskTreeBuilder implements Serializable
{
  private static final long serialVersionUID = -2425308275690643856L;

  private final Behavior theme = new WindowsTheme();

  private Integer highlightedTaskNodeId;

  private boolean selectMode, showRootNode, showCost, showOrders;

  @Autowired
  private AccessChecker accessChecker;

  private transient TaskTree taskTree;

  @Autowired
  private PriorityFormatter priorityFormatter;

  @Autowired
  private UserFormatter userFormatter;

  @Autowired
  private DateTimeFormatter dateTimeFormatter;

  private TableTree<TaskNode, String> tree;

  private AbstractSecuredPage parentPage;

  private ISelectCallerPage caller;

  private String selectProperty;

  @SuppressWarnings("serial")
  public AbstractTree<TaskNode> createTree(final String id, final AbstractSecuredPage parentPage,
      final TaskFilter taskFilter, TaskDao taskDao)
  {
    this.parentPage = parentPage;
    final List<IColumn<TaskNode, String>> columns = createColumns();

    tree = new TableTree<TaskNode, String>(id, columns,
        new TaskTreeProvider(taskDao, taskFilter).setShowRootNode(showRootNode), Integer.MAX_VALUE,
        TaskTreeExpansion.getExpansionModel())
    {
      private static final long serialVersionUID = 1L;

      @Override
      protected Component newContentComponent(final String id, final IModel<TaskNode> model)
      {
        return TaskTreeBuilder.this.newContentComponent(id, this, model);
      }

      @Override
      protected Item<TaskNode> newRowItem(final String id, final int index, final IModel<TaskNode> model)
      {
        return new OddEvenItem<TaskNode>(id, index, model);
      }
    };
    tree.getTable().addTopToolbar(new HeadersToolbar<String>(tree.getTable(), null));
    tree.getTable().addBottomToolbar(new NoRecordsToolbar(tree.getTable()));
    tree.add(new Behavior()
    {
      @Override
      public void onComponentTag(final Component component, final ComponentTag tag)
      {
        theme.onComponentTag(component, tag);
      }

      @Override
      public void renderHead(final Component component, final IHeaderResponse response)
      {
        theme.renderHead(component, response);
      }
    });
    tree.getTable().add(AttributeModifier.append("class", "tableTree"));
    return tree;
  }

  /**
   * @return
   */
  @SuppressWarnings("serial")
  private List<IColumn<TaskNode, String>> createColumns()
  {
    final CellItemListener<TaskNode> cellItemListener = new CellItemListener<TaskNode>()
    {
      public void populateItem(final Item<ICellPopulator<TaskNode>> item, final String componentId,
          final IModel<TaskNode> rowModel)
      {
        final TaskNode taskNode = rowModel.getObject();
        TaskListPage.appendCssClasses(item, taskNode.getTask(), highlightedTaskNodeId);
      }
    };
    final List<IColumn<TaskNode, String>> columns = new ArrayList<IColumn<TaskNode, String>>();

    columns.add(new TreeColumn<TaskNode, String>(new ResourceModel("task"))
    {
      @Override
      public void populateItem(final Item<ICellPopulator<TaskNode>> cellItem, final String componentId,
          final IModel<TaskNode> rowModel)
      {
        final RepeatingView view = new RepeatingView(componentId);
        cellItem.add(view);
        final TaskNode taskNode = rowModel.getObject();
        //        if (selectMode == false || ((TaskEditPage) caller) != null
        //            && parentPage.getPage().getPageId() < ((TaskEditPage) caller).getPageId()) {
        if (selectMode == false) {
          view.add(new ListSelectActionPanel(view.newChildId(), rowModel, TaskEditPage.class, taskNode.getId(),
              parentPage, ""));
        } else {
          view.add(
              new ListSelectActionPanel(view.newChildId(), rowModel, caller, selectProperty, taskNode.getId(), ""));
        }
        AbstractListPage.addRowClick(cellItem);
        final NodeModel<TaskNode> nodeModel = (NodeModel<TaskNode>) rowModel;
        final Component nodeComponent = getTree().newNodeComponent(view.newChildId(), nodeModel.getWrappedModel());
        nodeComponent.add(new NodeBorder(nodeModel.getBranches()));
        view.add(nodeComponent);
        cellItemListener.populateItem(cellItem, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<TaskNode>(new ResourceModel("task.consumption"), null, "task",
        cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<TaskNode>> item, final String componentId,
          final IModel<TaskNode> rowModel)
      {
        final TaskNode node = rowModel.getObject();
        item.add(TaskListPage.getConsumptionBarPanel(tree, componentId, getTaskTree(), selectMode, node));
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    if (showCost == true) {
      columns.add(new CellItemListenerPropertyColumn<TaskNode>(new ResourceModel("fibu.kost2"), null, "task.kost2",
          cellItemListener)
      {
        @Override
        public void populateItem(final Item<ICellPopulator<TaskNode>> item, final String componentId,
            final IModel<TaskNode> rowModel)
        {
          final Label label = TaskListPage.getKostLabel(componentId, getTaskTree(), rowModel.getObject().getTask());
          item.add(label);
          cellItemListener.populateItem(item, componentId, rowModel);
        }
      });
    }
    if (getTaskTree().hasOrderPositionsEntries() == true && showOrders == true) {
      columns.add(new CellItemListenerPropertyColumn<TaskNode>(new ResourceModel("fibu.auftrag.auftraege"), null, null,
          cellItemListener)
      {
        @Override
        public void populateItem(final Item<ICellPopulator<TaskNode>> item, final String componentId,
            final IModel<TaskNode> rowModel)
        {
          final TaskNode taskNode = rowModel.getObject();
          final Set<AuftragsPositionVO> orderPositions = getTaskTree().getOrderPositionEntries(taskNode.getId());
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
              };
            };
            item.add(orderPositionsPanel);
          }
          cellItemListener.populateItem(item, componentId, rowModel);
        }
      });
    }
    columns.add(new CellItemListenerPropertyColumn<TaskNode>(new ResourceModel("shortDescription"), null,
        "task.shortDescription",
        cellItemListener));
    if (accessChecker.isLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP) == true) {
      columns.add(new DatePropertyColumn<TaskNode>(dateTimeFormatter,
          parentPage.getString("task.protectTimesheetsUntil.short"), null,
          "task.protectTimesheetsUntil", cellItemListener));
    }
    columns.add(new CellItemListenerPropertyColumn<TaskNode>(new ResourceModel("task.reference"), null, "reference",
        cellItemListener));
    columns.add(
        new CellItemListenerPropertyColumn<TaskNode>(new ResourceModel("priority"), null, "priority", cellItemListener)
        {
          @Override
          public void populateItem(final Item<ICellPopulator<TaskNode>> item, final String componentId,
              final IModel<TaskNode> rowModel)
          {
            final Label label = TaskListPage.getPriorityLabel(componentId, priorityFormatter,
                rowModel.getObject().getTask());
            item.add(label);
            cellItemListener.populateItem(item, componentId, rowModel);
          }
        });
    columns
        .add(new CellItemListenerPropertyColumn<TaskNode>(new ResourceModel("status"), null, "status", cellItemListener)
        {
          @Override
          public void populateItem(final Item<ICellPopulator<TaskNode>> item, final String componentId,
              final IModel<TaskNode> rowModel)
          {
            final Label label = TaskListPage.getStatusLabel(componentId, rowModel.getObject().getTask());
            item.add(label);
            cellItemListener.populateItem(item, componentId, rowModel);
          }
        });
    final UserPropertyColumn<TaskNode> userPropertyColumn = new UserPropertyColumn<TaskNode>(getUserGroupCache(),
        parentPage.getString("task.assignedUser"),
        null, "task.responsibleUserId", cellItemListener).withUserFormatter(userFormatter);
    columns.add(userPropertyColumn);
    return columns;
  }

  private TenantRegistry getTenantRegistry()
  {
    return TenantRegistryMap.getInstance().getTenantRegistry();
  }

  private UserGroupCache getUserGroupCache()
  {
    return getTenantRegistry().getUserGroupCache();
  }

  protected void addColumn(final WebMarkupContainer parent, final Component component, final String cssStyle)
  {
    if (cssStyle != null) {
      component.add(AttributeModifier.append("style", new Model<String>(cssStyle)));
    }
    parent.add(component);
  }

  /**
   * @param id
   * @param model
   * @return
   */
  @SuppressWarnings("serial")
  protected Component newContentComponent(final String id, final TableTree<TaskNode, String> tree,
      final IModel<TaskNode> model)
  {
    return new Folder<TaskNode>(id, tree, model)
    {

      @Override
      protected IModel<?> newLabelModel(final IModel<TaskNode> model)
      {
        return new PropertyModel<String>(model, "task.title");
      }
    };
  }

  /**
   * @param selectMode the selectMode to set
   * @return this for chaining.
   */
  public TaskTreeBuilder setSelectMode(final boolean selectMode)
  {
    this.selectMode = selectMode;
    return this;
  }

  /**
   * @param showRootNode the showRootNode to set
   * @return this for chaining.
   */
  public TaskTreeBuilder setShowRootNode(final boolean showRootNode)
  {
    this.showRootNode = showRootNode;
    return this;
  }

  /**
   * @param showCost the showCost to set
   * @return this for chaining.
   */
  public TaskTreeBuilder setShowCost(final boolean showCost)
  {
    this.showCost = showCost;
    return this;
  }

  /**
   * @param showOrders the showOrders to set
   * @return this for chaining.
   */
  public TaskTreeBuilder setShowOrders(final boolean showOrders)
  {
    this.showOrders = showOrders;
    return this;
  }

  /**
   * @param caller the caller to set
   * @return this for chaining.
   */
  public TaskTreeBuilder setCaller(final ISelectCallerPage caller)
  {
    this.caller = caller;
    return this;
  }

  /**
   * @param selectProperty the selectProperty to set
   * @return this for chaining.
   */
  public TaskTreeBuilder setSelectProperty(final String selectProperty)
  {
    this.selectProperty = selectProperty;
    return this;
  }

  /**
   * @param highlightedTaskNodeId the highlightedTaskNodeId to set
   * @return this for chaining.
   */
  public TaskTreeBuilder setHighlightedTaskNodeId(final Integer highlightedTaskNodeId)
  {
    this.highlightedTaskNodeId = highlightedTaskNodeId;
    final TaskNode node = getTaskTree().getTaskNodeById(highlightedTaskNodeId);
    if (node == null) {
      // Shouldn't occur.
      return this;
    }
    // Open all ancestor nodes of the highlighted node:
    final Set<TaskNode> set = TaskTreeExpansion.getExpansionModel().getObject();
    TaskNode parent = node.getParent();
    while (parent != null) {
      set.add(parent);
      parent = parent.getParent();
    }
    return this;
  }

  private TaskTree getTaskTree()
  {
    if (taskTree == null) {
      taskTree = TaskTreeHelper.getTaskTree();
    }
    return taskTree;
  }
}
