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

package org.projectforge.framework.utils;

import java.io.PrintWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Helper class for writing comma separated key-value-pairs.
 * @author K.Reinhard@micromata.com
 */
public class KeyValuePairWriter
{
  protected DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

  public final static char DEFAULT_SEPARATOR_CHAR = ',';

  private final PrintWriter out;

  private char csvSeparatorChar = DEFAULT_SEPARATOR_CHAR;

  /**
   * First entry of line?
   */
  private boolean firstEntry = true;

  public KeyValuePairWriter(final Writer writer)
  {
    out = new PrintWriter(writer);
  }

  public void flush()
  {
    out.flush();
  }

  /**
   * Appends the given value to the buffer.
   * 
   * @param value The value to append.
   */
  public KeyValuePairWriter write(final String key, final long value)
  {
    writeSeparator();
    out.print(key);
    out.print("=");
    out.print(value);
    return this;
  }

  /**
   * Appends the given value in the format "yyyy-MM-dd HH:mm:ss.SSS".
   * 
   * @param value The value to append.
   */
  public KeyValuePairWriter write(final String key, final Date value)
  {
    writeSeparator();
    out.print(key);
    out.print("=");
    if (value != null) {
      out.print('"');
      out.print(dateFormat.format(value));
      out.print('"');
    }
    return this;
  }

  /**
   * Appends the given value. The string will be encapsulated in quotation marks: " Any occurrence of the quotation mark will be quoted by
   * duplication. Example: hallo -> "hallo", hal"lo -> "hal""lo"
   * 
   * @param value The value to append.
   */
  public KeyValuePairWriter write(final String key, final String s)
  {
    writeSeparator();
    out.print(key);
    out.print("=");
    if (s != null) {
      out.print('"');
      final int len = s.length();
      char c;
      for (int i = 0; i < len; i++) {
        c = s.charAt(i);
        if ('"' == c) {
          out.print('"');
        }
        out.print(c);
      }
      out.print('"');
    }
    return this;
  }

  /**
   * Appends the given value to the buffer in the format "yyyy-MM-dd HH:mm:ss.SSS".
   * 
   * @param value The value to append.
   */
  public KeyValuePairWriter write(final String key, final Object value)
  {
    writeSeparator();
    out.print(key);
    out.print("=");
    if (value != null) {
      out.print(String.valueOf(value));
    }
    return this;
  }

  /**
   * @param dateFormat The dateFormat to set.
   */
  public void setDateFormat(final DateFormat dateFormat)
  {
    this.dateFormat = dateFormat;
  }

  public void setCsvSeparator(final char csvSeparator)
  {
    this.csvSeparatorChar = csvSeparator;
  }

  private void writeSeparator()
  {
    if (firstEntry) {
      firstEntry = false;
    } else {
      out.print(csvSeparatorChar);
    }
  }
}
