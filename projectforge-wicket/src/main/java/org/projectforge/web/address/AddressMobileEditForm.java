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

import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.address.PersonalAddressDao;
import org.projectforge.web.address.AddressPageSupport.AddressParameters;
import org.projectforge.web.mobile.AbstractMobileEditForm;
import org.projectforge.web.wicket.flowlayout.FieldType;

public class AddressMobileEditForm extends AbstractMobileEditForm<AddressDO, AddressMobileEditPage>
{
  private static final long serialVersionUID = -8781593985402346929L;

  @SpringBean
  private AddressDao addressDao;

  @SpringBean
  private PersonalAddressDao personalAddressDao;

  protected AddressPageSupport pageSupport;

  public AddressMobileEditForm(final AddressMobileEditPage parentPage, final AddressDO data)
  {
    super(parentPage, data);
  }

  @Override
  protected void init()
  {
    super.init();
    pageSupport = new AddressPageSupport(this, gridBuilder, addressDao, personalAddressDao, data);
    gridBuilder.newCollapsiblePanel(data.getFullNameWithTitleAndForm());
    pageSupport.addFormOfAddress();
    pageSupport.addTitle();
    pageSupport.addFirstName();
    pageSupport.addName();
    pageSupport.addContactStatus();
    pageSupport.addBirthday();
    pageSupport.addOrganization();
    pageSupport.addDivision();
    pageSupport.addPosition();
    pageSupport.addWebsite();
    pageSupport.addAddressStatus();
    // addressEditSupport.addLanguage(); // Autocomplete doesn't work yet
    addAddress(pageSupport.getBusinessAddressParameters(), "businessPhone", "mobilePhone", "fax");
    pageSupport.addEmail();
    addAddress(pageSupport.getPrivateAddressParameters(), "privatePhone", "privateMobilePhone", null);
    pageSupport.addPrivateEmail();
    addAddress(pageSupport.getPostalAddressParameters(), null, null, null);

    gridBuilder.newCollapsiblePanel(getString("comment")).setCollapsed();
    pageSupport.addComment();
  }

  private void addAddress(final AddressParameters addressParameters, final String phone, final String mobile,
      final String fax)
  {
    gridBuilder.newCollapsiblePanel(addressParameters.addressType).setCollapsed();
    pageSupport.addAddressText(addressParameters.addressType, addressParameters.addressTextProperty);
    pageSupport.addZipCode(addressParameters.zipCodeProperty);
    pageSupport.addCity(addressParameters.cityProperty);
    if (phone != null) {
      pageSupport.addPhoneNumber(phone, "address.phone", null, FieldType.PHONE_NO);
    }
    if (mobile != null) {
      pageSupport.addPhoneNumber(mobile, "address.phoneType.mobile", null, FieldType.MOBILE_PHONE_NO);
    }
    if (fax != null) {
      pageSupport.addPhoneNumber(fax, "address.phoneType.fax", null, FieldType.PHONE_NO);
    }
  }
}
