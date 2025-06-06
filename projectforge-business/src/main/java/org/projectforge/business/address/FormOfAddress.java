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

package org.projectforge.business.address;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.common.i18n.I18nEnum;

public enum FormOfAddress implements I18nEnum
{
  MISTER("mister"), MISS("miss"), COMPANY("company"), MISC("misc"), UNKNOWN("unknown");

  private String key;

  public static FormOfAddress get(String s)
  {
    if (StringUtils.isEmpty(s)) {
      return null;
    }
    if ("MISTER".equals(s)) {
      return MISTER;
    } else if ("MISS".equals(s)) {
      return MISS;
    } else if ("COMPANY".equals(s)) {
      return COMPANY;
    } else if ("MISC".equals(s)) {
      return MISC;
    } else if ("UNKNOWN".equals(s)) {
      return UNKNOWN;
    }
    throw new UnsupportedOperationException("Unknown Anrede" + ": '" + s + "'");
  }

  public boolean isIn(final FormOfAddress... forms)
  {
    for (FormOfAddress form : forms) {
      if (this == form) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return The full i18n key including the i18n prefix "fibu.auftrag.status.".
   */
  @Override
  public String getI18nKey()
  {
    return "address.form." + key;
  }

  FormOfAddress(String key)
  {
    this.key = key;
  }
}
