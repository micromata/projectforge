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

package org.projectforge.framework.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

/**
 * For storing recent entries for selecting as templates by the user (recent time sheets, task etc.)
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class RecentQueue<T> implements Serializable
{
  private static final long serialVersionUID = -8952140806622373237L;

  protected int maxSize = 25;

  protected List<T> recents;

  public RecentQueue()
  {
  }

  public RecentQueue(int maxSize)
  {
    this.maxSize = maxSize;
  }

  /**
   * Does not throw IndexOutOfBoundsException.
   * @param pos
   * @return Entry of recent list if exist, otherwise null.
   */
  public T get(Integer pos)
  {
    if (CollectionUtils.isEmpty(recents) == true) {
      return null;
    }
    if (pos == null) {
      return recents.get(0);
    } else if (pos >= 0 && pos < recents.size()) {
      return recents.get(pos);
    }
    return null;
  }

  /**
   * Adds the entry to the list without any max size checking (useful for initialization a new queue).
   * @param entry
   */
  public void addOnly(T entry)
  {
    synchronized (this) {
      if (recents == null) {
        recents = new ArrayList<T>();
      }
    }
    if (recents.indexOf(entry) == -1) {
      recents.add(entry);
    }
  }

  public RecentQueue<T> append(T entry)
  {
    synchronized (this) {
      if (recents == null) {
        recents = new ArrayList<T>();
      }
    }
    int idx = recents.indexOf(entry);
    if (idx >= 0) {
      // Prevent duplicate entry:
      recents.remove(idx);
    }
    while (recents.size() >= maxSize && recents.size() > 0) {
      recents.remove(recents.size() - 1);
    }
    recents.add(0, entry);
    return this;
  }

  public List<T> getRecents()
  {
    return recents;
  }

  public void setRecents(List<T> recents)
  {
    this.recents = recents;
  }

  public void setMaxSize(int maxSize)
  {
    this.maxSize = maxSize;
  }

  public int size()
  {
    if (recents == null) {
      return 0;
    }
    return recents.size();
  }
}
