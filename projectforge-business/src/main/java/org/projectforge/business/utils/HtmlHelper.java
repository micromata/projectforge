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

package org.projectforge.business.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

public class HtmlHelper
{
  private static HtmlHelper instance = new HtmlHelper();

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(HtmlHelper.class);

  public static final int TAB_WIDTH = 8;

  public static final String IMAGE_INFO_ICON = "images/information.png";

  /**
   * Only xml characters will be escaped (for compatibility with fop rendering engine).
   * 
   * @return
   * @see StringEscapeUtils#escapeXml(String)
   */
  public static final String escapeXml(final String str)
  {
    return StringEscapeUtils.escapeXml(str);
  }

  /**
   * @param str The string to convert.
   * @param createLineBreaks If true then new lines will be replaced by newlines and &lt;br/&gt;
   * @return
   * @see StringEscapeUtils#escapeHtml(String)
   */
  public static final String escapeHtml(final String str, final boolean createLineBreaks)
  {
    if (str == null) {
      return null;
    }
    final String result = StringEscapeUtils.escapeHtml(str);
    if (createLineBreaks == false) {
      return result;
    } else {
      if (result.contains("\r\n") == true) {
        return StringUtils.replace(result, "\r\n", "<br/>\r\n");
      } else {
        return StringUtils.replace(result, "\n", "<br/>\n");
      }
    }
  }

  /**
   * Returns ' &lt;attribute&gt;="&lt;value&gt;"', e. g. ' width="120px"'.
   * 
   * @param attribute
   * @param value
   * @return
   */
  public static String attribute(final String attribute, final String value)
  {
    final StringBuffer buf = new StringBuffer();
    return attribute(buf, attribute, value).toString();
  }

  /**
   * Returns ' &lt;attribute&gt;="&lt;value&gt;"', e. g. ' width="120px"'.
   * 
   * @param buf
   * @param attribute
   * @param value
   * @return
   */
  public static StringBuffer attribute(final StringBuffer buf, final String attribute, final String value)
  {
    return buf.append(" ").append(attribute).append("=\"").append(value).append("\"");
  }

  /**
   * Returns " &lt;attribute&gt;='&lt;value&gt;'", e. g. " width='120px'".
   * 
   * @param attribute
   * @param value
   * @return
   */
  public static String attributeSQ(final String attribute, final String value)
  {
    final StringBuffer buf = new StringBuffer();
    return attributeSQ(buf, attribute, value).toString();
  }

  /**
   * Returns " &lt;attribute&gt;='&lt;value&gt;'", e. g. " width='120px'".
   * 
   * @param buf
   * @param attribute
   * @param value
   * @return
   */
  public static StringBuffer attributeSQ(final StringBuffer buf, final String attribute, final String value)
  {
    return buf.append(" ").append(attribute).append("='").append(value).append("'");
  }

  public static String encodeUrl(final String url)
  {
    try {
      return URLEncoder.encode(url, "UTF-8");
    } catch (final UnsupportedEncodingException ex) {
      log.warn(ex);
      return url;
    }
  }

  protected static void addTooltip(final HtmlTagBuilder tag, final String tooltip)
  {
    tag.addAttribute("rel", "mytooltip");
    tag.addAttribute("data-original-title", tooltip);
  }

  /**
   * Creates anchor: &lt;a href="#" onclick='javascript:${method}("${params}")'&gt;
   * 
   * @param buf
   * @param params
   * @return
   */
  public static HtmlHelper appendAncorOnClickSubmitEventStartTag(final StringBuffer buf, final String method,
      final String... params)
  {
    Validate.notNull(params);
    final HtmlTagBuilder tag = new HtmlTagBuilder(buf, "a");
    tag.addAttribute("href", "#");
    if (params.length == 1) {
      // Standard code in over 90%, so avoid creation of new StringBuffer:
      tag.addAttribute("onclick", "javascript:" + method + "('" + params[0] + "')");
    } else {
      final StringBuffer s = new StringBuffer();
      for (int i = 0; i < params.length; i++) {
        s.append(params[i]);
        if (i < params.length - 1) {
          s.append("', '");
        }
      }
      tag.addAttribute("onclick", "javascript:" + method + "('" + s.toString() + "')");
    }
    tag.finishStartTag();
    return instance;
  }

  public static HtmlHelper appendAncorEndTag(final StringBuffer buf)
  {
    buf.append("</a>");
    return instance;
  }

  public static String getInfoImage()
  {
    return IMAGE_INFO_ICON;
  }

  /**
   * Replaces the new lines of the given string by &lt;br/&gt; and returns the result. Later the Wiki notation should be
   * supported.
   * 
   * @param str
   * @param escapeChars If true then the html characters of the given string will be quoted before.
   * @return
   * @see StringEscapeUtils#escapeXml(String)
   */
  public static String formatText(final String str, final boolean escapeChars)
  {
    if (StringUtils.isEmpty(str) == true) {
      return "";
    }
    String s = str;
    if (escapeChars == true) {
      s = escapeXml(str);
    }
    final StringBuffer buf = new StringBuffer();
    boolean doubleSpace = false;
    int col = 0;
    for (int i = 0; i < s.length(); i++) {
      final char ch = s.charAt(i);
      if (ch == '\n') {
        buf.append("<br/>");
        col = 0;
      } else if (ch == '\r') {
        // Do nothing
      } else if (ch == ' ') {
        if (doubleSpace == true) {
          buf.append("&nbsp;");
        } else {
          buf.append(' ');
        }
      } else if (ch == '\t') {
        do {
          buf.append("&nbsp;");
          ++col;
        } while (col % TAB_WIDTH > 0);
      } else {
        buf.append(ch);
        ++col;
      }
      if (Character.isWhitespace(ch) == true) {
        doubleSpace = true;
      } else {
        doubleSpace = false;
      }
    }
    return buf.toString();
  }

  public static String formatXSLFOText(final String str, final boolean escapeChars)
  {
    String s = str;
    if (escapeChars == true) {
      s = escapeXml(str);
    }
    return StringUtils.replace(s, "\n", "<br/>");
  }
}
