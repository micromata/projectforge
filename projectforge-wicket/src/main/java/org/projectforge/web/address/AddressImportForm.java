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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.vcard.Parameter;
import net.fortuna.ical4j.vcard.Parameter.Id;
import net.fortuna.ical4j.vcard.Property;
import net.fortuna.ical4j.vcard.VCard;
import net.fortuna.ical4j.vcard.VCardBuilder;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.PropertyModel;
import org.joda.time.DateTime;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressStatus;
import org.projectforge.business.address.ContactStatus;
import org.projectforge.business.address.FormOfAddress;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.FileUploadPanel;

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * 
 */
public class AddressImportForm extends AbstractEditForm<AddressDO, AddressImportPage>
{
  private static final long serialVersionUID = -1691614676645602272L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AddressImportForm.class);

  private final List<FileUpload> uploads = new LinkedList<FileUpload>();

  /**
   * @param parentPage
   * @param data
   */
  public AddressImportForm(final AddressImportPage parentPage, final AddressDO data)
  {
    super(parentPage, data);
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();
    gridBuilder.newSplitPanel(GridSize.COL50);
    final FieldsetPanel newFieldset = gridBuilder.newFieldset(getString("address.book.vCardImport.fileUploadPanel"));

    final FileUploadField uploadField = new FileUploadField(FileUploadPanel.WICKET_ID, new PropertyModel<List<FileUpload>>(this, "uploads"));
    newFieldset.add(new FileUploadPanel(newFieldset.newChildId(), uploadField));
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditForm#getLogger()
   */
  @Override
  protected Logger getLogger()
  {
    return log;
  }

  @SuppressWarnings("serial")
  public void create()
  {
    if (uploads != null) {
      final FileUpload upload = uploads.get(0);
      if (upload.getClientFileName().endsWith(".vcf") == false) {
        feedbackPanel.error(getString("address.book.vCardImport.wrongFileType"));
      } else {
        try {
          final File file = upload.writeToTempFile();
          final List<AddressDO> newAddresses = new ArrayList<AddressDO>();

          final FileInputStream fis = new FileInputStream(file);
          final VCardBuilder builder = new VCardBuilder(fis);

          AddressDO newAddress = null;
          for (final VCard card : builder.buildAll()) {
            newAddress = new AddressDO();

            // //// SET BASE DATA
            if (card.getProperty(net.fortuna.ical4j.vcard.Property.Id.N) != null) {
              setName(card.getProperty(net.fortuna.ical4j.vcard.Property.Id.N), newAddress);
            }
            if (card.getProperty(net.fortuna.ical4j.vcard.Property.Id.ORG) != null) {
              setOrganization(card.getProperty(net.fortuna.ical4j.vcard.Property.Id.ORG), newAddress);
            }
            if (card.getProperty(net.fortuna.ical4j.vcard.Property.Id.BDAY) != null) {
              setBirth(card.getProperty(net.fortuna.ical4j.vcard.Property.Id.BDAY), newAddress);
            }
            if (card.getProperty(net.fortuna.ical4j.vcard.Property.Id.NOTE) != null) {
              setNote(card.getProperty(net.fortuna.ical4j.vcard.Property.Id.NOTE), newAddress);
            }

            // //// SET ADDITIONAL DATA
            final List<Property> li = card.getProperties();
            setProperties(li, newAddress);

            // handle item entries
            final VCardItemElementHandler ih = new VCardItemElementHandler(new FileInputStream(file));
            if (!ih.getItemList().isEmpty())
              setProperties(ih.getItemList(), newAddress);

            newAddress.setAddressStatus(AddressStatus.UPTODATE);
            newAddress.setDeleted(false);
            newAddress.setLastUpdate(DateTime.now().toDate());
            newAddress.setCreated(DateTime.now().toDate());
            newAddress.setContactStatus(ContactStatus.ACTIVE);
            newAddress.setForm(FormOfAddress.UNKNOWN);

            // //// CHECK IF THERE IS SOMETHING MORE TO ADD
            if (newAddress.getAddressText() == null || newAddress.getAddressText() == "") {
              setOtherPropertiesToWork(li, newAddress);
              if (!ih.getItemList().isEmpty() && newAddress.getAddressText() == null || newAddress.getAddressText() == "") {
                setOtherPropertiesToWork(ih.getItemList(), newAddress);
              }
            } else if (newAddress.getPrivateAddressText() == null || newAddress.getPrivateAddressText() == "") {
              setOtherPropertiesToPrivate(li, newAddress);
              if (!ih.getItemList().isEmpty() && newAddress.getPostalAddressText() == null || newAddress.getPostalAddressText() == "")
                setOtherPropertiesToPrivate(ih.getItemList(), newAddress);
            } else {
              setPostalProperties(li, newAddress);
              if (!ih.getItemList().isEmpty() && newAddress.getPostalAddressText() == null || newAddress.getPostalAddressText() == "")
                setPostalProperties(ih.getItemList(), newAddress);
            }
            newAddresses.add(newAddress);
          }

          for (final AddressDO address : newAddresses) {
            getBaseDao().save(address);
          }

          // /// CHECK FOR EXISTING ENTRIES
          // TODO shift to dao
          //          final BaseSearchFilter af = new BaseSearchFilter();
          //          af.setSearchString(getSearchString(newAddress));
          //          final AddressDao dao = (AddressDao) getBaseDao();
          //          final QueryFilter queryFilter = new QueryFilter(af);
          //          final List<AddressDO> list = dao.internalGetList(queryFilter);

          // //// SAVING
          /*
           * if list is > 0 there are entries with equal information.
           */
          //          if (list.size() == 0) {
          //            final PageParameters params = new PageParameters();
          //
          //            // inner class to set the right return page.
          //            //            @EditPage(defaultReturnPage = AddressListPage.class)
          //            class MyEditPage extends AddressListPage
          //            {
          //
          //              /**
          //               * @param parameters
          //               */
          //              public MyEditPage(final PageParameters parameters)
          //              {
          //                super(parameters);
          //              }
          //
          //              /**
          //               * @see org.projectforge.web.wicket.AbstractEditPage#init(org.projectforge.core.AbstractBaseDO)
          //               */
          //              @Override
          //              protected void init()
          //              {
          //                super.init();
          //                data = AddressImportForm.this.getData();
          //              }
          //            }
          //            //            final AddressListPage listPage = new AddressListPage(params);
          //            //            final AddressEditPage addressEditPage = new Address//new MyEditPage(params);
          //            //                addressEditPage.newEditForm(parentPage, getData());
          //            setResponsePage(new MyEditPage(params));
          //          } else {
          //            // compare new with first match
          //            setResponsePage(new AddressComparePage(getPage().getPageParameters(), data, list.get(0)));
          //          }
        } catch (final IOException ex) {
          log.fatal("Exception encountered " + ex, ex);
        } catch (final ParserException ex) {
          log.fatal("Exception encountered " + ex, ex);
        }
      }
    } else
      feedbackPanel.error(getString("address.book.vCardImport.noFile"));
  }

  /**
   * @return
   */
  private String getSearchString(final AddressDO address)
  {
    // default search string
    String searchString = address.getName() + " " + address.getFirstName() + " ";

    // extended search string
    if (address.getMobilePhone() != null && address.getMobilePhone() != "") {
      searchString += "\"" + address.getMobilePhone() + "\"" + " ";
    }
    if (address.getEmail() != null && address.getEmail() != "") {
      searchString += address.getEmail() + " ";
    }
    if (address.getBusinessPhone() != null && address.getBusinessPhone() != "") {
      searchString += "\"" + address.getBusinessPhone() + "\"" + " ";
    }
    if (address.getPrivateEmail() != null && address.getPrivateEmail() != "") {
      searchString += address.getPrivateEmail() + " ";
    }
    return searchString;
  }

  /**
   * @param property
   */
  private void setNote(final Property property, final AddressDO address)
  {
    address.setComment(property.getValue());
  }

  /**
   * @param li
   */
  private void setProperties(final List<Property> li, final AddressDO address)
  {
    for (final Property property : li) {
      final List<Parameter> lii = property.getParameters(Id.TYPE);
      for (final Parameter param : lii) {
        if (param.getValue().equals("HOME"))
          setHomeData(property, address);
        else if (param.getValue().equals("WORK"))
          setWorkData(property, address);
      }
    }
  }

  private void setPostalProperties(final List<Property> li, final AddressDO address)
  {
    for (final Property property : li) {
      final List<Parameter> lii = property.getParameters(Id.TYPE);
      for (final Parameter param : lii) {
        if (param.getValue().equals("OTHER")) {
          // ////SET WORK ADDRESS
          if (property.getId().toString().equals("ADR")) {
            final String str[] = StringUtils.split(property.getValue(), ';');
            final int size = str.length;
            if (size >= 1)
              address.setPostalAddressText(str[0]);
            if (size >= 2)
              address.setPostalCity(str[1]);
            if (size >= 3)
              address.setPostalZipCode(str[2]);
            if (size >= 4)
              address.setPostalCountry(str[3]);
            if (size >= 5)
              address.setPostalState(str[4]);
          }
        }
      }
    }
  }

  private void setOtherPropertiesToWork(final List<Property> li, final AddressDO address)
  {
    for (final Property property : li) {
      final List<Parameter> lii = property.getParameters(Id.TYPE);
      for (final Parameter param : lii) {
        if (param.getValue().equals("OTHER")) {
          // ////SET WORK ADDRESS
          if (property.getId().toString().equals("ADR")) {
            final String str[] = StringUtils.split(property.getValue(), ';');
            final int size = str.length;
            if (size >= 1)
              address.setAddressText(str[0]);
            if (size >= 2)
              address.setCity(str[1]);
            if (size >= 3)
              address.setZipCode(str[2]);
            if (size >= 4)
              address.setCountry(str[3]);
            if (size >= 5)
              address.setState(str[4]);
          }
        }
      }
    }
  }

  private void setOtherPropertiesToPrivate(final List<Property> li, final AddressDO address)
  {
    for (final Property property : li) {
      final List<Parameter> lii = property.getParameters(Id.TYPE);
      for (final Parameter param : lii) {
        if (param.getValue().equals("OTHER")) {
          // //// SET HOME ADDRESS
          if (property.getId().toString().equals("ADR")) {
            final String str[] = StringUtils.split(property.getValue(), ';');
            final int size = str.length;
            // street
            if (size >= 1)
              address.setPrivateAddressText(str[0]);
            // city
            if (size >= 2)
              address.setPrivateCity(str[1]);
            // zip code
            if (size >= 3)
              address.setPrivateZipCode(str[2]);
            // country
            if (size >= 4)
              address.setPrivateCountry(str[3]);
            // state
            if (size >= 5)
              address.setPrivateState(str[4]);
          }
        }
      }
    }
  }

  /**
   * @param property
   */
  private void setBirth(final Property property, final AddressDO address)
  {
    if (property != null)
      address.setBirthday(new Date(DateTime.parse(property.getValue()).getMillis()));
  }

  private void setName(final Property property, final AddressDO address)
  {
    final String str[] = StringUtils.split(property.getValue(), ';');
    address.setName(str[0]);
    address.setFirstName(str[1]);
    if (str.length >= 3)
      address.setTitle(str[2]);
  }

  private void setOrganization(final Property property, final AddressDO address)
  {
    final String org = StringUtils.substringBefore(property.getValue(), ";");
    address.setOrganization(org);
    final String division = StringUtils.substringAfter(property.getValue(), ";");
    address.setDivision(division);
  }

  /**
   * Create home newAddress
   * 
   * @param property
   */
  private void setHomeData(final Property property, final AddressDO address)
  {
    boolean telCheck = true; // to seperate phone and mobil number
    // //// SET HOME EMAIL
    if (property.getId().toString().equals("EMAIL"))
      address.setPrivateEmail(property.getValue());

    // //// SET HOME PHONE
    if (property.getId().toString().equals("TEL")) {
      final List<Parameter> list = property.getParameters();
      for (final Parameter p : list) {
        if (p.getValue().toString().equals("VOICE")) {
          final String tel = getTel(property.getValue());

          // phone number first, mobil number second
          if (telCheck) {
            address.setPrivatePhone(tel);
            telCheck = false;
            break;
          } else {
            address.setPrivateMobilePhone(tel);
            break;
          }
        }
        if (address.getPrivatePhone() == null && property.toString().contains("FAX") == false) {
          address.setPrivatePhone(getTel(property.getValue()));
        } else {
          if (address.getPrivateMobilePhone() == null && property.toString().contains("FAX") == false) {
            address.setPrivateMobilePhone(getTel(property.getValue()));
          }
        }
      }
    }

    // //// SET FAX -> no private fax

    // //// SET HOME ADDRESS
    if (property.getId().toString().equals("ADR")) {
      final String str[] = StringUtils.split(property.getValue(), ';');
      final int size = str.length;
      if (size >= 1)
        address.setPrivateAddressText(str[0]);
      if (size >= 2)
        address.setPrivateCity(str[1]);
      if (size >= 3)
        address.setPrivateZipCode(str[2]);
      if (size >= 4)
        address.setPrivateCountry(str[3]);
      if (size >= 5)
        address.setPrivateState(str[4]);
    }

    // //// SET HOME URL -> no space for home url
  }

  /**
   * @param property
   * @return
   */
  private String getTel(String tel)
  {
    if (tel.startsWith("0")) {
      tel = "+49 " + tel.substring(1);
    } else {
      if (!tel.startsWith("+"))
        tel = "+49 " + tel;
    }
    return tel;
  }

  private void setWorkData(final Property property, final AddressDO address)
  {
    boolean telCheck = true; // to seperate phone and mobil number

    // //// SET WORK PHONE
    if (property.getId().toString().equals("TEL")) {
      final List<Parameter> list = property.getParameters();
      for (final Parameter p : list) {
        if (p.getValue().toString().equals("VOICE")) {
          final String tel = getTel(property.getValue());

          // phone number first, mobil number second
          if (telCheck) {
            address.setBusinessPhone(tel);
            telCheck = false;
            break;
          } else {
            address.setMobilePhone(tel);
            break;
          }
        }
        // //// SET WORK FAX
        if (p.getValue().toString().equals("FAX")) {
          address.setFax(getTel(property.getValue()));
        }
        if (address.getBusinessPhone() == null && property.toString().contains("FAX") == false) {
          address.setBusinessPhone(getTel(property.getValue()));
        } else {
          if (address.getMobilePhone() == null && property.toString().contains("FAX") == false) {
            address.setMobilePhone(getTel(property.getValue()));
          }
        }
      }
    }

    // //// SET WORK EMAIL
    if (property.getId().toString().equals("EMAIL"))
      address.setEmail(property.getValue());

    // //// SET WORK ADDRESS
    if (property.getId().toString().equals("ADR")) {
      final String str[] = StringUtils.split(property.getValue(), ';');
      final int size = str.length;
      if (size >= 1)
        address.setAddressText(str[0]);
      if (size >= 2)
        address.setCity(str[1]);
      if (size >= 3)
        address.setZipCode(str[2]);
      if (size >= 4)
        address.setCountry(str[3]);
      if (size >= 5)
        address.setState(str[4]);
    }

    // //// SET WORK URL
    if (property.getId().toString().equals("URL"))
      address.setWebsite(property.getValue());
  }
}
