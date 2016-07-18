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

package org.projectforge.web.address;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.business.address.PhoneType;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.flowlayout.IconPanel;
import org.projectforge.web.wicket.flowlayout.IconType;

public class AddressListPhoneNumberPanel extends Panel
{
  private static final long serialVersionUID = 2546695290892989291L;

  @SuppressWarnings("serial")
  public AddressListPhoneNumberPanel(final String id, final AddressListPage parentPage, final Integer addressId,
      final PhoneType phoneType,
      final String phoneNumber, final boolean favoriteNumber, final boolean sendSms, final IconType icon,
      final boolean first)
  {
    super(id);
    final WebMarkupContainer linkOrSpan;
    if (parentPage.phoneCallSupported == true) {
      linkOrSpan = new Link<String>("directCallLink")
      {
        @Override
        public void onClick()
        {
          final PageParameters params = new PageParameters();
          params.add(PhoneCallPage.PARAMETER_KEY_ADDRESS_ID, addressId);
          params.add(PhoneCallPage.PARAMETER_KEY_NUMBER, phoneNumber);
          setResponsePage(new PhoneCallPage(params));
        }
      };
      add(WicketUtils.getInvisibleComponent("phoneNumber"));
    } else {
      linkOrSpan = new WebMarkupContainer("phoneNumber");
      add(WicketUtils.getInvisibleComponent("directCallLink"));
    }
    linkOrSpan.add(AttributeModifier.replace("onmouseover", "zoom('" + phoneNumber + "'); return false;"));
    final String tooltip = parentPage.getString(phoneType.getI18nKey());
    add(linkOrSpan);
    WicketUtils.addTooltip(linkOrSpan, tooltip);
    final Label numberLabel = new Label("number", phoneNumber);
    if (favoriteNumber == true) {
      numberLabel.add(AttributeModifier.replace("style", "color:red; font-weight:bold;"));
    } else {
      numberLabel.setRenderBodyOnly(true);
    }
    linkOrSpan.add(numberLabel);
    linkOrSpan.add(new IconPanel("phoneImage", icon));
    final Link<String> sendMessage = new Link<String>("sendMessageLink")
    {
      @Override
      public void onClick()
      {
        final PageParameters params = new PageParameters();
        params.add(SendSmsPage.PARAMETER_KEY_ADDRESS_ID, addressId);
        params.add(SendSmsPage.PARAMETER_KEY_PHONE_TYPE, phoneType.toString());
        setResponsePage(SendSmsPage.class, params);
      }
    };
    if (sendSms == false || parentPage.messagingSupported == false) {
      sendMessage.setVisible(false);
    }
    add(sendMessage);
  }
}
