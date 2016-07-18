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
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class TreeTableNode implements Comparable<TreeTableNode>, Serializable
{
  private static final long serialVersionUID = -593742577621084162L;

  /**
   * Every node must have an unique id for handling events by posting id as parameter. Every node will be stored in an HashMap of TableTree
   * for faster finding.
   */
  Serializable hashId;

  /** The indent counter for transparent.gif. */
  protected int indent;

  /** Is element of {FOLDER, FOLDER_OPEN, LEAF}. */
  protected NodeStatus nodeStatus = NodeStatus.LEAF;

  /** Reference to the parent node. */
  protected TreeTableNode parent;

  /**
   * References to all child nodes in an ArrayList from element type TreeTableNode.
   */
  SortedSet<TreeTableNode> childs = null;

  /** Returns the parent node. */
  public TreeTableNode getParent()
  {
    return parent;
  }

  public int getIndent()
  {
    return indent;
  }

  /**
   * Returns all childs of this node.
   */
  public SortedSet<TreeTableNode> getChilds()
  {
    return childs;
  }

  /** Returns the hashId of this node. */
  public Serializable getHashId()
  {
    return hashId;
  }

  /** Gets the nodeStatus of this node (FOLDER, FOLDER_OPEN, LEAF). */
  public NodeStatus getNodeStatus()
  {
    return nodeStatus;
  }

  /** Needed by jsp. */
  public boolean isFolder()
  {
    return nodeStatus == NodeStatus.FOLDER;
  }

  /** Needed by jsp. */
  public boolean isOpenFolder()
  {
    return nodeStatus == NodeStatus.FOLDER_OPEN;
  }

  /** Needed by jsp. */
  public boolean isLeaf()
  {
    return nodeStatus == NodeStatus.LEAF;
  }

  /** Needed by jsp. */
  public boolean getHasChilds()
  {
    return hasChilds();
  }

  /** Has this node any childs? */
  public boolean hasChilds()
  {
    if (childs == null || childs.size() == 0) {
      return false;
    }
    return true;
  }

  /** Checks if the given node is a child / descendant of this node. */
  public boolean isParentOf(final TreeTableNode node)
  {
    if (this.childs == null) {
      return false;
    }
    for (final TreeTableNode child : this.childs) {
      if (child.equals(node) == true) {
        return true;
      } else if (child.isParentOf(node) == true) {
        return true;
      }
    }
    return false;
  }

  /**
   * Are all childs already opened? This will be used for handling event explore. If the user clicks on explore event twice, all childs will
   * be recursively closed (implored).
   * @return If all childs are recursively open.
   */
  protected boolean allChildsOpened()
  {
    if (hasChilds() == false) {
      return true;
    }
    if (nodeStatus == NodeStatus.FOLDER) {
      return false; // Folder is closed.
    }
    for (TreeTableNode child : childs) {
      if (child.allChildsOpened() == false) {
        return false;
      }
    }
    return true;
  }

  /**
   * Only for deserialization.
   */
  protected TreeTableNode()
  {

  }

  /**
   * @param parent The parent node.
   * @param hashId The hashId to use.
   */
  protected TreeTableNode(TreeTableNode parent, Serializable hashId)
  {
    this.parent = parent;
    this.hashId = hashId;
  }

  public boolean isOpened()
  {
    return this.nodeStatus == NodeStatus.FOLDER_OPEN;
  }

  void setOpened(boolean isOpened)
  {
    if (this.nodeStatus == NodeStatus.LEAF) {
      // Do nothing.
      return;
    }
    if (isOpened) {
      this.nodeStatus = NodeStatus.FOLDER_OPEN;
    } else {
      this.nodeStatus = NodeStatus.FOLDER;
    }
  }

  protected void buildNodeList(List<TreeTableNode> nodes, int indent, TreeTableFilter<TreeTableNode> filter)
  {
    if (filter != null && filter.match(this) == false) {
      return;
    }
    this.indent = indent;
    if (hasChilds() == false) {
      nodeStatus = NodeStatus.LEAF;
      nodes.add(this);
      return;
    }
    nodes.add(this);
    if (isOpened() == true) {
      indent++;
      if (childs != null) {
        for (TreeTableNode node : childs) {
          node.buildNodeList(nodes, indent, filter);
        }
      }
    }
    return;
  }

  /**
   * Returns the path to the root node in an ArrayList. The elements are from type TreeTableNode.
   */
  public List<TreeTableNode> getPathToRoot()
  {
    if (parent == null) {
      return new ArrayList<TreeTableNode>();
    }
    List<TreeTableNode> path = parent.getPathToRoot();
    path.add(this);
    return path;
  }

  /**
   * Folder will be shown before leafs. If the TreeTableNode derived from this class implements the _compareTo(TreeTableNode) method, then
   * the folders and leafs are sortable themselves e. g. in alphabetical order.
   */
  public int compareTo(TreeTableNode node)
  {
    if (hasChilds() == true) {
      if (node.hasChilds() == false)
        return -1; // Folders before leafs.
      return compareTo(node);
    }
    if (node.hasChilds() == false)
      return compareTo(node);
    return 1;
  }

  /**
   * Adds a new child node. It does not check wether this node already exist as child or not! This method does not modify the child task!
   */
  protected void addChild(TreeTableNode child)
  {
    if (childs == null) {
      childs = new TreeSet<TreeTableNode>();
      nodeStatus = NodeStatus.FOLDER;
    }
    childs.add(child);
  }
}
