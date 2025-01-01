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

package org.projectforge.framework.access;

import org.projectforge.common.i18n.I18nEnum;

/**
 * Represents the available operation types as enum.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public enum OperationType implements I18nEnum {
  SELECT("select"), INSERT("insert"), UPDATE("update"), DELETE("delete"), UNDELETE("undelete");

  private String key;

  /**
   * The key will be used e. g. for i18n.
   *
   * @return
   */
  public String toString() {
    return key;
  }

  OperationType(String key) {
    this.key = key;
  }

  /**
   * @return The full i18n key including the i18n prefix "fibu.auftrag.status.".
   */
  public String getI18nKey() {
    return "access.type." + key;
  }

  public boolean isWriteType() {
    return isIn(INSERT, UPDATE, DELETE, UNDELETE);
  }

  public boolean isReadType() {
    return isIn(SELECT);
  }

  public boolean isIn(OperationType... type) {
    for (OperationType t : type) {
      if (this == t) {
        return true;
      }
    }
    return false;
  }
}
