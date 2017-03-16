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

import java.util.Arrays;

import org.projectforge.common.i18n.I18nEnum;

public enum EmployeeStatus implements I18nEnum
{
  FEST_ANGESTELLTER("festAngestellter"), BEFRISTET_ANGESTELLTER("befristetAngestellter"), FREELANCER("freelancer"), AUSHILFE(
    "aushilfe"), STUDENTISCHE_HILFSKRAFT(
    "studentischeHilfskraft"), STUD_ABSCHLUSSARBEIT("studentischeAbschlussarbeit"), PRAKTIKANT("praktikant"), AZUBI("azubi");

  private String key;

  /**
   * @return The full i18n key including the i18n prefix "fibu.auftrag.status.".
   */
  public String getI18nKey()
  {
    return "fibu.employee.status." + key;
  }

  /**
   * The key will be used e. g. for i18n.
   *
   * @return
   */
  public String getKey()
  {
    return key;
  }

  EmployeeStatus(String key)
  {
    this.key = key;
  }

  public static EmployeeStatus findByi18nKey(final String i18nKey)
  {
    return Arrays.stream(values())
        .filter(es -> es.getI18nKey().equals(i18nKey))
        .findFirst()
        .orElse(null);
  }
}
