/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.ldap;

import org.projectforge.business.address.AddressDO;
import org.projectforge.framework.utils.NumberHelper;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class AddressDOConverter
{
  public static final String UID_PREFIX = "pf-address-";

  public static AddressDO convert(final LdapPerson person) {
    final AddressDO address = new AddressDO();
    address.setName(person.getSurname());
    address.setFirstName(person.getGivenName());
    final String uid = person.getUid();
    if (uid != null && uid.startsWith(UID_PREFIX) == true && uid.length() > UID_PREFIX.length()) {
      final String id = uid.substring(UID_PREFIX.length());
      address.setId(NumberHelper.parseInteger(id));
    }
    address.setOrganization(person.getOrganization());
    address.setComment(person.getDescription());
    //person.getMail();
    address.setBusinessPhone( person.getTelephoneNumber());
    address.setPrivatePhone( person.getHomePhoneNumber());
    //person.getMobilePhoneNumber();
    return address;
  }

  public static LdapPerson convert(final AddressDO address)
  {
    final LdapPerson person = new LdapPerson();
    person.setSurname(address.getName());
    person.setGivenName(address.getFirstName());
    if (address.getId() != null) {
      person.setUid(UID_PREFIX + address.getId());
    }
    person.setOrganization(address.getOrganization());
    person.setDescription(address.getComment());
    person.setMail(address.getEmail(), address.getPrivateEmail());
    person.setTelephoneNumber(address.getBusinessPhone());
    person.setHomePhoneNumber(address.getPrivatePhone());
    person.setMobilePhoneNumber(address.getMobilePhone(), address.getPrivateMobilePhone());
    return person;
  }
}
