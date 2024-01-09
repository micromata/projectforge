/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.collections4.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * For storing recent entries for selecting as templates by the user (recent time sheets, task etc.)
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class RecentQueue<T> implements Serializable
{
  private static final long serialVersionUID = -8952140806622373237L;

  protected int maxSize = 25;

  @XStreamAlias("recents") // Was named recents in former version (before 2020-04-05.
  protected List<T> recentList;

  public RecentQueue()
  {
  }

  public RecentQueue(int maxSize)
  {
    this.maxSize = maxSize;
  }

  /**
   * Get the recent entries (the newest). Is equivalent to get(0).
   */
  public T getRecent() {
    return get(0);
  }

  /**
   * Does not throw IndexOutOfBoundsException.
   * @param pos
   * @return Entry of recent list if exist, otherwise null.
   */
  public T get(Integer pos)
  {
    if (CollectionUtils.isEmpty(recentList)) {
      return null;
    }
    if (pos == null) {
      return recentList.get(0);
    } else if (pos >= 0 && pos < recentList.size()) {
      return recentList.get(pos);
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
      if (recentList == null) {
        recentList = new ArrayList<>();
      }
    }
    if (recentList.indexOf(entry) == -1) {
      recentList.add(entry);
    }
  }

  public RecentQueue<T> append(T entry)
  {
    synchronized (this) {
      if (recentList == null) {
        recentList = new ArrayList<>();
      }
    }
    int idx = recentList.indexOf(entry);
    if (idx >= 0) {
      // Prevent duplicate entry:
      recentList.remove(idx);
    }
    while (recentList.size() >= maxSize && recentList.size() > 0) {
      recentList.remove(recentList.size() - 1);
    }
    recentList.add(0, entry);
    return this;
  }

  public List<T> getRecentList()
  {
    return recentList;
  }

  public void setRecentList(List<T> recentList)
  {
    this.recentList = recentList;
  }

  public void setMaxSize(int maxSize)
  {
    this.maxSize = maxSize;
  }

  public int size()
  {
    if (recentList == null) {
      return 0;
    }
    return recentList.size();
  }
}
