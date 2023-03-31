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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;

import java.util.UUID;

abstract class AbstractAjaxCallbackWithClientsideRevert extends AbstractAjaxCallback
{

  private String uuid = "u" + UUID.randomUUID().toString().replace("-", "");

  protected abstract String getRevertScript();

  protected abstract boolean onEvent(AjaxRequestTarget target);

  private String getRevertScriptBlock()
  {
    return String.format("{%s;}", getRevertScript());
  }

  @Override
  protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
  {
    super.updateAjaxAttributes(attributes);

    AjaxCallListener ajaxCallListener = new AjaxCallListener();
    ajaxCallListener.onSuccess(getSuccessScript());
    ajaxCallListener.onFailure(getRevertScriptBlock());
    attributes.getAjaxCallListeners().add(ajaxCallListener);
  }

  @Override
  protected final void respond(AjaxRequestTarget target)
  {
    boolean result = onEvent(target);
    target.prependJavaScript(String.format("$.data(document, '%s', %s);", uuid, String.valueOf(result)));
  }

  protected final CharSequence getSuccessScript()
  {
    return String.format("if (false===$.data(document, '%s')) %s $.removeData(document, '%s');", uuid,
        getRevertScriptBlock(), uuid);
  }

}
