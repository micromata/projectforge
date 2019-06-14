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

package org.projectforge.business.fibu;

import org.projectforge.common.i18n.I18nEnum;

public enum Gender implements I18nEnum
{
  NOT_KNOWN(0, "notKnown"),
  MALE(1, "male"),
  FEMALE(2, "female"),
  NOT_APPLICABLE(9, "notApplicable");

  private final int isoCode;
  private final String i18nKey;

  Gender(int isoCode, String i18nKey)
  {
    this.isoCode = isoCode;
    this.i18nKey = i18nKey;
  }

  /**
   * @return The integer representation of the gender according to the ISO/IEC 5218.
   */
  public int getIsoCode()
  {
    return isoCode;
  }

  /**
   * @return The full i18n key including the i18n prefix "gender.".
   */
  public String getI18nKey()
  {
    return "gender." + i18nKey;
  }

}
