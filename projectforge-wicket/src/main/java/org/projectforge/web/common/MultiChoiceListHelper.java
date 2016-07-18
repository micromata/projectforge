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

package org.projectforge.web.common;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class is an helper class for supporting the implementation of gui lists. The user can select and unselect entries. This will be
 * needed e. g. for assigning and unassigning user to one group.<br>
 * Finally, after the user has made his decisions (multiple assigning and/or multiple unassigning), this class will return the elements to
 * (un)assign by comparing with the original assigned values.
 */
public class MultiChoiceListHelper<T> implements Serializable
{
  private static final long serialVersionUID = 3522022033150328877L;

  private Collection<T> assignedItems;

  private Collection<T> originalAssignedList;

  private Collection<T> fullList;

  private Comparator<T> comparator;

  public MultiChoiceListHelper()
  {
  }

  /**
   * Initializes the lists.
   * @param fullList List of all elements available for (un)assigning.
   * @param assignedKeys List of already assigned elements (by key) or null if no elements assigned.
   */
  public MultiChoiceListHelper(final Set<T> fullList, final SortedSet<T> assignedItems)
  {
    this.fullList = fullList;
    this.assignedItems = assignedItems;
    this.originalAssignedList = assignedItems;
  }

  /**
   * @param fullList the fullList to set
   * @return this for chaining.
   */
  public MultiChoiceListHelper<T> setFullList(final Collection<T> fullList)
  {
    this.fullList = fullList;
    return this;
  }

  /**
   * @param originalAssignedList the originalAssignedList to set
   * @return this for chaining.
   */
  public MultiChoiceListHelper<T> setOriginalAssignedList(final Collection<T> originalAssignedList)
  {
    this.originalAssignedList = originalAssignedList;
    return this;
  }

  /**
   * Only for use in construction phase.
   * @param item
   * @return
   */
  public MultiChoiceListHelper<T> addOriginalAssignedItem(final T item)
  {
    if (this.originalAssignedList == null) {
      if (comparator != null) {
        this.originalAssignedList = new TreeSet<T>(comparator);
      } else {
        this.originalAssignedList = new TreeSet<T>();
      }
    }
    this.originalAssignedList.add(item);
    return this;
  }

  /**
   * @param comparator the comparator to set
   * @return this for chaining.
   */
  public MultiChoiceListHelper<T> setComparator(final Comparator<T> comparator)
  {
    this.comparator = comparator;
    return this;
  }

  /**
   * @return the comparator
   */
  public Comparator<T> getComparator()
  {
    return comparator;
  }

  /**
   * @return the assignedItems
   */
  public Collection<T> getAssignedItems()
  {
    return assignedItems;
  }

  public MultiChoiceListHelper<T> setAssignedItems(final Collection<T> assignedItems)
  {
    this.assignedItems = assignedItems;
    return this;
  }

  public MultiChoiceListHelper<T> assignItem(final T item)
  {
    if (this.assignedItems == null) {
      if (comparator != null) {
        this.assignedItems = new TreeSet<T>(comparator);
      } else {
        this.assignedItems = new TreeSet<T>();
      }
    }
    this.assignedItems.add(item);
    return this;
  }

  /**
   * @return the fullList
   */
  public Collection<T> getFullList()
  {
    return fullList;
  }

  public Set<T> getItemsToAssign()
  {
    final Set<T> result = new HashSet<T>();
    if (assignedItems == null) {
      return result;
    }
    for (final T item : assignedItems) {
      if (originalAssignedList == null || originalAssignedList.contains(item) == false) {
        result.add(item);
      }
    }
    return result;
  }

  public Set<T> getItemsToUnassign()
  {
    final Set<T> result = new HashSet<T>();
    if (originalAssignedList == null) {
      return result;
    }
    for (final T item : originalAssignedList) {
      if (assignedItems.contains(item) == false) {
        result.add(item);
      }
    }
    return result;
  }
}
