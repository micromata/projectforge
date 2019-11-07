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

import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import java.io.Serializable;
import java.util.*;

/**
 * @author Billy Duong (b.duong@micromata.de)
 *
 */
public class SkillTreeProvider implements ITreeProvider<SkillNode>
{

  private static final long serialVersionUID = 7692282103462630402L;

  private transient SkillDao skillDao;

  private final SkillFilter skillFilter;

  private boolean showRootNode;

  public SkillTreeProvider(SkillDao skillDao, final SkillFilter skillFilter)
  {
    this.skillFilter = skillFilter;
    this.skillDao = skillDao;
    skillFilter.resetMatch();
  }

  /**
   * Nothing to do.
   *
   * @see org.apache.wicket.model.IDetachable#detach()
   */
  @Override
  public void detach()
  {
  }

  /**
   * @see org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider#getRoots()
   */
  @Override
  public Iterator<SkillNode> getRoots()
  {
    if (getSkillTree().getRootSkillNode() == null) {
      // Force a refresh to load the root skill node
      getSkillTree().refresh();
    }
    return iterator(getSkillTree().getRootSkillNode().getChildren(), showRootNode);
  }

  /**
   * @see org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider#hasChildren(java.lang.Object)
   */
  @Override
  public boolean hasChildren(final SkillNode node)
  {
    if (node.isRootNode()) {
      // Don't show children of root node again.
      return false;
    }
    return node.hasChildren();
  }

  /**
   * @see org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider#getChildren(java.lang.Object)
   */
  @Override
  public Iterator<SkillNode> getChildren(final SkillNode node)
  {
    if (node.isRootNode()) {
      // Don't show children of root node again.
      return new LinkedList<SkillNode>().iterator();
    }
    return iterator(node.getChildren());
  }

  /**
   * @see org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider#model(java.lang.Object)
   */
  @Override
  public IModel<SkillNode> model(final SkillNode object)
  {
    return new SkillNodeModel(skillDao, object);
  }

  private Iterator<SkillNode> iterator(final List<SkillNode> nodes)
  {
    return iterator(nodes, false);
  }

  private Iterator<SkillNode> iterator(final List<SkillNode> nodes, final boolean appendRootNode)
  {
    // ensureSkillTree();
    final SortedSet<SkillNode> list = new TreeSet<>(new Comparator<SkillNode>() {
      /**
       * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
       */
      @Override
      public int compare(final SkillNode skillNode1, final SkillNode skillNode2) {
        if (skillNode1.isRootNode()) {
          // Show root node at last position.
          return 1;
        }
        if (skillNode2.isRootNode()) {
          // Show root node at last position.
          return -1;
        }
        String title1 = skillNode1.getSkill().getTitle();
        title1 = title1 != null ? title1.toLowerCase() : "";
        String title2 = skillNode2.getSkill().getTitle();
        title2 = title2 != null ? title2.toLowerCase() : "";
        return title1.compareTo(title2);
      }
    });
    if (appendRootNode) {
      if (skillFilter.match(getSkillTree().getRootSkillNode(), null, null)) {
        list.add(getSkillTree().getRootSkillNode());
      }
    }
    if (nodes == null || nodes.isEmpty()) {
      return list.iterator();
    }
    final PFUserDO user = ThreadLocalUserContext.getUser();
    for (final SkillNode node : nodes) {

      final boolean isMatch = skillFilter.match(node, skillDao, user);
      final boolean hasAccess = skillDao.hasUserSelectAccess(user, node.getSkill(), false);

      if (isMatch && hasAccess) {
        list.add(node);
      }
    }
    return list.iterator();
  }

  /**
   * @param showRootNode the showRootNode to set
   * @return this for chaining.
   */
  public SkillTreeProvider setShowRootNode(final boolean showRootNode)
  {
    this.showRootNode = showRootNode;
    return this;
  }

  /**
   * @return the skillTree
   */
  private SkillTree getSkillTree()
  {
    return skillDao.getSkillTree();
  }

  /**
   * A {@link Model} which uses an id to load its {@link Foo}.
   *
   * If {@link Foo}s were {@link Serializable} you could just use a standard {@link Model}.
   *
   * @see #equals(Object)
   * @see #hashCode()
   */
  private static class SkillNodeModel extends LoadableDetachableModel<SkillNode>
  {
    private static final long serialVersionUID = 1L;

    private final Integer id;

    private transient SkillTree skillTree;

    private SkillDao skillDao;

    public SkillNodeModel(SkillDao skillDao, final SkillNode skillNode)
    {
      super(skillNode);
      id = skillNode.getId();
      this.skillDao = skillDao;
    }

    @Override
    protected SkillNode load()
    {
      if (skillTree == null) {
        skillTree = skillDao.getSkillTree();
      }
      return skillTree.getSkillNodeById(id);
    }

    /**
     * Important! Models must be identifyable by their contained object.
     */
    @Override
    public boolean equals(final Object obj)
    {
      if (obj instanceof SkillNodeModel) {
        return ((SkillNodeModel) obj).id.equals(id);
      }
      return false;
    }

    /**
     * Important! Models must be identifyable by their contained object.
     */
    @Override
    public int hashCode()
    {
      return id.hashCode();
    }
  }

}
