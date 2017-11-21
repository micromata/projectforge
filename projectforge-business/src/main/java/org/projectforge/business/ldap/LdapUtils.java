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

package org.projectforge.business.ldap;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.common.StringHelper;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class LdapUtils
{
  private static final String ATTRIBUTE_SEPARATOR_CHAR = ",";

  /**
   * Escapes the following characters: , (comma), = (equals), + (plus), < (less than), > (greater than), # (number sign), ; (semicolon), \
   * (backslash), and " (quotation mark).
   * @param name
   * @return null if name is null otherwise the string name with escaped special characters.
   */
  public static String escapeCommonName(final String name)
  {
    if (name == null) {
      return null;
    }
    final StringBuffer buf = new StringBuffer(name.length() + 5);
    for (int i = 0; i < name.length(); i++) {
      final char ch = name.charAt(i);
      if (",=+<>#;\\\"".indexOf(ch) >= 0) {
        buf.append("\\");
      }
      buf.append(ch);
    }
    return buf.toString();
  }

  public static String getOu(final String... organizationalUnits)
  {
    if (organizationalUnits == null) {
      return "";
    }
    if (organizationalUnits.length == 1 && organizationalUnits[0] != null && organizationalUnits[0].startsWith("ou=") == true) {
      // organizationalUnit is already in the form ou=...,ou=.... Nothing to be done.
      return organizationalUnits[0];
    }
    final StringBuffer buf = new StringBuffer();
    buildOu(buf, null, organizationalUnits);
    return buf.toString();
  }

  public static String getOu(final String ou, final String[] organizationalUnits)
  {
    final StringBuffer buf = new StringBuffer();
    buildOu(buf, ou, organizationalUnits);
    return buf.toString();
  }

  public static void buildOu(final StringBuffer buf, final String ou, final String[] organizationalUnits)
  {
    if (ou == null && organizationalUnits == null) {
      return;
    }
    boolean first = true;
    if (ou != null) {
      first = false;
      if (ou.startsWith("ou=") == false) {
        buf.append("ou=");
      }
      buf.append(ou);
    }
    for (final String unit : organizationalUnits) {
      if (unit == null) {
        continue;
      }
      if (first == true) {
        first = false;
      } else {
        buf.append(',');
      }
      if (unit.startsWith("ou=") == false) {
        buf.append("ou=");
      }
      buf.append(unit);
    }
  }

  public static void buildOu(final StringBuffer buf, final String... organizationalUnits)
  {
    buildOu(buf, null, organizationalUnits);
  }

  public static void appendOu(final StringBuffer buf, final String... organizationalUnits)
  {
    if (organizationalUnits == null) {
      return;
    }
    for (final String ou : organizationalUnits) {
      if (ou == null) {
        continue;
      }
      buf.append(',');
      if (ou.startsWith("ou=") == false) {
        buf.append("ou=");
      }
      buf.append(ou);
    }
  }

  public static String getOrganizationalUnit(final String dn)
  {
    if (dn == null || dn.indexOf("ou=") < 0) {
      return null;
    }
    final String[] entries = StringUtils.split(dn, ",");
    final StringBuffer buf = new StringBuffer();
    boolean first = true;
    for (String entry : entries) {
      if (entry == null) {
        continue;
      }
      entry = entry.trim();
      if (entry.startsWith("ou=") == false || entry.length() < 4) {
        continue;
      }
      if (first == true) {
        first = false;
      } else {
        buf.append(",");
      }
      buf.append(entry);
    }
    return buf.toString();
  }

  public static String getOrganizationalUnit(final String dn, final String ouBase)
  {
    if (StringUtils.isNotBlank(ouBase) == true) {
      return getOrganizationalUnit(dn + "," + ouBase);
    } else {
      return getOrganizationalUnit(dn);
    }
  }

  public static Object getAttributeValue(final Attributes attributes, final String attrId) throws NamingException
  {
    final Attribute attr = attributes.get(attrId);
    if (attr == null) {
      return null;
    }
    return attr.get();
  }

  public static String getAttributeStringValue(final Attributes attributes, final String attrId) throws NamingException
  {
    final Attribute attr = attributes.get(attrId);
    if (attr == null) {
      return null;
    }
    return (String) attr.get();
  }

  public static String[] getAttributeStringValues(final Attributes attributes, final String attrId) throws NamingException
  {
    final Attribute attr = attributes.get(attrId);
    if (attr == null) {
      return null;
    }
    final NamingEnumeration< ? > enumeration = attr.getAll();
    final List<String> list = new ArrayList<String>();
    while (enumeration.hasMore() == true) {
      final Object attrValue = enumeration.next();
      if (attrValue == null) {
        list.add(null);
      }
      list.add(String.valueOf(attrValue));
    }
    return list.toArray(new String[list.size()]);
  }

  public static Integer getAttributeIntegerValue(final Attributes attributes, final String attrId) throws NamingException
  {
    final Attribute attr = attributes.get(attrId);
    if (attr == null) {
      return null;
    }
    return (Integer) attr.get();
  }

  public static Attribute putAttribute(final Attributes attributes, final String attrId, final String attrValue)
  {
    final Attribute attr = attributes.get(attrId);
    if (attrValue == null) {
      return attr;
    }
    if (attr == null) {
      return attributes.put(attrId, attrValue);
    }
    attr.add(attrValue);
    return attr;
  }

  /**
   * "customers,users" -> "ou=customers,ou=users".
   * @param attrId
   * @param value
   * @return
   */
  public static String splitMultipleAttribute(final String attrId, final String value)
  {
    if (value == null) {
      return null;
    }
    final int pos = value.indexOf(ATTRIBUTE_SEPARATOR_CHAR);
    if (pos < 0) {
      return attrId + "=" + value;
    }
    final String[] strs = StringHelper.splitAndTrim(value, ATTRIBUTE_SEPARATOR_CHAR);
    final StringBuffer buf = new StringBuffer();
    boolean first = true;
    for (final String str : strs) {
      if (first == true) {
        first = false;
      } else {
        buf.append(", ");
      }
      buf.append(attrId).append('=').append(str);
    }
    return buf.toString();
  }

  public static List<String> getMissedObjectClasses(final String[] expectedAdditionalObjectClasses, final String expectedObjectClass,
      final String[] objectClasses)
      {
    List<String> list = null;
    if (expectedObjectClass != null && (objectClasses == null || StringHelper.isIn(expectedObjectClass, objectClasses) == false)) {
      list = addEntry(list, expectedObjectClass);
    }
    if (expectedAdditionalObjectClasses != null) {
      for (final String objectClass : expectedAdditionalObjectClasses) {
        // objectClasses of users doesn't match the expected ones.
        if (objectClasses == null || StringHelper.isIn(objectClass, objectClasses) == false) {
          list = addEntry(list, objectClass);
        }
      }
    }
    return list;
      }

  private static List<String> addEntry(List<String> list, final String entry)
  {
    if (list == null) {
      list = new ArrayList<String>();
    }
    list.add(entry);
    return list;
  }
}
