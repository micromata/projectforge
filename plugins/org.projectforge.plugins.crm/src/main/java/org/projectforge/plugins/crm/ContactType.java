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

package org.projectforge.plugins.crm;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.common.i18n.I18nEnum;

/**
 * @author Werner Feder (werner.feder@t-online.de)
 */
public enum ContactType implements I18nEnum
{
  BUSINESS("business"), POSTAL("postal"), PRIVATE("private"), OTHER("other"), OWN("own");

  public static final String I18N_KEY_CONTACTTYPE_PREFIX = "contact.type.";

  /**
   * List of all available values.
   */
  public static final ContactType[] LIST = new ContactType[] { BUSINESS, POSTAL, PRIVATE, OTHER, OWN };

  private String key;

  public static ContactType get(final String s)
  {
    if (StringUtils.isEmpty(s) == true) {
      return null;
    }
    if ("BUSINESS".equals(s) == true) {
      return BUSINESS;
    } else if ("POSTAL".equals(s) == true) {
      return POSTAL;
    } else if ("PRIVATE".equals(s) == true) {
      return PRIVATE;
    } else if ("OTHER".equals(s) == true) {
      return OTHER;
    } else if ("OWN".equals(s) == true) {
      return OWN;
    }
    throw new UnsupportedOperationException("Unknown ContactType: '" + s + "'");
  }

  /**
   * The key will be used e. g. for i18n.
   * @return
   */
  public String getKey()
  {
    return key;
  }

  ContactType(final String key)
  {
    this.key = key;
  }

  /**
   * @see org.projectforge.common.i18n.I18nEnum#getI18nKey()
   */
  @Override
  public String getI18nKey()
  {
    return I18N_KEY_CONTACTTYPE_PREFIX + key;
  }
}
