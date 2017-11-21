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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.address.PhoneType;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredPage;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;
import org.projectforge.web.wicket.components.ExternalLinkPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.Heading1Panel;
import org.projectforge.web.wicket.flowlayout.Heading3Panel;
import org.projectforge.web.wicket.flowlayout.ParTextPanel;

public class AddressViewPage extends AbstractSecuredPage
{
  private static final long serialVersionUID = 6317382828021216284L;

  @SpringBean
  private AddressDao addressDao;

  @SpringBean
  private ConfigurationService configurationService;

  private AddressDO address;

  private GridBuilder gridBuilder;

  public AddressViewPage(final PageParameters parameters)
  {
    this(parameters, null);
  }

  @SuppressWarnings("serial")
  public AddressViewPage(final PageParameters parameters, final AbstractSecuredPage returnToPage)
  {
    super(parameters);
    this.returnToPage = returnToPage;
    if (parameters.get(AbstractEditPage.PARAMETER_KEY_ID).isEmpty() == false) {
      final Integer addressId = parameters.get(AbstractEditPage.PARAMETER_KEY_ID).toInteger();
      address = addressDao.getById(addressId);
    }
    if (address == null && returnToPage != null) {
      setResponsePage(returnToPage);
      return;
    }
    if (this.returnToPage != null) {
      final ContentMenuEntryPanel back = new ContentMenuEntryPanel(getNewContentMenuChildId(), new Link<Object>("link")
      {
        @Override
        public void onClick()
        {
          setResponsePage(returnToPage);
        }
      }, getString("back"));
      addContentMenuEntry(back);
    }
    {
      final ContentMenuEntryPanel edit = new ContentMenuEntryPanel(getNewContentMenuChildId(), new Link<Object>("link")
      {
        @Override
        public void onClick()
        {
          final PageParameters params = new PageParameters();
          params.add(AbstractEditPage.PARAMETER_KEY_ID, address.getId());
          final AddressEditPage addressEditPage = new AddressEditPage(params);
          addressEditPage.setReturnToPage(AddressViewPage.this);
          setResponsePage(addressEditPage);
        }
      }, getString("edit"));
      addContentMenuEntry(edit);
    }
    if (configurationService.isTelephoneSystemUrlConfigured() == true) {
      final ContentMenuEntryPanel menu = new ContentMenuEntryPanel(getNewContentMenuChildId(),
          new Link<Void>(ContentMenuEntryPanel.LINK_ID)
          {
            @Override
            public void onClick()
            {
              final Integer addressId = address.getId();
              final PageParameters params = new PageParameters();
              params.add(PhoneCallPage.PARAMETER_KEY_ADDRESS_ID, addressId);
              setResponsePage(new PhoneCallPage(params));
            }
          }, getString("address.directCall.call"));
      addContentMenuEntry(menu);
    }
    if (configurationService.isSmsConfigured() == true
        && StringHelper.isNotBlank(address.getMobilePhone(), address.getPrivateMobilePhone()) == true) {
      final ContentMenuEntryPanel menu = new ContentMenuEntryPanel(getNewContentMenuChildId(),
          new Link<Void>(ContentMenuEntryPanel.LINK_ID)
          {
            @Override
            public void onClick()
            {
              final Integer addressId = address.getId();
              final PageParameters params = new PageParameters();
              params.add(SendSmsPage.PARAMETER_KEY_ADDRESS_ID, addressId);
              if (StringUtils.isNotBlank(address.getPrivateMobilePhone()) == true) {
                params.add(SendSmsPage.PARAMETER_KEY_PHONE_TYPE, PhoneType.PRIVATE_MOBILE.toString());
              } else {
                params.add(SendSmsPage.PARAMETER_KEY_PHONE_TYPE, PhoneType.MOBILE.toString());
              }
              setResponsePage(new SendSmsPage(params));
            }
          }, getString("address.sendSms.title"));
      addContentMenuEntry(menu);
    }

    gridBuilder = new GridBuilder(body, "flowform", true);
    final String name = address.getFullNameWithTitleAndForm();

    gridBuilder.newSplitPanel(GridSize.COL50);
    DivPanel section = gridBuilder.getPanel();
    section.add(new Heading1Panel(section.newChildId(), name));
    appendFieldset("organization", address.getOrganization());
    appendFieldset("address.division", address.getDivision());
    appendFieldset("address.positionText", address.getPositionText());
    appendEmailFieldset("email", address.getEmail());
    appendEmailFieldset("address.privateEmail", address.getPrivateEmail());
    appendFieldset("address.website", address.getWebsite());
    final String birthday = address.getBirthday() != null
        ? DateTimeFormatter.instance().getFormattedDate(address.getBirthday()) : null;
    appendFieldset("address.birthday", birthday);
    // addRow("publicKey", address.getPublicKey());
    // addRow("fingerprint", address.getFingerprint());

    boolean firstRow = addAddressRow("address.heading.postalAddress", name, address.getOrganization(),
        address.getPostalAddressText(),
        address.getPostalZipCode(), address.getPostalCity(), address.getPostalCountry(), address.getPostalState(), null,
        null, null, true);
    firstRow = addAddressRow("address.business", name, address.getOrganization(), address.getAddressText(),
        address.getZipCode(),
        address.getCity(), address.getCountry(), address.getState(), address.getBusinessPhone(),
        address.getMobilePhone(),
        address.getFax(), firstRow);
    firstRow = addAddressRow("address.private", name, null, address.getPrivateAddressText(),
        address.getPrivateZipCode(),
        address.getPrivateCity(), address.getPrivateCountry(), address.getPrivateState(), address.getPrivatePhone(),
        address.getPrivateMobilePhone(), null, firstRow);

    if (StringUtils.isNotBlank(address.getComment()) == true) {
      gridBuilder.newGridPanel();
      section = gridBuilder.getPanel();
      section.add(new Heading3Panel(section.newChildId(), getString("comment")));
      final ParTextPanel textPanel = new ParTextPanel(section.newChildId(),
          HtmlHelper.escapeHtml(address.getComment(), true));
      textPanel.getLabel().setEscapeModelStrings(false);
      section.add(textPanel);
    }
  }

  private boolean addAddressRow(final String type, final String name, final String organization,
      final String addressText,
      final String zipCode, final String city, final String country, final String state, final String phone,
      final String mobile,
      final String fax, final boolean firstRow)
  {
    if (StringHelper.isNotBlank(addressText, zipCode, city, country, state, phone, mobile, fax) == false) {
      return firstRow;
    }
    if (firstRow == true) {
      gridBuilder.newSplitPanel(GridSize.COL50);
    }
    final DivPanel section = gridBuilder.getPanel();
    if (firstRow == true) {
      section.add(new Heading1Panel(section.newChildId(), getString("address.addresses")));
    }
    section.add(new ParTextPanel(section.newChildId(), getString(type) + ":"));
    final StringBuffer buf = new StringBuffer();
    boolean first = true;
    if (organization != null) {
      section.add(new Heading3Panel(section.newChildId(), organization));
      first = appendRow(buf, first, name);
    } else {
      section.add(new Heading3Panel(section.newChildId(), name));
    }
    if (StringUtils.isNotBlank(addressText) == true) {
      first = appendRow(buf, first, addressText);
    }
    if (StringUtils.isNotBlank(zipCode) == true || StringUtils.isNotBlank(city) == true) {
      final StringBuffer buf2 = new StringBuffer();
      if (zipCode != null) {
        buf2.append(zipCode).append(" ");
      }
      if (city != null) {
        buf2.append(city);
      }
      first = appendRow(buf, first, buf2.toString());
    }
    if (StringUtils.isNotBlank(country) == true) {
      first = appendRow(buf, first, country);
    }
    if (StringUtils.isNotBlank(state) == true) {
      first = appendRow(buf, first, state);
    }
    if (StringUtils.isNotBlank(phone) == true) {
      first = appendRow(buf, first, getString("address.phone") + ": " + phone);
    }
    if (StringUtils.isNotBlank(fax) == true) {
      first = appendRow(buf, first, getString("address.phoneType.fax") + ": " + fax);
    }
    if (StringUtils.isNotBlank(mobile) == true) {
      first = appendRow(buf, first, getString("address.phoneType.mobile") + ": " + mobile);
    }
    final ParTextPanel text = new ParTextPanel(section.newChildId(), buf.toString());
    text.getLabel().setEscapeModelStrings(false);
    section.add(text);
    return false;
  }

  private boolean appendRow(final StringBuffer buf, final boolean first, final String str)
  {
    return StringHelper.append(buf, first, HtmlHelper.escapeXml(str), "<br/>");
  }

  private boolean appendFieldset(final String label, final String value)
  {
    if (StringUtils.isBlank(value) == true) {
      return false;
    }
    final FieldsetPanel fs = gridBuilder.newFieldset(getString(label)).suppressLabelForWarning();
    fs.add(new DivTextPanel(fs.newChildId(), value));
    return true;
  }

  private boolean appendEmailFieldset(final String label, final String value)
  {
    if (StringUtils.isBlank(value) == true) {
      return false;
    }
    final FieldsetPanel fs = gridBuilder.newFieldset(getString(label)).suppressLabelForWarning();
    fs.add(new ExternalLinkPanel(fs.newChildId(), "mailto:" + value, value));
    return true;
  }

  @Override
  protected String getTitle()
  {
    return getString("address.view.title");
  }

}
