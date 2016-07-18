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

package org.projectforge.web.core;

import java.util.Collection;

import org.apache.commons.lang.ObjectUtils;

public class JsonBuilder
{
  final private StringBuilder sb = new StringBuilder();

  private boolean escapeHtml;

  /**
   * Creates Json result string from the given list.<br/>
   * [["Horst"], ["Klaus"], ...]] // For single property<br/>
   * [["Klein", "Horst"],["Schmidt", "Klaus"], ...] // For two Properties (e. g. name, first name) [["id:37", "Klein", "Horst"],["id:42",
   * "Schmidt", "Klaus"], ...] // For two Properties (e. g. name, first name) with id. <br/>
   * Uses ObjectUtils.toString(Object) for formatting each value.
   * @param col The array representation: List<Object> or List<Object[]>. If null then "[]" is returned.
   * @return
   */
  public static String buildToStringRows(final Collection< ? > col)
  {
    if (col == null) {
      return "[]";
    }
    final JsonBuilder builder = new JsonBuilder();
    return builder.append(col).getAsString();
  }

  /**
   * @param escapeHtml the escapeHtml to set (default is false).
   * @return this for chaining.
   */
  public JsonBuilder setEscapeHtml(final boolean escapeHtml)
  {
    this.escapeHtml = escapeHtml;
    return this;
  }

  public String getAsString()
  {
    return sb.toString();
  }

  /**
   * Appends objects to buffer, e. g.: ["Horst"], ["Klaus"], ... Uses formatValue(Object) to render the values.
   * @param oArray
   * @return This (fluent)
   */
  public JsonBuilder append(final Object[] oArray)
  {
    sb.append(" ["); // begin array
    String separator = "";
    for (final Object obj : oArray) {
      sb.append(separator);
      separator = ",";
      sb.append(escapeString(formatValue(obj)));
    }
    sb.append("]"); // end array
    return this;
  }

  private String escapeString(final String string)
  {
    if (string == null || string.length() == 0) {
      return "\"\"";
    }
    char c = 0;
    int i;
    final int len = string.length();
    final StringBuilder sb = new StringBuilder(len + 4);
    String t;
    sb.append('"');
    for (i = 0; i < len; i += 1) {
      c = string.charAt(i);
      switch (c) {
        case '\\':
        case '"':
          sb.append('\\');
          sb.append(c);
          break;
        case '/':
          // if (b == '<') {
          sb.append('\\');
          // }
          sb.append(c);
          break;
        case '\b':
          sb.append("\\b");
          break;
        case '\t':
          sb.append("\\t");
          break;
        case '\n':
          sb.append("\\n");
          break;
        case '\f':
          sb.append("\\f");
          break;
        case '\r':
          sb.append("\\r");
          break;
        default:
          if (c < ' ') {
            t = "000" + Integer.toHexString(c);
            sb.append("\\u" + t.substring(t.length() - 4));
          } else {
            if (escapeHtml == true) {
              switch (c) {
                case '<':
                  sb.append("&lt;");
                  break;
                case '>':
                  sb.append("&gt;");
                  break;
                case '&':
                  sb.append("&amp;");
                  break;
                case '"':
                  sb.append("&quot;");
                  break;
                case '\'':
                  sb.append("&#x27;");
                  break;
                case '/':
                  sb.append("&#x2F;");
                  break;
                default:
                  sb.append(c);
              }
            } else {
              sb.append(c);
            }
          }
      }
    }
    sb.append('"');
    return sb.toString();
  }

  /**
   * Creates Json result string from the given list.<br/>
   * [["Horst"], ["Klaus"], ...]] // For single property<br/>
   * [["Klein", "Horst"],["Schmidt", "Klaus"], ...] // For two Properties (e. g. name, first name) [["id:37", "Klein", "Horst"],["id:42",
   * "Schmidt", "Klaus"], ...] // For two Properties (e. g. name, first name) with id. <br/>
   * Uses formatValue(Object) for formatting each value.
   * @param col The array representation: List<Object> or List<Object[]>. If null then "[]" is returned.
   * @return
   */
  public JsonBuilder append(final Collection< ? > col)
  {
    if (col == null) {
      sb.append("[]");
      return this;
    }
    // Format: [["1.1", "1.2", ...],["2.1", "2.2", ...]]
    sb.append("[\n");
    String separator = "\n";
    for (final Object os : col) {
      sb.append(separator);
      separator = ",\n";
      if (os instanceof Object[]) { // Multiple properties
        append((Object[]) os);
      } else { // Only one property
        append(transform(os));
      }
    }
    sb.append("]"); // end data
    return this;
  }

  /**
   * @param obj
   * @return
   * @see ObjectUtils#toString(Object)
   */
  protected String formatValue(final Object obj)
  {
    return ObjectUtils.toString(obj);
  }

  protected JsonBuilder append(final Object obj)
  {
    if (obj instanceof Object[]) {
      return append((Object[]) obj);
    }
    sb.append(" ["); // begin row
    // " must be quoted as \":
    sb.append(escapeString(formatValue(obj)));
    sb.append("]"); // end row
    return this;
  }

  /**
   * Before rendering a obj of e. g. a collection the obj can be transformed e. g. in an Object array of dimension 2 containing label and
   * value.
   * @param obj
   * @return obj (identity function) if not overload.
   */
  protected Object transform(final Object obj)
  {
    return obj;
  }
}
