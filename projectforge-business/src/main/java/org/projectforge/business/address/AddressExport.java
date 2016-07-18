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

package org.projectforge.business.address;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.hssf.util.HSSFColor;
import org.projectforge.business.converter.LanguageConverter;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.excel.CellFormat;
import org.projectforge.excel.ContentProvider;
import org.projectforge.excel.ExportCell;
import org.projectforge.excel.ExportColumn;
import org.projectforge.excel.ExportRow;
import org.projectforge.excel.ExportSheet;
import org.projectforge.excel.ExportWorkbook;
import org.projectforge.excel.I18nExportColumn;
import org.projectforge.excel.PropertyMapping;
import org.projectforge.export.MyXlsContentProvider;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * For excel export.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service("addressExport")
public class AddressExport
{
  private class MyContentProvider extends MyXlsContentProvider
  {
    public MyContentProvider(final ExportWorkbook workbook)
    {
      super(workbook);
    }

    /**
     * @see org.projectforge.excel.XlsContentProvider#updateRowStyle(org.projectforge.excel.ExportRow)
     */
    @Override
    public MyContentProvider updateRowStyle(final ExportRow row)
    {
      for (final ExportCell cell : row.getCells()) {
        final CellFormat format = cell.ensureAndGetCellFormat();
        format.setFillForegroundColor(HSSFColor.WHITE.index);
        switch (row.getRowNum()) {
          case 0:
            format.setFont(FONT_HEADER);
            break;
          case 1:
            format.setFont(FONT_NORMAL_BOLD);
            format.setFillForegroundColor(HSSFColor.YELLOW.index);
            break;
          default:
            format.setFont(FONT_NORMAL);
            if (row.getRowNum() % 2 == 0) {
              format.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
            }
            break;
        }
      }
      return this;
    }

    @Override
    public org.projectforge.excel.ContentProvider newInstance()
    {
      return new MyContentProvider(this.workbook);
    }
  };

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AddressExport.class);

  @Autowired
  protected AccessChecker accessChecker;

  protected static final int LENGTH_PHONENUMBER = 20;

  protected static final int LENGTH_EMAIL = 30;

  protected static final int LENGTH_ZIPCODE = 7;

  protected static final int LENGTH_STD = 30;

  protected static final int LENGTH_EXTRA_LONG = 80;

  protected static final int DATE_LENGTH = 10;

  private enum Col
  {
    NAME, FIRST_NAME, FORM, TITLE, CONTACT_STATUS, ORGANIZATION, DIVISION, POSITION, COMMUNICATION_LANG, EMAIL, WEBSITE, MAILING_ADDRESS, MAILING_ZIPCODE, MAILING_CITY, MAILING_COUNTRY, MAILING_STATE, ADDRESS, ZIPCODE, CITY, COUNTRY, STATE, POSTAL_ADDRESS, POSTAL_ZIPCODE, POSTAL_CITY, POSTAL_COUNTRY, POSTAL_STATE, ADDRESS_STATUS, BUSINESS_PHONE, FAX, MOBILE_PHONE, PRIVATE_ADDRESS, PRIVATE_ZIPCODE, PRIVATE_CITY, PRIVATE_COUNTRY, PRIVATE_STATE, PRIVATE_EMAIL, PRIVATE_PHONE, PRIVATE_MOBILE, BIRTHDAY, CREATED, MODIFIED, COMMENT, FINGERPRINT, PUBLIC_KEY, ID;
  }

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
        new I18nExportColumn(Col.COMMENT, "comment", LENGTH_EXTRA_LONG),
        new I18nExportColumn(Col.FINGERPRINT, "address.fingerprint", LENGTH_STD),
        new I18nExportColumn(Col.PUBLIC_KEY, "address.publicKey", LENGTH_EXTRA_LONG), //
        new I18nExportColumn(Col.ID, "id", LENGTH_ZIPCODE) };
  }

  protected void addAddressMapping(final PropertyMapping mapping, final AddressDO address, final Object... params)
  {
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
    mapping.add(Col.COMMENT, address.getComment());
    mapping.add(Col.FINGERPRINT, address.getFingerprint());
    mapping.add(Col.PUBLIC_KEY, address.getPublicKey());
    mapping.add(Col.ID, address.getId());
  }

  protected String getSheetTitle()
  {
    return ThreadLocalUserContext.getLocalizedString("address.addresses");
  }

  protected void initSheet(final ExportSheet sheet, final Object... params)
  {
  }

  /**
   * Exports the filtered list as table with almost all fields. For members of group FINANCE_GROUP (PF_Finance) and
   * MARKETING_GROUP (PF_Marketing) all addresses are exported, for others only those which are marked as personal
   * favorites.
   * 
   * @param Used by sub classes such as AddressCampaignValueExport.
   * @throws IOException
   */
  public byte[] export(final List<AddressDO> origList, final Map<Integer, PersonalAddressDO> personalAddressMap,
      final Object... params)
  {
    log.info("Exporting address list.");
    final ExportColumn[] columns = createColumns();

    final List<AddressDO> list = new ArrayList<AddressDO>();
    for (final AddressDO address : origList) {
      if (accessChecker.isLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP,
          ProjectForgeGroup.MARKETING_GROUP) == true) {
        // Add all addresses for users of finance group:
        list.add(address);
      } else if (personalAddressMap.containsKey(address.getId()) == true)
        // For others only those which are personal:
        list.add(address);
    }
    if (CollectionUtils.isEmpty(list) == true) {
      return null;
    }
    final ExportWorkbook xls = new ExportWorkbook();
    final ContentProvider contentProvider = new MyContentProvider(xls);
    // create a default Date format and currency column
    xls.setContentProvider(contentProvider);

    final String sheetTitle = getSheetTitle();
    final ExportSheet sheet = xls.addSheet(sheetTitle);
    sheet.addRow(); // Column headers
    sheet.setMergedRegion(0, 0, Col.MAILING_ADDRESS.ordinal(), Col.MAILING_STATE.ordinal(), "Mailing");
    sheet.setMergedRegion(0, 0, Col.ADDRESS.ordinal(), Col.STATE.ordinal(),
        ThreadLocalUserContext.getLocalizedString("address.addressText"));
    sheet.setMergedRegion(0, 0, Col.POSTAL_ADDRESS.ordinal(), Col.POSTAL_STATE.ordinal(), ThreadLocalUserContext
        .getLocalizedString("address.postalAddressText"));
    sheet.setMergedRegion(0, 0, Col.PRIVATE_ADDRESS.ordinal(), Col.PRIVATE_STATE.ordinal(), ThreadLocalUserContext
        .getLocalizedString("address.privateAddressText"));
    initSheet(sheet, params);

    sheet.createFreezePane(1, 2);
    sheet.setColumns(columns);

    final PropertyMapping mapping = new PropertyMapping();
    for (final AddressDO address : list) {
      addAddressMapping(mapping, address, params);
      sheet.addRow(mapping.getMapping(), 0);
    }
    sheet.setZoom(3, 4); // 75%
    return xls.getAsByteArray();
  }

}
