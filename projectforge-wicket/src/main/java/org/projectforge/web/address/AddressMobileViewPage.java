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
import org.apache.log4j.Logger;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.common.StringHelper;
import org.projectforge.web.address.AddressPageSupport.AddressParameters;
import org.projectforge.web.mobile.AbstractMobileViewPage;
import org.projectforge.web.mobile.AbstractSecuredMobilePage;
import org.projectforge.web.mobile.CollapsiblePanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldProperties;
import org.projectforge.web.wicket.flowlayout.FieldType;
import org.projectforge.web.wicket.mobileflowlayout.LabelValueDataTablePanel;

public class AddressMobileViewPage extends AbstractMobileViewPage<AddressDO, AddressDao>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AddressMobileViewPage.class);

  private static final long serialVersionUID = 4478785262257939098L;

  @SpringBean
  private AddressDao addressDao;

  private final AddressPageSupport pageSupport;

  public AddressMobileViewPage(final PageParameters parameters)
  {
    super(parameters);
    pageSupport = new AddressPageSupport(gridBuilder, data);
    gridBuilder.newCollapsiblePanel(data.getFullNameWithTitleAndForm());
    final LabelValueDataTablePanel table = gridBuilder.newLabelValueDataTable();
    table.addRow(pageSupport.getOrganizationProperties());
    table.addRow(pageSupport.getPositionTextProperties());
    table.addRow(pageSupport.getAddressStatusProperties());
    table.addRow(pageSupport.getWebsiteProperties());
    addAddress(pageSupport.getBusinessAddressParameters(), "businessPhone", "mobilePhone", "fax",
        pageSupport.getEmailProperties());
    addAddress(pageSupport.getPrivateAddressParameters(), "privatePhone", "privateMobilePhone", null,
        pageSupport.getPrivateEmailProperties());
    addAddress(pageSupport.getPostalAddressParameters(), null, null, null, null);
    final FieldProperties<String> comment = pageSupport.getCommentProperties();
    if (StringUtils.isNotBlank(comment.getValue()) == true) {
      final CollapsiblePanel panel = gridBuilder.newCollapsiblePanel(getString(comment.getLabel())).setCollapsed();
      panel.add(new DivTextPanel(panel.newChildId(), comment.getValue()));
    }
  }

  private void addAddress(final AddressParameters addressParameters, final String phone, final String mobile,
      final String fax,
      final FieldProperties<String> emailProp)
  {
    final FieldProperties<String> addressTextProp = pageSupport.getAddressTextProperties(addressParameters.addressType,
        addressParameters.addressTextProperty);
    final FieldProperties<String> cityProp = pageSupport.getCityProperties(addressParameters.cityProperty);
    final FieldProperties<String> zipCodeProp = pageSupport.getCityProperties(addressParameters.zipCodeProperty);
    cityProp.setValueAsString(StringUtils.defaultString(zipCodeProp.getModel().getObject())
        + " "
        + StringUtils.defaultString(cityProp.getModel().getObject()));
    final FieldProperties<String> countryProp = pageSupport.getCountryProperties(addressParameters.countryProperty);
    final FieldProperties<String> phoneProp = phone != null
        ? pageSupport.getPhoneNumberProperties(phone, "address.phone", null,
            FieldType.PHONE_NO)
        : null;
    final String phoneValue = phoneProp != null ? phoneProp.getValue() : null;
    final FieldProperties<String> mobilePhoneProp = mobile != null ? pageSupport.getPhoneNumberProperties(mobile,
        "address.phoneType.mobile", null, FieldType.MOBILE_PHONE_NO) : null;
    final String mobileValue = mobilePhoneProp != null ? mobilePhoneProp.getValue() : null;
    final FieldProperties<String> faxProp = fax != null
        ? pageSupport.getPhoneNumberProperties(fax, "address.phoneType.fax", null, null)
        : null;
    final String faxValue = faxProp != null ? faxProp.getValue() : null;
    final String emailValue = emailProp != null ? emailProp.getValue() : null;
    if (StringHelper.isBlank(addressTextProp.getValue(), cityProp.getValueAsString(), countryProp.getValue(),
        phoneValue, mobileValue,
        faxValue, emailValue) == true) {
      // Do nothing.
      return;
    }
    gridBuilder.newCollapsiblePanel(addressParameters.addressType);
    final LabelValueDataTablePanel table = gridBuilder.newLabelValueDataTable();
    table.addRow(addressTextProp);
    table.addRow(cityProp);
    table.addRow(countryProp);
    if (phoneProp != null) {
      table.addRow(phoneProp);
    }
    if (mobilePhoneProp != null) {
      table.addRow(mobilePhoneProp);
    }
    if (faxProp != null) {
      table.addRow(faxProp);
    }
    if (emailProp != null) {
      table.addRow(emailProp);
    }
  }

  @Override
  protected AddressDao getBaseDao()
  {
    return addressDao;
  }

  /**
   * @see org.projectforge.web.mobile.AbstractMobileViewPage#getListPageClass()
   */
  @Override
  protected Class<? extends AbstractSecuredMobilePage> getListPageClass()
  {
    return AddressMobileListPage.class;
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  /**
   * @see org.projectforge.web.mobile.AbstractMobilePage#getTitle()
   */
  @Override
  protected String getTitle()
  {
    return getString("address.title.view");
  }
}
