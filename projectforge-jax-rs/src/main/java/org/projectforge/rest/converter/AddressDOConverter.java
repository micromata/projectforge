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

package org.projectforge.rest.converter;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.codec.binary.Base64;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.model.rest.AddressObject;

/**
 * For conversion of TaskDO to task object.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class AddressDOConverter
{
  public static AddressObject getAddressObject(final AddressDao addressDao, final AddressDO addressDO, boolean disableImageData, boolean disableVCard)
  {
    if (addressDO == null) {
      return null;
    }
    final AddressObject address = new AddressObject();
    DOConverter.copyFields(address, addressDO);
    address.setAddressStatus(addressDO.getAddressStatus() != null ? addressDO.getAddressStatus().toString() : null);
    address.setAddressText(addressDO.getAddressText());
    address.setBirthday(addressDO.getBirthday());
    address.setBusinessPhone(addressDO.getBusinessPhone());
    address.setCity(addressDO.getCity());
    address.setComment(addressDO.getComment());
    address.setCommunicationLanguage(addressDO.getCommunicationLanguage());
    address.setContactStatus(addressDO.getContactStatus() != null ? addressDO.getContactStatus().toString() : null);
    address.setCountry(addressDO.getCountry());
    address.setDivision(addressDO.getDivision());
    address.setEmail(addressDO.getEmail());
    address.setFax(addressDO.getFax());
    address.setFingerprint(addressDO.getFingerprint());
    address.setFirstName(addressDO.getFirstName());
    address.setForm(addressDO.getForm() != null ? addressDO.getForm().toString() : null);
    address.setMobilePhone(addressDO.getMobilePhone());
    address.setName(addressDO.getName());
    address.setOrganization(addressDO.getOrganization());
    address.setPositionText(addressDO.getPositionText());
    address.setPostalAddressText(addressDO.getPostalAddressText());
    address.setPostalCity(addressDO.getPostalCity());
    address.setPostalCountry(addressDO.getPostalCountry());
    address.setPostalState(addressDO.getPostalState());
    address.setPostalZipCode(addressDO.getPostalZipCode());
    address.setPrivateAddressText(addressDO.getPrivateAddressText());
    address.setPrivateCity(addressDO.getPrivateCity());
    address.setPrivateCountry(addressDO.getPrivateCountry());
    address.setPrivateEmail(addressDO.getPrivateEmail());
    address.setPrivateMobilePhone(addressDO.getPrivateMobilePhone());
    address.setPrivatePhone(addressDO.getPrivatePhone());
    address.setPrivateState(addressDO.getPrivateState());
    address.setPrivateZipCode(addressDO.getPrivateZipCode());
    address.setPublicKey(addressDO.getPublicKey());
    address.setState(addressDO.getState());
    address.setTitle(addressDO.getTitle());
    address.setWebsite(addressDO.getWebsite());
    address.setZipCode(addressDO.getZipCode());
    if (disableImageData == false) {
      address.setImage(Base64.encodeBase64String(addressDO.getAttribute("profileImageData", byte[].class)));
    }
    if (disableVCard == false) {
      final StringWriter writer = new StringWriter();
      addressDao.exportVCard(new PrintWriter(writer), addressDO);
      address.setVCardData(Base64.encodeBase64String(writer.toString().getBytes()));
    }
    return address;
  }
}
