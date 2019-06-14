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

package org.projectforge.framework.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * For cloning a object including all fields (recursive).
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class CloneHelper
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CloneHelper.class);

  /**
   * Serialized the given object as ByteArray and deserializes it.
   * @param orgin
   * @return
   */
  @SuppressWarnings("unchecked")
  public static <T> T cloneBySerialization(final T origin)
  {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      final ObjectOutputStream
      oos = new ObjectOutputStream(bos);
      oos.writeObject(origin);
      oos.flush();
      oos.close();
      bos.close();
    } catch (final IOException ex) {
      log.error("Exception encountered while cloning given object '" + origin + "': " + ex, ex);
      return null;
    }
    final byte[] byteData = bos.toByteArray();

    final ByteArrayInputStream bais = new ByteArrayInputStream(byteData);
    T object;
    try {
      object = (T) new ObjectInputStream(bais).readObject();
      return object;
    } catch (final ClassNotFoundException ex) {
      log.error("Exception encountered while cloning given object '" + origin + "': " + ex, ex);
      return null;
    } catch (final IOException ex) {
      log.error("Exception encountered while cloning given object '" + origin + "': " + ex, ex);
      return null;
    }
  }
}
