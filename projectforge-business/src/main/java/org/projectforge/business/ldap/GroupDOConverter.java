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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.common.BeanHelper;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.ListHelper;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.framework.xstream.XmlObjectReader;
import org.projectforge.framework.xstream.XmlObjectWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class GroupDOConverter
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(GroupDOConverter.class);

  @Autowired
  LdapService ldapService;

  @Autowired
  LdapUserDao ldapUserDao;

  static final String ID_PREFIX = "pf-id-";

  public Integer getId(final LdapGroup group)
  {
    final String businessCategory = group.getBusinessCategory();
    if (businessCategory != null && businessCategory.startsWith(ID_PREFIX) == true
        && businessCategory.length() > ID_PREFIX.length()) {
      final String id = businessCategory.substring(ID_PREFIX.length());
      return NumberHelper.parseInteger(id);
    }
    return null;
  }

  public GroupDO convert(final LdapGroup ldapGroup)
  {
    final GroupDO group = new GroupDO();
    group.setId(getId(ldapGroup));
    group.setName(ldapGroup.getCommonName());
    group.setOrganization(ldapGroup.getOrganization());
    group.setDescription(ldapGroup.getDescription());
    if (isPosixAccountValuesEmpty(ldapGroup) == false) {
      group.setLdapValues(getLdapValuesAsXml(ldapGroup));
    }
    return group;
  }

  public LdapGroup convert(final GroupDO pfGroup, final String baseDN, final Map<Integer, LdapUser> ldapUserMap)
  {
    final LdapGroup ldapGroup = new LdapGroup();
    if (pfGroup.getId() != null) {
      ldapGroup.setBusinessCategory(buildBusinessCategory(pfGroup));
    }
    ldapGroup.setCommonName(pfGroup.getName());
    ldapGroup.setOrganization(pfGroup.getOrganization());
    ldapGroup.setDescription(pfGroup.getDescription());
    if (pfGroup.getAssignedUsers() != null) {
      for (final PFUserDO user : pfGroup.getAssignedUsers()) {
        if (user.isDeactivated() == true || user.isDeleted() == true) {
          // Do not add deleted or deactivated users.
          continue;
        }
        final LdapUser ldapUser = ldapUserMap.get(user.getId());
        if (ldapUser != null) {
          ldapGroup.addMember(ldapUser, baseDN);
        } else {
          final PFUserDO cacheUser = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache()
              .getUser(user.getId());
          if (cacheUser == null || cacheUser.isDeleted() == false) {
            log.warn("LDAP user with id '"
                + user.getId()
                + "' not found in given ldapUserMap. User will be ignored in group '"
                + pfGroup.getName()
                + "'.");
          }
        }
      }
    }
    setLdapValues(ldapGroup, pfGroup.getLdapValues());
    return ldapGroup;
  }

  public boolean isPosixAccountValuesEmpty(final LdapGroup ldapGroup)
  {
    return ldapGroup.getGidNumber() == null;
  }

  /**
   * Sets the LDAP values such as posix account properties of the given ldapGroup configured in the given xml string.
   * 
   * @param ldapGroup
   * @param ldapValuesAsXml Posix account values as xml.
   */
  public void setLdapValues(final LdapGroup ldapGroup, final String ldapValuesAsXml)
  {
    if (StringUtils.isBlank(ldapValuesAsXml) == true) {
      return;
    }
    final LdapConfig ldapConfig = ldapService.getLdapConfig();
    final LdapPosixAccountsConfig posixAccountsConfig = ldapConfig != null ? ldapConfig.getPosixAccountsConfig() : null;
    if (posixAccountsConfig == null) {
      // No posix account default values configured
      return;
    }
    final LdapGroupValues values = readLdapGroupValues(ldapValuesAsXml);
    if (values == null) {
      return;
    }
    if (values.getGidNumber() != null) {
      ldapGroup.setGidNumber(values.getGidNumber());
    } else {
      ldapGroup.setGidNumber(-1);
    }
  }

  public LdapGroupValues readLdapGroupValues(final String ldapValuesAsXml)
  {
    if (StringUtils.isBlank(ldapValuesAsXml) == true) {
      return null;
    }
    final XmlObjectReader reader = new XmlObjectReader();
    reader.initialize(LdapGroupValues.class);
    final LdapGroupValues values = (LdapGroupValues) reader.read(ldapValuesAsXml);
    return values;
  }

  /**
   * Exports the LDAP values such as posix account properties of the given ldapGroup as xml string.
   * 
   * @param ldapGroup
   */
  public String getLdapValuesAsXml(final LdapGroup ldapGroup)
  {
    final LdapConfig ldapConfig = ldapService.getLdapConfig();
    final LdapPosixAccountsConfig posixAccountsConfig = ldapConfig != null ? ldapConfig.getPosixAccountsConfig() : null;
    LdapGroupValues values = null;
    if (posixAccountsConfig != null) {
      values = new LdapGroupValues();
      if (ldapGroup.getGidNumber() != null) {
        values.setGidNumber(ldapGroup.getGidNumber());
      }
    }
    return getLdapValuesAsXml(values);
  }

  /**
   * Exports the LDAP values such as posix account properties of the given ldapGroup as xml string.
   * 
   * @param values
   */
  public String getLdapValuesAsXml(final LdapGroupValues values)
  {
    final XmlObjectReader reader = new XmlObjectReader();
    reader.initialize(LdapGroupValues.class);
    final String xml = XmlObjectWriter.writeAsXml(values);
    return xml;
  }

  public String buildBusinessCategory(final GroupDO group)
  {
    return ID_PREFIX + group.getId();
  }

  /**
   * Copies the fields shared with ldap.
   * 
   * @param src
   * @param dest
   * @return true if any modification is detected, otherwise false.
   */
  public boolean copyGroupFields(final GroupDO src, final GroupDO dest)
  {
    final boolean modified = BeanHelper.copyProperties(src, dest, true, "name", "organization", "description");
    return modified;
  }

  /**
   * Copies the fields.
   * 
   * @param src
   * @param dest
   * @return true if any modification is detected, otherwise false.
   */
  public boolean copyGroupFields(final LdapGroup src, final LdapGroup dest)
  {
    boolean modified;
    final List<String> properties = new LinkedList<String>();
    ListHelper.addAll(properties, "description", "organization");
    if (ldapUserDao.isPosixAccountsConfigured() == true && isPosixAccountValuesEmpty(src) == false) {
      ListHelper.addAll(properties, "gidNumber");
    }
    modified = BeanHelper.copyProperties(src, dest, true, properties.toArray(new String[0]));
    // Checks if the sets aren't equal:
    if (SetUtils.isEqualSet(src.getMembers(), dest.getMembers()) == false) {
      if (LdapGroupDao.hasMembers(src) == true || LdapGroupDao.hasMembers(dest) == true) {
        // If both, src and dest have no members, then do nothing, otherwise:
        modified = true;
        dest.clearMembers();
        dest.addAllMembers(src.getMembers());
      }
    }
    return modified;
  }
}
