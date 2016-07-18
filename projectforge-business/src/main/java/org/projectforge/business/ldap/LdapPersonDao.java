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

package org.projectforge.business.ldap;

import org.springframework.stereotype.Service;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class LdapPersonDao extends LdapDao<String, LdapPerson>
{
  private static final String[] ADDITIONAL_OBJECT_CLASSES = { "top", "inetOrgPerson"};

  /**
   * @see org.projectforge.business.ldap.LdapDao#getObjectClass()
   */
  @Override
  protected String getObjectClass()
  {
    return "person";
  }

  @Override
  protected String[] getAdditionalObjectClasses()
  {
    return ADDITIONAL_OBJECT_CLASSES;
  }

  /**
   * @see org.projectforge.business.ldap.LdapDao#getIdAttrId()
   */
  @Override
  public String getIdAttrId()
  {
    return "uid";
  }

  /**
   * @see org.projectforge.business.ldap.LdapDao#getId(org.projectforge.business.ldap.LdapObject)
   */
  @Override
  public String getId(final LdapPerson obj)
  {
    return obj.getUid();
  }

  /**
   * @see org.projectforge.business.ldap.LdapDao#mapToObject(java.lang.String, javax.naming.directory.Attributes)
   */
  @Override
  protected LdapPerson mapToObject(final String dn, final Attributes attributes) throws NamingException
  {
    final LdapPerson person = new LdapPerson();
    mapToObject(dn, person, attributes);
    return person;
  }

  protected void mapToObject(final String dn, final LdapPerson person, final Attributes attributes) throws NamingException
  {
    person.setSurname(LdapUtils.getAttributeStringValue(attributes, "sn"));
    person.setGivenName(LdapUtils.getAttributeStringValue(attributes, "givenName"));
    person.setUid(LdapUtils.getAttributeStringValue(attributes, "uid"));
    person.setEmployeeNumber(LdapUtils.getAttributeStringValue(attributes, "employeeNumber"));
    person.setOrganization(LdapUtils.getAttributeStringValue(attributes, "o"));
    person.setMail(LdapUtils.getAttributeStringValues(attributes, "mail"));
    person.setDescription(LdapUtils.getAttributeStringValue(attributes, "description"));
    person.setTelephoneNumber(LdapUtils.getAttributeStringValue(attributes, "telephoneNumber"));
    person.setMobilePhoneNumber(LdapUtils.getAttributeStringValues(attributes, "mobile"));
    person.setHomePhoneNumber(LdapUtils.getAttributeStringValue(attributes, "homePhone"));
  }

  /**
   * Used for bind and update.
   * @param person
   * @return
   * @see org.projectforge.business.ldap.LdapDao#getModificationItems(org.projectforge.business.ldap.LdapObject)
   */
  @Override
  protected List<ModificationItem> getModificationItems(final List<ModificationItem> list, final LdapPerson person)
  {
    createAndAddModificationItems(list, "sn", person.getSurname());
    createAndAddModificationItems(list, "givenName", person.getGivenName());
    createAndAddModificationItems(list, "uid", person.getUid());
    createAndAddModificationItems(list, "employeeNumber", person.getEmployeeNumber());
    createAndAddModificationItems(list, "o", person.getOrganization());
    createAndAddModificationItems(list, "mail", person.getMail());
    createAndAddModificationItems(list, "description", person.getDescription());
    createAndAddModificationItems(list, "telephoneNumber", person.getTelephoneNumber());
    createAndAddModificationItems(list, "mobile", person.getMobilePhoneNumber());
    createAndAddModificationItems(list, "homePhone", person.getHomePhoneNumber());
    return list;
  }

  /**
   * @see org.projectforge.business.ldap.LdapDao#getOuBase()
   */
  @Override
  protected String getOuBase()
  {
    throw new UnsupportedOperationException(
        "No support of contacts (person) yet implemented (only users are supported by LdapUserDao yet). No ou-base available.");
  }
}
