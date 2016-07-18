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

package org.projectforge.framework.persistence.utils;

import java.util.List;

import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.entities.AbstractBaseDO;

/**
 * Some helper methods ...
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public abstract class ListCopyHelper<T>
{
  /**
   * @param src
   * @param dest
   * @return true, if any modification was detected, false if src and dest are equal.
   */
  public ModificationStatus copy(final List<T> srcList, final List<T> destList, final Object... objects)
  {
    final int srcSize = srcList != null ? srcList.size() : 0;
    final int destSize = destList != null ? destList.size() : 0;
    int index = 0;
    ModificationStatus modStatus = ModificationStatus.NONE;
    do {
      if (index < srcSize) {
        final T srcEntry = srcList.get(index);
        if (index < destSize) {
          final T destEntry = destList.get(index);
          final ModificationStatus st = copyFrom(srcEntry, destEntry, objects);
          modStatus = AbstractBaseDO.getModificationStatus(modStatus, st);
        } else {
          appendDestEntry(destList, srcEntry, objects);
          modStatus = ModificationStatus.MAJOR;
        }
      } else if (index < destSize) {
        final T destEntry = destList.get(index);
        removeDestEntry(destList, destEntry, index, objects);
        modStatus = ModificationStatus.MAJOR;
      } else {
        break;
      }
    } while (++index <= srcSize || index <= destSize); // Paranoia setting: endless loop protection
    return modStatus;
  }

  protected abstract ModificationStatus copyFrom(T srcEntry, T destEntry, final Object... objects);

  protected abstract void appendDestEntry(final List<T> destList, final T srcEntry, final Object... objects);

  protected abstract void removeDestEntry(final List<T> destList, final T destEntry, final int pos, final Object... objects);
}
