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

package org.projectforge.web.gantt;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.AbstractSubmitLink;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.markup.repeater.util.ModelIteratorAdapter;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.business.gantt.*;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.task.formatter.WicketTaskFormatter;
import org.projectforge.common.i18n.UserException;
import org.projectforge.framework.persistence.api.HibernateUtils;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.framework.utils.NumberFormatter;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.CSSColor;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.task.TaskEditForm;
import org.projectforge.web.task.TaskEditPage;
import org.projectforge.web.task.TaskTreePage;
import org.projectforge.web.tree.*;
import org.projectforge.web.wicket.*;
import org.projectforge.web.wicket.components.*;
import org.projectforge.web.wicket.converter.IntegerPercentConverter;
import org.projectforge.web.wicket.flowlayout.IconLinkPanel;
import org.projectforge.web.wicket.flowlayout.IconType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class GanttChartEditTreeTablePanel extends DefaultTreeTablePanel<GanttTreeTableNode> implements ISelectCallerPage
{
  private static final long serialVersionUID = -184278934597477820L;

  private static final int NUMBER_OF_REJECT_SAVE_COLS = 9;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
      .getLogger(GanttChartEditTreeTablePanel.class);

  private boolean[] rejectSaveColumnVisible;

  private GanttChartData ganttChartData;

  private final GanttChartEditForm form;

  private GanttTask clipboard;

  private final Map<Serializable, LocalDatePanel> startDatePanelMap = new HashMap<>();

  private final Map<Serializable, LocalDatePanel> endDatePanelMap = new HashMap<>();

  private final Map<Serializable, CheckBox> visibleCheckboxMap = new HashMap<>();

  private RefreshingView<GanttTreeTableNode> refreshingView;

  private final Component[] rejectSaveColHeads = new Component[NUMBER_OF_REJECT_SAVE_COLS];

  GanttChartEditTreeTablePanel(final String id, final GanttChartEditForm form, final GanttChartDO ganttChartDO)
  {
    super(id);
    this.form = form;
    clickRows = false;
    final StringBuilder buf = new StringBuilder();
    buf.append("function showSaveAsTaskQuestionDialog() {\n").append("  return window.confirm('");
    buf.append(ThreadLocalUserContext.getLocalizedString("gantt.question.saveGanttObjectAsTask"));
    buf.append("');\n}\n");
    buf.append("function showMoveTaskQuestionDialog() {\n").append("  return window.confirm('");
    buf.append(ThreadLocalUserContext.getLocalizedString("gantt.question.moveTask"));
    buf.append("');\n}\n");
    add(new Label("questionDialogsMethods", buf.toString()).setEscapeModelStrings(false));
  }

  @Override
  protected void initializeColumnHeads()
  {
    int col = 0;
    colHeadRepeater = new RepeatingView("cols");
    treeTableHead.add(colHeadRepeater);
    colHeadRepeater.add(createColHead("task"));
    colHeadRepeater.add(createEmtpyColumnHead(16)); // Column for edit icon.
    colHeadRepeater.add(SingleImagePanel
        .createTooltipImage(colHeadRepeater.newChildId(), WebConstants.IMAGE_EYE, getString("gantt.tooltip.isVisible"))
        .add(AttributeModifier.replace("style", "width: 16px;")).setRenderBodyOnly(false));
    addColumnHead(col++, "title");
    addColumnHead(col++, "gantt.startDate");
    addColumnHead(col++, "gantt.duration");
    addColumnHead(col++, "gantt.endDate");
    addColumnHead(col++, "task.progress");
    addColumnHead(col++, "gantt.predecessor");
    addColumnHead(col++, "gantt.predecessorOffset");
    addColumnHead(col++, "gantt.relationType.short");
    addColumnHead(col++, "gantt.objectType.short");
  }

  /**
   * Adds a column head for reject-save icons (only visible if modified elements exist).
   *
   * @param rejectSaveIndex
   * @param i18nKey
   */
  private void addColumnHead(final int rejectSaveIndex, final String i18nKey)
  {
    colHeadRepeater.add(createColHead(i18nKey));
    rejectSaveColHeads[rejectSaveIndex] = createEmtpyColumnHead(32);
    colHeadRepeater.add(rejectSaveColHeads[rejectSaveIndex]);
  }

  GanttChartEditTreeTablePanel setGanttChartData(final GanttChartData ganttChartData)
  {
    this.ganttChartData = ganttChartData;
    return this;
  }

  @Override
  public String getImageUrl(final String image)
  {
    return ((AbstractUnsecureBasePage) getPage()).getImageUrl(image);
  }

  @Override
  protected TreeTable<GanttTreeTableNode> buildTreeTable()
  {
    if (ganttChartData == null) {
      return null;
    }
    final GanttTreeTable ganttTreeTable = new GanttTreeTable(ganttChartData.getRootObject());
    return ganttTreeTable;
  }

  @Override
  protected List<GanttTreeTableNode> buildTreeList()
  {
    rejectSaveColumnVisible = new boolean[NUMBER_OF_REJECT_SAVE_COLS];
    if (ganttChartData == null) {
      return null;
    }
    final TreeTableFilter<TreeTableNode> filter = new TreeTableFilter<TreeTableNode>()
    {
      @Override
      public boolean match(final TreeTableNode name)
      {
        return true;
      }
    };
    final List<GanttTreeTableNode> treeList = getTreeTable().getNodeList(filter);
    return treeList;
  }

  @Override
  protected void onBeforeRender()
  {
    super.onBeforeRender();
    final TaskTree taskTree = TaskTree.getInstance();
    final List<GanttTreeTableNode> treeList = getTreeList();
    for (int i = 0; i < NUMBER_OF_REJECT_SAVE_COLS; i++) {
      rejectSaveColumnVisible[i] = false;
    }
    if (treeList != null) {
      for (final GanttTreeTableNode node : treeList) {
        final GanttTask ganttObject = node.getGanttObject();
        final TaskDO task = taskTree.getTaskById((Long) ganttObject.getId());
        if (task != null) {
          int col = 0;
          if (!rejectSaveColumnVisible[col] && isTitleModified(ganttObject, task)) {
            rejectSaveColumnVisible[col] = true;
          }
          if (!rejectSaveColumnVisible[++col] && isStartDateModified(ganttObject, task)) {
            rejectSaveColumnVisible[col] = true;
          }
          if (!rejectSaveColumnVisible[++col] && isDurationModified(ganttObject, task)) {
            rejectSaveColumnVisible[col] = true;
          }
          if (!rejectSaveColumnVisible[++col] && isEndDateModified(ganttObject, task)) {
            rejectSaveColumnVisible[col] = true;
          }
          if (!rejectSaveColumnVisible[++col] && isProgressModified(ganttObject, task)) {
            rejectSaveColumnVisible[col] = true;
          }
          if (!rejectSaveColumnVisible[++col] && isPredecessorModified(ganttObject, task)) {
            rejectSaveColumnVisible[col] = true;
          }
          if (!rejectSaveColumnVisible[++col] && isPredecessorOffsetModified(ganttObject, task)) {
            rejectSaveColumnVisible[col] = true;
          }
          if (!rejectSaveColumnVisible[++col] && isRelationTypeModified(ganttObject, task)) {
            rejectSaveColumnVisible[col] = true;
          }
          if (!rejectSaveColumnVisible[++col] && isTypeModified(ganttObject, task)) {
            rejectSaveColumnVisible[col] = true;
          }
        }
      }
    }
    for (int i = 0; i < NUMBER_OF_REJECT_SAVE_COLS; i++) {
      rejectSaveColHeads[i].setVisible(rejectSaveColumnVisible[i]);
    }
    createTreeRows();
    final Iterator<Item<GanttTreeTableNode>> it = refreshingView.getItems();
    while (it.hasNext()) {
      final Item<GanttTreeTableNode> row = it.next();
      final GanttTask ganttObject = row.getModelObject().getGanttObject();
      boolean visible = true;
      if (form.getSettings().isShowOnlyVisibles()) {
        final GanttTaskImpl root = (GanttTaskImpl) ganttChartData.getRootObject();
        GanttTask current = ganttObject;
        int i = 10;
        while (current != null && current != root) {
          if (i-- < 0) {
            break; // Endless loop protection.
          }
          if (!current.isVisible()) {
            visible = false;
            break;
          }
          current = root.findParent(current.getId());
        }
      }
      row.setVisible(visible);
      if (visible) {
        final TaskDO task = taskTree.getTaskById((Long) ganttObject.getId());
        int col = 0;
        setRejectSaveLinksFragmentVisibility("rejectSaveTitle", row, col++, isTitleModified(ganttObject, task));
        setRejectSaveLinksFragmentVisibility("rejectSaveStartDate", row, col++, isStartDateModified(ganttObject, task));
        setRejectSaveLinksFragmentVisibility("rejectSaveDuration", row, col++, isDurationModified(ganttObject, task));
        setRejectSaveLinksFragmentVisibility("rejectSaveEndDate", row, col++, isEndDateModified(ganttObject, task));
        setRejectSaveLinksFragmentVisibility("rejectSaveProgress", row, col++, isProgressModified(ganttObject, task));
        setRejectSaveLinksFragmentVisibility("rejectSavePredecessor", row, col++,
            isPredecessorModified(ganttObject, task));
        setRejectSaveLinksFragmentVisibility("rejectSavePredecessorOffset", row, col++,
            isPredecessorOffsetModified(ganttObject, task));
        setRejectSaveLinksFragmentVisibility("rejectSaveRelationType", row, col++,
            isRelationTypeModified(ganttObject, task));
        setRejectSaveLinksFragmentVisibility("rejectSaveType", row, col++, isTypeModified(ganttObject, task));
      }
    }
  }

  private void setRejectSaveLinksFragmentVisibility(final String id, final Item<GanttTreeTableNode> row, final int col,
      final boolean visible)
  {
    final RejectSaveLinksFragment rejectSaveFragment = (RejectSaveLinksFragment) row.get(id);
    if (!rejectSaveColumnVisible[col]) {
      rejectSaveFragment.setVisible(false);
    } else {
      rejectSaveFragment.setVisible(true);
      rejectSaveFragment.setIconsVisible(visible);
    }
  }

  private boolean isTitleModified(final GanttTask ganttObject, final TaskDO task)
  {
    return task != null && !StringUtils.equals(ganttObject.getTitle(), task.getTitle());
  }

  private boolean isStartDateModified(final GanttTask ganttObject, final TaskDO task)
  {
    return task != null && !DateHelper.isSameDay(PFDateTime.fromOrNull(ganttObject.getStartDate()), PFDateTime.fromOrNull(task.getStartDate()));
  }

  private boolean isDurationModified(final GanttTask ganttObject, final TaskDO task)
  {
    return task != null && !NumberHelper.isEqual(ganttObject.getDuration(), task.getDuration());
  }

  private boolean isEndDateModified(final GanttTask ganttObject, final TaskDO task)
  {
    return task != null && !DateHelper.isSameDay(PFDateTime.fromOrNull(ganttObject.getEndDate()), PFDateTime.fromOrNull(task.getEndDate()));
  }

  private boolean isProgressModified(final GanttTask ganttObject, final TaskDO task)
  {
    return task != null && !NumberHelper.isEqual(ganttObject.getProgress(), task.getProgress());
  }

  private boolean isPredecessorModified(final GanttTask ganttObject, final TaskDO task)
  {
    return task != null && !Objects.equals(ganttObject.getPredecessorId(), task.getGanttPredecessorId());
  }

  private boolean isPredecessorOffsetModified(final GanttTask ganttObject, final TaskDO task)
  {
    return task != null
        && !NumberHelper.isEqual(ganttObject.getPredecessorOffset(), task.getGanttPredecessorOffset());
  }

  private boolean isRelationTypeModified(final GanttTask ganttObject, final TaskDO task)
  {
    return task != null && ganttObject.getRelationType() != task.getGanttRelationType();
  }

  private boolean isTypeModified(final GanttTask ganttObject, final TaskDO task)
  {
    return task != null && ganttObject.getType() != task.getGanttObjectType();
  }

  @SuppressWarnings("serial")
  @Override
  protected void createTreeRows()
  {
    if (refreshingView != null) {
      // Already initialized.
      return;
    }

    refreshingView = new RefreshingView<GanttTreeTableNode>("rows")
    {

      @SuppressWarnings("unchecked")
      @Override
      protected Iterator<IModel<GanttTreeTableNode>> getItemModels()
      {
        List<GanttTreeTableNode> treeList = getTreeList();
        if (treeList == null) {
          treeList = new ArrayList<GanttTreeTableNode>();
        }
        return new ModelIteratorAdapter(treeList.iterator())
        {
          @Override
          protected IModel<?> model(final Object obj)
          {
            return EqualsDecorator.decorate(new CompoundPropertyModel(obj));
          }
        };
      }

      @Override
      protected void populateItem(final Item<GanttTreeTableNode> item)
      {
        final GanttTreeTableNode node = item.getModelObject();
        final GanttTask ganttObject = node.getGanttObject();
        final TaskDO task = TaskTree.getInstance().getTaskById((Long) ganttObject.getId());
        if (item.getIndex() % 2 == 0) {
          item.add(AttributeModifier.replace("class", "even"));
        } else {
          item.add(AttributeModifier.replace("class", "odd"));
        }
        final Label formattedLabel = new Label(ListSelectActionPanel.LABEL_ID, new Model<String>() {
          @Override
          public String getObject() {
            if (NumberHelper.greaterZero((Long) ganttObject.getId())) {
              return ganttObject.getTitle();
            } else {
              return "*" + ganttObject.getTitle() + "*";
            }
          }
        }) {
          @Override
          protected void onBeforeRender() {
            final boolean clipboarded = clipboard != null && clipboard.getId() == ganttObject.getId();
            if (clipboarded) {
              add(AttributeModifier.replace("style", "font-weight: bold; color:red;"));
            } else {
              final Behavior behavior = WicketUtils.getAttributeModifier(this, "style");
              if (behavior != null) {
                this.remove(behavior);
              }
            }
            super.onBeforeRender();
          }
        };

        final TreeIconsActionPanel<? extends TreeTableNode> treeIconsActionPanel = new TreeIconsActionPanel<GanttTreeTableNode>(
            "tree",
            new Model<>(node), formattedLabel, getTreeTable());
        treeIconsActionPanel.setUseAjaxAtDefault(false).setUseSubmitLinkImages(true);
        addColumn(item, treeIconsActionPanel, null);
        treeIconsActionPanel.init(GanttChartEditTreeTablePanel.this, node);
        treeIconsActionPanel.add(AttributeModifier.append("style", new Model<>("white-space: nowrap;")));
        treeIconsActionPanel.setUseAjaxAtDefault(false);
        {
          final WebMarkupContainer dropDownMenu = new WebMarkupContainer("dropDownMenu");
          addColumn(item, dropDownMenu, "white-space: nowrap; width: 32px;");
          dropDownMenu.add(new PresizedImage("cogImage", WebConstants.IMAGE_COG));
          dropDownMenu.add(new PresizedImage("arrowDownImage", WebConstants.IMAGE_ARROW_DOWN));
          final RepeatingView menuRepeater = new RepeatingView("menuEntriesRepeater");
          dropDownMenu.add(menuRepeater);
          menuRepeater.add(new ContextMenuEntry(menuRepeater.newChildId(), "mark") {
            @Override
            void onSubmit() {
              if (clipboard != null && clipboard == node.getGanttObject()) {
                clipboard = null;
              } else {
                clipboard = node.getGanttObject();
              }
            }
          });
          menuRepeater.add(new ContextMenuEntry(menuRepeater.newChildId(), "gantt.predecessor.paste")
          {
            @Override
            public boolean isVisible()
            {
              return clipboard != null && clipboard != ganttObject;
            }

            @Override
            void onSubmit()
            {
              ganttObject.setPredecessor(clipboard);
            }
          }.addTooltip(new Model<String>()
          {
            @Override
            public String getObject()
            {
              return getString("paste") + ": " + (clipboard != null ? clipboard.getTitle() : "-");
            }
          }));
          menuRepeater.add(new ContextMenuEntry(menuRepeater.newChildId(), "gantt.contextMenu.newSubActivity")
          {
            @Override
            void onSubmit()
            {
              final GanttTaskImpl root = (GanttTaskImpl) ganttChartData.getRootObject();
              final Long nextId = root.getNextId();
              ganttObject.addChild(new GanttTaskImpl(nextId).setVisible(true).setTitle(getString("untitled")));
              final Set<Serializable> openNodes = getOpenNodes();
              openNodes.add(ganttObject.getId());
              refreshTreeTable();
              setOpenNodes(openNodes);
              form.getParentPage().refresh();
            }
          });
          menuRepeater.add(new ContextMenuEntry(menuRepeater.newChildId(), new Model<String>() {
            @Override
            public String getObject() {
              if (clipboard == ganttObject) {
                return ThreadLocalUserContext.getLocalizedString("gantt.action.moveToTop");
              } else {
                return ThreadLocalUserContext.getLocalizedString("gantt.action.move");
              }
            }
          }) {
            @Override
            public boolean isVisible() {
              if (clipboard == null) {
                return false;
              }
              if (clipboard != ganttObject) {
                return true;
              }
              final GanttTaskImpl root = (GanttTaskImpl) ganttChartData.getRootObject();
              final GanttTask parent = root.findParent(ganttObject.getId());
              return (root != parent);
            }

            @Override
            void onSubmit() {
              if (clipboard == null) {
                return;
              }
              TaskDao taskDao = WicketSupport.getTaskDao();
              final GanttTaskImpl root = (GanttTaskImpl) ganttChartData.getRootObject();
              final GanttTask parent = root.findParent(clipboard.getId());
              final TaskDO task = TaskTree.getInstance().getTaskById((Long) clipboard.getId());
              parent.removeChild(clipboard);
              if (clipboard == ganttObject) {
                // Move to top level:
                root.addChild(ganttObject);
                final TaskDO rootTask = form.getData().getTask();
                if (rootTask != null && task != null) {
                  task.setParentTask(rootTask);
                  taskDao.update(task);
                }
              } else {
                // Move as a child of this Gantt activity:
                ganttObject.addChild(clipboard);
                final TaskDO parentTask = taskDao.getTaskTree().getTaskById((Long) ganttObject.getId());
                if (parentTask != null && task != null) {
                  task.setParentTask(parentTask);
                  taskDao.update(task);
                }
                getOpenNodes().add(ganttObject.getId());
              }
              final Set<Serializable> openNodes = getOpenNodes();
              refreshTreeTable();
              setOpenNodes(openNodes);
              form.getParentPage().refresh();
            }

            @Override
            protected void onBeforeRender() {
              if (clipboard != null) {
                final TaskDO task = TaskTree.getInstance().getTaskById((Long) clipboard.getId());
                if (task != null && onClick == null) {
                  // Question for safety before moving a task.
                  setOnClick("if (!showMoveTaskQuestionDialog()) return;");
                }
              }
              super.onBeforeRender();
            }
          });
          menuRepeater.add(new ContextMenuEntry(menuRepeater.newChildId(), "delete") {
            @Override
            public boolean isVisible() {
              return task == null;
            }

            @Override
            void onSubmit() {
              final GanttTaskImpl root = (GanttTaskImpl) ganttChartData.getRootObject();
              final GanttTask parent = root.findParent(ganttObject.getId());
              parent.removeChild(ganttObject);
              final Set<Serializable> openNodes = getOpenNodes();
              refreshTreeTable();
              setOpenNodes(openNodes);
              form.getParentPage().refresh();
            }
          }.setOnClick("if (!showDeleteQuestionDialog()) return;"));
          menuRepeater.add(new ContextMenuEntry(menuRepeater.newChildId(), "gantt.contextMenu.saveAsTask") {
            @Override
            public boolean isVisible() {
              return task == null;
            }

            @Override
            void onSubmit() {
              final GanttTaskImpl root = (GanttTaskImpl) ganttChartData.getRootObject();
              final GanttTask parent = root.findParent(ganttObject.getId());
              final TaskDO parentTask = TaskTree.getInstance().getTaskById((Long) parent.getId());
              if (parentTask == null) {
                throw new UserException("gantt.error.parentObjectIsNotAPFTask");
              }
              TaskDO task = TaskTree.getInstance().getTaskById((Long) ganttObject.getId());
              if (task != null) {
                // Oups, Gantt object is already a ProjectForge task.
                return;
              }
              task = Task2GanttTaskConverter.convertToTask(ganttObject);
              task.setParentTask(parentTask);
              final GanttTask predecessor = ganttObject.getPredecessor();
              if (predecessor != null) {
                final TaskDO predecessorTask = TaskTree.getInstance().getTaskById((Long) predecessor.getId());
                if (predecessorTask != null) {
                  task.setGanttPredecessor(predecessorTask);
                }
              }
              final Set<Serializable> openNodes = getOpenNodes();
              final Serializable id = WicketSupport.getTaskDao().insert(task);
              openNodes.remove(ganttObject.getId());
              ganttObject.setId(id);
              openNodes.add(id);
              refreshTreeTable();
              setOpenNodes(openNodes);
              form.getParentPage().refresh();
            }
          }.setOnClick("if (!showSaveAsTaskQuestionDialog()) return;"));
          menuRepeater.add(new ContextMenuEntry(menuRepeater.newChildId(), "task.title.edit") {
            @Override
            public boolean isVisible() {
              return task != null;
            }

            @Override
            void onSubmit() {
              final PageParameters pageParams = new PageParameters();
              pageParams.add(AbstractEditPage.PARAMETER_KEY_ID, String.valueOf(task.getId()));
              final TaskEditPage editPage = new TaskEditPage(pageParams);
              editPage.setReturnToPage((AbstractSecuredPage) getPage());
              setResponsePage(editPage);
            }
          });
          menuRepeater.add(new ContextMenuEntry(menuRepeater.newChildId(), "gantt.contextMenu.setInvisible") {
            @Override
            void onSubmit() {
              ((GanttTaskImpl) ganttObject).setInvisible();
              for (final CheckBox visibleCheckBox : visibleCheckboxMap.values()) {
                visibleCheckBox.modelChanged();
              }
            }
          });
          menuRepeater.add(new ContextMenuEntry(menuRepeater.newChildId(), "gantt.contextMenu.setSubTasksVisible") {
            @Override
            void onSubmit() {
              ganttObject.setVisible(true);
              if (ganttObject.getChildren() != null) {
                for (final GanttTask child : ganttObject.getChildren()) {
                  child.setVisible(true);
                }
              }
              for (final CheckBox visibleCheckBox : visibleCheckboxMap.values()) {
                visibleCheckBox.modelChanged();
              }
            }
          });
        }
        final CheckBox visibleCheckBox = (CheckBox) new CheckBox("visible",
            new PropertyModel<Boolean>(ganttObject, "visible"))
            .setRenderBodyOnly(false);
        visibleCheckboxMap.put(ganttObject.getId(), visibleCheckBox);
        addColumn(item, visibleCheckBox, "width: 16px;");
        addTitleColumns(item, node, ganttObject, task);
        addStartDateColumns(item, node, ganttObject, task);
        addDurationColumns(item, node, ganttObject, task);
        addEndDateColumns(item, node, ganttObject, task);
        addProgressColumns(item, node, ganttObject, task);
        addPredecessorColumns(item, node, ganttObject, task);
        addPredecessorOffsetColumns(item, node, ganttObject, task);
        addRelationTypeColumns(item, node, ganttObject, task);
        addTypeColumns(item, node, ganttObject, task);
      }
    };
    refreshingView.setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());
    treeTableBody.add(refreshingView);
  }

  @SuppressWarnings("serial")
  private abstract class ContextMenuEntry extends WebMarkupContainer
  {
    private final AbstractSubmitLink link;

    String onClick;

    private ContextMenuEntry(final String id)
    {
      super(id);
      link = new SubmitLink("menuEntry")
      {
        @Override
        public void onSubmit()
        {
          ContextMenuEntry.this.onSubmit();
        }
      }.setDefaultFormProcessing(false);
      add(link);
    }

    public ContextMenuEntry(final String id, final String labelKey)
    {
      this(id);
      link.add(new Label("label", ThreadLocalUserContext.getLocalizedString(labelKey)).setRenderBodyOnly(true));
    }

    public ContextMenuEntry(final String id, final Model<String> label)
    {
      this(id);
      link.add(new Label("label", label).setRenderBodyOnly(true));
    }

    abstract void onSubmit();

    public ContextMenuEntry addTooltip(final Model<String> model)
    {
      WicketUtils.addTooltip(link, model);
      return this;
    }

    public ContextMenuEntry setOnClick(final String value)
    {
      this.onClick = value;
      link.add(AttributeModifier.prepend("onclick", value));
      return this;
    }
  }

  private void addColumn(final Item<GanttTreeTableNode> item, final Component component, final String cssStyle)
  {
    if (cssStyle != null) {
      component.add(AttributeModifier.append("style", new Model<>(cssStyle)));
    }
    item.add(component);
  }

  @SuppressWarnings("serial")
  private void addTitleColumns(final Item<GanttTreeTableNode> item, final GanttTreeTableNode node,
      final GanttTask ganttObject,
      final TaskDO task)
  {
    final AjaxRequiredMaxLengthEditableLabel titleField = new AjaxRequiredMaxLengthEditableLabel("title",
        new PropertyModel<>(
            ganttObject, "title"),
        HibernateUtils.getPropertyLength(TaskDO.class.getName(), "title"));
    titleField.setOutputMarkupId(true);
    // final RequiredMaxLengthTextField titleField = new RequiredMaxLengthTextField("title", new PropertyModel<String>(ganttObject,
    // "title"),
    // HibernateUtils.getPropertyLength(TaskDO.class.getName(), "title"));
    addColumn(item, titleField, null);
    new RejectSaveLinksFragment("rejectSaveTitle", item, titleField, task, task != null ? task.getTitle() : "")
    {
      @Override
      protected void onSave()
      {
        task.setTitle(ganttObject.getTitle());
        WicketSupport.getTaskDao().update(task);
      }

      @Override
      protected void onReject()
      {
        ganttObject.setTitle(task.getTitle());
      }
    };
  }

  @SuppressWarnings("serial")
  private void addStartDateColumns(final Item<GanttTreeTableNode> item, final GanttTreeTableNode node,
      final GanttTask ganttObject,
      final TaskDO task)
  {
    final LocalDatePanel startDatePanel = new LocalDatePanel("startDate", new LocalDateModel(new PropertyModel<>(ganttObject, "startDate")),
        DatePanelSettings.get().withSelectProperty("startDate:" + node.getHashId()), true);
    addColumn(item, startDatePanel, "white-space: nowrap;");
    startDatePanelMap.put(ganttObject.getId(), startDatePanel);
    new RejectSaveLinksFragment("rejectSaveStartDate", item, startDatePanel, task,
        task != null ? DateTimeFormatter.instance()
            .getFormattedDate(task.getStartDate()) : "")
    {
      @Override
      protected void onSave()
      {
        task.setStartDate(ganttObject.getStartDate());
        WicketSupport.getTaskDao().update(task);
      }

      @Override
      protected void onReject()
      {
        ganttObject.setStartDate(task.getStartDate());
      }
    };
  }

  @SuppressWarnings("serial")
  private void addDurationColumns(final Item<GanttTreeTableNode> item, final GanttTreeTableNode node,
      final GanttTask ganttObject,
      final TaskDO task)
  {
    final MinMaxNumberField<BigDecimal> durationField = new MinMaxNumberField<>("duration",
        new PropertyModel<>(
            ganttObject, "duration"),
        BigDecimal.ZERO, TaskEditForm.MAX_DURATION_DAYS);
    addColumn(item, durationField, null);
    new RejectSaveLinksFragment("rejectSaveDuration", item, durationField, task,
        task != null ? NumberFormatter.format(task.getDuration(),
            2) : "")
    {
      @Override
      protected void onSave()
      {
        task.setDuration(ganttObject.getDuration());
        WicketSupport.getTaskDao().update(task);
      }

      @Override
      protected void onReject()
      {
        ganttObject.setDuration(task.getDuration());
      }
    };
  }

  @SuppressWarnings("serial")
  private void addEndDateColumns(final Item<GanttTreeTableNode> item, final GanttTreeTableNode node,
      final GanttTask ganttObject,
      final TaskDO task)
  {
    final LocalDatePanel endDatePanel = new LocalDatePanel("endDate", new LocalDateModel(new PropertyModel<>(ganttObject, "endDate")),
            DatePanelSettings.get().withSelectProperty("endDate:" + node.getHashId()), true);
    addColumn(item, endDatePanel, "white-space: nowrap;");
    endDatePanelMap.put(ganttObject.getId(), endDatePanel);
    new RejectSaveLinksFragment("rejectSaveEndDate", item, endDatePanel, task,
        task != null ? DateTimeFormatter.instance()
            .getFormattedDate(task.getEndDate()) : "")
    {
      @Override
      protected void onSave()
      {
        task.setEndDate(ganttObject.getEndDate());
        WicketSupport.getTaskDao().update(task);
      }

      @Override
      protected void onReject()
      {
        ganttObject.setEndDate(task.getEndDate());
      }
    };
  }

  @SuppressWarnings("serial")
  private void addProgressColumns(final Item<GanttTreeTableNode> item, final GanttTreeTableNode node,
      final GanttTask ganttObject,
      final TaskDO task)
  {
    final MinMaxNumberField<Integer> progressField = new MinMaxNumberField<Integer>("progress",
        new PropertyModel<Integer>(ganttObject,
            "progress"),
        0, 100)
    {
      @Override
      public IConverter getConverter(final Class type)
      {
        return new IntegerPercentConverter(0);
      }
    };
    addColumn(item, progressField, null);
    new RejectSaveLinksFragment("rejectSaveProgress", item, progressField, task,
        task != null ? NumberHelper.getAsString(task.getProgress()) : "")
    {
      @Override
      protected void onSave()
      {
        task.setProgress(ganttObject.getProgress());
        WicketSupport.getTaskDao().update(task);
      }

      @Override
      protected void onReject()
      {
        ganttObject.setProgress(task.getProgress());
      }
    };
  }

  @SuppressWarnings("serial")
  private void addPredecessorColumns(final Item<GanttTreeTableNode> item, final GanttTreeTableNode node,
      final GanttTask ganttObject,
      final TaskDO task)
  {
    final WebMarkupContainer panel = new WebMarkupContainer("predecessor");
    addColumn(item, panel, "white-space: nowrap;");
    final GanttTask predecessor = ganttObject.getPredecessor();
    final TaskDO predecessorTask = predecessor != null
        ? TaskTree.getInstance().getTaskById((Long) predecessor.getId()) : null;
    final Label asStringLabel = new Label("asString", new Model<String>()
    {
      @Override
      public String getObject()
      {
        final GanttTask predecessor = ganttObject.getPredecessor();
        return predecessor != null ? predecessor.getTitle() : "";
      }
    });
    panel.add(asStringLabel);
    final String taskSelectProperty = "predecessorId:" + ganttObject.getId();
    final IconLinkPanel selectSubmitLink = new IconLinkPanel("select", IconType.TASK,
        new SubmitLink(IconLinkPanel.LINK_ID)
        {
          @Override
          public void onSubmit()
          {
            final TaskTreePage taskTreePage = new TaskTreePage(GanttChartEditTreeTablePanel.this, taskSelectProperty);
            if (predecessorTask != null) {
              taskTreePage.setHighlightedRowId(predecessorTask.getId()); // Preselect node for highlighting.
            } else if (task != null) {
              taskTreePage.setHighlightedRowId(task.getId()); // Preselect node for highlighting.
            }
            setResponsePage(taskTreePage);
          }
        }.setDefaultFormProcessing(false));
    selectSubmitLink.setTooltip(new ResourceModel("tooltip.selectTask"));
    panel.add(selectSubmitLink);

    final IconLinkPanel unselectSubmitLink = new IconLinkPanel("unselect", IconType.MINUS_SIGN,
        new SubmitLink(IconLinkPanel.LINK_ID)
        {
          @Override
          public void onSubmit()
          {
            ganttObject.setPredecessor(null);
          }
        }.setDefaultFormProcessing(false))
    {
      @Override
      public boolean isVisible()
      {
        return ganttObject.getPredecessor() != null;
      }
    };
    unselectSubmitLink.setTooltip(new ResourceModel("tooltip.unselectTask"));
    panel.add(unselectSubmitLink);

    new RejectSaveLinksFragment("rejectSavePredecessor", item, panel, task,
        task != null ? WicketTaskFormatter.getTaskPath(getRequestCycle(),
            task.getGanttPredecessorId()) : "")
    {
      @Override
      protected void onSave()
      {
        TaskDao taskDao = WicketSupport.getTaskDao();
        taskDao.setGanttPredecessor(task, (Long) ganttObject.getPredecessorId());
        taskDao.update(task);
      }

      @Override
      protected void onReject()
      {
        ganttObject.setPredecessor(findById(task.getGanttPredecessorId()));
      }
    };
  }

  @SuppressWarnings("serial")
  private void addPredecessorOffsetColumns(final Item<GanttTreeTableNode> item, final GanttTreeTableNode node,
      final GanttTask ganttObject,
      final TaskDO task)
  {
    final MinMaxNumberField<Integer> offsetField = new MinMaxNumberField<>("predecessorOffset",
        new PropertyModel<>(
            ganttObject, "predecessorOffset"),
        Integer.MIN_VALUE, Integer.MAX_VALUE);
    addColumn(item, offsetField, null);
    new RejectSaveLinksFragment("rejectSavePredecessorOffset", item, offsetField, task,
        task != null ? NumberHelper.getAsString(task
            .getGanttPredecessorOffset()) : "")
    {
      @Override
      protected void onSave()
      {
        task.setGanttPredecessorOffset(ganttObject.getPredecessorOffset());
        WicketSupport.getTaskDao().update(task);
      }

      @Override
      protected void onReject()
      {
        ganttObject.setPredecessorOffset(task.getGanttPredecessorOffset());
      }
    };
  }

  @SuppressWarnings("serial")
  private void addRelationTypeColumns(final Item<GanttTreeTableNode> item, final GanttTreeTableNode node,
      final GanttTask ganttObject,
      final TaskDO task)
  {
    final LabelValueChoiceRenderer<GanttRelationType> relationTypeChoiceRenderer = new LabelValueChoiceRenderer<>();
    relationTypeChoiceRenderer.addValue(GanttRelationType.START_START,
        getString(GanttRelationType.START_START.getI18nKey() + ".short"));
    relationTypeChoiceRenderer.addValue(GanttRelationType.START_FINISH,
        getString(GanttRelationType.START_FINISH.getI18nKey() + ".short"));
    relationTypeChoiceRenderer.addValue(GanttRelationType.FINISH_START,
        getString(GanttRelationType.FINISH_START.getI18nKey() + ".short"));
    relationTypeChoiceRenderer
        .addValue(GanttRelationType.FINISH_FINISH, getString(GanttRelationType.FINISH_FINISH.getI18nKey() + ".short"));
    final DropDownChoice<GanttRelationType> relationTypeChoice = new DropDownChoice<>("relationType",
        new PropertyModel<>(ganttObject, "relationType"), relationTypeChoiceRenderer.getValues(),
        relationTypeChoiceRenderer);
    relationTypeChoice.setNullValid(true);
    addColumn(item, relationTypeChoice, null);
    final GanttRelationType relationType = task != null ? task.getGanttRelationType() : null;
    new RejectSaveLinksFragment("rejectSaveRelationType", item, relationTypeChoice, task,
        relationType != null ? getString(relationType.getI18nKey()) : "")
    {
      @Override
      protected void onSave()
      {
        task.setGanttRelationType(ganttObject.getRelationType());
        WicketSupport.getTaskDao().update(task);
      }

      @Override
      protected void onReject()
      {
        ganttObject.setRelationType(task.getGanttRelationType());
      }
    };
  }

  @SuppressWarnings("serial")
  private void addTypeColumns(final Item<GanttTreeTableNode> item, final GanttTreeTableNode node,
      final GanttTask ganttObject,
      final TaskDO task)
  {
    final LabelValueChoiceRenderer<GanttObjectType> objectTypeChoiceRenderer = new LabelValueChoiceRenderer<>();
    objectTypeChoiceRenderer.addValue(GanttObjectType.ACTIVITY,
        getString(GanttObjectType.ACTIVITY.getI18nKey() + ".short"));
    objectTypeChoiceRenderer.addValue(GanttObjectType.MILESTONE,
        getString(GanttObjectType.MILESTONE.getI18nKey() + ".short"));
    objectTypeChoiceRenderer.addValue(GanttObjectType.SUMMARY,
        getString(GanttObjectType.SUMMARY.getI18nKey() + ".short"));
    final DropDownChoice<GanttObjectType> objectTypeChoice = new DropDownChoice<>("type",
        new PropertyModel<>(ganttObject, "type"), objectTypeChoiceRenderer.getValues(),
        objectTypeChoiceRenderer);
    objectTypeChoice.setNullValid(true);
    addColumn(item, objectTypeChoice, null);
    final GanttObjectType type = task != null ? task.getGanttObjectType() : null;
    new RejectSaveLinksFragment("rejectSaveType", item, objectTypeChoice, task,
        type != null ? getString(type.getI18nKey()) : "")
    {
      @Override
      protected void onSave()
      {
        task.setGanttObjectType(ganttObject.getType());
        WicketSupport.getTaskDao().update(task);
      }

      @Override
      protected void onReject()
      {
        ganttObject.setType(task.getGanttObjectType());
      }
    };
  }

  /**
   * Creates an empty label with style="width: <size>px;" for reject-save-column heads and
   *
   * @return
   */
  private Component createEmtpyColumnHead(final int size)
  {
    return new Label(colHeadRepeater.newChildId(), "")
        .add(AttributeModifier.replace("style", "width: " + size + "px;"));
  }

  @Override
  protected TreeIconsActionPanel<? extends TreeTableNode> createTreeIconsActionPanel(final GanttTreeTableNode node)
  {
    throw new UnsupportedOperationException(
        "Please, don't use ajax for tree browsing (otherwise user inputs will be lost if you close trees");
  }

  @Override
  public void cancelSelection(final String property)
  {
  }

  private void markStartDateModelAsChanged(final Serializable id)
  {
    final LocalDatePanel startDatePanel = startDatePanelMap.get(id);
    if (startDatePanel != null) {
      startDatePanel.markModelAsChanged();
    } else {
      log.error("Oups, startDatePanel not found.");
    }
  }

  private void markEndDateModelAsChanged(final Serializable id)
  {
    final LocalDatePanel endDatePanel = endDatePanelMap.get(id);
    if (endDatePanel != null) {
      endDatePanel.markModelAsChanged();
    } else {
      log.error("Oups, endDatePanel not found.");
    }
  }

  @Override
  public void select(final String property, final Object selectedValue)
  {
    if (property.startsWith("startDate:")) {
      final GanttTask obj = getIndexedGanttObject(property);
      if (obj == null) {
        log.error("GanttObject not found: + " + property);
      } else {
        final LocalDate date = (LocalDate) selectedValue;
        obj.setStartDate(date);
        markStartDateModelAsChanged(obj.getId());
      }
    } else if (property.startsWith("endDate:")) {
      final GanttTask obj = getIndexedGanttObject(property);
      if (obj == null) {
        log.error("GanttObject not found: + " + property);
      } else {
        final LocalDate date = (LocalDate) selectedValue;
        obj.setEndDate(date);
        markEndDateModelAsChanged(obj.getId());
      }
    } else if (property.startsWith("predecessorId")) {
      final GanttTask obj = getIndexedGanttObject(property);
      if (obj == null) {
        log.error("GanttObject not found: + " + property);
      } else {
        final Long longValue = (Long) selectedValue;
        GanttTask predecessor = findById(longValue);
        if (predecessor != null) {
          obj.setPredecessor(predecessor);
        } else {
          // OK, maybe an external reference (meaning a reference to a task outside the current Gantt object tree.
          final TaskDO task = TaskTree.getInstance().getTaskById(longValue);
          if (task == null) {
            log.error("Task not found: + " + property);
          } else {
            predecessor = ganttChartData.ensureAndGetExternalGanttObject(task);
            obj.setPredecessor(predecessor);
          }
        }
      }
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  @Override
  public void unselect(final String property)
  {
  }

  private GanttTask findById(final Serializable id)
  {
    if (id == null || ganttChartData == null) {
      return null;
    }
    final GanttTask root = ganttChartData.getRootObject();
    if (root == null) {
      return null;
    }
    return root.findById(id);
  }

  private GanttTask getIndexedGanttObject(final String property)
  {
    final Integer id = NumberHelper.parseInteger(property.substring(property.indexOf(':') + 1));
    final GanttTask obj = ganttChartData.getRootObject().findById(id);
    if (obj == null) {
      log.error("Oups, can't find Gantt object with hash id: " + id);
    }
    return obj;
  }

  private abstract class RejectSaveLinksFragment extends Fragment
  {
    private static final long serialVersionUID = 2462093138788881814L;

    private final IconLinkPanel rejectSubmitLink;

    private final IconLinkPanel saveSubmitLink;

    @SuppressWarnings("unused")
    private final Component dataComponent;

    private boolean hasTaskUpdateAccess = true;

    protected abstract void onReject();

    protected abstract void onSave();

    @SuppressWarnings("serial")
    private RejectSaveLinksFragment(final String id, final WebMarkupContainer parent, final Component dataComponent,
        final TaskDO task,
        final String taskValueAsString)
    {
      super(id, "rejectSaveFragment", GanttChartEditTreeTablePanel.this);
      if (task != null) {
        hasTaskUpdateAccess = WicketSupport.getTaskDao().hasLoggedInUserUpdateAccess(task, task, false);
      }
      this.dataComponent = dataComponent;
      addColumn(parent, this, "white-space: nowrap; width: 32px;");
      rejectSubmitLink = new IconLinkPanel("reject", IconType.DENY, new SubmitLink(IconLinkPanel.LINK_ID, form)
      {
        @Override
        public void onSubmit()
        {
          onReject();
          if (dataComponent instanceof DatePanel) {
            ((DatePanel) dataComponent).markModelAsChanged();
          } else {
            dataComponent.modelChanged();
          }
        }
      }.setDefaultFormProcessing(false)).setColor(CSSColor.RED);
      rejectSubmitLink.setTooltip(
          Model.of(ThreadLocalUserContext.getLocalizedMessage("gantt.tooltip.rejectValue", taskValueAsString)));
      add(rejectSubmitLink);
      saveSubmitLink = new IconLinkPanel("save", IconType.ACCEPT, new SubmitLink(IconLinkPanel.LINK_ID, form)
      {
        @Override
        public void onSubmit()
        {
          onSave();
        }
      }.setDefaultFormProcessing(false)).setColor(CSSColor.GREEN);
      add(saveSubmitLink);

    }

    private void setIconsVisible(final boolean visible)
    {
      rejectSubmitLink.setVisible(visible);
      if (!hasTaskUpdateAccess) {
        saveSubmitLink.setVisible(false);
      } else {
        saveSubmitLink.setVisible(visible);
      }
    }
  }
}
