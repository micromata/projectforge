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

package org.projectforge.web.wicket;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.model.util.MapModel;

/**
 * Helper for parameterized Wicket i18n messages.
 * 
 * <code>
 *  getString("message.download", new I18nParams().put("title", data.getTitle().put("date", "2013-04-06"));
 *  i18n.properties: message.download=This is a message for ${title}. The date is ${date}.
 * </code>
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class I18nParamMap extends MapModel<String, String>
{
  private static final long serialVersionUID = -431038619973608328L;

  private final Map<String, String> map = new HashMap<String, String>();

  public I18nParamMap()
  {
    super();
    setObject(map);
  }

  public I18nParamMap put(final String parameterName, final String value)
  {
    map.put(parameterName, value);
    return this;
  }
}
