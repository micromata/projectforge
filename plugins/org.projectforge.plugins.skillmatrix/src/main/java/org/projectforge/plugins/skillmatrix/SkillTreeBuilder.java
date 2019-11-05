/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.skillmatrix;

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
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractSecuredPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.ListSelectActionPanel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Billy Duong (b.duong@micromata.de)
 */
public class SkillTreeBuilder implements Serializable
{
  private static final long serialVersionUID = -5283360078497855756L;

  private final Behavior theme = new WindowsTheme();

  private TableTree<SkillNode, String> tree;

  private AbstractSecuredPage parentPage;

  private Integer highlightedSkillNodeId;

  private boolean selectMode, showRootNode;

  private ISelectCallerPage caller;

  private String selectProperty;

  private SkillDao skillDao;

  public SkillTreeBuilder(SkillDao skillDao)
  {
    this.skillDao = skillDao;
  }

  @SuppressWarnings("serial")
  public AbstractTree<SkillNode> createTree(final String id, final AbstractSecuredPage parentPage,
      final SkillFilter skillFilter)
  {
    this.parentPage = parentPage;
    final List<IColumn<SkillNode, String>> columns = createColumns();

    tree = new TableTree<SkillNode, String>(id, columns,
        new SkillTreeProvider(skillDao, skillFilter).setShowRootNode(showRootNode),
        Integer.MAX_VALUE, SkillTreeExpansion.getExpansionModel())
    {
      private static final long serialVersionUID = 1L;

      @Override
      protected Component newContentComponent(final String id, final IModel<SkillNode> model)
      {
        return SkillTreeBuilder.this.newContentComponent(id, this, model);
      }

      @Override
      protected Item<SkillNode> newRowItem(final String id, final int index, final IModel<SkillNode> model)
      {
        return new OddEvenItem<>(id, index, model);
      }
    };
    tree.getTable().addTopToolbar(new HeadersToolbar<>(tree.getTable(), null));
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
  private List<IColumn<SkillNode, String>> createColumns()
  {
    final CellItemListener<SkillNode> cellItemListener = new CellItemListener<SkillNode>()
    {
      @Override
      public void populateItem(final Item<ICellPopulator<SkillNode>> item, final String componentId,
          final IModel<SkillNode> rowModel)
      {
        final SkillNode skillNode = rowModel.getObject();
        SkillListPage.appendCssClasses(item, skillNode.getSkill(), highlightedSkillNodeId);
      }
    };

    final List<IColumn<SkillNode, String>> columns = new ArrayList<>();

    final TreeColumn<SkillNode, String> title = new TreeColumn<SkillNode, String>(
        new ResourceModel("plugins.skillmatrix.skill.title"))
    {
      @Override
      public void populateItem(final Item<ICellPopulator<SkillNode>> cellItem, final String componentId,
          final IModel<SkillNode> rowModel)
      {
        final RepeatingView view = new RepeatingView(componentId);
        cellItem.add(view);
        final SkillNode skillNode = rowModel.getObject();
        if (!selectMode) {
          view.add(new ListSelectActionPanel(view.newChildId(), rowModel, SkillEditPage.class, skillNode.getId(),
              parentPage, ""));
        } else {
          view.add(
              new ListSelectActionPanel(view.newChildId(), rowModel, caller, selectProperty, skillNode.getId(), ""));
        }
        //AbstractListPage.addRowClick(cellItem);
        final NodeModel<SkillNode> nodeModel = (NodeModel<SkillNode>) rowModel;
        final Component nodeComponent = getTree().newNodeComponent(view.newChildId(), nodeModel.getWrappedModel());
        nodeComponent.add(new NodeBorder(nodeModel.getBranches()));
        view.add(nodeComponent);
        cellItemListener.populateItem(cellItem, componentId, rowModel);
      }
    };

    final CellItemListenerPropertyColumn<SkillNode> created = new CellItemListenerPropertyColumn<>(
        new ResourceModel(
            "created"),
        null, "skill.created", cellItemListener);

    final CellItemListenerPropertyColumn<SkillNode> lastUpdate = new CellItemListenerPropertyColumn<>(
        new ResourceModel(
            "lastUpdate"),
        null, "skill.lastUpdate", cellItemListener);

    final CellItemListenerPropertyColumn<SkillNode> description = new CellItemListenerPropertyColumn<>(
        new ResourceModel(
            "plugins.skillmatrix.skill.description"),
        null, "skill.description", cellItemListener);

    final CellItemListenerPropertyColumn<SkillNode> comment = new CellItemListenerPropertyColumn<>(
        new ResourceModel(
            "plugins.skillmatrix.skill.comment"),
        null, "skill.comment", cellItemListener);

    final CellItemListenerPropertyColumn<SkillNode> rateable = new CellItemListenerPropertyColumn<>(
        new ResourceModel(
            "plugins.skillmatrix.skill.rateable"),
        null, "skill.rateable", cellItemListener);

    columns.add(title);
    columns.add(description);
    columns.add(comment);
    columns.add(rateable);
    columns.add(created);
    columns.add(lastUpdate);

    return columns;
  }

  protected void addColumn(final WebMarkupContainer parent, final Component component, final String cssStyle)
  {
    if (cssStyle != null) {
      component.add(AttributeModifier.append("style", new Model<>(cssStyle)));
    }
    parent.add(component);
  }

  /**
   * @param id
   * @param model
   * @return
   */
  @SuppressWarnings("serial")
  protected Component newContentComponent(final String id, final TableTree<SkillNode, String> tree,
      final IModel<SkillNode> model)
  {
    return new Folder<SkillNode>(id, tree, model)
    {

      @Override
      protected IModel<?> newLabelModel(final IModel<SkillNode> model)
      {
        return new PropertyModel<String>(model, "skill.title");
      }
    };
  }

  /**
   * @param selectMode the selectMode to set
   * @return this for chaining.
   */
  public SkillTreeBuilder setSelectMode(final boolean selectMode)
  {
    this.selectMode = selectMode;
    return this;
  }

  /**
   * @param showRootNode the showRootNode to set
   * @return this for chaining.
   */
  public SkillTreeBuilder setShowRootNode(final boolean showRootNode)
  {
    this.showRootNode = showRootNode;
    return this;
  }

  /**
   * @param caller the caller to set
   * @return this for chaining.
   */
  public SkillTreeBuilder setCaller(final ISelectCallerPage caller)
  {
    this.caller = caller;
    return this;
  }

  /**
   * @param selectProperty the selectProperty to set
   * @return this for chaining.
   */
  public SkillTreeBuilder setSelectProperty(final String selectProperty)
  {
    this.selectProperty = selectProperty;
    return this;
  }

  /**
   * @param highlightedSkillNodeId the highlightedSkillNodeId to set
   * @return this for chaining.
   */
  public SkillTreeBuilder setHighlightedSkillNodeId(final Integer highlightedSkillNodeId)
  {
    this.highlightedSkillNodeId = highlightedSkillNodeId;
    final SkillNode node = getSkillTree().getSkillNodeById(highlightedSkillNodeId);
    if (node == null) {
      // Shouldn't occur.
      return this;
    }
    // Open all ancestor nodes of the highlighted node:
    final Set<SkillNode> set = SkillTreeExpansion.getExpansionModel().getObject();
    SkillNode parent = node.getParent();
    while (parent != null) {
      set.add(parent);
      parent = parent.getParent();
    }
    return this;
  }

  private SkillTree getSkillTree()
  {
    return skillDao.getSkillTree();
  }

}
