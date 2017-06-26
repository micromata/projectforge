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

package org.projectforge.plugins.marketing;

import java.util.Map;

import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressExport;
import org.projectforge.business.converter.LanguageConverter;
import org.projectforge.business.excel.ExportColumn;
import org.projectforge.business.excel.ExportSheet;
import org.projectforge.business.excel.I18nExportColumn;
import org.projectforge.business.excel.PropertyMapping;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.springframework.stereotype.Service;

/**
 * For excel export. export must be called with two params, the first is the AddressCampaignValue map and the second the
 * title of the address campaign.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service("addressCampaignValueExport")
public class AddressCampaignValueExport extends AddressExport
{
  private enum Col
  {
    NAME, FIRST_NAME, FORM, TITLE, CONTACT_STATUS, ORGANIZATION, DIVISION, POSITION, COMMUNICATION_LANG, ADDRESS_CAMPAIGN_VALUE, ADDRESS_CAMPAIGN_COMMENT, EMAIL, WEBSITE, MAILING_ADDRESS, MAILING_ZIPCODE, MAILING_CITY, MAILING_COUNTRY, MAILING_STATE, ADDRESS, ZIPCODE, CITY, COUNTRY, STATE, POSTAL_ADDRESS, POSTAL_ZIPCODE, POSTAL_CITY, POSTAL_COUNTRY, POSTAL_STATE, ADDRESS_STATUS, BUSINESS_PHONE, FAX, MOBILE_PHONE, PRIVATE_ADDRESS, PRIVATE_ZIPCODE, PRIVATE_CITY, PRIVATE_COUNTRY, PRIVATE_STATE, PRIVATE_EMAIL, PRIVATE_PHONE, PRIVATE_MOBILE, BIRTHDAY, CREATED, MODIFIED, ID;
  }

  @Override
  protected ExportColumn[] createColumns()
  {
    return new ExportColumn[] { //
        new I18nExportColumn(Col.NAME, "name", 20),
        new I18nExportColumn(Col.FIRST_NAME, "firstName", 20),
        new I18nExportColumn(Col.FORM, "address.form", 8),
        new I18nExportColumn(Col.TITLE, "address.title", 10),
        new I18nExportColumn(Col.CONTACT_STATUS, "address.contactStatus", 10),
        new I18nExportColumn(Col.ORGANIZATION, "organization", LENGTH_STD),
        new I18nExportColumn(Col.DIVISION, "address.division", LENGTH_STD),
        new I18nExportColumn(Col.POSITION, "address.positionText", LENGTH_STD), //
        new I18nExportColumn(Col.COMMUNICATION_LANG, "address.communication", LENGTH_STD), //
        new I18nExportColumn(Col.ADDRESS_CAMPAIGN_VALUE, "value", LENGTH_STD),
        new I18nExportColumn(Col.ADDRESS_CAMPAIGN_COMMENT, "comment", LENGTH_STD),
        new I18nExportColumn(Col.EMAIL, "email", LENGTH_EMAIL),
        new I18nExportColumn(Col.WEBSITE, "address.website", LENGTH_STD),
        new I18nExportColumn(Col.MAILING_ADDRESS, "address.addressText", LENGTH_STD),
        new I18nExportColumn(Col.MAILING_ZIPCODE, "address.zipCode", LENGTH_ZIPCODE),
        new I18nExportColumn(Col.MAILING_CITY, "address.city", LENGTH_STD),
        new I18nExportColumn(Col.MAILING_COUNTRY, "address.country", LENGTH_STD),
        new I18nExportColumn(Col.MAILING_STATE, "address.state", LENGTH_STD),
        new I18nExportColumn(Col.ADDRESS, "address.addressText", LENGTH_STD),
        new I18nExportColumn(Col.ZIPCODE, "address.zipCode", LENGTH_ZIPCODE), //
        new I18nExportColumn(Col.CITY, "address.city", LENGTH_STD),
        new I18nExportColumn(Col.COUNTRY, "address.country", LENGTH_STD), //
        new I18nExportColumn(Col.STATE, "address.state", LENGTH_STD),
        new I18nExportColumn(Col.POSTAL_ADDRESS, "address.postalAddressText", LENGTH_STD),
        new I18nExportColumn(Col.POSTAL_ZIPCODE, "address.zipCode", LENGTH_ZIPCODE),
        new I18nExportColumn(Col.POSTAL_CITY, "address.city", LENGTH_STD),
        new I18nExportColumn(Col.POSTAL_COUNTRY, "address.country", LENGTH_STD),
        new I18nExportColumn(Col.POSTAL_STATE, "address.state", LENGTH_STD),
        new I18nExportColumn(Col.ADDRESS_STATUS, "address.addressStatus", 12),
        new I18nExportColumn(Col.BUSINESS_PHONE, "address.phoneType.business", LENGTH_PHONENUMBER),
        new I18nExportColumn(Col.FAX, "address.phoneType.fax", LENGTH_PHONENUMBER),
        new I18nExportColumn(Col.MOBILE_PHONE, "address.phoneType.mobile", LENGTH_PHONENUMBER),
        new I18nExportColumn(Col.PRIVATE_ADDRESS, "address.privateAddressText", LENGTH_STD),
        new I18nExportColumn(Col.PRIVATE_ZIPCODE, "address.zipCode", LENGTH_ZIPCODE),
        new I18nExportColumn(Col.PRIVATE_CITY, "address.city", LENGTH_STD),
        new I18nExportColumn(Col.PRIVATE_COUNTRY, "address.country", LENGTH_STD),
        new I18nExportColumn(Col.PRIVATE_STATE, "address.state", LENGTH_STD),
        new I18nExportColumn(Col.PRIVATE_EMAIL, "address.privateEmail", LENGTH_EMAIL),
        new I18nExportColumn(Col.PRIVATE_PHONE, "address.phoneType.private", LENGTH_PHONENUMBER),
        new I18nExportColumn(Col.PRIVATE_MOBILE, "address.phoneType.privateMobile", LENGTH_PHONENUMBER),
        new I18nExportColumn(Col.BIRTHDAY, "address.birthday", DATE_LENGTH), //
        new I18nExportColumn(Col.MODIFIED, "modified", DATE_LENGTH), //
        new I18nExportColumn(Col.ID, "id", LENGTH_ZIPCODE) };
  }

  @Override
  protected void addAddressMapping(final PropertyMapping mapping, final AddressDO address, final Object... params)
  {
    @SuppressWarnings("unchecked")
    final Map<Integer, AddressCampaignValueDO> addressCampaignValueMap = (Map<Integer, AddressCampaignValueDO>) params[0];
    final AddressCampaignValueDO addressCampaignValue = addressCampaignValueMap.get(address.getId());
    mapping.add(Col.NAME, address.getName());
    mapping.add(Col.FIRST_NAME, address.getFirstName());
    mapping.add(Col.FORM,
        address.getForm() != null ? ThreadLocalUserContext.getLocalizedString(address.getForm().getI18nKey()) : "");
    mapping.add(Col.TITLE, address.getTitle());
    mapping.add(Col.CONTACT_STATUS, address.getContactStatus());
    mapping.add(Col.ORGANIZATION, address.getOrganization());
    mapping.add(Col.DIVISION, address.getDivision());
    mapping.add(Col.POSITION, address.getPositionText());
    mapping.add(Col.COMMUNICATION_LANG,
        LanguageConverter.getLanguageAsString(address.getCommunicationLanguage(), ThreadLocalUserContext.getLocale()));
    mapping.add(Col.ADDRESS_CAMPAIGN_VALUE, addressCampaignValue != null ? addressCampaignValue.getValue() : "");
    mapping.add(Col.ADDRESS_CAMPAIGN_COMMENT, addressCampaignValue != null ? addressCampaignValue.getComment() : "");
    mapping.add(Col.EMAIL, address.getEmail());
    mapping.add(Col.WEBSITE, address.getWebsite());
    mapping.add(Col.MAILING_ADDRESS, address.getMailingAddressText());
    mapping.add(Col.MAILING_ZIPCODE, address.getMailingZipCode());
    mapping.add(Col.MAILING_CITY, address.getMailingCity());
    mapping.add(Col.MAILING_COUNTRY, address.getMailingCountry());
    mapping.add(Col.MAILING_STATE, address.getMailingState());
    mapping.add(Col.ADDRESS, address.getAddressText());
    mapping.add(Col.ZIPCODE, address.getZipCode());
    mapping.add(Col.CITY, address.getCity());
    mapping.add(Col.COUNTRY, address.getCountry());
    mapping.add(Col.STATE, address.getState());
    mapping.add(Col.POSTAL_ADDRESS, address.getPostalAddressText());
    mapping.add(Col.POSTAL_ZIPCODE, address.getPostalZipCode());
    mapping.add(Col.POSTAL_CITY, address.getPostalCity());
    mapping.add(Col.POSTAL_COUNTRY, address.getPostalCountry());
    mapping.add(Col.POSTAL_STATE, address.getPostalState());
    mapping.add(Col.ADDRESS_STATUS, address.getAddressStatus());
    mapping.add(Col.BUSINESS_PHONE, address.getBusinessPhone());
    mapping.add(Col.FAX, address.getFax());
    mapping.add(Col.MOBILE_PHONE, address.getMobilePhone());
    mapping.add(Col.PRIVATE_ADDRESS, address.getPrivateAddressText());
    mapping.add(Col.PRIVATE_ZIPCODE, address.getPrivateZipCode());
    mapping.add(Col.PRIVATE_CITY, address.getPrivateCity());
    mapping.add(Col.PRIVATE_COUNTRY, address.getPrivateCountry());
    mapping.add(Col.PRIVATE_STATE, address.getPrivateState());
    mapping.add(Col.PRIVATE_EMAIL, address.getPrivateEmail());
    mapping.add(Col.PRIVATE_PHONE, address.getPrivatePhone());
    mapping.add(Col.PRIVATE_MOBILE, address.getPrivateMobilePhone());
    mapping.add(Col.BIRTHDAY, address.getBirthday());
    mapping.add(Col.CREATED, address.getCreated());
    mapping.add(Col.MODIFIED, address.getLastUpdate());
    mapping.add(Col.ID, address.getId());
  }

  @Override
  protected String getSheetTitle()
  {
    return ThreadLocalUserContext.getLocalizedString("plugins.marketing.addressCampaign");
  }

  @Override
  protected void initSheet(final ExportSheet sheet, final Object... params)
  {
    super.initSheet(sheet);
    sheet.setMergedRegion(0, 0, Col.ADDRESS_CAMPAIGN_VALUE.ordinal(), Col.ADDRESS_CAMPAIGN_COMMENT.ordinal(),
        params[1]);
  }
}
