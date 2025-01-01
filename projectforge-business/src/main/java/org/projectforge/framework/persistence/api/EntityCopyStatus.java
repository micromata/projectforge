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

package org.projectforge.framework.persistence.api;


/**
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public enum EntityCopyStatus
{
  /** Object isn't modified. */
  NONE(0),
  /**
   * Modification is minor (e. g. only nonhistorizable attributes are modified). Therefore no history entries will be
   * written.
   */
  MINOR(1),
  /** Modification of object (e. g. normal case). see MINOR. */
  MAJOR(2)

  ;
  private int level;

  EntityCopyStatus(int level)
  {
    this.level = level;
  }

  /**
   * combine modification status. Mayer wins over minor.
   *
   * @param other
   * @return
   */
  public EntityCopyStatus combine(EntityCopyStatus other)
  {
    return level < other.level ? other : this;
  }
}
