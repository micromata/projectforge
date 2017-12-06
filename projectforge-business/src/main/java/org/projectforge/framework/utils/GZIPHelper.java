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

package org.projectforge.framework.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;

public class GZIPHelper
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GZIPHelper.class);

  /**
   * @param str
   * @return Base64 encoded byte array.
   */
  public static String compress(final String str)
  {
    if (str == null || str.length() == 0) {
      return str;
    }
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      final GZIPOutputStream gzip = new GZIPOutputStream(out);
      gzip.write(str.getBytes());
      gzip.close();
      return Base64Helper.encodeObject(out.toByteArray());
    } catch (final IOException ex) {
      log.error("Error while compressing string: " + ex.getMessage(), ex);
      return null;
    }
  }

  /**
   * @param base64ByteArray
   * @return
   */
  public static String uncompress(final String base64ByteArray)
  {
    if (base64ByteArray == null || base64ByteArray.length() == 0) {
      return base64ByteArray;
    }
    try {
      final byte[] byteArray = (byte[]) Base64Helper.decodeObject(base64ByteArray);
      final ByteArrayInputStream in = new ByteArrayInputStream(byteArray);
      final GZIPInputStream gzip = new GZIPInputStream(in);
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      IOUtils.copy(gzip, out);
      gzip.close();
      return out.toString();
    } catch (final IOException ex) {
      log.error("Error while uncompressing string: " + ex.getMessage(), ex);
      return null;
    } catch (final ClassNotFoundException ex) {
      log.error("Error while uncompressing string: " + ex.getMessage(), ex);
      return null;
    }
  }
}
