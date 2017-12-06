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

package org.projectforge.framework.xstream.converter;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.framework.xstream.XmlConstants;

public class DoubleConverter extends AbstractValueConverter<Double>
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DoubleConverter.class);

  @Override
  public Double fromString(String str)
  {
    try {
      if (StringUtils.isEmpty(str) == true || XmlConstants.NULL_IDENTIFIER.equals(str) == true) {
        return null;
      }
      return new Double(str);
    } catch (final NumberFormatException ex) {
      log.warn("Can't convert value '" + str + "' to double.");
      return 0.0;
    }
  }
}
