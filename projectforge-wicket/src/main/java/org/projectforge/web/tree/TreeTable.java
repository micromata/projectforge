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

package org.projectforge.web.tree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * A TreeTable is a really nice tree view which entries will be shown as HTML table rows. The functionality of this tree is like a file
 * manager with tree view. It's much more than the source code looks like ;-) <br>
 * Attention: This implementation of a tree isn't thread safe, because modification will be done only by one thread.
 * @see TreeTableNode
 */
public abstract class TreeTable<T extends TreeTableNode> implements Serializable
{
  private static final long serialVersionUID = -8178813483140004622L;

  /** The root node. */
  protected T root;

  /**
   * All opened nodes will be registered with their hashIds in an HashSet, so after reloading data the open node history should be restored.
   */
  private Set<Serializable> openedNodes = new HashSet<Serializable>();

  /** For faster finding containing nodes by hashId. */
  protected Map<Serializable, T> allNodes = new HashMap<Serializable, T>();

  /**
   * @return True, if at least the root node is given.
   */
  public boolean isInitialized()
  {
    return this.root != null;
  }

  /**
   * Gets nodes as list. For the first time, only the top level childs will be shown. After the user is able to open and close some entries
   * like in an file manager tree view.
   */
  @SuppressWarnings("unchecked")
  public List<T> getNodeList(TreeTableFilter<TreeTableNode> filter)
  {
    List<T> nodes = new ArrayList<T>();
    if (root == null) {
      return null;
    }
    SortedSet<T> childs = (SortedSet<T>) root.getChilds();
    if (childs != null) {
      for (T node : childs) {
        node.buildNodeList((List<TreeTableNode>) nodes, 0, filter);
      }
    }
    return nodes;
  }

  /**
   * Gets all available nodes as list.
   */
  public List<T> getAllNodes()
  {
    return getNodeList(null);
  }

  /**
   * Called after user has clicked on folder / folder open icon.
   * @param hashId The hashID of the node for which the event was released.
   * @param eventKey If "open" then the node will be opened if already closed, otherwise the node will be opened if already closed.
   * @return Found TreeNode by hashId, otherwise null.
   */
  public TreeTableNode setOpenedStatusOfNode(String eventKey, Serializable hashId)
  {
    if ("explore".equals(eventKey) == true) {
      return setOpenedStatusOfNode(TreeTableEvent.EXPLORE, hashId);
    } else if ("implore".equals(eventKey) == true) {
      return setOpenedStatusOfNode(TreeTableEvent.IMPLORE, hashId);
    } else if ("open".equals(eventKey) == true) {
      return setOpenedStatusOfNode(TreeTableEvent.OPEN, hashId);
    } else {
      return setOpenedStatusOfNode(TreeTableEvent.CLOSE, hashId);
    }
  }

  /**
   * Called after user has clicked on folder / folder open icon.
   * @param hashId The hashID of the node for which the event was released.
   * @param eventKey If "open" then the node will be opened if already closed, otherwise the node will be opened if already closed.
   * @return Found TreeNode by hashId, otherwise null.
   */
  public TreeTableNode setOpenedStatusOfNode(TreeTableEvent event, Serializable hashId)
  {
    T node = allNodes.get(hashId);
    if (node == null) {
      return null;
    }
    if (event == TreeTableEvent.EXPLORE) {
      if (node.isOpened() == true) {
        closeFoldersRecursive(node);
      } else {
        openFoldersRecursive(node);
      }
    } else if (event == TreeTableEvent.IMPLORE) {
      if (node.hasChilds() == true)
        closeFoldersRecursive(node);
    } else if (event == TreeTableEvent.OPEN) {
      node.setOpened(true);
      openedNodes.add(node.hashId);
    } else {
      openedNodes.remove(node.hashId);
      node.setOpened(false);
    }
    return node;
  }

  /**
   * Opens the whole path to the given node (set open status of all ancestors to true).
   * @param hashId
   * @return
   */
  public void openNode(Serializable hashId)
  {
    T node = allNodes.get(hashId);
    if (node == null) {
      return;
    }
    TreeTableNode parent = node.getParent();
    while (parent != null) {
      parent.setOpened(true);
      parent = parent.getParent();
    }
  }

  /**
   * Get open nodes e. g. for persistent storing in the user's preferences.
   * @return
   */
  public Set<Serializable> getOpenNodes()
  {
    return this.openedNodes;
  }

  /**
   * Set open nodes e. g. after restoring from the user's preferences.
   * @param openNodes
   */
  public void setOpenNodes(Set<Serializable> openNodes)
  {
    if (openNodes == null) {
      this.openedNodes = new HashSet<Serializable>();
    } else {
      this.openedNodes = openNodes;
    }
    updateOpenStatus();
  }

  /**
   * Update opened status of all tasks, given by openedNodes set.
   */
  protected void updateOpenStatus()
  {
    for (TreeTableNode node : allNodes.values()) {
      if (this.openedNodes.contains(node.getHashId()) == true) {
        node.setOpened(true);
      } else {
        node.setOpened(false);
      }
    }
  }

  void openFoldersRecursive(TreeTableNode node)
  {
    node.setOpened(true);
    openedNodes.add(node.hashId);
    if (node.hasChilds() == true) {
      node.getChilds();
      for (TreeTableNode n : node.getChilds()) {
        openFoldersRecursive(n);
      }
    }
  }

  void closeFoldersRecursive(TreeTableNode node)
  {
    node.setOpened(false);
    openedNodes.remove(node.hashId);
    if (node.hasChilds() == true) {
      for (TreeTableNode n : node.getChilds()) {
        closeFoldersRecursive(n);
      }
    }
  }

  /**
   * Adds a new node by putting it into to the HashMap holding all nodes for faster finding by hashMap.
   */
  protected void addTreeTableNode(T node)
  {
    TreeTableNode parent = node.getParent();
    if (parent != null) {
      parent.addChild(node);
    } else {
      root.addChild(node);
    }
    allNodes.put(node.hashId, node);
  }

  protected T getNode(Serializable hashId)
  {
    return allNodes.get(hashId);
  }

  /**
   * Does the node specified by given hashID is an opened node (folder)? (Tests the existing of the given node in the HashMap of all opened
   * nodes.)
   * @param hashId The hashID of the node to examine.
   * @return True, if the specified node (folder) is open, otherwise false.
   */
  boolean containsOpenedNode(Serializable hashId)
  {
    return openedNodes.contains(hashId);
  }

  /**
   * Helper method for getting the element after the given node (referenced by id) in the given node list which is not a child node!. This
   * can be the next sibling, the parent's successor or null if the node is the last node in the tree table. <br/>
   * This method is used by the Ajax implementations of the trees for manipulating the DOM tree.
   * @param hashId
   */
  public T getElementAfter(final List<T> nodeList, final Serializable hashId)
  {
    final Iterator<T> it = nodeList.iterator();
    TreeTableNode node = null;
    // Find node:
    while (it.hasNext() == true) {
      node = it.next();
      if (node.getHashId().equals(hashId) == true) {
        break;
      }
    }
    if (node == null) {
      return null;
    }
    // Find successive node which is not child of current node:
    while (it.hasNext() == true) {
      final T suc = it.next();
      if (node.isParentOf(suc) == false) {
        return suc;
      }
    }
    return null;
  }

  /**
   * Helper method.
   * @param nodeList
   * @param treeTableNode
   */
  public List<T> getDescendants(final List<T> nodeList, final T treeTableNode)
  {
    final List<T> result = new ArrayList<T>();
    boolean before = true;
    for (final T entry : nodeList) {
      if (before == true) {
        if (entry.getHashId().equals(treeTableNode.getHashId()) == true) {
          before = false;
        }
      } else {
        if (treeTableNode.isParentOf(entry) == true) {
          result.add(entry);
        } else {
          break; // End of descendants.
        }
      }
    }
    return result;
  }
}
