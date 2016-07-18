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

package org.projectforge.framework.utils;

import org.projectforge.common.i18n.I18nEnum;


/**
 * The status of a result holder object.
 */
public enum ResultHolderStatus implements I18nEnum
{
  OK("ok"), WARNING("warning"), ERROR("error"), FAILED("failed");

  private String key;

  @Override
  public String getI18nKey()
  {
    return "common.resultholder." + key;
  }

  /**
   * The key will be used e. g. for i18n.
   * @return
   */
  public String getKey()
  {
    return key;
  }

  ResultHolderStatus(final String key)
  {
    this.key = key;
  }

  public boolean isIn(final ResultHolderStatus... status) {
    for (final ResultHolderStatus st : status) {
      if (this == st) {
        return true;
      }
    }
    return false;
  }
}
