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

package org.projectforge.business.fibu;

import org.projectforge.common.i18n.I18nEnum;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public enum PaymentStatus implements I18nEnum
{
  ALL("all"), PAID("paid"), UNPAID("unpaid");

  private String key;

  /**
   * @return The key suffix will be used e. g. for i18n.
   */
  public String getKey()
  {
    return key;
  }

  /**
   * @return The full i18n key including the i18n prefix "fibu.auftrag.status.".
   */
  public String getI18nKey()
  {
    return "fibu.paymentStatus." + key;
  }

  PaymentStatus(final String key)
  {
    this.key = key;
  }

  public boolean isIn(final PaymentStatus... status)
  {
    for (final PaymentStatus st : status) {
      if (this == st) {
        return true;
      }
    }
    return false;
  }
};
