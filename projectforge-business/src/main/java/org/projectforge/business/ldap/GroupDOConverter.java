/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.common.BeanHelper;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.ListHelper;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.framework.xmlstream.XmlObjectReader;
import org.projectforge.framework.xmlstream.XmlObjectWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class GroupDOConverter
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GroupDOConverter.class);

  @Autowired
  LdapService ldapService;

  @Autowired
  LdapUserDao ldapUserDao;

  @Autowired
  private UserGroupCache userGroupCache;

  static final String ID_PREFIX = "pf-id-";

  public Long getId(final LdapGroup group)
  {
    final String businessCategory = group.getBusinessCategory();
    if (businessCategory != null && businessCategory.startsWith(ID_PREFIX)
        && businessCategory.length() > ID_PREFIX.length()) {
      final String id = businessCategory.substring(ID_PREFIX.length());
      return NumberHelper.parseLong(id);
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
    if (!isPosixAccountValuesEmpty(ldapGroup)) {
      group.setLdapValues(getLdapValuesAsXml(ldapGroup));
    }
    return group;
  }

  public LdapGroup convert(final GroupDO pfGroup, final String baseDN, final Map<Long, LdapUser> ldapUserMap)
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
        if (user.getDeactivated() || user.getDeleted()) {
          // Do not add deleted or deactivated users.
          continue;
        }
        final LdapUser ldapUser = ldapUserMap.get(user.getId());
        if (ldapUser != null) {
          ldapGroup.addMember(ldapUser, baseDN);
        } else {
          final PFUserDO cacheUser = userGroupCache.getUser(user.getId());
          if (cacheUser == null || !cacheUser.getDeleted()) {
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
    if (StringUtils.isBlank(ldapValuesAsXml)) {
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
    if (StringUtils.isBlank(ldapValuesAsXml)) {
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
    final List<String> properties = new LinkedList<>();
    ListHelper.addAll(properties, "description", "organization");
    if (ldapUserDao.isPosixAccountsConfigured() && !isPosixAccountValuesEmpty(src)) {
      ListHelper.addAll(properties, "gidNumber");
    }
    modified = BeanHelper.copyProperties(src, dest, true, properties.toArray(new String[0]));
    // Checks if the sets aren't equal:
    if (!SetUtils.isEqualSet(src.getMembers(), dest.getMembers())) {
      if (LdapGroupDao.hasMembers(src) || LdapGroupDao.hasMembers(dest)) {
        // If both, src and dest have no members, then do nothing, otherwise:
        modified = true;
        dest.clearMembers();
        dest.addAllMembers(src.getMembers());
      }
    }
    return modified;
  }
}
