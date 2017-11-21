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

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Simple key value parser
 * @author K.Reinhard@micromata.com
 */
public class KeyValuePairParser
{
  private char pairsSeparatorChar = KeyValuePairWriter.DEFAULT_SEPARATOR_CHAR;

  public static final String ERROR_UNEXPECTED_QUOTATIONMARK = "Unexpected quotation mark \" (only allowed in quoted cells).";

  public static final String ERROR_QUOTATIONMARK_MISSED_AT_END_OF_CELL = "Quotation \" missed at the end of cell.";

  public static final String ERROR_DELIMITER_OR_NEW_LINE_EXPECTED_AFTER_QUOTATION_MARK = "Delimiter or new line expected after quotation mark.";

  public static final String ERROR_UNEXPECTED_CHARACTER_AFTER_QUOTATION_MARK = "Unexpected character after quotation mark.";

  public static final String ERROR_INVALID_KEY = "Unexpected key. A key must be an identifier followed by '='.";

  public enum Type
  {
    EOF, EOL, CHAR
  }

  private final Reader source;

  private Type type;

  private int lineno = 1;

  private int colno = 0;

  private int val;

  private char cval;

  private final int pushbackBuffer[] = new int[5];

  private int pushbackIndex = -1;

  private Map<String, String> keyValuePairs;

  public KeyValuePairParser(final Reader source)
  {
    this.source = source;
  }

  /**
   * Returns null, if EOF.
   * @return
   */
  public Map<String, String> parse()
  {
    if (type == Type.EOF) {
      return null;
    }
    keyValuePairs = null;
    do {
      final String key = parseKey();
      final String value = parseValue();
      if (key != null) {
        if (keyValuePairs == null) {
          keyValuePairs = new HashMap<String, String>();
        }
        keyValuePairs.put(key, value);
      }
    } while (type != Type.EOF && type != Type.EOL);
    return keyValuePairs;
  }

  public String getString(final String key)
  {
    return keyValuePairs.get(key);
  }

  public Integer getInteger(final String key)
  {
    final String value = keyValuePairs.get(key);
    return NumberHelper.parseInteger(value);
  }

  public BigDecimal getBigDecimal(final String key)
  {
    final String value = keyValuePairs.get(key);
    return NumberHelper.parseBigDecimal(value);
  }

  public String parseKey()
  {
    skipWhitespaces();
    final StringBuffer buf = new StringBuffer();
    while (true) {
      nextToken();
      if (type != Type.CHAR) {
        if (buf.length() > 0) {
          throw new RuntimeException(createMessage(ERROR_INVALID_KEY, buf.toString()));
        }
        return null;
      }
      if (cval == '=') {
        break;
      } else {
        buf.append(cval);
      }
    }
    return buf.toString();
  }

  public String parseValue()
  {
    skipWhitespaces();
    nextToken();
    if (type != Type.CHAR || cval == pairsSeparatorChar) {
      return null;
    }
    boolean quoted = false;
    if (cval == '"') {
      quoted = true; // value is quoted.
      nextToken();
    }
    final StringBuffer buf = new StringBuffer();
    while (true) {
      if (type != Type.CHAR) {
        if (quoted == true) {
          throw new RuntimeException(createMessage(ERROR_QUOTATIONMARK_MISSED_AT_END_OF_CELL));
        }
        return buf.toString();
      }
      if (cval == '"') {
        if (quoted == false) {
          throw new RuntimeException(createMessage(ERROR_UNEXPECTED_QUOTATIONMARK, buf.toString()));
        }
        nextToken();
        if (type != Type.CHAR || cval == pairsSeparatorChar) { // End of cell
          break;
        } else if (quoted == true && cval == '"') { // Escaped quotation mark
          buf.append(cval);
        } else if (Character.isWhitespace(cval) == true) {
          skipWhitespaces();
          nextToken();
          if (type != Type.CHAR || cval == pairsSeparatorChar) {
            break;
          } else {
            throw new RuntimeException(createMessage(ERROR_DELIMITER_OR_NEW_LINE_EXPECTED_AFTER_QUOTATION_MARK));
          }
        } else {
          throw new RuntimeException(createMessage(ERROR_UNEXPECTED_CHARACTER_AFTER_QUOTATION_MARK));
        }
      } else if (quoted == false && cval == pairsSeparatorChar) {
        break;
      } else {
        buf.append(cval);
      }
      nextToken();
    }
    return buf.toString();
  }

  public void setCsvSeparatorChar(final char csvSeparatorChar)
  {
    this.pairsSeparatorChar = csvSeparatorChar;
  }

  private String createMessage(final String msg, final String s)
  {
    return createMessage(msg, s, lineno, colno);
  }

  private String createMessage(final String msg)
  {
    return createMessage(msg, null, lineno, colno);
  }

  static String createMessage(final String msg, final String s, final int line, final int col)
  {
    return msg + " Error in line: " + line + " (" + col + ")" + (StringUtils.isNotBlank(s) ? ": " + s : "");
  }

  /**
   * Skips white spaces excluding new line ("\n" or "\r\n").
   */
  private void skipWhitespaces()
  {
    while (true) {
      nextToken();
      if (type != Type.CHAR || Character.isWhitespace(cval) == false) {
        unread();
        break;
      }
    }
  }

  public int lineno()
  {
    return lineno;
  }

  public boolean isIdentifierPart(final char ch)
  {
    return Character.isUnicodeIdentifierPart(ch);
  }

  public Type nextToken()
  {
    cval = 0;
    type = Type.CHAR;
    val = read();
    if (val == -1) {
      // EOF
      type = Type.EOF;
      colno = 0;
      return type;
    }
    final char c = (char) val;
    if (c == '\r') {
      val = read();
      if (val == -1) {
        unread(val); // EOF
      } else if ((char) val == '\n') {
        colno = 0;
        type = Type.EOL; // MS-DOS CR: \r\n
        return type;
      }
      unread(val); // No MS-DOS CR.
    } else if (c == '\n') {
      colno = 0;
      type = Type.EOL;
      return type;
    }
    type = Type.CHAR;
    cval = c;
    colno++;
    return type;
  }

  public void unread(final int b)
  {
    if (b == '\n') {
      lineno--;
      colno = 0;
    } else {
      colno--;
    }
    pushbackBuffer[++pushbackIndex] = b;
  }

  public void unread()
  {
    unread(val);
  }

  public int read()
  {
    int b;
    if (pushbackIndex >= 0) {
      b = pushbackBuffer[pushbackIndex--];
    } else {
      try {
        b = source.read();
      } catch (final IOException ex) {
        throw new RuntimeException("IOException in line: " + lineno, ex);
      }
    }
    if (b == '\n')
      lineno++;
    return b;
  }

}
