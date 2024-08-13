/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.history;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.utils.NumberHelper;

/**
 * StringBridge for hibernate search to search in phone numbers (reduce phone number fields to digits without white spaces and non digits).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class HibernateSearchPhoneNumberBridge implements ValueBridge<Object, String>
{

  @Override
  public String toIndexedValue(Object o, ValueBridgeToIndexedValueContext valueBridgeToIndexedValueContext) {
    return "";
  }

  @Override
  public Object fromIndexedValue(String value, ValueBridgeFromIndexedValueContext context) {
    if (value == null || !(value instanceof String)) {
      return "";
    }
    final String number = (String) value;
    return number + '|' + StringHelper.removeNonDigits(number) + '|' + NumberHelper.extractPhonenumber(number);
  }
}
