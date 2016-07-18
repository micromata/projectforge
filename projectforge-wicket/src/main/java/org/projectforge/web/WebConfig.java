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

import org.apache.wicket.markup.html.WebPage;
import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.xstream.XmlField;
import org.projectforge.framework.xstream.XmlObject;
import org.projectforge.web.calendar.CalendarPage;
import org.projectforge.web.wicket.WicketApplication;

/**
 * Example of a web config inside the config.xml file:
 * 
 * <pre>
 * &lt;config&gt;
 *   ...
 *   &lt;web defaultPage="org.projectforge.web.calendar.CalendarPage" /&gt;
 *   ...
 * &lt;/config&gt;
 * </pre>
 * 
 * <br/>
 * See all the predefined id's here: {@link MenuItemDef} <br/>
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@XmlObject(alias = "web")
public class WebConfig
{
  @XmlField(asAttribute = true)
  private Class< ? extends WebPage> defaultPage;

  /**
   * The default page is the standard page shown after login or after finishing any action without an defined page to go next. The default
   * page is at default {@link CalendarPage}.
   * @return the defaultPage
   */
  public Class< ? extends WebPage> getDefaultPage()
  {
    if (defaultPage == null) {
      return WicketApplication.internalGetDefaultPage();
    }
    return defaultPage;
  }

  /**
   * @param defaultPage the defaultPage to set
   * @return this for chaining.
   */
  public WebConfig setDefaultPage(final Class< ? extends WebPage> defaultPage)
  {
    this.defaultPage = defaultPage;
    return this;
  }

  /**
   * @see ConfigXml#toString(Object)
   */
  @Override
  public String toString()
  {
    return ConfigXml.toString(this);
  }
}
