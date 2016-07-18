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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.codec.binary.Base64;

/*******************************************************************************************************************************************
 * Helper to encode/decode serializable objects into base64 representation.
 * 
 * @see java.io.ObjectInputStream
 * @see java.io.ObjectOutputStream
 * @author Wolfgang Jung (w.jung@micromata.de)
 * 
 *         TODO DESIGNBUG ueberfluessig
 */
public class Base64Helper
{

  /*****************************************************************************************************************************************
   * Convert a given base64 representation into an object.
   * 
   * @param base64object a base64 encoded object
   * @return the deserialized object
   * @throws IOException if the base64 representation doesn't contain an object
   * @throws ClassNotFoundException if the deserialized object can not resolved
   */
  public static Object decodeObject(String base64object) throws IOException, ClassNotFoundException
  {
    ObjectInputStream ois = new ObjectInputStream(
        new ByteArrayInputStream(Base64.decodeBase64(base64object.getBytes())));
    Object o = ois.readObject();
    ois.close();
    return o;
  }

  /*****************************************************************************************************************************************
   * encodes an object into the base64 representation
   * 
   * @param obj a serializable object
   * @return the string representation
   * @throws IOException if the object is not serializable
   */
  public static String encodeObject(Object obj) throws IOException
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(obj);
    oos.close();
    return new String(Base64.encodeBase64(baos.toByteArray()));
  }

}
