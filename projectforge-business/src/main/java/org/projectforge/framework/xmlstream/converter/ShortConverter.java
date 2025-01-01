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

package org.projectforge.framework.xmlstream.converter;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.framework.xmlstream.XmlConstants;

public class ShortConverter extends AbstractValueConverter<Short>
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ShortConverter.class);

  @Override
  public Short fromString(final String str)
  {
    try {
      if (StringUtils.isEmpty(str) || XmlConstants.NULL_IDENTIFIER.equals(str)) {
        return null;
      }
      return new Short(str);
    } catch (final NumberFormatException ex) {
      log.warn("Can't convert value '" + str + "' to short.");
      return 0;
    }
  }
}
