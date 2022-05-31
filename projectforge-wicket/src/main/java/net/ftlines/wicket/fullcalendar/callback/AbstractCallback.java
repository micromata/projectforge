/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package net.ftlines.wicket.fullcalendar.callback;

import net.ftlines.wicket.fullcalendar.FullCalendar;
import org.apache.wicket.Component;
import org.apache.wicket.IRequestListener;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Map;

abstract class AbstractCallback extends Behavior implements IRequestListener
{
  private FullCalendar calendar;

  @Override
  public void bind(Component component)
  {
    super.bind(component);
    this.calendar = (FullCalendar) component;
  }

  protected final String getUrl(Map<String, Object> parameters)
  {
    PageParameters params = new PageParameters();
    String url = calendar.urlForListener(params).toString();

    if (parameters != null) {
      for (Map.Entry<String, Object> parameter : parameters.entrySet()) {
        url += "&" + parameter.getKey() + "=" + parameter.getValue();
      }
    }
    return url;
  }

  @Override
  public final void onRequest()
  {
    respond();
  }

  protected abstract void respond();

  protected final FullCalendar getCalendar()
  {
    return calendar;
  }

  @Override
  public boolean getStatelessHint(Component component)
  {
    return false;
  }
}
