/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.attributes.AjaxCallListener;

/**
 * Prevents multiple clicks while ajax request is executing. We keep a variable that is set to {@code true} while the
 * request is running and to any other value when its done.
 *
 * @author igor
 */
public class BlockingDecorator extends AjaxCallListener
{

  private static String clean(String str)
  {
    return str != null ? str.replaceAll("[^0-9a-zA-Z]", "") : null;
  }

  private String var(Component component)
  {
    if (!component.getOutputMarkupId()) {
      throw new IllegalStateException();
    }
    // Calling clean() ensures that no Javascript operators (+, -, etc) are accidentally
    // used in the markup id, which breaks this functionality.
    String id = clean(component.getMarkupId());
    return "window.wicketblock" + id;

  }

  @Override
  public CharSequence getPrecondition(Component component)
  {
    // before we allow the request we check if one is already running by checking the var

    // return false if the var is set to true (request running)
    return var(component) + "!==true;";
  }

  @Override
  public CharSequence getBeforeSendHandler(Component component)
  {
    // just before we start the request, we set the var to true
    return var(component) + "=true;";
  }

  @Override
  public CharSequence getCompleteHandler(Component component)
  {
    // when the request is complete we set the var to false
    return var(component) + "=false;";
  }
}
