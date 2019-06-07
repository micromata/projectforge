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

package org.projectforge.business.utils;

import java.util.ArrayList;
import java.util.Collection;

import org.projectforge.framework.utils.KeyValueBean;

/**
 * Helper for building and writing html tags.
 */
public class HtmlTagBuilder
{
  //private static final Logger log = Logger.getLogger(HtmlTagBuilder.class);
  protected String name = null;
  protected Collection<KeyValueBean<String, String>> attrs = null;
  protected StringBuffer stringBuffer = null;

  /**
   * Creates a new HtmlTagBuilder and an empty collection of attributes.
   * 
   * @param name Name of the HTML element.
   * @see #HtmlTagBuilder(String) For quick and dirty mode for writing of tag strings.
   */
  public HtmlTagBuilder(String name)
  {
    this.name = name;
    attrs = new ArrayList<KeyValueBean<String, String>>();
  }

  /**
   * Use this constructor for quick and dirty mode for writing of tag strings.
   * 
   * @param name Name of the HTML element.
   * @param buf StringBuffer to append element and attributes directly.
   */
  public HtmlTagBuilder(StringBuffer buf, String name)
  {
    this.name = name;
    this.stringBuffer = buf;
    this.stringBuffer.append("<");
    this.stringBuffer.append(name);
  }

  /**
   * Use this method for adding local hrefs and sources. TODO: Add c:url functionality.
   */
  public void addUrl(String url, String value)
  {
    addAttribute(url, value);
  }

  /**
   * Adds a attribute. For quick and dirty mode, the attribute will be appended to the StringBuffer given in
   * constructor. Please note: The attribute will be ignored, if the value is null;
   * 
   * @param name Name of the attribute.
   * @param value The value of the attribute. Null value are allowed. Those attributes will be ignored.
   */
  public void addAttribute(String name, String value)
  {
    if (value == null) {
      // Do nothing
      return;
    }
    if (stringBuffer != null) {
      stringBuffer.append(" ");
      stringBuffer.append(name);
      stringBuffer.append("=\"");
      stringBuffer.append(value);
      stringBuffer.append("\"");
    } else {
      attrs.add(new KeyValueBean<String, String>(name, value));
    }
  }

  /**
   * Appends '>' character for closing the start tag. Only useable in quick and dirty mode. Otherwise a
   * NullPointerException will be thrown.
   */
  public void finishStartTag()
  {
    this.stringBuffer.append(">");
  }

  /**
   * Appends " />" character for closing the start tag. Only useable in quick and dirty mode. Otherwise a
   * NullPointerException will be thrown.
   */
  public void finishEmptyTag()
  {
    this.stringBuffer.append(" />");
  }

  /** Gets the start tag. The attributes added before will be ignored in quick and dirty mode. */
  public String getStartTag()
  {
    StringBuffer buf = new StringBuffer();
    buf.append("<");
    buf.append(name);
    buf.append(">");
    if (attrs != null && attrs.size() > 0) {
      for (KeyValueBean<String, String> prop : attrs) {
        buf.append(" ");
        buf.append(prop.getKey());
        buf.append("=\"");
        buf.append(prop.getValue());
        buf.append("\"");
      }
    }
    return buf.toString();
  }

  public String getEndTag()
  {
    return "</" + this.name + ">";
  }
}
