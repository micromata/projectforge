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
import org.projectforge.framework.utils.Base64Helper;
import org.projectforge.framework.xmlstream.XmlConstants;

import java.io.IOException;
import java.util.Base64;

public class ByteArrayConverter implements IConverter<byte[]>
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ByteArrayConverter.class);

  @Override
  public byte[] fromString(final String str)
  {
    if (StringUtils.isEmpty(str) || XmlConstants.NULL_IDENTIFIER.equals(str)) {
      return null;
    }
    try {
      final byte[] bytes = (byte[]) Base64Helper.decodeObject(str);
      return bytes;
    } catch (final IOException | ClassNotFoundException ex) {
      log.error("Error while uncompressing string: " + ex.getMessage(), ex);
      return null;
    }
  }

  /**
   * @see org.projectforge.framework.xmlstream.converter.IConverter#toString(java.lang.Object)
   */
  @Override
  public String toString(final Object obj)
  {
    if (obj == null || !(obj instanceof byte[])) {
      return null;
    }
    final String result = Base64.getEncoder().encodeToString((byte[]) obj);
    return result;
  }
}
