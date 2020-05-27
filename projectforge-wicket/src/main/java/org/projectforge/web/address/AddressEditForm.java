/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.address.*;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.web.address.AddressPageSupport.AddressParameters;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.flowlayout.*;
import org.slf4j.Logger;
import org.wicketstuff.select2.Select2MultiChoice;

public class AddressEditForm extends AbstractEditForm<AddressDO, AddressEditPage> {
  private static final long serialVersionUID = 3881031215413525517L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AddressEditForm.class);

  protected AddressPageSupport addressEditSupport;

  @SpringBean
  private PersonalAddressDao personalAddressDao;

  @SpringBean
  private ConfigurationService configurationService;

  @SpringBean
  private AddressbookDao addressbookDao;

  public AddressEditForm(final AddressEditPage parentPage, final AddressDO data) {
    super(parentPage, data);
  }

  @Override
  protected void init() {
    super.init();
    addressEditSupport = new AddressPageSupport(this, gridBuilder, (AddressDao) getBaseDao(), personalAddressDao, data);
    /* GRID8 - BLOCK */
    gridBuilder.newSplitPanel(GridSize.COL100);
    {
      // Addressbooks
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("address.addressbooks"));
      final Select2MultiChoice<AddressbookDO> calendarSelect = new Select2MultiChoice<>(
              Select2MultiChoicePanel.WICKET_ID,
              new PropertyModel<>(data, "addressbookList"),
              new AddressbookWicketProvider(addressbookDao));
      calendarSelect.setRequired(true);
      calendarSelect.setMarkupId("addressbook-select").setOutputMarkupId(true);
      fs.add(new Select2MultiChoicePanel<>(fs.newChildId(), calendarSelect));
    }
    gridBuilder.newSplitPanel(GridSize.COL50, true).newSubSplitPanel(GridSize.COL50);
    addressEditSupport.addName();
    addressEditSupport.addFirstName();
    final FieldsetPanel fs = (FieldsetPanel) addressEditSupport.addFormOfAddress();
    final DivPanel checkBoxPanel = fs.addNewCheckBoxButtonDiv();
    checkBoxPanel.addCheckBoxButton(new PropertyModel<Boolean>(addressEditSupport.personalAddress, "favoriteCard"),
            getString("favorite"),
            getString("address.tooltip.vCardList"));
    addressEditSupport.addTitle();
    addressEditSupport.addWebsite();

    // /////////////////
    // Second box
    // /////////////////
    gridBuilder.newSubSplitPanel(GridSize.COL50);
    addressEditSupport.addOrganization();
    addressEditSupport.addDivision();
    addressEditSupport.addPosition();
    addressEditSupport.addEmail();
    addressEditSupport.addPrivateEmail();

    // /////////////////
    // Status
    // /////////////////
    gridBuilder.newSplitPanel(GridSize.COL50, true).newSubSplitPanel(GridSize.COL50);
    addressEditSupport.addBirthday();
    addressEditSupport.addLanguage();
    gridBuilder.newSubSplitPanel(GridSize.COL50);
    addressEditSupport.addContactStatus();
    addressEditSupport.addAddressStatus();

    // /////////////////
    // Phone numbers
    // /////////////////
    gridBuilder.newSubSplitPanel(GridSize.COL50);
    addressEditSupport.addPhoneNumber("businessPhone", "address.phone", "address.business", FieldType.PHONE_NO);
    addressEditSupport.addPhoneNumber("fax", "address.phoneType.fax", "address.business", FieldType.PHONE_NO);
    addressEditSupport.addPhoneNumber("mobilePhone", "address.phoneType.mobile", "address.business", FieldType.MOBILE_PHONE_NO);

    gridBuilder.newSubSplitPanel(GridSize.COL50);
    addressEditSupport.addPhoneNumber("privatePhone", "address.phone", "address.private", FieldType.PHONE_NO);
    addressEditSupport.addPhoneNumber("privateMobilePhone", "address.phoneType.mobile", "address.private", FieldType.MOBILE_PHONE_NO);

    // /////////////////
    // Addresses
    // /////////////////
    gridBuilder.newSplitPanel(GridSize.COL50, true).newSubSplitPanel(GridSize.COL100);
    addAddress(addressEditSupport.getBusinessAddressParameters());
    gridBuilder.newSubSplitPanel(GridSize.COL100);
    addAddress(addressEditSupport.getPostalAddressParameters());
    gridBuilder.newSplitPanel(GridSize.COL50, true).newSubSplitPanel(GridSize.COL100);
    addAddress(addressEditSupport.getPrivateAddressParameters());

    gridBuilder.newSubSplitPanel(GridSize.COL100);
    addressEditSupport.addFingerPrint();
    addressEditSupport.addPublicKey();
    addressEditSupport.addComment();

    addCloneButton();
  }

  private void addAddress(final AddressParameters params) {
    addressEditSupport.addAddressText(params.addressType, params.addressTextProperty);
    gridBuilder.newSubSplitPanel(GridSize.COL50);
    addressEditSupport.addZipCode(params.zipCodeProperty);
    addressEditSupport.addCity(params.cityProperty);
    gridBuilder.newSubSplitPanel(GridSize.COL50);
    addressEditSupport.addCountry(params.countryProperty);
    addressEditSupport.addState(params.stateProperty);
  }

  @Override
  protected Logger getLogger() {
    return log;
  }

  @Override
  public boolean isNew() {
    return super.isNew() || this.getParentPage().getCloneFlag();
  }
}
