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

package org.projectforge.business.address;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.common.i18n.I18nEnum;

/**
 * Status of address quality.<br/>
 * <ul>
 * <li>UPTODATE - At last time of check or modification, the address seems to be updated.</li>
 * <li>OUTDATED - Address needs update.</li>
 * <li>LEAVED - Person has leaved the company.</li>
 * </ul>
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public enum AddressStatus implements I18nEnum
{
  UPTODATE("uptodate"), OUTDATED("outdated"), LEAVED("leaved");

  /**
   * List of all available values.
   */
  public static final AddressStatus[] LIST = new AddressStatus[] { UPTODATE, OUTDATED, LEAVED};

  private String key;

  public static AddressStatus get(final String s)
  {
    if (StringUtils.isEmpty(s) == true) {
      return null;
    }
    if ("LEAVED".equals(s) == true) {
      return LEAVED;
    } else if ("OUTDATED".equals(s) == true) {
      return OUTDATED;
    } else if ("UPTODATE".equals(s) == true) {
      return UPTODATE;
    }
    throw new UnsupportedOperationException("Unknown AddressStatus: '" + s + "'");
  }

  public boolean isIn(final AddressStatus... status) {
    for (final AddressStatus st : status) {
      if (this == st) {
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
    return "address.addressStatus." + key;
  }

  AddressStatus(final String key)
  {
    this.key = key;
  }
}
