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

package org.projectforge.business.fibu;

import org.projectforge.common.i18n.I18nEnum;

public enum KundeStatus implements I18nEnum
{

  /** Kunde befindet sich noch in der Akquisephase. */
  ACQUISISTION("acquisition"), /** Mit dem Kunden werden aktiv Geschäfte abgewickelt. */
  ACTIVE("active"), /** Mit dem Kunden bestehen (vorübergehend) keine Geschäftsverbindungen mehr. */
  NONACTIVE("nonactive"), /** Die Zusammenarbeit mit dem Kunden ist beendet. Eine erneute Zusammenarbeit ist unwahrscheinlich. */
  ENDED("ended"), /** Der Kunde ist insolvent, umfirmiert etc. */
  NONEXISTENT("nonexistent");

  public static final KundeStatus[] LIST = new KundeStatus[] { ACQUISISTION, ACTIVE, NONACTIVE, ENDED, NONEXISTENT};

  private String key;

  /**
   * The key will be used e. g. for i18n.
   * @return
   */
  public String getKey()
  {
    return key;
  }

  public String getI18nKey()
  {
    return "fibu.kunde.status." + key;
  };

  KundeStatus(String key)
  {
    this.key = key;
  }

  public boolean isIn(KundeStatus... status)
  {
    for (KundeStatus st : status) {
      if (this == st) {
        return true;
      }
    }
    return false;
  }
}
