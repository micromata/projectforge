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

import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.apache.commons.collections.CollectionUtils;
import org.projectforge.framework.utils.NumberHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
public class LdapGroupDao extends LdapDao<String, LdapGroup>
{
  private static final String[] ADDITIONAL_OBJECT_CLASSES = { "top" };

  private static final String[] ADDITIONAL_OBJECT_CLASSES_WITH_POSIX_SUPPORT = { "top", "posixGroup" };

  private static final String NONE_UNIQUE_MEMBER_ID = "cn=none";

  @Autowired
  GroupDOConverter groupDOConverter;

  @Autowired
  LdapUserDao ldapUserDao;

  /**
   * Since member of groups can't be null, "cn=none" if the group has no real members.
   * 
   * @param group
   * @return
   */
  public static boolean hasMembers(final LdapGroup group)
  {
    if (group.getMembers() == null || group.getMembers().size() == 0) {
      return false;
    }
    if (group.getMembers().size() > 1) {
      return true;
    }
    return group.getMembers().iterator().next().startsWith(NONE_UNIQUE_MEMBER_ID) == false;
  }

  /**
   * @see org.projectforge.business.ldap.LdapDao#getObjectClass()
   */
  @Override
  protected String getObjectClass()
  {
    return "groupOfUniqueNames";
  }

  /**
   * @see org.projectforge.business.ldap.LdapDao#getAdditionalObjectClasses()
   */
  @Override
  protected String[] getAdditionalObjectClasses()
  {
    throw new UnsupportedOperationException("Call getAdditionalObjectClasses(LdapGroup) instead.");
  }

  /**
   * @see org.projectforge.business.ldap.LdapDao#getAdditionalObjectClasses(org.projectforge.business.ldap.LdapObject)
   */
  @Override
  protected String[] getAdditionalObjectClasses(final LdapGroup obj)
  {
    final boolean posixAccount = ldapUserDao.isPosixAccountsConfigured() == true
        && groupDOConverter.isPosixAccountValuesEmpty(obj) == false;
    if (posixAccount == true) {
      return ADDITIONAL_OBJECT_CLASSES_WITH_POSIX_SUPPORT;
    }
    return ADDITIONAL_OBJECT_CLASSES;
  }

  /**
   * @see org.projectforge.business.ldap.LdapDao#getIdAttrId()
   */
  @Override
  public String getIdAttrId()
  {
    return "businessCategory";
  }

  /**
   * @see org.projectforge.business.ldap.LdapDao#getId(org.projectforge.business.ldap.LdapObject)
   */
  @Override
  public String getId(final LdapGroup obj)
  {
    return obj.getBusinessCategory();
  }

  /**
   * Used for bind and update.
   * 
   * @param person
   * @return
   * @see org.projectforge.business.ldap.LdapDao#getModificationItems(org.projectforge.business.ldap.LdapObject)
   */
  @Override
  protected List<ModificationItem> getModificationItems(final List<ModificationItem> list, final LdapGroup group)
  {
    createAndAddModificationItems(list, "businessCategory", group.getBusinessCategory());
    createAndAddModificationItems(list, "o", group.getOrganization());
    createAndAddModificationItems(list, "description", group.getDescription());
    if (CollectionUtils.isNotEmpty(group.getMembers()) == true) {
      createAndAddModificationItems(list, "uniqueMember", group.getMembers());
    } else {
      createAndAddModificationItems(list, "uniqueMember", NONE_UNIQUE_MEMBER_ID);
    }
    final boolean modifyPosixAccount = ldapUserDao.isPosixAccountsConfigured() == true
        && groupDOConverter.isPosixAccountValuesEmpty(group) == false;
    if (modifyPosixAccount == true) {
      if (group.getObjectClasses() != null) {
        final List<String> missedObjectClasses = LdapUtils.getMissedObjectClasses(getAdditionalObjectClasses(group),
            getObjectClass(),
            group.getObjectClasses());
        if (CollectionUtils.isNotEmpty(missedObjectClasses) == true) {
          for (final String missedObjectClass : missedObjectClasses) {
            list.add(createModificationItem(DirContext.ADD_ATTRIBUTE, "objectClass", missedObjectClass));
          }
        }
      }
    }
    if (modifyPosixAccount == true) {
      createAndAddModificationItems(list, "gidNumber", String.valueOf(group.getGidNumber()));
    }
    return list;
  }

  /**
   * @see org.projectforge.business.ldap.LdapDao#mapToObject(java.lang.String, javax.naming.directory.Attributes)
   */
  @Override
  protected LdapGroup mapToObject(final String dn, final Attributes attributes) throws NamingException
  {
    final LdapGroup group = new LdapGroup();
    group.setBusinessCategory(LdapUtils.getAttributeStringValue(attributes, "businessCategory"));
    group.setDescription(LdapUtils.getAttributeStringValue(attributes, "description"));
    group.setOrganization(LdapUtils.getAttributeStringValue(attributes, "o"));
    final String[] members = LdapUtils.getAttributeStringValues(attributes, "uniqueMember");
    if (members != null) {
      for (final String member : members) {
        group.addMember(member, ldapConfig.getBaseDN());
      }
    }
    final boolean posixAccountsConfigured = ldapUserDao.isPosixAccountsConfigured();
    if (posixAccountsConfigured == true) {
      final String no = LdapUtils.getAttributeStringValue(attributes, "gidNumber");
      group.setGidNumber(NumberHelper.parseInteger(no));
    }
    return group;
  }

  /**
   * @see org.projectforge.business.ldap.LdapDao#buildId(java.lang.Object)
   */
  @Override
  protected String buildId(final Object id)
  {
    if (id == null) {
      return null;
    }
    if (id instanceof String && ((String) id).startsWith(GroupDOConverter.ID_PREFIX) == true) {
      return String.valueOf(id);
    }
    return GroupDOConverter.ID_PREFIX + id;
  }

  /**
   * @see org.projectforge.business.ldap.LdapDao#getOuBase()
   */
  @Override
  protected String getOuBase()
  {
    return ldapConfig.getGroupBase();
  }
}
