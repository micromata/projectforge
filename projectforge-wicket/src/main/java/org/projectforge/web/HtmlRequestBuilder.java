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

package org.projectforge.web;

import java.util.ArrayList;
import java.util.Collection;

import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.framework.utils.KeyValueBean;

/**
 * Helper for building and writing html tags.
 */
public class HtmlRequestBuilder
{
  //private static final Logger log = Logger.getLogger(HtmlRequestBuilder.class);
  protected Collection<KeyValueBean<String, String>> attrs = null;
  protected StringBuffer stringBuffer = null;
  protected boolean firstAttribute = true;
  protected String url = null;

  /**
   * Creates a new HtmlRequest and an empty collection of attributes. TODO: c:url
   * 
   * @param url Url of the request.
   * @see #HtmlTag(String) For quick and dirty mode for writing of request strings.
   */
  public HtmlRequestBuilder(String url)
  {
    this.url = url;
    attrs = new ArrayList<KeyValueBean<String, String>>();
  }

  /**
   * Use this constructor for quick and dirty mode for writing request strings.
   * 
   * @param name Name of the HTML element.
   * @param buf StringBuffer to url and attributes directly.
   */
  public HtmlRequestBuilder(StringBuffer buf, String url)
  {
    this.url = url;
    buf.append(url);
  }

  /**
   * Adds a parameter. For quick and dirty mode, the attribute will be appended to the StringBuffer given in
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
      if (this.firstAttribute == true) {
        stringBuffer.append("?");
        this.firstAttribute = false;
      } else {
        stringBuffer.append("&");
      }
      stringBuffer.append(name);
      stringBuffer.append(HtmlHelper.encodeUrl(value));
    } else {
      attrs.add(new KeyValueBean<String, String>(name, value));
    }
  }

  /** Gets the request string. The attributes added before will be ignored in quick and dirty mode. */
  public String getRequestString()
  {
    StringBuffer buf = new StringBuffer();
    buf.append(url);
    boolean first = true;
    if (attrs != null) {
      for (KeyValueBean<String, String> prop : attrs) {
        if (first == true) {
          buf.append("?");
          first = false;
        } else {
          buf.append("&");
        }
        buf.append(prop.getKey());
        buf.append("=");
        buf.append(HtmlHelper.encodeUrl(prop.getValue().toString()));
      }
    }
    return buf.toString();
  }
}
