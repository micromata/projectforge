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

package org.projectforge.business.book;

import org.projectforge.common.i18n.I18nEnum;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 *         <ul>
 *         <li>MISSED - Book not found (espacially after an inventory).</li>
 *         <li>PRESENT - Book is present at the office ore lend out is known.</li>
 *         <li>DISPOSED - Book is disposed.</li>
 *         </ul>
 */
public enum BookStatus implements I18nEnum
{
  PRESENT("present"), MISSED("missed"), DISPOSED("disposed"), UNKNOWN("unknown");

  private String key;

  /**
   * @return The full i18n key including the i18n prefix "book.status.".
   */
  public String getI18nKey()
  {
    return "book.status." + key;
  }

  /**
   * The key will be used e. g. for i18n.
   * @return
   */
  public String getKey()
  {
    return key;
  }

  BookStatus(String key)
  {
    this.key = key;
  }
}
