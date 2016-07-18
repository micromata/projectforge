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

package org.projectforge.web.calendar;

import org.apache.wicket.Component;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.border.BorderBehavior;

/**
 * Appends a div html element to an existing panel.
 * 
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 */
public class QRCodeDivAppenderBehavior extends BorderBehavior
{

  @Override
  public void bind(Component component)
  {
    super.bind(component);
    component.setOutputMarkupId(true);
  }

  @Override
  public void renderHead(Component component, IHeaderResponse response)
  {
    super.renderHead(component, response);
    response.render(JavaScriptHeaderItem.forUrl("scripts/qrcode.js"));
    response.render(OnDomReadyHeaderItem.forScript("$(function() {\n" +
        "var urlComponent = $('#" + component.getMarkupId() + "');\n" +
        "if (urlComponent.val() != undefined && urlComponent.val().length > 0) {\n"
        + "if ($( \"div.pf_qrcode\" ).size() > 1) {\n" +
        "$( \"div.pf_qrcode\" ).last().remove();\n"
        + "}\n"
        + "var qrCode = new QRCode(urlComponent.siblings('.pf_qrcode')[0], {width: 250, height: 250});\n" +
        "qrCode.makeCode(urlComponent.val());\n" +
        "}\n" +
        "});"));
  }
}
