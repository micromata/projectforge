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

package org.projectforge.business.common;

/**
 * @author Kai Reinhard (k.reinhard@me.de)
 */
public enum DataobjectAccessType {
  OWNER, FULL, READONLY, MINIMAL, NONE;

  /**
   * @return true, if equals to OWNER, FULL, READONLY or MINIMAL, false otherwise (NONE).
   */
  public boolean hasAnyAccess() {
    return this != NONE;
  }

  /**
   * @return true, if equals to OWNER or FULL, false otherwies.
   */
  public boolean hasFullAccess() {
    return this == OWNER || this == FULL;
  }

  public boolean isIn(final DataobjectAccessType... types) {
    for (final DataobjectAccessType type : types) {
      if (this == type) {
        return true;
      }
    }
    return false;
  }
}
