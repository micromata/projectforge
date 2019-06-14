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
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 *         <ul>
 *         <li>ACTIVE - Contact is warm, working together, don't forget.</li>
 *         <li>NON_ACTIVE - No current projects, maybe interesting later.</li>
 *         <li>UNINTERESTING - Uninteresting for the company.</li>
 *         <li>DEPARTED</li>
 *         <li>PERSONA_INGRATA</li>
 *         </ul>
 */
public enum ContactStatus implements I18nEnum
{
  ACTIVE("active"), NON_ACTIVE("nonActive"), UNINTERESTING("uninteresting"), PERSONA_INGRATA("personaIngrata"), DEPARTED("departed");

  private String key;

  public static ContactStatus get(String s)
  {
    if (StringUtils.isEmpty(s) == true) {
      return null;
    }
    if ("ACTIVE".equals(s) == true) {
      return ACTIVE;
    } else if ("NON_ACTIVE".equals(s) == true) {
      return NON_ACTIVE;
    } else if ("UNINTERESTING".equals(s) == true) {
      return UNINTERESTING;
    } else if ("DEPARTED".equals(s) == true) {
      return DEPARTED;
    } else if ("PERSONA_INGRATA".equals(s) == true) {
      return PERSONA_INGRATA;
    }
    throw new UnsupportedOperationException("Unknown ContactStatus: '" + s + "'");
  }

  public boolean isIn(final ContactStatus... status) {
    for (final ContactStatus st : status) {
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
    return "address.contactStatus." + key;
  }

  ContactStatus(String key)
  {
    this.key = key;
  }
}
