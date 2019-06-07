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

package org.projectforge.business.fibu.kost;

import org.projectforge.common.i18n.I18nEnum;

public enum KostentraegerStatus implements I18nEnum
{
  /** Aktiver Kostenträger */
  ACTIVE("active"), /** Aktuell nicht aktiver Kostenträger. */
  NONACTIVE("nonactive"), /** Beendet. */
  ENDED("ended");

  private String key;

  /**
   * The key will be used e. g. for i18n.
   * @return
   */
  public String getKey()
  {
    return key;
  }

  KostentraegerStatus(String key)
  {
    this.key = key;
  }

  public boolean isIn(KostentraegerStatus... status)
  {
    for (KostentraegerStatus st : status) {
      if (this == st) {
        return true;
      }
    }
    return false;
  }

  public String getI18nKey()
  {
    return "fibu.kost.status." + key;
  }
}
