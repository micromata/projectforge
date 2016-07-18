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

package org.projectforge.common;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * Simple csv parser
 * 
 * @author K.Reinhard@micromata.com
 * @author H.Spiewok@micromata.com (07/2005)
 */
public class CSVParser
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(CSVParser.class);

  private char csvSeparatorChar = CSVWriter.DEFAULT_CSV_SEPARATOR_CHAR;

  public static final String ERROR_UNEXPECTED_QUOTATIONMARK = "Unexpected quotation mark \" (only allowed in quoted cells).";

  public static final String ERROR_QUOTATIONMARK_MISSED_AT_END_OF_CELL = "Quotation \" missed at the end of cell.";

  public static final String ERROR_DELIMITER_OR_NEW_LINE_EXPECTED_AFTER_QUOTATION_MARK = "Delimter or new line expected after quotation mark.";

  public static final String ERROR_UNEXPECTED_CHARACTER_AFTER_QUOTATION_MARK = "Unexpected character after quotation mark.";

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

  private Map<String, Integer> colMap;

  public CSVParser(final Reader source)
  {
    this.source = source;
  }

  /**
   * Parses the first (next) line and returns the cells as string. Stores also the col numbers of the head cols for .
   * 
   * @return
   * @see #parseLine()
   * @see #getCell(List, String)
   */
  public List<String> parseHeadCols()
  {
    final List<String> cells = parseLine();
    if (cells == null) {
      return null;
    }
    colMap = new HashMap<String, Integer>();
    for (int i = 0; i < cells.size(); i++) {
      colMap.put(cells.get(i), i);
    }
    return cells;
  }

  /**
   * Get the cell with the given colname (head cols had to be parsed first via {@link #parseHeadCols()}.
   * 
   * @param cells
   * @param colname
   * @return cell content
   */
  public String getCell(final List<String> cells, final String colname)
  {
    if (colMap == null) {
      log.error("No head cols given (may-be parseHeadCols() wasn't called before)!");
      return null;
    }
    final Integer idx = colMap.get(colname);
    if (idx == null) {
      log.error("No head col with name '" + colname + "' found by parseHeadCols()!");
      return null;
    }
    if (idx >= cells.size()) {
      log.error(
          "Index " + idx + " of colname '" + colname + "' out of index (>=" + cells.size() + ") in line " + lineno);
      return null;
    }
    return cells.get(idx);
  }

  /**
   * Returns null, if EOF.
   * 
   * @return
   */
  public List<String> parseLine()
  {
    if (type == Type.EOF) {
      return null;
    }
    List<String> result = null;
    do {
      final String cell = parseCell();
      if (cell != null) {
        if (result == null) {
          result = new ArrayList<String>();
        }
        result.add(cell);
      }
    } while (type != Type.EOF && type != Type.EOL);
    return result;
  }

  public String parseCell()
  {
    skipWhitespaces();
    nextToken();
    if (type != Type.CHAR) {
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
        if (type != Type.CHAR || cval == csvSeparatorChar) { // End of cell
          break;
        } else if (quoted == true && cval == '"') { // Escaped quotation mark
          buf.append(cval);
        } else if (Character.isWhitespace(cval) == true) {
          skipWhitespaces();
          nextToken();
          if (type != Type.CHAR || cval == csvSeparatorChar) {
            break;
          } else {
            throw new RuntimeException(createMessage(ERROR_DELIMITER_OR_NEW_LINE_EXPECTED_AFTER_QUOTATION_MARK));
          }
        } else {
          throw new RuntimeException(createMessage(ERROR_UNEXPECTED_CHARACTER_AFTER_QUOTATION_MARK));
        }
      } else if (quoted == false && cval == csvSeparatorChar) {
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
    this.csvSeparatorChar = csvSeparatorChar;
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
